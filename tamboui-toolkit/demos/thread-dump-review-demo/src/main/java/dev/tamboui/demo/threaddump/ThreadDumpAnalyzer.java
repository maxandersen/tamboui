/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.threaddump;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import me.bechberger.jthreaddump.model.StackFrame;
import me.bechberger.jthreaddump.model.ThreadInfo;

/**
 * Analysis and comparison utilities for thread dump snapshots.
 */
final class ThreadDumpAnalyzer {

    private ThreadDumpAnalyzer() {
    }

    enum ThreadFilter {
        ALL("All"),
        RUNNABLE("Runnable"),
        BLOCKED("Blocked"),
        WAITING("Waiting"),
        TIMED_WAITING("Timed waiting"),
        CONTENDED("Contended"),
        NEW_THREADS("New vs baseline"),
        CHANGED_THREADS("Changed vs baseline");

        private final String label;

        ThreadFilter(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        ThreadFilter next() {
            ThreadFilter[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    enum ThreadSort {
        CPU_DESC("CPU"),
        NAME("Name"),
        STATE("State"),
        STACK_DEPTH("Depth"),
        DIFF_STATUS("Diff");

        private final String label;

        ThreadSort(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        ThreadSort next() {
            ThreadSort[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    enum ThreadDiffStatus {
        SAME,
        CHANGED,
        NEW
    }

    record ThreadView(
        ThreadInfo thread,
        ThreadInfo baselineThread,
        ThreadDiffStatus diffStatus,
        String topFrameKey,
        String topFrameDisplay,
        int stackDepth
    ) {
        boolean isNew() {
            return diffStatus == ThreadDiffStatus.NEW;
        }

        boolean isChanged() {
            return diffStatus == ThreadDiffStatus.CHANGED;
        }
    }

    record FrameDelta(String frame, int delta) {
    }

    record SnapshotDiff(
        int baselineThreadCount,
        int currentThreadCount,
        int addedThreads,
        int removedThreads,
        int changedStateThreads,
        int changedTopFrameThreads,
        int deadlockDelta,
        Map<Thread.State, Integer> stateDelta,
        List<FrameDelta> addedTopFrames,
        List<FrameDelta> removedTopFrames
    ) {
        SnapshotDiff {
            stateDelta = Map.copyOf(stateDelta);
            addedTopFrames = List.copyOf(addedTopFrames);
            removedTopFrames = List.copyOf(removedTopFrames);
        }
    }

    record SearchCriteria(String query, boolean regex, Pattern pattern, String error) {
        boolean active() {
            return query != null && !query.isBlank();
        }

        boolean valid() {
            return error == null;
        }
    }

    static List<ThreadView> buildThreadViews(
        ThreadDumpSnapshot currentSnapshot,
        ThreadDumpSnapshot baselineSnapshot,
        ThreadFilter filter,
        ThreadSort sort
    ) {
        if (currentSnapshot == null) {
            return List.of();
        }

        Map<String, ArrayDeque<ThreadInfo>> baselineIndex = baselineSnapshot == null
            ? Map.of()
            : indexByLogicalId(baselineSnapshot.dump().threads());

        List<ThreadView> views = new ArrayList<>(currentSnapshot.dump().threads().size());
        for (ThreadInfo thread : currentSnapshot.dump().threads()) {
            ThreadInfo baselineThread = pollFirst(baselineIndex, logicalId(thread));
            String topFrameKey = topFrameKey(thread);
            String topFrameDisplay = topFrameDisplay(thread);
            int depth = thread.stackTrace() == null ? 0 : thread.stackTrace().size();
            ThreadDiffStatus status = determineDiffStatus(thread, baselineThread);
            ThreadView view = new ThreadView(thread, baselineThread, status, topFrameKey, topFrameDisplay, depth);
            if (matchesFilter(view, filter)) {
                views.add(view);
            }
        }

        views.sort(comparator(sort));
        return views;
    }

    static SnapshotDiff compare(ThreadDumpSnapshot baselineSnapshot, ThreadDumpSnapshot currentSnapshot) {
        if (baselineSnapshot == null || currentSnapshot == null) {
            return new SnapshotDiff(0, 0, 0, 0, 0, 0, 0, Map.of(), List.of(), List.of());
        }

        Map<String, ArrayDeque<ThreadInfo>> baselineIndex = indexByLogicalId(baselineSnapshot.dump().threads());
        int added = 0;
        int changedState = 0;
        int changedTopFrame = 0;

        for (ThreadInfo currentThread : currentSnapshot.dump().threads()) {
            ThreadInfo baselineThread = pollFirst(baselineIndex, logicalId(currentThread));
            if (baselineThread == null) {
                added++;
                continue;
            }
            if (!Objects.equals(currentThread.state(), baselineThread.state())) {
                changedState++;
            }
            if (!Objects.equals(topFrameKey(currentThread), topFrameKey(baselineThread))) {
                changedTopFrame++;
            }
        }

        int removed = baselineIndex.values().stream().mapToInt(ArrayDeque::size).sum();
        Map<Thread.State, Integer> stateDelta = computeStateDelta(baselineSnapshot, currentSnapshot);
        FrameDeltaData frameDeltaData = computeFrameDeltas(baselineSnapshot, currentSnapshot);

        return new SnapshotDiff(
            baselineSnapshot.stats().totalThreads(),
            currentSnapshot.stats().totalThreads(),
            added,
            removed,
            changedState,
            changedTopFrame,
            currentSnapshot.stats().deadlocks() - baselineSnapshot.stats().deadlocks(),
            stateDelta,
            frameDeltaData.added(),
            frameDeltaData.removed()
        );
    }

    static SearchCriteria searchCriteria(String query, boolean regex) {
        String effectiveQuery = query == null ? "" : query;
        if (effectiveQuery.isBlank()) {
            return new SearchCriteria("", regex, null, null);
        }
        if (!regex) {
            return new SearchCriteria(effectiveQuery, false, null, null);
        }
        try {
            Pattern compiled = Pattern.compile(effectiveQuery, Pattern.CASE_INSENSITIVE);
            return new SearchCriteria(effectiveQuery, true, compiled, null);
        } catch (PatternSyntaxException e) {
            return new SearchCriteria(effectiveQuery, true, null, e.getDescription());
        }
    }

    static List<ThreadView> applySearch(List<ThreadView> views, SearchCriteria criteria) {
        if (views.isEmpty()) {
            return views;
        }
        if (criteria == null || !criteria.active() || !criteria.valid()) {
            return views;
        }
        List<ThreadView> filtered = new ArrayList<>();
        for (ThreadView view : views) {
            if (matchesSearch(view, criteria)) {
                filtered.add(view);
            }
        }
        return filtered;
    }

    private static ThreadDiffStatus determineDiffStatus(ThreadInfo currentThread, ThreadInfo baselineThread) {
        if (baselineThread == null) {
            return ThreadDiffStatus.NEW;
        }
        boolean sameState = Objects.equals(currentThread.state(), baselineThread.state());
        boolean sameFrame = Objects.equals(topFrameKey(currentThread), topFrameKey(baselineThread));
        return sameState && sameFrame ? ThreadDiffStatus.SAME : ThreadDiffStatus.CHANGED;
    }

    private static boolean matchesSearch(ThreadView view, SearchCriteria criteria) {
        String haystack = searchText(view);
        if (criteria.regex()) {
            return criteria.pattern().matcher(haystack).find();
        }
        return haystack.toLowerCase(Locale.ROOT).contains(criteria.query().toLowerCase(Locale.ROOT));
    }

    private static String searchText(ThreadView view) {
        ThreadInfo thread = view.thread();
        StringBuilder text = new StringBuilder(192);
        text.append(safeName(thread.name()));
        text.append('\n').append(stateLabel(thread.state()));
        text.append('\n').append(view.topFrameDisplay());
        text.append('\n').append(view.topFrameKey());
        if (thread.additionalInfo() != null && !thread.additionalInfo().isBlank()) {
            text.append('\n').append(thread.additionalInfo());
        }
        if (thread.stackTrace() != null) {
            for (StackFrame frame : thread.stackTrace()) {
                text.append('\n').append(formatSearchFrame(frame));
            }
        }
        if (thread.locks() != null) {
            thread.locks().forEach(lock -> text.append('\n').append(lock.toString()));
        }
        return text.toString();
    }

    private static String formatSearchFrame(StackFrame frame) {
        String className = frame.className() == null ? "?" : frame.className();
        String methodName = frame.methodName() == null ? "?" : frame.methodName();
        StringBuilder value = new StringBuilder();
        value.append(className).append('.').append(methodName);
        if (Boolean.TRUE.equals(frame.nativeMethod())) {
            value.append(" Native Method");
            return value.toString();
        }
        if (frame.fileName() != null) {
            value.append(' ').append(frame.fileName());
        }
        if (frame.lineNumber() != null) {
            value.append(':').append(frame.lineNumber());
        }
        return value.toString();
    }

    private static boolean matchesFilter(ThreadView view, ThreadFilter filter) {
        if (filter == null || filter == ThreadFilter.ALL) {
            return true;
        }
        Thread.State state = view.thread().state();
        return switch (filter) {
            case ALL -> true;
            case RUNNABLE -> state == Thread.State.RUNNABLE;
            case BLOCKED -> state == Thread.State.BLOCKED;
            case WAITING -> state == Thread.State.WAITING;
            case TIMED_WAITING -> state == Thread.State.TIMED_WAITING;
            case CONTENDED -> state == Thread.State.BLOCKED
                || state == Thread.State.WAITING
                || state == Thread.State.TIMED_WAITING;
            case NEW_THREADS -> view.diffStatus() == ThreadDiffStatus.NEW;
            case CHANGED_THREADS -> view.diffStatus() == ThreadDiffStatus.CHANGED;
        };
    }

    private static Comparator<ThreadView> comparator(ThreadSort sort) {
        Comparator<ThreadView> byName = Comparator.comparing(
            (ThreadView v) -> safeName(v.thread().name()), String.CASE_INSENSITIVE_ORDER);

        return switch (sort) {
            case NAME -> byName;
            case STATE -> Comparator
                .comparing((ThreadView v) -> stateLabel(v.thread().state()))
                .thenComparing(byName);
            case STACK_DEPTH -> Comparator
                .comparingInt(ThreadView::stackDepth)
                .reversed()
                .thenComparing(byName);
            case DIFF_STATUS -> Comparator
                .comparingInt((ThreadView v) -> diffStatusPriority(v.diffStatus()))
                .thenComparing(byName);
            case CPU_DESC -> Comparator
                .comparingDouble((ThreadView v) -> cpuValue(v.thread()))
                .reversed()
                .thenComparing(byName);
        };
    }

    private static int diffStatusPriority(ThreadDiffStatus status) {
        return switch (status) {
            case NEW -> 0;
            case CHANGED -> 1;
            case SAME -> 2;
        };
    }

    private static double cpuValue(ThreadInfo thread) {
        return thread.cpuTimeSec() == null ? -1.0 : thread.cpuTimeSec();
    }

    static String stateLabel(Thread.State state) {
        if (state == null) {
            return "UNKNOWN";
        }
        return state.name();
    }

    static String topFrameDisplay(ThreadInfo thread) {
        if (thread.stackTrace() == null || thread.stackTrace().isEmpty()) {
            return "(no stack)";
        }
        StackFrame frame = thread.stackTrace().get(0);
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

    static String topFrameKey(ThreadInfo thread) {
        if (thread.stackTrace() == null || thread.stackTrace().isEmpty()) {
            return "(no-stack)";
        }
        StackFrame frame = thread.stackTrace().get(0);
        String className = frame.className() == null ? "?" : frame.className();
        String methodName = frame.methodName() == null ? "?" : frame.methodName();
        return (className + "." + methodName).toLowerCase(Locale.ROOT);
    }

    private static Map<String, ArrayDeque<ThreadInfo>> indexByLogicalId(List<ThreadInfo> threads) {
        Map<String, ArrayDeque<ThreadInfo>> index = new HashMap<>();
        for (ThreadInfo thread : threads) {
            index.computeIfAbsent(logicalId(thread), ignored -> new ArrayDeque<>()).add(thread);
        }
        return index;
    }

    private static String logicalId(ThreadInfo thread) {
        String name = safeName(thread.name());
        if (thread.threadId() != null) {
            return name + "#" + thread.threadId();
        }
        return name;
    }

    private static String safeName(String threadName) {
        return threadName == null || threadName.isBlank() ? "<unnamed>" : threadName;
    }

    private static ThreadInfo pollFirst(Map<String, ArrayDeque<ThreadInfo>> index, String key) {
        ArrayDeque<ThreadInfo> queue = index.get(key);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        return queue.removeFirst();
    }

    private static Map<Thread.State, Integer> computeStateDelta(
        ThreadDumpSnapshot baselineSnapshot,
        ThreadDumpSnapshot currentSnapshot
    ) {
        EnumMap<Thread.State, Integer> baseline = new EnumMap<>(Thread.State.class);
        EnumMap<Thread.State, Integer> current = new EnumMap<>(Thread.State.class);

        for (ThreadInfo thread : baselineSnapshot.dump().threads()) {
            if (thread.state() != null) {
                baseline.merge(thread.state(), 1, Integer::sum);
            }
        }
        for (ThreadInfo thread : currentSnapshot.dump().threads()) {
            if (thread.state() != null) {
                current.merge(thread.state(), 1, Integer::sum);
            }
        }

        EnumMap<Thread.State, Integer> delta = new EnumMap<>(Thread.State.class);
        for (Thread.State state : Thread.State.values()) {
            int value = current.getOrDefault(state, 0) - baseline.getOrDefault(state, 0);
            if (value != 0) {
                delta.put(state, value);
            }
        }
        return delta;
    }

    private static FrameDeltaData computeFrameDeltas(
        ThreadDumpSnapshot baselineSnapshot,
        ThreadDumpSnapshot currentSnapshot
    ) {
        Map<String, Integer> baselineFrames = countTopFrames(baselineSnapshot);
        Map<String, Integer> currentFrames = countTopFrames(currentSnapshot);

        Map<String, Integer> mergedKeys = new HashMap<>();
        for (Map.Entry<String, Integer> entry : baselineFrames.entrySet()) {
            mergedKeys.put(entry.getKey(), 0);
        }
        for (Map.Entry<String, Integer> entry : currentFrames.entrySet()) {
            mergedKeys.put(entry.getKey(), 0);
        }

        List<FrameDelta> added = new ArrayList<>();
        List<FrameDelta> removed = new ArrayList<>();
        for (String frame : mergedKeys.keySet()) {
            int delta = currentFrames.getOrDefault(frame, 0) - baselineFrames.getOrDefault(frame, 0);
            if (delta > 0) {
                added.add(new FrameDelta(frame, delta));
            } else if (delta < 0) {
                removed.add(new FrameDelta(frame, -delta));
            }
        }

        added.sort(Comparator.comparingInt(FrameDelta::delta).reversed().thenComparing(FrameDelta::frame));
        removed.sort(Comparator.comparingInt(FrameDelta::delta).reversed().thenComparing(FrameDelta::frame));
        return new FrameDeltaData(added, removed);
    }

    private static Map<String, Integer> countTopFrames(ThreadDumpSnapshot snapshot) {
        Map<String, Integer> counts = new HashMap<>();
        for (ThreadInfo thread : snapshot.dump().threads()) {
            String key = topFrameKey(thread);
            counts.merge(key, 1, Integer::sum);
        }
        return counts;
    }

    private record FrameDeltaData(List<FrameDelta> added, List<FrameDelta> removed) {
    }
}
