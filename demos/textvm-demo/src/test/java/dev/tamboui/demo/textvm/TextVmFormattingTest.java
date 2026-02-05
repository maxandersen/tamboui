/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.textvm;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextVmFormattingTest {

    @Test
    void shortNameHandlesJarPaths() {
        assertThat(TextVmFormatting.shortName("/opt/apps/textvm.jar")).isEqualTo("textvm.jar");
    }

    @Test
    void shortNameHandlesClassNames() {
        assertThat(TextVmFormatting.shortName("com.example.Main")).isEqualTo("Main");
    }

    @Test
    void shortNameHandlesBlankValues() {
        assertThat(TextVmFormatting.shortName(" ")).isEqualTo("Unknown");
    }

    @Test
    void findMainCandidatePrefersJarArguments() {
        String[] args = {"-Xmx1g", "-jar", "/srv/app.jar", "--mode", "test"};
        assertThat(TextVmFormatting.findMainCandidate(args, "")).isEqualTo("/srv/app.jar");
    }

    @Test
    void formatBytesUsesUnits() {
        assertThat(TextVmFormatting.formatBytes(0)).isEqualTo("0 B");
        assertThat(TextVmFormatting.formatBytes(1024)).isEqualTo("1.0 KB");
    }

    @Test
    void formatDurationFormatsHoursMinutesSeconds() {
        assertThat(TextVmFormatting.formatDuration(Duration.ofSeconds(65))).isEqualTo("1m 5s");
        assertThat(TextVmFormatting.formatDuration(Duration.ofSeconds(3661))).isEqualTo("1h 1m 1s");
    }

    @Test
    void formatPercentHandlesUnknown() {
        assertThat(TextVmFormatting.formatPercent(-1)).isEqualTo("n/a");
        assertThat(TextVmFormatting.formatPercent(0.42)).isEqualTo("42%");
    }
}
