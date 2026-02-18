/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.threaddump;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import me.bechberger.jthreaddump.model.ThreadDump;
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

    private static ThreadDumpSnapshot snapshot(String source, int sequence, String dumpText) throws IOException {
        ThreadDump parsed = ThreadDumpParser.parse(dumpText);
        return ThreadDumpSnapshot.of(source, sequence, parsed);
    }
}
