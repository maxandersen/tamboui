/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.boxtext;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoxTextTest {

    @Test
    @DisplayName("Renders a single glyph")
    void rendersSingleGlyph() {
        Rect area = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(area);

        BoxText.from("A").render(area, buffer);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "╭─╮", Style.EMPTY);
        expected.setString(0, 1, "├─┤", Style.EMPTY);
        expected.setString(0, 2, "╵ ╵", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Lowercase is rendered as uppercase by default")
    void lowerCaseDefaultsToUpperCase() {
        Rect area = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(area);

        BoxText.from("a").render(area, buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("─");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("╮");
    }

    @Test
    @DisplayName("Lowercase renders as lowercase when uppercase(false)")
    void lowerCaseWhenUppercaseDisabled() {
        Rect area = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(area);

        BoxText.builder()
            .text("a")
            .uppercase(false)
            .build()
            .render(area, buffer);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "   ", Style.EMPTY);
        expected.setString(0, 1, "╭─╮", Style.EMPTY);
        expected.setString(0, 2, "╰─┤", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Right alignment clips from the start (keeps rightmost text)")
    void rightAlignmentClipsFromStart() {
        Rect area = new Rect(0, 0, 6, 3);
        Buffer buffer = Buffer.empty(area);

        // Each digit glyph is 3 cols wide; 3 digits would be 9 cols, but area is 6.
        // With RIGHT alignment we should see the last 2 digits ("23").
        BoxText.builder()
            .text("123")
            .alignment(Alignment.RIGHT)
            .build()
            .render(area, buffer);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "╶─╮╶─╮", Style.EMPTY);
        expected.setString(0, 1, "┌─┘╶─┤", Style.EMPTY);
        expected.setString(0, 2, "└─╴╶─╯", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Renders multiple glyphs and clips to area width")
    void clipsHorizontally() {
        Rect area = new Rect(0, 0, 4, 3);
        Buffer buffer = Buffer.empty(area);

        BoxText.from("AB").render(area, buffer);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "╭─╮┌", Style.EMPTY);
        expected.setString(0, 1, "├─┤├", Style.EMPTY);
        expected.setString(0, 2, "╵ ╵╰", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Renders multi-line text using 3 rows per input line")
    void multilineUsesThreeRowsPerLine() {
        Rect area = new Rect(0, 0, 3, 6);
        Buffer buffer = Buffer.empty(area);

        BoxText.from("A\nB").render(area, buffer);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "╭─╮", Style.EMPTY);
        expected.setString(0, 1, "├─┤", Style.EMPTY);
        expected.setString(0, 2, "╵ ╵", Style.EMPTY);

        expected.setString(0, 3, "┌╮ ", Style.EMPTY);
        expected.setString(0, 4, "├┴╮", Style.EMPTY);
        expected.setString(0, 5, "╰─╯", Style.EMPTY);

        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }
}

