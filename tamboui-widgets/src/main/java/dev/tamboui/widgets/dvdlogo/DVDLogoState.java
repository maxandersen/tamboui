/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.dvdlogo;

import dev.tamboui.style.Color;

/**
 * State for the {@link DVDLogo} widget, tracking the bouncing position and color.
 *
 * <p>The logo bounces around the available area, changing color each time it hits an edge,
 * just like the classic DVD screensaver.
 *
 * <pre>{@code
 * DVDLogoState state = new DVDLogoState();
 *
 * // In your render loop (each frame):
 * state.update(area.width(), area.height());
 * Rect logoRect = new Rect(state.logoX(), state.logoY(),
 *                          DVDLogoState.LOGO_WIDTH, DVDLogoState.LOGO_HEIGHT);
 * frame.renderStatefulWidget(DVDLogo.INSTANCE, logoRect, state);
 * }</pre>
 */
public final class DVDLogoState {

    /** Width of the DVD logo widget including its border. */
    public static final int LOGO_WIDTH = 16;

    /** Height of the DVD logo widget including its border. */
    public static final int LOGO_HEIGHT = 6;

    private static final Color[] COLORS = {
        Color.RED,
        Color.GREEN,
        Color.YELLOW,
        Color.BLUE,
        Color.MAGENTA,
        Color.CYAN,
        Color.WHITE
    };

    private double x;
    private double y;
    private double vx;
    private double vy;
    private int colorIndex;

    /**
     * Creates a new DVDLogoState with default starting position and velocity.
     */
    public DVDLogoState() {
        this.x = 2;
        this.y = 2;
        this.vx = 1.0;
        this.vy = 0.5;
        this.colorIndex = 0;
    }

    /**
     * Advances the animation one step, bouncing off the edges of the given area.
     * Color changes each time the logo hits an edge.
     *
     * @param areaWidth  the width of the enclosing area
     * @param areaHeight the height of the enclosing area
     */
    public void update(int areaWidth, int areaHeight) {
        if (areaWidth <= LOGO_WIDTH || areaHeight <= LOGO_HEIGHT) {
            return;
        }

        x += vx;
        y += vy;

        int maxX = areaWidth - LOGO_WIDTH;
        int maxY = areaHeight - LOGO_HEIGHT;

        boolean bounced = false;
        if (x <= 0) {
            x = 0;
            vx = Math.abs(vx);
            bounced = true;
        } else if (x >= maxX) {
            x = maxX;
            vx = -Math.abs(vx);
            bounced = true;
        }
        if (y <= 0) {
            y = 0;
            vy = Math.abs(vy);
            bounced = true;
        } else if (y >= maxY) {
            y = maxY;
            vy = -Math.abs(vy);
            bounced = true;
        }

        if (bounced) {
            colorIndex = (colorIndex + 1) % COLORS.length;
        }
    }

    /**
     * Returns the x coordinate of the logo's top-left corner.
     *
     * @return the x coordinate
     */
    public int logoX() {
        return (int) x;
    }

    /**
     * Returns the y coordinate of the logo's top-left corner.
     *
     * @return the y coordinate
     */
    public int logoY() {
        return (int) y;
    }

    /**
     * Returns the current logo color.
     *
     * @return the color
     */
    public Color color() {
        return COLORS[colorIndex];
    }
}
