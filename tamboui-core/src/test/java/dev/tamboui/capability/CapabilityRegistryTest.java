/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityRegistryTest {

    @Test
    void loadsProvidersViaServiceLoader_and_buildsReport() {
        CapabilityRegistry registry = CapabilityRegistry.load();

        assertThat(registry.providers())
                .extracting(p -> p.getClass().getName())
                .contains(
                        "dev.tamboui.capability.core.CoreCapabilityProvider",
                        "dev.tamboui.capability.test.TestCapabilityProvider"
                );

        CapabilityReport report = registry.buildReport();
        assertThat(report.section("tamboui-test:Test")).isPresent();
        assertThat(report.value("tamboui-test:Test", "foo")).contains("bar");
        assertThat(report.feature("tamboui-test:Test", "feature.a")).contains(true);
        assertThat(report.feature("tamboui-test:Test", "feature.b")).contains(false);
    }
}


