/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.threaddump;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.text.CharWidth;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.elements.ListElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.sparkline;
import static dev.tamboui.toolkit.Toolkit.tabs;
import static dev.tamboui.toolkit.Toolkit.text;

import me.bechberger.jthreaddump.model.LockInfo;
import me.bechberger.jthreaddump.model.StackFrame;
import me.bechberger.jthreaddump.model.ThreadInfo;

/**
 * Stateful Toolkit app for browsing and comparing thread dump snapshots.
 */
final class ThreadDumpReviewApp extends ToolkitApp {

    private static final int SNAPSHOT_LABEL_WIDTH = 28;
    private static final int THREAD_NAME_WIDTH = 44;
    private static final int FRAME_LABEL_WIDTH = 72;
    private static final int DETAIL_LINE_WIDTH = 96;
    private static final int MOVE_PAGE = 8;

    private final List<String> inputArgs;
    private final List<ThreadDumpSnapshot> snapshots = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    private boolean usingEmbeddedSamples;
    private int selectedSnapshotIndex;
    private int baselineSnapshotIndex = -1;
    private int selectedThreadIndex;
    private int detailCursor;
    private int comparisonCursor;

    private FocusPane focusPane = FocusPane.SNAPSHOTS;
    private DetailTab detailTab = DetailTab.THREAD;
    private ThreadDumpAnalyzer.ThreadFilter filter = ThreadDumpAnalyzer.ThreadFilter.ALL;
    private ThreadDumpAnalyzer.ThreadSort sort = ThreadDumpAnalyzer.ThreadSort.CPU_DESC;
    private String searchQuery = "";
    private boolean regexSearch;
    private boolean searchInputMode;
    private ThreadDumpAnalyzer.SearchCriteria currentSearchCriteria = ThreadDumpAnalyzer.searchCriteria("", false);

    ThreadDumpReviewApp(List<String> inputArgs, ThreadDumpSnapshotLoader.LoadResult initialData) {
        this.inputArgs = List.copyOf(inputArgs);
        applyLoadResult(initialData);
    }

    @Override
    protected TuiConfig configure() {
        return TuiConfig.builder()
            .mouseCapture(true)
            .build();
    }

    @Override
    protected Element render() {
        clampSnapshotSelection();

        ThreadDumpSnapshot currentSnapshot = currentSnapshot();
        ThreadDumpSnapshot baselineSnapshot = effectiveBaselineSnapshot(currentSnapshot);
        List<ThreadDumpAnalyzer.ThreadView> unfilteredThreadViews = ThreadDumpAnalyzer.buildThreadViews(
            currentSnapshot,
            baselineSnapshot,
            filter,
            sort
        );
        ThreadDumpAnalyzer.SearchCriteria searchCriteria = searchCriteria();
        currentSearchCriteria = searchCriteria;
        List<ThreadDumpAnalyzer.ThreadView> threadViews = ThreadDumpAnalyzer.applySearch(unfilteredThreadViews, searchCriteria);

        selectedThreadIndex = clampIndex(selectedThreadIndex, threadViews.size());
        ThreadDumpAnalyzer.ThreadView selectedThread = threadViews.isEmpty()
            ? null
            : threadViews.get(selectedThreadIndex);

        ThreadDumpAnalyzer.SnapshotDiff diff = baselineSnapshot == null || currentSnapshot == null
            ? null
            : ThreadDumpAnalyzer.compare(baselineSnapshot, currentSnapshot);

        List<StyledLine> detailLines = buildThreadDetailLines(selectedThread);
        List<StyledLine> comparisonLines = buildComparisonLines(currentSnapshot, baselineSnapshot, diff);
        detailCursor = clampIndex(detailCursor, detailLines.size());
        comparisonCursor = clampIndex(comparisonCursor, comparisonLines.size());

        Element header = renderHeader(currentSnapshot, baselineSnapshot, searchCriteria);
        Element body = renderBody(
            currentSnapshot,
            baselineSnapshot,
            unfilteredThreadViews.size(),
            threadViews,
            selectedThread,
            diff,
            detailLines,
            comparisonLines
        );
        Element footer = renderFooter(searchCriteria);

        return dock()
            .top(header)
            .center(body)
            .bottom(footer)
            .onKeyEvent(this::handleKeyEvent)
            .onMouseEvent(this::handleMouseEvent)
            .focusable()
            .id("thread-dump-review-root");
    }

    private Element renderHeader(
        ThreadDumpSnapshot currentSnapshot,
        ThreadDumpSnapshot baselineSnapshot,
        ThreadDumpAnalyzer.SearchCriteria searchCriteria
    ) {
        String currentLabel = currentSnapshot == null ? "-" : currentSnapshot.displayName();
        String baselineLabel = baselineSnapshot == null ? "-" : baselineSnapshot.displayName();
        String modeLabel = regexSearch ? "regex" : "text";
        String searchLabel = searchCriteria.active()
            ? abbreviateByWidth(searchCriteria.query(), 16)
            : "none";
        if (searchInputMode) {
            searchLabel = searchLabel + "_";
        }

        return panel(row(
            text(" ThreadDump Review ").bold().cyan(),
            usingEmbeddedSamples ? text(" sample data ").yellow().bold() : text(" file data ").green(),
            spacer(),
            text("focus=").dim(),
            text(focusPane.label()).bold(),
            text("  filter=").dim(),
            text(filter.label()).white(),
            text("  sort=").dim(),
            text(sort.label()).white(),
            text("  search[").dim(),
            text(modeLabel).fg(regexSearch ? Color.MAGENTA : Color.CYAN),
            text("]=").dim(),
            searchCriteria.valid()
                ? text(searchLabel).white()
                : text("invalid").red(),
            spacer(),
            text("current ").dim(),
            text(abbreviateByWidth(currentLabel, 20)).cyan(),
            text(" | baseline ").dim(),
            text(abbreviateByWidth(baselineLabel, 20)).magenta()
        )).rounded().borderColor(Color.DARK_GRAY).length(3);
    }

