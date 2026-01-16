/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text.markup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Style;
import dev.tamboui.style.StyleParser;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;

class MarkupTest {

    @Test
    void escapeAddsBackslashBeforeMarkupLookingTag() {
        assertThat(Markup.escape("Hello [red]World[/red]"))
            .isEqualTo("Hello \\[red]World\\[/red]");
    }

    @Test
    void renderAppliesStyleUntilEndWhenNotClosed() {
        Text text = Markup.render("[bold]Hello");
        assertThat(text.rawContent()).isEqualTo("Hello");
        assertThat(text.lines()).hasSize(1);

        Line line = text.lines().get(0);
        assertThat(line.spans()).containsExactly(
            Span.styled("Hello", StyleParser.parse("bold"))
        );
    }

    @Test
    void renderImplicitCloseStopsStyle() {
        Text text = Markup.render("[bold red]Hello[/] World");
        assertThat(text.rawContent()).isEqualTo("Hello World");
        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).containsExactly(
            Span.styled("Hello", StyleParser.parse("bold red")),
            Span.styled(" World", Style.EMPTY)
        );
    }

    @Test
    void renderSplitsLinesAndKeepsStylesPerLine() {
        Text text = Markup.render("[bold red]Hello[/]\nWorld");
        assertThat(text.rawContent()).isEqualTo("Hello\nWorld");
        assertThat(text.lines()).hasSize(2);
        assertThat(text.lines().get(0).spans()).containsExactly(
            Span.styled("Hello", StyleParser.parse("bold red"))
        );
        assertThat(text.lines().get(1).spans()).containsExactly(
            Span.styled("World", Style.EMPTY)
        );
    }

    @Test
    void renderNestedColorTagsOverrideForeground() {
        Text text = Markup.render("[blue][green][red]R[/red]G[/green]B[/blue]");
        assertThat(text.rawContent()).isEqualTo("RGB");
        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).containsExactly(
            Span.styled("R", Style.EMPTY.fg(Color.RED)),
            Span.styled("G", Style.EMPTY.fg(Color.GREEN)),
            Span.styled("B", Style.EMPTY.fg(Color.BLUE))
        );
    }

    @Test
    void renderSupportsRgbWithSpaces() {
        Text text = Markup.render("[bold rgb(10, 20, 30)]Hello");
        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).containsExactly(
            Span.styled("Hello", StyleParser.parse("bold rgb(10, 20, 30)"))
        );
    }

    @Test
    void renderThrowsOnMismatchedExplicitClose() {
        assertThatThrownBy(() -> Markup.render("foo[/]"))
            .isInstanceOf(MarkupParseException.class);
    }

    @Test
    void renderLinkCreatesHyperlinkStyleExtension() {
        Text text = Markup.render("[link=https://example.com]x[/link]");
        assertThat(text.rawContent()).isEqualTo("x");
        Style style = text.lines().get(0).spans().get(0).style();
        assertThat(style.hyperlink()).contains(Hyperlink.of("https://example.com"));
    }
}


