/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.model.Stylesheet;
import dev.tamboui.css.parser.CssParser;
import dev.tamboui.style.Color;

/**
 * Immutable theme representation with semantic color tokens.
 *
 * <p>Can be built programmatically or parsed from CSS:</p>
 * <pre>{@code
 * // Build programmatically
 * Theme theme = Theme.builder()
 *     .name("my-theme")
 *     .dark(true)
 *     .primary("#3498DB")
 *     .error("#E74C3C")
 *     .build();
 *
 * // Parse from CSS
 * Theme existing = Theme.from(cssString);
 *
 * // Generate CSS
 * String css = theme.toCss();
 *
 * // Load into StyleEngine
 * engine.addStylesheet("my-theme", theme.toCss());
 * }</pre>
 *
 * <p><b>Important:</b> Theme only stores explicitly defined variables.
 * It does NOT auto-generate missing semantic colors in the constructor.
 * Use the {@link #generate()} method to create a complete color palette from minimal input.</p>
 */
public final class Theme {
    private final String name;
    private final boolean dark;
    private final float luminositySpread;
    private final Map<String, String> variables;
    private final StyleEngine styleEngine;

    private Theme(String name, boolean dark, float luminositySpread, Map<String, String> variables) {
        this.name = name;
        this.dark = dark;
        this.luminositySpread = luminositySpread;
        this.variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
        this.styleEngine = StyleEngine.create();
    }

    // ===== Factory Methods =====

    /**
     * Creates a new theme builder.
     *
     * @return new builder instance
     */
    public static ThemeBuilder builder() {
        return new ThemeBuilder();
    }

    /**
     * Parses a theme from CSS content.
     *
     * @param cssContent the CSS/TCSS content
     * @return parsed theme
     */
    public static Theme from(String cssContent) {
        Stylesheet stylesheet = CssParser.parse(cssContent);
        return fromStylesheet("parsed-theme", stylesheet);
    }

    /**
     * Creates a theme from a Stylesheet.
     *
     * @param name the theme name
     * @param stylesheet the stylesheet
     * @return theme with variables from stylesheet
     */
    public static Theme fromStylesheet(String name, Stylesheet stylesheet) {
        boolean dark = inferDarkMode(stylesheet);
        float luminositySpread = 0.15f;  // Default value (Textual's default)
        return new Theme(name, dark, luminositySpread, stylesheet.variables());
    }

    /**
     * Infers whether theme is dark based on background/text colors.
     * Heuristic: if background is darker than text, it's a dark theme.
     */
    private static boolean inferDarkMode(Stylesheet stylesheet) {
        Optional<String> bg = stylesheet.resolveVariable(ThemePropertyNames.BACKGROUND);
        Optional<String> text = stylesheet.resolveVariable(ThemePropertyNames.TEXT);

        if (bg.isPresent() && text.isPresent()) {
            StyleEngine engine = StyleEngine.create();
            Optional<Color> bgColor = engine.parseColor(bg.get());
            Optional<Color> textColor = engine.parseColor(text.get());

            if (bgColor.isPresent() && textColor.isPresent()) {
                double bgLightness = getLightness(bgColor.get());
                double textLightness = getLightness(textColor.get());
                return bgLightness < textLightness;
            }
        }

        return false;
    }

