/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.markup;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.theme.ThemePropertyNames;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.MarkupParser;

import static org.assertj.core.api.Assertions.assertThat;

class CssMarkupBridgeTest {

    private StyleEngine engine;
    private CssMarkupBridge bridge;

    @BeforeEach
    void setUp() throws IOException {
        engine = StyleEngine.create();

        // Load a test theme with semantic tokens
        String css = "$primary: #ff0000;\n"
                + "$secondary: #00ff00;\n"
                + "$error: #ff00ff;\n"
                + "$success: #00ffff;\n";

        engine.addStylesheet("test-theme", css);
        engine.setActiveStylesheet("test-theme");

        bridge = new CssMarkupBridge(engine);
    }

    @Test
    void createResolverReturnsNonNullResolver() {
        MarkupParser.StyleResolver resolver = bridge.createResolver();
        assertThat(resolver).isNotNull();
    }

    @Test
    void resolverResolvesPrimaryColor() {
        MarkupParser.StyleResolver resolver = bridge.createResolver();
        Style style = resolver.resolve(ThemePropertyNames.PRIMARY);

        assertThat(style).isNotNull();
        assertThat(style.fg()).isPresent();

        Color.Rgb rgb = style.fg().get().toRgb();
        assertThat(rgb.r()).isEqualTo(255);
        assertThat(rgb.g()).isEqualTo(0);
        assertThat(rgb.b()).isEqualTo(0);
    }

    @Test
    void resolverResolvesSecondaryColor() {
        MarkupParser.StyleResolver resolver = bridge.createResolver();
        Style style = resolver.resolve(ThemePropertyNames.SECONDARY);

        assertThat(style).isNotNull();
        assertThat(style.fg()).isPresent();

        Color.Rgb rgb = style.fg().get().toRgb();
        assertThat(rgb.r()).isEqualTo(0);
        assertThat(rgb.g()).isEqualTo(255);
        assertThat(rgb.b()).isEqualTo(0);
    }

    @Test
    void resolverResolvesErrorColor() {
        MarkupParser.StyleResolver resolver = bridge.createResolver();
        Style style = resolver.resolve(ThemePropertyNames.ERROR);

        assertThat(style).isNotNull();
        assertThat(style.fg()).isPresent();

        Color.Rgb rgb = style.fg().get().toRgb();
        assertThat(rgb.r()).isEqualTo(255);
        assertThat(rgb.g()).isEqualTo(0);
        assertThat(rgb.b()).isEqualTo(255);
    }

    @Test
    void resolverReturnsNullForUnknownVariable() {
        MarkupParser.StyleResolver resolver = bridge.createResolver();
        Style style = resolver.resolve("unknown-variable");

        assertThat(style).isNull();
    }

    @Test
    void resolverReturnsNullForInvalidColorValue() throws IOException {
        // Create a theme with invalid color value
        String css = "$invalid: not-a-color;";
        engine.addStylesheet("invalid-theme", css);
        engine.setActiveStylesheet("invalid-theme");

        CssMarkupBridge invalidBridge = new CssMarkupBridge(engine);
        MarkupParser.StyleResolver resolver = invalidBridge.createResolver();

        Style style = resolver.resolve("invalid");
        assertThat(style).isNull();
    }

    @Test
    void themeSwitchChangesResolvedColors() throws IOException {
        // Create second theme with different colors
        String css2 = "$primary: #0000ff;";
        engine.addStylesheet("theme2", css2);

        MarkupParser.StyleResolver resolver = bridge.createResolver();

        // Check color with first theme
        engine.setActiveStylesheet("test-theme");
        Style style1 = resolver.resolve(ThemePropertyNames.PRIMARY);
        assertThat(style1.fg()).isPresent();
        Color.Rgb rgb1 = style1.fg().get().toRgb();
        assertThat(rgb1.r()).isEqualTo(255);
        assertThat(rgb1.g()).isEqualTo(0);
        assertThat(rgb1.b()).isEqualTo(0);

        // Switch theme and check again
        engine.setActiveStylesheet("theme2");
        Style style2 = resolver.resolve(ThemePropertyNames.PRIMARY);
        assertThat(style2.fg()).isPresent();
        Color.Rgb rgb2 = style2.fg().get().toRgb();
        assertThat(rgb2.r()).isEqualTo(0);
        assertThat(rgb2.g()).isEqualTo(0);
        assertThat(rgb2.b()).isEqualTo(255);
    }
}
