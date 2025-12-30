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

    private static final double DEFAULT_FORCE = 3.0;
    private static final double CPU_FORCE_MIN = 1.0;
    private static final double CPU_FORCE_MAX = 6.0;
    private static final double FORCE_SMOOTHING = 0.9; // like (force*9 + cpu)/10
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
    private int simHPlusOne = 0; // height + 1 (Rust keeps a seeded row at y=height)
    private float[] fire;

    // UI state
    private boolean showHelp = false;
    private double force = DEFAULT_FORCE;
    private double cpuTargetForce = DEFAULT_FORCE;

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

    double force() {
        return force;
    }

    double cpuTargetForce() {
        return cpuTargetForce;
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
            Span.raw(String.format(" force %.2f ", force)).magenta(),
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
            force *= 1.1;
            return EventResult.HANDLED;
        }
        if (Keys.isDown(event)) {
            force /= 1.1;
            return EventResult.HANDLED;
        }
        if (Keys.isChar(event, 'r') || Keys.isChar(event, 'R')) {
            force = DEFAULT_FORCE;
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

            cpuTargetForce = CPU_FORCE_MIN + (CPU_FORCE_MAX - CPU_FORCE_MIN) * cpuLoad;
            // Smoothly blend like upstream: force = (force*9 + cpu)/10
            force = force * FORCE_SMOOTHING + cpuTargetForce * (1.0 - FORCE_SMOOTHING);

            lastCpuSampleNanos = now;
        }
    }

    private void stepFire(int width, int height) {
        ensureSimulationSize(width, height);
        if (fire == null) {
            return;
        }

        // Seed the row at y=height (Rust: *entry = (*entry*2 + rand)/3)
        int seedY = simHPlusOne - 1;
        for (int x = 0; x < simW; x++) {
            int idx = seedY * simW + x;
            float prev = fire[idx];
            float r = random.nextFloat();
            fire[idx] = (prev * 2.0f + r) / 3.0f;
        }

        // Propagate upwards (Rust averages (x,y) with (x,y+1) and diagonals)
        float heatDamp = (float) (0.9 + 0.05 * ((double) (simHPlusOne - 1) / 80.0));
        for (int y = (simHPlusOne - 2); y >= 0; y--) {
            int row = y * simW;
            int below = (y + 1) * simW;
            for (int x = 0; x < simW; x++) {
                int n = 2;
                float v = fire[row + x];
                if (x > 0) {
                    v += fire[below + (x - 1)];
                    n++;
                }
                v += fire[below + x];
                // Note: upstream uses x < width - 2 (not - 1)
                if (x < simW - 2) {
                    v += fire[below + (x + 1)];
                    n++;
                }
                v /= (float) n;
                v *= heatDamp;
                fire[row + x] = v;
            }
        }
    }

    private void ensureSimulationSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            simW = 0;
            simHPlusOne = 0;
            fire = null;
            return;
        }

        if (width == simW && (height + 1) == simHPlusOne && fire != null) {
            return;
        }

        simW = width;
        simHPlusOne = height + 1;
        fire = new float[simW * simHPlusOne];
    }

    private void renderFire(Buffer buffer, Rect area) {
        int height = area.height();
        for (int y = 0; y < height; y++) {
            int srcRow = y * simW;
            int dstY = area.y() + y;
            for (int x = 0; x < simW; x++) {
                float v = fire[srcRow + x];
                Color bg = valToColor(v * (float) force);
                buffer.set(area.x() + x, dstY, new Cell(" ", Style.EMPTY.bg(bg)));
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
        lines.add("Up / Down           : increase / decrease force");
        lines.add("r                   : reset force");
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

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    // Match upstream tiof's piecewise RGB mapping (background color)
    private static Color valToColor(float val) {
        float v = Math.min(1.0f, Math.max(0.0f, val));
        if (v > 0.75f) {
            int b = (int) (255.0f * 4.0f * (v - 0.75f));
            return Color.rgb(255, 255, clamp(b, 0, 255));
        }
        if (v > 0.5f) {
            int g = (int) (255.0f * 4.0f * (v - 0.5f));
            return Color.rgb(255, clamp(g, 0, 255), 0);
        }
        int r = (int) (255.0f * 2.0f * v);
        return Color.rgb(clamp(r, 0, 255), 0, 0);
    }
}

