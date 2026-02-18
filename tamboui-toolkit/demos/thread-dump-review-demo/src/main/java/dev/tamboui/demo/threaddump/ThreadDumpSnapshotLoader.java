/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.threaddump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import me.bechberger.jthreaddump.model.ThreadDump;
import me.bechberger.jthreaddump.parser.ThreadDumpParser;

/**
 * Loads and parses thread dumps from files/directories.
 */
final class ThreadDumpSnapshotLoader {

    private static final Pattern TIMESTAMP_LINE = Pattern.compile(
        "\\d{4}-\\d{2}-\\d{2}[ T].*");
    private static final Pattern PID_PREFIX = Pattern.compile("\\d+:");
    private static final Set<String> KNOWN_EXTENSIONS = Set.of(
        ".txt", ".log", ".dump", ".tdump", ".threads", ".jstack", ".out");

    private ThreadDumpSnapshotLoader() {
    }

    static LoadResult load(List<String> inputArgs) {
        if (inputArgs == null || inputArgs.isEmpty()) {
            return loadEmbeddedSamples();
        }

        List<String> warnings = new ArrayList<>();
        List<Path> files = collectInputFiles(inputArgs, warnings);
        List<ThreadDumpSnapshot> snapshots = new ArrayList<>();

        for (Path file : files) {
            String sourceLabel = toDisplayLabel(file);
            String content;
            try {
                content = Files.readString(file);
            } catch (IOException e) {
                warnings.add("Failed to read " + sourceLabel + ": " + e.getMessage());
                continue;
            }
            parseContent(sourceLabel, content, snapshots, warnings);
        }

        if (snapshots.isEmpty()) {
            warnings.add("No parseable thread dumps found in input paths.");
        }
        return new LoadResult(snapshots, warnings, false);
    }

    private static LoadResult loadEmbeddedSamples() {
        List<String> warnings = new ArrayList<>();
        List<ThreadDumpSnapshot> snapshots = new ArrayList<>();

        parseContent("embedded-sample", embeddedSampleDump(), snapshots, warnings);
        if (snapshots.isEmpty()) {
            warnings.add("Embedded sample data could not be parsed.");
        }

        return new LoadResult(snapshots, warnings, true);
    }

    private static void parseContent(
        String sourceLabel,
        String content,
        List<ThreadDumpSnapshot> snapshots,
        List<String> warnings
    ) {
        List<String> sections = splitDumpSections(content);
        int parsedInSource = 0;

        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            String section = sections.get(sectionIndex);
            if (section.isBlank()) {
                continue;
            }
            try {
                ThreadDump parsed = ThreadDumpParser.parse(section);
                if (!parsed.threads().isEmpty()) {
                    parsedInSource++;
                    snapshots.add(ThreadDumpSnapshot.of(sourceLabel, parsedInSource, parsed));
                } else {
                    warnings.add("Parsed empty snapshot in " + sourceLabel + " section " + (sectionIndex + 1));
                }
            } catch (Exception e) {
                warnings.add("Failed to parse " + sourceLabel + " section " + (sectionIndex + 1) + ": " + e.getMessage());
            }
        }

