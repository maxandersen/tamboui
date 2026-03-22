/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ColorSpaceTest {

    @Test
    void rgbToHslRoundTrip() {
        Color red = Color.rgb(255, 0, 0);

        float[] hsl = ColorSpace.toHsl(red);
        Color converted = ColorSpace.fromHsl(hsl[0], hsl[1], hsl[2]);

        assertThat(converted.toRgb()).isEqualTo(red.toRgb());
    }

    @Test
    void rgbToHsvRoundTrip() {
        Color green = Color.rgb(0, 255, 0);

        float[] hsv = ColorSpace.toHsv(green);
        Color converted = ColorSpace.fromHsv(hsv[0], hsv[1], hsv[2]);

        assertThat(converted.toRgb()).isEqualTo(green.toRgb());
    }

    @Test
    void toHslConvertsRedCorrectly() {
        Color red = Color.rgb(255, 0, 0);

        float[] hsl = ColorSpace.toHsl(red);

        assertThat(hsl[0]).isCloseTo(0f, within(1f));      // Hue ~0°
        assertThat(hsl[1]).isCloseTo(100f, within(1f));    // Saturation 100%
        assertThat(hsl[2]).isCloseTo(50f, within(1f));     // Lightness 50%
    }

    @Test
    void toHslConvertsGrayCorrectly() {
        Color gray = Color.rgb(128, 128, 128);

        float[] hsl = ColorSpace.toHsl(gray);

        assertThat(hsl[0]).isCloseTo(0f, within(1f));      // Hue ~0° (undefined for gray)
        assertThat(hsl[1]).isCloseTo(0f, within(1f));      // Saturation 0% (no color)
        assertThat(hsl[2]).isCloseTo(50f, within(1f));     // Lightness 50%
    }

    @Test
    void lightenMakesColorBrighter() {
        Color dark = Color.rgb(50, 50, 50);

        Color lighter = ColorSpace.lighten(dark, 0.2f);

        float[] hslOriginal = ColorSpace.toHsl(dark);
        float[] hslLighter = ColorSpace.toHsl(lighter);

        assertThat(hslLighter[2]).isGreaterThan(hslOriginal[2]);  // Lightness increased
    }

    @Test
    void darkenMakesColorDarker() {
        Color light = Color.rgb(200, 200, 200);

        Color darker = ColorSpace.darken(light, 0.2f);

        float[] hslOriginal = ColorSpace.toHsl(light);
        float[] hslDarker = ColorSpace.toHsl(darker);

        assertThat(hslDarker[2]).isLessThan(hslOriginal[2]);  // Lightness decreased
    }

    @Test
    void adjustSaturationDesaturates() {
        Color vibrant = Color.rgb(255, 0, 0);  // Pure red

        Color muted = ColorSpace.adjustSaturation(vibrant, 0.5f);

        float[] hslOriginal = ColorSpace.toHsl(vibrant);
        float[] hslMuted = ColorSpace.toHsl(muted);

        assertThat(hslMuted[1]).isLessThan(hslOriginal[1]);  // Saturation decreased
    }

    @Test
    void rotateHueCreatesComplementaryColor() {
        Color blue = Color.rgb(0, 0, 255);

        Color complementary = ColorSpace.rotateHue(blue, 180);

        float[] hslBlue = ColorSpace.toHsl(blue);
        float[] hslComp = ColorSpace.toHsl(complementary);

        // Hue should be rotated by 180°
        float expectedHue = (hslBlue[0] + 180) % 360;
        assertThat(hslComp[0]).isCloseTo(expectedHue, within(1f));
    }

    @Test
    void rotateHueHandlesNegativeDegrees() {
        Color red = Color.rgb(255, 0, 0);

        Color rotated = ColorSpace.rotateHue(red, -30);

        float[] hslRotated = ColorSpace.toHsl(rotated);

        // Should wrap around (0 - 30 = 330)
        assertThat(hslRotated[0]).isCloseTo(330f, within(1f));
    }

    @Test
    void mixBlendsTwoColors() {
        Color red = Color.rgb(255, 0, 0);
        Color blue = Color.rgb(0, 0, 255);

        Color mixed = ColorSpace.mix(red, blue, 0.5f);

        Color.Rgb rgb = mixed.toRgb();

        // 50/50 mix should be purple-ish
        assertThat(rgb.r()).isCloseTo(127, within(1));
        assertThat(rgb.g()).isCloseTo(0, within(1));
        assertThat(rgb.b()).isCloseTo(127, within(1));
    }

    @Test
    void mixRatioZeroReturnsFirstColor() {
        Color red = Color.rgb(255, 0, 0);
        Color blue = Color.rgb(0, 0, 255);

        Color result = ColorSpace.mix(red, blue, 0.0f);

        assertThat(result.toRgb()).isEqualTo(red.toRgb());
    }

    @Test
    void mixRatioOneReturnsSecondColor() {
        Color red = Color.rgb(255, 0, 0);
        Color blue = Color.rgb(0, 0, 255);

        Color result = ColorSpace.mix(red, blue, 1.0f);

        assertThat(result.toRgb()).isEqualTo(blue.toRgb());
    }

    @Test
    void fromHslCreatesCorrectRgb() {
        // Create pure red from HSL (0°, 100%, 50%)
        Color red = ColorSpace.fromHsl(0, 100, 50);

        Color.Rgb rgb = red.toRgb();
        assertThat(rgb.r()).isCloseTo(255, within(1));
        assertThat(rgb.g()).isCloseTo(0, within(1));
        assertThat(rgb.b()).isCloseTo(0, within(1));
    }

    @Test
    void fromHsvCreatesCorrectRgb() {
        // Create pure green from HSV (120°, 100%, 100%)
        Color green = ColorSpace.fromHsv(120, 100, 100);

        Color.Rgb rgb = green.toRgb();
        assertThat(rgb.r()).isCloseTo(0, within(1));
        assertThat(rgb.g()).isCloseTo(255, within(1));
        assertThat(rgb.b()).isCloseTo(0, within(1));
    }

    @Test
    void blendDelegatesToMix() {
        Color red = Color.rgb(255, 0, 0);
        Color blue = Color.rgb(0, 0, 255);

        Color mixed = ColorSpace.mix(red, blue, 0.5f);
        Color blended = ColorSpace.blend(red, blue, 0.5f, 1.0f);

        // Should produce same result since alpha is ignored for now
        assertThat(blended.toRgb()).isEqualTo(mixed.toRgb());
    }

    // ===== Fluent API Tests =====

    @Test
    void fluentToHexConvertsColorCorrectly() {
        Color red = Color.rgb(255, 0, 0);
        assertThat(red.toHex()).isEqualTo("#ff0000");

        Color custom = Color.rgb(58, 150, 221);
        assertThat(custom.toHex()).isEqualTo("#3a96dd");
    }

    @Test
    void fluentLightenDelegatesToStaticMethod() {
        Color dark = Color.rgb(50, 50, 50);

        Color lighterFluent = dark.lighten(0.2f);
        Color lighterStatic = ColorSpace.lighten(dark, 0.2f);

        assertThat(lighterFluent.toRgb()).isEqualTo(lighterStatic.toRgb());
    }

    @Test
    void fluentDarkenDelegatesToStaticMethod() {
        Color light = Color.rgb(200, 200, 200);

        Color darkerFluent = light.darken(0.2f);
        Color darkerStatic = ColorSpace.darken(light, 0.2f);

        assertThat(darkerFluent.toRgb()).isEqualTo(darkerStatic.toRgb());
    }

    @Test
    void fluentRotateHueDelegatesToStaticMethod() {
        Color blue = Color.rgb(0, 0, 255);

        Color rotatedFluent = blue.rotateHue(180);
        Color rotatedStatic = ColorSpace.rotateHue(blue, 180);

        assertThat(rotatedFluent.toRgb()).isEqualTo(rotatedStatic.toRgb());
    }

    @Test
    void fluentMixDelegatesToStaticMethod() {
        Color red = Color.rgb(255, 0, 0);
        Color blue = Color.rgb(0, 0, 255);

        Color mixedFluent = red.mix(blue, 0.5f);
        Color mixedStatic = ColorSpace.mix(red, blue, 0.5f);

        assertThat(mixedFluent.toRgb()).isEqualTo(mixedStatic.toRgb());
    }

    @Test
    void fluentAdjustSaturationDelegatesToStaticMethod() {
        Color vibrant = Color.rgb(255, 0, 0);

        Color mutedFluent = vibrant.adjustSaturation(0.5f);
        Color mutedStatic = ColorSpace.adjustSaturation(vibrant, 0.5f);

        assertThat(mutedFluent.toRgb()).isEqualTo(mutedStatic.toRgb());
    }

    @Test
    void fluentToHslDelegatesToStaticMethod() {
        Color red = Color.rgb(255, 0, 0);

        float[] hslFluent = red.toHsl();
        float[] hslStatic = ColorSpace.toHsl(red);

        assertThat(hslFluent).isEqualTo(hslStatic);
    }

    @Test
    void fluentToHsvDelegatesToStaticMethod() {
        Color green = Color.rgb(0, 255, 0);

        float[] hsvFluent = green.toHsv();
        float[] hsvStatic = ColorSpace.toHsv(green);

        assertThat(hsvFluent).isEqualTo(hsvStatic);
    }

    @Test
    void fluentApiCanChainOperations() {
        Color base = Color.rgb(100, 150, 200);

        // Chain: lighten → rotate hue → mix with white
        Color result = base
            .lighten(0.1f)
            .rotateHue(30)
            .mix(Color.rgb(255, 255, 255), 0.3f);

        assertThat(result).isNotNull();
        assertThat(result.toRgb()).isNotEqualTo(base.toRgb());
    }
}
