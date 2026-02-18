/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.threaddump;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import me.bechberger.jthreaddump.model.ThreadDump;

/**
 * A parsed thread dump snapshot and pre-computed statistics for rendering.
 */
record ThreadDumpSnapshot(
    String snapshotId,
    String sourceLabel,
    int sequenceInSource,
    ThreadDump dump,
    SnapshotStats stats
) {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    static ThreadDumpSnapshot of(String sourceLabel, int sequenceInSource, ThreadDump dump) {
        String snapshotId = sourceLabel + "#" + sequenceInSource;
        return new ThreadDumpSnapshot(snapshotId, sourceLabel, sequenceInSource, dump, SnapshotStats.from(dump));
    }

    String displayName() {
        if (sequenceInSource <= 1) {
            return sourceLabel;
        }
        return sourceLabel + " [" + sequenceInSource + "]";
    }

    String timestampLabel() {
        if (dump.timestamp() == null) {
            return "n/a";
        }
        return TIME_FORMAT.format(dump.timestamp().atZone(ZoneId.systemDefault()));
    }
}
