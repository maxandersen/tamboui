/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.dvdlogo;

import java.time.Duration;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.Clear;
import dev.tamboui.widgets.dvdlogo.DVDLogo;
import dev.tamboui.widgets.dvdlogo.DVDLogoState;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Standalone DVD logo screensaver demo.
 *
 * <p>The classic DVD screensaver: a "DVD" logo bounces around the terminal,
 * changing color each time it hits an edge.
 *
 * <p>Key bindings:
 * <ul>
 *   <li>q / Ctrl+C - Quit</li>
 * </ul>
 */
public class DVDLogoDemo implements Element {

    private final DVDLogoState state = new DVDLogoState();

    /**
     * Demo entry point.
     *
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        DVDLogoDemo demo = new DVDLogoDemo();

        TuiConfig config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(50))
                .build();

        try (ToolkitRunner runner = ToolkitRunner.create(config)) {
            runner.run(() -> demo);
        }
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        // Black background
        Clear.INSTANCE.render(area, frame.buffer());

        // Advance the logo position
        state.update(area.width(), area.height());

        // Render the bouncing logo
        if (area.width() >= DVDLogoState.LOGO_WIDTH && area.height() >= DVDLogoState.LOGO_HEIGHT) {
            Rect logoRect = new Rect(
                    area.x() + state.logoX(),
                    area.y() + state.logoY(),
                    DVDLogoState.LOGO_WIDTH,
                    DVDLogoState.LOGO_HEIGHT);
            frame.renderStatefulWidget(DVDLogo.INSTANCE, logoRect, state);
        }

        // Help hint at the bottom
        text(" [q] Quit ")
                .dim()
                .fg(Color.DARK_GRAY)
                .render(frame, new Rect(area.x(), area.bottom() - 1, area.width(), 1), context);
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return Size.UNKNOWN;
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Let the ToolkitRunner handle 'q' / Ctrl+C
        return EventResult.UNHANDLED;
    }
}
