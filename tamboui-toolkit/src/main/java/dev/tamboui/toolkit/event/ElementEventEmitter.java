/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

/**
 * Emits element events.
 */
public interface ElementEventEmitter {

    /**
     * Emits an event, honoring the event's target ID if present.
     *
     * @param event the event to emit
     * @return HANDLED if any handler handled the event, UNHANDLED otherwise
     */
    EventResult emit(ElementEvent event);

    /**
     * Emits an event to a specific target element.
     * <p>
     * This overrides any target ID present on the event for dispatch purposes.
     *
     * @param targetId the target element ID
     * @param event the event to emit
     * @return HANDLED if any handler handled the event, UNHANDLED otherwise
     */
    EventResult emitTo(String targetId, ElementEvent event);

    /**
     * Returns a no-op emitter.
     *
     * @return a no-op emitter
     */
    static ElementEventEmitter noop() {
        return NoopEmitter.INSTANCE;
    }

    /**
     * No-op emitter implementation.
     */
    final class NoopEmitter implements ElementEventEmitter {
        private static final NoopEmitter INSTANCE = new NoopEmitter();

        private NoopEmitter() {
        }

        @Override
        public EventResult emit(ElementEvent event) {
            return EventResult.UNHANDLED;
        }

        @Override
        public EventResult emitTo(String targetId, ElementEvent event) {
            return EventResult.UNHANDLED;
        }
    }
}
