/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.buffer;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.AnsiStringBuilder;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BufferAnsiStringHyperlinkTest {

    @Test
    void toAnsiStringTrimmedEmitsOsc8HyperlinkSequences() {
        Buffer buffer = Buffer.empty(Rect.of(1, 1));
        buffer.setLine(0, 0, Line.from(Span.styled("X", Style.EMPTY.hyperlink("https://example.com"))));

        String ansi = buffer.toAnsiStringTrimmed();

        String start = AnsiStringBuilder.hyperlinkStart(Hyperlink.of("https://example.com"));
        String end = AnsiStringBuilder.hyperlinkEnd();

        assertThat(ansi).contains(start);
        assertThat(ansi).contains("X");
        assertThat(ansi).contains(end);

        assertThat(ansi.indexOf(start)).isLessThan(ansi.indexOf("X"));
        assertThat(ansi.indexOf("X")).isLessThan(ansi.indexOf(end));
    }
}


