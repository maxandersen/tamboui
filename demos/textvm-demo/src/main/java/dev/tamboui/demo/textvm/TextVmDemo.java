//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-css:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//SOURCES TextVmFormatting.java
//FILES styles/textvm.tcss=../../../../../resources/styles/textvm.tcss
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.textvm;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.management.OperatingSystemMXBean;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.elements.ListElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.widgets.common.ScrollBarPolicy;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.event.KeyEvent;

import dev.tamboui.widgets.chart.Axis;
import dev.tamboui.widgets.chart.Dataset;
import dev.tamboui.widgets.chart.GraphType;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * TextVM is a VisualVM-inspired terminal dashboard for inspecting running Java
 * processes.
 *
 * <p>It adapts common VisualVM views (overview, monitor, threads, GC, and
 * properties) into a high-level TamboUI toolkit interface with CSS styling.</p>
 */
public final class TextVmDemo {

    private static final Duration PROCESS_REFRESH_INTERVAL = Duration.ofSeconds(5);
    private static final Duration METRICS_REFRESH_INTERVAL = Duration.ofSeconds(1);
    private static final Duration ATTACH_RETRY_INTERVAL = Duration.ofSeconds(5);
    private static final int HISTORY_SIZE = 60;
    private static final int MAX_THREADS = 80;
    private static final int PROCESS_PANEL_WIDTH = 38;
    private static final int MAX_ERROR_LOG_ENTRIES = 200;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> TAB_TITLES = List.of("Overview", "Monitor", "Threads", "GC", "Properties");

    private final List<JavaProcess> processes = new ArrayList<>();
    private volatile Map<Long, JavaProcess> processByPid = Map.of();
    private final AtomicLong selectedPid = new AtomicLong(-1);
    private int selectedProcessIndex;
    private int selectedTabIndex;

    private MetricsSnapshot metrics = MetricsSnapshot.empty();
    private List<ThreadSnapshot> threads = new ArrayList<>();
    private List<GcSnapshot> garbageCollectors = new ArrayList<>();
    private List<Map.Entry<String, String>> properties = new ArrayList<>();

    private final MetricSeries cpuSeries = new MetricSeries(HISTORY_SIZE);
    private final MetricSeries heapSeries = new MetricSeries(HISTORY_SIZE);
    private final MetricSeries threadSeries = new MetricSeries(HISTORY_SIZE);
    private final MetricSeries classSeries = new MetricSeries(HISTORY_SIZE);
    private final MetricSeries gcSeries = new MetricSeries(HISTORY_SIZE);

    private final MetricsCollector metricsCollector = new MetricsCollector();
    private ToolkitRunner runner;
    private String statusMessage = "Ready";
    private final Deque<ErrorEntry> errorLog = new ArrayDeque<>();
    private boolean showErrorPopup;
    private boolean showThreadDetail;
    private int selectedThreadIndex;

    private record ErrorEntry(String timestamp, String message, List<String> stackTraceLines) {
    }

    private static Element withFill(Element element) {
        if (element instanceof StyledElement) {
            ((StyledElement<?>) element).fill();
        }
        return element;
    }

    private static Element withLength(Element element, int length) {
        if (element instanceof StyledElement) {
            ((StyledElement<?>) element).length(length);
        }
        return element;
    }

    private TextVmDemo() {
    }

    /**
     * Entry point for the TextVM demo.
     *
     * @param args command line arguments
     * @throws Exception if the demo fails to start
     */
    public static void main(String[] args) throws Exception {
        new TextVmDemo().run();
    }

    private void run() throws Exception {
        var styleEngine = createStyleEngine();
        var config = TuiConfig.builder()
                .mouseCapture(true)
                .tickRate(Duration.ofMillis(250))
                .build();

        try (var runner = ToolkitRunner.builder().config(config).styleEngine(styleEngine).build()) {
            this.runner = runner;
            runner.eventRouter().addGlobalHandler(event -> {
                if (event instanceof KeyEvent ke) {
                    return handleKey(ke);
                }
                return EventResult.UNHANDLED;
            });
            updateProcessList(ProcessCollector.collect());
            startBackgroundTasks();
            runner.run(this::render);
        } finally {
            metricsCollector.close();
        }
    }

    private StyleEngine createStyleEngine() throws IOException {
        var engine = StyleEngine.create();
        engine.loadStylesheet("/styles/textvm.tcss");
        return engine;
    }

    private void startBackgroundTasks() {
        runner.scheduleRepeating(() -> {
            try {
                var updated = ProcessCollector.collect();
                runner.runOnRenderThread(() -> updateProcessList(updated));
            } catch (Exception e) {
                runner.runOnRenderThread(() -> logError("Process scan failed", e));
            }
        }, PROCESS_REFRESH_INTERVAL);

        runner.scheduleRepeating(() -> {
            long pid = selectedPid.get();
            JavaProcess process = processByPid.get(pid);
            MetricsSnapshot snapshot = metricsCollector.collect(process);
            runner.runOnRenderThread(() -> applySnapshot(snapshot));
        }, METRICS_REFRESH_INTERVAL);
    }

    private void logError(String context, Exception e) {
        String message = context + ": " + safeMessage(e);
        String timestamp = TIME_FORMAT.format(Instant.now().atZone(java.time.ZoneId.systemDefault()));

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        List<String> stackLines = sw.toString().lines().collect(Collectors.toList());
        if (stackLines.isEmpty()) {
            stackLines = List.of(message);
        }

        errorLog.addLast(new ErrorEntry(timestamp, message, stackLines));
        while (errorLog.size() > MAX_ERROR_LOG_ENTRIES) {
            errorLog.removeFirst();
        }
        statusMessage = message;
        showErrorPopup = true;
    }

    private void updateProcessList(List<JavaProcess> updated) {
        processes.clear();
        processes.addAll(updated);
        processByPid = updated.stream()
                .collect(Collectors.toUnmodifiableMap(JavaProcess::pid, p -> p));

        long currentPid = selectedPid.get();
        int index = indexOfPid(currentPid);
        if (index < 0 && !processes.isEmpty()) {
            selectedProcessIndex = 0;
            selectedPid.set(processes.get(0).pid());
        } else if (index >= 0) {
            selectedProcessIndex = index;
        }
    }

