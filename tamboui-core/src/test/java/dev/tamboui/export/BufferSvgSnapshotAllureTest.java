/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BufferSvgSnapshotAllureTest {

    @Test
    @DisplayName("Attaches SVG snapshot to Allure report")
    void attachesSvgSnapshotToAllureReport() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 14, 3));
        buffer.setString(0, 0, "Allure", Style.EMPTY.fg(Color.hex("#00bcd4")).bold());
        buffer.setString(0, 1, "SVG", Style.EMPTY.onBlue());
        buffer.setString(4, 1, "Snapshot", Style.EMPTY.italic());
        buffer.setString(0, 2, "Diff Ready", Style.EMPTY.underlined());

        String svg = BufferSvgExporter.exportSvg(buffer, new BufferSvgExporter.Options()
            .title("Allure SVG Snapshot")
            .uniqueId("allure-snapshot"));

        Allure.addAttachment("snapshot", "image/svg+xml", svg, ".svg");

        assertTrue(svg.contains("<svg"));
    }

}
