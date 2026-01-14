/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability.core;

import dev.tamboui.capability.CapabilityProvider;
import dev.tamboui.capability.CapabilityReportBuilder;
import dev.tamboui.terminal.BackendProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.StringJoiner;
import java.util.Locale;

/**
 * Core capability contributor: prints what core assumes/uses and what it can infer from the environment.
 */
public final class CoreCapabilityProvider implements CapabilityProvider {

    @Override
    public String source() {
        return "tamboui-core";
    }

    @Override
    public void contribute(CapabilityReportBuilder report) {
        report.section(source(), "properties")
                .kv("java.version", System.getProperty("java.version"))
                .kv("java.vendor", System.getProperty("java.vendor"))
                .kv("os.name", System.getProperty("os.name"))
                .kv("os.arch", System.getProperty("os.arch"))
                .kv("os.version", System.getProperty("os.version"))
                .kv("tamboui.backend", System.getProperty("tamboui.backend"))
                .end();

        report.section(source(), "environment")
                .kv("TERM", getenv("TERM"))
                .kv("COLORTERM", getenv("COLORTERM"))
                .kv("TERM_PROGRAM", getenv("TERM_PROGRAM"))
                .kv("TERM_PROGRAM_VERSION", getenv("TERM_PROGRAM_VERSION"))
                .kv("LC_ALL", getenv("LC_ALL"))
                .kv("LANG", getenv("LANG"))
                .kv("TAMBOUI_BACKEND", getenv("TAMBOUI_BACKEND"))
                .end();

        List<BackendProvider> providers = discoverBackends();
        CapabilityReportBuilder.Section backends = report.section(source(), "features")
                .kv("backend.count", providers.size());
        backends.feature("backend.present", !providers.isEmpty());
        backends.kv("backend.providers", joinProviderDescriptions(providers, backends));
        backends.end();

        // these are what LLM found as something tamboui assumes.
        // no need to print it until we actually query/adapt to it.
        /*report.section("Core terminal features used/assumed")
                .line("Screen management: alternate screen enter/leave")
                .line("Input: raw mode enable/disable, timed read/peek")
                .line("Rendering: cursor positioning + diff-based drawing")
                .line("Cursor: hide/show + set position")
                .line("Resize: onResize callback")
                .line("Optional: mouse capture, scroll up/down, raw byte output (images)")
                .end();
                */
    }

    private static List<BackendProvider> discoverBackends() {
        ServiceLoader<BackendProvider> loader = ServiceLoader.load(BackendProvider.class);
        List<BackendProvider> providers = new ArrayList<>();
        for (BackendProvider provider : loader) {
            providers.add(provider);
        }
        return providers;
    }

    private static String joinProviderDescriptions(List<BackendProvider> providers, CapabilityReportBuilder.Section out) {
        if (providers == null || providers.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (BackendProvider provider : providers) {
            if (provider == null) {
                continue;
            }
            String providerClass = provider.getClass().getName();
            String providerName = backendProviderName(provider);
            if (providerName == null || providerName.isEmpty()) {
                providerName = deriveBackendProviderName(providerClass);
            }
            String featureKey = "backend.provider." + sanitizeFeatureToken(providerName);
            out.feature(featureKey, true);
            joiner.add(providerName + " (" + providerClass + ")");
        }
        return joiner.toString();
    }

    /**
     * Best-effort: uses BackendProvider#name() if present (newer versions),
     * otherwise returns null.
     */
    private static String backendProviderName(BackendProvider provider) {
        try {
            java.lang.reflect.Method m = provider.getClass().getMethod("name");
            Object value = m.invoke(provider);
            return value != null ? String.valueOf(value) : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String deriveBackendProviderName(String providerClassName) {
        String simple = providerClassName;
        int lastDot = simple.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < simple.length()) {
            simple = simple.substring(lastDot + 1);
        }
        if (simple.endsWith("BackendProvider")) {
            simple = simple.substring(0, simple.length() - "BackendProvider".length());
        }
        return simple.toLowerCase(Locale.ROOT);
    }

    private static String sanitizeFeatureToken(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
            } else {
                out.append('-');
            }
        }
        // Collapse repeated dashes
        String collapsed = out.toString().replaceAll("-{2,}", "-");
        // Trim leading/trailing dash
        while (collapsed.startsWith("-")) collapsed = collapsed.substring(1);
        while (collapsed.endsWith("-")) collapsed = collapsed.substring(0, collapsed.length() - 1);
        return collapsed.isEmpty() ? "unknown" : collapsed;
    }

    private static String getenv(String name) {
        try {
            return System.getenv(name);
        } catch (SecurityException e) {
            return null;
        }
    }
}


