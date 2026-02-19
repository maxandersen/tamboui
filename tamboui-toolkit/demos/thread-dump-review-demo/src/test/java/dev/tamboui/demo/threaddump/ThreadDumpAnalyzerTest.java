/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.threaddump;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import me.bechberger.jthreaddump.model.DeadlockInfo;
import me.bechberger.jthreaddump.model.LockInfo;
import me.bechberger.jthreaddump.model.StackFrame;
import me.bechberger.jthreaddump.model.ThreadDump;
import me.bechberger.jthreaddump.model.ThreadInfo;
import me.bechberger.jthreaddump.parser.ThreadDumpParser;

class ThreadDumpAnalyzerTest {

    @Test
    void shouldComputeSnapshotDiffAcrossBaselineAndCurrent() throws IOException {
        ThreadDumpSnapshot baseline = snapshot("baseline", 1, ThreadDumpTestData.SAMPLE_DUMP_1);
        ThreadDumpSnapshot current = snapshot("current", 1, ThreadDumpTestData.SAMPLE_DUMP_2);

        ThreadDumpAnalyzer.SnapshotDiff diff = ThreadDumpAnalyzer.compare(baseline, current);

        assertThat(diff.baselineThreadCount()).isEqualTo(3);
        assertThat(diff.currentThreadCount()).isEqualTo(3);
        assertThat(diff.addedThreads()).isEqualTo(1);
        assertThat(diff.removedThreads()).isEqualTo(1);
        assertThat(diff.changedStateThreads()).isEqualTo(2);
        assertThat(diff.changedTopFrameThreads()).isEqualTo(2);
        assertThat(diff.stateDelta()).containsEntry(Thread.State.RUNNABLE, 1);
        assertThat(diff.stateDelta()).containsEntry(Thread.State.BLOCKED, -1);
    }

    @Test
    void shouldFilterNewAndChangedThreadsAgainstBaseline() throws IOException {
        ThreadDumpSnapshot baseline = snapshot("baseline", 1, ThreadDumpTestData.SAMPLE_DUMP_1);
        ThreadDumpSnapshot current = snapshot("current", 1, ThreadDumpTestData.SAMPLE_DUMP_2);

        List<ThreadDumpAnalyzer.ThreadView> newThreads = ThreadDumpAnalyzer.buildThreadViews(
            current,
            baseline,
            ThreadDumpAnalyzer.ThreadFilter.NEW_THREADS,
            ThreadDumpAnalyzer.ThreadSort.NAME
        );
        List<ThreadDumpAnalyzer.ThreadView> changedThreads = ThreadDumpAnalyzer.buildThreadViews(
            current,
            baseline,
            ThreadDumpAnalyzer.ThreadFilter.CHANGED_THREADS,
            ThreadDumpAnalyzer.ThreadSort.NAME
        );

        assertThat(newThreads).hasSize(1);
        assertThat(newThreads.get(0).thread().name()).isEqualTo("http-acceptor");

        assertThat(changedThreads).hasSize(2);
        assertThat(changedThreads)
            .extracting(view -> view.thread().name())
            .containsExactly("main", "worker-2");
    }

    @Test
    void shouldSortThreadsByCpuDescending() throws IOException {
        ThreadDumpSnapshot baseline = snapshot("baseline", 1, ThreadDumpTestData.SAMPLE_DUMP_1);
        ThreadDumpSnapshot current = snapshot("current", 1, ThreadDumpTestData.SAMPLE_DUMP_2);

        List<ThreadDumpAnalyzer.ThreadView> sorted = ThreadDumpAnalyzer.buildThreadViews(
            current,
            baseline,
            ThreadDumpAnalyzer.ThreadFilter.ALL,
            ThreadDumpAnalyzer.ThreadSort.CPU_DESC
        );

        assertThat(sorted).extracting(view -> view.thread().name())
            .containsExactly("worker-2", "main", "http-acceptor");
    }

