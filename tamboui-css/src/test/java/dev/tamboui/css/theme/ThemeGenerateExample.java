/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.theme;

import java.util.Map;

import dev.tamboui.style.Color;

/**
 * Example demonstrating Theme.generate() and fluent Color API.
 *
 * <p>This is not a test - it's a runnable example showing the new APIs.</p>
 */
public class ThemeGenerateExample {

    public static void main(String[] args) {
        // Example 1: Minimal theme - just primary color
        System.out.println("=== Example 1: Minimal Theme ===");
        Theme minimal = Theme.builder()
            .name("ocean")
            .dark(true)
            .primary("#3498DB")  // Ocean blue
            .luminositySpread(0.15f)
            .build();

        Map<String, String> palette = minimal.generate();
        System.out.println("Generated " + palette.size() + " colors from just primary!");
        System.out.println("Primary shades:");
        palette.entrySet().stream()
            .filter(e -> e.getKey().startsWith("primary"))
            .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue()));

        System.out.println("\nDerived colors:");
        System.out.println("  secondary: " + palette.get("secondary") + " (same as primary)");
        System.out.println("  error: " + palette.get("error") + " (complementary)");
        System.out.println("  warning: " + palette.get("warning") + " (analogous)");
        System.out.println("  success: " + palette.get("success") + " (analogous)");

        // Example 2: Explicit colors (no derivation)
        System.out.println("\n=== Example 2: Explicit Colors ===");
        Theme explicit = Theme.builder()
            .name("gruvbox")
            .dark(true)
            .primary("#D3869B")
            .secondary("#83A598")
            .accent("#FE8019")
            .error("#FB4934")
            .warning("#FABD2F")
            .success("#B8BB26")
            .build();

        Map<String, String> explicitPalette = explicit.generate();
        System.out.println("Uses explicit values when provided:");
        System.out.println("  primary: " + explicitPalette.get("primary"));
        System.out.println("  secondary: " + explicitPalette.get("secondary") + " (explicit)");
        System.out.println("  error: " + explicitPalette.get("error") + " (explicit)");

        // Example 3: Different luminosity spreads
        System.out.println("\n=== Example 3: Luminosity Spread Control ===");
        Theme subtle = Theme.builder()
            .primary("#808080")
            .luminositySpread(0.1f)  // Subtle shades
            .build();

        Theme dramatic = Theme.builder()
            .primary("#808080")
            .luminositySpread(0.3f)  // Dramatic shades
            .build();

        Map<String, String> subtlePalette = subtle.generate();
        Map<String, String> dramaticPalette = dramatic.generate();

        System.out.println("Subtle (0.1 spread):");
        System.out.println("  primary-lighten-3: " + subtlePalette.get("primary-lighten-3"));
        System.out.println("  primary-darken-3: " + subtlePalette.get("primary-darken-3"));

        System.out.println("Dramatic (0.3 spread):");
        System.out.println("  primary-lighten-3: " + dramaticPalette.get("primary-lighten-3"));
        System.out.println("  primary-darken-3: " + dramaticPalette.get("primary-darken-3"));

        // Example 4: Fluent Color API
        System.out.println("\n=== Example 4: Fluent Color API ===");
        Color baseColor = Color.hex("#3498DB");

        System.out.println("Base color: " + baseColor.toHex());
        System.out.println("Lightened: " + baseColor.lighten(0.2f).toHex());
        System.out.println("Darkened: " + baseColor.darken(0.2f).toHex());
        System.out.println("Complementary: " + baseColor.rotateHue(180).toHex());
        System.out.println("Desaturated: " + baseColor.adjustSaturation(0.5f).toHex());

        // Chain operations
        Color customShade = baseColor
            .lighten(0.1f)
            .rotateHue(15)
            .mix(Color.rgb(255, 255, 255), 0.2f);
        System.out.println("\nChained (lighten→rotateHue→mix): " + customShade.toHex());

        // Convert to HSL
        float[] hsl = baseColor.toHsl();
        System.out.println(String.format("\nHSL: [%.1f°, %.1f%%, %.1f%%]", hsl[0], hsl[1], hsl[2]));
    }
}
