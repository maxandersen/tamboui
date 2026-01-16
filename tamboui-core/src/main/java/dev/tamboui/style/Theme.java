/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A theme that maps semantic style names to {@link Style} objects for use in markup.
 *
 * <p>Allows using semantic names like {@code [error]}, {@code [warning]}, {@code [success]}
 * instead of raw color names like {@code [red]}, {@code [yellow]}, {@code [green]}.
 *
 * <p>Example usage:
 * <pre>{@code
 * Theme theme = Theme.builder()
 *     .style("error", Style.EMPTY.fg(Color.RED).bold())
 *     .style("warning", Style.EMPTY.fg(Color.YELLOW))
 *     .style("success", Style.EMPTY.fg(Color.GREEN))
 *     .build();
 *
 * Text text = Markup.render("[error]Failed![/error]", Style.EMPTY, theme);
 * }</pre>
 *
 * <p>Themes are case-insensitive and can be composed. The default theme includes common
 * semantic names like {@code error}, {@code warning}, {@code success}, {@code info}, etc.
 */
public final class Theme {

    private final Map<String, Style> styles;

    private Theme(Map<String, Style> styles) {
        this.styles = Collections.unmodifiableMap(new HashMap<>(styles));
    }

    /**
     * Returns a new theme builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the default theme with common semantic style names.
     *
     * <p>Includes: {@code error}, {@code warning}, {@code success}, {@code info},
     * {@code debug}, {@code trace}, {@code highlight}, {@code accent}, {@code muted}.
     *
     * @return the default theme
     */
    public static Theme defaultTheme() {
        return DEFAULT_THEME;
    }

    /**
     * Returns an empty theme (no semantic names).
     *
     * @return an empty theme
     */
    public static Theme empty() {
        return EMPTY_THEME;
    }

    /**
     * Returns the style for the given semantic name, if present.
     *
     * @param name the semantic name (case-insensitive)
     * @return the style, or empty if not found
     */
    public Optional<Style> get(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return Optional.ofNullable(styles.get(normalized));
    }

    /**
     * Returns a new theme that combines this theme with another.
     * Styles from {@code other} override styles from this theme.
     *
     * @param other the theme to merge
     * @return a new combined theme
     */
    public Theme merge(Theme other) {
        Map<String, Style> merged = new HashMap<>(this.styles);
        merged.putAll(other.styles);
        return new Theme(merged);
    }

    /**
     * Builder for creating custom themes.
     */
    public static final class Builder {
        private final Map<String, Style> styles = new HashMap<>();

        private Builder() {
        }

        /**
         * Adds a semantic style name mapping.
         *
         * @param name the semantic name (case-insensitive)
         * @param style the style to map to
         * @return this builder for chaining
         */
        public Builder style(String name, Style style) {
            if (name != null && style != null) {
                String normalized = name.trim().toLowerCase(Locale.ROOT);
                styles.put(normalized, style);
            }
            return this;
        }

        /**
         * Builds the theme.
         *
         * @return a new theme
         */
        public Theme build() {
            return new Theme(styles);
        }
    }

    private static final Theme DEFAULT_THEME = builder()
        .style("error", Style.EMPTY.fg(Color.RED).bold())
        .style("warning", Style.EMPTY.fg(Color.YELLOW).bold())
        .style("success", Style.EMPTY.fg(Color.GREEN).bold())
        .style("info", Style.EMPTY.fg(Color.BLUE).bold())
        .style("debug", Style.EMPTY.fg(Color.CYAN))
        .style("trace", Style.EMPTY.fg(Color.GRAY))
        .style("highlight", Style.EMPTY.fg(Color.YELLOW).bold())
        .style("accent", Style.EMPTY.fg(Color.CYAN).bold())
        .style("muted", Style.EMPTY.fg(Color.GRAY))
        .style("dim", Style.EMPTY.dim())
        .style("bold", Style.EMPTY.bold())
        .style("italic", Style.EMPTY.italic())
        .style("underline", Style.EMPTY.underlined())
        .build();

    private static final Theme EMPTY_THEME = new Theme(Collections.emptyMap());
}

