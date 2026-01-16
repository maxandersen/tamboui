/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text.markup;

/**
 * Thrown when Rich / BBCode-style markup cannot be parsed.
 */
public final class MarkupParseException extends RuntimeException {

    public MarkupParseException(String message) {
        super(message);
    }
}


