/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit;

import dev.tamboui.tui.RenderThread;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

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
