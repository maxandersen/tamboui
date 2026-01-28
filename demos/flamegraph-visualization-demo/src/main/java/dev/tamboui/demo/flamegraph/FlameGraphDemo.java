//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//FILES flamegraph-sample.folded=../../../../../resources/flamegraph-sample.folded
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.flamegraph;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widget.Widget;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo application that renders collapsed flamegraph stacks in a TUI.
 */
public class FlameGraphDemo {
    private static final int HEADER_HEIGHT = 4;
    private static final int FOOTER_HEIGHT = 3;
    private static final int DETAILS_WIDTH = 38;

    /**
     * Entry point for the flamegraph visualization demo.
     *
     * @param args command line arguments
     * @throws Exception if the TUI fails to start
     */
    public static void main(String[] args) throws Exception {
        ParsedArgs parsed = ParsedArgs.parse(args);
        if (parsed.error() != null) {
            System.err.println(parsed.error());
            printUsage();
            return;
        }
        if (parsed.showHelp()) {
            printUsage();
            return;
        }

        LoadedData loaded = FlameGraphLoader.load(parsed.inputPath());
        FlameGraphState state = new FlameGraphState(
            loaded.data(),
            loaded.sourceLabel(),
            loaded.statusMessage(),
            loaded.statusError()
        );

        TuiConfig config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(200))
            .build();

        try (ToolkitRunner runner = ToolkitRunner.create(config)) {
            runner.run(() -> render(state));
        }
    }

    private static Element render(FlameGraphState state) {
        Element header = panel(() -> column(
            row(
                text(" Flamegraph Viewer ").bold().cyan(),
                spacer(),
                text(state.sourceLabel()).dim().ellipsis()
            ).length(1),
            statusLine(state)
        ))
            .rounded()
            .borderColor(Color.DARK_GRAY)
            .length(HEADER_HEIGHT);

        Element flameGraphPanel = panel(() -> widget(state.widget()))
            .title("Flamegraph")
            .rounded()
            .borderColor(Color.BLUE)
            .fill();

        Element detailsPanel = panel(() -> buildDetails(state))
            .title("Details")
            .rounded()
            .borderColor(Color.GREEN)
            .length(DETAILS_WIDTH);

        Element main = row(flameGraphPanel, detailsPanel)
            .spacing(1)
            .fill();

        Element footer = panel(() -> text(
            "Arrows: navigate  Enter/Space: zoom  Backspace/Esc: up  R: reset  q: quit"
        ).dim().ellipsis())
            .rounded()
            .borderColor(Color.DARK_GRAY)
            .length(FOOTER_HEIGHT);

        return column(header, main, footer)
            .spacing(1)
            .focusable()
            .onKeyEvent(event -> handleKeyEvent(event, state));
    }

    private static Element statusLine(FlameGraphState state) {
        var line = text(state.statusMessage()).ellipsis();
        if (state.statusError()) {
            return line.red();
        }
        return line.dim();
    }

    private static Element buildDetails(FlameGraphState state) {
        FlameGraphNode selected = state.selected();
        FlameGraphNode viewRoot = state.viewRoot();
        long viewTotal = viewRoot.totalSamples();
        long allTotal = state.totalSamples();
        int relativeDepth = state.relativeDepth(selected);
        int maxDepth = viewRoot.maxDepth();

        return column(
            text("Selected").bold().yellow(),
            detailLine("Frame: " + selected.name()),
            detailLine("Samples: " + formatCount(selected.totalSamples())
                + " (" + formatPercent(selected.totalSamples(), viewTotal) + " view, "
                + formatPercent(selected.totalSamples(), allTotal) + " total)"),
            detailLine("Self: " + formatCount(selected.selfSamples())),
            detailLine("Children: " + selected.childrenCount()),
            detailLine("Depth: " + relativeDepth + "/" + maxDepth),
            spacer(1),
            text("View").bold().yellow(),
            detailLine("Root: " + viewRoot.name()),
            detailLine("Total samples: " + formatCount(allTotal))
        ).spacing(0);
    }

    private static Element detailLine(String value) {
        return text(value).ellipsis();
    }

    private static EventResult handleKeyEvent(KeyEvent event, FlameGraphState state) {
        if (event.isUp()) {
            state.selectParent();
            return EventResult.HANDLED;
        }
        if (event.isDown()) {
            state.selectFirstChild();
            return EventResult.HANDLED;
        }
        if (event.isLeft()) {
            state.selectPreviousSibling();
            return EventResult.HANDLED;
        }
        if (event.isRight()) {
            state.selectNextSibling();
            return EventResult.HANDLED;
        }
        if (event.isSelect()) {
            state.zoomToSelected();
            return EventResult.HANDLED;
        }
        if (event.isDeleteBackward() || event.isCancel()) {
            state.zoomOut();
            return EventResult.HANDLED;
        }
        if (event.isCharIgnoreCase('r')) {
            state.resetView();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private static void printUsage() {
        System.out.println("Flamegraph Visualization Demo");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("  flamegraph-visualization-demo [--file path]");
        System.out.println("");
        System.out.println("Options:");
        System.out.println("  -f, --file   Path to collapsed flamegraph file (.folded, .collapsed, .gz)");
        System.out.println("  -h, --help   Show this help text");
        System.out.println("");
        System.out.println("If no file is provided, the demo loads a bundled sample.");
    }

    private static String formatPercent(long part, long total) {
        if (total <= 0) {
            return "0%";
        }
        double percent = (double) part * 100.0 / (double) total;
        return String.format(Locale.ROOT, "%.2f%%", percent);
    }

    private static String formatCount(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}

final class ParsedArgs {
    private final Path inputPath;
    private final boolean showHelp;
    private final String error;

    private ParsedArgs(Path inputPath, boolean showHelp, String error) {
        this.inputPath = inputPath;
        this.showHelp = showHelp;
        this.error = error;
    }

    static ParsedArgs parse(String[] args) {
        Path path = null;
        boolean help = false;
        String error = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-h".equals(arg) || "--help".equals(arg)) {
                help = true;
            } else if ("-f".equals(arg) || "--file".equals(arg)) {
                if (i + 1 >= args.length) {
                    error = "Missing value for " + arg;
                    break;
                }
                path = Path.of(args[++i]);
            } else if (arg.startsWith("-")) {
                error = "Unknown option: " + arg;
                break;
            } else if (path == null) {
                path = Path.of(arg);
            }
        }

        return new ParsedArgs(path, help, error);
    }

    Path inputPath() {
        return inputPath;
    }

    boolean showHelp() {
        return showHelp;
    }

    String error() {
        return error;
    }
}