    private Element renderBody(
        ThreadDumpSnapshot currentSnapshot,
        ThreadDumpSnapshot baselineSnapshot,
        int unfilteredThreadCount,
        List<ThreadDumpAnalyzer.ThreadView> threadViews,
        ThreadDumpAnalyzer.ThreadView selectedThread,
        ThreadDumpAnalyzer.SnapshotDiff diff,
        List<StyledLine> detailLines,
        List<StyledLine> comparisonLines
    ) {
        return row(
            renderSnapshotsPane(),
            renderThreadsPane(currentSnapshot, baselineSnapshot, unfilteredThreadCount, threadViews),
            renderDetailPane(currentSnapshot, baselineSnapshot, selectedThread, diff, detailLines, comparisonLines)
        );
    }

    private Element renderSnapshotsPane() {
        List<SnapshotRow> rows = new ArrayList<>(snapshots.size());
        for (int i = 0; i < snapshots.size(); i++) {
            rows.add(new SnapshotRow(i, snapshots.get(i)));
        }

        ListElement<SnapshotRow> listElement = list()
            .data(rows, this::renderSnapshotRow)
            .selected(selectedSnapshotIndex)
            .highlightSymbol(focusPane == FocusPane.SNAPSHOTS ? "> " : "  ")
            .highlightColor(focusPane == FocusPane.SNAPSHOTS ? Color.CYAN : Color.GRAY)
            .scrollbar(ListElement.ScrollBarPolicy.AS_NEEDED)
            .autoScroll()
            .title("Snapshots (" + snapshots.size() + ")")
            .rounded()
            .borderColor(focusPane == FocusPane.SNAPSHOTS ? Color.CYAN : Color.DARK_GRAY)
            .id("snapshot-list");

        if (snapshots.isEmpty()) {
            listElement.displayOnly();
        }
        return listElement.length(40);
    }

    private StyledElement<?> renderSnapshotRow(SnapshotRow rowData) {
        ThreadDumpSnapshot snapshot = rowData.snapshot();
        SnapshotStats stats = snapshot.stats();
        boolean isBaseline = rowData.index() == baselineSnapshotIndex;
        boolean isCurrent = rowData.index() == selectedSnapshotIndex;

        String marker = (isCurrent ? "C" : "-") + (isBaseline ? "B" : "-");
        String line1Text = "[" + marker + "] " + abbreviateByWidth(snapshot.displayName(), SNAPSHOT_LABEL_WIDTH);
        String line2Text = "t=" + snapshot.stats().totalThreads()
            + " run=" + stats.runnable()
            + " blk=" + stats.blocked()
            + " wait=" + (stats.waiting() + stats.timedWaiting())
            + " dl=" + stats.deadlocks()
            + " @" + snapshot.timestampLabel();

        return column(
            row(
                text(line1Text).bold(),
                spacer(),
                text(snapshot.timestampLabel()).dim()
            ),
            text(abbreviateByWidth(line2Text, SNAPSHOT_LABEL_WIDTH + 18)).dim()
        ).length(2);
    }

    private Element renderThreadsPane(
        ThreadDumpSnapshot currentSnapshot,
        ThreadDumpSnapshot baselineSnapshot,
        int unfilteredThreadCount,
        List<ThreadDumpAnalyzer.ThreadView> threadViews
    ) {
        String title;
        if (currentSnapshot == null) {
            title = "Threads";
        } else {
            title = "Threads " + threadViews.size() + "/" + unfilteredThreadCount;
            if (baselineSnapshot != null) {
                title += " (vs baseline)";
            }
            if (!searchQuery.isBlank()) {
                title += " [search]";
            }
        }

        ListElement<ThreadDumpAnalyzer.ThreadView> listElement = list()
            .data(threadViews, this::renderThreadRow)
            .selected(selectedThreadIndex)
            .highlightSymbol(focusPane == FocusPane.THREADS ? "> " : "  ")
            .highlightColor(focusPane == FocusPane.THREADS ? Color.CYAN : Color.GRAY)
            .scrollbar(ListElement.ScrollBarPolicy.AS_NEEDED)
            .autoScroll()
            .title(title)
            .rounded()
            .borderColor(focusPane == FocusPane.THREADS ? Color.CYAN : Color.DARK_GRAY)
            .id("thread-list");

        if (threadViews.isEmpty()) {
            listElement.displayOnly();
        }
        return listElement.percent(36);
    }

    private StyledElement<?> renderThreadRow(ThreadDumpAnalyzer.ThreadView view) {
        Color statusColor = switch (view.diffStatus()) {
            case NEW -> Color.GREEN;
            case CHANGED -> Color.YELLOW;
            case SAME -> Color.DARK_GRAY;
        };
        String statusText = switch (view.diffStatus()) {
            case NEW -> "[NEW]";
            case CHANGED -> "[CHG]";
            case SAME -> "[ = ]";
        };

        String name = abbreviateByWidth(safeThreadName(view.thread()), THREAD_NAME_WIDTH);
        String state = ThreadDumpAnalyzer.stateLabel(view.thread().state());
        String paddedState = String.format("%-13s", state);
        String cpuInfo = view.thread().cpuTimeSec() == null
            ? "cpu=n/a"
            : String.format("cpu=%.4fs", view.thread().cpuTimeSec());
        String depthInfo = "depth=" + view.stackDepth();
        String secondLine = cpuInfo + "  " + depthInfo + "  " + abbreviateByWidth(view.topFrameDisplay(), FRAME_LABEL_WIDTH);

        MatchScope matchScope = searchMatchScope(view, currentSearchCriteria);
        StyledElement<?> stateElement = text(paddedState).fg(stateColor(view.thread().state())).length(14);
        if (matchScope == MatchScope.STATE) {
            stateElement = text(paddedState).fg(stateColor(view.thread().state())).underlined().bold().length(14);
        }
        StyledElement<?> matchBadge = matchScope == MatchScope.NONE
            ? text("")
            : text(" [" + matchScope.label + "]").fg(Color.CYAN).dim();

        return column(
            row(
                text(statusText).fg(statusColor).bold().length(6),
                stateElement,
                highlightedText(name, currentSearchCriteria, false),
                matchBadge
            ),
            highlightedText(secondLine, currentSearchCriteria, true)
        ).length(2);
    }

