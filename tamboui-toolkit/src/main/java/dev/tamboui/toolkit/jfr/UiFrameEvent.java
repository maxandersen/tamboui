/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.EventType;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Name("dev.tamboui.ui.frame")
@Label("UI Frame Snapshot")
@Description("Frame-level UI snapshot metadata")
@Category({ "TamboUI", "Toolkit", "UI" })
public final class UiFrameEvent extends Event {
    private static final EventType EVENT = EventType.getEventType(UiFrameEvent.class);

    @Label("Frame ID")
    long frameId;
    @Label("Screen Width")
    int screenWidth;
    @Label("Screen Height")
    int screenHeight;
    @Label("Focused Node ID")
    long focusedNodeId;
    @Label("Node Count")
    int nodeCount;

    private UiFrameEvent() {}

    public static boolean enabled() { return EVENT.isEnabled(); }

    public static void emit(long frameId, int screenWidth, int screenHeight, long focusedNodeId, int nodeCount) {
        UiFrameEvent ev = new UiFrameEvent();
        ev.frameId = frameId;
        ev.screenWidth = screenWidth;
        ev.screenHeight = screenHeight;
        ev.focusedNodeId = focusedNodeId;
        ev.nodeCount = nodeCount;
        ev.commit();
    }
}
