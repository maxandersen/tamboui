///usr/bin/env jbang "$0" "$@"; exit $?
//JAVA 17+
//DEPS com.google.code.gson:gson:2.11.0

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class jfr_to_trace {
    private static final Set<String> SUPPORTED = Set.of(
            "dev.tamboui.terminal.draw",
            "dev.tamboui.toolkit.route",
            "dev.tamboui.toolkit.candidate",
            "dev.tamboui.toolkit.focus.change",
            "dev.tamboui.toolkit.focus.navigation",
            "dev.tamboui.toolkit.drag.state",
            "dev.tamboui.toolkit.global.handler"
    );

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: jbang tools/jfr-to-trace.java <recording.jfr> [out.json]");
            System.exit(2);
        }
        Path in = Path.of(args[0]);
        Path out = args.length >= 2 ? Path.of(args[1]) : Path.of("trace.json");

        Trace trace = new Trace();
        trace.meta = new Meta("tamboui", "current-events", in.toString(), Instant.now().toString());

        Map<Long, RouteFrame> byRoute = new LinkedHashMap<>();
        List<DrawSample> draws = new ArrayList<>();

        try (RecordingFile rf = new RecordingFile(in)) {
            long drawSeq = 0;
            while (rf.hasMoreEvents()) {
                RecordedEvent ev = rf.readEvent();
                String name = ev.getEventType().getName();
                if (!SUPPORTED.contains(name)) continue;

                switch (name) {
                    case "dev.tamboui.terminal.draw" -> {
                        DrawSample d = new DrawSample();
                        d.index = ++drawSeq;
                        d.tsNanos = ev.getStartTime().toEpochMilli() * 1_000_000L;
                        d.durationNanos = ev.getDuration() != null ? ev.getDuration().toNanos() : 0L;
                        draws.add(d);
                    }
                    case "dev.tamboui.toolkit.route" -> {
                        long routeId = longField(ev, "routeId", -1);
                        if (routeId < 0) break;
                        RouteFrame f = byRoute.computeIfAbsent(routeId, RouteFrame::new);
                        f.tsNanos = ev.getStartTime().toEpochMilli() * 1_000_000L;
                        f.event = strField(ev, "event", f.event);
                        f.focusedId = strField(ev, "focusedId", f.focusedId);
                        f.elementCount = intField(ev, "elementCount", f.elementCount);
                        f.result = strField(ev, "result", f.result);
                        f.durationNanos = ev.getDuration() != null ? ev.getDuration().toNanos() : f.durationNanos;
                    }
                    case "dev.tamboui.toolkit.candidate" -> {
                        long routeId = longField(ev, "routeId", -1);
                        if (routeId < 0) break;
                        RouteFrame f = byRoute.computeIfAbsent(routeId, RouteFrame::new);
                        Candidate c = new Candidate();
                        c.elementId = strField(ev, "elementId", null);
                        c.elementType = strField(ev, "elementType", null);
                        c.phase = strField(ev, "phase", null);
                        c.decision = strField(ev, "decision", null);
                        c.reason = strField(ev, "reason", null);
                        f.candidates.add(c);
                    }
                    case "dev.tamboui.toolkit.focus.change" -> {
                        long routeId = longField(ev, "routeId", -1);
                        if (routeId < 0) break;
                        RouteFrame f = byRoute.computeIfAbsent(routeId, RouteFrame::new);
                        FocusChange ch = new FocusChange();
                        ch.fromId = strField(ev, "fromId", null);
                        ch.toId = strField(ev, "toId", null);
                        ch.reason = strField(ev, "reason", null);
                        f.focusChanges.add(ch);
                    }
                    case "dev.tamboui.toolkit.focus.navigation" -> {
                        long routeId = longField(ev, "routeId", -1);
                        if (routeId < 0) break;
                        RouteFrame f = byRoute.computeIfAbsent(routeId, RouteFrame::new);
                        FocusNavigation nav = new FocusNavigation();
                        nav.action = strField(ev, "action", null);
                        nav.success = boolField(ev, "success", false);
                        nav.fromId = strField(ev, "fromId", null);
                        nav.toId = strField(ev, "toId", null);
                        f.focusNavigations.add(nav);
                    }
                    case "dev.tamboui.toolkit.drag.state" -> {
                        long routeId = longField(ev, "routeId", -1);
                        if (routeId < 0) break;
                        RouteFrame f = byRoute.computeIfAbsent(routeId, RouteFrame::new);
                        DragState ds = new DragState();
                        ds.action = strField(ev, "action", null);
                        ds.elementId = strField(ev, "elementId", null);
                        ds.x = intField(ev, "x", 0);
                        ds.y = intField(ev, "y", 0);
                        f.dragStates.add(ds);
                    }
                    case "dev.tamboui.toolkit.global.handler" -> {
                        long routeId = longField(ev, "routeId", -1);
                        if (routeId < 0) break;
                        RouteFrame f = byRoute.computeIfAbsent(routeId, RouteFrame::new);
                        GlobalHandler gh = new GlobalHandler();
                        gh.index = intField(ev, "index", -1);
                        gh.result = strField(ev, "result", null);
                        f.globalHandlers.add(gh);
                    }
                }
            }
        }

        trace.routes = new ArrayList<>(byRoute.values());
        trace.routes.sort(Comparator.comparingLong(r -> r.routeId));
        trace.draws = draws;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(out, gson.toJson(trace));
        System.out.println("Wrote " + out + " with " + trace.routes.size() + " route(s), " + trace.draws.size() + " draw sample(s)");
    }

    private static long longField(RecordedEvent ev, String field, long def) { try { return ev.getLong(field); } catch (Throwable t) { return def; } }
    private static int intField(RecordedEvent ev, String field, int def) { try { return ev.getInt(field); } catch (Throwable t) { return def; } }
    private static boolean boolField(RecordedEvent ev, String field, boolean def) { try { return ev.getBoolean(field); } catch (Throwable t) { return def; } }
    private static String strField(RecordedEvent ev, String field, String def) { try { Object v = ev.getValue(field); return v != null ? String.valueOf(v) : def; } catch (Throwable t) { return def; } }

    static class Trace { Meta meta; List<RouteFrame> routes = List.of(); List<DrawSample> draws = List.of(); }
    record Meta(String app, String version, String source, String generatedAt) {}
    static class DrawSample { long index; long tsNanos; long durationNanos; }
    static class RouteFrame {
        long routeId; long tsNanos; long durationNanos; String event; String focusedId; int elementCount; String result;
        List<Candidate> candidates = new ArrayList<>();
        List<FocusChange> focusChanges = new ArrayList<>();
        List<FocusNavigation> focusNavigations = new ArrayList<>();
        List<DragState> dragStates = new ArrayList<>();
        List<GlobalHandler> globalHandlers = new ArrayList<>();
        RouteFrame(long routeId) { this.routeId = routeId; }
    }
    static class Candidate { String elementId; String elementType; String phase; String decision; String reason; }
    static class FocusChange { String fromId; String toId; String reason; }
    static class FocusNavigation { String action; boolean success; String fromId; String toId; }
    static class DragState { String action; String elementId; int x; int y; }
    static class GlobalHandler { int index; String result; }
}
