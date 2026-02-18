/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.threaddump;

import java.util.EnumMap;
import java.util.Map;

import me.bechberger.jthreaddump.model.ThreadDump;
import me.bechberger.jthreaddump.model.ThreadInfo;

/**
 * Thread-state counters for a single snapshot.
 */
record SnapshotStats(
    int totalThreads,
    int runnable,
    int blocked,
    int waiting,
    int timedWaiting,
    int newlyCreated,
    int terminated,
    int unknown,
    int deadlocks
) {

    static SnapshotStats from(ThreadDump dump) {
        Map<Thread.State, Integer> counts = new EnumMap<>(Thread.State.class);
        int unknownStates = 0;
        for (ThreadInfo thread : dump.threads()) {
            Thread.State state = thread.state();
            if (state == null) {
                unknownStates++;
            } else {
                counts.merge(state, 1, Integer::sum);
            }
        }
        int deadlockCount = dump.deadlockInfos() == null ? 0 : dump.deadlockInfos().size();
        return new SnapshotStats(
            dump.threads().size(),
            counts.getOrDefault(Thread.State.RUNNABLE, 0),
            counts.getOrDefault(Thread.State.BLOCKED, 0),
            counts.getOrDefault(Thread.State.WAITING, 0),
            counts.getOrDefault(Thread.State.TIMED_WAITING, 0),
            counts.getOrDefault(Thread.State.NEW, 0),
            counts.getOrDefault(Thread.State.TERMINATED, 0),
            unknownStates,
            deadlockCount
        );
    }

    int activeContention() {
        return blocked + waiting + timedWaiting;
    }
}
