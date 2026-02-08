/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Typed event bus for element-to-element communication.
 * <p>
 * The bus supports both broadcast handlers and target-specific handlers.
 * Broadcast handlers receive all events (including targeted ones).
 * Target-specific handlers only receive events directed at their target ID.
 */
public final class ElementEventBus implements ElementEventEmitter {

    /**
     * Subscription handle for globally registered handlers.
     */
    public interface Subscription {
        /**
         * Cancels this subscription.
         */
        void cancel();
    }

    private final List<HandlerRegistration<?>> globalHandlers = new ArrayList<>();
    private final List<HandlerRegistration<?>> frameHandlers = new ArrayList<>();
    private final ElementEventScope frameScope = new FrameScope();

    /**
     * Registers a global handler for broadcast events of the given type.
     * <p>
     * Global handlers persist until cancelled.
     *
     * @param type the event type
     * @param handler the handler
     * @param <E> the event type
     * @return a subscription handle
     */
    public <E extends ElementEvent> Subscription subscribe(Class<E> type, ElementEventHandler<E> handler) {
        return subscribe(null, type, handler);
    }

    /**
     * Registers a global handler for events targeted at the given element ID.
     * <p>
     * Global handlers persist until cancelled.
     *
     * @param targetId the target element ID
     * @param type the event type
     * @param handler the handler
     * @param <E> the event type
     * @return a subscription handle
     */
    public <E extends ElementEvent> Subscription subscribe(String targetId,
                                                           Class<E> type,
                                                           ElementEventHandler<E> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        HandlerRegistration<E> registration = new HandlerRegistration<>(targetId, type, handler);
        globalHandlers.add(registration);
        return () -> globalHandlers.remove(registration);
    }

    /**
     * Returns the frame-scoped event scope.
     *
     * @return the frame-scoped event scope
     */
    public ElementEventScope frameScope() {
        return frameScope;
    }

    /**
     * Clears all frame-scoped handlers.
     * Should be called at the start of each render cycle.
     */
    public void clearFrameHandlers() {
        frameHandlers.clear();
    }

    @Override
    public EventResult emit(ElementEvent event) {
        Objects.requireNonNull(event, "event");
        return dispatch(event, event.targetId());
    }

    @Override
    public EventResult emitTo(String targetId, ElementEvent event) {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(event, "event");
        return dispatch(event, targetId);
    }

    private EventResult dispatch(ElementEvent event, String targetId) {
        EventResult result = EventResult.UNHANDLED;
        result = result.or(dispatchToHandlers(frameHandlers, event, targetId));
        result = result.or(dispatchToHandlers(globalHandlers, event, targetId));
        return result;
    }

    private EventResult dispatchToHandlers(List<HandlerRegistration<?>> handlers,
                                           ElementEvent event,
                                           String targetId) {
        if (handlers.isEmpty()) {
            return EventResult.UNHANDLED;
        }
        EventResult result = EventResult.UNHANDLED;
        List<HandlerRegistration<?>> snapshot = new ArrayList<>(handlers);
        for (HandlerRegistration<?> registration : snapshot) {
            if (!registration.matches(targetId)) {
                continue;
            }
            result = result.or(registration.dispatch(event));
        }
        return result;
    }

    private <E extends ElementEvent> void registerFrame(String targetId,
                                                        Class<E> type,
                                                        ElementEventHandler<E> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        frameHandlers.add(new HandlerRegistration<>(targetId, type, handler));
    }

    private final class FrameScope implements ElementEventScope {
        @Override
        public <E extends ElementEvent> void on(Class<E> type, ElementEventHandler<E> handler) {
            registerFrame(null, type, handler);
        }

        @Override
        public <E extends ElementEvent> void on(String targetId,
                                                Class<E> type,
                                                ElementEventHandler<E> handler) {
            registerFrame(targetId, type, handler);
        }

        @Override
        public EventResult emit(ElementEvent event) {
            return ElementEventBus.this.emit(event);
        }

        @Override
        public EventResult emitTo(String targetId, ElementEvent event) {
            return ElementEventBus.this.emitTo(targetId, event);
        }
    }

    private static final class HandlerRegistration<E extends ElementEvent> {
        private final String targetId;
        private final Class<E> type;
        private final ElementEventHandler<E> handler;

        private HandlerRegistration(String targetId, Class<E> type, ElementEventHandler<E> handler) {
            this.targetId = targetId;
            this.type = type;
            this.handler = handler;
        }

        private boolean matches(String eventTargetId) {
            if (targetId == null) {
                return true;
            }
            return targetId.equals(eventTargetId);
        }

        private EventResult dispatch(ElementEvent event) {
            if (!type.isInstance(event)) {
                return EventResult.UNHANDLED;
            }
            return handler.handle(type.cast(event));
        }
    }
}