    /**
     * Calculates perceived lightness of a color (0.0 = black, 1.0 = white).
     * Uses standard relative luminance formula.
     */
    private static double getLightness(Color color) {
        Color.Rgb rgb = color.toRgb();
        double r = rgb.r() / 255.0;
        double g = rgb.g() / 255.0;
        double b = rgb.b() / 255.0;

        // Relative luminance
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    // ===== Accessors =====

    /**
     * Returns the theme name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether this is a dark theme.
     * Can be used to determine appropriate contrast colors.
     *
     * @return true if dark theme, false if light theme
     */
    public boolean isDark() {
        return dark;
    }

    /**
     * Returns all variables in this theme.
     * The returned map is unmodifiable.
     *
     * @return unmodifiable map of variable name → value
     */
    public Map<String, String> getVariables() {
        return variables;
    }

    /**
     * Gets a variable value by name.
     *
     * @param name the variable name (without $)
     * @return variable value if present
     */
    public Optional<String> getVariable(String name) {
        return Optional.ofNullable(variables.get(name));
    }

    // ===== Type-Safe Semantic Token Getters =====

    /**
     * Gets the primary color, if defined.
     *
     * @return primary color or empty if not defined
     */
    public Optional<Color> getPrimary() {
        return getColorVariable(ThemePropertyNames.PRIMARY);
    }

    /**
     * Gets the secondary color, if defined.
     *
     * @return secondary color or empty if not defined
     */
    public Optional<Color> getSecondary() {
        return getColorVariable(ThemePropertyNames.SECONDARY);
    }

    /**
     * Gets the accent color, if defined.
     *
     * @return accent color or empty if not defined
     */
    public Optional<Color> getAccent() {
        return getColorVariable(ThemePropertyNames.ACCENT);
    }

    /**
     * Gets the error color, if defined.
     *
     * @return error color or empty if not defined
     */
    public Optional<Color> getError() {
        return getColorVariable(ThemePropertyNames.ERROR);
    }

    /**
     * Gets the warning color, if defined.
     *
     * @return warning color or empty if not defined
     */
    public Optional<Color> getWarning() {
        return getColorVariable(ThemePropertyNames.WARNING);
    }

    /**
     * Gets the success color, if defined.
     *
     * @return success color or empty if not defined
     */
    public Optional<Color> getSuccess() {
        return getColorVariable(ThemePropertyNames.SUCCESS);
    }

    /**
     * Gets the info color, if defined.
     *
     * @return info color or empty if not defined
     */
    public Optional<Color> getInfo() {
        return getColorVariable(ThemePropertyNames.INFO);
    }

    /**
     * Gets the background color, if defined.
     *
     * @return background color or empty if not defined
     */
    public Optional<Color> getBackground() {
        return getColorVariable(ThemePropertyNames.BACKGROUND);
    }

    /**
     * Gets the surface color, if defined.
     *
     * @return surface color or empty if not defined
     */
    public Optional<Color> getSurface() {
        return getColorVariable(ThemePropertyNames.SURFACE);
    }

    /**
     * Gets the surface variant color, if defined.
     *
     * @return surface variant color or empty if not defined
     */
    public Optional<Color> getSurfaceVariant() {
        return getColorVariable(ThemePropertyNames.SURFACE_VARIANT);
    }

    /**
     * Gets the text color, if defined.
     *
     * @return text color or empty if not defined
     */
    public Optional<Color> getText() {
        return getColorVariable(ThemePropertyNames.TEXT);
    }

    /**
     * Gets the muted text color, if defined.
     *
     * @return muted text color or empty if not defined
     */
    public Optional<Color> getTextMuted() {
        return getColorVariable(ThemePropertyNames.TEXT_MUTED);
    }

    /**
     * Gets the disabled text color, if defined.
     *
     * @return disabled text color or empty if not defined
     */
    public Optional<Color> getTextDisabled() {
        return getColorVariable(ThemePropertyNames.TEXT_DISABLED);
    }

    /**
     * Gets the border color, if defined.
     *
     * @return border color or empty if not defined
     */
    public Optional<Color> getBorder() {
        return getColorVariable(ThemePropertyNames.BORDER);
    }

    /**
     * Helper to get and parse a color variable.
     */
    private Optional<Color> getColorVariable(String name) {
        return getVariable(name)
                .flatMap(value -> styleEngine.parseColor(value));
    }

    // ===== Palette Generation =====

    /**
     * Generates a complete color palette from the theme's base colors.
     *
     * <p>Inspired by Textual's ColorSystem.generate(). Creates a full palette including:</p>
     * <ul>
     *   <li>Lightened variants (primary-lighten-1, primary-lighten-2, primary-lighten-3)</li>
     *   <li>Darkened variants (primary-darken-1, primary-darken-2, primary-darken-3)</li>
     *   <li>Derived colors (if secondary/error/warning not defined, derive from primary)</li>
     *   <li>Muted variants (primary-muted, secondary-muted, error-muted)</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>{@code
     * Theme theme = Theme.builder()
     *     .dark(true)
     *     .primary("#3498DB")
     *     .luminositySpread(0.15f)
     *     .build();
     *
     * Map<String, String> palette = theme.generate();
     * // Contains: primary, primary-lighten-1, primary-darken-1,
     * //           secondary (derived), error (derived), etc.
     * }</pre>
     *
     * @return map of color name → hex value for complete palette
     * @throws IllegalStateException if primary color is not defined
     */
    public Map<String, String> generate() {
        Map<String, String> palette = new LinkedHashMap<>();

        // Get base colors with fallbacks
        Color primary = getPrimary().orElseThrow(() ->
            new IllegalStateException("Primary color required for palette generation"));
        Color secondary = getSecondary().orElse(primary);
        Color accent = getAccent().orElse(primary);
        Color error = getError().orElse(primary.rotateHue(180));  // Complementary
        Color warning = getWarning().orElse(primary.rotateHue(30));  // Analogous
        Color success = getSuccess().orElse(primary.rotateHue(-30));  // Analogous

        // Generate shades for each color (-3, -2, -1, 0, +1, +2, +3)
        generateShades(palette, ThemePropertyNames.PRIMARY, primary);
        generateShades(palette, ThemePropertyNames.SECONDARY, secondary);
        generateShades(palette, ThemePropertyNames.ACCENT, accent);
        generateShades(palette, ThemePropertyNames.ERROR, error);
        generateShades(palette, ThemePropertyNames.WARNING, warning);
        generateShades(palette, ThemePropertyNames.SUCCESS, success);

        // Generate muted variants
        Color background = getBackground().orElse(dark ?
            Color.rgb(18, 18, 18) : Color.rgb(255, 255, 255));
        palette.put(ThemePropertyNames.PRIMARY + "-muted",
            primary.mix(background, 0.7f).toHex());
        palette.put(ThemePropertyNames.SECONDARY + "-muted",
            secondary.mix(background, 0.7f).toHex());
        palette.put(ThemePropertyNames.ERROR + "-muted",
            error.mix(background, 0.7f).toHex());

        return palette;
    }

    private void generateShades(Map<String, String> palette, String name, Color baseColor) {
        float step = luminositySpread / 2.0f;  // Each shade is half the spread

        palette.put(name, baseColor.toHex());

        for (int i = 1; i <= 3; i++) {
            float delta = step * i;
            palette.put(name + "-lighten-" + i, baseColor.lighten(delta).toHex());
            palette.put(name + "-darken-" + i, baseColor.darken(delta).toHex());
        }
    }

    // ===== CSS Generation =====

    /**
     * Generates CSS content that can be loaded into StyleEngine.
     *
     * <p>Output format:</p>
     * <pre>{@code
     * /* Theme: my-theme (dark) *‍/
     * $primary: #3498DB;
     * $error: #E74C3C;
     * ...
     * }</pre>
     *
     * <p><b>Important:</b> Only generates variables that were explicitly
     * defined. Does NOT auto-generate missing semantic tokens.</p>
     *
     * @return CSS content
     */
    public String toCss() {
        StringBuilder sb = new StringBuilder();

        // Add header comment with theme metadata
        sb.append("/* Theme: ").append(name);
        if (dark) {
            sb.append(" (dark)");
        } else {
            sb.append(" (light)");
        }
        sb.append(" */\n\n");

        // Generate variable declarations in definition order
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            sb.append("$")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append(";\n");
        }

        return sb.toString();
    }

