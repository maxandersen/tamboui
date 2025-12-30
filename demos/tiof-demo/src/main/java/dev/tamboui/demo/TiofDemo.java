///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
//DEPS com.github.oshi:oshi-core:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.TuiConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * TIOF - the Term Is On Fire (Java demo).
 * <p>
 * A small fire animation inspired by https://codeberg.org/corpsmoderne/tiof,
 * implemented using TamboUI for rendering. Fire intensity is driven by OSHI CPU load,
 * with manual overrides.
 */
public final class TiofDemo extends ToolkitApp {

    private static final int DEFAULT_FPS = 20;
    private static final int MAX_MESSAGE_CHARS = 4000;

    private final List<String> messageLines;
    private final TiofFire fire;

    private TiofDemo(List<String> messageLines) {
        this.messageLines = List.copyOf(messageLines);
        this.fire = new TiofFire(
            this.messageLines,
            this::quit
        );
    }

    public static void main(String[] args) throws Exception {
        var parsed = parseArgs(args);
        if (parsed.showHelp) {
            System.out.println("""
                tiof-demo (Java)

                Usage:
                  ./gradlew :demos:tiof-demo:run --args="HELLO WORLD"
                  ./gradlew :demos:tiof-demo:run --args="-f path/to/file.txt"

                In app:
                  Esc / Enter / Space : quit
                  h                   : help
                  Up / Down           : increase / decrease fire strength override
                  r                   : reset override
                """.trim());
            return;
        }

        new TiofDemo(parsed.messageLines).run();
    }

    @Override
    protected TuiConfig configure() {
        return TuiConfig.builder()
            .tickRate(Duration.ofMillis(1000L / DEFAULT_FPS))
            .build();
    }

    @Override
    protected Element render() {
        return column(
            panel(() -> row(
                text(" TIOF ").bold().red(),
                text(" the Term Is On Fire ").bold().yellow(),
                spacer(),
                text(" [h] Help ").dim(),
                text(" [↑/↓] Force ").dim(),
                text(" [Esc/Enter/Space] Quit ").dim()
            )).rounded().length(3),
            fire,
            panel(() -> row(
                text(" CPU ").dim(),
                text(String.format("%5.1f%%", fire.cpuLoadPercent())).bold().cyan(),
                text("  Target ").dim(),
                text(String.format("%.2f", fire.cpuTargetForce())).bold().yellow(),
                text("  Force ").dim(),
                text(String.format("%.2f", fire.force())).bold().magenta()
            )).rounded().length(3)
        );
    }

    private static ParsedArgs parseArgs(String[] args) throws Exception {
        boolean showHelp = false;
        Path file = null;
        List<String> textParts = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("-h".equals(a) || "--help".equals(a)) {
                showHelp = true;
                continue;
            }
            if ("-f".equals(a) && i + 1 < args.length) {
                file = Path.of(args[++i]);
                continue;
            }
            textParts.add(a);
        }

        List<String> lines;
        if (file != null) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (content.length() > MAX_MESSAGE_CHARS) {
                content = content.substring(0, MAX_MESSAGE_CHARS) + "\n…";
            }
            lines = normalizeMessage(content);
        } else if (!textParts.isEmpty()) {
            lines = normalizeMessage(String.join(" ", textParts));
        } else {
            lines = List.of("the Term Is On Fire", "CPU-driven edition");
        }

        return new ParsedArgs(showHelp, lines);
    }

    private static List<String> normalizeMessage(String content) {
        // Keep it simple: split into lines, trim trailing whitespace, drop empty tail.
        String[] raw = content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<String> out = new ArrayList<>();
        for (String line : raw) {
            out.add(rstrip(line));
        }
        // Drop trailing empty lines
        while (!out.isEmpty() && out.get(out.size() - 1).isBlank()) {
            out.remove(out.size() - 1);
        }
        if (out.isEmpty()) {
            return List.of("(empty)");
        }
        return out;
    }

    private static String rstrip(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }

    private record ParsedArgs(boolean showHelp, List<String> messageLines) {}
}

