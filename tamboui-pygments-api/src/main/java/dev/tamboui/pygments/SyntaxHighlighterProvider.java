/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments;

/**
 * Service provider interface for creating {@link SyntaxHighlighter} instances.
 * <p>
 * Implementations of this interface should be registered via the
 * Java {@link java.util.ServiceLoader} mechanism by creating a file
 * {@code META-INF/services/dev.tamboui.pygments.SyntaxHighlighterProvider} containing
 * the fully qualified class name of the implementation.
 * <p>
 * When multiple providers are available, the one with the highest priority is selected.
 * Providers can also be selected by name via the {@code tamboui.pygments.provider}
 * system property or {@code TAMBOUI_PYGMENTS_PROVIDER} environment variable.
 *
 * @see SyntaxHighlighter
 * @see SyntaxHighlighters
 */
public interface SyntaxHighlighterProvider {

    /**
     * Returns a simple identifier for this provider.
     * <p>
     * This name is used when selecting a provider via the {@code tamboui.pygments.provider}
     * system property or {@code TAMBOUI_PYGMENTS_PROVIDER} environment variable.
     * <p>
     * Examples: "process", "graalpy"
     *
     * @return a simple identifier for this provider
     */
    String name();

    /**
     * Returns the priority of this provider.
     * <p>
     * Higher priority values are preferred when multiple providers are available.
     * Suggested values:
     * <ul>
     *   <li>50 - external process implementation (requires external tools)</li>
     *   <li>100 - embedded implementation (self-contained, preferred)</li>
     * </ul>
     *
     * @return the priority (higher = more preferred)
     */
    int priority();

    /**
     * Returns whether this provider is available and can create a working highlighter.
     * <p>
     * This method should perform any necessary checks to determine if the provider
     * can function correctly (e.g., checking if external tools are installed).
     *
     * @return true if this provider can create a working syntax highlighter
     */
    boolean isAvailable();

    /**
     * Creates a new syntax highlighter instance.
     *
     * @return a new syntax highlighter instance
     */
    SyntaxHighlighter create();
}
