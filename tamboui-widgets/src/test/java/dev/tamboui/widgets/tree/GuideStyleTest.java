/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tree;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GuideStyle}.
 */
class GuideStyleTest {

    @Test
    @DisplayName("UNICODE guide style has correct characters")
    void unicodeStyle() {
        GuideStyle style = GuideStyle.UNICODE;

        assertThat(style.branch()).isEqualTo("\u251c\u2500\u2500 ");      // ├──
        assertThat(style.vertical()).isEqualTo("\u2502   ");              // │
        assertThat(style.lastBranch()).isEqualTo("\u2514\u2500\u2500 "); // └──
        assertThat(style.space()).isEqualTo("    ");
    }

    @Test
    @DisplayName("ASCII guide style has correct characters")
    void asciiStyle() {
        GuideStyle style = GuideStyle.ASCII;

        assertThat(style.branch()).isEqualTo("+-- ");
        assertThat(style.vertical()).isEqualTo("|   ");
        assertThat(style.lastBranch()).isEqualTo("+-- ");
        assertThat(style.space()).isEqualTo("    ");
    }

    @Test
    @DisplayName("NONE guide style has empty strings")
    void noneStyle() {
        GuideStyle style = GuideStyle.NONE;

        assertThat(style.branch()).isEmpty();
        assertThat(style.vertical()).isEmpty();
        assertThat(style.lastBranch()).isEmpty();
        assertThat(style.space()).isEmpty();
    }

    @Test
    @DisplayName("All guide styles are defined")
    void allStylesDefined() {
        assertThat(GuideStyle.values()).containsExactly(
                GuideStyle.UNICODE,
                GuideStyle.ASCII,
                GuideStyle.NONE
        );
    }
}