    private int indexOfPid(long pid) {
        for (int i = 0; i < processes.size(); i++) {
            if (processes.get(i).pid() == pid) {
                return i;
            }
        }
        return -1;
    }

    private void applySnapshot(MetricsSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        this.metrics = snapshot;
        this.threads = snapshot.threads();
        this.garbageCollectors = snapshot.garbageCollectors();
        this.properties = snapshot.properties();

        if (snapshot.cpuPercent() >= 0) {
            cpuSeries.add(snapshot.cpuPercent());
        }
        if (snapshot.heapPercent() >= 0) {
            heapSeries.add(snapshot.heapPercent());
        }
        if (snapshot.threadCount() >= 0) {
            threadSeries.add(snapshot.threadCount());
        }
        if (snapshot.loadedClassCount() >= 0) {
            classSeries.add(snapshot.loadedClassCount());
        }
        if (snapshot.gcPausePercent() >= 0) {
            gcSeries.add(snapshot.gcPausePercent());
        }
    }

    private Element render() {
        JavaProcess selected = selectedProcess();
        Element header = renderHeader(selected);
        Element content = renderContent(selected);
        Element footer = renderFooter();

        Element base = dock()
                .top(header, Constraint.length(1))
                .center(content)
                .bottom(footer, Constraint.length(1))
                .id("textvm");
        if (showThreadDetail) {
            return stack(base, renderThreadDetailPopup());
        }
        if (showErrorPopup) {
            return stack(base, renderErrorPopup());
        }
        return base;
    }

    private Element renderErrorPopup() {
        ErrorEntry last = errorLog.peekLast();
        if (last == null) {
            return dialog("Errors", text("No errors logged."))
                    .rounded()
                    .onCancel(() -> showErrorPopup = false)
                    .onConfirm(() -> showErrorPopup = false);
        }

        Element header = row(
                text(last.timestamp()).addClass("error-timestamp"),
                spacer(),
                text("[Esc] close").addClass("hint"))
                .spacing(1)
                .length(1);

        Element message = text(last.message()).addClass("error-message");

        Element trace = list()
                .data(last.stackTraceLines(), line -> text(line).addClass("error-trace").ellipsis())
                .scrollbar(ScrollBarPolicy.AS_NEEDED)
                .id("error-trace")
                .focusable()
                .fill();

        return dialog("Errors",
                column(
                        header,
                        withLength(message, 2),
                        trace)
                        .spacing(1))
                .rounded()
                .width(110)
                .length(24)
                .onCancel(() -> showErrorPopup = false)
                .onConfirm(() -> showErrorPopup = false);
    }

    private Element renderThreadDetailPopup() {
        if (selectedThreadIndex < 0 || selectedThreadIndex >= threads.size()) {
            showThreadDetail = false;
            return text("");
        }
        ThreadSnapshot thread = threads.get(selectedThreadIndex);
        String title = thread.name() + " [" + thread.state().name() + "]";

        Element header = row(
                text(title).addClass("thread-detail-title"),
                spacer(),
                text("[Esc] close").addClass("hint"))
                .spacing(1)
                .length(1);

        List<String> lines = thread.stackTrace().isEmpty()
                ? List.of("  (no stack trace available)")
                : thread.stackTrace();

        Element trace = list()
                .data(lines, line -> text(line).addClass("stack-frame").ellipsis())
                .scrollbar(ScrollBarPolicy.AS_NEEDED)
                .displayOnly()
                .id("thread-stack")
                .focusable()
                .fill();

        return dialog("Thread Detail",
                column(header, trace).spacing(1))
                .rounded()
                .width(100)
                .length(30)
                .onCancel(() -> showThreadDetail = false)
                .onConfirm(() -> showThreadDetail = false);
    }

    private JavaProcess selectedProcess() {
        if (processes.isEmpty()) {
            return null;
        }
        int index = Math.min(selectedProcessIndex, processes.size() - 1);
        return processes.get(index);
    }

    private Element renderHeader(JavaProcess selected) {
        String processLabel = selected != null
                ? selected.displayName() + " (pid " + selected.pid() + ")"
                : "No Java process selected";

        return row(
                text(" TextVM ").addClass("title"),
                text(processLabel).addClass("subtitle"),
                spacer(),
                text(metrics.jmxStatus()).addClass("status"))
                .id("header");
    }

    private Element renderFooter() {
        String common = " [Tab] Focus  [↑↓] Navigate  [←→] Tab  [1-5] Jump  [R] Refresh  [E] Errors  [Q] Quit";
        String tabHint = switch (selectedTabIndex) {
            case 2 -> "  [Enter] Stack trace";
            default -> "";
        };
        return row(
                text(common + tabHint).addClass("hint"),
                spacer(),
                text(statusMessage).addClass("status"))
                .id("footer");
    }

    private Element renderContent(JavaProcess selected) {
        Element processPanel = withLength(renderProcessPanel(), PROCESS_PANEL_WIDTH);
        Element mainPanel = withFill(renderMainPanel(selected));

        return row(processPanel, mainPanel)
                .spacing(1)
                .fill();
    }