final class LoadedData {
    private final FlameGraphData data;
    private final String sourceLabel;
    private final String statusMessage;
    private final boolean statusError;

    LoadedData(FlameGraphData data, String sourceLabel, String statusMessage, boolean statusError) {
        this.data = data;
        this.sourceLabel = sourceLabel;
        this.statusMessage = statusMessage;
        this.statusError = statusError;
    }

    FlameGraphData data() {
        return data;
    }

    String sourceLabel() {
        return sourceLabel;
    }

    String statusMessage() {
        return statusMessage;
    }

    boolean statusError() {
        return statusError;
    }
}

final class FlameGraphLoader {
    private static final String SAMPLE_RESOURCE = "/flamegraph-sample.folded";

    private FlameGraphLoader() {
    }

    static LoadedData load(Path inputPath) {
        if (inputPath != null) {
            Path resolved = inputPath.toAbsolutePath().normalize();
            if (!Files.exists(resolved)) {
                return loadSample("File not found: " + resolved + " (showing sample)", true);
            }
            if (Files.isDirectory(resolved)) {
                return loadSample("Path is a directory: " + resolved + " (showing sample)", true);
            }
            try (InputStream stream = openStream(resolved)) {
                ParseResult result = FlameGraphParser.parse(stream);
                String message = "Loaded " + resolved + " (" + result.data().totalSamples() + " samples)";
                if (result.skippedLines() > 0) {
                    message += ", skipped " + result.skippedLines() + " lines";
                }
                return new LoadedData(result.data(), resolved.toString(), message, false);
            } catch (IOException e) {
                return loadSample("Failed to read " + resolved + ": " + e.getMessage(), true);
            }
        }
        return loadSample("Loaded bundled sample flamegraph", false);
    }

    private static LoadedData loadSample(String message, boolean error) {
        InputStream stream = FlameGraphLoader.class.getResourceAsStream(SAMPLE_RESOURCE);
        if (stream == null) {
            FlameGraphNode root = new FlameGraphNode("All samples", null);
            FlameGraphData data = new FlameGraphData(root, 0);
            return new LoadedData(data, "Sample data missing", message, true);
        }
        try (InputStream input = stream) {
            ParseResult result = FlameGraphParser.parse(input);
            return new LoadedData(result.data(), "Sample data", message, error);
        } catch (IOException e) {
            FlameGraphNode root = new FlameGraphNode("All samples", null);
            FlameGraphData data = new FlameGraphData(root, 0);
            return new LoadedData(data, "Sample data error", "Failed to load sample: " + e.getMessage(), true);
        }
    }

