///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.markup.CssMarkupBridge;
import dev.tamboui.css.theme.ThemePropertyNames;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.MarkupParser;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.app.InlineApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.widgets.paragraph.Paragraph;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demonstrates TamboUI's semantic color system across multiple themes.
 *
 * <p>Shows how the same semantic color names (primary, error, etc.)
 * are resolved to different values depending on the active theme, enabling
 * theme-independent application development.</p>
 *
 * <p>Similar to Clique's ThemeShowcase but using TamboUI's CSS-based semantic tokens.</p>
 */
public class ThemesDemo extends InlineApp {

    /**
     * List of theme names to showcase.
     */
    private static final String[] THEME_NAMES = {
        "catppuccin-mocha",
        "dracula",
        "gruvbox-dark",
        "nord",
        "tokyo-night",
        "dark",
        "light"
    };

    private StyleEngine engine;
    private CssMarkupBridge bridge;
    private MarkupParser.StyleResolver resolver;

    /**
     * Creates a new themes demo application.
     */
    public ThemesDemo() {
        super();
    }

    /**
     * Main entry point for the themes demo.
     *
     * @param args command line arguments (unused)
     * @throws Exception if the application fails to start
     */
    public static void main(String[] args) throws Exception {
        new ThemesDemo().run();
    }

    @Override
    protected void onStart() {
        // Load all themes
        engine = StyleEngine.create();
        int loadedCount = 0;

        for (String themeName : THEME_NAMES) {
            String path = "/themes/" + themeName + ".tcss";
            try {
                engine.loadStylesheet(themeName, path);
                loadedCount++;
            } catch (Exception e) {
                println(text("Warning: Failed to load theme " + themeName + ": " + e.getMessage()));
            }
        }

        // Create markup bridge
        bridge = new CssMarkupBridge(engine);
        resolver = bridge.createResolver();

        // Print header
        println(text(""));
        println(text("=".repeat(70)));
        println(text("TamboUI Semantic Color System Showcase"));
        println(text("=".repeat(70)));
        println(text(""));
        println(text("Demonstrating theme independence using semantic color names."));
        println(text("The same markup code renders with different colors per theme."));
        println(text(""));
        println(text("Loaded " + loadedCount + " themes successfully."));
        println(text(""));

        // Showcase each theme
        for (String themeName : THEME_NAMES) {
            if (!engine.getStylesheetNames().contains(themeName)) {
                continue; // Skip themes that failed to load
            }
            showcaseTheme(themeName);
        }

        // Print footer
        println(text("=".repeat(70)));
        println(text("Key takeaway: Applications use semantic names ([primary], [error], etc.)"));
        println(text("and get appropriate colors for the active theme automatically."));
        println(text("=".repeat(70)));
        println(text(""));

        // Quit when done
        quit();
    }

    private void showcaseTheme(String themeName) {
        // Activate theme
        engine.setActiveStylesheet(themeName);

        // Display theme name
        String formattedName = formatThemeName(themeName);
        println(text("-".repeat(70)));
        println(text("Theme: " + formattedName));
        println(text("-".repeat(70)));

        // Show core brand colors WITH ACTUAL THEMED RENDERING
        println(text("  Core Brand Colors:"));
        printColorSample("    ", ThemePropertyNames.PRIMARY, "Primary");
        printColorSample("    ", ThemePropertyNames.SECONDARY, "Secondary");
        printColorSample("    ", ThemePropertyNames.ACCENT, "Accent");

        // Show feedback colors WITH ACTUAL THEMED RENDERING
        println(text("  Feedback Colors:"));
        printColorSample("    ", ThemePropertyNames.ERROR, "Error");
        printColorSample("    ", ThemePropertyNames.WARNING, "Warning");
        printColorSample("    ", ThemePropertyNames.SUCCESS, "Success");
        printColorSample("    ", ThemePropertyNames.INFO, "Info");

        // Show practical usage examples - SAME MARKUP, DIFFERENT COLORS!
        println(text("  Usage Examples:"));
        println(row(
            text("    "),
            markupText("[" + ThemePropertyNames.SUCCESS + "]✓ Success message[/]"),
            text("  "),
            markupText("[" + ThemePropertyNames.ERROR + "]✗ Error message[/]"),
            text("  "),
            markupText("[" + ThemePropertyNames.WARNING + "]⚠ Warning[/]")
        ));

        // Show text variants
        println(text("  Text Variants:"));
        println(row(
            text("    "),
            markupText("[" + ThemePropertyNames.TEXT + "]Normal text[/]"),
            text("  "),
            markupText("[" + ThemePropertyNames.TEXT_MUTED + "]Muted text[/]"),
            text("  "),
            markupText("[" + ThemePropertyNames.TEXT_DISABLED + "]Disabled text[/]")
        ));

        println(text(""));
    }

    private void printColorSample(String indent, String colorName, String label) {
        // Print colored bullet and label with hex value
        String markup = "[" + colorName + "]● " + label + "[/]";
        String hexValue = " (" + getColorValue(colorName) + ")";
        println(
            markupText(indent + markup + hexValue)
        );
    }

    private Element markupText(String markup) {
        Text parsedText = MarkupParser.parse(markup, resolver);
        return new MarkupElement(parsedText);
    }

    /**
     * Custom element that renders pre-styled Text from MarkupParser.
     */
    private static class MarkupElement implements Element {
        private final Text styledText;

        MarkupElement(Text styledText) {
            this.styledText = styledText;
        }

        @Override
        public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
            int width = styledText.lines().stream()
                    .mapToInt(Line::width)
                    .max()
                    .orElse(0);
            int height = styledText.lines().size();
            return Size.of(width, height);
        }

        @Override
        public void render(Frame frame, Rect area, RenderContext context) {
            Paragraph paragraph = Paragraph.builder()
                    .text(styledText)
                    .build();
            frame.renderWidget(paragraph, area);
        }
    }

    private String formatThemeName(String name) {
        // Convert "catppuccin-mocha" -> "Catppuccin Mocha"
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == '-') {
                result.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String getColorValue(String variableName) {
        // Parse the variable to get its color value
        String variableRef = "$" + variableName;
        return engine.parseColor(variableRef)
                .map(color -> {
                    var rgb = color.toRgb();
                    return String.format("#%02x%02x%02x", rgb.r(), rgb.g(), rgb.b());
                })
                .orElse("N/A");
    }

    @Override
    protected Element render() {
        // No UI to render - we just print output and quit
        return column();
    }

    @Override
    protected int height() {
        // Minimal height since we're just printing
        return 1;
    }
}
