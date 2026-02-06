/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.textvm;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

final class TextVmFormatting {

    private static final String[] BYTE_UNITS = {"B", "KB", "MB", "GB", "TB"};

    private TextVmFormatting() {
    }

    static String shortName(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        String trimmed = value.trim();
        int slash = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < trimmed.length()) {
            trimmed = trimmed.substring(slash + 1);
        }
        if (trimmed.endsWith(".jar")) {
            return trimmed;
        }
        int dot = trimmed.lastIndexOf('.');
        if (dot > 0 && dot + 1 < trimmed.length()) {
            trimmed = trimmed.substring(dot + 1);
        }
        return trimmed;
    }

    static String findMainCandidate(String[] args, String commandLine) {
        // Special-case jcmd: its command line is the tool, not the target main
        if (commandLine != null && commandLine.endsWith("/jcmd")) {
            return "jcmd";
        }

        String main = findMainFromArgs(args);
        if (!main.isBlank()) {
            return main;
        }
        if (commandLine == null || commandLine.isBlank()) {
            return "";
        }
        return findMainFromArgs(commandLine.trim().split("\\s+"));
    }

    static String joinArgs(String[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        return String.join(" ", args).trim();
    }

    static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "n/a";
        }
        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < BYTE_UNITS.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        if (unitIndex == 0) {
            return String.format(Locale.ROOT, "%d %s", (long) value, BYTE_UNITS[unitIndex]);
        }
        return String.format(Locale.ROOT, "%.1f %s", value, BYTE_UNITS[unitIndex]);
    }

    static String formatPercent(double value) {
        if (value < 0) {
            return "n/a";
        }
        double clamped = Math.max(0, Math.min(1, value));
        return String.format(Locale.ROOT, "%.0f%%", clamped * 100);
    }

    static String formatDuration(Duration duration) {
        if (duration == null) {
            return "n/a";
        }
        long seconds = Math.max(0, duration.getSeconds());
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%dh %dm %ds", hours, minutes, secs);
        }
        if (minutes > 0) {
            return String.format(Locale.ROOT, "%dm %ds", minutes, secs);
        }
        return String.format(Locale.ROOT, "%ds", secs);
    }

    private static String findMainFromArgs(String[] args) {
        if (args == null) {
            return "";
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-jar".equals(arg) && i + 1 < args.length) {
                return args[i + 1];
            }
            // Flags that take a separate argument we should skip as a pair
            if ("-cp".equals(arg) || "-classpath".equals(arg)) {
                i++;
                continue;
            }
            if ("--add-opens".equals(arg)
                    || "--add-exports".equals(arg)
                    || "--add-reads".equals(arg)
                    || "--patch-module".equals(arg)) {
                i++;
                continue;
            }
            // Flags that embed their value with '='
            if (arg.startsWith("--add-opens=")
                    || arg.startsWith("--add-exports=")
                    || arg.startsWith("--add-reads=")
                    || arg.startsWith("--patch-module=")) {
                continue;
            }
            if (arg.startsWith("-")) {
                continue;
            }
            return arg;
        }
        return "";
    }
}
