///usr/bin/env jbang "$0" "$@"; exit $?
//JAVA 17+
//DEPS com.google.code.gson:gson:2.11.0

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Convert Tamboui JFR events to viewer-friendly trace.json.
 *
 * Usage:
 *   jbang tools/jfr-to-trace.java recording.jfr [out.json]
 */
public class jfr_to_trace {

    private static final Set<String> SUPPORTED = Set.of(
            "dev.tamboui.ui.frame",
            "dev.tamboui.ui.summary",
            "dev.tamboui.ui.node",
            "dev.tamboui.ui.layout",
            "dev.tamboui.ui.cost.layout",
            "dev.tamboui.ui.cost.render"
    );

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: jbang tools/jfr-to-trace.java <recording.jfr> [out.json]");
            System.exit(2);
        }

        Path in = Path.of(args[0]);
        Path out = args.length >= 2 ? Path.of(args[1]) : Path.of("trace.json");

        Map<Long, Frame> frames = new LinkedHashMap<>();

        try (RecordingFile rf = new RecordingFile(in)) {
            while (rf.hasMoreEvents()) {
                RecordedEvent ev = rf.readEvent();
                String name = ev.getEventType().getName();
                if (!SUPPORTED.contains(name)) continue;

                long frameId = longField(ev, "frameId", -1);
                if (frameId < 0) continue;

                Frame f = frames.computeIfAbsent(frameId, id -> new Frame(id));
                f.tsNanos = ev.getStartTime().toEpochMilli() * 1_000_000L;

                switch (name) {
                    case "dev.tamboui.ui.frame" -> mergeFrame(f, ev);
                    case "dev.tamboui.ui.summary" -> mergeSummary(f, ev);
                    case "dev.tamboui.ui.node" -> mergeNode(f, ev);
                    case "dev.tamboui.ui.layout" -> mergeLayout(f, ev);
                    case "dev.tamboui.ui.cost.layout" -> mergeCostLayout(f, ev);
                    case "dev.tamboui.ui.cost.render" -> mergeCostRender(f, ev);
                }
            }
        }

        Trace trace = new Trace();
        trace.meta = new Meta("tamboui", "unknown", in.toString(), Instant.now().toString());
        trace.frames = new ArrayList<>(frames.values());

        // sort nodes by parent/index for stable tree rendering
        for (Frame f : trace.frames) {
            f.nodes.sort(Comparator
                    .comparingLong((Node n) -> n.parentId)
                    .thenComparingInt(n -> n.indexInParent)
                    .thenComparingLong(n -> n.nodeId));
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        java.nio.file.Files.writeString(out, gson.toJson(trace));
        System.out.println("Wrote " + out + " with " + trace.frames.size() + " frame(s)");
    }

    private static void mergeFrame(Frame f, RecordedEvent ev) {
        f.screen = new Screen(
                intField(ev, "screenWidth", f.screen != null ? f.screen.w : 0),
                intField(ev, "screenHeight", f.screen != null ? f.screen.h : 0)
        );
        f.focusedNodeId = longField(ev, "focusedNodeId", f.focusedNodeId);
        f.routeId = longField(ev, "routeId", f.routeId);
    }

    private static void mergeSummary(Frame f, RecordedEvent ev) {
        f.summary = new Summary(
                intField(ev, "nodeCount", 0),
                longField(ev, "layoutTotalNanos", 0),
                longField(ev, "renderTotalNanos", 0),
                longField(ev, "diffNanos", 0),
                intField(ev, "dirtyCells", 0),
                longField(ev, "flushNanos", 0)
        );
    }

    private static void mergeNode(Frame f, RecordedEvent ev) {
        long nodeId = longField(ev, "nodeId", -1);
        if (nodeId < 0) return;
        Node n = f.nodeMap.computeIfAbsent(nodeId, Node::new);
        n.parentId = longField(ev, "parentId", n.parentId);
        n.indexInParent = intField(ev, "indexInParent", n.indexInParent);
        n.type = strField(ev, "type", n.type);
        n.id = strField(ev, "id", n.id);
        n.classes = strField(ev, "classes", n.classes);
        n.flags = parseFlags(strField(ev, "flags", n.flagsRaw));
        n.flagsRaw = strField(ev, "flags", n.flagsRaw);
        n.zIndex = intField(ev, "zIndex", n.zIndex);
        // Support combined node+layout event payloads
        if (ev.getEventType().getName().equals("dev.tamboui.ui.node")) {
            int x = intField(ev, "x", Integer.MIN_VALUE);
            int y = intField(ev, "y", Integer.MIN_VALUE);
            int w = intField(ev, "width", Integer.MIN_VALUE);
            int h = intField(ev, "height", Integer.MIN_VALUE);
            if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE && w != Integer.MIN_VALUE && h != Integer.MIN_VALUE) {
                n.layout = new Layout(x, y, w, h, boolField(ev, "clipped", false));
            }
        }
        if (!f.nodes.contains(n)) f.nodes.add(n);
    }

    private static void mergeLayout(Frame f, RecordedEvent ev) {
        long nodeId = longField(ev, "nodeId", -1);
        if (nodeId < 0) return;
        Node n = f.nodeMap.computeIfAbsent(nodeId, Node::new);
        n.layout = new Layout(
                intField(ev, "x", 0),
                intField(ev, "y", 0),
                intField(ev, "width", 0),
                intField(ev, "height", 0),
                boolField(ev, "clipped", false)
        );
        if (!f.nodes.contains(n)) f.nodes.add(n);
    }

    private static void mergeCostLayout(Frame f, RecordedEvent ev) {
        long nodeId = longField(ev, "nodeId", -1);
        if (nodeId < 0) return;
        Node n = f.nodeMap.computeIfAbsent(nodeId, Node::new);
        n.cost.layoutNanos = longField(ev, "durationNanos", 0);
        if (!f.nodes.contains(n)) f.nodes.add(n);
    }

    private static void mergeCostRender(Frame f, RecordedEvent ev) {
        long nodeId = longField(ev, "nodeId", -1);
        if (nodeId < 0) return;
        Node n = f.nodeMap.computeIfAbsent(nodeId, Node::new);
        n.cost.renderNanos = longField(ev, "durationNanos", 0);
        n.cost.dirtyCells = intField(ev, "dirtyCells", n.cost.dirtyCells);
        if (!f.nodes.contains(n)) f.nodes.add(n);
    }

    private static List<String> parseFlags(String s) {
        if (s == null || s.isBlank()) return List.of();
        String[] parts = s.split("[,\\s]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) if (!p.isBlank()) out.add(p.trim());
        return out;
    }

    private static long longField(RecordedEvent ev, String field, long def) {
        try { return ev.getLong(field); } catch (Throwable t) { return def; }
    }

    private static int intField(RecordedEvent ev, String field, int def) {
        try { return ev.getInt(field); } catch (Throwable t) { return def; }
    }

    private static boolean boolField(RecordedEvent ev, String field, boolean def) {
        try { return ev.getBoolean(field); } catch (Throwable t) { return def; }
    }

    private static String strField(RecordedEvent ev, String field, String def) {
        try {
            Object v = ev.getValue(field);
            return v != null ? String.valueOf(v) : def;
        } catch (Throwable t) {
            return def;
        }
    }

    static class Trace {
        Meta meta;
        List<Frame> frames;
    }

    record Meta(String app, String version, String source, String generatedAt) {}

    static class Frame {
        long frameId;
        long tsNanos;
        Screen screen = new Screen(0, 0);
        long focusedNodeId = -1;
        long routeId = -1;
        Summary summary = new Summary(0,0,0,0,0,0);
        List<Node> nodes = new ArrayList<>();
        transient Map<Long, Node> nodeMap = new LinkedHashMap<>();

        Frame(long frameId) { this.frameId = frameId; }
    }

    record Screen(int w, int h) {}

    record Summary(int nodeCount, long layoutTotalNanos, long renderTotalNanos,
                   long diffNanos, int dirtyCells, long flushNanos) {}

    static class Node {
        long nodeId;
        long parentId = -1;
        int indexInParent = 0;
        String type;
        String id;
        String classes;
        transient String flagsRaw;
        List<String> flags = List.of();
        int zIndex = 0;
        Layout layout;
        Cost cost = new Cost();

        Node(long nodeId) { this.nodeId = nodeId; }
    }

    record Layout(int x, int y, int w, int h, boolean clipped) {}

    static class Cost {
        long layoutNanos;
        long renderNanos;
        int dirtyCells;
    }
}
