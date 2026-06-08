/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.capability;

import java.util.EnumSet;
import java.util.Set;

import dev.tamboui.image.protocol.BrailleProtocol;
import dev.tamboui.image.protocol.HalfBlockProtocol;
import dev.tamboui.image.protocol.ITermProtocol;
import dev.tamboui.image.protocol.ImageProtocol;
import dev.tamboui.image.protocol.KittyProtocol;
import dev.tamboui.image.protocol.KittyUnicodePlaceholderProtocol;
import dev.tamboui.image.protocol.SixelProtocol;

/**
 * Detects and caches terminal image capabilities.
 * <p>
 * Detection is performed using environment variables to identify the terminal emulator.
 * This approach is fast (no I/O) and works in most cases.
 *
 * <pre>{@code
 * TerminalImageCapabilities caps = TerminalImageCapabilities.detect();
 * ImageProtocol protocol = caps.bestProtocol();
 * }</pre>
 *
 * <h2>Terminal Detection</h2>
 * <table>
 *   <caption>Environment variable to protocol mapping</caption>
 *   <tr><th>Environment</th><th>Protocol</th></tr>
 *   <tr><td>KITTY_WINDOW_ID</td><td>Kitty (unicode placeholders)</td></tr>
 *   <tr><td>TERM=xterm-kitty</td><td>Kitty (unicode placeholders)</td></tr>
 *   <tr><td>TERM=xterm-ghostty</td><td>Kitty (unicode placeholders)</td></tr>
 *   <tr><td>WEZTERM_PANE</td><td>Kitty (direct, no unicode placeholders)</td></tr>
 *   <tr><td>ITERM_SESSION_ID</td><td>iTerm2</td></tr>
 *   <tr><td>TERM=rio</td><td>iTerm2, Sixel</td></tr>
 *   <tr><td>KONSOLE_VERSION</td><td>Sixel</td></tr>
 *   <tr><td>TERM contains mlterm</td><td>Sixel</td></tr>
 * </table>
 */
public final class TerminalImageCapabilities {

    private static volatile TerminalImageCapabilities instance;

    private final Set<TerminalImageProtocol> supportedProtocols;
    private final TerminalImageProtocol bestSupport;
    private final boolean kittyUnicodePlaceholders;

    private TerminalImageCapabilities(Set<TerminalImageProtocol> supportedProtocols,
                                      boolean kittyUnicodePlaceholders) {
        this.supportedProtocols = EnumSet.copyOf(supportedProtocols);
        this.bestSupport = determineBestSupport(supportedProtocols);
        this.kittyUnicodePlaceholders = kittyUnicodePlaceholders;
    }

    /**
     * Detects terminal capabilities from the current environment.
     * <p>
     * The result is cached for subsequent calls.
     *
     * @return the detected capabilities
     */
    public static TerminalImageCapabilities detect() {
        if (instance == null) {
            synchronized (TerminalImageCapabilities.class) {
                if (instance == null) {
                    instance = detectFromEnvironment();
                }
            }
        }
        return instance;
    }

    /**
     * Forces re-detection of terminal capabilities.
     * <p>
     * Useful when the terminal environment may have changed.
     *
     * @return the newly detected capabilities
     */
    public static TerminalImageCapabilities refresh() {
        synchronized (TerminalImageCapabilities.class) {
            instance = detectFromEnvironment();
            return instance;
        }
    }

    /**
     * Creates capabilities with explicitly specified support.
     * <p>
     * Useful for testing or when environment detection is unreliable.
     *
     * @param supportedProtocols the set of supported protocols
     * @return capabilities with the specified support
     */
    public static TerminalImageCapabilities withSupport(Set<TerminalImageProtocol> supportedProtocols) {
        return new TerminalImageCapabilities(supportedProtocols, false);
    }

