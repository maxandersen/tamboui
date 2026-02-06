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

    @Test
    void findMainCandidate() {
        String[] args = {"--add-modules=ALL-SYSTEM",
            "--add-opens",
            "java.base/java.util=ALL-UNNAMED",
            "--add-opens",
            "java.base/java.lang=ALL-UNNAMED",
            "--add-opens",
            "java.base/sun.nio.fs=ALL-UNNAMED",
            "-Declipse.application=org.eclipse.jdt.ls.core.id1",
            "-Dosgi.bundles.defaultStartLevel=4",
            "-Declipse.product=org.eclipse.jdt.ls.core.product",
            "-Djava.import.generatesMetadataFilesAtProjectRoot=false",
            "-DDetectVMInstallationsJob.disabled=true",
            "-Dfile.encoding=utf8",
            "-XX:+UseParallelGC",
            "-XX:GCTimeRatio=4",
            "-XX:AdaptiveSizePolicyWeight=90",
            "-Dsun.zip.disableMemoryMapping=true",
            "-Xmx2G",
            "-Xms100m",
            "-Xlog:disable",
            "-javaagent:/Users/max/.cursor/extensions/redhat.java-1.52.0-darwin-arm64/lombok/lombok-1.18.39-4050.jar",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:HeapDumpPath=/Users/max/Library/Application Support/Cursor/User/workspaceStorage/0d1c7f837137ce40f4",
            "-Daether.dependencyCollector.impl=bf",
            "-jar",
            "/Users/max/.cursor/extensions/redhat.java-1.52.0-darwin-arm64/server/plugins/org.eclipse.equinox.launcher_1.6.700.v20231214-2017.jar",
            "-configuration",
            "/Users/max/Library/Application Support/Cursor/User/globalStorage/redhat.java/1.52.0/config_mac|"
        };
        assertThat(TextVmFormatting.findMainCandidate(args, "")).isEqualTo("/Users/max/.cursor/extensions/redhat.java-1.52.0-darwin-arm64/server/plugins/org.eclipse.equinox.launcher_1.6.700.v20231214-2017.jar");
    }

    @Test
    void findMainCandidatejcmd() {
        String[] args = {"8883", "something.cool"
        };
        assertThat(TextVmFormatting.findMainCandidate(args, "/Users/bin/jcmd")).isEqualTo("jcmd");
    }
}
