/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.markup;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.theme.ThemePropertyNames;
import dev.tamboui.style.Color;
import dev.tamboui.text.MarkupParser;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that themed markup actually produces styled text.
 */
class ThemeRenderingTest {

    private StyleEngine engine;
    private CssMarkupBridge bridge;
    private MarkupParser.StyleResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        engine = StyleEngine.create();

        // Load catppuccin-mocha theme
        engine.loadStylesheet("catppuccin-mocha", "/themes/catppuccin-mocha.tcss");
        engine.setActiveStylesheet("catppuccin-mocha");

        bridge = new CssMarkupBridge(engine);
        resolver = bridge.createResolver();
    }

    @Test
    void markedUpTextHasColor() {
        Text text = MarkupParser.parse("[" + ThemePropertyNames.PRIMARY + "]Hello[/]", resolver);

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).hasSize(1);
        Span span = text.lines().get(0).spans().get(0);
        assertThat(span.content()).isEqualTo("Hello");
        assertThat(span.style().fg()).isPresent();

        // Catppuccin Mocha primary is #cba6f7 (mauve)
        Color.Rgb rgb = span.style().fg().get().toRgb();
        assertThat(rgb.r()).isEqualTo(203);
        assertThat(rgb.g()).isEqualTo(166);
        assertThat(rgb.b()).isEqualTo(247);
    }

    @Test
    void switchingThemesChangesColors() throws IOException {
        // Parse with catppuccin-mocha active
        Text text1 = MarkupParser.parse("[" + ThemePropertyNames.PRIMARY + "]Test[/]", resolver);
        Color.Rgb rgb1 = text1.lines().get(0).spans().get(0).style().fg().get().toRgb();

        // Switch to dracula
        engine.loadStylesheet("dracula", "/themes/dracula.tcss");
        engine.setActiveStylesheet("dracula");

        // Parse again - should get dracula's primary color
        Text text2 = MarkupParser.parse("[" + ThemePropertyNames.PRIMARY + "]Test[/]", resolver);
        Color.Rgb rgb2 = text2.lines().get(0).spans().get(0).style().fg().get().toRgb();

        // Colors should be different
        assertThat(rgb1.r()).isNotEqualTo(rgb2.r());

        // Dracula primary is #bd93f9
        assertThat(rgb2.r()).isEqualTo(189);
        assertThat(rgb2.g()).isEqualTo(147);
        assertThat(rgb2.b()).isEqualTo(249);
    }

    @Test
    void multipleSemanticColorsInOneString() {
        String markup = "[" + ThemePropertyNames.SUCCESS + "]✓ Success[/] and ["
                + ThemePropertyNames.ERROR + "]✗ Error[/]";
        Text text = MarkupParser.parse(markup, resolver);

        assertThat(text.lines()).hasSize(1);
        List<Span> spans = text.lines().get(0).spans();
        assertThat(spans).hasSizeGreaterThanOrEqualTo(2);

        // Find success and error spans
        Span successSpan = spans.stream()
                .filter(s -> s.content().contains("Success"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Success span not found"));
        Span errorSpan = spans.stream()
                .filter(s -> s.content().contains("Error"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Error span not found"));

        // Both should have colors
        assertThat(successSpan.style().fg()).isPresent();
        assertThat(errorSpan.style().fg()).isPresent();

        // And they should be different colors
        Color successColor = successSpan.style().fg().get();
        Color errorColor = errorSpan.style().fg().get();
        assertThat(successColor.toRgb()).isNotEqualTo(errorColor.toRgb());
    }
}
