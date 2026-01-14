/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.capability;

import dev.tamboui.capability.CapabilityProvider;
import dev.tamboui.capability.CapabilityReportBuilder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Capability contributor for {@code tamboui-image}.
 */
public final class ImageCapabilityProvider implements CapabilityProvider {

    @Override
    public String source() {
        return "tamboui-image";
    }

    @Override
    public void contribute(CapabilityReportBuilder report) {
        TerminalImageCapabilities caps = TerminalImageCapabilities.detect();
        Set<TerminalImageProtocol> supported = caps.supportedProtocols();

        CapabilityReportBuilder.Section images = report.section(source(), "features")
                .kv("bestSupport", caps.bestSupport())
                .kv("supportedProtocols", supported.stream().map(Enum::name).collect(Collectors.joining(", ")));

        for (TerminalImageProtocol protocol : TerminalImageProtocol.values()) {
            images.feature("native." + protocol.name().toLowerCase(), caps.supports(protocol));
        }
        images.feature("native.any", caps.supportsNativeImages());
        images.end();
    }
}


