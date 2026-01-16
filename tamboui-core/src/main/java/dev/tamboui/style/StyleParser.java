/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Locale;
import java.util.Optional;

/**
 * Parses a Rich / BBCode-inspired style string (e.g. {@code "bold red on blue"})
 * into a {@link Style}.
 *
 * <p>This parser is intentionally small and dependency-free. It supports:
 * <ul>
 *   <li>Modifiers: {@code bold|b}, {@code italic|i}, {@code underline|u}, {@code dim},
 *       {@code reversed|reverse}, {@code hidden}, {@code crossed-out|strike|strikethrough},
 *       {@code blink|slow-blink|rapid-blink}</li>
 *   <li>Foreground colors: named colors or {@code #RGB}/{@code #RRGGBB}/{@code rgb(r,g,b)}/{@code indexed(n)}</li>
 *   <li>Background colors: {@code on <color>} or {@code on_<color>} / {@code on-<color>}</li>
 * </ul>
 *
 * <p>Unknown tokens are ignored so that higher-level layers may add extensions.
 */
public final class StyleParser {

    private StyleParser() {
    }

    /**
     * Parse a style specification.
     *
     * @param spec style specification, e.g. {@code "bold red on blue"}
     * @return parsed style, or {@link Style#EMPTY} if {@code spec} is null/blank
     */
    public static Style parse(String spec) {
        if (spec == null) {
            return Style.EMPTY;
        }
        String trimmed = spec.trim();
        if (trimmed.isEmpty()) {
            return Style.EMPTY;
        }

        Style style = Style.EMPTY;
        String[] tokens = tokenize(trimmed);

        boolean expectBgColor = false;
        for (int i = 0; i < tokens.length; i++) {
            String token = normalizeToken(tokens[i]);
            if (token.isEmpty()) {
                continue;
            }

            if (expectBgColor) {
                style = applyBackgroundColor(style, token);
                expectBgColor = false;
                continue;
            }

            if ("on".equals(token)) {
                expectBgColor = true;
                continue;
            }

            if (token.startsWith("on_") || token.startsWith("on-")) {
                style = applyBackgroundColor(style, token.substring(3));
                continue;
            }

            // Modifiers
            Style maybeModified = applyModifier(style, token);
            if (maybeModified != style) {
                style = maybeModified;
                continue;
            }

            // Foreground color
            Optional<Color> fg = ColorConverter.INSTANCE.convert(token);
            if (fg.isPresent()) {
                style = style.fg(fg.get());
            }
        }

        return style;
    }

    private static String[] tokenize(String spec) {
        // Split on whitespace, but keep rgb(...) and similar function tokens intact even if they contain spaces.
        // This is sufficient for Rich/Textual-style specs like "bold rgb(10, 20, 30)".
        StringBuilder current = new StringBuilder();
        java.util.List<String> tokens = new java.util.ArrayList<>();

        int parenDepth = 0;
        for (int i = 0; i < spec.length(); i++) {
            char ch = spec.charAt(i);
            if (ch == '(') {
                parenDepth++;
                current.append(ch);
                continue;
            }
            if (ch == ')') {
                if (parenDepth > 0) {
                    parenDepth--;
                }
                current.append(ch);
                continue;
            }

            if (Character.isWhitespace(ch) && parenDepth == 0) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(ch);
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens.toArray(new String[0]);
    }

    private static Style applyModifier(Style style, String token) {
        if ("bold".equals(token) || "b".equals(token)) {
            return style.bold();
        }
        if ("italic".equals(token) || "i".equals(token)) {
            return style.italic();
        }
        if ("underline".equals(token) || "underlined".equals(token) || "u".equals(token)) {
            return style.underlined();
        }
        if ("dim".equals(token)) {
            return style.dim();
        }
        if ("reverse".equals(token) || "reversed".equals(token)) {
            return style.reversed();
        }
        if ("hidden".equals(token)) {
            return style.hidden();
        }
        if ("crossed-out".equals(token) || "crossed_out".equals(token)
            || "strike".equals(token) || "strikethrough".equals(token)) {
            return style.crossedOut();
        }
        if ("blink".equals(token) || "slow-blink".equals(token) || "slow_blink".equals(token)) {
            return style.slowBlink();
        }
        if ("rapid-blink".equals(token) || "rapid_blink".equals(token)) {
            return style.rapidBlink();
        }
        return style;
    }

    private static Style applyBackgroundColor(Style style, String token) {
        Optional<Color> bg = ColorConverter.INSTANCE.convert(token);
        if (bg.isPresent()) {
            return style.bg(bg.get());
        }
        return style;
    }

    private static String normalizeToken(String token) {
        if (token == null) {
            return "";
        }
        // Keep characters for ColorConverter formats, but normalize a few separators.
        return token.trim().toLowerCase(Locale.ROOT);
    }
}


