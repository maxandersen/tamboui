/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.theme;

import org.junit.jupiter.api.Test;

import dev.tamboui.css.model.Stylesheet;

import static org.assertj.core.api.Assertions.assertThat;

class ThemeTest {

    @Test
    void builderCreatesThemeWithSemanticColors() {
        Theme theme = Theme.builder()
                .name("test-theme")
                .dark(true)
                .primary("#FF0000")
                .error("#00FF00")
                .build();

        assertThat(theme.getName()).isEqualTo("test-theme");
        assertThat(theme.isDark()).isTrue();
        assertThat(theme.getPrimary()).isPresent();
        assertThat(theme.getPrimary().get().toRgb().r()).isEqualTo(255);
        assertThat(theme.getError()).isPresent();
        assertThat(theme.getError().get().toRgb().r()).isEqualTo(0);
    }

    @Test
    void minimalThemeOnlyStoresDefinedColors() {
        // Theme with only primary - NO auto-expansion
        Theme minimal = Theme.builder()
                .name("minimal")
                .primary("#3498DB")
                .build();

        assertThat(minimal.getPrimary()).isPresent();
        assertThat(minimal.getError()).isEmpty();
        assertThat(minimal.getWarning()).isEmpty();
        assertThat(minimal.getSuccess()).isEmpty();
        assertThat(minimal.getInfo()).isEmpty();
        assertThat(minimal.getSecondary()).isEmpty();
        assertThat(minimal.getAccent()).isEmpty();
        assertThat(minimal.getBackground()).isEmpty();
    }

    @Test
    void toCssGeneratesOnlyDefinedVariables() {
        Theme theme = Theme.builder()
                .name("partial")
                .dark(false)
                .primary("#3498DB")
                .error("#E74C3C")
                .build();

        String css = theme.toCss();
        assertThat(css).contains("/* Theme: partial (light) */");
        assertThat(css).contains("$primary: #3498DB;");
        assertThat(css).contains("$error: #E74C3C;");

        // Should NOT contain warning, success, etc.
        assertThat(css).doesNotContain("$warning");
        assertThat(css).doesNotContain("$success");
        assertThat(css).doesNotContain("$secondary");
    }

    @Test
    void darkThemeIncludesDarkAnnotation() {
        Theme dark = Theme.builder()
                .name("dark-theme")
                .dark(true)
                .primary("#000000")
                .build();

        assertThat(dark.toCss()).contains("(dark)");
    }

    @Test
    void lightThemeIncludesLightAnnotation() {
        Theme light = Theme.builder()
                .name("light-theme")
                .dark(false)
                .primary("#FFFFFF")
                .build();

        assertThat(light.toCss()).contains("(light)");
    }

    @Test
    void fromCssParsesOnlyPresentVariables() {
        String css = "$primary: #FF0000;\n$error: #00FF00;";
        Theme theme = Theme.from(css);

        assertThat(theme.getVariable("primary")).hasValue("#FF0000");
        assertThat(theme.getPrimary()).isPresent();
        assertThat(theme.getError()).isPresent();

        // Warning not in CSS = empty
        assertThat(theme.getWarning()).isEmpty();
        assertThat(theme.getSuccess()).isEmpty();
    }

    @Test
    void roundTripPreservesOnlyDefinedVariables() {
        Theme original = Theme.builder()
                .name("original")
                .dark(true)
                .primary("#3498DB")
                .variable("custom", "value")
                .build();

        String css = original.toCss();
        Theme parsed = Theme.from(css);

        assertThat(parsed.getVariable("primary")).isEqualTo(original.getVariable("primary"));
        assertThat(parsed.getVariable("custom")).isEqualTo(original.getVariable("custom"));
        assertThat(parsed.getVariables()).hasSize(2);
    }

