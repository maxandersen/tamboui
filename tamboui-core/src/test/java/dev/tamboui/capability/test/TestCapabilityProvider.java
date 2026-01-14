/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability.test;

import dev.tamboui.capability.CapabilityProvider;
import dev.tamboui.capability.CapabilityReportBuilder;

public final class TestCapabilityProvider implements CapabilityProvider {

    @Override
    public String source() {
        return "tamboui-test";
    }

    @Override
    public void contribute(CapabilityReportBuilder report) {
        report.section(source(), "Test")
            .kv("foo", "bar")
            .feature("feature.a", true)
            .feature("feature.b", false)
            .end();
    }
}