    private Element renderProcessPanel() {
        if (processes.isEmpty()) {
            return panel(() -> text("No Java processes detected.").addClass("empty"))
                    .title("Processes");
        }

        ListElement<JavaProcess> processList = list()
                .data(processes, this::renderProcessItem)
                .selected(selectedProcessIndex)
                .scrollbar(ScrollBarPolicy.AS_NEEDED)
                .id("process-list")
                .focusable();
        processList.onKeyEvent(event -> {
            if (event.matches(Actions.MOVE_UP)) {
                selectProcess(selectedProcessIndex - 1);
                return EventResult.HANDLED;
            }
            if (event.matches(Actions.MOVE_DOWN)) {
                selectProcess(selectedProcessIndex + 1);
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        });

        return panel(() -> processList)
                .title("Processes")
                .id("process-panel");
    }

    private StyledElement<?> renderProcessItem(JavaProcess process) {
        String pidText = String.format(Locale.ROOT, "%6d", process.pid());
        String selfMark = process.isSelf() ? "*" : " ";

        return row(
                text(pidText).addClass("process-pid"),
                text(selfMark).addClass("process-self"),
                text(process.displayName()).ellipsis().addClass("process-name").fill())
                .spacing(1)
                .length(1);
    }

    private Element renderMainPanel(JavaProcess selected) {
        Element tabBar = tabs(TAB_TITLES)
                .selected(selectedTabIndex)
                .highlightStyle(Style.EMPTY.fg(Color.LIGHT_YELLOW).bold().underlined())
                .divider(" | ")
                .padding("", "")
                .id("tabs")
                .length(1);

        Element content = withFill(renderTabContent(selected));

        return column(tabBar, content)
                .spacing(1)
                .fill();
    }

    private Element renderTabContent(JavaProcess selected) {
        switch (selectedTabIndex) {
            case 0:
                return renderOverview(selected);
            case 1:
                return renderMonitor();
            case 2:
                return renderThreads();
            case 3:
                return renderGc();
            case 4:
                return renderProperties();
            default:
                return renderOverview(selected);
        }
    }

    private Element renderOverview(JavaProcess selected) {
        if (selected == null) {
            return emptyPanel("Overview", "Select a Java process to inspect.");
        }

        Instant startTime = metrics.startTime() != null ? metrics.startTime() : selected.startTime();
        Duration uptime = metrics.uptime() != null
                ? metrics.uptime()
                : startTime != null ? Duration.between(startTime, Instant.now()) : null;

        String heapSummary = metrics.heapUsed() >= 0 && metrics.heapMax() > 0
                ? TextVmFormatting.formatBytes(metrics.heapUsed()) + " / "
                + TextVmFormatting.formatBytes(metrics.heapMax())
                : "n/a";

        String threadSummary = metrics.threadCount() >= 0
                ? String.format(Locale.ROOT, "%d (daemon %d, peak %d)",
                        metrics.threadCount(),
                        Math.max(0, metrics.daemonThreadCount()),
                        Math.max(0, metrics.peakThreadCount()))
                : "n/a";

        String classSummary = metrics.loadedClassCount() >= 0
                ? String.format(Locale.ROOT, "%d loaded, %d total, %d unloaded",
                        metrics.loadedClassCount(),
                        Math.max(0, metrics.totalLoadedClassCount()),
                        Math.max(0, metrics.unloadedClassCount()))
                : "n/a";

        String gcSummary = metrics.gcCount() >= 0
                ? String.format(Locale.ROOT, "%d collections, %d ms",
                        metrics.gcCount(),
                        Math.max(0, metrics.gcTimeMs()))
                : "n/a";

        // Split command and args for list display
        String commandLines = selected.commandLine();
        List<String> argLines = Arrays.asList(selected.arguments());
        
        Element processPanel = panel(() -> column(
                // Simple fields in a table
                table()
                        .widths(Constraint.length(10), Constraint.fill())
                        .columnSpacing(1)
                        .row("PID", String.valueOf(selected.pid()))
                        .row("Name", selected.fullName())
                        .row("User", selected.user())
                        .row("Start", formatInstant(startTime))
                        .row("Uptime", TextVmFormatting.formatDuration(uptime))
                        .row("JMX", metrics.jmxStatus())
                        .fill(),
                // Command as a list
                text("Command:").addClass("kv-key").length(1),
                list(commandLines)
                        .scrollbar(ScrollBarPolicy.AS_NEEDED),
                // Args as a list
                text("Args:").addClass("kv-key").length(1),
                list(argLines)
                        .scrollbar(ScrollBarPolicy.AS_NEEDED)
                        .fill())
                        .spacing(0)
                        .fill())
                .title("Process")
                .fill();

        Element runtimePanel = panel(() -> table()
                .widths(Constraint.length(10), Constraint.fill())
                .columnSpacing(1)
                .row("CPU", TextVmFormatting.formatPercent(metrics.processCpuLoad()))
                .row("System CPU", TextVmFormatting.formatPercent(metrics.systemCpuLoad()))
                .row("Heap", heapSummary)
                .row("Non-Heap", TextVmFormatting.formatBytes(metrics.nonHeapUsed()))
                .row("Threads", threadSummary)
                .row("Classes", classSummary)
                .row("GC", gcSummary)
                .fill())
                .title("Runtime")
                .fill();

        return row(processPanel, runtimePanel)
                .spacing(1)
                .fill();
    }

    private Element renderMonitor() {
        String cpuLabel = TextVmFormatting.formatPercent(metrics.processCpuLoad());
        String heapLabel = metrics.heapUsed() >= 0 && metrics.heapMax() > 0
                ? TextVmFormatting.formatBytes(metrics.heapUsed())
                : "n/a";
        String threadLabel = metrics.threadCount() >= 0
                ? String.valueOf(metrics.threadCount())
                : "n/a";
        String classLabel = metrics.loadedClassCount() >= 0
                ? String.valueOf(metrics.loadedClassCount())
                : "n/a";

        Element cpuPanel = panel(() -> column(
                metricHeader("Current", cpuLabel),
                gauge(Math.max(0, metrics.cpuPercent()))
                        .label(cpuLabel)
                        .useUnicode(false),
                sparkline(cpuSeries.values()).max(100)))
                .title("CPU")
                .fill();

        Element heapPanel = panel(() -> column(
                metricHeader("Used", heapLabel),
                gauge(Math.max(0, metrics.heapPercent()))
                        .label(formatHeapPercent())
                        .useUnicode(false),
                metricLineChart("Heap %", heapSeries.values(), 100)))
                .title("Heap")
                .fill();

        Element threadPanel = panel(() -> column(
                metricHeader("Threads", threadLabel),
                gauge(threadPercent())
                        .label(threadLabel)
                        .useUnicode(false),
                metricLineChart("Threads", threadSeries.values(), threadSeriesMax())))
                .title("Threads")
                .fill();

        Element classPanel = panel(() -> column(
                metricHeader("Loaded", classLabel),
                gauge(classPercent())
                        .label(classLabel)
                        .useUnicode(false),
                metricLineChart("Classes", classSeries.values(), classSeriesMax())))
                .title("Classes")
                .fill();

        Element gcPanel = panel(() -> column(
                metricHeader("GC Pause", formatGcPause()),
                gauge(Math.max(0, metrics.gcPausePercent()))
                        .label(formatGcPause())
                        .useUnicode(false),
                sparkline(gcSeries.values()).max(100)))
                .title("GC Pause")
                .fill();

        Element rowTop = row(cpuPanel, heapPanel)
                .spacing(1)
                .fill();
        Element rowBottom = row(threadPanel, classPanel, gcPanel)
                .spacing(1)
                .fill();

        return column(rowTop, rowBottom)
                .spacing(1)
                .fill();
    }

    private Element renderThreads() {
        if (threads.isEmpty()) {
            return emptyPanel("Threads", "Thread details require a successful JMX attach.");
        }

        Map<Thread.State, Long> stateCounts = threads.stream()
                .collect(Collectors.groupingBy(ThreadSnapshot::state, Collectors.counting()));

        ListElement<ThreadSnapshot> threadList = list()
                .data(threads, this::renderThreadItem)
                .selected(selectedThreadIndex)
                .scrollbar(ScrollBarPolicy.AS_NEEDED)
                .id("thread-list")
                .focusable();
        threadList.onKeyEvent(event -> {
            if (event.matches(Actions.CONFIRM) || event.matches(Actions.SELECT)) {
                selectedThreadIndex = threadList.selected();
                showThreadDetail = true;
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        });

        Element listPanel = panel(() -> threadList)
                .title("Threads (" + threads.size() + ")")
                .fill();

        Element statePanel = panel(() -> column(
                keyValueRow("RUNNABLE", formatCount(stateCounts.get(Thread.State.RUNNABLE))),
                keyValueRow("WAITING", formatCount(stateCounts.get(Thread.State.WAITING))),
                keyValueRow("TIMED_WAITING", formatCount(stateCounts.get(Thread.State.TIMED_WAITING))),
                keyValueRow("BLOCKED", formatCount(stateCounts.get(Thread.State.BLOCKED))),
                keyValueRow("NEW", formatCount(stateCounts.get(Thread.State.NEW))),
                keyValueRow("TERMINATED", formatCount(stateCounts.get(Thread.State.TERMINATED)))))
                .title("States")
                .length(24);

        return row(listPanel, statePanel)
                .spacing(1)
                .fill();
    }

    private StyledElement<?> renderThreadItem(ThreadSnapshot snapshot) {
        String cpuTime = snapshot.cpuTimeMs() >= 0
                ? String.format(Locale.ROOT, "%d ms", snapshot.cpuTimeMs())
                : "n/a";

        String stateClass = switch (snapshot.state()) {
            case RUNNABLE -> "state-runnable";
            case BLOCKED -> "state-blocked";
            case WAITING -> "state-waiting";
            case TIMED_WAITING -> "state-timed-waiting";
            case NEW -> "state-new";
            case TERMINATED -> "state-terminated";
        };

        return row(
                text(snapshot.name()).ellipsis().addClass("thread-name").fill(),
                text(snapshot.state().name()).addClass("thread-state").addClass(stateClass),
                text(cpuTime).addClass("thread-cpu"))
                .spacing(1)
                .length(1);
    }

    private Element renderGc() {
        if (garbageCollectors.isEmpty()) {
            return emptyPanel("GC", "GC statistics are not available without JMX.");
        }

        List<GcSnapshot> sorted = new ArrayList<>(garbageCollectors);
        sorted.sort(Comparator.comparing(GcSnapshot::name));

        Element list = list()
                .data(sorted, this::renderGcItem)
                .scrollbar(ScrollBarPolicy.AS_NEEDED)
                .id("gc-list")
                .focusable();

        Element listPanel = panel(() -> list)
                .title("Garbage Collectors")
                .fill();

        Element pausePanel = panel(() -> column(
                metricHeader("GC Pause", formatGcPause()),
                gauge(Math.max(0, metrics.gcPausePercent()))
                        .label(formatGcPause())
                        .useUnicode(false),
                sparkline(gcSeries.values()).max(100)))
                .title("GC Pause %")
                .fill();

        return row(listPanel, pausePanel)
                .spacing(1)
                .fill();
    }

    private StyledElement<?> renderGcItem(GcSnapshot snapshot) {
        String count = snapshot.count() >= 0 ? String.valueOf(snapshot.count()) : "n/a";
        String time = snapshot.timeMs() >= 0 ? snapshot.timeMs() + " ms" : "n/a";

        return row(
                text(snapshot.name()).ellipsis().addClass("gc-name").fill(),
                text(count).addClass("gc-count"),
                text(time).addClass("gc-time"))
                .spacing(1)
                .length(1);
    }

    private Element renderProperties() {
        if (properties.isEmpty()) {
            return emptyPanel("Properties", "Runtime properties require a successful JMX attach.");
        }

        Element list = list()
                .data(properties, entry -> row(
                        text(entry.getKey()).addClass("kv-key"),
                        text(entry.getValue()).ellipsis().addClass("kv-value").fill())
                        .spacing(1)
                        .length(1))
                .scrollbar(ScrollBarPolicy.AS_NEEDED)
                .id("properties-list")
                .focusable();

        return panel(() -> list)
                .title("Runtime Properties")
                .fill();
    }

    private Element emptyPanel(String title, String message) {
        return panel(() -> text(message).addClass("empty"))
                .title(title)
                .fill();
    }

    private Element keyValueRow(String key, String value) {
        String safeValue = value == null || value.isBlank() ? "n/a" : value;
        return row(
                text(key).addClass("kv-key"),
                text(safeValue).ellipsis().addClass("kv-value").fill())
                .spacing(1)
                .length(1);
    }

    private Element metricHeader(String label, String value) {
        return row(
                text(label).addClass("kv-key"),
                spacer(),
                text(value).addClass("kv-value"))
                .length(1);
    }

    private int threadPercent() {
        if (metrics.threadCount() < 0 || metrics.peakThreadCount() <= 0) {
            return 0;
        }
        return (int) Math.min(100,
                Math.round(metrics.threadCount() * 100.0 / metrics.peakThreadCount()));
    }

    private int classPercent() {
        if (metrics.loadedClassCount() < 0 || metrics.totalLoadedClassCount() <= 0) {
            return 0;
        }
        return (int) Math.min(100,
                Math.round(metrics.loadedClassCount() * 100.0 / metrics.totalLoadedClassCount()));
    }

    private long threadSeriesMax() {
        return Math.max(1, metrics.peakThreadCount());
    }

    private long classSeriesMax() {
        return Math.max(1, metrics.loadedClassCount());
    }

    private Element metricLineChart(String name, List<Long> series, long maxY) {
        if (series == null || series.isEmpty()) {
            return text("no data").dim();
        }
        int n = series.size();
        double[][] data = new double[n][2];
        long localMax = 0;
        for (int i = 0; i < n; i++) {
            long v = series.get(i);
            data[i][0] = i;
            data[i][1] = v;
            if (v > localMax) {
                localMax = v;
            }
        }
        double yMax = Math.max(1, maxY > 0 ? maxY : localMax);

        Dataset dataset = Dataset.builder()
                .name(name)
                .data(data)
                .graphType(GraphType.LINE)
                .style(Style.EMPTY.fg(Color.CYAN))
                .build();

        Axis xAxis = Axis.builder()
                .bounds(0, Math.max(1, n - 1))
                .build();
        Axis yAxis = Axis.builder()
                .bounds(0, yMax)
                .build();

        return chart()
                .datasets(dataset)
                .xAxis(xAxis)
                .yAxis(yAxis)
                .hideLegend();
    }

    private String formatHeapPercent() {
        if (metrics.heapPercent() < 0) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%d%%", metrics.heapPercent());
    }

    private String formatGcPause() {
        if (metrics.gcPausePercent() < 0) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%d%%", metrics.gcPausePercent());
    }

    private static String formatInstant(Instant instant) {
        if (instant == null) {
            return "n/a";
        }
        return TIME_FORMAT.format(instant.atZone(java.time.ZoneId.systemDefault()));
    }

    private EventResult handleKey(KeyEvent event) {
        // Left/Right for tabs, letter keys for actions, number keys for tab jumping
        if (event.matches(Actions.MOVE_LEFT)) {
            selectTab(selectedTabIndex - 1);
            return EventResult.HANDLED;
        }
        if (event.matches(Actions.MOVE_RIGHT)) {
            selectTab(selectedTabIndex + 1);
            return EventResult.HANDLED;
        }
        if (event.isChar('r') || event.isChar('R')) {
            updateProcessList(ProcessCollector.collect());
            statusMessage = "Process list refreshed";
            return EventResult.HANDLED;
        }
        if (event.isChar('e') || event.isChar('E')) {
            showErrorPopup = !showErrorPopup;
            return EventResult.HANDLED;
        }
        for (int i = 0; i < TAB_TITLES.size(); i++) {
            if (event.isChar((char) ('1' + i))) {
                selectTab(i);
                return EventResult.HANDLED;
            }
        }
        return EventResult.UNHANDLED;
    }

    private void selectProcess(int index) {
        if (processes.isEmpty()) {
            return;
        }
        int bounded = Math.max(0, Math.min(index, processes.size() - 1));
        if (bounded != selectedProcessIndex) {
            selectedProcessIndex = bounded;
            selectedPid.set(processes.get(bounded).pid());
            resetSeries();
            metrics = MetricsSnapshot.empty();
            threads = Collections.emptyList();
            garbageCollectors = Collections.emptyList();
            properties = Collections.emptyList();
        }
    }

    private void selectTab(int index) {
        int bounded = Math.max(0, Math.min(index, TAB_TITLES.size() - 1));
        selectedTabIndex = bounded;
    }

    private void resetSeries() {
        cpuSeries.clear();
        heapSeries.clear();
        threadSeries.clear();
        classSeries.clear();
        gcSeries.clear();
    }

    private static String formatCount(Long value) {
        if (value == null) {
            return "0";
        }
        return String.valueOf(value);
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private static final class ProcessCollector {
        private static List<JavaProcess> collect() {
            long selfPid = ProcessHandle.current().pid();
            List<JavaProcess> result = new ArrayList<>();
            ProcessHandle.allProcesses().forEach(handle -> {
                JavaProcess process = JavaProcess.from(handle, selfPid);
                if (process != null) {
                    result.add(process);
                }
            });
            result.sort(Comparator.comparingLong(JavaProcess::pid));
            return result;
        }
    }

    private record JavaProcess(long pid,
                               String displayName,
                               String fullName,
                               String commandLine,
                               String[] arguments,
                               String user,
                               Instant startTime,
                               boolean isSelf,
                               ProcessHandle handle) {

        private static JavaProcess from(ProcessHandle handle, long selfPid) {
            try {
                ProcessHandle.Info info = handle.info();
                boolean isSelf = handle.pid() == selfPid;
                String commandLine = info.command().orElse("");
                String[] args = info.arguments().orElse(new String[0]);

                boolean isJava = isSelf || isJavaCommand(info.command().orElse(""), args);
                if (!isJava) {
                    return null;
                }

                String mainCandidate = TextVmFormatting.findMainCandidate(args, commandLine);
                String displayName = TextVmFormatting.shortName(mainCandidate);
                String fullName = mainCandidate.isBlank() ? "Unknown" : mainCandidate;
                String user = info.user().orElse("unknown");
                Instant startTime = info.startInstant().orElse(null);
                String effectiveCommandLine = commandLine.isBlank()
                        ? buildCommandLine(info.command().orElse(""), args)
                        : commandLine;

                return new JavaProcess(handle.pid(), displayName, fullName, effectiveCommandLine,
                        args, user, startTime, isSelf, handle);
            } catch (Exception e) {
                return null;
            }
        }

        private static String buildCommandLine(String command, String[] args) {
            if (command == null) {
                command = "";
            }
            if (args == null || args.length == 0) {
                return command;
            }
            if (command.isBlank()) {
                return TextVmFormatting.joinArgs(args);
            }
            return command + " " + TextVmFormatting.joinArgs(args);
        }

        private static boolean isJavaCommand(String command, String[] args) {
            StringBuilder buffer = new StringBuilder();
            buffer.append(command);
            if (args != null) {
                for (String arg : args) {
                    buffer.append(' ').append(arg);
                }
            }
            String lower = buffer.toString().toLowerCase(Locale.ROOT);
            if (lower.contains("java")) {
                return true;
            }
            if (args != null) {
                for (String arg : args) {
                    if (arg.endsWith(".jar")) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private record MetricsSnapshot(
            String jmxStatus,
            String jmxDetails,
            Instant startTime,
            Duration uptime,
            double processCpuLoad,
            double systemCpuLoad,
            long heapUsed,
            long heapCommitted,
            long heapMax,
            long nonHeapUsed,
            int threadCount,
            int daemonThreadCount,
            int peakThreadCount,
            int loadedClassCount,
            long totalLoadedClassCount,
            long unloadedClassCount,
            long gcCount,
            long gcTimeMs,
            int cpuPercent,
            int heapPercent,
            int gcPausePercent,
            List<ThreadSnapshot> threads,
            List<GcSnapshot> garbageCollectors,
            List<Map.Entry<String, String>> properties) {

        private static MetricsSnapshot empty() {
            return new MetricsSnapshot(
                    "n/a",
                    "",
                    null,
                    null,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList());
        }
    }

    private record ThreadSnapshot(String name, Thread.State state, long cpuTimeMs, List<String> stackTrace) {
    }

    private record GcSnapshot(String name, long count, long timeMs) {
    }

    private final class MetricsCollector {
        private final CpuSampler cpuSampler = new CpuSampler();
        private final JmxAccess localAccess = new LocalJmxAccess();
        private JmxAccess remoteAccess;
        private long remotePid = -1;
        private long lastGcTimeMs = -1;
        private Instant lastGcSampleTime;
        private Instant lastAttachAttempt;

        private MetricsSnapshot collect(JavaProcess process) {
            if (process == null) {
                return MetricsSnapshot.empty();
            }
            if (process.isSelf()) {
                return collectFromAccess(localAccess, process, "Local");
            }

            if (remoteAccess == null || remotePid != process.pid()) {
                closeRemote();
                remotePid = process.pid();
                lastGcTimeMs = -1;
                lastGcSampleTime = null;
                lastAttachAttempt = null;
            }

            if (remoteAccess == null) {
                Instant now = Instant.now();
                if (lastAttachAttempt == null
                        || Duration.between(lastAttachAttempt, now).compareTo(ATTACH_RETRY_INTERVAL) >= 0) {
                    lastAttachAttempt = now;
                    remoteAccess = tryAttach(process.pid());
                } else {
                    return collectFallback(process, "Attach pending");
                }
            }

            if (remoteAccess == null) {
                return collectFallback(process, "Attach failed");
            }

            MetricsSnapshot snapshot = collectFromAccess(remoteAccess, process, "Attached");
            if (snapshot.jmxDetails() != null && !snapshot.jmxDetails().isBlank()) {
                return snapshot;
            }
            return snapshot;
        }

        private MetricsSnapshot collectFromAccess(JmxAccess access, JavaProcess process, String status) {
            try {
                RuntimeMXBean runtime = access.runtime();
                MemoryMXBean memory = access.memory();
                ThreadMXBean threads = access.threads();
                ClassLoadingMXBean classes = access.classes();
                OperatingSystemMXBean os = access.os();

                Instant startTime = runtime != null
                        ? Instant.ofEpochMilli(runtime.getStartTime())
                        : process.startTime();
                Duration uptime = runtime != null
                        ? Duration.ofMillis(runtime.getUptime())
                        : startTime != null ? Duration.between(startTime, Instant.now()) : null;

                MemoryUsage heap = memory != null ? memory.getHeapMemoryUsage() : null;
                MemoryUsage nonHeap = memory != null ? memory.getNonHeapMemoryUsage() : null;
                long heapUsed = heap != null ? heap.getUsed() : -1;
                long heapCommitted = heap != null ? heap.getCommitted() : -1;
                long heapMax = heap != null ? heap.getMax() : -1;
                long nonHeapUsed = nonHeap != null ? nonHeap.getUsed() : -1;

                double processCpu = os != null ? normalizeCpuLoad(os.getProcessCpuLoad()) : -1;
                double systemCpu = os != null ? normalizeCpuLoad(systemCpuLoad(os)) : -1;

                int threadCount = threads != null ? threads.getThreadCount() : -1;
                int daemonThreads = threads != null ? threads.getDaemonThreadCount() : -1;
                int peakThreads = threads != null ? threads.getPeakThreadCount() : -1;

                int loadedClasses = classes != null ? classes.getLoadedClassCount() : -1;
                long totalLoaded = classes != null ? classes.getTotalLoadedClassCount() : -1;
                long unloaded = classes != null ? classes.getUnloadedClassCount() : -1;

                List<GcSnapshot> gcSnapshots = collectGc(access.garbageCollectors());
                long gcCount = gcSnapshots.stream().mapToLong(GcSnapshot::count).filter(v -> v >= 0).sum();
                long gcTime = gcSnapshots.stream().mapToLong(GcSnapshot::timeMs).filter(v -> v >= 0).sum();

                int gcPausePercent = computeGcPausePercent(gcTime);
                int cpuPercent = processCpu >= 0 ? (int) Math.round(processCpu * 100) : -1;
                int heapPercent = heapMax > 0 ? (int) Math.round(heapUsed * 100.0 / heapMax) : -1;

                List<ThreadSnapshot> threadSnapshots = collectThreads(threads);
                List<Map.Entry<String, String>> props = collectProperties(runtime);

                return new MetricsSnapshot(
                        status,
                        "",
                        startTime,
                        uptime,
                        processCpu,
                        systemCpu,
                        heapUsed,
                        heapCommitted,
                        heapMax,
                        nonHeapUsed,
                        threadCount,
                        daemonThreads,
                        peakThreads,
                        loadedClasses,
                        totalLoaded,
                        unloaded,
                        gcCount,
                        gcTime,
                        cpuPercent,
                        heapPercent,
                        gcPausePercent,
                        threadSnapshots,
                        gcSnapshots,
                        props);
            } catch (Exception e) {
                return collectFallback(process, "JMX error: " + safeMessage(e));
            }
        }

        private MetricsSnapshot collectFallback(JavaProcess process, String status) {
            double cpuLoad = cpuSampler.sample(process);
            int cpuPercent = cpuLoad >= 0 ? (int) Math.round(cpuLoad * 100) : -1;
            return new MetricsSnapshot(
                    status,
                    "",
                    process.startTime(),
                    process.startTime() != null ? Duration.between(process.startTime(), Instant.now()) : null,
                    cpuLoad,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    cpuPercent,
                    -1,
                    -1,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList());
        }

        private int computeGcPausePercent(long totalGcTimeMs) {
            if (totalGcTimeMs < 0) {
                return -1;
            }
            Instant now = Instant.now();
            if (lastGcSampleTime == null) {
                lastGcSampleTime = now;
                lastGcTimeMs = totalGcTimeMs;
                return -1;
            }
            long delta = totalGcTimeMs - lastGcTimeMs;
            long window = Duration.between(lastGcSampleTime, now).toMillis();
            lastGcSampleTime = now;
            lastGcTimeMs = totalGcTimeMs;
            if (delta < 0 || window <= 0) {
                return -1;
            }
            return (int) Math.min(100, Math.round(delta * 100.0 / window));
        }

        private List<ThreadSnapshot> collectThreads(ThreadMXBean threadBean) {
            if (threadBean == null) {
                return Collections.emptyList();
            }
            long[] ids = threadBean.getAllThreadIds();
            ThreadInfo[] infos = threadBean.getThreadInfo(ids, 64);
            boolean cpuEnabled = threadBean.isThreadCpuTimeSupported()
                    && threadBean.isThreadCpuTimeEnabled();

            List<ThreadSnapshot> snapshots = new ArrayList<>();
            for (ThreadInfo info : infos) {
                if (info == null) {
                    continue;
                }
                long cpuTimeMs = -1;
                if (cpuEnabled) {
                    long cpuTimeNs = threadBean.getThreadCpuTime(info.getThreadId());
                    if (cpuTimeNs >= 0) {
                        cpuTimeMs = TimeUnit.NANOSECONDS.toMillis(cpuTimeNs);
                    }
                }
                List<String> stackTrace = new ArrayList<>();
                for (StackTraceElement frame : info.getStackTrace()) {
                    stackTrace.add("  at " + frame.toString());
                }
                snapshots.add(new ThreadSnapshot(info.getThreadName(), info.getThreadState(), cpuTimeMs, stackTrace));
            }

            snapshots.sort((a, b) -> Long.compare(b.cpuTimeMs(), a.cpuTimeMs()));
            if (snapshots.size() > MAX_THREADS) {
                return new ArrayList<>(snapshots.subList(0, MAX_THREADS));
            }
            return snapshots;
        }

        private List<GcSnapshot> collectGc(List<GarbageCollectorMXBean> collectors) {
            if (collectors == null) {
                return Collections.emptyList();
            }
            List<GcSnapshot> snapshots = new ArrayList<>();
            for (GarbageCollectorMXBean gc : collectors) {
                if (gc == null) {
                    continue;
                }
                snapshots.add(new GcSnapshot(gc.getName(), gc.getCollectionCount(), gc.getCollectionTime()));
            }
            return snapshots;
        }

        private List<Map.Entry<String, String>> collectProperties(RuntimeMXBean runtime) {
            if (runtime == null) {
                return Collections.emptyList();
            }
            Map<String, String> props = runtime.getSystemProperties();
            List<Map.Entry<String, String>> entries = new ArrayList<>();

            String args = TextVmFormatting.joinArgs(runtime.getInputArguments().toArray(new String[0]));
            if (!args.isBlank()) {
                entries.add(Map.entry("vm.args", args));
            }

            props.entrySet().stream()
                    .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entries::add);

            return entries;
        }

        private JmxAccess tryAttach(long pid) {
            try {
                return RemoteJmxAccess.connect(pid);
            } catch (AttachNotSupportedException e) {
                logError("Attach not supported (pid " + pid + ")", e);
                return null;
            } catch (IOException e) {
                logError("Attach I/O failure (pid " + pid + ")", e);
                return null;
            } catch (Exception e) {
                logError("Attach failure (pid " + pid + ")", e);
                return null;
            }
        }

        private void closeRemote() {
            if (remoteAccess != null) {
                try {
                    remoteAccess.close();
                } catch (Exception ignored) {
                    // ignore close failures
                }
                remoteAccess = null;
            }
        }

        private void close() {
            closeRemote();
        }
    }

    private static double normalizeCpuLoad(double value) {
        if (value < 0) {
            return -1;
        }
        return Math.max(0, Math.min(1, value));
    }

    @SuppressWarnings("deprecation")
    private static double systemCpuLoad(OperatingSystemMXBean os) {
        return os.getSystemCpuLoad();
    }

    private interface JmxAccess extends AutoCloseable {
        MemoryMXBean memory();

        ThreadMXBean threads();

        ClassLoadingMXBean classes();

        RuntimeMXBean runtime();

        OperatingSystemMXBean os();

        List<GarbageCollectorMXBean> garbageCollectors();

        @Override
        void close();
    }

    private static final class LocalJmxAccess implements JmxAccess {
        private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        private final ClassLoadingMXBean classes = ManagementFactory.getClassLoadingMXBean();
        private final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        private final OperatingSystemMXBean os;
        private final List<GarbageCollectorMXBean> garbageCollectors =
                new ArrayList<>(ManagementFactory.getGarbageCollectorMXBeans());

        private LocalJmxAccess() {
            OperatingSystemMXBean cast = null;
            java.lang.management.OperatingSystemMXBean platform = ManagementFactory.getOperatingSystemMXBean();
            if (platform instanceof OperatingSystemMXBean) {
                cast = (OperatingSystemMXBean) platform;
            }
            this.os = cast;
        }

        @Override
        public MemoryMXBean memory() {
            return memory;
        }

        @Override
        public ThreadMXBean threads() {
            return threads;
        }

        @Override
        public ClassLoadingMXBean classes() {
            return classes;
        }

        @Override
        public RuntimeMXBean runtime() {
            return runtime;
        }

        @Override
        public OperatingSystemMXBean os() {
            return os;
        }

        @Override
        public List<GarbageCollectorMXBean> garbageCollectors() {
            return garbageCollectors;
        }

        @Override
        public void close() {
            // no-op for local access
        }
    }

    private static final class RemoteJmxAccess implements JmxAccess {
        private final JMXConnector connector;
        private final MemoryMXBean memory;
        private final ThreadMXBean threads;
        private final ClassLoadingMXBean classes;
        private final RuntimeMXBean runtime;
        private final OperatingSystemMXBean os;
        private final List<GarbageCollectorMXBean> garbageCollectors;

        private RemoteJmxAccess(JMXConnector connector, MBeanServerConnection connection)
                throws IOException {
            this.connector = connector;
            this.memory = ManagementFactory.newPlatformMXBeanProxy(
                    connection,
                    ManagementFactory.MEMORY_MXBEAN_NAME,
                    MemoryMXBean.class);
            this.threads = ManagementFactory.newPlatformMXBeanProxy(
                    connection,
                    ManagementFactory.THREAD_MXBEAN_NAME,
                    ThreadMXBean.class);
            this.classes = ManagementFactory.newPlatformMXBeanProxy(
                    connection,
                    ManagementFactory.CLASS_LOADING_MXBEAN_NAME,
                    ClassLoadingMXBean.class);
            this.runtime = ManagementFactory.newPlatformMXBeanProxy(
                    connection,
                    ManagementFactory.RUNTIME_MXBEAN_NAME,
                    RuntimeMXBean.class);
            this.os = ManagementFactory.newPlatformMXBeanProxy(
                    connection,
                    ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
                    OperatingSystemMXBean.class);
            this.garbageCollectors = loadGarbageCollectors(connection);
        }

        private static RemoteJmxAccess connect(long pid)
                throws IOException, AttachNotSupportedException {
            VirtualMachine vm = VirtualMachine.attach(String.valueOf(pid));
            try {
                Properties props = vm.getAgentProperties();
                String address = props.getProperty("com.sun.management.jmxremote.localConnectorAddress");
                if (address == null) {
                    // Modern JDKs provide a supported API for starting the local management agent.
                    // This avoids relying on management-agent.jar, which may not exist in modular JDKs
                    // or may not be a valid Java agent in some distributions.
                    try {
                        address = vm.startLocalManagementAgent();
                    } catch (Exception ignored) {
                        // Fall back to legacy agent jar loading below.
                    }
                }
                if (address == null) {
                    String javaHome = vm.getSystemProperties().getProperty("java.home");
                    String agentPath = javaHome + File.separator + "lib"
                            + File.separator + "management-agent.jar";
                    vm.loadAgent(agentPath);
                    props = vm.getAgentProperties();
                    address = props.getProperty("com.sun.management.jmxremote.localConnectorAddress");
                }
                if (address == null) {
                    throw new IOException("JMX connector address not available");
                }
                JMXServiceURL url = new JMXServiceURL(address);
                JMXConnector connector = JMXConnectorFactory.connect(url);
                return new RemoteJmxAccess(connector, connector.getMBeanServerConnection());
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("Failed to attach to JVM", e);
            } finally {
                vm.detach();
            }
        }

        private static List<GarbageCollectorMXBean> loadGarbageCollectors(MBeanServerConnection connection)
                throws IOException {
            try {
                Set<ObjectName> names = connection.queryNames(
                        new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",name=*"),
                        null);
                List<GarbageCollectorMXBean> collectors = new ArrayList<>();
                for (ObjectName name : names) {
                    collectors.add(ManagementFactory.newPlatformMXBeanProxy(
                            connection,
                            name.toString(),
                            GarbageCollectorMXBean.class));
                }
                return collectors;
            } catch (javax.management.MalformedObjectNameException e) {
                throw new IOException("Failed to query GC beans", e);
            }
        }

        @Override
        public MemoryMXBean memory() {
            return memory;
        }

        @Override
        public ThreadMXBean threads() {
            return threads;
        }

        @Override
        public ClassLoadingMXBean classes() {
            return classes;
        }

        @Override
        public RuntimeMXBean runtime() {
            return runtime;
        }

        @Override
        public OperatingSystemMXBean os() {
            return os;
        }

        @Override
        public List<GarbageCollectorMXBean> garbageCollectors() {
            return garbageCollectors;
        }

        @Override
        public void close() {
            try {
                connector.close();
            } catch (IOException ignored) {
                // ignore close failures
            }
        }
    }

    private static final class CpuSampler {
        private final Map<Long, CpuSample> samples = new java.util.HashMap<>();

        private double sample(JavaProcess process) {
            if (process == null) {
                return -1;
            }
            Optional<Duration> cpuTime = process.handle().info().totalCpuDuration();
            if (cpuTime.isEmpty()) {
                return -1;
            }
            Instant now = Instant.now();
            CpuSample previous = samples.put(process.pid(), new CpuSample(cpuTime.get(), now));
            if (previous == null) {
                return -1;
            }
            long cpuDelta = cpuTime.get().minus(previous.cpuTime()).toNanos();
            long wallDelta = Duration.between(previous.sampleTime(), now).toNanos();
            if (wallDelta <= 0) {
                return -1;
            }
            int processors = Runtime.getRuntime().availableProcessors();
            double load = cpuDelta / (double) wallDelta / Math.max(1, processors);
            return Math.max(0, Math.min(1, load));
        }
    }

    private record CpuSample(Duration cpuTime, Instant sampleTime) {
    }

    private static final class MetricSeries {
        private final int maxSize;
        private final List<Long> values = new ArrayList<>();

        private MetricSeries(int maxSize) {
            this.maxSize = maxSize;
        }

        private void add(long value) {
            values.add(value);
            if (values.size() > maxSize) {
                values.remove(0);
            }
        }

        private void clear() {
            values.clear();
        }

        private List<Long> values() {
            return values.isEmpty() ? Collections.emptyList() : new ArrayList<>(values);
        }
    }
}