    private static InputStream openStream(Path path) throws IOException {
        InputStream base = Files.newInputStream(path);
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".gz") || name.endsWith(".gzip")) {
            return new GZIPInputStream(base);
        }
        return base;
    }
}

final class ParseResult {
    private final FlameGraphData data;
    private final int skippedLines;

    ParseResult(FlameGraphData data, int skippedLines) {
        this.data = data;
        this.skippedLines = skippedLines;
    }

    FlameGraphData data() {
        return data;
    }

    int skippedLines() {
        return skippedLines;
    }
}

final class FlameGraphParser {
    private FlameGraphParser() {
    }

    static ParseResult parse(InputStream inputStream) throws IOException {
        FlameGraphNode root = new FlameGraphNode("All samples", null);
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int lastSpace = trimmed.lastIndexOf(' ');
                if (lastSpace < 0) {
                    skipped++;
                    continue;
                }
                String stackPart = trimmed.substring(0, lastSpace).trim();
                String countPart = trimmed.substring(lastSpace + 1).trim();
                if (stackPart.isEmpty() || countPart.isEmpty()) {
                    skipped++;
                    continue;
                }
                long samples;
                try {
                    samples = Long.parseLong(countPart);
                } catch (NumberFormatException e) {
                    skipped++;
                    continue;
                }
                if (samples <= 0) {
                    continue;
                }

                FlameGraphNode current = root;
                current.addSamples(samples);
                String[] frames = stackPart.split(";");
                for (String frame : frames) {
                    String name = frame.trim();
                    if (name.isEmpty()) {
                        continue;
                    }
                    current = current.child(name);
                    current.addSamples(samples);
                }
                current.addSelfSamples(samples);
            }
        }

        int maxDepth = computeMaxDepth(root);
        return new ParseResult(new FlameGraphData(root, maxDepth), skipped);
    }

    private static int computeMaxDepth(FlameGraphNode node) {
        int max = 0;
        for (FlameGraphNode child : node.children()) {
            max = Math.max(max, 1 + computeMaxDepth(child));
        }
        node.setMaxDepth(max);
        return max;
    }
}

final class FlameGraphData {
    private final FlameGraphNode root;
    private final int maxDepth;

    FlameGraphData(FlameGraphNode root, int maxDepth) {
        this.root = root;
        this.maxDepth = maxDepth;
    }

    FlameGraphNode root() {
        return root;
    }

    long totalSamples() {
        return root.totalSamples();
    }

    int maxDepth() {
        return maxDepth;
    }
}

final class FlameGraphNode {
    private final String name;
    private final FlameGraphNode parent;
    private final Map<String, FlameGraphNode> children = new LinkedHashMap<>();
    private final int depth;
    private long totalSamples;
    private long selfSamples;
    private int maxDepth;

    FlameGraphNode(String name, FlameGraphNode parent) {
        this.name = name;
        this.parent = parent;
        this.depth = parent != null ? parent.depth + 1 : 0;
    }

    String name() {
        return name;
    }

    FlameGraphNode parent() {
        return parent;
    }

    int depth() {
        return depth;
    }

    long totalSamples() {
        return totalSamples;
    }

    long selfSamples() {
        return selfSamples;
    }

    int maxDepth() {
        return maxDepth;
    }

    int childrenCount() {
        return children.size();
    }

    List<FlameGraphNode> children() {
        return new ArrayList<>(children.values());
    }

    FlameGraphNode child(String frame) {
        FlameGraphNode existing = children.get(frame);
        if (existing != null) {
            return existing;
        }
        FlameGraphNode created = new FlameGraphNode(frame, this);
        children.put(frame, created);
        return created;
    }

    void addSamples(long samples) {
        totalSamples += samples;
    }

    void addSelfSamples(long samples) {
        selfSamples += samples;
    }

    void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }
}

final class FlameGraphState {
    private final FlameGraphData data;
    private final String sourceLabel;
    private final String statusMessage;
    private final boolean statusError;
    private final FlameGraphWidget widget;
    private FlameGraphNode viewRoot;
    private FlameGraphNode selected;
    private int depthOffset;
    private int viewportHeight = 1;

