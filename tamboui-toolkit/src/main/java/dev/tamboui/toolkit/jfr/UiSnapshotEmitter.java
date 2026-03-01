/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.jfr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dev.tamboui.layout.Rect;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.focus.FocusManager;

public final class UiSnapshotEmitter {

    private UiSnapshotEmitter() {
    }

    public static void emit(long frameId, Rect screen, ElementRegistry elementRegistry, FocusManager focusManager) {
        if (!UiFrameEvent.enabled() && !UiNodeEvent.enabled()) {
            return;
        }

        List<ElementRegistry.ElementInfo> all = elementRegistry.queryAll("*");

        Map<ElementRegistry.ElementInfo, Long> nodeIds = new HashMap<>();
        for (int i = 0; i < all.size(); i++) {
            nodeIds.put(all.get(i), (long) (i + 1));
        }

        long focusedNodeId = -1;
        String focusedElementId = focusManager.focusedId();
        if (focusedElementId != null) {
            for (ElementRegistry.ElementInfo info : all) {
                if (focusedElementId.equals(info.id())) {
                    focusedNodeId = nodeIds.getOrDefault(info, -1L);
                    break;
                }
            }
        }

        if (UiFrameEvent.enabled()) {
            UiFrameEvent.emit(frameId, screen.width(), screen.height(), focusedNodeId, all.size());
        }

        if (!UiNodeEvent.enabled()) {
            return;
        }

        Map<Long, Integer> siblingIndexCounter = new HashMap<>();
        for (ElementRegistry.ElementInfo info : all) {
            long nodeId = nodeIds.getOrDefault(info, -1L);
            long parentId = info.parent() == null ? -1L : nodeIds.getOrDefault(info.parent(), -1L);
            int idx = siblingIndexCounter.getOrDefault(parentId, 0);
            siblingIndexCounter.put(parentId, idx + 1);

            String classes = join(info.cssClasses());
            String flags = flags(info, focusedElementId);
            Rect a = info.area();

            UiNodeEvent.emit(frameId, nodeId, parentId, idx,
                    info.type(), info.id(), classes, flags, 0, false,
                    a.x(), a.y(), a.width(), a.height());
        }
    }

    private static String join(Set<String> classes) {
        if (classes == null || classes.isEmpty()) {
            return "";
        }
        return classes.stream().sorted().collect(Collectors.joining(" "));
    }

    private static String flags(ElementRegistry.ElementInfo info, String focusedElementId) {
        StringBuilder sb = new StringBuilder("visible");
        if (info.id() != null && info.id().equals(focusedElementId)) {
            sb.append(",focused");
        }
        return sb.toString();
    }
}