    private Element renderDetailPane(
        ThreadDumpSnapshot currentSnapshot,
        ThreadDumpSnapshot baselineSnapshot,
        ThreadDumpAnalyzer.ThreadView selectedThread,
        ThreadDumpAnalyzer.SnapshotDiff diff,
        List<StyledLine> detailLines,
        List<StyledLine> comparisonLines
    ) {
        Element summary = renderSummaryPanel(currentSnapshot, baselineSnapshot, diff);
        var tabBar = tabs("Thread detail", "Comparison")
            .selected(detailTab == DetailTab.THREAD ? 0 : 1)
            .highlightColor(Color.CYAN)
            .padding(" ", " ")
            .divider(" ")
            .rounded()
            .borderColor(focusPane == FocusPane.DETAILS ? Color.CYAN : Color.DARK_GRAY)
            .length(3);

        List<StyledLine> activeLines = detailTab == DetailTab.THREAD ? detailLines : comparisonLines;
        int activeCursor = detailTab == DetailTab.THREAD ? detailCursor : comparisonCursor;

        ListElement<StyledLine> content = list()
            .data(activeLines, this::renderStyledLine)
            .selected(activeCursor)
            .displayOnly()
            .scrollbar(ListElement.ScrollBarPolicy.AS_NEEDED)
            .autoScroll()
            .title(detailTab == DetailTab.THREAD ? detailTitle(selectedThread) : comparisonTitle(baselineSnapshot))
            .rounded()
            .borderColor(focusPane == FocusPane.DETAILS ? Color.CYAN : Color.DARK_GRAY)
            .id("detail-list");

        return column(
            summary,
            tabBar,
            content.fill()
        ).fill();
    }

    private Element renderSummaryPanel(
        ThreadDumpSnapshot currentSnapshot,
        ThreadDumpSnapshot baselineSnapshot,
        ThreadDumpAnalyzer.SnapshotDiff diff
    ) {
        List<Element> lines = new ArrayList<>();

        if (currentSnapshot == null) {
            lines.add(text("No snapshots loaded.").yellow().bold());
            lines.add(text("Pass files/dirs as arguments, or use embedded sample data.").dim());
        } else {
            SnapshotStats stats = currentSnapshot.stats();
            lines.add(row(
                text("Source ").dim().length(8),
                text(abbreviateByWidth(currentSnapshot.displayName(), 42)).cyan()
            ));
            lines.add(row(
                text("Threads ").dim().length(8),
                text(Integer.toString(stats.totalThreads())).bold(),
                text("  deadlocks ").dim(),
                text(Integer.toString(stats.deadlocks())).fg(stats.deadlocks() > 0 ? Color.RED : Color.GREEN)
            ));
            lines.add(row(
                text("States ").dim().length(8),
                text("RUN " + stats.runnable()).fg(Color.GREEN),
                text("  BLK " + stats.blocked()).fg(Color.YELLOW),
                text("  WAIT " + (stats.waiting() + stats.timedWaiting())).fg(Color.MAGENTA)
            ));
            if (baselineSnapshot != null && diff != null) {
                String delta = signed(diff.currentThreadCount() - diff.baselineThreadCount());
                lines.add(row(
                    text("Delta ").dim().length(8),
                    text("threads " + delta).fg(colorForSigned(diff.currentThreadCount() - diff.baselineThreadCount())),
                    text("  +new " + diff.addedThreads()).fg(Color.GREEN),
                    text("  -gone " + diff.removedThreads()).fg(Color.RED)
                ));
            }
        }

        return panel(column(lines.toArray(Element[]::new)))
            .title("Summary")
            .rounded()
            .borderColor(Color.DARK_GRAY)
            .length(baselineSnapshot == null ? 6 : 7);
    }

    private StyledElement<?> renderStyledLine(StyledLine line) {
        String value = abbreviateByWidth(line.text(), DETAIL_LINE_WIDTH);
        return switch (line.tone()) {
            case TITLE -> text(value).bold().cyan();
            case SECTION -> text(value).bold().white();
            case MUTED -> text(value).dim();
            case GOOD -> text(value).green();
            case BAD -> text(value).red();
            case WARN -> text(value).yellow();
            case NORMAL -> text(value);
        };
    }

    private Element renderFooter(ThreadDumpAnalyzer.SearchCriteria searchCriteria) {
        Element timeline = renderTimelineElement();
        StyledElement<?> searchHint = searchCriteria.valid()
            ? text(searchPrompt()).dim()
            : text(" invalid regex: " + abbreviateByWidth(searchCriteria.error(), 28) + " ").red();
        return panel(row(
            text(" Left/Right pane ").dim(),
            text(" Up/Down navigate ").dim(),
            text(" click/select ").dim(),
            text(" b baseline ").dim(),
            text(" f filter ").dim(),
            text(" s sort ").dim(),
            text(" / search ").dim(),
            text(" Ctrl+R regex ").dim(),
            text(" 1/2 tabs ").dim(),
            text(" r reload ").dim(),
            text(" q quit ").dim(),
            searchHint,
            spacer(),
            timeline
        )).rounded().borderColor(Color.DARK_GRAY).length(3);
    }

    private Element renderTimelineElement() {
        if (snapshots.size() < 2) {
            return text("timeline n/a").dim().length(14);
        }
        long[] counts = new long[snapshots.size()];
        long max = 1;
        for (int i = 0; i < snapshots.size(); i++) {
            counts[i] = snapshots.get(i).stats().totalThreads();
            max = Math.max(max, counts[i]);
        }
        return sparkline(counts)
            .max(max)
            .color(Color.CYAN)
            .length(1)
            .min(20);
    }

