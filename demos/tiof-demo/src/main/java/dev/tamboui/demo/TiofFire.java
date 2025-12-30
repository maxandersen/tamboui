/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.Keys;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Stateful fire animation element (Doom fire).
 * <p>
 * CPU load is read via OSHI. A manual strength offset can be applied with Up/Down.
 */
final class TiofFire implements Element {

    private static final int PALETTE_SIZE = 37;
    private static final int BASE_STRENGTH = 10;
    private static final int MAX_STRENGTH = PALETTE_SIZE - 1;
    private static final int STRENGTH_STEP = 2;
    private static final Duration CPU_SAMPLE_EVERY = Duration.ofMillis(250);

    private static final Style HELP_STYLE = Style.EMPTY.fg(Color.WHITE).bg(Color.BLACK).bold();
    private static final Style OVERLAY_STYLE = Style.EMPTY.fg(Color.BRIGHT_WHITE).bg(Color.BLACK).bold();

    private final List<String> messageLines;
    private final Runnable quit;
    private final Random random = new Random();

    // OSHI CPU sampling
    private final CentralProcessor processor;
    private long[] previousTicks;
    private long lastCpuSampleNanos = 0;
    private double cpuLoad = 0.0; // 0..1

    // Fire simulation state
    private int simW = 0;
    private int simH = 0;
    private int[] fire;
    private int[] next;

    private final Style[] palette = buildPalette();

    // UI state
    private boolean showHelp = false;
    private int manualStrengthOffset = 0;

    TiofFire(List<String> messageLines, Runnable quit) {
        this.messageLines = List.copyOf(messageLines);
        this.quit = quit;

        var si = new SystemInfo();
        this.processor = si.getHardware().getProcessor();
        this.previousTicks = processor.getSystemCpuLoadTicks();
    }

    double cpuLoadPercent() {
        return cpuLoad * 100.0;
    }

    int manualStrengthOffset() {
        return manualStrengthOffset;
    }

    int effectiveStrength() {
        return computeStrength();
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        // Panel frame
        var title = Title.from(Line.from(
            Span.raw(" Fire ").bold().red(),
            Span.raw("·").dim(),
            Span.raw(String.format(" cpu %.1f%% ", cpuLoadPercent())).yellow(),
            Span.raw("·").dim(),
            Span.raw(String.format(" strength %d ", effectiveStrength())).magenta(),
            Span.raw("·").dim(),
            Span.raw(" [h] help ").dim()
        )).centered();

        var block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
            .title(title)
            .style(Style.EMPTY.bg(Color.BLACK))
            .build();

        frame.renderWidget(block, area);
        Rect inner = block.inner(area);
        if (inner.isEmpty()) {
            return;
        }

        sampleCpuIfNeeded();
        stepFire(inner.width(), inner.height());
        renderFire(frame.buffer(), inner);

        if (showHelp) {
            renderHelpOverlay(frame.buffer(), inner);
        } else {
            renderMessageOverlay(frame.buffer(), inner);
        }
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Quit: Esc / Enter / Space, plus standard q/Ctrl+C (ToolkitRunner already handles q/Ctrl+C, but keep it explicit)
        if (Keys.isEscape(event) || Keys.isSelect(event) || Keys.isQuit(event)) {
            quit.run();
            return EventResult.HANDLED;
        }

        if (Keys.isUp(event)) {
            manualStrengthOffset = clamp(manualStrengthOffset + STRENGTH_STEP, -MAX_STRENGTH, MAX_STRENGTH);
            return EventResult.HANDLED;
        }
        if (Keys.isDown(event)) {
            manualStrengthOffset = clamp(manualStrengthOffset - STRENGTH_STEP, -MAX_STRENGTH, MAX_STRENGTH);
            return EventResult.HANDLED;
        }
        if (Keys.isChar(event, 'r') || Keys.isChar(event, 'R')) {
            manualStrengthOffset = 0;
            return EventResult.HANDLED;
        }
        if (Keys.isChar(event, 'h') || Keys.isChar(event, 'H')) {
            showHelp = !showHelp;
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    private void sampleCpuIfNeeded() {
        long now = System.nanoTime();
        if (lastCpuSampleNanos == 0 || now - lastCpuSampleNanos >= CPU_SAMPLE_EVERY.toNanos()) {
            double load = processor.getSystemCpuLoadBetweenTicks(previousTicks);
            previousTicks = processor.getSystemCpuLoadTicks();

            if (Double.isFinite(load) && load >= 0.0) {
                cpuLoad = clamp01(load);
            }

            lastCpuSampleNanos = now;
        }
    }

    private int computeStrength() {
        // Make idle still burn a bit, and map to palette.
        double baseline = 0.15;
        double scaled = baseline + (1.0 - baseline) * cpuLoad;
        int cpuStrength = (int) Math.round(scaled * MAX_STRENGTH);
        int strength = cpuStrength + manualStrengthOffset;
        strength = clamp(strength, 0, MAX_STRENGTH);
        return Math.max(BASE_STRENGTH, strength);
    }

    private void stepFire(int width, int height) {
        ensureSimulationSize(width, height);

        int strength = computeStrength();

        // Clear next
        for (int i = 0; i < next.length; i++) {
            next[i] = 0;
        }

        // Seed bottom row
        int bottomY = simH - 1;
        for (int x = 0; x < simW; x++) {
            next[bottomY * simW + x] = random.nextInt(strength + 1);
        }

        // Propagate upwards
        for (int y = 0; y < simH - 1; y++) {
            int row = y * simW;
            int below = (y + 1) * simW;
            for (int x = 0; x < simW; x++) {
                int belowIntensity = fire[below + x];
                int decay = random.nextInt(3); // 0..2
                int newIntensity = Math.max(0, belowIntensity - decay);
                int shift = random.nextInt(3) - 1; // -1..+1
                int dstX = clamp(x + shift, 0, simW - 1);
                next[row + dstX] = Math.max(next[row + dstX], newIntensity);
            }
        }

        // Swap
        int[] tmp = fire;
        fire = next;
        next = tmp;
    }

    private void ensureSimulationSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            simW = 0;
            simH = 0;
            fire = null;
            next = null;
            return;
        }

        if (width == simW && height == simH && fire != null && next != null) {
            return;
        }

        simW = width;
        simH = height;
        fire = new int[simW * simH];
        next = new int[simW * simH];
    }