        if (parsedInSource == 0 && sections.size() > 1) {
            try {
                ThreadDump parsed = ThreadDumpParser.parse(content);
                if (!parsed.threads().isEmpty()) {
                    snapshots.add(ThreadDumpSnapshot.of(sourceLabel, 1, parsed));
                }
            } catch (Exception ignored) {
                // Keep section-level warnings only.
            }
        }
    }

    static List<String> splitDumpSections(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<String> lines = Arrays.asList(content.split("\\R", -1));
        List<Integer> starts = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!isSectionHeader(line)) {
                continue;
            }
            int start = rewindPreamble(lines, i);
            if (starts.isEmpty() || start > starts.get(starts.size() - 1)) {
                starts.add(start);
            }
        }

        if (starts.isEmpty()) {
            return List.of(content);
        }

        List<String> sections = new ArrayList<>();
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = (i + 1) < starts.size() ? starts.get(i + 1) : lines.size();
            if (start >= end) {
                continue;
            }
            String section = String.join("\n", lines.subList(start, end)).trim();
            if (!section.isBlank()) {
                sections.add(section);
            }
        }
        return sections;
    }

    private static boolean isSectionHeader(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("full thread dump") || lower.startsWith("thread dump");
    }

    private static int rewindPreamble(List<String> lines, int headerIndex) {
        int start = headerIndex;
        if (start > 0 && TIMESTAMP_LINE.matcher(lines.get(start - 1).trim()).matches()) {
            start--;
        }
        if (start > 0 && PID_PREFIX.matcher(lines.get(start - 1).trim()).matches()) {
            start--;
        }
        return start;
    }

    private static List<Path> collectInputFiles(List<String> inputArgs, List<String> warnings) {
        Set<Path> unique = new LinkedHashSet<>();
        for (String inputArg : inputArgs) {
            Path inputPath = Paths.get(inputArg).toAbsolutePath().normalize();
            if (!Files.exists(inputPath)) {
                warnings.add("Input does not exist: " + inputArg);
                continue;
            }
            if (Files.isRegularFile(inputPath)) {
                unique.add(inputPath);
                continue;
            }
            if (Files.isDirectory(inputPath)) {
                unique.addAll(collectDirectoryFiles(inputPath, warnings));
                continue;
            }
            warnings.add("Unsupported input path: " + inputArg);
        }
        return unique.stream()
            .sorted(Comparator.comparing(Path::toString))
            .toList();
    }

    private static List<Path> collectDirectoryFiles(Path directory, List<String> warnings) {
        try (Stream<Path> stream = Files.walk(directory, 4)) {
            List<Path> files = stream
                .filter(Files::isRegularFile)
                .filter(ThreadDumpSnapshotLoader::isLikelyDumpFile)
                .sorted(Comparator.comparing(Path::toString))
                .toList();
            if (files.isEmpty()) {
                warnings.add("No candidate dump files found in " + directory);
            }
            return files;
        } catch (IOException e) {
            warnings.add("Failed to scan directory " + directory + ": " + e.getMessage());
            return List.of();
        }
    }

    private static boolean isLikelyDumpFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String extension : KNOWN_EXTENSIONS) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return name.contains("thread") || name.contains("jstack");
    }

    private static String toDisplayLabel(Path file) {
        Path workspaceRoot = Paths.get("").toAbsolutePath().normalize();
        try {
            Path relative = workspaceRoot.relativize(file);
            return relative.toString();
        } catch (IllegalArgumentException ignored) {
            return file.toString();
        }
    }

    private static String embeddedSampleDump() {
        return SAMPLE_DUMP_1 + "\n\n" + SAMPLE_DUMP_2;
    }

    private static final String SAMPLE_DUMP_1 = """
        2026-02-18 10:00:00
        Full thread dump OpenJDK 64-Bit Server VM (25+):

        "main" #1 prio=5 os_prio=0 cpu=12.00ms elapsed=1.20s tid=0x000000000001 nid=0x1 runnable [0x000000000001]
           java.lang.Thread.State: RUNNABLE
                at dev.demo.Main.loop(Main.java:42)
                - locked <0x0000000011111111> (a java.lang.Object)
                at dev.demo.Main.main(Main.java:12)

        "worker-1" #22 daemon prio=5 os_prio=0 cpu=80.00ms elapsed=1.10s tid=0x000000000022 nid=0x16 waiting on condition [0x000000000002]
           java.lang.Thread.State: WAITING (parking)
                at jdk.internal.misc.Unsafe.park(Native Method)
                - parking to wait for <0x0000000012121212> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
                at java.util.concurrent.locks.LockSupport.park(LockSupport.java:211)

        "worker-2" #23 daemon prio=5 os_prio=0 cpu=20.00ms elapsed=1.00s tid=0x000000000023 nid=0x17 blocked on monitor enter [0x000000000003]
           java.lang.Thread.State: BLOCKED (on object monitor)
                at dev.demo.Queue.take(Queue.java:88)
                - waiting to lock <0x0000000013131313> (a java.lang.Object)
                at dev.demo.Service.run(Service.java:33)

        JNI global refs: 10, weak refs: 1
        """;

    private static final String SAMPLE_DUMP_2 = """
        2026-02-18 10:00:05
        Full thread dump OpenJDK 64-Bit Server VM (25+):

        "main" #1 prio=5 os_prio=0 cpu=18.00ms elapsed=6.00s tid=0x000000000001 nid=0x1 waiting on condition [0x000000000001]
           java.lang.Thread.State: WAITING (parking)
                at java.lang.Object.wait(Native Method)
                - waiting on <0x0000000011111111> (a java.lang.Object)
                at dev.demo.Main.main(Main.java:18)

        "worker-2" #23 daemon prio=5 os_prio=0 cpu=55.00ms elapsed=5.50s tid=0x000000000023 nid=0x17 runnable [0x000000000003]
           java.lang.Thread.State: RUNNABLE
                at dev.demo.Queue.poll(Queue.java:93)
                at dev.demo.Service.run(Service.java:35)

        "http-acceptor" #40 daemon prio=5 os_prio=0 cpu=4.00ms elapsed=5.20s tid=0x000000000040 nid=0x28 runnable [0x000000000004]
           java.lang.Thread.State: RUNNABLE
                at sun.nio.ch.ServerSocketChannelImpl.accept0(Native Method)
                at sun.nio.ch.ServerSocketChannelImpl.accept(ServerSocketChannelImpl.java:521)

        JNI global refs: 11, weak refs: 1
        """;

    record LoadResult(List<ThreadDumpSnapshot> snapshots, List<String> warnings, boolean usingEmbeddedSamples) {
        LoadResult {
            snapshots = List.copyOf(snapshots);
            warnings = List.copyOf(warnings);
        }
    }
}
