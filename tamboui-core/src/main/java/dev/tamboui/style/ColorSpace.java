/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

/**
 * Color space conversion and manipulation utilities.
 *
 * <p>Provides HSL/HSV color space conversions and color manipulation methods
 * like lighten, darken, rotate hue, and mix colors.</p>
 *
 * <p>Inspired by Textual's Color class and TFxColorSpace.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Convert to HSL
 * float[] hsl = ColorSpace.toHsl(Color.rgb(255, 0, 0));
 * // hsl = [0.0, 100.0, 50.0] - red in HSL
 *
 * // Create from HSL
 * Color color = ColorSpace.fromHsl(120, 100, 50);  // Green
 *
 * // Lighten a color
 * Color lighter = ColorSpace.lighten(Color.rgb(100, 100, 100), 0.2f);
 *
 * // Rotate hue for complementary color
 * Color complementary = ColorSpace.rotateHue(primaryColor, 180);
 * }</pre>
 */
public final class ColorSpace {

    private ColorSpace() {
        // Utility class
    }

    // ===== Color Space Conversion =====

    /**
     * Converts RGB color to HSL (Hue, Saturation, Lightness) components.
     *
     * @param color the color to convert
     * @return array [h, s, l] where h is 0-360 degrees, s and l are 0-100 percent
     */
    public static float[] toHsl(Color color) {
        int[] rgb = toRgbComponents(color);
        return rgbToHsl(color.toRgb().r(), color.toRgb().g(), color.toRgb().b());
    }

