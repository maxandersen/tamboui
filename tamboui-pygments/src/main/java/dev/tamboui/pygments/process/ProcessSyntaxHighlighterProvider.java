/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments.process;

import dev.tamboui.pygments.SyntaxHighlighter;
import dev.tamboui.pygments.SyntaxHighlighterProvider;

/**
 * ServiceLoader provider for the external process-based syntax highlighter.
 * <p>
 * This provider creates {@link ProcessSyntaxHighlighter} instances that use
 * the {@code pygmentize} CLI (or {@code uvx}/{@code pipx} fallbacks).
 */
public final class ProcessSyntaxHighlighterProvider implements SyntaxHighlighterProvider {

    private static final int PRIORITY = 50;

    /**
     * Creates a new provider instance.
     * <p>
     * This constructor is called by ServiceLoader.
     */
    public ProcessSyntaxHighlighterProvider() {
    }

    @Override
    public String name() {
        return "process";
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public boolean isAvailable() {
        // Create a temporary highlighter to check availability
        return new ProcessSyntaxHighlighter().isAvailable();
    }

    @Override
    public SyntaxHighlighter create() {
        return new ProcessSyntaxHighlighter();
    }
}
