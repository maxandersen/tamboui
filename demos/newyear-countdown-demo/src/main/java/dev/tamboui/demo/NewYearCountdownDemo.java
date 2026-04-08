/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.widgets.canvas.Context;
import dev.tamboui.widgets.canvas.Marker;
import dev.tamboui.widgets.canvas.shapes.Points;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * New Year countdown demo inspired by "ny2026", adapted to TamboUI.
 *
 * - Big digital countdown clock to local New Year (Jan 1st 00:00 local time)
 * - Firework rockets + particle explosions (triggered at midnight or Space)
 *
 * Notes on sound:
 * - This demo emits a terminal bell (BEL, \\u0007) for the "boom" at explosion.
 *   Whether you hear it depends on your terminal settings.
 */
public final class NewYearCountdownDemo extends ToolkitApp {

    private static final Duration TICK = Duration.ofMillis(50); // ~20 fps
    private static final Duration MANUAL_SHOW_DURATION = Duration.ofSeconds(6);
    private static final Duration MIDNIGHT_SHOW_DURATION = Duration.ofSeconds(18);

    private final ZoneId zone = ZoneId.systemDefault();
    private final Random random = new Random();
    private final FireworksShow fireworks = new FireworksShow(random);

    private ZonedDateTime target;
    private boolean midnightTriggered = false;

    private long lastUpdateNanos = 0L;
    private boolean pendingBell = false;

    public static void main(String[] args) throws Exception {
        new NewYearCountdownDemo().run();
    }

    @Override
    protected TuiConfig configure() {
        return TuiConfig.builder()
            .tickRate(TICK)
            .mouseCapture(false)
            .build();
    }

    @Override
    protected void onStart() {
        // Ensure we receive key events immediately.
        if (runner() != null) {
            runner().focusManager().setFocus("root");
        }
        target = computeNextNewYear(zone);
        lastUpdateNanos = System.nanoTime();
    }

    @Override
    protected Element render() {
        Rect area = Rect.of(80, 24);
        if (runner() != null) {
            area = runner().tuiRunner().terminal().area();
        }
        int innerW = Math.max(1, area.width() - 2);
        int innerH = Math.max(1, area.height() - 2);

        ZonedDateTime now = ZonedDateTime.now(zone);
        Duration remaining = Duration.between(now, target);
        // Auto-trigger at (or after) midnight, and clamp the display at 00:00:00.
        if (remaining.isZero() || remaining.isNegative()) {
            if (!midnightTriggered) {
                midnightTriggered = true;
                fireworks.startShow(MIDNIGHT_SHOW_DURATION);
                pendingBell = true; // a little "midnight!" punctuation
            }
            remaining = Duration.ZERO;
        }

        // Update simulation time step.
        long nowNanos = System.nanoTime();
        double dt = Math.max(0.0, Math.min(0.2, (nowNanos - lastUpdateNanos) / 1_000_000_000.0));
        lastUpdateNanos = nowNanos;
        fireworks.update(dt, innerW, innerH, () -> pendingBell = true);

        String bottomHelp = " [Space] Fireworks   [q] Quit ";
        final ZonedDateTime nowFinal = now;
        final Duration remainingFinal = remaining;
        final int innerWFinal = innerW;
        final int innerHFinal = innerH;

        return panel(
            canvas(0, innerWFinal, 0, innerHFinal)
                .marker(Marker.BRAILLE)
                .paint(ctx -> {
                    // Background fireworks (points/lines)
                    fireworks.render(ctx);

                    // Foreground clock + labels (rendered above points)
                    renderOverlay(ctx, innerWFinal, innerHFinal, nowFinal, remainingFinal);

                    // One-frame terminal bell for "boom".
                    if (pendingBell) {
                        // Clamp puts it on-screen; BEL is non-printing for most terminals.
                        ctx.print(0, 0, "\u0007");
                        pendingBell = false;
                    }
                })
                .fill()
        )
            .id("root")
            .focusable()
            .rounded()
            .borderColor(Color.DARK_GRAY)
            .focusedBorderColor(Color.CYAN)
            .bottomTitle(bottomHelp)
            .onKeyEvent(e -> {
                if (e != null && e.isChar(' ')) {
                    fireworks.startShow(MANUAL_SHOW_DURATION);
                    return EventResult.HANDLED;
                }
                return EventResult.UNHANDLED;
            });
    }

