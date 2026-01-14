/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * A titled section of a capability report.
 */
public final class CapabilitySection {
    private final String source;
    private final String name;
    private final Map<String, Boolean> features;
    private final Map<String, String> values;

    CapabilitySection(String source, String name, Map<String, Boolean> features, Map<String, String> values) {
        this.source = source;
        this.name = name;
        this.features = Collections.unmodifiableMap(features);
        this.values = Collections.unmodifiableMap(values);
    }

    public String source() {
        return source;
    }

    public String name() {
        return name;
    }

    /**
     * Module-qualified title, e.g. {@code tamboui-core:Environment}.
     */
    public String title() {
        return source + ":" + name;
    }

    /**
     * Structured values for programmatic querying.
     */
    public Map<String, String> values() {
        return values;
    }

    public Optional<String> value(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(key));
    }

    public Map<String, Boolean> features() {
        return features;
    }

    public Optional<Boolean> feature(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(features.get(key));
    }
}


