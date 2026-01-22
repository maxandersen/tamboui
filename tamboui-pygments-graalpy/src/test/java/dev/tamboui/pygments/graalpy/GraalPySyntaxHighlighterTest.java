/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments.graalpy;

import dev.tamboui.pygments.Result;
import dev.tamboui.pygments.SyntaxHighlighterProvider;
import dev.tamboui.pygments.SyntaxHighlighters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GraalPySyntaxHighlighterTest {

    private GraalPySyntaxHighlighter highlighter;

    @AfterEach
    void tearDown() {
        if (highlighter != null) {
            highlighter.close();
        }
    }

    @Test
    @EnabledIf("isGraalPyAvailable")
    void testHighlightJava() {
        highlighter = new GraalPySyntaxHighlighter();
        Result result = highlighter.highlightWithInfo(
            "Test.java",
            "public class Test { }",
            Duration.ofSeconds(30) // Longer timeout for first init
        );
        assertThat(result).isNotNull();
        assertThat(result.lexer()).contains("java");
        // May or may not be highlighted depending on GraalPy availability
        if (result.highlighted()) {
            assertThat(result.text().rawContent()).contains("public");
            assertThat(result.text().rawContent()).contains("class");
            assertThat(result.text().rawContent()).contains("Test");
        }
    }

    @Test
    @EnabledIf("isGraalPyAvailable")
    void testHighlightPython() {
        highlighter = new GraalPySyntaxHighlighter();
        Result result = highlighter.highlightWithInfo(
            "test.py",
            "def hello():\n    print('Hello')",
            Duration.ofSeconds(30)
        );
        assertThat(result).isNotNull();
        assertThat(result.lexer()).contains("python");
    }

    @Test
    void testProviderIsRegistered() {
        assertThat(SyntaxHighlighters.providers())
            .extracting(SyntaxHighlighterProvider::name)
            .contains("graalpy");
    }

    @Test
    void testProviderHasHigherPriority() {
        var graalpy = SyntaxHighlighters.providers().stream()
            .filter(p -> "graalpy".equals(p.name()))
            .findFirst();
        assertThat(graalpy).isPresent();
        assertThat(graalpy.get().priority()).isEqualTo(100);
    }

    static boolean isGraalPyAvailable() {
        try {
            GraalPySyntaxHighlighter highlighter = new GraalPySyntaxHighlighter();
            boolean available = highlighter.isAvailable();
            highlighter.close();
            return available;
        } catch (Throwable e) {
            // May throw NoClassDefFoundError or other errors if GraalPy libs aren't properly loaded
            return false;
        }
    }
}
