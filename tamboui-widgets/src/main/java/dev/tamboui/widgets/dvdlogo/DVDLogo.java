/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.dvdlogo;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widgets.Clear;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;

/**
 * A widget that renders a bouncing "DVD" logo, reminiscent of the classic DVD screensaver.
 *
 * <p>The logo uses Unicode Braille characters to render a dot-matrix style "DVD" text,
 * similar to the viral Claude Code hook by @itseieio.
 *
 * <p>This is a {@link StatefulWidget} – the caller must provide a {@link DVDLogoState} that
 * tracks position, velocity, and color. Call {@link DVDLogoState#update(int, int)} once per
 * frame before rendering to advance the animation.
 *
 * <p>The widget renders into whatever {@link Rect} it is given. Use the state's
 * {@link DVDLogoState#logoX()} and {@link DVDLogoState#logoY()} together with
 * {@link DVDLogoState#LOGO_WIDTH} / {@link DVDLogoState#LOGO_HEIGHT} to compute
 * the correct rect each frame.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * DVDLogoState state = new DVDLogoState();
 *
 * // Each render frame:
 * state.update(frame.width(), frame.height());
 * Rect logoRect = new Rect(state.logoX(), state.logoY(),
 *                          DVDLogoState.LOGO_WIDTH, DVDLogoState.LOGO_HEIGHT);
 * frame.renderWidget(Clear.INSTANCE, logoRect);
 * frame.renderStatefulWidget(DVDLogo.INSTANCE, logoRect, state);
 * }</pre>
 */
public final class DVDLogo implements StatefulWidget<DVDLogoState> {

    /**
     * Singleton instance – the widget has no configuration of its own.
     */
    public static final DVDLogo INSTANCE = new DVDLogo();

    // Braille dot-matrix "DVD" logo rows (generated from a 16×16 pixel bitmap).
    // Each braille character encodes a 2×4 pixel block. The result is 14 chars wide × 4 rows tall,
    // which fits inside the 16×6 widget area (1-char border on each side).
    private static final String[] LOGO_ROWS = {
        "⣾⠉⢳⡄⠀⣿⠀⢸⡇⠀⣾⠉⢳⡄",
        "⣿⠀⢸⡇⠀⠹⣆⡾⠁⠀⣿⠀⢸⡇",
        "⣿⠀⢸⡇⠀⠀⢹⠁⠀⠀⣿⠀⢸⡇",
        "⠻⠤⠞⠁⠀⠀⠸⠀⠀⠀⠻⠤⠞⠁",
    };

    private DVDLogo() {
    }

    @Override
    public void render(Rect area, Buffer buffer, DVDLogoState state) {
        if (area.isEmpty()) {
            return;
        }

        // Clear the cell background first so the logo appears over other content
        Clear.INSTANCE.render(area, buffer);

        // Colored rounded border
        Style style = Style.EMPTY.fg(state.color());
        Block block = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(style)
                .build();
        block.render(area, buffer);

        // Render braille "DVD" rows inside the border
        Rect inner = block.inner(area);
        for (int i = 0; i < LOGO_ROWS.length && i < inner.height(); i++) {
            buffer.setString(inner.x(), inner.y() + i, LOGO_ROWS[i], style);
        }
    }
}