    private List<StyledLine> buildThreadDetailLines(ThreadDumpAnalyzer.ThreadView selectedThread) {
        if (selectedThread == null) {
            return List.of(
                new StyledLine("No thread selected.", LineTone.TITLE),
                new StyledLine("Move to the Threads pane and select a thread.", LineTone.MUTED)
            );
        }

        List<StyledLine> lines = new ArrayList<>();
        ThreadInfo thread = selectedThread.thread();
        ThreadInfo baseline = selectedThread.baselineThread();

        lines.add(new StyledLine("Thread: " + safeThreadName(thread), LineTone.TITLE));
        lines.add(new StyledLine("State: " + ThreadDumpAnalyzer.stateLabel(thread.state()), LineTone.NORMAL));
        lines.add(new StyledLine("ThreadId: " + nullableNumber(thread.threadId())
            + "  NativeId: " + nullableNumber(thread.nativeId())
            + "  Priority: " + nullableNumber(thread.priority())
            + "  Daemon: " + nullableBoolean(thread.daemon()), LineTone.MUTED));
        lines.add(new StyledLine("CPU: " + nullableDouble(thread.cpuTimeSec())
            + "  Elapsed: " + nullableDouble(thread.elapsedTimeSec())
            + "  Stack depth: " + selectedThread.stackDepth(), LineTone.MUTED));

        if (baseline == null) {
            lines.add(new StyledLine("Comparison: no baseline match for this thread.", LineTone.WARN));
        } else {
            lines.add(new StyledLine("Comparison", LineTone.SECTION));
            lines.add(new StyledLine("Status: " + selectedThread.diffStatus(), diffTone(selectedThread.diffStatus())));
            lines.add(new StyledLine("Baseline state: " + ThreadDumpAnalyzer.stateLabel(baseline.state()), LineTone.MUTED));
            lines.add(new StyledLine("Baseline top frame: " + ThreadDumpAnalyzer.topFrameDisplay(baseline), LineTone.MUTED));
        }

        lines.add(new StyledLine("", LineTone.NORMAL));
        lines.add(new StyledLine("Stack trace", LineTone.SECTION));
        if (thread.stackTrace() == null || thread.stackTrace().isEmpty()) {
            lines.add(new StyledLine("(no stack frames)", LineTone.MUTED));
        } else {
            for (StackFrame frame : thread.stackTrace()) {
                lines.add(new StyledLine("at " + formatFrame(frame), LineTone.NORMAL));
            }
        }

        lines.add(new StyledLine("", LineTone.NORMAL));
        lines.add(new StyledLine("Locks", LineTone.SECTION));
        if (thread.locks() == null || thread.locks().isEmpty()) {
            lines.add(new StyledLine("(no lock information)", LineTone.MUTED));
        } else {
            for (LockInfo lock : thread.locks()) {
                LineTone tone = lock.isBlocking() ? LineTone.WARN : LineTone.NORMAL;
                lines.add(new StyledLine(lock.toString(), tone));
            }
        }

        return lines;
    }

    private List<StyledLine> buildComparisonLines(
        ThreadDumpSnapshot currentSnapshot,
        ThreadDumpSnapshot baselineSnapshot,
        ThreadDumpAnalyzer.SnapshotDiff diff
    ) {
        List<StyledLine> lines = new ArrayList<>();
        if (baselineSnapshot == null || currentSnapshot == null || diff == null) {
            lines.add(new StyledLine("No baseline selected.", LineTone.TITLE));
            lines.add(new StyledLine("In the Snapshots pane, press [b] to mark baseline.", LineTone.MUTED));
            if (!warnings.isEmpty()) {
                lines.add(new StyledLine("", LineTone.NORMAL));
                lines.add(new StyledLine("Warnings", LineTone.SECTION));
                for (String warning : warnings) {
                    lines.add(new StyledLine("- " + warning, LineTone.WARN));
                }
            }
            return lines;
        }

        lines.add(new StyledLine("Baseline: " + baselineSnapshot.displayName(), LineTone.TITLE));
        lines.add(new StyledLine("Current: " + currentSnapshot.displayName(), LineTone.TITLE));
        lines.add(new StyledLine("", LineTone.NORMAL));
        lines.add(new StyledLine("Thread counts", LineTone.SECTION));
        lines.add(new StyledLine("baseline=" + diff.baselineThreadCount()
            + " current=" + diff.currentThreadCount()
            + " delta=" + signed(diff.currentThreadCount() - diff.baselineThreadCount()), LineTone.NORMAL));
        lines.add(new StyledLine("added=" + diff.addedThreads()
            + " removed=" + diff.removedThreads()
            + " changedState=" + diff.changedStateThreads()
            + " changedTopFrame=" + diff.changedTopFrameThreads(), LineTone.NORMAL));
        lines.add(new StyledLine("deadlock delta=" + signed(diff.deadlockDelta()), toneForSigned(diff.deadlockDelta())));

        lines.add(new StyledLine("", LineTone.NORMAL));
        lines.add(new StyledLine("State deltas", LineTone.SECTION));
        if (diff.stateDelta().isEmpty()) {
            lines.add(new StyledLine("(no state deltas)", LineTone.MUTED));
        } else {
            diff.stateDelta().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .forEach(entry -> lines.add(new StyledLine(
                    entry.getKey().name() + " " + signed(entry.getValue()),
                    toneForSigned(entry.getValue())
                )));
        }

        lines.add(new StyledLine("", LineTone.NORMAL));
        lines.add(new StyledLine("Top added stack roots", LineTone.SECTION));
        if (diff.addedTopFrames().isEmpty()) {
            lines.add(new StyledLine("(none)", LineTone.MUTED));
        } else {
            diff.addedTopFrames().stream().limit(12)
                .forEach(delta -> lines.add(new StyledLine("+" + delta.delta() + " " + delta.frame(), LineTone.GOOD)));
        }

        lines.add(new StyledLine("", LineTone.NORMAL));
        lines.add(new StyledLine("Top removed stack roots", LineTone.SECTION));
        if (diff.removedTopFrames().isEmpty()) {
            lines.add(new StyledLine("(none)", LineTone.MUTED));
        } else {
            diff.removedTopFrames().stream().limit(12)
                .forEach(delta -> lines.add(new StyledLine("-" + delta.delta() + " " + delta.frame(), LineTone.BAD)));
        }

        if (!warnings.isEmpty()) {
            lines.add(new StyledLine("", LineTone.NORMAL));
            lines.add(new StyledLine("Load warnings", LineTone.SECTION));
            for (String warning : warnings) {
                lines.add(new StyledLine("- " + warning, LineTone.WARN));
            }
        }

        return lines;
    }

