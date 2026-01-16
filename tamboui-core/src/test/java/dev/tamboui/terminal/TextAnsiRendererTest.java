/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextAnsiRendererTest {

    @Test
    void toAnsiEmitsOsc8HyperlinkSequences() {
        Text text = Text.from(Line.from(Span.styled("X", Style.EMPTY.hyperlink("https://example.com"))));
        String ansi = TextAnsiRenderer.toAnsi(text);

        String start = AnsiStringBuilder.hyperlinkStart(Hyperlink.of("https://example.com"));
        String end = AnsiStringBuilder.hyperlinkEnd();

        assertThat(ansi).contains(start);
        assertThat(ansi).contains("X");
        assertThat(ansi).contains(end);
        assertThat(ansi).endsWith(AnsiStringBuilder.RESET);
    }
}


