/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text.markup;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import dev.tamboui.style.Style;
import dev.tamboui.style.StyleParser;
import dev.tamboui.style.Theme;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;

/**
 * Rich / BBCode-inspired markup for producing styled {@link Text}.
 *
 * <p>Supported syntax mirrors Rich console markup:
 * <ul>
 *   <li>Opening tags: {@code [bold red]}, {@code [red]}, {@code [on blue]}</li>
 *   <li>Closing tags: {@code [/]} (implicit close), {@code [/red]} (explicit close)</li>
 *   <li>Escaping tags: prefix the {@code [} with a backslash, e.g. {@code \[red]}</li>
 *   <li>Hyperlinks: {@code [link=https://example.com]text[/link]}</li>
 *   <li>Emoji codes: {@code :smiley:}, {@code :warning:}, {@code :success:}, etc.</li>
 * </ul>
 *
 * <p>Markup is converted into {@link Line}s of {@link Span}s using the core style system.
 */
public final class Markup {

    private Markup() {
    }

    /**
     * Escape text so it won't be interpreted as markup.
     *
     * <p>This mirrors Rich's behavior: any markup-like {@code [tag]} is escaped as {@code \[tag]}.
     *
     * @param text raw text
     * @return escaped text safe for insertion into markup
     */
    public static String escape(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '[' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (isTagStartChar(next)) {
                    out.append('\\');
                }
            }
            out.append(ch);
        }
        // Preserve Rich's edge-case: if it ends with a single backslash, add one more.
        if (out.length() > 0 && out.charAt(out.length() - 1) == '\\') {
            if (out.length() == 1 || out.charAt(out.length() - 2) != '\\') {
                out.append('\\');
            }
        }
        return out.toString();
    }

    public static Text render(String markup) {
        return render(markup, Style.EMPTY);
    }

    /**
     * Render markup into {@link Text}.
     *
     * @param markup markup string
     * @param baseStyle base style applied to all produced spans (may be {@link Style#EMPTY})
     * @return rendered text
     * @throws MarkupParseException on malformed markup (e.g. mismatched closing tags)
     */
    public static Text render(String markup, Style baseStyle) {
        return render(markup, baseStyle, Theme.defaultTheme(), true);
    }

    /**
     * Render markup into {@link Text} with emoji replacement control.
     *
     * @param markup markup string
     * @param baseStyle base style applied to all produced spans (may be {@link Style#EMPTY})
     * @param emoji whether to replace emoji codes (e.g. {@code :smiley:}) with Unicode characters
     * @return rendered text
     * @throws MarkupParseException on malformed markup (e.g. mismatched closing tags)
     */
    public static Text render(String markup, Style baseStyle, boolean emoji) {
        return render(markup, baseStyle, Theme.defaultTheme(), emoji);
    }

    /**
     * Render markup into {@link Text} with a custom theme.
     *
     * @param markup markup string
     * @param baseStyle base style applied to all produced spans (may be {@link Style#EMPTY})
     * @param theme theme for resolving semantic style names (e.g. {@code [error]}, {@code [warning]})
     * @return rendered text
     * @throws MarkupParseException on malformed markup (e.g. mismatched closing tags)
     */
    public static Text render(String markup, Style baseStyle, Theme theme) {
        return render(markup, baseStyle, theme, true);
    }

    /**
     * Render markup into {@link Text} with a custom theme and emoji replacement control.
     *
     * @param markup markup string
     * @param baseStyle base style applied to all produced spans (may be {@link Style#EMPTY})
     * @param theme theme for resolving semantic style names (e.g. {@code [error]}, {@code [warning]})
     * @param emoji whether to replace emoji codes (e.g. {@code :smiley:}) with Unicode characters
     * @return rendered text
     * @throws MarkupParseException on malformed markup (e.g. mismatched closing tags)
     */
    public static Text render(String markup, Style baseStyle, Theme theme, boolean emoji) {
        if (markup == null) {
            return Text.empty();
        }
        if (baseStyle == null) {
            baseStyle = Style.EMPTY;
        }
        // Replace emoji codes if enabled
        String processedMarkup = emoji ? dev.tamboui.text.Emoji.replace(markup) : markup;

        if (processedMarkup.indexOf('[') < 0) {
            return Text.styled(processedMarkup, baseStyle);
        }

        List<Line> lines = new ArrayList<>();
        List<Span> currentLine = new ArrayList<>();

        Deque<TagFrame> stack = new ArrayDeque<>();

        StringBuilder plain = new StringBuilder();

        for (int i = 0; i < processedMarkup.length(); i++) {
            char ch = processedMarkup.charAt(i);
            if (ch != '[') {
                plain.append(ch);
                continue;
            }

            // Handle escapes like Rich: count trailing backslashes immediately preceding '['.
            int trailingBackslashes = countTrailingBackslashes(plain);
            if (trailingBackslashes > 0) {
                plain.setLength(plain.length() - trailingBackslashes);
                int literalBackslashes = trailingBackslashes / 2;
                for (int b = 0; b < literalBackslashes; b++) {
                    plain.append('\\');
                }
                if (trailingBackslashes % 2 == 1) {
                    // Escaped tag: treat '[' literally.
                    plain.append('[');
                    continue;
                }
            }

            int closeIdx = findTagClose(processedMarkup, i + 1);
            if (closeIdx < 0) {
                plain.append('[');
                continue;
            }

            String tagText = processedMarkup.substring(i + 1, closeIdx);
            if (tagText.isEmpty() || !isTagStartChar(tagText.charAt(0))) {
                plain.append('[');
                continue;
            }

            // Flush pending plain text before processing the tag.
            currentLine = flushPlainText(plain, currentLine, lines, computeCurrentStyle(baseStyle, stack));

            if (tagText.charAt(0) == '/') {
                String closeName = normalizeTagName(tagText.substring(1));
                if (closeName.isEmpty()) {
                    // implicit close
                    if (stack.isEmpty()) {
                        throw new MarkupParseException("closing tag '[/]' has nothing to close");
                    }
                    stack.pop();
                } else {
                    popExplicit(stack, closeName, tagText);
                }
            } else {
                Tag openTag = parseTag(tagText);
                String tagName = normalizeTagName(openTag.name);
                Style patch = styleFor(openTag, theme);
                stack.push(new TagFrame(tagName, patch));
            }

            i = closeIdx; // Jump past closing bracket.
        }

        currentLine = flushPlainText(plain, currentLine, lines, computeCurrentStyle(baseStyle, stack));

        // Match Text.raw / Text.styled line semantics:
        // - don't add an extra empty trailing line just because input ends with '\n'
        // - return Text.empty() when there is no content at all (e.g. "[red]")
        if (!currentLine.isEmpty()) {
            lines.add(Line.from(currentLine));
        }
        if (lines.isEmpty()) {
            return Text.empty();
        }
        return new Text(lines, null);
    }

    private static void popExplicit(Deque<TagFrame> stack, String closeName, String tagText) {
        if (stack.isEmpty()) {
            throw new MarkupParseException("closing tag '[/" + closeName + "]' doesn't match any open tag");
        }
        // Pop the first matching tag (Rich scans from the top).
        Deque<TagFrame> buffer = new ArrayDeque<>();
        boolean found = false;
        while (!stack.isEmpty()) {
            TagFrame frame = stack.pop();
            if (frame.name.equals(closeName)) {
                found = true;
                break;
            }
            buffer.push(frame);
        }
        while (!buffer.isEmpty()) {
            stack.push(buffer.pop());
        }
        if (!found) {
            throw new MarkupParseException("closing tag '[/" + closeName + "]' doesn't match any open tag");
        }
    }

    private static List<Span> flushPlainText(StringBuilder plain, List<Span> currentLine, List<Line> lines, Style style) {
        if (plain.length() == 0) {
            return currentLine;
        }
        // Unescape "\[" sequences in plain text (Rich behavior).
        String content = plain.toString().replace("\\[", "[");
        plain.setLength(0);

        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                if (i > start) {
                    appendSpan(currentLine, content.substring(start, i), style);
                }
                lines.add(Line.from(currentLine));
                currentLine = new ArrayList<>();
                start = i + 1;
            }
        }
        if (start < content.length()) {
            appendSpan(currentLine, content.substring(start), style);
        }
        return currentLine;
    }

    private static void appendSpan(List<Span> line, String content, Style style) {
        if (content.isEmpty()) {
            return;
        }
        int size = line.size();
        if (size > 0) {
            Span last = line.get(size - 1);
            if (last.style().equals(style)) {
                line.set(size - 1, Span.styled(last.content() + content, style));
                return;
            }
        }
        line.add(Span.styled(content, style));
    }

    private static Style computeCurrentStyle(Style base, Deque<TagFrame> stack) {
        Style style = base;
        if (stack.isEmpty()) {
            return style;
        }
        // Stack is LIFO; to apply in opening order, iterate from bottom.
        TagFrame[] frames = stack.toArray(new TagFrame[0]);
        for (int i = frames.length - 1; i >= 0; i--) {
            style = style.patch(frames[i].patch);
        }
        return style;
    }

    private static Style styleFor(Tag tag, Theme theme) {
        String name = normalizeTagName(tag.name);
        if ("link".equals(name) || "hyperlink".equals(name)) {
            if (tag.parameters == null || tag.parameters.trim().isEmpty()) {
                return Style.EMPTY;
            }
            String params = tag.parameters.trim();
            // Allow "url id" as a convenience.
            String[] parts = params.split("\\s+", 2);
            if (parts.length == 2) {
                return Style.EMPTY.hyperlink(parts[0], parts[1]);
            }
            return Style.EMPTY.hyperlink(parts[0]);
        }

        // Check theme first for semantic names (e.g. [error], [warning])
        // Only check if there are no parameters (semantic names don't take parameters)
        if (tag.parameters == null || tag.parameters.trim().isEmpty()) {
            Optional<Style> themeStyle = theme.get(name);
            if (themeStyle.isPresent()) {
                return themeStyle.get();
            }
        }

        // Fall back to StyleParser for raw style specs (e.g. "bold red", "rgb(10,20,30)")
        // Rich allows multi-token style names in tag.name; if there are parameters, Rich's Tag.__str__
        // becomes "name params", so we mirror that.
        String styleSpec = (tag.parameters == null) ? tag.name : (tag.name + " " + tag.parameters);
        return StyleParser.parse(styleSpec);
    }

    private static Tag parseTag(String tagText) {
        int eq = tagText.indexOf('=');
        if (eq >= 0) {
            String name = tagText.substring(0, eq).trim();
            String params = tagText.substring(eq + 1);
            return new Tag(name, params);
        }
        return new Tag(tagText.trim(), null);
    }

    private static int findTagClose(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '[') {
                return -1;
            }
            if (ch == ']') {
                return i;
            }
        }
        return -1;
    }

    private static int countTrailingBackslashes(StringBuilder sb) {
        int count = 0;
        for (int i = sb.length() - 1; i >= 0; i--) {
            if (sb.charAt(i) == '\\') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private static boolean isTagStartChar(char ch) {
        return (ch >= 'a' && ch <= 'z')
            || (ch >= 'A' && ch <= 'Z')
            || ch == '#'
            || ch == '/'
            || ch == '@';
    }

    private static String normalizeTagName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        // Collapse whitespace to match Rich's normalize behavior sufficiently for tag matching.
        return trimmed.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static final class Tag {
        private final String name;
        private final String parameters;

        private Tag(String name, String parameters) {
            this.name = name;
            this.parameters = parameters;
        }
    }

    private static final class TagFrame {
        private final String name;
        private final Style patch;

        private TagFrame(String name, Style patch) {
            this.name = name;
            this.patch = patch;
        }
    }
}