    private String detailTitle(ThreadDumpAnalyzer.ThreadView selectedThread) {
        if (selectedThread == null) {
            return "Thread detail";
        }
        return "Thread detail: " + abbreviateByWidth(safeThreadName(selectedThread.thread()), 32);
    }

    private String comparisonTitle(ThreadDumpSnapshot baselineSnapshot) {
        if (baselineSnapshot == null) {
            return "Comparison (set baseline)";
        }
        return "Comparison vs " + abbreviateByWidth(baselineSnapshot.displayName(), 24);
    }

    private EventResult handleKeyEvent(KeyEvent event) {
        if (event.isCtrlC()) {
            quit();
            return EventResult.HANDLED;
        }
        if (searchInputMode) {
            return handleSearchInput(event);
        }
        if (event.isQuit() || event.isCharIgnoreCase('q')) {
            quit();
            return EventResult.HANDLED;
        }
        if (event.isChar('/')) {
            searchInputMode = true;
            return EventResult.HANDLED;
        }
        if (event.hasCtrl() && event.isCharIgnoreCase('r')) {
            regexSearch = !regexSearch;
            selectedThreadIndex = 0;
            return EventResult.HANDLED;
        }
        if (event.isCharIgnoreCase('x')) {
            searchQuery = "";
            selectedThreadIndex = 0;
            return EventResult.HANDLED;
        }

        if (event.isLeft()) {
            focusPane = focusPane.previous();
            return EventResult.HANDLED;
        }
        if (event.isRight()) {
            focusPane = focusPane.next();
            return EventResult.HANDLED;
        }
        if (event.isCharIgnoreCase('f')) {
            filter = filter.next();
            selectedThreadIndex = 0;
            detailCursor = 0;
            comparisonCursor = 0;
            return EventResult.HANDLED;
        }
        if (event.isCharIgnoreCase('s')) {
            sort = sort.next();
            selectedThreadIndex = 0;
            detailCursor = 0;
            comparisonCursor = 0;
            return EventResult.HANDLED;
        }
        if (event.isCharIgnoreCase('b')) {
            if (!snapshots.isEmpty()) {
                baselineSnapshotIndex = selectedSnapshotIndex;
                detailTab = DetailTab.COMPARISON;
            }
            return EventResult.HANDLED;
        }
        if (event.isCharIgnoreCase('r')) {
            reloadSnapshots();
            return EventResult.HANDLED;
        }
        if (event.isCharIgnoreCase('c')) {
            detailTab = detailTab == DetailTab.THREAD ? DetailTab.COMPARISON : DetailTab.THREAD;
            return EventResult.HANDLED;
        }
        if (event.isChar('1')) {
            detailTab = DetailTab.THREAD;
            return EventResult.HANDLED;
        }
        if (event.isChar('2')) {
            detailTab = DetailTab.COMPARISON;
            return EventResult.HANDLED;
        }
        if (event.isChar('[')) {
            focusPane = focusPane.previous();
            return EventResult.HANDLED;
        }
        if (event.isChar(']')) {
            focusPane = focusPane.next();
            return EventResult.HANDLED;
        }

        if (event.isHome()) {
            navigateHome();
            return EventResult.HANDLED;
        }
        if (event.isEnd()) {
            navigateEnd();
            return EventResult.HANDLED;
        }
        if (event.isPageUp()) {
            moveSelection(-MOVE_PAGE);
            return EventResult.HANDLED;
        }
        if (event.isPageDown()) {
            moveSelection(MOVE_PAGE);
            return EventResult.HANDLED;
        }
        if (event.isUp()) {
            moveSelection(-1);
            return EventResult.HANDLED;
        }
        if (event.isDown()) {
            moveSelection(1);
            return EventResult.HANDLED;
        }
        if (event.isConfirm() && focusPane == FocusPane.SNAPSHOTS && !snapshots.isEmpty()) {
            baselineSnapshotIndex = selectedSnapshotIndex;
            detailTab = DetailTab.COMPARISON;
            return EventResult.HANDLED;
        }
        if (event.isConfirm() && focusPane == FocusPane.THREADS) {
            detailTab = DetailTab.THREAD;
            focusPane = FocusPane.DETAILS;
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleSearchInput(KeyEvent event) {
        if (event.isCancel()) {
            searchInputMode = false;
            return EventResult.HANDLED;
        }
        if (event.isConfirm()) {
            searchInputMode = false;
            selectedThreadIndex = 0;
            return EventResult.HANDLED;
        }
        if (event.hasCtrl() && event.isCharIgnoreCase('r')) {
            regexSearch = !regexSearch;
            selectedThreadIndex = 0;
            return EventResult.HANDLED;
        }
        if (event.hasCtrl() && event.isCharIgnoreCase('u')) {
            searchQuery = "";
            selectedThreadIndex = 0;
            return EventResult.HANDLED;
        }
        if (event.isDeleteBackward()) {
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                selectedThreadIndex = 0;
            }
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.CHAR && !event.hasCtrl() && !event.hasAlt()) {
            char c = event.character();
            if (c >= 32 && c != 127) {
                searchQuery = searchQuery + c;
                selectedThreadIndex = 0;
            }
            return EventResult.HANDLED;
        }
        return EventResult.HANDLED;
    }

    private EventResult handleMouseEvent(MouseEvent event) {
        if (searchInputMode && event.isPress() && event.isLeftButton()) {
            searchInputMode = false;
        }
        FocusPane hovered = paneAt(event.x(), event.y());
        if (hovered == null) {
            return EventResult.UNHANDLED;
        }

        if (event.kind() == MouseEventKind.PRESS && event.isLeftButton()) {
            focusPane = hovered;
            switch (hovered) {
                case SNAPSHOTS -> {
                    int row = rowIndex(areaById("snapshot-list"), event.y(), 2);
                    if (row >= 0) {
                        int next = clampIndex(row, snapshots.size());
                        if (next != selectedSnapshotIndex) {
                            selectedSnapshotIndex = next;
                            selectedThreadIndex = 0;
                            detailCursor = 0;
                            comparisonCursor = 0;
                        }
                    }
                }
                case THREADS -> {
                    List<ThreadDumpAnalyzer.ThreadView> views = currentThreadViews();
                    int row = rowIndex(areaById("thread-list"), event.y(), 2);
                    if (row >= 0) {
                        selectedThreadIndex = clampIndex(row, views.size());
                        detailCursor = 0;
                    }
                }
                case DETAILS -> {
                    int row = rowIndex(areaById("detail-list"), event.y(), 1);
                    if (row >= 0) {
                        int size = activeDetailLines().size();
                        if (detailTab == DetailTab.THREAD) {
                            detailCursor = clampIndex(row, size);
                        } else {
                            comparisonCursor = clampIndex(row, size);
                        }
                    }
                }
            }
            return EventResult.HANDLED;
        }

        if (event.kind() == MouseEventKind.SCROLL_UP || event.kind() == MouseEventKind.SCROLL_DOWN) {
            int delta = event.kind() == MouseEventKind.SCROLL_DOWN ? 3 : -3;
            switch (hovered) {
                case SNAPSHOTS -> {
                    int next = clampIndex(selectedSnapshotIndex + delta, snapshots.size());
                    if (next != selectedSnapshotIndex) {
                        selectedSnapshotIndex = next;
                        selectedThreadIndex = 0;
                        detailCursor = 0;
                        comparisonCursor = 0;
                    }
                }
                case THREADS -> {
                    int total = currentThreadViews().size();
                    selectedThreadIndex = clampIndex(selectedThreadIndex + delta, total);
                    detailCursor = 0;
                }
                case DETAILS -> {
                    int total = activeDetailLines().size();
                    if (detailTab == DetailTab.THREAD) {
                        detailCursor = clampIndex(detailCursor + delta, total);
                    } else {
                        comparisonCursor = clampIndex(comparisonCursor + delta, total);
                    }
                }
            }
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    private void moveSelection(int delta) {
        if (delta == 0) {
            return;
        }
        switch (focusPane) {
            case SNAPSHOTS -> {
                if (snapshots.isEmpty()) {
                    return;
                }
                int newIndex = clampIndex(selectedSnapshotIndex + delta, snapshots.size());
                if (newIndex != selectedSnapshotIndex) {
                    selectedSnapshotIndex = newIndex;
                    selectedThreadIndex = 0;
                    detailCursor = 0;
                    comparisonCursor = 0;
                }
            }
            case THREADS -> {
                int total = currentThreadViews().size();
                selectedThreadIndex = clampIndex(selectedThreadIndex + delta, total);
                detailCursor = 0;
            }
            case DETAILS -> {
                int total = activeDetailLines().size();
                if (detailTab == DetailTab.THREAD) {
                    detailCursor = clampIndex(detailCursor + delta, total);
                } else {
                    comparisonCursor = clampIndex(comparisonCursor + delta, total);
                }
            }
        }
    }

    private void navigateHome() {
        switch (focusPane) {
            case SNAPSHOTS -> selectedSnapshotIndex = clampIndex(0, snapshots.size());
            case THREADS -> selectedThreadIndex = clampIndex(0, currentThreadViews().size());
            case DETAILS -> {
                if (detailTab == DetailTab.THREAD) {
                    detailCursor = 0;
                } else {
                    comparisonCursor = 0;
                }
            }
        }
    }

    private void navigateEnd() {
        switch (focusPane) {
            case SNAPSHOTS -> selectedSnapshotIndex = clampIndex(Integer.MAX_VALUE, snapshots.size());
            case THREADS -> selectedThreadIndex = clampIndex(Integer.MAX_VALUE, currentThreadViews().size());
            case DETAILS -> {
                int size = activeDetailLines().size();
                if (detailTab == DetailTab.THREAD) {
                    detailCursor = clampIndex(Integer.MAX_VALUE, size);
                } else {
                    comparisonCursor = clampIndex(Integer.MAX_VALUE, size);
                }
            }
        }
    }

    private List<ThreadDumpAnalyzer.ThreadView> currentThreadViews() {
        ThreadDumpSnapshot current = currentSnapshot();
        ThreadDumpSnapshot baseline = effectiveBaselineSnapshot(current);
        List<ThreadDumpAnalyzer.ThreadView> base = ThreadDumpAnalyzer.buildThreadViews(current, baseline, filter, sort);
        return ThreadDumpAnalyzer.applySearch(base, searchCriteria());
    }

    private List<StyledLine> activeDetailLines() {
        ThreadDumpSnapshot current = currentSnapshot();
        ThreadDumpSnapshot baseline = effectiveBaselineSnapshot(current);
        List<ThreadDumpAnalyzer.ThreadView> views = currentThreadViews();
        ThreadDumpAnalyzer.ThreadView selected = views.isEmpty()
            ? null
            : views.get(clampIndex(selectedThreadIndex, views.size()));
        if (detailTab == DetailTab.THREAD) {
            return buildThreadDetailLines(selected);
        }
        ThreadDumpAnalyzer.SnapshotDiff diff = baseline == null || current == null
            ? null
            : ThreadDumpAnalyzer.compare(baseline, current);
        return buildComparisonLines(current, baseline, diff);
    }

    private void reloadSnapshots() {
        String selectedSnapshotId = currentSnapshot() == null ? null : currentSnapshot().snapshotId();
        String baselineSnapshotId = baselineSnapshot() == null ? null : baselineSnapshot().snapshotId();
        ThreadDumpSnapshotLoader.LoadResult reload = ThreadDumpSnapshotLoader.load(inputArgs);
        applyLoadResult(reload);
        restoreSnapshotSelection(selectedSnapshotId, baselineSnapshotId);
    }

    private void applyLoadResult(ThreadDumpSnapshotLoader.LoadResult loadResult) {
        snapshots.clear();
        snapshots.addAll(loadResult.snapshots());
        warnings.clear();
        warnings.addAll(loadResult.warnings());
        usingEmbeddedSamples = loadResult.usingEmbeddedSamples();
        clampSnapshotSelection();
    }

    private void restoreSnapshotSelection(String selectedSnapshotId, String baselineSnapshotId) {
        if (selectedSnapshotId != null) {
            int restored = indexBySnapshotId(selectedSnapshotId);
            if (restored >= 0) {
                selectedSnapshotIndex = restored;
            }
        }
        if (baselineSnapshotId != null) {
            baselineSnapshotIndex = indexBySnapshotId(baselineSnapshotId);
        }
        clampSnapshotSelection();
    }

    private int indexBySnapshotId(String snapshotId) {
        for (int i = 0; i < snapshots.size(); i++) {
            if (Objects.equals(snapshots.get(i).snapshotId(), snapshotId)) {
                return i;
            }
        }
        return -1;
    }

    private ThreadDumpSnapshot currentSnapshot() {
        if (snapshots.isEmpty()) {
            return null;
        }
        return snapshots.get(clampIndex(selectedSnapshotIndex, snapshots.size()));
    }

    private ThreadDumpSnapshot baselineSnapshot() {
        if (baselineSnapshotIndex < 0 || baselineSnapshotIndex >= snapshots.size()) {
            return null;
        }
        return snapshots.get(baselineSnapshotIndex);
    }

    private ThreadDumpSnapshot effectiveBaselineSnapshot(ThreadDumpSnapshot currentSnapshot) {
        ThreadDumpSnapshot baseline = baselineSnapshot();
        if (baseline == null || currentSnapshot == null || baseline == currentSnapshot) {
            return null;
        }
        return baseline;
    }

    private void clampSnapshotSelection() {
        selectedSnapshotIndex = clampIndex(selectedSnapshotIndex, snapshots.size());
        if (baselineSnapshotIndex >= snapshots.size()) {
            baselineSnapshotIndex = -1;
        }
        if (snapshots.isEmpty()) {
            baselineSnapshotIndex = -1;
            selectedThreadIndex = 0;
            detailCursor = 0;
            comparisonCursor = 0;
        }
    }

    private ThreadDumpAnalyzer.SearchCriteria searchCriteria() {
        return ThreadDumpAnalyzer.searchCriteria(searchQuery, regexSearch);
    }

    private String searchPrompt() {
        if (searchInputMode) {
            return " search: " + abbreviateByWidth(searchQuery + "_", 26) + " ";
        }
        if (searchQuery.isBlank()) {
            return " search: <none> ";
        }
        String mode = regexSearch ? "re" : "txt";
        return " search(" + mode + "): " + abbreviateByWidth(searchQuery, 20) + " ";
    }

    private StyledElement<?> highlightedText(
        String value,
        ThreadDumpAnalyzer.SearchCriteria criteria,
        boolean dimBase
    ) {
        MatchSpan span = matchSpan(value, criteria);
        if (span == null) {
            return dimBase ? text(value).dim() : text(value);
        }
        List<Element> segments = new ArrayList<>(3);
        if (span.start() > 0) {
            segments.add(dimBase ? text(value.substring(0, span.start())).dim() : text(value.substring(0, span.start())));
        }
        segments.add(text(value.substring(span.start(), span.end())).yellow().underlined().bold());
        if (span.end() < value.length()) {
            segments.add(dimBase ? text(value.substring(span.end())).dim() : text(value.substring(span.end())));
        }
        return row(segments.toArray(Element[]::new));
    }

    private MatchSpan matchSpan(String value, ThreadDumpAnalyzer.SearchCriteria criteria) {
        if (value == null || value.isEmpty() || criteria == null || !criteria.active() || !criteria.valid()) {
            return null;
        }
        if (criteria.regex()) {
            var matcher = criteria.pattern().matcher(value);
            if (matcher.find() && matcher.end() > matcher.start()) {
                return new MatchSpan(matcher.start(), matcher.end());
            }
            return null;
        }
        String search = criteria.query().toLowerCase(Locale.ROOT);
        int index = value.toLowerCase(Locale.ROOT).indexOf(search);
        if (index < 0) {
            return null;
        }
        return new MatchSpan(index, index + search.length());
    }

    private boolean matchesText(String value, ThreadDumpAnalyzer.SearchCriteria criteria) {
        return matchSpan(value, criteria) != null;
    }

    private MatchScope searchMatchScope(ThreadDumpAnalyzer.ThreadView view, ThreadDumpAnalyzer.SearchCriteria criteria) {
        if (criteria == null || !criteria.active() || !criteria.valid()) {
            return MatchScope.NONE;
        }
        ThreadInfo thread = view.thread();
        if (matchesText(safeThreadName(thread), criteria)) {
            return MatchScope.NAME;
        }
        if (matchesText(ThreadDumpAnalyzer.stateLabel(thread.state()), criteria)) {
            return MatchScope.STATE;
        }
        if (matchesText(view.topFrameDisplay(), criteria)) {
            return MatchScope.FRAME;
        }
        if (thread.stackTrace() != null) {
            for (int i = 1; i < thread.stackTrace().size(); i++) {
                if (matchesText(formatFrame(thread.stackTrace().get(i)), criteria)) {
                    return MatchScope.STACK;
                }
            }
        }
        if (thread.locks() != null) {
            for (LockInfo lock : thread.locks()) {
                if (matchesText(lock.toString(), criteria)) {
                    return MatchScope.LOCKS;
                }
            }
        }
        if (matchesText(thread.additionalInfo(), criteria)) {
            return MatchScope.INFO;
        }
        return MatchScope.NONE;
    }

    private FocusPane paneAt(int x, int y) {
        Rect snapshotArea = areaById("snapshot-list");
        if (snapshotArea != null && snapshotArea.contains(x, y)) {
            return FocusPane.SNAPSHOTS;
        }
        Rect threadArea = areaById("thread-list");
        if (threadArea != null && threadArea.contains(x, y)) {
            return FocusPane.THREADS;
        }
        Rect detailArea = areaById("detail-list");
        if (detailArea != null && detailArea.contains(x, y)) {
            return FocusPane.DETAILS;
        }
        return null;
    }

    private Rect areaById(String elementId) {
        if (runner() == null || runner().elementRegistry() == null) {
            return null;
        }
        return runner().elementRegistry().getArea(elementId);
    }

    private static int rowIndex(Rect area, int mouseY, int rowHeight) {
        if (area == null || rowHeight <= 0) {
            return -1;
        }
        int innerTop = area.top() + 1;
        int innerBottom = area.bottom() - 1;
        if (mouseY < innerTop || mouseY >= innerBottom) {
            return -1;
        }
        return (mouseY - innerTop) / rowHeight;
    }

    private static int clampIndex(int index, int size) {
        if (size <= 0) {
            return 0;
        }
        if (index < 0) {
            return 0;
        }
        return Math.min(index, size - 1);
    }

    private static Color stateColor(Thread.State state) {
        if (state == null) {
            return Color.GRAY;
        }
        return switch (state) {
            case RUNNABLE -> Color.GREEN;
            case BLOCKED -> Color.YELLOW;
            case WAITING -> Color.MAGENTA;
            case TIMED_WAITING -> Color.CYAN;
            case NEW -> Color.BLUE;
            case TERMINATED -> Color.DARK_GRAY;
        };
    }

    private static String safeThreadName(ThreadInfo thread) {
        if (thread.name() == null || thread.name().isBlank()) {
            return "<unnamed>";
        }
        return thread.name();
    }

    private static String nullableBoolean(Boolean value) {
        if (value == null) {
            return "n/a";
        }
        return Boolean.TRUE.equals(value) ? "true" : "false";
    }

    private static String nullableNumber(Number value) {
        return value == null ? "n/a" : value.toString();
    }

    private static String nullableDouble(Double value) {
        return value == null ? "n/a" : String.format("%.4fs", value);
    }

    private static String formatFrame(StackFrame frame) {
        if (frame == null) {
            return "(unknown)";
        }
        String className = frame.className() == null ? "?" : frame.className();
        String methodName = frame.methodName() == null ? "?" : frame.methodName();
        if (Boolean.TRUE.equals(frame.nativeMethod())) {
            return className + "." + methodName + "(Native Method)";
        }
        if (frame.fileName() != null && frame.lineNumber() != null) {
            return className + "." + methodName + "(" + frame.fileName() + ":" + frame.lineNumber() + ")";
        }
        if (frame.fileName() != null) {
            return className + "." + methodName + "(" + frame.fileName() + ")";
        }
        return className + "." + methodName;
    }

    private static String abbreviateByWidth(String value, int maxWidth) {
        if (value == null) {
            return "";
        }
        if (maxWidth <= 0) {
            return "";
        }
        if (CharWidth.of(value) <= maxWidth) {
            return value;
        }
        if (maxWidth <= 3) {
            return CharWidth.substringByWidth(value, maxWidth);
        }
        return CharWidth.substringByWidth(value, maxWidth - 3) + "...";
    }

    private static String signed(int value) {
        if (value > 0) {
            return "+" + value;
        }
        return Integer.toString(value);
    }

    private static Color colorForSigned(int value) {
        if (value > 0) {
            return Color.GREEN;
        }
        if (value < 0) {
            return Color.RED;
        }
        return Color.GRAY;
    }

    private static LineTone toneForSigned(int value) {
        if (value > 0) {
            return LineTone.GOOD;
        }
        if (value < 0) {
            return LineTone.BAD;
        }
        return LineTone.MUTED;
    }

    private static LineTone diffTone(ThreadDumpAnalyzer.ThreadDiffStatus status) {
        return switch (status) {
            case NEW -> LineTone.GOOD;
            case CHANGED -> LineTone.WARN;
            case SAME -> LineTone.MUTED;
        };
    }

    private enum FocusPane {
        SNAPSHOTS("snapshots"),
        THREADS("threads"),
        DETAILS("details");

        private final String label;

        FocusPane(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        FocusPane next() {
            FocusPane[] panes = values();
            return panes[(ordinal() + 1) % panes.length];
        }

        FocusPane previous() {
            FocusPane[] panes = values();
            return panes[(ordinal() + panes.length - 1) % panes.length];
        }
    }

    private enum DetailTab {
        THREAD,
        COMPARISON
    }

    private enum LineTone {
        TITLE,
        SECTION,
        NORMAL,
        MUTED,
        GOOD,
        BAD,
        WARN
    }

    private enum MatchScope {
        NONE(""),
        NAME("name"),
        STATE("state"),
        FRAME("frame"),
        STACK("stack"),
        LOCKS("locks"),
        INFO("info");

        private final String label;

        MatchScope(String label) {
            this.label = label;
        }
    }

    private record StyledLine(String text, LineTone tone) {
    }

    private record SnapshotRow(int index, ThreadDumpSnapshot snapshot) {
    }

    private record MatchSpan(int start, int end) {
    }
}
