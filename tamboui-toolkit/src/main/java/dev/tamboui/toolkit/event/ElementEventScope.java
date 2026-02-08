/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

/**
 * Frame-scoped registry for element events.
 * <p>
 * Handlers registered on this scope are cleared at the start of each render cycle.
 */
public interface ElementEventScope extends ElementEventEmitter {

    /**
     * Registers a handler for broadcast events of the given type.
     *
     * @param type the event type
     * @param handler the handler
     * @param <E> the event type
     */
    <E extends ElementEvent> void on(Class<E> type, ElementEventHandler<E> handler);

    /**
     * Registers a handler for events targeted at the given element ID.
     *
     * @param targetId the target element ID
     * @param type the event type
     * @param handler the handler
     * @param <E> the event type
     */
    <E extends ElementEvent> void on(String targetId, Class<E> type, ElementEventHandler<E> handler);

    /**
     * Returns a no-op event scope.
     *
     * @return a no-op event scope
     */
    static ElementEventScope noop() {
        return NoopScope.INSTANCE;
    }

    /**
     * No-op scope implementation.
     */
    final class NoopScope implements ElementEventScope {
        private static final NoopScope INSTANCE = new NoopScope();

        private NoopScope() {
        }

        @Override
        public <E extends ElementEvent> void on(Class<E> type, ElementEventHandler<E> handler) {
            // no-op
        }

        @Override
        public <E extends ElementEvent> void on(String targetId, Class<E> type, ElementEventHandler<E> handler) {
            // no-op
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
