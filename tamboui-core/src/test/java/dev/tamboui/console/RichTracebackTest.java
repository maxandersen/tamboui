/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.console;

import dev.tamboui.text.Text;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RichTracebackTest {

    @Test
    void rendersCauseAndSuppressed() {
        IllegalArgumentException cause = new IllegalArgumentException("bad input");
        RuntimeException top = new RuntimeException("top", cause);
        top.addSuppressed(new IllegalStateException("suppressed"));

        Text text = RichTraceback.render(top);
        String raw = text.rawContent();

        assertThat(raw).contains(RuntimeException.class.getName() + ": top");
        assertThat(raw).contains("Suppressed:");
        assertThat(raw).contains(IllegalStateException.class.getName() + ": suppressed");
        assertThat(raw).contains("Caused by:");
        assertThat(raw).contains(IllegalArgumentException.class.getName() + ": bad input");
        assertThat(text.lines().size()).isGreaterThan(1);
    }
}


