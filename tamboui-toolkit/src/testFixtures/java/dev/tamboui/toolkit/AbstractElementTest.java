/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import dev.tamboui.tui.RenderThread;

public abstract class AbstractElementTest {

    @BeforeEach
    void setUpRenderThread() {
        RenderThread.markAsRenderThread();
    }

    @AfterEach
    void tearDownRenderThread() {
        RenderThread.clearRenderThread();
    }

}