    FlameGraphState(FlameGraphData data, String sourceLabel, String statusMessage, boolean statusError) {
        this.data = data;
        this.sourceLabel = sourceLabel;
        this.statusMessage = statusMessage;
        this.statusError = statusError;
        this.viewRoot = data.root();
        this.selected = this.viewRoot;
        this.widget = new FlameGraphWidget(this);
    }

    FlameGraphData data() {
        return data;
    }

    String sourceLabel() {
        return sourceLabel;
    }

    String statusMessage() {
        return statusMessage;
    }

    boolean statusError() {
        return statusError;
    }

    FlameGraphNode viewRoot() {
        return viewRoot;
    }

    FlameGraphNode selected() {
        return selected;
    }

    FlameGraphWidget widget() {
        return widget;
    }

    int depthOffset() {
        return depthOffset;
    }

    long totalSamples() {
        return data.totalSamples();
    }

    int relativeDepth(FlameGraphNode node) {
        return node.depth() - viewRoot.depth();
    }

    void setViewportHeight(int height) {
        if (height <= 0) {
            viewportHeight = 1;
        } else {
            viewportHeight = height;
        }
        ensureSelectedVisible();
    }

    void selectParent() {
        if (selected == viewRoot) {
            return;
        }
        FlameGraphNode parent = selected.parent();
        if (parent != null) {
            selected = parent;
            ensureSelectedVisible();
        }
    }

    void selectFirstChild() {
        List<FlameGraphNode> children = selected.children();
        if (!children.isEmpty()) {
            selected = children.get(0);
            ensureSelectedVisible();
        }
    }

    void selectPreviousSibling() {
        FlameGraphNode parent = selected.parent();
        if (parent == null || selected == viewRoot) {
            return;
        }
        List<FlameGraphNode> siblings = parent.children();
        int index = indexOf(siblings, selected);
        if (index > 0) {
            selected = siblings.get(index - 1);
            ensureSelectedVisible();
        }
    }

    void selectNextSibling() {
        FlameGraphNode parent = selected.parent();
        if (parent == null || selected == viewRoot) {
            return;
        }
        List<FlameGraphNode> siblings = parent.children();
        int index = indexOf(siblings, selected);
        if (index >= 0 && index + 1 < siblings.size()) {
            selected = siblings.get(index + 1);
            ensureSelectedVisible();
        }
    }

    void zoomToSelected() {
        viewRoot = selected;
        depthOffset = 0;
        selected = viewRoot;
    }

    void zoomOut() {
        FlameGraphNode parent = viewRoot.parent();
        if (parent != null) {
            viewRoot = parent;
            selected = viewRoot;
            depthOffset = 0;
        }
    }

    void resetView() {
        viewRoot = data.root();
        selected = viewRoot;
        depthOffset = 0;
    }

    private void ensureSelectedVisible() {
        int relativeDepth = relativeDepth(selected);
        int maxOffset = maxDepthOffset();
        if (relativeDepth < depthOffset) {
            depthOffset = relativeDepth;
        } else if (relativeDepth > depthOffset + viewportHeight - 1) {
            depthOffset = relativeDepth - (viewportHeight - 1);
        }
        if (depthOffset < 0) {
            depthOffset = 0;
        }
        if (depthOffset > maxOffset) {
            depthOffset = maxOffset;
        }
    }

    private void clampDepthOffset() {
        int maxOffset = maxDepthOffset();
        if (depthOffset > maxOffset) {
            depthOffset = maxOffset;
        }
        if (depthOffset < 0) {
            depthOffset = 0;
        }
    }

    private int maxDepthOffset() {
        int maxDepth = viewRoot.maxDepth();
        int viewport = Math.max(1, viewportHeight);
        return Math.max(0, maxDepth - (viewport - 1));
    }

    private int indexOf(List<FlameGraphNode> nodes, FlameGraphNode target) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) == target) {
                return i;
            }
        }
        return -1;
    }
}

final class FlameGraphWidget implements Widget {
    private static final int COLOR_CUBE_SIZE = 216;
    private static final int COLOR_CUBE_OFFSET = 16;
    private static final int HIGHLIGHT_DELTA = 30;

    private final FlameGraphState state;
    private final Map<Integer, String> spaceCache = new HashMap<>();

