/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.util.List;
import java.util.Objects;

import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;

/**
 * Renders {@link Text}/{@link Line} to an ANSI-escaped string (including OSC8 hyperlinks).
 *
 * <p>This is useful for CLI tools that want Rich-like formatted output without using widgets / buffers.
 */
public final class TextAnsiRenderer {

    private TextAnsiRenderer() {
    }

    /**
     * Render {@link Text} to an ANSI string. The returned string always ends with {@link AnsiStringBuilder#RESET}
     * if there is any content.
     *
     * @param text the text to render
     * @return ANSI escaped string
     */
    public static String toAnsi(Text text) {
        if (text == null || text.lines().isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        List<Line> lines = text.lines();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(toAnsiLine(lines.get(i)));
        }
        out.append(AnsiStringBuilder.RESET);
        return out.toString();
    }

    /**
     * Render a {@link Line} to an ANSI string (without a trailing RESET).
     *
     * @param line the line to render
     * @return ANSI escaped string for the line
     */
    public static String toAnsiLine(Line line) {
        if (line == null || line.spans().isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        Style lastStyle = null;
        Hyperlink lastHyperlink = null;

        for (Span span : line.spans()) {
            Style style = span.style();
            if (!style.equals(lastStyle)) {
                Hyperlink currentHyperlink = style.hyperlink().orElse(null);
                if (!Objects.equals(currentHyperlink, lastHyperlink)) {
                    if (lastHyperlink != null) {
                        out.append(AnsiStringBuilder.hyperlinkEnd());
                    }
                    if (currentHyperlink != null) {
                        out.append(AnsiStringBuilder.hyperlinkStart(currentHyperlink));
                    }
                    lastHyperlink = currentHyperlink;
                }
                out.append(AnsiStringBuilder.styleToAnsi(style));
                lastStyle = style;
            }
            out.append(span.content());
        }

        if (lastHyperlink != null) {
            out.append(AnsiStringBuilder.hyperlinkEnd());
        }
        return out.toString();
    }
}


