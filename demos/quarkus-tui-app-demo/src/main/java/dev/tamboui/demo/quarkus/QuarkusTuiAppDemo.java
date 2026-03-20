//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.quarkus;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.elements.ListElement;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.KeyEventHandler;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.widgets.form.BooleanFieldState;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.SelectFieldState;
import dev.tamboui.widgets.input.TextInputState;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * code.quarkus.io-style project generator in a terminal UI.
 * <p>
 * This demo lets you:
 * <ul>
 *   <li>Configure project coordinates and generation options</li>
 *   <li>Search and toggle Quarkus extensions</li>
 *   <li>See generated CLI/Maven commands and a downloadable URL</li>
 * </ul>
 */
public final class QuarkusTuiAppDemo {

    private static final int LABEL_WIDTH = 12;
    private static final int EXTENSION_PAGE_STEP = 8;
    private static final String DEFAULT_GROUP_ID = "org.acme";
    private static final String DEFAULT_ARTIFACT_ID = "code-with-quarkus";
    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    private static final String DEFAULT_PACKAGE_NAME = "org.acme";
    private static final String DEFAULT_APP_NAME = "code-with-quarkus";
    private static final String DEFAULT_QUARKUS_VERSION = "3.28.0";

    private static final List<String> BUILD_TOOLS = List.of(
            "Maven",
            "Gradle Kotlin DSL",
            "Gradle Groovy DSL"
    );

    private static final List<String> LANGUAGES = List.of(
            "Java",
            "Kotlin",
            "Scala"
    );

    private static final List<String> JAVA_VERSIONS = List.of(
            "17",
            "21",
            "23"
    );

    private static final List<ExtensionOption> EXTENSION_CATALOG = List.of(
            new ExtensionOption("resteasy-reactive", "RESTEasy Reactive", "Build REST APIs quickly with reactive endpoints.", "Web"),
            new ExtensionOption("smallrye-openapi", "SmallRye OpenAPI", "Generate OpenAPI/Swagger docs for your endpoints.", "Web"),
            new ExtensionOption("websockets", "WebSockets", "Real-time bidirectional communication over WebSockets.", "Web"),
            new ExtensionOption("grpc", "gRPC", "Build high-performance RPC services with protobuf.", "Web"),
            new ExtensionOption("hibernate-orm-panache", "Hibernate ORM with Panache", "Active record style persistence with JPA/Hibernate.", "Data"),
            new ExtensionOption("jdbc-postgresql", "JDBC PostgreSQL", "Connect Quarkus to PostgreSQL databases.", "Data"),
            new ExtensionOption("jdbc-mysql", "JDBC MySQL", "Connect Quarkus to MySQL or MariaDB databases.", "Data"),
            new ExtensionOption("flyway", "Flyway", "Versioned schema migrations for relational databases.", "Data"),
            new ExtensionOption("liquibase", "Liquibase", "Database change management and schema migrations.", "Data"),
            new ExtensionOption("redis-client", "Redis Client", "Use Redis for caching, pub/sub, and state storage.", "Data"),
            new ExtensionOption("smallrye-health", "SmallRye Health", "Liveness and readiness health checks.", "Observability"),
            new ExtensionOption("micrometer", "Micrometer", "Application metrics with Prometheus and other registries.", "Observability"),
            new ExtensionOption("opentelemetry", "OpenTelemetry", "Distributed tracing and telemetry instrumentation.", "Observability"),
            new ExtensionOption("scheduler", "Scheduler", "Run periodic jobs and cron-like tasks.", "Core"),
            new ExtensionOption("cache", "Cache", "In-memory and provider-backed method caching.", "Core"),
            new ExtensionOption("hibernate-validator", "Hibernate Validator", "Bean validation with Jakarta Validation API.", "Core"),
            new ExtensionOption("messaging-kafka", "Messaging Kafka", "Reactive messaging with Apache Kafka.", "Messaging"),
            new ExtensionOption("amqp", "AMQP", "Reactive messaging with AMQP brokers.", "Messaging"),
            new ExtensionOption("oidc", "OpenID Connect", "Authentication and authorization with OIDC providers.", "Security"),
            new ExtensionOption("security-jpa", "Security JPA", "JPA-backed user stores for security.", "Security")
    );

