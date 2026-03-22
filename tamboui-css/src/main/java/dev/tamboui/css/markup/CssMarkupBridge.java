package dev.tamboui.css.markup;

import java.util.Optional;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.MarkupParser;

/**
 * Bridges CSS variables to MarkupParser StyleResolver.
 * Enables using CSS-defined design tokens in markup text.
 *
 * <p>This allows semantic color names defined in theme .tcss files to be used
 * in markup strings. For example, if a theme defines {@code $primary: #cba6f7;},
 * you can use {@code [primary]text[/]} in markup.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * StyleEngine engine = new StyleEngine();
 * engine.loadStylesheet("catppuccin-mocha", "/themes/catppuccin-mocha.tcss");
 * engine.setActiveStylesheet("catppuccin-mocha");
 *
 * CssMarkupBridge bridge = new CssMarkupBridge(engine);
 * Text text = MarkupParser.parse("[primary]Title[/]", bridge.createResolver());
 * }</pre>
 *
 * <p>Background colors use MarkupParser's existing "on" syntax:</p>
 * <pre>{@code
 * [text on primary]Highlighted text[/]
 * [white on error]Alert![/]
 * }</pre>
 */
public class CssMarkupBridge {

    private final StyleEngine styleEngine;

    /**
     * Creates a new CSS-to-Markup bridge.
     *
     * @param styleEngine the style engine containing loaded themes
     */
    public CssMarkupBridge(StyleEngine styleEngine) {
        this.styleEngine = styleEngine;
    }

    /**
     * Creates a StyleResolver that resolves color names from CSS variables.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>Check if tag name is a CSS variable (e.g., "primary")</li>
     *   <li>Return null to fall back to built-in MarkupParser styles</li>
     * </ol>
     *
     * <p>Note: Background colors use MarkupParser's "on" syntax.
     * The parser handles the "on" keyword automatically, calling this resolver
     * for both foreground and background colors separately.</p>
     *
     * @return a StyleResolver that resolves CSS variable names to colors
     */
    public MarkupParser.StyleResolver createResolver() {
        return tagName -> {
            // Try to parse as a CSS variable reference
            // The StyleEngine will resolve $tagName to its value and parse it as a color
            String variableRef = "$" + tagName;
            Optional<Color> color = styleEngine.parseColor(variableRef);

            if (!color.isPresent()) {
                return null;  // Not a CSS variable, use built-in styles
            }

            // Return Style with foreground color
            // MarkupParser handles "on" keyword for background
            return Style.EMPTY.fg(color.get());
        };
    }
}
