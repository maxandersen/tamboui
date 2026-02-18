/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.threaddump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadDumpSnapshotLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadEmbeddedSnapshotsWhenNoInputIsProvided() {
        ThreadDumpSnapshotLoader.LoadResult result = ThreadDumpSnapshotLoader.load(List.of());

        assertThat(result.usingEmbeddedSamples()).isTrue();
        assertThat(result.snapshots()).hasSize(2);
        assertThat(result.snapshots().get(0).stats().totalThreads()).isEqualTo(3);
        assertThat(result.snapshots().get(1).stats().totalThreads()).isEqualTo(3);
    }

    @Test
    void shouldParseMultipleSnapshotsFromSingleFile() throws IOException {
        Path dumpFile = tempDir.resolve("captured-thread-dumps.log");
        Files.writeString(dumpFile, ThreadDumpTestData.SAMPLE_DUMP_1 + "\n\n" + ThreadDumpTestData.SAMPLE_DUMP_2);

        ThreadDumpSnapshotLoader.LoadResult result = ThreadDumpSnapshotLoader.load(List.of(dumpFile.toString()));

        assertThat(result.snapshots()).hasSize(2);
        assertThat(result.warnings()).isEmpty();
        assertThat(result.snapshots().get(0).sourceLabel()).contains("captured-thread-dumps.log");
        assertThat(result.snapshots().get(1).stats().runnable()).isEqualTo(2);
    }

    @Test
    void shouldWarnWhenInputDoesNotExist() {
        ThreadDumpSnapshotLoader.LoadResult result = ThreadDumpSnapshotLoader.load(List.of("definitely-missing-dump.log"));

        assertThat(result.snapshots()).isEmpty();
        assertThat(result.warnings()).isNotEmpty();
        assertThat(result.warnings().get(0)).contains("does not exist");
    }

    @Test
    void shouldSplitSectionsOnThreadDumpHeaders() {
        String joined = "noise line\n"
            + ThreadDumpTestData.SAMPLE_DUMP_1
            + "\n\nrandom text\n"
            + ThreadDumpTestData.SAMPLE_DUMP_2;

        List<String> sections = ThreadDumpSnapshotLoader.splitDumpSections(joined);

        assertThat(sections).hasSize(2);
        assertThat(sections.get(0)).contains("Full thread dump");
        assertThat(sections.get(1)).contains("\"http-acceptor\"");
    }
}