    /**
     * Converts this theme to a Stylesheet object.
     *
     * @return stylesheet representation
     */
    public Stylesheet toStylesheet() {
        return CssParser.parse(toCss());
    }

    // ===== Builder =====

    /**
     * Fluent builder for creating themes programmatically.
     *
     * <p>Example:</p>
     * <pre>{@code
     * Theme theme = Theme.builder()
     *     .name("gruvbox-dark")
     *     .dark(true)
     *     .primary("#D3869B")
     *     .secondary("#83A598")
     *     .warning("#FABD2F")
     *     .error("#FB4934")
     *     .success("#B8BB26")
     *     .background("#282828")
     *     .text("#EBDBB2")
     *     .variable("border-radius", "4")
     *     .build();
     * }</pre>
     *
     * <p><b>Note:</b> Only the colors you explicitly set are stored.
     * Missing colors will return {@code Optional.empty()} from the corresponding
     * getters.</p>
     */
    public static final class ThemeBuilder {
        private final Map<String, String> variables;
        private String themeName;
        private boolean dark;
        private float luminositySpread;

        private ThemeBuilder() {
            this.variables = new LinkedHashMap<>();
            this.themeName = "default-theme";
            this.dark = false;
            this.luminositySpread = 0.15f;  // Textual's default
        }

        /**
         * Sets the theme name.
         *
         * @param name the theme name
         * @return this builder
         */
        public ThemeBuilder name(String name) {
            this.themeName = name;
            return this;
        }

        /**
         * Sets whether this is a dark theme.
         * Used for appropriate contrast and surface color selection.
         *
         * @param dark true for dark theme, false for light theme
         * @return this builder
         */
        public ThemeBuilder dark(boolean dark) {
            this.dark = dark;
            return this;
        }

