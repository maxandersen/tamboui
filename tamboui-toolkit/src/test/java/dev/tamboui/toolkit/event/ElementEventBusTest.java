/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ElementEventBusTest {

    @Test
    @DisplayName("broadcast handlers receive emitted events")
    void broadcastHandlersReceiveEvents() {
        ElementEventBus bus = new ElementEventBus();
        AtomicBoolean handled = new AtomicBoolean(false);
        TestEvent event = new TestEvent("source", "payload");

        bus.subscribe(TestEvent.class, e -> {
            handled.set(true);
            return EventResult.HANDLED;
        });

        EventResult result = bus.emit(event);

        assertThat(handled.get()).isTrue();
        assertThat(result).isEqualTo(EventResult.HANDLED);
    }

    @Test
    @DisplayName("targeted handlers receive only matching events")
    void targetedHandlersReceiveMatchingEvents() {
        ElementEventBus bus = new ElementEventBus();
        AtomicBoolean targetHandled = new AtomicBoolean(false);
        AtomicBoolean broadcastHandled = new AtomicBoolean(false);

        bus.subscribe("target", TestEvent.class, e -> {
            targetHandled.set(true);
            return EventResult.HANDLED;
        });
        bus.subscribe(TestEvent.class, e -> {
            broadcastHandled.set(true);
            return EventResult.UNHANDLED;
        });

        bus.emitTo("target", new TestEvent("source", "payload"));

        assertThat(targetHandled.get()).isTrue();
        assertThat(broadcastHandled.get()).isTrue();
    }

    @Test
    @DisplayName("frame handlers are cleared between render cycles")
    void frameHandlersAreCleared() {
        ElementEventBus bus = new ElementEventBus();
        AtomicInteger count = new AtomicInteger();

        ElementEventScope frame = bus.frameScope();
        frame.on(TestEvent.class, e -> {
            count.incrementAndGet();
            return EventResult.HANDLED;
        });

        bus.emit(new TestEvent("source", "payload"));
        bus.clearFrameHandlers();
        bus.emit(new TestEvent("source", "payload"));

        assertThat(count.get()).isEqualTo(1);
    }

    private static final class TestEvent implements ElementEvent {
        private final String sourceId;
        private final String targetId;
        private final String payload;

        private TestEvent(String sourceId, String payload) {
            this(sourceId, null, payload);
        }

        private TestEvent(String sourceId, String targetId, String payload) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.payload = payload;
        }

        @Override
        public String sourceId() {
            return sourceId;
        }

        @Override
        public String targetId() {
            return targetId;
        }

        public String payload() {
            return payload;
        }
    }
}
