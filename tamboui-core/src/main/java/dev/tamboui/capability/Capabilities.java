/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

import java.io.PrintStream;

/**
 * Convenience entrypoints for capability inspection.
 */
public final class Capabilities {

    private Capabilities() {
        // utility
    }

    public static CapabilityReport detect() {
        return CapabilityRegistry.load().buildReport();
    }

    public static void print(PrintStream out) {
        detect().print(out);
    }

    public static void main(String[] args) {
        print(System.out);
    }
}


