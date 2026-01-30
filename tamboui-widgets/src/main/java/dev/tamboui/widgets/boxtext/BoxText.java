/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.boxtext;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Renders "big" text using box-drawing characters.
 * 
 * Think like a old-school calculator display.
 *
 * <p>Each input character is mapped to a 3-row glyph and rendered into the provided area.
 * Unknown characters are rendered as spaces.
 * 
 * <p>Current glyphs are:</p>
 * <ul>
 *   <li>A-Z</li>
 *   <li>a-z</li>
 *   <li>0-9</li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <pre>{@code
 * BoxText text = BoxText.from("HELLO");
 * frame.renderWidget(text, frame.area());
 * }</pre>
 */
public final class BoxText implements Widget {

    /**
     * Height (in terminal rows) of each glyph.
     */
    public static final int GLYPH_HEIGHT = 3;

    private static final Glyph DEFAULT_GLYPH = Glyph.of(" ", " ", " ");
    private static final Map<Integer, Glyph> GLYPHS = createGlyphs();

    private final String text;
    private final Block block;
    private final Style style;
    private final boolean uppercase;
    private final Alignment alignment;

    private BoxText(Builder builder) {
        this.text = builder.text;
        this.block = builder.block;
        this.style = builder.style;
        this.uppercase = builder.uppercase;
        this.alignment = builder.alignment;
    }

    /**
     * Creates a new {@link BoxText} builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a {@link BoxText} widget from a string.
     *
     * @param text the text to render
     * @return a new {@link BoxText}
     */
    public static BoxText from(String text) {
        return builder().text(text).build();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }

        // Apply base style to full area (matches ratatui-style behavior)
        buffer.setStyle(area, style);

        Rect textArea = area;
        if (block != null) {
            block.render(area, buffer);
            textArea = block.inner(area);
        }

        if (textArea.isEmpty()) {
            return;
        }

        buffer.setStyle(textArea, style);

        if (text == null || text.isEmpty()) {
            return;
        }

        List<String> logicalLines = splitLines(text);
        int maxLogicalLines = (textArea.height() + GLYPH_HEIGHT - 1) / GLYPH_HEIGHT;
        int count = Math.min(logicalLines.size(), maxLogicalLines);

