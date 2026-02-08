/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

/**
 * Marker interface for typed element-to-element events.
 * <p>
 * Element events are used for application-specific communication between
 * elements (and the application) without relying on stringly-typed channels.
 */
public interface ElementEvent {

    /**
     * Returns the source element ID, or null if the event was emitted by the application.
     *
     * @return the source element ID, or null for application-originated events
     */
    String sourceId();

    /**
     * Returns the target element ID for directed delivery, or null for broadcast.
     * <p>
     * When null, the event is considered a broadcast and will only be delivered
     * to broadcast handlers.
     *
     * @return the target element ID, or null for broadcast
     */
    default String targetId() {
        return null;
    }
}
