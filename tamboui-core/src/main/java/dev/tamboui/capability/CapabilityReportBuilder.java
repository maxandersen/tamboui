/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builder for assembling a {@link CapabilityReport}.
 */
public final class CapabilityReportBuilder {

    private final List<CapabilitySection> sections = new ArrayList<>();

    public Section section(String source, String name) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(name, "name");
        return new Section(source, name);
    }

    public CapabilityReport build() {
        return new CapabilityReport(new ArrayList<>(sections));
    }

    public final class Section {
        private final String source;
        private final String name;
        private final Map<String, String> values = new LinkedHashMap<>();
        private final Map<String, Boolean> features = new LinkedHashMap<>();

        private Section(String source, String name) {
            this.source = source;
            this.name = name;
        }

        public Section kv(String key, Object value) {
            if (key == null || key.isEmpty()) {
                return this;
            }
            String stringValue = String.valueOf(value);
            values.put(key, stringValue);
            return this;
        }

        public Section feature(String feature, boolean supported) {
            if (feature == null || feature.isEmpty()) {
                return this;
            }
            features.put(feature, supported);
            return this;
        }

        public CapabilityReportBuilder end() {
            sections.add(new CapabilitySection(source, name, new LinkedHashMap<>(features), new LinkedHashMap<>(values)));
            return CapabilityReportBuilder.this;
        }
    }
}


