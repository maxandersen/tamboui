/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.text.markup.Markup;

class ThemeTest {

    @Test
    @DisplayName("defaultTheme includes common semantic names")
    void defaultThemeIncludesCommonNames() {
        Theme theme = Theme.defaultTheme();

        assertThat(theme.get("error")).isPresent();
        assertThat(theme.get("warning")).isPresent();
        assertThat(theme.get("success")).isPresent();
        assertThat(theme.get("info")).isPresent();
        assertThat(theme.get("debug")).isPresent();
        assertThat(theme.get("trace")).isPresent();
        assertThat(theme.get("highlight")).isPresent();
        assertThat(theme.get("accent")).isPresent();
        assertThat(theme.get("muted")).isPresent();
    }

    @Test
    @DisplayName("get is case-insensitive")
    void getIsCaseInsensitive() {
        Theme theme = Theme.defaultTheme();

        assertThat(theme.get("ERROR")).isPresent();
        assertThat(theme.get("Error")).isPresent();
        assertThat(theme.get("  error  ")).isPresent();
    }

    @Test
    @DisplayName("custom theme can be built")
    void customThemeCanBeBuilt() {
        Theme theme = Theme.builder()
            .style("custom", Style.EMPTY.fg(Color.MAGENTA).bold())
            .build();

        assertThat(theme.get("custom")).isPresent();
        assertThat(theme.get("custom").get().fg().get()).isEqualTo(Color.MAGENTA);
    }

    @Test
    @DisplayName("empty theme returns empty for all names")
    void emptyThemeReturnsEmpty() {
        Theme theme = Theme.empty();

        assertThat(theme.get("error")).isEmpty();
        assertThat(theme.get("anything")).isEmpty();
    }

    @Test
    @DisplayName("merge combines themes with other overriding this")
    void mergeCombinesThemes() {
        Theme base = Theme.builder()
            .style("error", Style.EMPTY.fg(Color.RED))
            .style("warning", Style.EMPTY.fg(Color.YELLOW))
            .build();

        Theme override = Theme.builder()
            .style("error", Style.EMPTY.fg(Color.MAGENTA))  // Override
            .style("info", Style.EMPTY.fg(Color.BLUE))      // New
            .build();

        Theme merged = base.merge(override);

        // Overridden
        assertThat(merged.get("error")).isPresent();
        assertThat(merged.get("error").get().fg().get()).isEqualTo(Color.MAGENTA);

        // Preserved
        assertThat(merged.get("warning")).isPresent();
        assertThat(merged.get("warning").get().fg().get()).isEqualTo(Color.YELLOW);

        // New
        assertThat(merged.get("info")).isPresent();
        assertThat(merged.get("info").get().fg().get()).isEqualTo(Color.BLUE);
    }

    @Test
    @DisplayName("Markup.render uses theme for semantic names")
    void markupRenderUsesTheme() {
        Theme theme = Theme.builder()
            .style("error", Style.EMPTY.fg(Color.RED).bold())
            .style("success", Style.EMPTY.fg(Color.GREEN).bold())
            .build();

        Text text = Markup.render("[error]Failed![/error] [success]OK![/success]", Style.EMPTY, theme);

        // Should have spans with the theme styles (space between words creates a separate span)
        assertThat(text.lines()).hasSize(1);
        Line line = text.lines().get(0);
        assertThat(line.spans().size()).isGreaterThanOrEqualTo(2);

        // Find the error and success spans (may have a space span in between)
        Span errorSpan = null;
        Span successSpan = null;
        for (Span span : line.spans()) {
            if (span.content().contains("Failed!")) {
                errorSpan = span;
            } else if (span.content().contains("OK!")) {
                successSpan = span;
            }
        }

        assertThat(errorSpan).isNotNull();
        assertThat(errorSpan.style().fg().get()).isEqualTo(Color.RED);
        assertThat(errorSpan.style().effectiveModifiers()).contains(Modifier.BOLD);

        assertThat(successSpan).isNotNull();
        assertThat(successSpan.style().fg().get()).isEqualTo(Color.GREEN);
        assertThat(successSpan.style().effectiveModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("Markup.render falls back to StyleParser when theme name not found")
    void markupRenderFallsBackToStyleParser() {
        Theme theme = Theme.empty();  // No semantic names

        Text text = Markup.render("[bold red]Hello[/]", Style.EMPTY, theme);

        // Should still work via StyleParser
        assertThat(text.lines()).hasSize(1);
        Line line = text.lines().get(0);
        assertThat(line.spans()).hasSize(1);
        Span span = line.spans().get(0);
        assertThat(span.content()).isEqualTo("Hello");
        assertThat(span.style().fg().get()).isEqualTo(Color.RED);
        assertThat(span.style().effectiveModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("default theme works with Markup.render")
    void defaultThemeWorksWithMarkup() {
        Text text = Markup.render("[error]Error![/error] [success]Success![/success]");

        // Should use default theme
        assertThat(text.lines()).hasSize(1);
        Line line = text.lines().get(0);
        assertThat(line.spans().size()).isGreaterThanOrEqualTo(2);
    }
}

