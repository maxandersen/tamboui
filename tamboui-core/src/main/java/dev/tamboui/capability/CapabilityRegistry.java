/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Loads {@link CapabilityProvider}s via {@link ServiceLoader} and builds an aggregated report.
 */
public final class CapabilityRegistry {

    private final List<CapabilityProvider> providers;

    private CapabilityRegistry(List<CapabilityProvider> providers) {
        this.providers = Collections.unmodifiableList(providers);
    }

    public static CapabilityRegistry load() {
        return load(Thread.currentThread().getContextClassLoader());
    }

    public static CapabilityRegistry load(ClassLoader classLoader) {
        ServiceLoader<CapabilityProvider> loader = ServiceLoader.load(CapabilityProvider.class, classLoader);
        List<CapabilityProvider> providers = new ArrayList<>();
        for (CapabilityProvider provider : loader) {
            providers.add(provider);
        }
        providers.sort(Comparator.comparing(CapabilityProvider::source));
        return new CapabilityRegistry(providers);
    }

    public List<CapabilityProvider> providers() {
        return providers;
    }

    public CapabilityReport buildReport() {
        CapabilityReportBuilder builder = new CapabilityReportBuilder();
        for (CapabilityProvider provider : providers) {
            try {
                provider.contribute(builder);
            } catch (Exception e) {
                builder.section(provider.source(), "CapabilityProviderError")
                        .kv("providerClass", provider.getClass().getName())
                        .kv("provider", provider.source())
                        .kv("error", e.getClass().getName() + ": " + e.getMessage())
                        .end();
            }
        }
        return builder.build();
    }
}


