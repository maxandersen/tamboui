/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.figlet;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FigletTest {

    @Test
    @DisplayName("Bundled fonts load from classpath (no external figlet binary)")
    void bundledFontsLoad() {
        assertThat(FigletFont.bundled(BundledFigletFont.MINI).height()).isGreaterThan(0);
        assertThat(FigletFont.bundled(BundledFigletFont.SMALL).height()).isGreaterThan(0);
        assertThat(FigletFont.bundled(BundledFigletFont.STANDARD).height()).isGreaterThan(0);
        assertThat(FigletFont.bundled(BundledFigletFont.SLANT).height()).isGreaterThan(0);
        assertThat(FigletFont.bundled(BundledFigletFont.BIG).height()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Figlet renders MINI font")
    void rendersMini() {
        Figlet widget = Figlet.builder()
            .text("Hi")
            .font(BundledFigletFont.MINI)
            .build();

        Rect area = new Rect(0, 0, 20, 6);
        Buffer buffer = Buffer.empty(area);
        widget.render(area, buffer);

        // Expected "Hi" in mini.flf (right-trimmed)
        assertThat(row(buffer, 0, 20)).isEqualTo("");
        assertThat(row(buffer, 1, 20)).isEqualTo("|_|o");
        assertThat(row(buffer, 2, 20)).isEqualTo("| ||");
        assertThat(row(buffer, 3, 20)).isEqualTo("");
    }

    @Test
    @DisplayName("Figlet renders SMALL font")
    void rendersSmall() {
        Figlet widget = Figlet.builder()
            .text("Hi")
            .font(BundledFigletFont.SMALL)
            .build();

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        widget.render(area, buffer);

        // Expected "Hi" in small.flf (right-trimmed)
        assertThat(row(buffer, 0, 30)).isEqualTo("_  _ _");
        assertThat(row(buffer, 1, 30)).isEqualTo("| || (_)");
        assertThat(row(buffer, 2, 30)).isEqualTo("| __ | |");
        assertThat(row(buffer, 3, 30)).isEqualTo("|_||_|_|");
        assertThat(row(buffer, 4, 30)).isEqualTo("");
    }

    private static String row(Buffer buffer, int y, int width) {
        StringBuilder sb = new StringBuilder(width);
        for (int x = 0; x < width; x++) {
            sb.append(buffer.get(x, y).symbol());
        }
        return rtrim(sb.toString());
    }

    private static String rtrim(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }
}

