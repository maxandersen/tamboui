//REPOS mavenLocal,mavenCentral
//DEPS dev.tamboui:tamboui-toolkit:0.2.0-SNAPSHOT
//DEPS dev.tamboui:tamboui-aesh-backend:0.2.0-SNAPSHOT
//DEPS dev.tamboui:tamboui-demos:0.2.0-SNAPSHOT
//DEPS org.aesh:terminal-http:3.1
//DEPS io.netty:netty-all:4.1.81.Final
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.aesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.aesh.terminal.Connection;
import org.aesh.terminal.http.netty.NettyWebsocketTtyBootstrap;

import dev.tamboui.backend.aesh.AeshBackend;
import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Color;
import dev.tamboui.style.Overflow;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.ListElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Web-accessible demo chooser using Aesh HTTP/WebSocket backend.
 * <p>
 * Starts an HTTP/WebSocket server and presents a TUI demo chooser
 * to each connected client. When a demo is selected and it extends
 * {@link ToolkitApp}, it launches within the same web terminal session.
 * <p>
 * Connect via browser: Open {@code http://localhost:8080}
 */
public class AeshWebChooserDemo implements java.util.function.Consumer<Connection> {

    private static final String MANIFEST_PATH = "/demos-manifest.json";
    private static final int HTTP_PORT = 8080;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, DemoInfo> demos;

    /**
     * Creates a new AeshWebChooserDemo instance.
     *
     * @throws IOException if the demo manifest cannot be loaded
     */
    public AeshWebChooserDemo() throws IOException {
        this.demos = loadDemoManifest();
    }

    /**
     * Main entry point. Starts the HTTP server and waits for connections.
     *
     * @param args the command line arguments
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        var demo = new AeshWebChooserDemo();
        try {
            demo.start();
            System.out.println("TamboUI Web Demo Chooser started:");
            System.out.println("  HTTP: http://localhost:" + HTTP_PORT);
            System.out.println("  Demos available: " + demo.demos.size());
            System.out.println("\nPress Ctrl+C to stop...");
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("\nShutting down...");
        } finally {
            demo.stop();
        }
    }

    private void start() throws Exception {
        var bootstrap = new NettyWebsocketTtyBootstrap();
        bootstrap.setPort(HTTP_PORT);
        bootstrap.setHost("localhost");
        bootstrap.start(this).get(10, TimeUnit.SECONDS);
        System.out.println("HTTP server started on port " + HTTP_PORT);
    }

    @Override
    public void accept(Connection connection) {
        connection.setCloseHandler(close -> {
            // Connection closed
        });

        executor.submit(() -> {
            try {
                runChooserLoop(connection);
            } catch (Exception e) {
                System.err.println("Error running chooser: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    connection.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        });
    }

    /**
     * Runs the chooser loop for a connection. Shows the demo list,
     * launches selected demos, and returns to the chooser when they exit.
     */
    private void runChooserLoop(Connection connection) throws Exception {
        var backend = new AeshBackend(connection);
        try {
            while (true) {
                var chooserApp = new ChooserApp(demos);
                runToolkitApp(backend, chooserApp::render, chooserApp::init, chooserApp::onStop);

                var selected = chooserApp.selectedDemo;
                if (selected == null) {
                    break; // User quit without selecting
                }

                // Launch the selected demo
                try {
                    launchDemo(backend, selected);
                } catch (Exception e) {
                    System.err.println("Error launching demo " + selected.id() + ": " + e.getMessage());
                    e.printStackTrace();
                }
                // Loop back to chooser
            }
        } finally {
            backend.close();
        }
    }

    /**
     * Creates a TuiConfig using a non-closing wrapper around the given backend.
     * This allows the TuiRunner to clean up terminal state without closing
     * the underlying connection, so the backend can be reused.
     */
    private static TuiConfig createConfig(AeshBackend backend) {
        return TuiConfig.builder()
                .backend(new NonClosingBackend(backend))
                .rawMode(true)
                .alternateScreen(true)
                .hideCursor(true)
                .mouseCapture(true)
                .build();
    }

    /**
     * Runs a ToolkitApp-style render loop using the given backend.
     * The backend is NOT closed when the runner exits, allowing reuse.
     */
    private static void runToolkitApp(AeshBackend backend, Supplier<Element> render,
                                      java.util.function.Consumer<ToolkitRunner> onStart,
                                      Runnable onStop) throws Exception {
        try (var runner = ToolkitRunner.create(createConfig(backend))) {
            if (onStart != null) {
                onStart.accept(runner);
            }
            runner.run(render);
        } finally {
            if (onStop != null) {
                onStop.run();
            }
        }
    }

    /**
     * Launches a demo in the web terminal.
     * <p>
     * Sets a thread-local backend override via {@link BackendFactory#setThreadLocalBackend}
     * so that any demo calling {@code BackendFactory.create()}, {@code TuiRunner.create()},
     * or {@code ToolkitApp.run()} will use the Aesh backend connected to the web terminal.
     */
    private static void launchDemo(AeshBackend backend, DemoInfo demo) throws Exception {
        var clazz = Class.forName(demo.mainClass());

        BackendFactory.setThreadLocalBackend(new NonClosingBackend(backend));
        try {
            Method main;
            try {
                main = clazz.getMethod("main", String[].class);
            } catch (NoSuchMethodException e) {
                try {
                    main = clazz.getMethod("main");
                } catch (NoSuchMethodException e2) {
                    System.err.println("Cannot launch " + demo.id() + ": no main method found");
                    return;
                }
            }

            main.setAccessible(true);
            Object instance = null;
            if (!Modifier.isStatic(main.getModifiers())) {
                var constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                instance = constructor.newInstance();
            }

            if (main.getParameterCount() == 0) {
                main.invoke(instance);
            } else {
                main.invoke(instance, (Object) new String[0]);
            }
        } finally {
            BackendFactory.clearThreadLocalBackend();
        }
    }

    private void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    // ==================== Demo Chooser App ====================

    /**
     * ToolkitApp-style chooser that presents a list of demos.
     */
    private static class ChooserApp {

        private final Map<String, List<DemoInfo>> demosByModule;
        private final List<DemoInfo> allDemos;
        private final ListElement<?> demoList;
        private String filter = "";
        private List<DisplayItem> displayItems = new ArrayList<>();
        DemoInfo selectedDemo;

        ChooserApp(Map<String, DemoInfo> demos) {
            this.allDemos = demos.values().stream()
                    .sorted(Comparator.comparing(DemoInfo::module).thenComparing(DemoInfo::displayName))
                    .collect(Collectors.toList());

            this.demosByModule = new TreeMap<>();
            for (var demo : allDemos) {
                demosByModule.computeIfAbsent(demo.module(), k -> new ArrayList<>()).add(demo);
            }

            this.demoList = list()
                    .highlightSymbol("> ")
                    .highlightColor(Color.YELLOW)
                    .autoScroll()
                    .scrollbar()
                    .scrollbarThumbColor(Color.CYAN);
        }

        private ToolkitRunner runner;

        void init(ToolkitRunner runner) {
            this.runner = runner;
            rebuildDisplayList();
            if (!displayItems.isEmpty()) {
                demoList.selected(0);
            }
        }

        void onStop() {
            // Nothing to clean up
        }

        private void rebuildDisplayList() {
            displayItems = new ArrayList<>();
            var lowerFilter = filter.toLowerCase(Locale.ROOT);

            for (var entry : demosByModule.entrySet()) {
                var module = entry.getKey();
                var demos = entry.getValue();

                var filteredDemos = filter.isEmpty() ? demos : demos.stream()
                        .filter(d -> d.displayName().toLowerCase(Locale.ROOT).contains(lowerFilter)
                                || d.id().toLowerCase(Locale.ROOT).contains(lowerFilter)
                                || d.description().toLowerCase(Locale.ROOT).contains(lowerFilter)
                                || d.tags().stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(lowerFilter)))
                        .collect(Collectors.toList());

                if (filteredDemos.isEmpty() && !filter.isEmpty()) {
                    continue;
                }

                // Module header
                displayItems.add(new DisplayItem(module, null, filteredDemos.size()));

                // Demos
                for (var demo : filteredDemos) {
                    displayItems.add(new DisplayItem(module, demo, 0));
                }
            }
        }

        private DisplayItem selectedItem() {
            var idx = demoList.selected();
            if (idx >= 0 && idx < displayItems.size()) {
                return displayItems.get(idx);
            }
            return null;
        }

        Element render() {
            List<String> lines = new ArrayList<>();
            for (var item : displayItems) {
                lines.add(item.toDisplayString());
            }

            var title = filter.isEmpty()
                    ? "Demos (" + allDemos.size() + ")"
                    : "Filter: " + filter;

            var selected = selectedItem();

            // Description panel
            Element descriptionContent;
            if (selected != null && selected.demo() != null) {
                var demo = selected.demo();
                var tags = demo.tags();
                var tagsLine = tags.isEmpty() ? "" : "Tags: " + String.join(", ", tags);
                descriptionContent = column(
                        text(tagsLine).magenta().overflow(Overflow.WRAP_WORD),
                        text(""),
                        text(demo.description()).overflow(Overflow.WRAP_WORD)
                );
            } else if (selected != null) {
                var count = demosByModule.getOrDefault(selected.module(), Collections.emptyList()).size();
                descriptionContent = column(
                        text(selected.module()).bold().cyan().length(1),
                        text(""),
                        text(count + " demo" + (count != 1 ? "s" : "")).length(1)
                );
            } else {
                descriptionContent = text("");
            }

            return dock()
                    .top(panel(
                            text(" TamboUI Web Demo Chooser ").bold().cyan()
                    ).rounded().borderColor(Color.CYAN))

                    .left(panel(
                            demoList.items(lines)
                    )
                            .title(title)
                            .rounded()
                            .borderColor(filter.isEmpty() ? Color.WHITE : Color.YELLOW)
                            .id("demo-list")
                            .focusable()
                            .onKeyEvent(this::handleKey), Constraint.percentage(55))

                    .center(panel(descriptionContent)
                            .title("Description")
                            .rounded()
                            .borderColor(Color.DARK_GRAY))

                    .bottom(panel(
                            text(" Type: Filter | ↑↓: Navigate | Enter: Launch | Backspace: Clear | Ctrl+C: Quit ").dim()
                    ).rounded().borderColor(Color.DARK_GRAY));
        }

        private EventResult handleKey(KeyEvent event) {
            // Select demo
            if (event.isConfirm()) {
                var selected = selectedItem();
                if (selected != null && selected.demo() != null) {
                    selectedDemo = selected.demo();
                    runner.quit();
                }
                return EventResult.HANDLED;
            }

            // Clear filter
            if (event.isCancel() && !filter.isEmpty()) {
                filter = "";
                rebuildDisplayList();
                demoList.selected(0);
                return EventResult.HANDLED;
            }

            // Backspace
            if (event.code() == KeyCode.BACKSPACE && !filter.isEmpty()) {
                filter = filter.substring(0, filter.length() - 1);
                rebuildDisplayList();
                demoList.selected(0);
                return EventResult.HANDLED;
            }

            // Type to filter
            if (event.code() == KeyCode.CHAR && !event.hasCtrl() && !event.hasAlt()) {
                var c = event.character();
                if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ' ') {
                    filter += c;
                    rebuildDisplayList();
                    demoList.selected(0);
                    return EventResult.HANDLED;
                }
            }

            return EventResult.UNHANDLED;
        }
    }

    // ==================== Display Item ====================

    private record DisplayItem(String module, DemoInfo demo, int demoCount) {
        String toDisplayString() {
            if (demo == null) {
                return "▼ " + module + " (" + demoCount + ")";
            }
            return "    " + demo.displayName();
        }
    }

    // ==================== Demo Info ====================

    private record DemoInfo(String id, String displayName, String description,
                            String module, String mainClass, Set<String> tags) {
    }

    // ==================== Manifest Loading ====================

    private static Map<String, DemoInfo> loadDemoManifest() throws IOException {
        try (var is = AeshWebChooserDemo.class.getResourceAsStream(MANIFEST_PATH)) {
            if (is == null) {
                throw new IOException("Demo manifest not found: " + MANIFEST_PATH
                        + ". Build tamboui-demos first.");
            }
            return parseManifest(is);
        }
    }

    private static Map<String, DemoInfo> parseManifest(InputStream is) throws IOException {
        Map<String, DemoInfo> demos = new LinkedHashMap<>();

        try (var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            var content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            var json = content.toString();
            int idx = 0;
            while ((idx = json.indexOf("{", idx + 1)) != -1) {
                int end = json.indexOf("}", idx);
                if (end == -1) {
                    break;
                }

                var obj = json.substring(idx, end + 1);
                if (obj.contains("\"mainClass\"")) {
                    var id = extractField(obj, "id");
                    var displayName = extractField(obj, "displayName");
                    var description = extractField(obj, "description");
                    var module = extractField(obj, "module");
                    var mainClass = extractField(obj, "mainClass");
                    Set<String> tags = extractTags(obj);

                    if (id != null && mainClass != null) {
                        demos.put(id, new DemoInfo(id,
                                displayName != null ? displayName : id,
                                description != null ? description : "",
                                module != null ? module : "Other",
                                mainClass, tags));
                    }
                }
                idx = end;
            }
        }
        return demos;
    }

    private static Set<String> extractTags(String json) {
        var tags = extractField(json, "tags");
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static String extractField(String json, String field) {
        var pattern = "\"" + field + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) {
            return null;
        }
        idx = json.indexOf(":", idx);
        if (idx == -1) {
            return null;
        }
        idx = json.indexOf("\"", idx);
        if (idx == -1) {
            return null;
        }
        int start = idx + 1;
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '\\' && end + 1 < json.length()) {
                end += 2;
            } else if (c == '"') {
                break;
            } else {
                end++;
            }
        }
        return json.substring(start, end)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    // ==================== Non-Closing Backend Wrapper ====================

    /**
     * A delegating backend wrapper that ignores {@link #close()} calls.
     * This allows {@link dev.tamboui.tui.TuiRunner} to clean up terminal state
     * (alternate screen, cursor, etc.) without closing the underlying connection,
     * so the backend can be reused for the next demo.
     */
    private static class NonClosingBackend implements Backend {

        private final Backend delegate;

        NonClosingBackend(Backend delegate) {
            this.delegate = delegate;
        }

        @Override
        public void draw(DiffResult diff) throws IOException {
            delegate.draw(diff);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void clear() throws IOException {
            delegate.clear();
        }

        @Override
        public Size size() throws IOException {
            return delegate.size();
        }

        @Override
        public void showCursor() throws IOException {
            delegate.showCursor();
        }

        @Override
        public void hideCursor() throws IOException {
            delegate.hideCursor();
        }

        @Override
        public Position getCursorPosition() throws IOException {
            return delegate.getCursorPosition();
        }

        @Override
        public void setCursorPosition(Position position) throws IOException {
            delegate.setCursorPosition(position);
        }

        @Override
        public void enterAlternateScreen() throws IOException {
            delegate.enterAlternateScreen();
        }

        @Override
        public void leaveAlternateScreen() throws IOException {
            delegate.leaveAlternateScreen();
        }

        @Override
        public void enableRawMode() throws IOException {
            delegate.enableRawMode();
        }

        @Override
        public void disableRawMode() throws IOException {
            delegate.disableRawMode();
        }

        @Override
        public void enableMouseCapture() throws IOException {
            delegate.enableMouseCapture();
        }

        @Override
        public void disableMouseCapture() throws IOException {
            delegate.disableMouseCapture();
        }

        @Override
        public void scrollUp(int lines) throws IOException {
            delegate.scrollUp(lines);
        }

        @Override
        public void scrollDown(int lines) throws IOException {
            delegate.scrollDown(lines);
        }

        @Override
        public void writeRaw(byte[] data) throws IOException {
            delegate.writeRaw(data);
        }

        @Override
        public void writeRaw(String data) throws IOException {
            delegate.writeRaw(data);
        }

        @Override
        public void writeRaw(CharSequence data) throws IOException {
            delegate.writeRaw(data);
        }

        @Override
        public void onResize(Runnable handler) {
            delegate.onResize(handler);
        }

        @Override
        public int read(int timeoutMs) throws IOException {
            return delegate.read(timeoutMs);
        }

        @Override
        public int peek(int timeoutMs) throws IOException {
            return delegate.peek(timeoutMs);
        }

        @Override
        public void insertLines(int n) throws IOException {
            delegate.insertLines(n);
        }

        @Override
        public void deleteLines(int n) throws IOException {
            delegate.deleteLines(n);
        }

        @Override
        public void moveCursorUp(int n) throws IOException {
            delegate.moveCursorUp(n);
        }

        @Override
        public void moveCursorDown(int n) throws IOException {
            delegate.moveCursorDown(n);
        }

        @Override
        public void moveCursorRight(int n) throws IOException {
            delegate.moveCursorRight(n);
        }

        @Override
        public void moveCursorLeft(int n) throws IOException {
            delegate.moveCursorLeft(n);
        }

        @Override
        public void eraseToEndOfLine() throws IOException {
            delegate.eraseToEndOfLine();
        }

        @Override
        public void carriageReturn() throws IOException {
            delegate.carriageReturn();
        }

        @Override
        public void close() throws IOException {
            // Intentionally do nothing - the real backend is closed
            // by runChooserLoop when the session ends.
        }
    }
}