    /**
     * Creates capabilities with explicitly specified support and unicode placeholder flag.
     * <p>
     * Useful for testing or when environment detection is unreliable.
     *
     * @param supportedProtocols       the set of supported protocols
     * @param kittyUnicodePlaceholders true if the terminal supports Kitty unicode placeholders
     * @return capabilities with the specified support
     */
    public static TerminalImageCapabilities withSupport(Set<TerminalImageProtocol> supportedProtocols,
                                                        boolean kittyUnicodePlaceholders) {
        return new TerminalImageCapabilities(supportedProtocols, kittyUnicodePlaceholders);
    }

    /**
     * Returns the best available image support level.
     *
     * @return the best image support
     */
    public TerminalImageProtocol bestSupport() {
        return bestSupport;
    }

    /**
     * Returns true if the specified protocol is supported.
     *
     * @param support the support level to check
     * @return true if supported
     */
    public boolean supports(TerminalImageProtocol support) {
        return supportedProtocols.contains(support);
    }

    /**
     * Returns all supported protocols.
     * <p>
     * The returned set is immutable.
     *
     * @return the set of supported protocols
     */
    public Set<TerminalImageProtocol> supportedProtocols() {
        return supportedProtocols;
    }

    /**
     * Returns true if any native image protocol is supported.
     *
     * @return true if Kitty, iTerm2, or Sixel is available
     */
    public boolean supportsNativeImages() {
        return supports(TerminalImageProtocol.KITTY)
            || supports(TerminalImageProtocol.ITERM2)
            || supports(TerminalImageProtocol.SIXEL);
    }

    /**
     * Returns whether the terminal supports Kitty unicode placeholders.
     * <p>
     * When {@code true}, the {@link KittyUnicodePlaceholderProtocol} is used, which transmits
     * the image once and renders via buffer cells — eliminating all per-frame retransmission.
     * When {@code false}, the direct {@link KittyProtocol} is used with stable image ids and
     * redraw suppression.
     * <p>
     * Known terminals with unicode placeholder support: Kitty (&ge; 0.28.0), Ghostty.
     * Known terminals <em>without</em>: WezTerm, Konsole, Warp.
     *
     * @return true if Kitty unicode placeholders are supported
     */
    public boolean supportsKittyUnicodePlaceholders() {
        return kittyUnicodePlaceholders;
    }

    /**
     * Returns the best available image protocol implementation.
     * <p>
     * For terminals supporting Kitty with unicode placeholders, this returns
     * {@link KittyUnicodePlaceholderProtocol} which transmits the image once and renders via
     * buffer cells. For terminals with Kitty support but without unicode placeholders (e.g.
     * WezTerm), the direct {@link KittyProtocol} with stable image ids is returned.
     *
     * @return the best available protocol
     */
    public ImageProtocol bestProtocol() {
        switch (bestSupport) {
            case KITTY:
                return kittyUnicodePlaceholders
                    ? new KittyUnicodePlaceholderProtocol()
                    : new KittyProtocol();
            case ITERM2:
                return new ITermProtocol();
            case SIXEL:
                return new SixelProtocol();
            case HALF_BLOCK:
                return new HalfBlockProtocol();
            case BRAILLE:
                return new BrailleProtocol();
            default:
                return new HalfBlockProtocol();
        }
    }

    /**
     * Returns a protocol implementation for the specified support level.
     * <p>
     * For {@link TerminalImageProtocol#KITTY}, returns the unicode placeholder variant
     * if the terminal supports it, otherwise the direct variant.
     *
     * @param support the support level
     * @return the protocol implementation, or null if not available
     */
    public ImageProtocol protocolFor(TerminalImageProtocol support) {
        switch (support) {
            case KITTY:
                return kittyUnicodePlaceholders
                    ? new KittyUnicodePlaceholderProtocol()
                    : new KittyProtocol();
            case ITERM2:
                return new ITermProtocol();
            case SIXEL:
                return new SixelProtocol();
            case HALF_BLOCK:
                return new HalfBlockProtocol();
            case BRAILLE:
                return new BrailleProtocol();
            default:
                return null;
        }
    }

