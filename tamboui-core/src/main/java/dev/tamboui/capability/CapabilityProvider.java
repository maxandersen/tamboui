/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

/**
 * Service-provider interface (SPI) for contributing capability information.
 * <p>
 * Implementations should be registered via the Java {@link java.util.ServiceLoader}
 * mechanism by creating a file:
 * {@code META-INF/services/dev.tamboui.capability.CapabilityProvider}
 * containing the fully qualified class name of the implementation.
 */
public interface CapabilityProvider {

    /**
     * A stable, user-friendly source id for this provider.
     * <p>
     * The default implementation derives a name from the class name by removing
     * "CapabilityProvider" suffix and converting to lowercase.
     *
     * @return source id (e.g. "tamboui-core", "tamboui-image")
     */
    default String source() {
        String className = getClass().getSimpleName();
        if (className.endsWith("CapabilityProvider")) {
            return className.substring(0, className.length() - "CapabilityProvider".length()).toLowerCase();
        }
        return className.toLowerCase();
    }

    /**
     * Contribute one or more sections to the capability report.
     *
     * @param report report builder
     */
    void contribute(CapabilityReportBuilder report);
}