        for (int lineIndex = 0; lineIndex < count; lineIndex++) {
            String line = logicalLines.get(lineIndex);
            if (uppercase) {
                line = line.toUpperCase(Locale.ROOT);
            }

            int baseY = textArea.top() + (lineIndex * GLYPH_HEIGHT);
            if (baseY >= textArea.bottom()) {
                break;
            }

            for (int glyphRow = 0; glyphRow < GLYPH_HEIGHT; glyphRow++) {
                int y = baseY + glyphRow;
                if (y >= textArea.bottom()) {
                    break;
                }

                String row = renderGlyphRow(line, glyphRow);

                String rendered = alignAndClip(row, textArea.width(), alignment);
                int startX = alignedStartX(textArea, rendered, alignment);
                buffer.setString(startX, y, rendered, style);
            }
        }
    }

    private static String alignAndClip(String row, int maxWidth, Alignment alignment) {
        if (maxWidth <= 0) {
            return "";
        }
        if (alignment == Alignment.RIGHT) {
            // Calculator-style: keep the rightmost characters when overflowing.
            return CharWidth.substringByWidthFromEnd(row, maxWidth);
        }
        // LEFT / CENTER: keep the leftmost characters when overflowing.
        return CharWidth.substringByWidth(row, maxWidth);
    }

    private static int alignedStartX(Rect area, String renderedRow, Alignment alignment) {
        int width = CharWidth.of(renderedRow);
        if (width <= 0) {
            return area.left();
        }
        switch (alignment) {
            case RIGHT:
                return Math.max(area.left(), area.right() - width);
            case CENTER:
                return area.left() + Math.max(0, (area.width() - width) / 2);
            case LEFT:
            default:
                return area.left();
        }
    }

    private static String renderGlyphRow(String text, int glyphRow) {
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            Glyph glyph = GLYPHS.get(codePoint);
            if (glyph == null) {
                glyph = DEFAULT_GLYPH;
            }

            String line = glyph.line(glyphRow);
            out.append(line);

            int pad = glyph.width() - CharWidth.of(line);
            if (pad > 0) {
                appendSpaces(out, pad);
            }

            i += Character.charCount(codePoint);
        }

        return out.toString();
    }

    private static void appendSpaces(StringBuilder sb, int count) {
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
    }

    private static List<String> splitLines(String text) {
        // Avoid regex to keep this Java 8-friendly and allocation-light.
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                String line = text.substring(start, i);
                // Handle Windows CRLF gracefully
                if (!line.isEmpty() && line.charAt(line.length() - 1) == '\r') {
                    line = line.substring(0, line.length() - 1);
                }
                lines.add(line);
                start = i + 1;
            }
        }
        String tail = text.substring(start);
        if (!tail.isEmpty() && tail.charAt(tail.length() - 1) == '\r') {
            tail = tail.substring(0, tail.length() - 1);
        }
        lines.add(tail);
        return lines;
    }

    private static Map<Integer, Glyph> createGlyphs() {
        Map<Integer, Glyph> m = new HashMap<>();

        // Space
        put(m, ' ', Glyph.of(" ", " ", " "));

        // Minimal punctuation needed for numeric displays
        put(m, '.', Glyph.of("   ", "   ", "  ."));
        put(m, '-', Glyph.of("   ", "── ", "   "));
        put(m, '+', Glyph.of(" ╷ ", "─┼─", " ╵ "));

        // Digits
        put(m, '0', Glyph.of("╭─╮", "│ │", "╰─╯"));
        put(m, '1', Glyph.of("╶┐", " │", "─┴─"));
        put(m, '2', Glyph.of("╶─╮", "┌─┘", "└─╴"));
        put(m, '3', Glyph.of("╶─╮", "╶─┤", "╶─╯"));
        put(m, '4', Glyph.of("╷ ╷", "╰─┤", "  ╵"));
        put(m, '5', Glyph.of("┌─╴", "└─╮", "──╯"));
        put(m, '6', Glyph.of("╭─╴", "├─╮", "╰─╯"));
        put(m, '7', Glyph.of("╶─┐", "  │", "  ╵"));
        put(m, '8', Glyph.of("╭─╮", "├─┤", "╰─╯"));
        put(m, '9', Glyph.of("╭─╮", "╰─┤", "╶─╯"));

        // Letters (uppercase)
        put(m, 'A', Glyph.of("╭─╮", "├─┤", "╵ ╵"));
        put(m, 'B', Glyph.of("┌╮ ", "├┴╮", "╰─╯"));
        put(m, 'C', Glyph.of("╭─╮", "│  ", "╰─╯"));
        put(m, 'D', Glyph.of("┌─╮", "│ │", "└─╯"));
        put(m, 'E', Glyph.of("┌─╴", "├─ ", "└─╴"));
        put(m, 'F', Glyph.of("┌─╴", "├─ ", "╵  "));
        put(m, 'G', Glyph.of("╭─╮", "│─╮", "╰─╯"));
        put(m, 'H', Glyph.of("╷ ╷", "├─┤", "╵ ╵"));
        put(m, 'I', Glyph.of("╶┬╴", " │ ", "╶┴╴"));
        put(m, 'J', Glyph.of(" ╶┐", "  │", "╰─╯"));
        put(m, 'K', Glyph.of("╷╭ ", "├┴╮", "╵ ╵"));
        put(m, 'L', Glyph.of("╷  ", "│  ", "└──"));
        put(m, 'M', Glyph.of("╭┬╮", "│││", "╵╵╵"));
        put(m, 'N', Glyph.of("╭─╮", "│ │", "╵ ╵"));
        put(m, 'O', Glyph.of("╭─╮", "│ │", "╰─╯"));
        put(m, 'P', Glyph.of("┌─╮", "├─╯", "╵  "));
        put(m, 'Q', Glyph.of("╭─╮", "│ │", "╰─╳"));
        put(m, 'R', Glyph.of("┌─╮", "├┬╯", "╵╰ "));
        put(m, 'S', Glyph.of("╭─╮", "╰─╮", "╰─╯"));
        put(m, 'T', Glyph.of("╶┬╴", " │ ", " ╵ "));
        put(m, 'U', Glyph.of("╷ ╷", "│ │", "╰─╯"));
        put(m, 'V', Glyph.of("╷ ╷", "│ │", "└─╯"));
        put(m, 'W', Glyph.of("╷╷╷", "│││", "╰┴╯"));
        put(m, 'X', Glyph.of("╮ ╭", "╰─╮", "╯ ╰"));
        put(m, 'Y', Glyph.of("╮ ╭", "╰┬╯", " ╵ "));
        put(m, 'Z', Glyph.of("╶─╮", " ╱ ", "╰─╴"));

        // Letters (lowercase)
        put(m, 'a', Glyph.of("   ", "╭─╮", "╰─┤"));
        put(m, 'b', Glyph.of("╷  ", "├─╮", "╰─╯"));
        put(m, 'c', Glyph.of("   ", "╭─╴", "╰─╴"));
        put(m, 'd', Glyph.of("  ╷", "╭─┤", "╰─╯"));
        put(m, 'e', Glyph.of("   ", "╭─╮", "╰─╴"));
        put(m, 'f', Glyph.of(" ╭─", "─┤ ", " ╵ "));
        put(m, 'g', Glyph.of("   ", "╭─╮", "╰─┤")); // same as a, context matters
        put(m, 'h', Glyph.of("╷  ", "├─╮", "╵ ╵"));
        put(m, 'i', Glyph.of("   ", " ╷ ", " ╵ "));
        put(m, 'j', Glyph.of("   ", "  ╷", "╰─╯"));
        put(m, 'k', Glyph.of("╷  ", "├╮ ", "╵╰ "));
        put(m, 'l', Glyph.of(" ╷ ", " │ ", " ╵ "));
        put(m, 'm', Glyph.of("   ", "╭┬╮", "╵╵╵"));
        put(m, 'n', Glyph.of("   ", "├─╮", "╵ ╵"));
        put(m, 'o', Glyph.of("   ", "╭─╮", "╰─╯"));
        put(m, 'p', Glyph.of("   ", "┌─╮", "├─╯"));
        put(m, 'q', Glyph.of("   ", "╭─╮", "╰─┤"));
        put(m, 'r', Glyph.of("   ", "├─╮", "╵  "));
        put(m, 's', Glyph.of("   ", "╭─╮", "╶─╯"));
        put(m, 't', Glyph.of(" ╷ ", "─┼─", " ╵ "));
        put(m, 'u', Glyph.of("   ", "╷ ╷", "╰─╯"));
        put(m, 'v', Glyph.of("   ", "╷ ╷", "└─╯"));
        put(m, 'w', Glyph.of("   ", "╷╷╷", "╰┴╯"));
        put(m, 'x', Glyph.of("   ", "╮ ╭", "╯ ╰"));
        put(m, 'y', Glyph.of("   ", "╮ ╭", "╰─┤"));
        put(m, 'z', Glyph.of("   ", "╶─╮", "╰─╴"));
     

        return Collections.unmodifiableMap(m);
    }

    private static void put(Map<Integer, Glyph> map, char key, Glyph glyph) {
        map.put((int) key, glyph);
    }

    /**
     * Builder for {@link BoxText}.
     */
    public static final class Builder {
        private String text = "";
        private Block block;
        private Style style = Style.EMPTY;
        private boolean uppercase = true;
        private Alignment alignment = Alignment.LEFT;

        private Builder() {}

        /**
         * Sets the text to render.
         */
        public Builder text(String text) {
            this.text = text != null ? text : "";
            return this;
        }

        /**
         * Wraps the box text in a {@link Block}.
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the base style for this widget.
         */
        public Builder style(Style style) {
            this.style = style != null ? style : Style.EMPTY;
            return this;
        }

        /**
         * Controls whether the text is uppercased before glyph lookup (default: true).
         */
        public Builder uppercase(boolean uppercase) {
            this.uppercase = uppercase;
            return this;
        }

        /**
         * Sets horizontal alignment for the rendered box text (default: LEFT).
         * <p>
         * When {@link Alignment#RIGHT} is used, overflowing text is clipped from the start
         * (keeping the rightmost characters), which is useful for numeric displays.
         */
        public Builder alignment(Alignment alignment) {
            this.alignment = alignment != null ? alignment : Alignment.LEFT;
            return this;
        }

        /**
         * Builds the widget.
         */
        public BoxText build() {
            // Validate here so render() can remain allocation-light.
            Objects.requireNonNull(style, "style");
            return new BoxText(this);
        }
    }

    private static final class Glyph {
        private final String[] lines;
        private final int width;

        private Glyph(String[] lines, int width) {
            this.lines = lines;
            this.width = width;
        }

        static Glyph of(String... lines) {
            String[] fixed = new String[GLYPH_HEIGHT];
            for (int i = 0; i < GLYPH_HEIGHT; i++) {
                fixed[i] = (lines != null && i < lines.length && lines[i] != null) ? lines[i] : "";
            }

            int w = 0;
            for (int i = 0; i < GLYPH_HEIGHT; i++) {
                w = Math.max(w, CharWidth.of(fixed[i]));
            }

            return new Glyph(fixed, w);
        }

        String line(int row) {
            if (row < 0 || row >= GLYPH_HEIGHT) {
                return "";
            }
            return lines[row];
        }

        int width() {
            return width;
        }
    }
}