    /**
     * Creates a color from HSL components.
     *
     * @param h hue in degrees (0-360)
     * @param s saturation percent (0-100)
     * @param l lightness percent (0-100)
     * @return RGB color
     */
    public static Color fromHsl(float h, float s, float l) {
        int[] rgb = hslToRgb(h, s, l);
        return Color.rgb(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Converts RGB color to HSV (Hue, Saturation, Value) components.
     *
     * @param color the color to convert
     * @return array [h, s, v] where h is 0-360 degrees, s and v are 0-100 percent
     */
    public static float[] toHsv(Color color) {
        int[] rgb = toRgbComponents(color);
        return rgbToHsv(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Creates a color from HSV components.
     *
     * @param h hue in degrees (0-360)
     * @param s saturation percent (0-100)
     * @param v value/brightness percent (0-100)
     * @return RGB color
     */
    public static Color fromHsv(float h, float s, float v) {
        int[] rgb = hsvToRgb(h, s, v);
        return Color.rgb(rgb[0], rgb[1], rgb[2]);
    }

    // ===== Color Manipulation =====

    /**
     * Lightens a color by increasing lightness in HSL space.
     *
     * @param color the base color
     * @param amount the amount to lighten (0.0 = no change, 1.0 = fully light)
     * @return lightened color
     */
    public static Color lighten(Color color, float amount) {
        float[] hsl = toHsl(color);
        float newLightness = Math.min(100f, hsl[2] + amount * 100f);
        return fromHsl(hsl[0], hsl[1], newLightness);
    }

    /**
     * Darkens a color by decreasing lightness in HSL space.
     *
     * @param color the base color
     * @param amount the amount to darken (0.0 = no change, 1.0 = fully dark)
     * @return darkened color
     */
    public static Color darken(Color color, float amount) {
        float[] hsl = toHsl(color);
        float newLightness = Math.max(0f, hsl[2] - amount * 100f);
        return fromHsl(hsl[0], hsl[1], newLightness);
    }

    /**
     * Adjusts saturation (color vibrancy).
     *
     * @param color the base color
     * @param factor multiplication factor (&lt; 1.0 desaturates toward gray, &gt; 1.0 saturates)
     * @return color with adjusted saturation
     */
    public static Color adjustSaturation(Color color, float factor) {
        float[] hsl = toHsl(color);
        float newSaturation = Math.max(0f, Math.min(100f, hsl[1] * factor));
        return fromHsl(hsl[0], newSaturation, hsl[2]);
    }

    /**
     * Rotates hue on the color wheel.
     * Useful for generating complementary (180°) or analogous (±30°) colors.
     *
     * @param color the base color
     * @param degrees rotation in degrees (-360 to 360)
     * @return color with rotated hue
     */
    public static Color rotateHue(Color color, float degrees) {
        float[] hsl = toHsl(color);
        float newHue = (hsl[0] + degrees) % 360f;
        if (newHue < 0f) {
            newHue += 360f;
        }
        return fromHsl(newHue, hsl[1], hsl[2]);
    }

    /**
     * Mixes two colors.
     *
     * @param color1 first color
     * @param color2 second color
     * @param ratio mixing ratio (0.0 = all color1, 1.0 = all color2)
     * @return blended color
     */
    public static Color mix(Color color1, Color color2, float ratio) {
        int[] rgb1 = toRgbComponents(color1);
        int[] rgb2 = toRgbComponents(color2);

        int r = Math.round(rgb1[0] + (rgb2[0] - rgb1[0]) * ratio);
        int g = Math.round(rgb1[1] + (rgb2[1] - rgb1[1]) * ratio);
        int b = Math.round(rgb1[2] + (rgb2[2] - rgb1[2]) * ratio);

        return Color.rgb(r, g, b);
    }

    /**
     * Blends two colors with alpha control (like Textual's Color.blend).
     *
     * @param base the base color
     * @param overlay the overlay color
     * @param ratio blend ratio (0.0 to 1.0)
     * @param alpha final alpha value (0.0 to 1.0)
     * @return blended color
     */
    public static Color blend(Color base, Color overlay, float ratio, float alpha) {
        // For now, just delegate to mix since TamboUI Color doesn't have alpha channel
        // In the future, this could be extended to support alpha blending
        return mix(base, overlay, ratio);
    }

    // ===== Internal Conversion Helpers =====

    /**
     * Converts a color to RGB components [r, g, b].
     */
    private static int[] toRgbComponents(Color color) {
        if (color instanceof Color.Reset) {
            return new int[]{0, 0, 0};
        }
        Color.Rgb rgb = color.toRgb();
        return new int[]{rgb.r(), rgb.g(), rgb.b()};
    }

    /**
     * Converts RGB to HSL.
     * Algorithm from https://en.wikipedia.org/wiki/HSL_and_HSV
     */
    private static float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        // Lightness
        float l = (max + min) / 2.0f;

        // Saturation
        float s;
        if (delta == 0.0f) {
            s = 0.0f;
        } else {
            s = delta / (1.0f - Math.abs(2.0f * l - 1.0f));
        }

        // Hue
        float h;
        if (delta == 0.0f) {
            h = 0.0f;
        } else if (max == rf) {
            h = 60.0f * (((gf - bf) / delta) % 6.0f);
        } else if (max == gf) {
            h = 60.0f * ((bf - rf) / delta + 2.0f);
        } else {
            h = 60.0f * ((rf - gf) / delta + 4.0f);
        }

        if (h < 0.0f) {
            h += 360.0f;
        }

        return new float[]{h, s * 100.0f, l * 100.0f};
    }

    /**
     * Converts HSL to RGB.
     * Algorithm from https://en.wikipedia.org/wiki/HSL_and_HSV
     */
    private static int[] hslToRgb(float h, float s, float l) {
        h = h % 360.0f;
        if (h < 0.0f) {
            h += 360.0f;
        }
        s = s / 100.0f;
        l = l / 100.0f;

        float c = (1.0f - Math.abs(2.0f * l - 1.0f)) * s;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = l - c / 2.0f;

        float r, g, b;
        if (h < 60.0f) {
            r = c;
            g = x;
            b = 0.0f;
        } else if (h < 120.0f) {
            r = x;
            g = c;
            b = 0.0f;
        } else if (h < 180.0f) {
            r = 0.0f;
            g = c;
            b = x;
        } else if (h < 240.0f) {
            r = 0.0f;
            g = x;
            b = c;
        } else if (h < 300.0f) {
            r = x;
            g = 0.0f;
            b = c;
        } else {
            r = c;
            g = 0.0f;
            b = x;
        }

        return new int[]{
            Math.round((r + m) * 255.0f),
            Math.round((g + m) * 255.0f),
            Math.round((b + m) * 255.0f)
        };
    }

    /**
     * Converts RGB to HSV.
     */
    private static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        // Hue
        float h;
        if (delta == 0.0f) {
            h = 0.0f;
        } else if (max == rf) {
            h = 60.0f * (((gf - bf) / delta) % 6.0f);
        } else if (max == gf) {
            h = 60.0f * ((bf - rf) / delta + 2.0f);
        } else {
            h = 60.0f * ((rf - gf) / delta + 4.0f);
        }

        if (h < 0.0f) {
            h += 360.0f;
        }

        // Saturation
        float s = max == 0.0f ? 0.0f : delta / max;

        // Value
        float v = max;

        return new float[]{h, s * 100.0f, v * 100.0f};
    }

    /**
     * Converts HSV to RGB.
     */
    private static int[] hsvToRgb(float h, float s, float v) {
        h = h % 360.0f;
        if (h < 0.0f) {
            h += 360.0f;
        }
        s = s / 100.0f;
        v = v / 100.0f;

        float c = v * s;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = v - c;

        float r, g, b;
        if (h < 60.0f) {
            r = c;
            g = x;
            b = 0.0f;
        } else if (h < 120.0f) {
            r = x;
            g = c;
            b = 0.0f;
        } else if (h < 180.0f) {
            r = 0.0f;
            g = c;
            b = x;
        } else if (h < 240.0f) {
            r = 0.0f;
            g = x;
            b = c;
        } else if (h < 300.0f) {
            r = x;
            g = 0.0f;
            b = c;
        } else {
            r = c;
            g = 0.0f;
            b = x;
        }

        return new int[]{
            Math.round((r + m) * 255.0f),
            Math.round((g + m) * 255.0f),
            Math.round((b + m) * 255.0f)
        };
    }
}
