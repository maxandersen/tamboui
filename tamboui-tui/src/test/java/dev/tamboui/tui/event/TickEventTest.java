/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TickEventTest {

    @Test
    @DisplayName("of creates a tick event with the given frame count")
    void ofCreatesTickEvent() {
        TickEvent event = TickEvent.of(42);
        assertThat(event.frameCount()).isEqualTo(42);
    }

    @Test
    @DisplayName("tick event is an Event")
    void tickEventIsEvent() {
        Event event = TickEvent.of(1);
        assertThat(event).isInstanceOf(TickEvent.class);
    }

    @Test
    @DisplayName("frame count starts at expected values")
    void frameCountValues() {
        assertThat(TickEvent.of(0).frameCount()).isEqualTo(0);
        assertThat(TickEvent.of(1000).frameCount()).isEqualTo(1000);
        assertThat(TickEvent.of(Long.MAX_VALUE).frameCount()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @DisplayName("ticks with the same frame count are equal")
    void equalityByFrameCount() {
        assertThat(TickEvent.of(7)).isEqualTo(TickEvent.of(7));
        assertThat(TickEvent.of(7)).isNotEqualTo(TickEvent.of(8));
    }
}
