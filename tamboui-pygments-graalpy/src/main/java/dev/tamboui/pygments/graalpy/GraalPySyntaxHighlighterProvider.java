/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments.graalpy;

import dev.tamboui.pygments.SyntaxHighlighter;
import dev.tamboui.pygments.SyntaxHighlighterProvider;

/**
 * ServiceLoader provider for the GraalPy-based syntax highlighter.
 * <p>
 * This provider creates {@link GraalPySyntaxHighlighter} instances that use
 * embedded Python via GraalPy. It has higher priority than the process-based
 * provider because it's self-contained and doesn't require external tools.
 */
public final class GraalPySyntaxHighlighterProvider implements SyntaxHighlighterProvider {

    private static final int PRIORITY = 100;

    /**
     * Creates a new provider instance.
     * <p>
     * This constructor is called by ServiceLoader.
     */
    public GraalPySyntaxHighlighterProvider() {
    }

    @Override
    public String name() {
        return "graalpy";
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public boolean isAvailable() {
        return new GraalPySyntaxHighlighter().isAvailable();
    }

    @Override
    public SyntaxHighlighter create() {
        return new GraalPySyntaxHighlighter();
    }
}
