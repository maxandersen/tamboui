/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import java.util.Objects;

/**
 * Emitted when a TextAreaElement's text changes.
 */
public final class TextAreaChangedEvent implements ElementEvent {
    private final String sourceId;
    private final String text;

    /**
     * Creates a new text area changed event.
     *
     * @param sourceId the source element ID
     * @param text the updated text
     */
    public TextAreaChangedEvent(String sourceId, String text) {
        this.sourceId = sourceId;
        this.text = text != null ? text : "";
    }

    @Override
    public String sourceId() {
        return sourceId;
    }

    /**
     * Returns the updated text.
     *
     * @return the updated text
     */
    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return "TextAreaChangedEvent{sourceId=" + sourceId + ", text=" + text + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId, text);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TextAreaChangedEvent other = (TextAreaChangedEvent) obj;
        return Objects.equals(sourceId, other.sourceId)
            && Objects.equals(text, other.text);
    }
}
