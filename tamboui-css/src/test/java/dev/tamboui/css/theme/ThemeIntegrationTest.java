/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.theme;

import org.junit.jupiter.api.Test;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.markup.CssMarkupBridge;
import dev.tamboui.text.MarkupParser;
import dev.tamboui.text.Text;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests showing Theme working with StyleEngine and CssMarkupBridge.
 */
class ThemeIntegrationTest {

    @Test
    void programmaticallyCreatedThemeWorksWithStyleEngine() {
        // Create theme programmatically
        Theme theme = Theme.builder()
                .name("test-theme")
                .dark(true)
                .primary("#3498DB")
                .error("#E74C3C")
                .success("#27AE60")
                .background("#1E1E1E")
                .text("#FFFFFF")
                .build();

        // Load into StyleEngine
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("test-theme", theme.toCss());
        engine.setActiveStylesheet("test-theme");

        // Verify colors are accessible via parseColor
        assertThat(engine.parseColor("$primary")).isPresent();
        assertThat(engine.parseColor("$primary").get().toRgb().r()).isEqualTo(52);
        assertThat(engine.parseColor("$primary").get().toRgb().g()).isEqualTo(152);
        assertThat(engine.parseColor("$primary").get().toRgb().b()).isEqualTo(219);
    }

    @Test
    void programmaticallyCreatedThemeWorksWithMarkup() {
        // Create theme
        Theme theme = Theme.builder()
                .name("markup-theme")
                .primary("#FF0000")
                .error("#00FF00")
                .success("#0000FF")
                .build();

        // Load into StyleEngine
        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("markup-theme", theme.toCss());
        engine.setActiveStylesheet("markup-theme");

        // Use with CssMarkupBridge
        CssMarkupBridge bridge = new CssMarkupBridge(engine);
        MarkupParser.StyleResolver resolver = bridge.createResolver();

        // Parse markup with semantic colors
        Text text = MarkupParser.parse("[primary]Hello[/]", resolver);

        // Verify the text has the correct color
        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).style().fg()).isPresent();

        // Should be red (primary = #FF0000)
        assertThat(text.lines().get(0).spans().get(0).style().fg().get().toRgb().r()).isEqualTo(255);
        assertThat(text.lines().get(0).spans().get(0).style().fg().get().toRgb().g()).isEqualTo(0);
        assertThat(text.lines().get(0).spans().get(0).style().fg().get().toRgb().b()).isEqualTo(0);
    }

    @Test
    void canParseExistingThemeAndModify() {
        // Parse existing theme CSS
        String originalCss = "$primary: #3498DB;\n$error: #E74C3C;\n$background: #1E1E1E;";
        Theme original = Theme.from(originalCss);

        assertThat(original.getPrimary()).isPresent();
        assertThat(original.getError()).isPresent();

        // Create modified version with additional colors
        Theme modified = Theme.builder()
                .name("modified-theme")
                .dark(original.isDark())
                .variables(original.getVariables())
                .success("#27AE60")  // Add success color
                .warning("#F39C12")  // Add warning color
                .build();

        assertThat(modified.getPrimary()).isPresent();  // Original colors preserved
        assertThat(modified.getError()).isPresent();
        assertThat(modified.getSuccess()).isPresent();  // New colors added
        assertThat(modified.getWarning()).isPresent();

        // Generate CSS for modified theme
        String modifiedCss = modified.toCss();
        assertThat(modifiedCss).contains("$primary: #3498DB");
        assertThat(modifiedCss).contains("$success: #27AE60");
    }

    @Test
    void minimalisticThemeWorksInPractice() {
        // Create minimal theme with only essential colors
        Theme minimal = Theme.builder()
                .name("minimal")
                .primary("#007ACC")
                .background("#FFFFFF")
                .text("#000000")
                .build();

        StyleEngine engine = StyleEngine.create();
        engine.addStylesheet("minimal", minimal.toCss());
        engine.setActiveStylesheet("minimal");

        // Missing colors return Optional.empty() but don't break anything
        assertThat(minimal.getError()).isEmpty();
        assertThat(minimal.getWarning()).isEmpty();

        // Can still use the colors that ARE defined
        assertThat(engine.parseColor("$primary")).isPresent();
        assertThat(engine.parseColor("$background")).isPresent();

        // Missing colors just aren't resolvable
        assertThat(engine.parseColor("$error")).isEmpty();
    }

    @Test
    void darkThemeMetadataIsPreserved() {
        Theme dark = Theme.builder()
                .name("dark-theme")
                .dark(true)
                .background("#1E1E1E")
                .text("#FFFFFF")
                .build();

        String css = dark.toCss();

        // Re-parse the CSS
        Theme reparsed = Theme.from(css);

        // Dark mode should be inferred from colors
        assertThat(reparsed.isDark()).isTrue();
    }

    @Test
    void lightThemeMetadataIsPreserved() {
        Theme light = Theme.builder()
                .name("light-theme")
                .dark(false)
                .background("#FFFFFF")
                .text("#000000")
                .build();

        String css = light.toCss();

        // Re-parse the CSS
        Theme reparsed = Theme.from(css);

        // Light mode should be inferred from colors
        assertThat(reparsed.isDark()).isFalse();
    }
}
