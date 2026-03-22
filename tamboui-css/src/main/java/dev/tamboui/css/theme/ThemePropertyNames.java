package dev.tamboui.css.theme;

/**
 * Documents the standard semantic design tokens that themes should define.
 * This is documentation only - actual values are defined in .tcss theme files.
 *
 * <p>All theme .tcss files should define these variables for consistency.
 * Applications should use these semantic names instead of hardcoded colors
 * to ensure theme independence.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // In markup
 * Text.from("[" + SemanticTokens.PRIMARY + "]Title[/]")
 *
 * // In CSS
 * Panel { border-color: $primary; }
 * }</pre>
 */
public final class ThemePropertyNames {

    // Prevent instantiation - documentation only
    private ThemePropertyNames() {}

    // ===== Core Brand Colors =====

    /**
     * Primary brand color.
     * Used for main actions, key UI elements, and brand identity.
     */
    public static final String PRIMARY = "primary";

    /**
     * Secondary accent color.
     * Used for supporting UI elements and secondary actions.
     */
    public static final String SECONDARY = "secondary";

    /**
     * Accent/highlight color.
     * Used for emphasis, highlights, and special callouts.
     */
    public static final String ACCENT = "accent";

    // ===== Feedback Colors =====

    /**
     * Error state color.
     * Used for error messages, validation failures, and destructive actions.
     */
    public static final String ERROR = "error";

    /**
     * Warning state color.
     * Used for warnings, cautions, and important notices.
     */
    public static final String WARNING = "warning";

    /**
     * Success state color.
     * Used for success messages, confirmations, and positive feedback.
     */
    public static final String SUCCESS = "success";

    /**
     * Informational state color.
     * Used for info messages, tips, and neutral notifications.
     */
    public static final String INFO = "info";

    // ===== Surface Colors =====

    /**
     * Main background color.
     * Used for the primary application background.
     */
    public static final String BACKGROUND = "background";

    /**
     * Elevated surface color.
     * Used for panels, cards, and elevated UI elements.
     */
    public static final String SURFACE = "surface";

    /**
     * Alternative surface color.
     * Used for secondary panels and nested surfaces.
     */
    public static final String SURFACE_VARIANT = "surface-variant";

    /**
     * Dimmed surface color.
     * Used for disabled surfaces and subtle backgrounds.
     */
    public static final String SURFACE_DIM = "surface-dim";

    // ===== Text Colors =====

    /**
     * Primary text color.
     * Used for main body text and headings.
     */
    public static final String TEXT = "text";

    /**
     * Muted text color.
     * Used for secondary text, descriptions, and less important content.
     */
    public static final String TEXT_MUTED = "text-muted";

    /**
     * Disabled text color.
     * Used for disabled or inactive text.
     */
    public static final String TEXT_DISABLED = "text-disabled";

    /**
     * Text color for use on primary color backgrounds.
     * Ensures proper contrast when text appears on primary-colored surfaces.
     */
    public static final String TEXT_ON_PRIMARY = "text-on-primary";

    // ===== Border Colors =====

    /**
     * Default border color.
     * Used for standard borders, dividers, and outlines.
     */
    public static final String BORDER = "border";

    /**
     * Focused element border color.
     * Used to indicate keyboard focus and active selection.
     */
    public static final String BORDER_FOCUS = "border-focus";

    /**
     * Subtle border color.
     * Used for subtle dividers and low-emphasis borders.
     */
    public static final String BORDER_MUTED = "border-muted";
}