    @Test
    void customVariablesArePreserved() {
        Theme theme = Theme.builder()
                .primary("#3498DB")
                .variable("border-radius", "4")
                .variable("font-size", "14px")
                .variable("spacing", "8")
                .build();

        assertThat(theme.getVariable("border-radius")).hasValue("4");
        assertThat(theme.getVariable("font-size")).hasValue("14px");
        assertThat(theme.getVariable("spacing")).hasValue("8");

        String css = theme.toCss();
        assertThat(css).contains("$border-radius: 4;");
        assertThat(css).contains("$font-size: 14px;");
        assertThat(css).contains("$spacing: 8;");
    }

    @Test
    void toStylesheetProducesValidStylesheet() {
        Theme theme = Theme.builder()
                .name("test")
                .primary("#FF0000")
                .error("#00FF00")
                .build();

        Stylesheet stylesheet = theme.toStylesheet();
        assertThat(stylesheet.variables()).containsKey("primary");
        assertThat(stylesheet.variables()).containsKey("error");
        assertThat(stylesheet.variables()).hasSize(2);
    }

    @Test
    void builderWithVariablesMapAddsAllVariables() {
        java.util.Map<String, String> vars = new java.util.LinkedHashMap<>();
        vars.put("custom1", "value1");
        vars.put("custom2", "value2");

        Theme theme = Theme.builder()
                .primary("#FF0000")
                .variables(vars)
                .build();

        assertThat(theme.getVariable("primary")).hasValue("#FF0000");
        assertThat(theme.getVariable("custom1")).hasValue("value1");
        assertThat(theme.getVariable("custom2")).hasValue("value2");
        assertThat(theme.getVariables()).hasSize(3);
    }

    @Test
    void builderIgnoresNullVariableValues() {
        Theme theme = Theme.builder()
                .primary("#FF0000")
                .variable("null-var", null)
                .build();

        assertThat(theme.getVariable("null-var")).isEmpty();
        assertThat(theme.getVariables()).hasSize(1);
    }

    @Test
    void allSemanticTokenSettersWork() {
        Theme theme = Theme.builder()
                .name("complete")
                .dark(true)
                .primary("#111111")
                .secondary("#222222")
                .accent("#333333")
                .error("#444444")
                .warning("#555555")
                .success("#666666")
                .info("#777777")
                .background("#888888")
                .surface("#999999")
                .surfaceVariant("#AAAAAA")
                .surfaceDim("#BBBBBB")
                .text("#CCCCCC")
                .textMuted("#DDDDDD")
                .textDisabled("#EEEEEE")
                .textOnPrimary("#FFFFFF")
                .border("#F1F1F1")
                .borderFocus("#F2F2F2")
                .borderMuted("#F3F3F3")
                .build();

        assertThat(theme.getPrimary()).isPresent();
        assertThat(theme.getSecondary()).isPresent();
        assertThat(theme.getAccent()).isPresent();
        assertThat(theme.getError()).isPresent();
        assertThat(theme.getWarning()).isPresent();
        assertThat(theme.getSuccess()).isPresent();
        assertThat(theme.getInfo()).isPresent();
        assertThat(theme.getBackground()).isPresent();
        assertThat(theme.getSurface()).isPresent();
        assertThat(theme.getSurfaceVariant()).isPresent();
        assertThat(theme.getText()).isPresent();
        assertThat(theme.getTextMuted()).isPresent();
        assertThat(theme.getTextDisabled()).isPresent();
        assertThat(theme.getBorder()).isPresent();

        assertThat(theme.getVariables()).hasSize(18);
    }

    @Test
    void infersDarkModeFromBackgroundAndTextColors() {
        // Dark theme: dark background, light text
        String darkCss = "$background: #1E1E1E;\n$text: #FFFFFF;";
        Theme dark = Theme.from(darkCss);
        assertThat(dark.isDark()).isTrue();

        // Light theme: light background, dark text
        String lightCss = "$background: #FFFFFF;\n$text: #000000;";
        Theme light = Theme.from(lightCss);
        assertThat(light.isDark()).isFalse();
    }

    @Test
    void defaultsToDarkFalseWhenCannotInfer() {
        String css = "$primary: #FF0000;";  // No background/text to infer from
        Theme theme = Theme.from(css);
        assertThat(theme.isDark()).isFalse();
    }

