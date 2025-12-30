/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.widgets.StatefulWidget;
import dev.tamboui.widgets.Widget;

import java.util.Optional;

/**
 * A frame represents a single render cycle.
 * Widgets are rendered to the frame's buffer.
 */
public final class Frame {

    private final Buffer buffer;
    private final Rect area;
    private Position cursorPosition;
    private boolean cursorVisible;

    Frame(Buffer buffer) {
        this.buffer = buffer;
        this.area = buffer.area();
        this.cursorPosition = null;
        this.cursorVisible = false;
    }

    /**
     * Creates a frame for testing purposes.
     * This allows tests to create frames without going through Terminal.
     *
     * @param buffer the buffer to render to
     * @return a new frame backed by the given buffer
     */
    public static Frame forTesting(Buffer buffer) {
        return new Frame(buffer);
    }

    /**
     * Returns the area available for rendering.
     *
     * @return the rendering area
     */
    public Rect area() {
        return area;
    }

    /**
     * Returns the underlying buffer.
     *
     * @return the buffer
     */
    public Buffer buffer() {
        return buffer;
    }

    /**
     * Returns the terminal width.
     *
     * @return the terminal width
     */
    public int width() {
        return area.width();
    }

    /**
     * Returns the terminal height.
     *
     * @return the terminal height
     */
    public int height() {
        return area.height();
    }

    /**
     * Renders a widget to the given area.
     *
     * @param widget the widget to render
     * @param area the area to render within
     */
    public void renderWidget(Widget widget, Rect area) {
        if (area.isEmpty()) {
            return;
        }

        // Render to a clipped buffer so widgets cannot paint outside `area`.
        // To preserve layering semantics, start by copying the current content
        // for the area from the main buffer into the clipped buffer.
        Buffer clipped = Buffer.empty(area);
        for (int y = area.top(); y < area.bottom(); y++) {
            for (int x = area.left(); x < area.right(); x++) {
                Cell cell = buffer.get(x, y);
                clipped.set(x, y, cell);
            }
        }

        widget.render(area, clipped);
        buffer.merge(clipped, area.x(), area.y());
    }

    /**
     * Renders a stateful widget to the given area.
     *
     * @param <S> the state type
     * @param widget the stateful widget to render
     * @param area the area to render within
     * @param state the widget state
     */
    public <S> void renderStatefulWidget(StatefulWidget<S> widget, Rect area, S state) {
        if (area.isEmpty()) {
            return;
        }

        Buffer clipped = Buffer.empty(area);
        for (int y = area.top(); y < area.bottom(); y++) {
            for (int x = area.left(); x < area.right(); x++) {
                Cell cell = buffer.get(x, y);
                clipped.set(x, y, cell);
            }
        }

        widget.render(area, clipped, state);
        buffer.merge(clipped, area.x(), area.y());
    }

    /**
     * Sets the cursor position. The cursor will be shown at this position
     * after the frame is drawn.
     *
     * @param position the cursor position
     */
    public void setCursorPosition(Position position) {
        this.cursorPosition = position;
        this.cursorVisible = true;
    }

    /**
     * Sets the cursor position using x and y coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void setCursorPosition(int x, int y) {
        setCursorPosition(new Position(x, y));
    }

    /**
     * Returns the cursor position if set.
     *
     * @return the cursor position, or empty if not set
     */
    Optional<Position> cursorPosition() {
        return Optional.ofNullable(cursorPosition);
    }

    /**
     * Returns whether the cursor should be visible.
     *
     * @return true if the cursor should be visible, false otherwise
     */
    boolean isCursorVisible() {
        return cursorVisible;
    }
}
