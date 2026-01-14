/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Aggregated capability information from all discovered {@link CapabilityProvider}s.
 */
public final class CapabilityReport {
    private final List<CapabilitySection> sections;

    CapabilityReport(List<CapabilitySection> sections) {
        this.sections = Collections.unmodifiableList(sections);
    }

    public List<CapabilitySection> sections() {
        return sections;
    }

    public Optional<CapabilitySection> section(String title) {
        if (title == null) {
            return Optional.empty();
        }
        for (CapabilitySection section : sections) {
            if (title.equals(section.title())) {
                return Optional.of(section);
            }
        }
        return Optional.empty();
    }

    public Optional<String> value(String sectionTitle, String key) {
        return section(sectionTitle).flatMap(s -> s.value(key));
    }

    public Optional<Boolean> feature(String sectionTitle, String key) {
        return section(sectionTitle).flatMap(s -> s.feature(key));
    }

    public void print(PrintStream out) {
        out.println("TamboUI capability report");
        out.println();
        for (CapabilitySection section : sections()) {
            out.println("== " + section.title());
            if (!section.features().isEmpty()) {
                for (Map.Entry<String, Boolean> entry : section.features().entrySet()) {
                    out.println(entry.getKey() + ": " + entry.getValue());
                }
            }
            if (!section.values().isEmpty()) {
                for (Map.Entry<String, String> entry : section.values().entrySet()) {
                    out.println(entry.getKey() + ": " + entry.getValue());
                }
            }
            out.println();
        }
    }
}


