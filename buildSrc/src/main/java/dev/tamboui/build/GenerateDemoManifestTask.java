/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

/**
 * Task to generate a JSON manifest file listing all demos with their main classes.
 * This manifest is embedded in the fat jar and used by the DemoLauncher.
 */
@CacheableTask
public abstract class GenerateDemoManifestTask extends DefaultTask {

    /**
     * A demo entry containing all information needed for the manifest.
     */
    public static final class DemoEntry implements Serializable {
        private final String id;
        private final String displayName;
        private final String description;
        private final String module;
        private final String mainClass;
        private final Set<String> tags;
        private final String projectPath;

        public DemoEntry(String id, String displayName, String description, String module, String mainClass, Set<String> tags, String projectPath) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.module = module;
            this.mainClass = mainClass;
            this.tags = tags;
            this.projectPath = projectPath;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String getModule() {
            return module;
        }

        public String getMainClass() {
            return mainClass;
        }

        public Set<String> getTags() {
            return tags;
        }

        public String getProjectPath() {
            return projectPath;
        }
    }

    @Input
    public abstract ListProperty<DemoEntry> getDemos();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    public GenerateDemoManifestTask() {
        setGroup("build");
        setDescription("Generate JSON manifest file listing all demos with their main classes");
    }

    @TaskAction
    public void generate() throws IOException {
        List<DemoEntry> demos = getDemos().get();
        String json = toJson(demos);

        Files.createDirectories(getOutputFile().get().getAsFile().toPath().getParent());
        Files.writeString(getOutputFile().get().getAsFile().toPath(), json);
    }

    private static String toJson(List<DemoEntry> demos) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"demos\": [\n");

        boolean first = true;
        for (DemoEntry demo : demos) {
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            sb.append("    {\n");
            sb.append("      \"id\": ").append(jsonString(demo.getId())).append(",\n");
            sb.append("      \"projectPath\": ").append(jsonString(demo.getProjectPath())).append(",\n");
            sb.append("      \"displayName\": ").append(jsonString(demo.getDisplayName())).append(",\n");
            sb.append("      \"description\": ").append(jsonString(demo.getDescription())).append(",\n");
            sb.append("      \"module\": ").append(jsonString(demo.getModule())).append(",\n");
            sb.append("      \"mainClass\": ").append(jsonString(demo.getMainClass())).append(",\n");
            sb.append("      \"tags\": ");
            boolean firstTag = true;
            sb.append("\"");
            for (String tag : demo.getTags()) {
                if (!firstTag) {
                    sb.append(", ");
                }
                firstTag = false;
                sb.append(tag);
            }
            sb.append("\"\n");
            sb.append("    }");
        }

        sb.append("\"\n  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