    private void renderOverlay(Context ctx, int w, int h, ZonedDateTime now, Duration remaining) {
        int nextYear = target.getYear();
        long totalSeconds = Math.max(0, remaining.toSeconds());
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;

        String clock = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        SevenSegFont font = SevenSegFont.DEFAULT;
        List<String> lines = font.render(clock);

        // Centered big clock.
        int clockHeight = lines.size();
        int clockWidth = 0;
        for (String line : lines) {
            clockWidth = Math.max(clockWidth, line.length());
        }

        int startX = Math.max(0, (w - clockWidth) / 2);
        int topY = Math.max(0, (int) Math.round(h * 0.72));

        Color clockColor = fireworks.isShowActive() ? Color.YELLOW : Color.CYAN;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // Canvas y is bottom-origin; decreasing y moves down visually.
            double y = topY - i;
            ctx.print(startX, y, Line.from(Span.raw(line).fg(clockColor).bold()));
        }

        // Days label (normal size)
        if (days > 0) {
            String daysLine = String.format("%d day%s", days, days == 1 ? "" : "s");
            int dx = Math.max(0, (w - daysLine.length()) / 2);
            ctx.print(dx, topY + 2, Line.from(Span.raw(daysLine).magenta().bold()));
        }

        // Title / subtext
        String title = "Counting down to " + nextYear + " (local time)";
        int titleX = Math.max(0, (w - title.length()) / 2);
        ctx.print(titleX, topY + 4, Line.from(Span.raw(title).gray()));

        // At/after midnight: happy message
        if (midnightTriggered || remaining.isZero()) {
            String msg = "HAPPY NEW YEAR " + nextYear + "!";
            int mx = Math.max(0, (w - msg.length()) / 2);
            double my = Math.max(0, Math.round(h * 0.25));
            ctx.print(mx, my, Line.from(Span.raw(msg).green().bold()));
        }

