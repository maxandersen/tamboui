///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//DEPS me.bechberger:jthreaddump:0.5.8
//SOURCES ThreadDumpSnapshot.java SnapshotStats.java ThreadDumpSnapshotLoader.java ThreadDumpAnalyzer.java ThreadDumpReviewApp.java
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.threaddump;

import java.util.List;

/**
 * Interactive thread dump review demo.
 * <p>
 * Features:
 * <ul>
 *   <li>Load one or many thread dump files/directories</li>
 *   <li>Browse snapshot timelines and per-thread stacks</li>
 *   <li>Set a baseline snapshot and compare changes</li>
 *   <li>Filter/sort threads by state, diff status, CPU, and depth</li>
 * </ul>
 *
 * <p>Usage examples:
 * <pre>{@code
 * ./run-demo.sh thread-dump-review-demo
 * ./run-demo.sh thread-dump-review-demo -- /tmp/dumps
 * ./run-demo.sh thread-dump-review-demo -- dump1.log dump2.log
 * }</pre>
 */
public final class ThreadDumpReviewDemo {

    private ThreadDumpReviewDemo() {
    }

    /**
     * Demo entry point.
     *
     * @param args optional paths to files/directories with thread dumps
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        List<String> inputArgs = List.of(args);
        ThreadDumpSnapshotLoader.LoadResult initialData = ThreadDumpSnapshotLoader.load(inputArgs);
        new ThreadDumpReviewApp(inputArgs, initialData).run();
    }
}