        /**
         * Sets the luminosity spread for shade generation.
         * Controls how much lighter/darker the shade variants are when using {@link #generate()}.
         * Textual uses 0.15 as default.
         *
         * @param spread the spread value (typically 0.1 to 0.3)
         * @return this builder
         */
        public ThemeBuilder luminositySpread(float spread) {
            this.luminositySpread = spread;
            return this;
        }

        // ===== Semantic Color Setters =====

        /**
         * Sets the primary color.
         *
         * @param colorValue color value (hex, rgb, named color)
         * @return this builder
         */
        public ThemeBuilder primary(String colorValue) {
            variables.put(ThemePropertyNames.PRIMARY, colorValue);
            return this;
        }

        /**
         * Sets the secondary color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder secondary(String colorValue) {
            variables.put(ThemePropertyNames.SECONDARY, colorValue);
            return this;
        }

        /**
         * Sets the accent color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder accent(String colorValue) {
            variables.put(ThemePropertyNames.ACCENT, colorValue);
            return this;
        }

        /**
         * Sets the error color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder error(String colorValue) {
            variables.put(ThemePropertyNames.ERROR, colorValue);
            return this;
        }

        /**
         * Sets the warning color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder warning(String colorValue) {
            variables.put(ThemePropertyNames.WARNING, colorValue);
            return this;
        }

        /**
         * Sets the success color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder success(String colorValue) {
            variables.put(ThemePropertyNames.SUCCESS, colorValue);
            return this;
        }

        /**
         * Sets the info color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder info(String colorValue) {
            variables.put(ThemePropertyNames.INFO, colorValue);
            return this;
        }

        /**
         * Sets the background color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder background(String colorValue) {
            variables.put(ThemePropertyNames.BACKGROUND, colorValue);
            return this;
        }

        /**
         * Sets the surface color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder surface(String colorValue) {
            variables.put(ThemePropertyNames.SURFACE, colorValue);
            return this;
        }

        /**
         * Sets the surface variant color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder surfaceVariant(String colorValue) {
            variables.put(ThemePropertyNames.SURFACE_VARIANT, colorValue);
            return this;
        }

        /**
         * Sets the surface dim color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder surfaceDim(String colorValue) {
            variables.put(ThemePropertyNames.SURFACE_DIM, colorValue);
            return this;
        }

        /**
         * Sets the text color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder text(String colorValue) {
            variables.put(ThemePropertyNames.TEXT, colorValue);
            return this;
        }

        /**
         * Sets the muted text color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder textMuted(String colorValue) {
            variables.put(ThemePropertyNames.TEXT_MUTED, colorValue);
            return this;
        }

        /**
         * Sets the disabled text color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder textDisabled(String colorValue) {
            variables.put(ThemePropertyNames.TEXT_DISABLED, colorValue);
            return this;
        }

        /**
         * Sets the text-on-primary color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder textOnPrimary(String colorValue) {
            variables.put(ThemePropertyNames.TEXT_ON_PRIMARY, colorValue);
            return this;
        }

        /**
         * Sets the border color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder border(String colorValue) {
            variables.put(ThemePropertyNames.BORDER, colorValue);
            return this;
        }

        /**
         * Sets the border focus color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder borderFocus(String colorValue) {
            variables.put(ThemePropertyNames.BORDER_FOCUS, colorValue);
            return this;
        }

        /**
         * Sets the border muted color.
         *
         * @param colorValue color value
         * @return this builder
         */
        public ThemeBuilder borderMuted(String colorValue) {
            variables.put(ThemePropertyNames.BORDER_MUTED, colorValue);
            return this;
        }

        // ===== Generic Variable Support =====

        /**
         * Sets a custom variable.
         *
         * @param name variable name (without $)
         * @param value variable value
         * @return this builder
         */
        public ThemeBuilder variable(String name, String value) {
            if (value != null) {
                variables.put(name, value);
            }
            return this;
        }

        /**
         * Adds multiple variables.
         *
         * @param variables map of variable name → value
         * @return this builder
         */
        public ThemeBuilder variables(Map<String, String> variables) {
            this.variables.putAll(variables);
            return this;
        }

        /**
         * Builds the immutable Theme.
         *
         * @return new Theme instance
         */
        public Theme build() {
            return new Theme(themeName, dark, luminositySpread, variables);
        }
    }
}