    @Test
    void shouldFilterThreadsByPlainTextSearch() throws IOException {
        ThreadDumpSnapshot baseline = snapshot("baseline", 1, ThreadDumpTestData.SAMPLE_DUMP_1);
        ThreadDumpSnapshot current = snapshot("current", 1, ThreadDumpTestData.SAMPLE_DUMP_2);
        List<ThreadDumpAnalyzer.ThreadView> views = ThreadDumpAnalyzer.buildThreadViews(
            current,
            baseline,
            ThreadDumpAnalyzer.ThreadFilter.ALL,
            ThreadDumpAnalyzer.ThreadSort.NAME
        );

        ThreadDumpAnalyzer.SearchCriteria criteria = ThreadDumpAnalyzer.searchCriteria("accept", false);
        List<ThreadDumpAnalyzer.ThreadView> filtered = ThreadDumpAnalyzer.applySearch(views, criteria);

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).thread().name()).isEqualTo("http-acceptor");
    }

    @Test
    void shouldFilterThreadsByRegexSearch() throws IOException {
        ThreadDumpSnapshot baseline = snapshot("baseline", 1, ThreadDumpTestData.SAMPLE_DUMP_1);
        ThreadDumpSnapshot current = snapshot("current", 1, ThreadDumpTestData.SAMPLE_DUMP_2);
        List<ThreadDumpAnalyzer.ThreadView> views = ThreadDumpAnalyzer.buildThreadViews(
            current,
            baseline,
            ThreadDumpAnalyzer.ThreadFilter.ALL,
            ThreadDumpAnalyzer.ThreadSort.NAME
        );

        ThreadDumpAnalyzer.SearchCriteria criteria = ThreadDumpAnalyzer.searchCriteria("worker-\\d", true);
        List<ThreadDumpAnalyzer.ThreadView> filtered = ThreadDumpAnalyzer.applySearch(views, criteria);

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).thread().name()).isEqualTo("worker-2");
    }

    @Test
    void shouldIgnoreInvalidRegexAndReturnUnfilteredView() throws IOException {
        ThreadDumpSnapshot baseline = snapshot("baseline", 1, ThreadDumpTestData.SAMPLE_DUMP_1);
        ThreadDumpSnapshot current = snapshot("current", 1, ThreadDumpTestData.SAMPLE_DUMP_2);
        List<ThreadDumpAnalyzer.ThreadView> views = ThreadDumpAnalyzer.buildThreadViews(
            current,
            baseline,
            ThreadDumpAnalyzer.ThreadFilter.ALL,
            ThreadDumpAnalyzer.ThreadSort.NAME
        );

        ThreadDumpAnalyzer.SearchCriteria criteria = ThreadDumpAnalyzer.searchCriteria("[", true);
        List<ThreadDumpAnalyzer.ThreadView> filtered = ThreadDumpAnalyzer.applySearch(views, criteria);

        assertThat(criteria.valid()).isFalse();
        assertThat(criteria.error()).isNotBlank();
        assertThat(filtered).hasSize(views.size());
    }

    @Test
    void shouldBuildLockGraphFromBlockingRelationships() {
        ThreadInfo owner = thread(
            "owner-thread",
            11L,
            Thread.State.RUNNABLE,
            List.of(lock("0xdead", "java.lang.Object", LockInfo.LockOperation.LOCKED, null))
        );
        ThreadInfo waiter = thread(
            "waiter-thread",
            22L,
            Thread.State.BLOCKED,
            List.of(lock("0xdead", "java.lang.Object", LockInfo.LockOperation.WAITING_TO_LOCK, null))
        );

        ThreadDumpSnapshot snapshot = ThreadDumpSnapshot.of(
            "synthetic",
            1,
            new ThreadDump(null, "synthetic-jvm", List.of(owner, waiter), null, "synthetic", List.of())
        );

        ThreadDumpAnalyzer.LockGraph graph = ThreadDumpAnalyzer.buildLockGraph(snapshot);

        assertThat(graph.contendedLocks()).isEqualTo(1);
        assertThat(graph.unknownOwners()).isZero();
        assertThat(graph.edges()).hasSize(1);
        assertThat(graph.edges().get(0).waitingThread()).isEqualTo("waiter-thread");
        assertThat(graph.edges().get(0).ownerThread()).isEqualTo("owner-thread");
        assertThat(graph.edges().get(0).ownerKnown()).isTrue();
        assertThat(graph.hotspots())
            .extracting(ThreadDumpAnalyzer.LockNode::threadName)
            .contains("owner-thread", "waiter-thread");
    }

    @Test
    void shouldExposeDeadlockCyclesForExplorer() {
        DeadlockInfo.DeadlockedThread threadA = new DeadlockInfo.DeadlockedThread(
            "deadlock-A",
            null,
            "0x1",
            "java.lang.Object",
            "deadlock-B",
            List.of(frame("demo.A", "run")),
            List.of(lock("0x1", "java.lang.Object", LockInfo.LockOperation.WAITING_TO_LOCK, "2"))
        );
        DeadlockInfo.DeadlockedThread threadB = new DeadlockInfo.DeadlockedThread(
            "deadlock-B",
            null,
            "0x2",
            "java.lang.Object",
            "deadlock-A",
            List.of(frame("demo.B", "run")),
            List.of(lock("0x2", "java.lang.Object", LockInfo.LockOperation.WAITING_TO_LOCK, "1"))
        );

        ThreadDumpSnapshot snapshot = ThreadDumpSnapshot.of(
            "synthetic-deadlock",
            1,
            new ThreadDump(
                null,
                "synthetic-jvm",
                List.of(),
                null,
                "synthetic",
                List.of(new DeadlockInfo(List.of(threadA, threadB)))
            )
        );

        ThreadDumpAnalyzer.DeadlockExplorer explorer = ThreadDumpAnalyzer.buildDeadlockExplorer(snapshot);

        assertThat(explorer.cycles()).hasSize(1);
        assertThat(explorer.totalParticipants()).isEqualTo(2);
        assertThat(explorer.cycles().get(0).participants())
            .extracting(ThreadDumpAnalyzer.DeadlockParticipant::threadName)
            .containsExactly("deadlock-A", "deadlock-B");
        assertThat(explorer.cycles().get(0).participants().get(0).topFrame()).contains("demo.A.run");
    }

    private static ThreadInfo thread(String name, long threadId, Thread.State state, List<LockInfo> locks) {
        return new ThreadInfo(
            name,
            threadId,
            null,
            5,
            false,
            state,
            null,
            null,
            List.of(frame("demo." + name.replace('-', '_'), "run")),
            locks,
            ""
        );
    }

    private static LockInfo lock(
        String lockId,
        String className,
        LockInfo.LockOperation operation,
        String ownerThreadId
    ) {
        if (ownerThreadId == null) {
            return new LockInfo(lockId, className, operation);
        }
        return new LockInfo(lockId, className, operation, ownerThreadId);
    }

    private static StackFrame frame(String className, String methodName) {
        return new StackFrame(className, methodName, "Synthetic.java", 1);
    }

    private static ThreadDumpSnapshot snapshot(String source, int sequence, String dumpText) throws IOException {
        ThreadDump parsed = ThreadDumpParser.parse(dumpText);
        return ThreadDumpSnapshot.of(source, sequence, parsed);
    }
}
