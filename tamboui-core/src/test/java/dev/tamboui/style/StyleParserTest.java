/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StyleParserTest {

    @Test
    void parsesModifiersAndColors() {
        Style style = StyleParser.parse("bold red on blue");
        assertThat(style.fg()).contains(Color.RED);
        assertThat(style.bg()).contains(Color.BLUE);
        assertThat(style.effectiveModifiers()).contains(Modifier.BOLD);
    }

    @Test
    void preservesRgbTokenWithSpaces() {
        Style style = StyleParser.parse("bold rgb(10, 20, 30)");
        assertThat(style.fg()).contains(Color.rgb(10, 20, 30));
        assertThat(style.effectiveModifiers()).contains(Modifier.BOLD);
    }
}


