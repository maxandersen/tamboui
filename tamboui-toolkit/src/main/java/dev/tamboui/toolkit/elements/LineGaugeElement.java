/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.gauge.LineGauge;

/// A DSL wrapper for the LineGauge widget.
///
/// A compact single-line progress indicator.
/// ```java
/// lineGauge(0.75)
///      .label("Progress: ")
///      .filledColor(Color.GREEN)
/// }
/// ```
public final class LineGaugeElement extends StyledElement<LineGaugeElement> {

    private double ratio = 0.0;
    private String label;
    private Style filledStyle = Style.EMPTY;
    private Style unfilledStyle = Style.EMPTY;
    private LineGauge.LineSet lineSet = LineGauge.NORMAL;

    public LineGaugeElement() {
    }

    public LineGaugeElement(double ratio) {
        this.ratio = Math.max(0.0, Math.min(1.0, ratio));
    }

    public LineGaugeElement(int percent) {
        this.ratio = Math.max(0, Math.min(100, percent)) / 100.0;
    }

    /// Sets the progress as a ratio (0.0-1.0).
    public LineGaugeElement ratio(double ratio) {
        this.ratio = Math.max(0.0, Math.min(1.0, ratio));
        return this;
    }

    /// Sets the progress as a percentage (0-100).
    public LineGaugeElement percent(int percent) {
        this.ratio = Math.max(0, Math.min(100, percent)) / 100.0;
        return this;
    }

    /// Sets the label displayed before the gauge line.
    public LineGaugeElement label(String label) {
        this.label = label;
        return this;
    }

    /// Sets the style for the filled portion.
    public LineGaugeElement filledStyle(Style style) {
        this.filledStyle = style;
        return this;
    }

    /// Sets the color for the filled portion.
    public LineGaugeElement filledColor(Color color) {
        this.filledStyle = Style.EMPTY.fg(color);
        return this;
    }

    /// Sets the style for the unfilled portion.
    public LineGaugeElement unfilledStyle(Style style) {
        this.unfilledStyle = style;
        return this;
    }

    /// Sets the color for the unfilled portion.
    public LineGaugeElement unfilledColor(Color color) {
        this.unfilledStyle = Style.EMPTY.fg(color);
        return this;
    }

    /// Uses thick line characters.
    public LineGaugeElement thick() {
        this.lineSet = LineGauge.THICK;
        return this;
    }

    /// Uses double line characters.
    public LineGaugeElement doubleLine() {
        this.lineSet = LineGauge.DOUBLE;
        return this;
    }

    /// Sets the line character set.
    public LineGaugeElement lineSet(LineGauge.LineSet lineSet) {
        this.lineSet = lineSet;
        return this;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        LineGauge.Builder builder = LineGauge.builder()
            .ratio(ratio)
            .style(context.currentStyle())
            .filledStyle(filledStyle)
            .unfilledStyle(unfilledStyle)
            .lineSet(lineSet);

        if (label != null) {
            builder.label(label);
        }

        frame.renderWidget(builder.build(), area);
    }
}