    private static TerminalImageCapabilities detectFromEnvironment() {
        Set<TerminalImageProtocol> supported = EnumSet.noneOf(TerminalImageProtocol.class);
        boolean unicodePlaceholders = false;

        // Always support character-based fallbacks
        supported.add(TerminalImageProtocol.HALF_BLOCK);
        supported.add(TerminalImageProtocol.BRAILLE);

        // Check for Kitty terminal (supports unicode placeholders since 0.28.0)
        if (getEnv("KITTY_WINDOW_ID") != null) {
            supported.add(TerminalImageProtocol.KITTY);
            unicodePlaceholders = true;
        }

        String term = getEnv("TERM");
        if (term != null) {
            if (term.equals("xterm-kitty")) {
                supported.add(TerminalImageProtocol.KITTY);
                unicodePlaceholders = true;
            }
            if (term.equals("xterm-ghostty")) {
                // Ghostty implements Kitty protocol with unicode placeholders
                supported.add(TerminalImageProtocol.KITTY);
                unicodePlaceholders = true;
            }
            if (term.equals("rio")) {
                // Rio supports iTerm2 and Sixel; iTerm2 is preferred
                supported.add(TerminalImageProtocol.ITERM2);
                supported.add(TerminalImageProtocol.SIXEL);
            }
            if (term.contains("mlterm")) {
                supported.add(TerminalImageProtocol.SIXEL);
            }
        }

        // Check for WezTerm (supports Kitty graphics but NOT unicode placeholders)
        if (getEnv("WEZTERM_PANE") != null) {
            supported.add(TerminalImageProtocol.KITTY);
            // unicodePlaceholders stays false — WezTerm does not implement the placeholder part
        }

        // Check for Ghostty (supports Kitty protocol with unicode placeholders). TERM is only
        // "xterm-ghostty" when the bundled terminfo is installed; the env var is always present,
        // so it is the reliable hint.
        if (getEnv("GHOSTTY_RESOURCES_DIR") != null) {
            supported.add(TerminalImageProtocol.KITTY);
            unicodePlaceholders = true;
        }

        // Check for iTerm2
        if (getEnv("ITERM_SESSION_ID") != null) {
            supported.add(TerminalImageProtocol.ITERM2);
        }

        // Check for Konsole (supports Sixel in recent versions)
        String konsoleVersion = getEnv("KONSOLE_VERSION");
        if (konsoleVersion != null) {
            try {
                int version = Integer.parseInt(konsoleVersion.split("\\.")[0]);
                if (version >= 22) {
                    supported.add(TerminalImageProtocol.SIXEL);
                }
            } catch (NumberFormatException ignored) {
                // Unknown version format, assume Sixel support
                supported.add(TerminalImageProtocol.SIXEL);
            }
        }

        // Check TERM_PROGRAM for additional hints
        String termProgram = getEnv("TERM_PROGRAM");
        if (termProgram != null) {
            if (termProgram.equalsIgnoreCase("iTerm.app")) {
                supported.add(TerminalImageProtocol.ITERM2);
            }
            if (termProgram.equalsIgnoreCase("WezTerm")) {
                supported.add(TerminalImageProtocol.KITTY);
                // No unicode placeholders for WezTerm
            }
            if (termProgram.equalsIgnoreCase("ghostty")) {
                supported.add(TerminalImageProtocol.KITTY);
                unicodePlaceholders = true;
            }
        }

        return new TerminalImageCapabilities(supported, unicodePlaceholders);
    }

    private static TerminalImageProtocol determineBestSupport(Set<TerminalImageProtocol> supported) {
        // EnumSet iterates in enum declaration order (which is our priority order)
        return supported.stream()
            .filter(p -> p != TerminalImageProtocol.NONE)
            .findFirst()
            .orElse(TerminalImageProtocol.NONE);
    }

    private static String getEnv(String name) {
        try {
            return System.getenv(name);
        } catch (SecurityException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("TerminalImageCapabilities[best=%s, supported=%s, kittyUnicodePlaceholders=%s]",
            bestSupport, supportedProtocols, kittyUnicodePlaceholders);
    }
}
