/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments;

import dev.tamboui.text.Text;

import java.time.Duration;

/**
 * Interface for syntax highlighting source code.
 * <p>
 * Implementations may use different backends such as external processes (pygmentize CLI)
 * or embedded Python runtimes (GraalPy).
 *
 * @see SyntaxHighlighterProvider
 * @see SyntaxHighlighters
 */
public interface SyntaxHighlighter {

    /**
     * Default timeout for highlighting operations.
     */
    Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);

    /**
     * Highlights source code using the default timeout and style resolver.
     *
     * @param filename the filename used to infer the language/lexer
     * @param source   the source code to highlight
     * @return highlighted text
     */
    Text highlight(String filename, String source);

    /**
     * Highlights source code with a custom style resolver.
     *
     * @param filename the filename used to infer the language/lexer
     * @param source   the source code to highlight
     * @param resolver the style resolver for mapping token types to styles
     * @return highlighted text
     */
    Text highlight(String filename, String source, TokenStyleResolver resolver);

    /**
     * Highlights source code with a custom timeout.
     *
     * @param filename the filename used to infer the language/lexer
     * @param source   the source code to highlight
     * @param timeout  the maximum time to wait for highlighting
     * @return highlighted text
     */
    Text highlight(String filename, String source, Duration timeout);

    /**
     * Highlights source code with a custom timeout and style resolver.
     *
     * @param filename the filename used to infer the language/lexer
     * @param source   the source code to highlight
     * @param timeout  the maximum time to wait for highlighting
     * @param resolver the style resolver for mapping token types to styles
     * @return highlighted text
     */
    Text highlight(String filename, String source, Duration timeout, TokenStyleResolver resolver);

    /**
     * Highlights source code and returns detailed result information.
     *
     * @param filename the filename used to infer the language/lexer
     * @param source   the source code to highlight
     * @param timeout  the maximum time to wait for highlighting
     * @return result containing highlighted text and metadata
     */
    Result highlightWithInfo(String filename, String source, Duration timeout);

    /**
     * Highlights source code and returns detailed result information.
     *
     * @param filename the filename used to infer the language/lexer
     * @param source   the source code to highlight
     * @param timeout  the maximum time to wait for highlighting
     * @param resolver the style resolver for mapping token types to styles
     * @return result containing highlighted text and metadata
     */
    Result highlightWithInfo(String filename, String source, Duration timeout, TokenStyleResolver resolver);

    /**
     * Returns whether this syntax highlighter is available and can perform highlighting.
     *
     * @return true if highlighting is available, false otherwise
     */
    boolean isAvailable();
}
