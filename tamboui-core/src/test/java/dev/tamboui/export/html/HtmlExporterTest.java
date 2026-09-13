/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.html;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.export.Formats;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import static dev.tamboui.export.ExportRequest.export;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlExporterTest {

    @Test
    void exportsHtmlWithNonEmbeddedStyles() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 8, 2));
        buffer.setString(0, 0, "Hello", Style.EMPTY.fg(Color.CYAN));
        buffer.setString(0, 1, "Bold", Style.EMPTY.bold().fg(Color.RED));

        String html = export(buffer).as(Formats.HTML).toString();
        // Default: styles in stylesheet (non-embedded), spans use classes
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("<pre"));
        assertTrue(html.contains("<code"));
        assertTrue(html.contains("Hello"));
        assertTrue(html.contains("Bold"));
        assertTrue(html.contains(".r1 {") || html.contains(".r2 {"), "stylesheet with class rules");
        assertTrue(html.contains("class=\"r1 w5\"") || html.contains("class=\"r2 w5\""),
            "spans use style + width classes, not inline style");
        assertFalse(html.contains("<span style="), "non-embedded must not use inline styles");
        // Width enforcement: each run is pinned to its column count so wide/emoji glyphs
        // cannot drift borders out of alignment (see #415).
        assertTrue(html.contains(".w5 {"), "stylesheet declares width class for 5-column run");
        assertTrue(html.contains("width:5ch") && html.contains("display:inline-block"),
            "width class pins the run to its display columns");
    }

    @Test
    void exportsHtmlWithEmbeddedInlineStyles() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 4, 1));
        buffer.setString(0, 0, "Hi", Style.EMPTY.bold());

        String html = export(buffer).as(Formats.HTML).options(o -> o.inlineStyles(true)).toString();

        assertTrue(html.contains("<span style="), "embedded: styles inlined in spans");
        assertTrue(html.contains("font-weight: bold"));
        assertTrue(html.contains("Hi"));
        assertTrue(html.contains("width:2ch") && html.contains("display:inline-block"),
            "embedded runs also carry width enforcement");
        assertFalse(html.contains("class=\"r1\""), "embedded must not use stylesheet classes");
    }

    @Test
    void wideAndEmojiRunsArePinnedToDisplayColumns() {
        // Regression test for #415: a run containing wide (CJK) glyphs must be pinned to
        // its display-column count (2 columns per CJK char), not its UTF-16 length, so the
        // following border character stays aligned regardless of browser font fallback.
        Buffer buffer = Buffer.empty(new Rect(0, 0, 6, 1));
        buffer.setString(0, 0, "\u4e16\u754c|", Style.EMPTY.fg(Color.CYAN));

        String html = export(buffer).as(Formats.HTML).options(o -> o.inlineStyles(true)).toString();

        // 世界| is 2 + 2 + 1 = 5 display columns in a single-style run.
        assertTrue(html.contains("width:5ch"), "CJK run pinned to 5 columns, not 3 UTF-16 units");
    }
}
