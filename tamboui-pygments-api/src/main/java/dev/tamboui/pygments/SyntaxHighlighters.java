/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments;

import dev.tamboui.util.SafeServiceLoader;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Factory for creating {@link SyntaxHighlighter} instances using the {@link java.util.ServiceLoader} mechanism.
 * <p>
 * This factory discovers {@link SyntaxHighlighterProvider} implementations on the classpath
 * and uses them to create syntax highlighter instances. When multiple providers are available,
 * the one with the highest priority (that is also available) is selected.
 * <p>
 * The provider can be explicitly selected via the {@code tamboui.pygments.provider}
 * system property or {@code TAMBOUI_PYGMENTS_PROVIDER} environment variable.
 *
 * @see SyntaxHighlighter
 * @see SyntaxHighlighterProvider
 */
public final class SyntaxHighlighters {

    private static volatile SyntaxHighlighter cached;

    private SyntaxHighlighters() {
        // Utility class
    }

    /**
     * Returns a cached syntax highlighter instance.
     * <p>
     * This method discovers providers via ServiceLoader and caches the selected highlighter.
     * Provider selection follows this priority:
     * <ol>
     *   <li>System property {@code tamboui.pygments.provider} (if set)</li>
     *   <li>Environment variable {@code TAMBOUI_PYGMENTS_PROVIDER} (if set)</li>
     *   <li>Highest priority available provider via ServiceLoader</li>
     * </ol>
     *
     * @return a syntax highlighter instance
     * @throws IllegalStateException if no provider is found or all providers are unavailable
     */
    public static SyntaxHighlighter get() {
        SyntaxHighlighter h = cached;
        if (h != null) {
            return h;
        }
        synchronized (SyntaxHighlighters.class) {
            if (cached == null) {
                cached = createHighlighter();
            }
            return cached;
        }
    }

    /**
     * Returns a syntax highlighter from a specific provider by name.
     *
     * @param name the provider name (e.g., "process", "graalpy")
     * @return the syntax highlighter from the named provider
     * @throws IllegalStateException if no provider with the given name is found
     */
    public static SyntaxHighlighter get(String name) {
        List<SyntaxHighlighterProvider> allProviders = providers();
        return allProviders.stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .map(SyntaxHighlighterProvider::create)
                .orElseThrow(() -> new IllegalStateException(
                        "No SyntaxHighlighterProvider found with name '" + name + "'.\n" +
                                "Available providers: " + formatProviderNames(allProviders)
                ));
    }

    /**
     * Returns all available syntax highlighter providers.
     *
     * @return list of all discovered providers, sorted by priority (highest first)
     */
    public static List<SyntaxHighlighterProvider> providers() {
        List<SyntaxHighlighterProvider> providers = SafeServiceLoader.load(SyntaxHighlighterProvider.class);
        providers.sort(Comparator.comparingInt(SyntaxHighlighterProvider::priority).reversed());
        return providers;
    }

    /**
     * Clears the cached highlighter instance, forcing re-discovery on next access.
     * <p>
     * This is primarily useful for testing scenarios.
     */
    public static void clearCache() {
        synchronized (SyntaxHighlighters.class) {
            cached = null;
        }
    }

    private static SyntaxHighlighter createHighlighter() {
        // Check system property first, then environment variable
        String userSelectedProvider = System.getProperty("tamboui.pygments.provider");
        if (userSelectedProvider == null || userSelectedProvider.isEmpty()) {
            userSelectedProvider = System.getenv("TAMBOUI_PYGMENTS_PROVIDER");
        }

        List<SyntaxHighlighterProvider> allProviders = providers();

        if (userSelectedProvider != null && !userSelectedProvider.isEmpty()) {
            String finalUserSelectedProvider = userSelectedProvider;
            return allProviders.stream()
                    .filter(p -> p.name().equals(finalUserSelectedProvider))
                    .findFirst()
                    .map(SyntaxHighlighterProvider::create)
                    .orElseThrow(() -> new IllegalStateException(
                            "No SyntaxHighlighterProvider found with name '" + finalUserSelectedProvider + "'.\n" +
                                    "Available providers: " + formatProviderNames(allProviders)
                    ));
        }

        // Find highest priority available provider
        Optional<SyntaxHighlighterProvider> provider = allProviders.stream()
                .filter(SyntaxHighlighterProvider::isAvailable)
                .findFirst();

        if (provider.isPresent()) {
            return provider.get().create();
        }

        // No available provider found
        if (allProviders.isEmpty()) {
            throw new IllegalStateException(
                    "No SyntaxHighlighterProvider found on classpath.\n" +
                            "Add a dependency such as tamboui-pygments or tamboui-pygments-graalpy."
            );
        }

        throw new IllegalStateException(
                "All syntax highlighter providers are unavailable.\n" +
                        "Providers found: " + formatProviderNames(allProviders) + "\n" +
                        "For process provider: ensure pygmentize, uvx, or pipx is installed.\n" +
                        "For graalpy provider: should work without external dependencies."
        );
    }

    private static String formatProviderNames(List<SyntaxHighlighterProvider> providers) {
        if (providers.isEmpty()) {
            return "(none)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < providers.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            SyntaxHighlighterProvider p = providers.get(i);
            sb.append(p.name())
                    .append(" (priority=").append(p.priority())
                    .append(", available=").append(p.isAvailable()).append(")");
        }
        return sb.toString();
    }
}
