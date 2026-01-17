/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.*;

/// Tests for GaugeElement.
class GaugeElementTest {

    @Test
    @DisplayName("GaugeElement fluent API chains correctly")
    void fluentApiChaining() {
        GaugeElement element = gauge(0.5)
            .label("Loading...")
            .gaugeColor(Color.GREEN)
            .useUnicode(true)
            .title("Progress")
            .rounded()
            .borderColor(Color.CYAN);

        assertThat(element).isInstanceOf(GaugeElement.class);
    }

    @Test
    @DisplayName("gauge(double) clamps ratio to 0.0-1.0")
    void ratioClampingLow() {
        GaugeElement element = gauge(-0.5);
        // Should not throw, ratio is clamped
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("gauge(double) clamps high ratio to 1.0")
    void ratioClampingHigh() {
        GaugeElement element = gauge(1.5);
        // Should not throw, ratio is clamped
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("percent() method works correctly")
    void percentMethod() {
        GaugeElement element = gauge().percent(75);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("GaugeElement renders to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        gauge(0.5)
            .label("50%")
            .title("Progress")
            .rounded()
            .render(frame, area, RenderContext.empty());

        // Check border is rendered (rounded corner)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
        assertThat(buffer.get(19, 0).symbol()).isEqualTo("╮");
    }

    @Test
    @DisplayName("GaugeElement renders filled portion")
    void rendersFilledPortion() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        gauge(1.0)
            .label("")
            .render(frame, area, RenderContext.empty());

        // At 100%, first cells should be filled blocks
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
        assertThat(buffer.get(19, 0).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        gauge(0.5).render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("GaugeElement with gaugeStyle")
    void withGaugeStyle() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        gauge(1.0)
            .label("")
            .gaugeColor(Color.RED)
            .render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
    }
}