    private final TextInputState groupIdState = new TextInputState(DEFAULT_GROUP_ID);
    private final TextInputState artifactIdState = new TextInputState(DEFAULT_ARTIFACT_ID);
    private final TextInputState versionState = new TextInputState(DEFAULT_VERSION);
    private final TextInputState packageNameState = new TextInputState(DEFAULT_PACKAGE_NAME);
    private final TextInputState appNameState = new TextInputState(DEFAULT_APP_NAME);
    private final TextInputState quarkusVersionState = new TextInputState(DEFAULT_QUARKUS_VERSION);
    private final TextInputState extensionFilterState = new TextInputState();

    private final SelectFieldState buildToolState = new SelectFieldState(BUILD_TOOLS, 0);
    private final SelectFieldState languageState = new SelectFieldState(LANGUAGES, 0);
    private final SelectFieldState javaVersionState = new SelectFieldState(JAVA_VERSIONS, 1);
    private final BooleanFieldState noCodeState = new BooleanFieldState(false);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final Set<String> selectedExtensionIds = new LinkedHashSet<>();
    private int extensionCursorIndex = 0;
    private GeneratedOutput generatedOutput;
    private String statusMessage = "Ready. Edit fields, press Ctrl+G to generate, Ctrl+D to download.";

    /**
     * Demo entry point.
     *
     * @param args command line arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new QuarkusTuiAppDemo().run();
    }

    private QuarkusTuiAppDemo() {
        selectedExtensionIds.add("resteasy-reactive");
        selectedExtensionIds.add("smallrye-openapi");
        selectedExtensionIds.add("smallrye-health");
        generatedOutput = createGeneratedOutput();
    }

    private void run() throws Exception {
        TuiConfig config = TuiConfig.builder()
                .mouseCapture(true)
                .tickRate(Duration.ofMillis(100))
                .build();
        try (ToolkitRunner runner = ToolkitRunner.create(config)) {
            runner.run(this::render);
        }
    }

    private Element render() {
        List<ExtensionOption> filteredExtensions = filteredExtensions();
        clampExtensionCursor(filteredExtensions.size());

        Element projectPanel = panel("Project setup", column(
                formField("Group", groupIdState)
                        .id("group-id")
                        .labelWidth(LABEL_WIDTH),
                formField("Artifact", artifactIdState)
                        .id("artifact-id")
                        .labelWidth(LABEL_WIDTH),
                formField("Version", versionState)
                        .id("version")
                        .labelWidth(LABEL_WIDTH),
                formField("Package", packageNameState)
                        .id("package")
                        .labelWidth(LABEL_WIDTH),
                formField("Name", appNameState)
                        .id("app-name")
                        .labelWidth(LABEL_WIDTH),
                formField("Quarkus", quarkusVersionState)
                        .id("quarkus-version")
                        .labelWidth(LABEL_WIDTH),
                formField("Build tool", buildToolState)
                        .id("build-tool")
                        .labelWidth(LABEL_WIDTH),
                formField("Language", languageState)
                        .id("language")
                        .labelWidth(LABEL_WIDTH),
                formField("Java", javaVersionState)
                        .id("java-version")
                        .labelWidth(LABEL_WIDTH),
                formField("No code", noCodeState, FieldType.CHECKBOX)
                        .id("no-code")
                        .labelWidth(LABEL_WIDTH)
                        .checkedColor(Color.GREEN)
        ).spacing(0)).rounded().borderColor(Color.CYAN).constraint(Constraint.percentage(40));

        KeyEventHandler extensionListHandler = this::handleExtensionListKey;
        var extensionList = list()
                .data(filteredExtensions, this::renderExtensionItem)
                .selected(extensionCursorIndex)
                .autoScroll()
                .highlightSymbol(">> ")
                .highlightStyle(Style.EMPTY.bg(Color.CYAN).fg(Color.BLACK))
                .scrollbar(ListElement.ScrollBarPolicy.AS_NEEDED)
                .id("extension-list")
                .focusable()
                .onKeyEvent(extensionListHandler)
                .onMouseEvent(this::handleExtensionListMouse)
                .constraint(Constraint.fill());

        Element extensionPanel = panel(
                "Extensions (" + selectedExtensionIds.size() + " selected)",
                column(
                        formField("Filter", extensionFilterState)
                                .id("extension-filter")
                                .labelWidth(8)
                                .rounded()
                                .borderColor(Color.DARK_GRAY)
                                .focusedBorderColor(Color.CYAN)
                                .constraint(Constraint.length(3)),
                        extensionList,
                        extensionHint(filteredExtensions)
                ).spacing(1)
        ).rounded().borderColor(Color.MAGENTA).constraint(Constraint.fill(2));

        List<String> outputLines = generatedOutput.lines();
        Element outputPanel = panel("Generated recipe", list()
                .data(outputLines, line -> text(line))
                .displayOnly()
                .scrollbar(ListElement.ScrollBarPolicy.AS_NEEDED)
                .constraint(Constraint.fill())
        ).rounded().borderColor(Color.GREEN).constraint(Constraint.fill(2));

        Element rightSide = column(
                extensionPanel,
                outputPanel
        ).spacing(1).constraint(Constraint.fill());

        return column(
                header().constraint(Constraint.length(3)),
                row(projectPanel, rightSide).spacing(1).constraint(Constraint.fill()),
                footer().constraint(Constraint.length(2))
        ).spacing(1)
         .fill()
         .id("root")
         .onKeyEvent(this::handleGlobalKey);
    }

    private Panel header() {
        return panel(
                row(
                        text(" Quarkus Project Generator ").bold().cyan(),
                        spacer(),
                        text("code.quarkus.io in your terminal").dim()
                ).constraint(Constraint.length(1))
        ).rounded().borderColor(Color.DARK_GRAY);
    }

    private Panel footer() {
        return panel(
                row(
                        text("Tab").bold(),
                        text(": focus  ").dim(),
                        text("Space/Enter").bold(),
                        text(": toggle extension  ").dim(),
                        text("Ctrl+G").bold(),
                        text(": generate  ").dim(),
                        text("Ctrl+D").bold(),
                        text(": download zip  ").dim(),
                        text("Ctrl+A").bold(),
                        text(": toggle visible  ").dim(),
                        text("Ctrl+R").bold(),
                        text(": reset defaults").dim(),
                        spacer(),
                        text(statusMessage).fg(Color.GREEN)
                )
        ).rounded().borderColor(Color.DARK_GRAY);
    }

    private StyledElement<?> extensionHint(List<ExtensionOption> filteredExtensions) {
        if (filteredExtensions.isEmpty()) {
            return column(
                    text("No extension matches the current filter.").fg(Color.YELLOW),
                    text("Try searching by id, name, or category.").dim()
            ).spacing(1);
        }

        ExtensionOption focused = filteredExtensions.get(extensionCursorIndex);
        boolean selected = selectedExtensionIds.contains(focused.id());
        return column(
                row(
                        text(selected ? "enabled" : "disabled")
                                .fg(selected ? Color.GREEN : Color.YELLOW)
                                .bold(),
                        spacer(),
                        text(focused.category()).dim()
                ),
                text(focused.name()).bold(),
                text(focused.id()).dim(),
                text(focused.description())
        ).spacing(0);
    }

    private EventResult handleGlobalKey(KeyEvent event) {
        if (event.code() != KeyCode.CHAR || !event.hasCtrl()) {
            return EventResult.UNHANDLED;
        }
        char command = Character.toLowerCase(event.character());
        if (command == 'g') {
            generateFromCurrentInputs();
            return EventResult.HANDLED;
        }
        if (command == 'd') {
            downloadGeneratedProjectArchive();
            return EventResult.HANDLED;
        }
        if (command == 'r') {
            resetToDefaults();
            return EventResult.HANDLED;
        }
        if (command == 'a') {
            toggleAllVisible();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleExtensionListKey(KeyEvent event) {
        List<ExtensionOption> filteredExtensions = filteredExtensions();
        clampExtensionCursor(filteredExtensions.size());

        if (filteredExtensions.isEmpty()) {
            return EventResult.HANDLED;
        }
        if (event.isUp()) {
            extensionCursorIndex = Math.max(0, extensionCursorIndex - 1);
            return EventResult.HANDLED;
        }
        if (event.isDown()) {
            extensionCursorIndex = Math.min(filteredExtensions.size() - 1, extensionCursorIndex + 1);
            return EventResult.HANDLED;
        }
        if (event.isPageUp()) {
            extensionCursorIndex = Math.max(0, extensionCursorIndex - EXTENSION_PAGE_STEP);
            return EventResult.HANDLED;
        }
        if (event.isPageDown()) {
            extensionCursorIndex = Math.min(filteredExtensions.size() - 1, extensionCursorIndex + EXTENSION_PAGE_STEP);
            return EventResult.HANDLED;
        }
        if (event.isHome()) {
            extensionCursorIndex = 0;
            return EventResult.HANDLED;
        }
        if (event.isEnd()) {
            extensionCursorIndex = filteredExtensions.size() - 1;
            return EventResult.HANDLED;
        }
        if (event.isSelect() || event.isConfirm()) {
            toggleCurrentExtension(filteredExtensions);
            return EventResult.HANDLED;
        }
        if (event.code() == KeyCode.CHAR && event.hasCtrl() && Character.toLowerCase(event.character()) == 'a') {
            toggleAllVisible();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private EventResult handleExtensionListMouse(MouseEvent event) {
        if (!event.isClick()) {
            return EventResult.UNHANDLED;
        }
        List<ExtensionOption> filteredExtensions = filteredExtensions();
        if (filteredExtensions.isEmpty()) {
            return EventResult.HANDLED;
        }
        toggleCurrentExtension(filteredExtensions);
        return EventResult.HANDLED;
    }

    private void generateFromCurrentInputs() {
        generatedOutput = createGeneratedOutput();
        statusMessage = "Generated project recipe for " + generatedOutput.artifactId() + ".";
    }

    private void downloadGeneratedProjectArchive() {
        if (generatedOutput == null) {
            generateFromCurrentInputs();
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(generatedOutput.downloadUrl()))
                .header("Accept", "application/zip")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                statusMessage = "Download failed: code.quarkus.io returned HTTP " + response.statusCode() + ".";
                return;
            }
            byte[] archiveBytes = response.body();
            if (archiveBytes == null || archiveBytes.length == 0) {
                statusMessage = "Download failed: received an empty archive.";
                return;
            }
            Path outputPath = uniqueArchivePath(generatedOutput.artifactId());
            Files.write(outputPath, archiveBytes);
            statusMessage = "Downloaded archive: " + outputPath.toAbsolutePath();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            statusMessage = "Download interrupted.";
        } catch (IOException e) {
            statusMessage = "Download failed: " + e.getMessage();
        }
    }

    private Path uniqueArchivePath(String artifactId) {
        String sanitizedArtifact = artifactId.replaceAll("[^A-Za-z0-9._-]", "-");
        String baseName = sanitizedArtifact + "-quarkus-project";
        Path candidate = Path.of(baseName + ".zip");
        int index = 1;
        while (Files.exists(candidate)) {
            candidate = Path.of(baseName + "-" + index + ".zip");
            index++;
        }
        return candidate;
    }

    private void toggleCurrentExtension(List<ExtensionOption> filteredExtensions) {
        ExtensionOption option = filteredExtensions.get(extensionCursorIndex);
        if (selectedExtensionIds.contains(option.id())) {
            selectedExtensionIds.remove(option.id());
            statusMessage = "Removed extension: " + option.id();
        } else {
            selectedExtensionIds.add(option.id());
            statusMessage = "Added extension: " + option.id();
        }
    }

    private void toggleAllVisible() {
        List<ExtensionOption> filteredExtensions = filteredExtensions();
        if (filteredExtensions.isEmpty()) {
            statusMessage = "Nothing to toggle: extension filter returned no results.";
            return;
        }

        boolean hasUnselected = false;
        for (ExtensionOption option : filteredExtensions) {
            if (!selectedExtensionIds.contains(option.id())) {
                hasUnselected = true;
                break;
            }
        }

        if (hasUnselected) {
            for (ExtensionOption option : filteredExtensions) {
                selectedExtensionIds.add(option.id());
            }
            statusMessage = "Enabled all visible extensions (" + filteredExtensions.size() + ").";
        } else {
            for (ExtensionOption option : filteredExtensions) {
                selectedExtensionIds.remove(option.id());
            }
            statusMessage = "Disabled all visible extensions (" + filteredExtensions.size() + ").";
        }
    }

    private void resetToDefaults() {
        setText(groupIdState, DEFAULT_GROUP_ID);
        setText(artifactIdState, DEFAULT_ARTIFACT_ID);
        setText(versionState, DEFAULT_VERSION);
        setText(packageNameState, DEFAULT_PACKAGE_NAME);
        setText(appNameState, DEFAULT_APP_NAME);
        setText(quarkusVersionState, DEFAULT_QUARKUS_VERSION);
        setText(extensionFilterState, "");

        buildToolState.selectIndex(0);
        languageState.selectIndex(0);
        javaVersionState.selectIndex(1);
        noCodeState.setValue(false);

        selectedExtensionIds.clear();
        selectedExtensionIds.add("resteasy-reactive");
        selectedExtensionIds.add("smallrye-openapi");
        selectedExtensionIds.add("smallrye-health");

        extensionCursorIndex = 0;
        generatedOutput = createGeneratedOutput();
        statusMessage = "Reset defaults and regenerated project recipe.";
    }

    private void setText(TextInputState state, String value) {
        state.setText(value);
        state.moveCursorToEnd();
    }

    private List<ExtensionOption> filteredExtensions() {
        String filter = extensionFilterState.text().trim().toLowerCase(Locale.ROOT);
        if (filter.isEmpty()) {
            return EXTENSION_CATALOG;
        }
        List<ExtensionOption> filtered = new ArrayList<>();
        for (ExtensionOption option : EXTENSION_CATALOG) {
            if (option.searchableText().contains(filter)) {
                filtered.add(option);
            }
        }
        return filtered;
    }

    private void clampExtensionCursor(int extensionCount) {
        if (extensionCount <= 0) {
            extensionCursorIndex = 0;
            return;
        }
        if (extensionCursorIndex < 0) {
            extensionCursorIndex = 0;
        }
        if (extensionCursorIndex >= extensionCount) {
            extensionCursorIndex = extensionCount - 1;
        }
    }

    private StyledElement<?> renderExtensionItem(ExtensionOption extension) {
        boolean selected = selectedExtensionIds.contains(extension.id());
        Style selectionStyle = selected
                ? Style.EMPTY.fg(Color.GREEN).bold()
                : Style.EMPTY.fg(Color.DARK_GRAY);
        return row(
                text(selected ? "[x]" : "[ ]").style(selectionStyle),
                text(extension.id()).bold(),
                text("[" + extension.category() + "]").dim()
        ).spacing(1);
    }

    private GeneratedOutput createGeneratedOutput() {
        List<String> selectedIds = selectedExtensionIdsInCatalogOrder();
        String extensionCsv = String.join(",", selectedIds);
        BuildTool buildTool = BuildTool.fromLabel(buildToolState.selectedValue());
        ProjectLanguage language = ProjectLanguage.fromLabel(languageState.selectedValue());

        List<String> lines = new ArrayList<>();
        lines.add("Quarkus CLI");
        lines.add("quarkus create app " + coordinateString());
        lines.add("  --name=\"" + appName() + "\"");
        lines.add("  --package-name=" + packageName());
        lines.add("  --java=" + javaVersionState.selectedValue());
        lines.add("  --build-tool=" + buildTool.cliValue());
        lines.add("  --language=" + language.cliValue());
        lines.add("  --quarkus-version=" + quarkusVersion());
        if (!extensionCsv.isEmpty()) {
            lines.add("  --extensions=\"" + extensionCsv + "\"");
        }
        if (noCodeState.value()) {
            lines.add("  --no-code");
        }

        lines.add("");
        lines.add("Maven plugin");
        lines.add("mvn io.quarkus.platform:quarkus-maven-plugin:" + quarkusVersion() + ":create");
        lines.add("  -DprojectGroupId=" + groupId());
        lines.add("  -DprojectArtifactId=" + artifactId());
        lines.add("  -DprojectVersion=" + projectVersion());
        lines.add("  -DpackageName=" + packageName());
        lines.add("  -DjavaVersion=" + javaVersionState.selectedValue());
        lines.add("  -DbuildTool=" + buildTool.queryValue());
        lines.add("  -Dlanguage=" + language.queryValue());
        if (!extensionCsv.isEmpty()) {
            lines.add("  -Dextensions=\"" + extensionCsv + "\"");
        }
        if (noCodeState.value()) {
            lines.add("  -DnoCode=true");
        }

        lines.add("");
        lines.add("Download URL");
        String downloadUrl = downloadUrl(selectedIds, buildTool, language);
        lines.add(downloadUrl);
        lines.add("");
        lines.add("Selected extensions (" + selectedIds.size() + "): " + summarizeExtensions(selectedIds));
        return new GeneratedOutput(lines, downloadUrl, artifactId());
    }

    private List<String> selectedExtensionIdsInCatalogOrder() {
        List<String> ordered = new ArrayList<>();
        for (ExtensionOption option : EXTENSION_CATALOG) {
            if (selectedExtensionIds.contains(option.id())) {
                ordered.add(option.id());
            }
        }
        return ordered;
    }

    private String summarizeExtensions(List<String> selectedIds) {
        if (selectedIds.isEmpty()) {
            return "(none)";
        }
        if (selectedIds.size() <= 5) {
            return String.join(", ", selectedIds);
        }
        List<String> firstFive = selectedIds.subList(0, 5);
        return String.join(", ", firstFive) + " ... +" + (selectedIds.size() - 5) + " more";
    }

    private String coordinateString() {
        return groupId() + ":" + artifactId() + ":" + projectVersion();
    }

    private String downloadUrl(List<String> extensionIds, BuildTool buildTool, ProjectLanguage language) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("g", groupId());
        params.put("a", artifactId());
        params.put("v", projectVersion());
        params.put("p", packageName());
        params.put("n", appName());
        params.put("b", buildTool.queryValue());
        params.put("l", language.queryValue());
        params.put("j", javaVersionState.selectedValue());
        params.put("q", quarkusVersion());
        if (!extensionIds.isEmpty()) {
            params.put("e", String.join(",", extensionIds));
        }
        if (noCodeState.value()) {
            params.put("noCode", "true");
        }

        StringBuilder query = new StringBuilder("https://code.quarkus.io/d?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                query.append('&');
            }
            query.append(urlEncode(entry.getKey()))
                    .append('=')
                    .append(urlEncode(entry.getValue()));
            first = false;
        }
        return query.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String groupId() {
        return normalized(groupIdState.text(), DEFAULT_GROUP_ID);
    }

    private String artifactId() {
        return normalized(artifactIdState.text(), DEFAULT_ARTIFACT_ID);
    }

    private String projectVersion() {
        return normalized(versionState.text(), DEFAULT_VERSION);
    }

    private String packageName() {
        return normalized(packageNameState.text(), DEFAULT_PACKAGE_NAME);
    }

    private String appName() {
        return normalized(appNameState.text(), DEFAULT_APP_NAME);
    }

    private String quarkusVersion() {
        return normalized(quarkusVersionState.text(), DEFAULT_QUARKUS_VERSION);
    }

    private String normalized(String rawValue, String fallback) {
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        return trimmed;
    }

    private record GeneratedOutput(List<String> lines, String downloadUrl, String artifactId) {
    }

    private enum BuildTool {
        MAVEN("Maven", "maven"),
        GRADLE_KOTLIN_DSL("Gradle Kotlin DSL", "gradle-kotlin-dsl"),
        GRADLE_GROOVY_DSL("Gradle Groovy DSL", "gradle");

        private final String label;
        private final String value;

        BuildTool(String label, String value) {
            this.label = label;
            this.value = value;
        }

        String queryValue() {
            return value;
        }

        String cliValue() {
            return value;
        }

        static BuildTool fromLabel(String label) {
            for (BuildTool tool : values()) {
                if (tool.label.equals(label)) {
                    return tool;
                }
            }
            return MAVEN;
        }
    }

    private enum ProjectLanguage {
        JAVA("Java", "java"),
        KOTLIN("Kotlin", "kotlin"),
        SCALA("Scala", "scala");

        private final String label;
        private final String value;

        ProjectLanguage(String label, String value) {
            this.label = label;
            this.value = value;
        }

        String queryValue() {
            return value;
        }

        String cliValue() {
            return value;
        }

        static ProjectLanguage fromLabel(String label) {
            for (ProjectLanguage language : values()) {
                if (language.label.equals(label)) {
                    return language;
                }
            }
            return JAVA;
        }
    }

    private record ExtensionOption(String id, String name, String description, String category) {

        String searchableText() {
            return (id + " " + name + " " + description + " " + category).toLowerCase(Locale.ROOT);
        }
    }
}