        // Corner info
        String nowLine = now.toLocalTime().withNano(0).toString();
        ctx.print(1, h - 2, Line.from(Span.raw("Now: " + nowLine).dim().gray()));
    }

    private static ZonedDateTime computeNextNewYear(ZoneId zone) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        int nextYear = now.getYear() + 1;
        return ZonedDateTime.of(LocalDate.of(nextYear, 1, 1), LocalTime.MIDNIGHT, zone);
    }

    /**
     * Simple fireworks show: rockets fly up, then explode into particles which fall and fade.
     * Coordinates are in "cell space" (x: 0..width, y: 0..height) and rendered onto a braille canvas.
     */
    static final class FireworksShow {
        private static final double GRAVITY = -28.0; // units / s^2
        private static final double DRAG = 0.995;

        private final Random rng;
        private final List<Rocket> rockets = new ArrayList<>();
        private final List<Particle> particles = new ArrayList<>();

        private double showTimeLeft = 0.0;
        private double rocketSpawnCooldown = 0.0;

        FireworksShow(Random rng) {
            this.rng = Objects.requireNonNull(rng, "rng");
        }

        boolean isShowActive() {
            return showTimeLeft > 0.0;
        }

        void startShow(Duration duration) {
            showTimeLeft = Math.max(showTimeLeft, duration.toMillis() / 1000.0);
            // Spawn a couple immediately for responsiveness.
            rocketSpawnCooldown = 0.0;
        }

        void update(double dt, int width, int height, Runnable onBoom) {
            if (dt <= 0) {
                return;
            }

            // Spawn rockets while show is active.
            if (showTimeLeft > 0.0) {
                showTimeLeft = Math.max(0.0, showTimeLeft - dt);
                rocketSpawnCooldown -= dt;
                if (rocketSpawnCooldown <= 0.0) {
                    spawnRocket(width, height);
                    // Random cadence (denser near the start).
                    rocketSpawnCooldown = 0.25 + rng.nextDouble() * 0.55;
                }
            }

            // Update rockets (and explode if needed).
            for (Iterator<Rocket> it = rockets.iterator(); it.hasNext(); ) {
                Rocket r = it.next();
                r.update(dt, width, height, GRAVITY);

                // "Hiss" trail: small short-lived sparks while ascending.
                if (r.age > 0.05 && r.vy > 4.0) {
                    int n = 1 + rng.nextInt(3);
                    for (int i = 0; i < n; i++) {
                        double sx = r.x + (rng.nextDouble() - 0.5) * 0.6;
                        double sy = r.y + (rng.nextDouble() - 0.5) * 0.6;
                        double svx = (rng.nextDouble() - 0.5) * 1.5;
                        double svy = -2.0 - rng.nextDouble() * 3.0;
                        double life = 0.18 + rng.nextDouble() * 0.18;
                        particles.add(new Particle(sx, sy, svx, svy, life, FxColor.GRAY));
                    }
                }

                if (r.shouldExplode()) {
                    explode(r, onBoom);
                    it.remove();
                } else if (r.isOutOfBounds(width, height)) {
                    it.remove();
                }
            }

            // Update particles.
            for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
                Particle p = it.next();
                p.update(dt, GRAVITY, DRAG);
                if (p.life <= 0.0 || p.y < -2) {
                    it.remove();
                }
            }
        }

        void render(Context ctx) {
            // Draw rocket trails as short lines + a head point.
            for (Rocket r : rockets) {
                ctx.draw(dev.tamboui.widgets.canvas.shapes.Line.of(r.prevX, r.prevY, r.x, r.y, Color.GRAY));
                ctx.draw(Points.of(new double[][] {{r.x, r.y}}, Color.WHITE));
            }

            // Group particles by color for efficient Points rendering.
            Map<FxColor, List<double[]>> buckets = new EnumMap<>(FxColor.class);
            for (FxColor fc : FxColor.values()) {
                buckets.put(fc, new ArrayList<>());
            }
            for (Particle p : particles) {
                FxColor fc = p.color;
                List<double[]> list = buckets.get(fc);
                if (list != null) {
                    list.add(new double[] {p.x, p.y});
                }
            }
            for (Map.Entry<FxColor, List<double[]>> e : buckets.entrySet()) {
                List<double[]> pts = e.getValue();
                if (pts == null || pts.isEmpty()) {
                    continue;
                }
                double[][] coords = new double[pts.size()][2];
                for (int i = 0; i < pts.size(); i++) {
                    coords[i] = pts.get(i);
                }
                ctx.draw(Points.of(coords, e.getKey().toColor()));
            }
        }

        private void spawnRocket(int width, int height) {
            if (width <= 0 || height <= 0) {
                return;
            }
            double x = 2 + rng.nextDouble() * Math.max(1, width - 4);
            double y = 1.0;
            double vx = (rng.nextDouble() - 0.5) * 6.0;
            double vy = 26 + rng.nextDouble() * 10.0;
            double fuse = 0.9 + rng.nextDouble() * 0.8;
            rockets.add(new Rocket(x, y, vx, vy, fuse));
        }

        private void explode(Rocket r, Runnable onBoom) {
            // "Boom" (terminal bell).
            if (onBoom != null) {
                onBoom.run();
            }

            FxColor base = FxColor.random(rng);
            int count = 110 + rng.nextInt(120);
            for (int i = 0; i < count; i++) {
                double angle = rng.nextDouble() * Math.PI * 2.0;
                double speed = 8 + rng.nextDouble() * 18.0;
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed;
                double life = 0.9 + rng.nextDouble() * 1.2;
                FxColor color = rng.nextDouble() < 0.20 ? FxColor.WHITE : base;
                particles.add(new Particle(r.x, r.y, vx, vy, life, color));
            }

            // Falling "tail" sparks.
            int tail = 40 + rng.nextInt(45);
            for (int i = 0; i < tail; i++) {
                double angle = (rng.nextDouble() * Math.PI) - (Math.PI / 2.0); // mostly downward
                double speed = 2 + rng.nextDouble() * 8.0;
                double vx = Math.cos(angle) * speed * 0.6;
                double vy = Math.sin(angle) * speed - 6.0;
                double life = 1.4 + rng.nextDouble() * 1.2;
                particles.add(new Particle(r.x, r.y, vx, vy, life, FxColor.YELLOW));
            }
        }
    }

    static final class Rocket {
        double x;
        double y;
        double prevX;
        double prevY;
        double vx;
        double vy;
        double fuse;
        double age;

        Rocket(double x, double y, double vx, double vy, double fuse) {
            this.x = x;
            this.y = y;
            this.prevX = x;
            this.prevY = y;
            this.vx = vx;
            this.vy = vy;
            this.fuse = fuse;
            this.age = 0.0;
        }

        void update(double dt, int width, int height, double gravity) {
            age += dt;
            prevX = x;
            prevY = y;

            // Integrate
            x += vx * dt;
            y += vy * dt;
            vy += gravity * dt;

            // Gentle screen-bounds influence to keep it visible.
            if (x < 1) x = 1;
            if (x > width - 2) x = width - 2;
            if (y < 0) y = 0;
            if (y > height) y = height;
        }

        boolean shouldExplode() {
            // Explode after fuse or at apex.
            return age >= fuse || vy < 2.0;
        }

        boolean isOutOfBounds(int width, int height) {
            return y <= 0 && age > 1.0;
        }
    }

    static final class Particle {
        double x;
        double y;
        double vx;
        double vy;
        double life;
        final FxColor color;

        Particle(double x, double y, double vx, double vy, double life, FxColor color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.color = color != null ? color : FxColor.WHITE;
        }

        void update(double dt, double gravity, double drag) {
            life -= dt;
            x += vx * dt;
            y += vy * dt;
            vx *= Math.pow(drag, dt * 60.0);
            vy *= Math.pow(drag, dt * 60.0);
            vy += gravity * dt;
        }
    }

    enum FxColor {
        GRAY(Color.GRAY),
        RED(Color.RED),
        GREEN(Color.GREEN),
        YELLOW(Color.YELLOW),
        BLUE(Color.BLUE),
        MAGENTA(Color.MAGENTA),
        CYAN(Color.CYAN),
        WHITE(Color.WHITE);

        private final Color color;

        FxColor(Color color) {
            this.color = color;
        }

        Color toColor() {
            return color;
        }

        static FxColor random(Random rng) {
            FxColor[] values = values();
            // avoid GRAY/WHITE most of the time for variety
            int idx = 1 + rng.nextInt(values.length - 2);
            return values[idx];
        }
    }

    /**
     * Tiny 7-seg-ish font for "HH:MM:SS" (height 7).
     */
    static final class SevenSegFont {
        static final SevenSegFont DEFAULT = new SevenSegFont();

        private final Map<Character, String[]> glyphs = Map.ofEntries(
            Map.entry('0', new String[] {
                " ███ ",
                "█   █",
                "█  ██",
                "█ █ █",
                "██  █",
                "█   █",
                " ███ "
            }),
            Map.entry('1', new String[] {
                "  █  ",
                " ██  ",
                "  █  ",
                "  █  ",
                "  █  ",
                "  █  ",
                " ███ "
            }),
            Map.entry('2', new String[] {
                " ███ ",
                "█   █",
                "    █",
                "  ██ ",
                " █   ",
                "█    ",
                "█████"
            }),
            Map.entry('3', new String[] {
                "████ ",
                "    █",
                "    █",
                " ███ ",
                "    █",
                "    █",
                "████ "
            }),
            Map.entry('4', new String[] {
                "█   █",
                "█   █",
                "█   █",
                "█████",
                "    █",
                "    █",
                "    █"
            }),
            Map.entry('5', new String[] {
                "█████",
                "█    ",
                "█    ",
                "████ ",
                "    █",
                "    █",
                "████ "
            }),
            Map.entry('6', new String[] {
                " ███ ",
                "█    ",
                "█    ",
                "████ ",
                "█   █",
                "█   █",
                " ███ "
            }),
            Map.entry('7', new String[] {
                "█████",
                "    █",
                "   █ ",
                "  █  ",
                " █   ",
                " █   ",
                " █   "
            }),
            Map.entry('8', new String[] {
                " ███ ",
                "█   █",
                "█   █",
                " ███ ",
                "█   █",
                "█   █",
                " ███ "
            }),
            Map.entry('9', new String[] {
                " ███ ",
                "█   █",
                "█   █",
                " ████",
                "    █",
                "    █",
                " ███ "
            }),
            Map.entry(':', new String[] {
                "     ",
                "  ░  ",
                "  ░  ",
                "     ",
                "  ░  ",
                "  ░  ",
                "     "
            })
        );

        List<String> render(String text) {
            String s = text != null ? text : "";
            int height = 7;
            String[] out = new String[height];
            for (int i = 0; i < height; i++) {
                out[i] = "";
            }

            for (int idx = 0; idx < s.length(); idx++) {
                char ch = s.charAt(idx);
                String[] g = glyphs.getOrDefault(ch, blank(height));
                for (int row = 0; row < height; row++) {
                    out[row] += g[row];
                    if (idx < s.length() - 1) {
                        out[row] += " ";
                    }
                }
            }

            List<String> lines = new ArrayList<>(height);
            for (int i = 0; i < height; i++) {
                lines.add(rtrim(out[i]));
            }
            return lines;
        }

        private static String[] blank(int h) {
            String[] b = new String[h];
            for (int i = 0; i < h; i++) {
                b[i] = "     ";
            }
            return b;
        }

        private static String rtrim(String s) {
            int i = s.length() - 1;
            while (i >= 0 && s.charAt(i) == ' ') {
                i--;
            }
            return s.substring(0, i + 1);
        }
    }
}

