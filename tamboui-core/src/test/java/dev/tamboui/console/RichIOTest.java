/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.console;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import dev.tamboui.text.Text;

class RichIOTest {

    @Test
    @DisplayName("print with markup enabled")
    void printWithMarkupEnabled() {
        StringWriter sw = new StringWriter();
        RichIO console = new RichIO(new PrintWriter(sw, true));
        console.print("[bold red]Hello[/] World");
        String output = sw.toString();
        assertThat(output).contains("Hello");
        assertThat(output).contains("\u001b"); // ANSI escape codes
    }

    @Test
    @DisplayName("print with markup disabled")
    void printWithMarkupDisabled() {
        StringWriter sw = new StringWriter();
        RichIO console = new RichIO(new PrintWriter(sw, true), false);
        console.print("[bold red]Hello[/] World");
        String output = sw.toString();
        assertThat(output).contains("[bold red]Hello[/] World");
        assertThat(output).doesNotContain("\u001b[0;31"); // No red ANSI code
    }

    @Test
    @DisplayName("pretty converts objects to Text")
    void prettyConvertsObjects() {
        RichIO console = new RichIO(new PrintWriter(new StringWriter(), true));
        
        assertThat(console.pretty(null).rawContent()).isEqualTo("null");
        assertThat(console.pretty("hello").rawContent()).contains("hello");
        assertThat(console.pretty(Text.from("test")).rawContent()).isEqualTo("test");
    }
}