    @Test
    void getVariablesReturnsUnmodifiableMap() {
        Theme theme = Theme.builder()
                .primary("#FF0000")
                .build();

        java.util.Map<String, String> vars = theme.getVariables();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> {
            vars.put("new-var", "value");
        }).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void builderPreservesVariableInsertionOrder() {
        Theme theme = Theme.builder()
                .variable("z-var", "1")
                .variable("a-var", "2")
                .variable("m-var", "3")
                .build();

        String css = theme.toCss();
        int zIndex = css.indexOf("$z-var");
        int aIndex = css.indexOf("$a-var");
        int mIndex = css.indexOf("$m-var");

        // Order should be preserved (z, a, m)
        assertThat(zIndex).isLessThan(aIndex);
        assertThat(aIndex).isLessThan(mIndex);
    }

    @Test
    void fromStylesheetExtractsAllVariables() {
        String css = "$primary: #FF0000;\n$custom: value;\n$error: #00FF00;";
        Stylesheet stylesheet = dev.tamboui.css.parser.CssParser.parse(css);

        Theme theme = Theme.fromStylesheet("test", stylesheet);

        assertThat(theme.getName()).isEqualTo("test");
        assertThat(theme.getVariable(ThemePropertyNames.PRIMARY)).hasValue("#FF0000");
        assertThat(theme.getVariable(ThemePropertyNames.ERROR)).hasValue("#00FF00");
        assertThat(theme.getVariable("custom")).hasValue("value");
    }

    @Test
    void generateCreatesFullPalette() {
        Theme theme = Theme.builder()
            .name("minimal")
            .dark(true)
            .primary("#3498DB")
            .luminositySpread(0.15f)
            .build();

        java.util.Map<String, String> palette = theme.generate();

        // Should have primary and all its shades
        assertThat(palette).containsKey("primary");
        assertThat(palette).containsKey("primary-lighten-1");
        assertThat(palette).containsKey("primary-lighten-2");
        assertThat(palette).containsKey("primary-lighten-3");
        assertThat(palette).containsKey("primary-darken-1");
        assertThat(palette).containsKey("primary-darken-2");
        assertThat(palette).containsKey("primary-darken-3");

        // Should derive secondary, error, warning, success from primary
        assertThat(palette).containsKey("secondary");
        assertThat(palette).containsKey("error");
        assertThat(palette).containsKey("warning");
        assertThat(palette).containsKey("success");

        // Should have muted variants
        assertThat(palette).containsKey("primary-muted");
    }

    @Test
    void generateRequiresPrimaryColor() {
        Theme theme = Theme.builder()
            .name("no-primary")
            .error("#E74C3C")
            .build();

        // Should throw because primary is required for generation
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> theme.generate())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Primary color required");
    }

    @Test
    void generateUsesExplicitColorsWhenProvided() {
        Theme theme = Theme.builder()
            .primary("#3498DB")
            .secondary("#2C3E50")  // Explicit, not derived
            .error("#E74C3C")      // Explicit, not derived
            .build();

        java.util.Map<String, String> palette = theme.generate();

        // Should use explicit values, not derive from primary
        assertThat(palette.get("secondary")).isEqualTo("#2c3e50");  // May be lowercase
        assertThat(palette.get("error")).isEqualTo("#e74c3c");
    }

    @Test
    void luminositySpreadControlsShadeGeneration() {
        Theme small = Theme.builder()
            .primary("#808080")  // Gray
            .luminositySpread(0.1f)
            .build();

        Theme large = Theme.builder()
            .primary("#808080")  // Gray
            .luminositySpread(0.3f)
            .build();

        java.util.Map<String, String> smallPalette = small.generate();
        java.util.Map<String, String> largePalette = large.generate();

        // Larger spread should produce lighter lighten-3 and darker darken-3
        // (We can't easily assert exact values, but they should be different)
        assertThat(smallPalette.get("primary-lighten-3"))
            .isNotEqualTo(largePalette.get("primary-lighten-3"));
        assertThat(smallPalette.get("primary-darken-3"))
            .isNotEqualTo(largePalette.get("primary-darken-3"));
    }
}