    private void renderFire(Buffer buffer, Rect area) {
        for (int y = 0; y < simH; y++) {
            int srcRow = y * simW;
            int dstY = area.y() + y;
            for (int x = 0; x < simW; x++) {
                int intensity = fire[srcRow + x];
                Style style = palette[clamp(intensity, 0, MAX_STRENGTH)];
                buffer.set(area.x() + x, dstY, new Cell(" ", style));
            }
        }
    }

    private void renderMessageOverlay(Buffer buffer, Rect area) {
        if (messageLines.isEmpty()) {
            return;
        }

        // Center message around the upper third of the fire.
        int maxLines = Math.min(messageLines.size(), Math.max(1, area.height() / 3));
        int startY = area.y() + Math.max(1, area.height() / 5);

        for (int i = 0; i < maxLines; i++) {
            String line = messageLines.get(i);
            if (line.isBlank()) {
                continue;
            }
            if (line.length() > area.width() - 2) {
                line = line.substring(0, Math.max(0, area.width() - 2));
            }
            int x = area.x() + Math.max(0, (area.width() - line.length()) / 2);
            int y = startY + i;
            if (y >= area.bottom()) {
                break;
            }
            buffer.setString(x, y, line, OVERLAY_STYLE);
        }
    }

    private void renderHelpOverlay(Buffer buffer, Rect area) {
        List<String> lines = new ArrayList<>();
        lines.add("tiof (java demo)");
        lines.add("");
        lines.add("Esc / Enter / Space : quit");
        lines.add("h                   : toggle help");
        lines.add("Up / Down           : strength override");
        lines.add("r                   : reset override");
        lines.add("");
        lines.add("fire strength tracks CPU load via OSHI");

        int boxW = 0;
        for (String s : lines) {
            boxW = Math.max(boxW, s.length());
        }
        boxW = Math.min(boxW + 4, area.width());
        int boxH = Math.min(lines.size() + 2, area.height());

        if (boxW <= 4 || boxH <= 2) {
            return;
        }

        int x0 = area.x() + (area.width() - boxW) / 2;
        int y0 = area.y() + (area.height() - boxH) / 2;

        // background box
        for (int y = 0; y < boxH; y++) {
            for (int x = 0; x < boxW; x++) {
                buffer.set(x0 + x, y0 + y, new Cell(" ", HELP_STYLE));
            }
        }

        // content
        int maxContentLines = Math.min(lines.size(), boxH - 2);
        for (int i = 0; i < maxContentLines; i++) {
            String s = lines.get(i);
            if (s.length() > boxW - 4) {
                s = s.substring(0, boxW - 4);
            }
            buffer.setString(x0 + 2, y0 + 1 + i, s, HELP_STYLE);
        }
    }

    private static Style[] buildPalette() {
        // A warm gradient: black -> deep red -> orange -> yellow -> white.
        Style[] out = new Style[PALETTE_SIZE];
        out[0] = Style.EMPTY.bg(Color.BLACK);

        // Hand-tuned RGB ramp (PALETTE_SIZE-1 entries).
        for (int i = 1; i < PALETTE_SIZE; i++) {
            double t = (i - 1) / (double) (PALETTE_SIZE - 2); // 0..1

            int r;
            int g;
            int b;
            if (t < 0.33) {
                // black -> red
                double u = t / 0.33;
                r = lerp(0, 200, u);
                g = 0;
                b = 0;
            } else if (t < 0.75) {
                // red -> orange/yellow
                double u = (t - 0.33) / (0.75 - 0.33);
                r = lerp(200, 255, u);
                g = lerp(0, 180, u);
                b = 0;
            } else {
                // yellow -> white
                double u = (t - 0.75) / (1.0 - 0.75);
                r = 255;
                g = lerp(180, 255, u);
                b = lerp(0, 255, u);
            }

            out[i] = Style.EMPTY.bg(Color.rgb(r, g, b));
        }

        return out;
    }

    private static int lerp(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * clamp01(t));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}

