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

@Name("dev.tamboui.ui.node")
@Label("UI Node Snapshot")
@Description("Element tree node and layout rectangle snapshot")
@Category({ "TamboUI", "Toolkit", "UI" })
public final class UiNodeEvent extends Event {
    private static final EventType EVENT = EventType.getEventType(UiNodeEvent.class);

    @Label("Frame ID") long frameId;
    @Label("Node ID") long nodeId;
    @Label("Parent ID") long parentId;
    @Label("Index In Parent") int indexInParent;
    @Label("Type") String type;
    @Label("Element ID") String id;
    @Label("Classes") String classes;
    @Label("Flags") String flags;
    @Label("Z Index") int zIndex;
    @Label("Clipped") boolean clipped;
    @Label("X") int x;
    @Label("Y") int y;
    @Label("Width") int width;
    @Label("Height") int height;

    private UiNodeEvent() {}

    public static boolean enabled() { return EVENT.isEnabled(); }

    public static void emit(long frameId, long nodeId, long parentId, int indexInParent,
            String type, String id, String classes, String flags, int zIndex, boolean clipped,
            int x, int y, int width, int height) {
        UiNodeEvent ev = new UiNodeEvent();
        ev.frameId = frameId;
        ev.nodeId = nodeId;
        ev.parentId = parentId;
        ev.indexInParent = indexInParent;
        ev.type = type;
        ev.id = id;
        ev.classes = classes;
        ev.flags = flags;
        ev.zIndex = zIndex;
        ev.clipped = clipped;
        ev.x = x;
        ev.y = y;
        ev.width = width;
        ev.height = height;
        ev.commit();
    }
}
