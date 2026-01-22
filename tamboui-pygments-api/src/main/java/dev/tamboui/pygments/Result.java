/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments;

import dev.tamboui.text.Text;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of a syntax highlighting operation, containing the highlighted text
 * and additional metadata.
 */
public final class Result {
    private final Text text;
    private final String lexer;
    private final boolean highlighted;
    private final String message;

    /**
     * Creates a new result.
     *
     * @param text        the highlighted (or raw) text
     * @param lexer       the lexer used for highlighting (may be null)
     * @param highlighted whether highlighting was actually performed
     * @param message     an informational or error message (may be null)
     */
    public Result(Text text, String lexer, boolean highlighted, String message) {
        this.text = Objects.requireNonNull(text, "text");
        this.lexer = lexer;
        this.highlighted = highlighted;
        this.message = message;
    }

    /**
     * Returns the highlighted text.
     * <p>
     * If highlighting failed, this returns the original source as raw text.
     *
     * @return the highlighted or raw text
     */
    public Text text() {
        return text;
    }

    /**
     * Returns the lexer that was used for highlighting.
     *
     * @return the lexer name, or empty if no lexer was determined
     */
    public Optional<String> lexer() {
        return Optional.ofNullable(lexer);
    }

    /**
     * Returns whether highlighting was successfully performed.
     *
     * @return true if the text was highlighted, false if it's raw text
     */
    public boolean highlighted() {
        return highlighted;
    }

    /**
     * Returns an informational or error message about the highlighting process.
     *
     * @return a message, or empty if there's no message
     */
    public Optional<String> message() {
        return Optional.ofNullable(message);
    }
}