    FlameGraphWidget(FlameGraphState state) {
        this.state = state;
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }
        state.setViewportHeight(area.height());
        clearArea(area, buffer);
        renderNode(buffer, area, state.viewRoot(), 0, 0, area.width());
    }

    private void renderNode(Buffer buffer, Rect area, FlameGraphNode node, int depth, int startX, int width) {
        if (width <= 0) {
            return;
        }
        int depthOffset = state.depthOffset();
        int maxVisibleDepth = depthOffset + area.height() - 1;
        if (depth >= depthOffset && depth <= maxVisibleDepth) {
            int y = area.y() + area.height() - 1 - (depth - depthOffset);
            renderFrame(buffer, area.x() + startX, y, width, node, node == state.selected());
        }

        if (depth >= maxVisibleDepth) {
            return;
        }
        List<FlameGraphNode> children = node.children();
        if (children.isEmpty()) {
            return;
        }

        int[] widths = computeChildWidths(children, width, node.totalSamples());
        int childX = startX;
        for (int i = 0; i < children.size(); i++) {
            int childWidth = widths[i];
            if (childWidth > 0) {
                renderNode(buffer, area, children.get(i), depth + 1, childX, childWidth);
            }
            childX += childWidth;
        }
    }

    private void renderFrame(Buffer buffer, int x, int y, int width, FlameGraphNode node, boolean selected) {
        Style style = styleFor(node, selected);
        buffer.setString(x, y, spaces(width), style);

        String label = node.name();
        if (!label.isEmpty() && width > 0) {
            int labelWidth = CharWidth.of(label);
            if (labelWidth > width) {
                label = CharWidth.truncateWithEllipsis(label, width, CharWidth.TruncatePosition.END);
            }
            buffer.setString(x, y, label, style);
        }
    }

    private void clearArea(Rect area, Buffer buffer) {
        String blank = spaces(area.width());
        for (int y = 0; y < area.height(); y++) {
            buffer.setString(area.x(), area.y() + y, blank, Style.EMPTY);
        }
    }

    private String spaces(int width) {
        if (width <= 0) {
            return "";
        }
        String cached = spaceCache.get(width);
        if (cached != null) {
            return cached;
        }
        String value = " ".repeat(width);
        spaceCache.put(width, value);
        return value;
    }

    private int[] computeChildWidths(List<FlameGraphNode> children, int width, long total) {
        int count = children.size();
        int[] widths = new int[count];
        if (count == 0 || width <= 0 || total <= 0) {
            return widths;
        }

        double scale = (double) width / (double) total;
        double[] remainders = new double[count];
        int used = 0;
        for (int i = 0; i < count; i++) {
            double exact = children.get(i).totalSamples() * scale;
            int base = (int) Math.floor(exact);
            widths[i] = base;
            remainders[i] = exact - base;
            used += base;
        }

        int remaining = width - used;
        if (remaining > 0) {
            Integer[] order = new Integer[count];
            for (int i = 0; i < count; i++) {
                order[i] = i;
            }
            Arrays.sort(order, Comparator.comparingDouble((Integer i) -> remainders[i]).reversed());
            for (int i = 0; i < remaining && i < order.length; i++) {
                widths[order[i]]++;
            }
        }

        return widths;
    }

    private Style styleFor(FlameGraphNode node, boolean selected) {
        Color base = node.parent() == null ? Color.DARK_GRAY : colorFor(node.name());
        if (selected) {
            base = highlight(base);
        }
        Color fg = foregroundFor(base);
        Style style = Style.EMPTY.bg(base).fg(fg);
        if (selected) {
            style = style.addModifier(Modifier.BOLD);
        }
        return style;
    }

    private Color colorFor(String name) {
        int index = COLOR_CUBE_OFFSET + Math.floorMod(name.hashCode(), COLOR_CUBE_SIZE);
        return Color.indexed(index);
    }

    private Color highlight(Color base) {
        Color.Rgb rgb = base.toRgb();
        int r = Math.min(255, rgb.r() + HIGHLIGHT_DELTA);
        int g = Math.min(255, rgb.g() + HIGHLIGHT_DELTA);
        int b = Math.min(255, rgb.b() + HIGHLIGHT_DELTA);
        return Color.rgb(r, g, b);
    }

    private Color foregroundFor(Color bg) {
        Color.Rgb rgb = bg.toRgb();
        double luminance = 0.2126 * rgb.r() + 0.7152 * rgb.g() + 0.0722 * rgb.b();
        if (luminance > 140) {
            return Color.BLACK;
        }
        return Color.BRIGHT_WHITE;
    }
}
