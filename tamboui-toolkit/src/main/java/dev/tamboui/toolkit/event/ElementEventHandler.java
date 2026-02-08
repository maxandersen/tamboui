/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

/**
 * Handles a typed element event.
 *
 * @param <E> the event type
 */
@FunctionalInterface
public interface ElementEventHandler<E extends ElementEvent> {

    /**
     * Handles the event.
     *
     * @param event the event
     * @return HANDLED if the event was handled, UNHANDLED otherwise
     */
    EventResult handle(E event);
}
