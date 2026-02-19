//DEPS dev.tamboui:tamboui-tui:LATEST
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//DEPS com.fasterxml.jackson.core:jackson-databind:2.21.0
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.jdkdb;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Interactive browser for Java SDK metadata exposed by jdkdb-data.
 * <p>
 * The demo loads metadata from:
 * <ul>
 *   <li>{@code /metadata/latest.json} for latest distribution builds</li>
 *   <li>{@code /metadata/all.json} for full history</li>
 * </ul>
 * Then provides keyboard-driven filtering, search, and details for each artifact.
 */
public final class JdkDbDataBrowserDemo {

    private static final String API_BASE_URL = "https://jbangdev.github.io/jdkdb-data";
    private static final int PAGE_STEP = 10;
    private static final int MAX_VISIBLE_RESULTS = 2_000;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private static final TypeReference<List<Artifact>> ARTIFACT_LIST_TYPE = new TypeReference<List<Artifact>>() {
    };

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final Comparator<Artifact> ARTIFACT_COMPARATOR =
            Comparator.comparing((Artifact a) -> normalizedSortValue(a.vendor()))
                    .thenComparing((Artifact a) -> normalizedSortValue(a.javaVersion()), Comparator.reverseOrder())
                    .thenComparing((Artifact a) -> normalizedSortValue(a.version()), Comparator.reverseOrder())
                    .thenComparing((Artifact a) -> normalizedSortValue(a.filename()));

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ListState listState;
    private final TextInputState searchInputState;

    private EndpointMode endpointMode;
    private boolean searchFocused;

    private List<Artifact> artifacts;
    private List<Artifact> visibleArtifacts;
    private List<ListItem> visibleItems;

    private List<String> vendorOptions;
    private List<String> osOptions;
    private List<String> imageTypeOptions;
    private List<String> jvmOptions;
    private List<String> releaseTypeOptions;

    private int vendorIndex;
    private int osIndex;
    private int imageTypeIndex;
    private int jvmIndex;
    private int releaseTypeIndex;

    private int totalMatchCount;
    private Instant lastLoadedAt;
    private String statusMessage;
    private String errorMessage;

    private JdkDbDataBrowserDemo() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.listState = new ListState();
        this.searchInputState = new TextInputState();

        this.endpointMode = EndpointMode.LATEST;
        this.searchFocused = false;
        this.artifacts = Collections.emptyList();
        this.visibleArtifacts = Collections.emptyList();
        this.visibleItems = Collections.emptyList();

        this.vendorOptions = defaultFilterOptions();
        this.osOptions = defaultFilterOptions();
        this.imageTypeOptions = defaultFilterOptions();
        this.jvmOptions = defaultFilterOptions();
        this.releaseTypeOptions = defaultFilterOptions();

        this.vendorIndex = 0;
        this.osIndex = 0;
        this.imageTypeIndex = 0;
        this.jvmIndex = 0;
        this.releaseTypeIndex = 0;

        this.totalMatchCount = 0;
        this.lastLoadedAt = null;
        this.statusMessage = "Loading metadata...";
        this.errorMessage = null;
    }

    /**
     * Demo entry point.
     *
     * @param args ignored
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new JdkDbDataBrowserDemo().run();
    }

    private void run() throws Exception {
        reloadData(endpointMode);

        TuiConfig config = TuiConfig.builder()
                .noTick()
                .build();

        try (TuiRunner runner = TuiRunner.create(config)) {
            runner.run(this::handleEvent, this::render);
        }
    }

    private boolean handleEvent(Event event, TuiRunner runner) {
        if (!(event instanceof KeyEvent)) {
            return false;
        }

        KeyEvent keyEvent = (KeyEvent) event;

        if (keyEvent.isQuit()) {
            runner.quit();
            return true;
        }

        if (keyEvent.isFocusNext() || keyEvent.isFocusPrevious()) {
            searchFocused = !searchFocused;
            return true;
        }

        if (searchFocused) {
            if (keyEvent.isCancel() || keyEvent.isConfirm()) {
                searchFocused = false;
                return true;
            }
            if (handleTextInputKey(searchInputState, keyEvent)) {
                applyFilters();
                return true;
            }
            return false;
        }

        if (isPlainChar(keyEvent, '/')) {
            searchFocused = true;
            return true;
        }
        if (isPlainChar(keyEvent, 'f')) {
            reloadData(endpointMode);
            return true;
        }
        if (isPlainChar(keyEvent, 'l')) {
            reloadData(EndpointMode.LATEST);
            return true;
        }
        if (isPlainChar(keyEvent, 'a')) {
            reloadData(EndpointMode.ALL);
            return true;
        }
        if (isPlainChar(keyEvent, 'x')) {
            resetFilters();
            applyFilters();
            return true;
        }
        if (isPlainChar(keyEvent, 'c')) {
            if (!searchInputState.text().isEmpty()) {
                searchInputState.clear();
                applyFilters();
            }
            return true;
        }

        if (handleFilterCycleKey(keyEvent)) {
            return true;
        }

        if (keyEvent.isUp()) {
            listState.selectPrevious();
            return true;
        }
        if (keyEvent.isDown()) {
            listState.selectNext(visibleArtifacts.size());
            return true;
        }
        if (keyEvent.isPageUp()) {
            moveSelectionBy(-PAGE_STEP);
            return true;
        }
        if (keyEvent.isPageDown()) {
            moveSelectionBy(PAGE_STEP);
            return true;
        }
        if (keyEvent.isHome()) {
            if (!visibleArtifacts.isEmpty()) {
                listState.selectFirst();
            }
            return true;
        }
        if (keyEvent.isEnd()) {
            listState.selectLast(visibleArtifacts.size());
            return true;
        }

        return false;
    }

    private boolean handleFilterCycleKey(KeyEvent keyEvent) {
        if (keyEvent.code() != KeyCode.CHAR || keyEvent.hasAlt() || keyEvent.hasCtrl()) {
            return false;
        }

        char c = keyEvent.character();
        boolean backwards = Character.isUpperCase(c);
        switch (Character.toLowerCase(c)) {
            case 'v':
                vendorIndex = cycleIndex(vendorIndex, vendorOptions.size(), backwards);
                applyFilters();
                return true;
            case 'o':
                osIndex = cycleIndex(osIndex, osOptions.size(), backwards);
                applyFilters();
                return true;
            case 'i':
                imageTypeIndex = cycleIndex(imageTypeIndex, imageTypeOptions.size(), backwards);
                applyFilters();
                return true;
            case 'j':
                jvmIndex = cycleIndex(jvmIndex, jvmOptions.size(), backwards);
                applyFilters();
                return true;
            case 'r':
                releaseTypeIndex = cycleIndex(releaseTypeIndex, releaseTypeOptions.size(), backwards);
                applyFilters();
                return true;
            default:
                return false;
        }
    }

    private void moveSelectionBy(int delta) {
        if (visibleArtifacts.isEmpty()) {
            listState.select(null);
            return;
        }

        Integer current = listState.selected();
        int base = current == null ? 0 : current;
        int target = clamp(base + delta, 0, visibleArtifacts.size() - 1);
        listState.select(target);
    }

    private void reloadData(EndpointMode targetMode) {
        String endpointUrl = API_BASE_URL + targetMode.path();

        try {
            List<Artifact> loaded = fetchArtifacts(endpointUrl);
            loaded.sort(ARTIFACT_COMPARATOR);

            artifacts = loaded;
            endpointMode = targetMode;
            lastLoadedAt = Instant.now();
            errorMessage = null;
            statusMessage = String.format(
                    Locale.ROOT,
                    "Loaded %,d artifacts from %s",
                    artifacts.size(),
                    endpointMode.path()
            );

            rebuildFilterOptions();
            applyFilters();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorMessage = "Request interrupted while loading " + targetMode.path();
            statusMessage = errorMessage;
        } catch (IOException e) {
            errorMessage = "Failed to load " + targetMode.path() + ": " + e.getMessage();
            statusMessage = errorMessage;
            if (artifacts.isEmpty()) {
                visibleArtifacts = Collections.emptyList();
                visibleItems = Collections.emptyList();
                totalMatchCount = 0;
                listState.select(null);
            }
        }
    }

    private List<Artifact> fetchArtifacts(String endpointUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpointUrl))
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " from " + endpointUrl);
        }

        try (InputStream body = response.body()) {
            List<Artifact> loaded = objectMapper.readValue(body, ARTIFACT_LIST_TYPE);
            return loaded == null ? Collections.emptyList() : loaded;
        }
    }

    private void rebuildFilterOptions() {
        String selectedVendor = selectedOption(vendorOptions, vendorIndex);
        String selectedOs = selectedOption(osOptions, osIndex);
        String selectedImageType = selectedOption(imageTypeOptions, imageTypeIndex);
        String selectedJvm = selectedOption(jvmOptions, jvmIndex);
        String selectedReleaseType = selectedOption(releaseTypeOptions, releaseTypeIndex);

        vendorOptions = buildFilterOptions(artifacts, Artifact::vendor);
        osOptions = buildFilterOptions(artifacts, Artifact::os);
        imageTypeOptions = buildFilterOptions(artifacts, Artifact::imageType);
        jvmOptions = buildFilterOptions(artifacts, Artifact::jvmImpl);
        releaseTypeOptions = buildFilterOptions(artifacts, Artifact::releaseType);

        vendorIndex = findOptionIndex(vendorOptions, selectedVendor);
        osIndex = findOptionIndex(osOptions, selectedOs);
        imageTypeIndex = findOptionIndex(imageTypeOptions, selectedImageType);
        jvmIndex = findOptionIndex(jvmOptions, selectedJvm);
        releaseTypeIndex = findOptionIndex(releaseTypeOptions, selectedReleaseType);
    }

    private static List<String> buildFilterOptions(List<Artifact> source, Function<Artifact, String> extractor) {
        SortedSet<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Artifact artifact : source) {
            String value = normalizedValue(extractor.apply(artifact));
            if (!"-".equals(value)) {
                values.add(value);
            }
        }
        List<String> options = new ArrayList<>(values.size() + 1);
        options.add("all");
        options.addAll(values);
        return options;
    }

    private void applyFilters() {
        String selectedVendor = selectedOption(vendorOptions, vendorIndex);
        String selectedOs = selectedOption(osOptions, osIndex);
        String selectedImageType = selectedOption(imageTypeOptions, imageTypeIndex);
        String selectedJvm = selectedOption(jvmOptions, jvmIndex);
        String selectedReleaseType = selectedOption(releaseTypeOptions, releaseTypeIndex);
        String searchQuery = searchInputState.text().trim().toLowerCase(Locale.ROOT);

        String previousSelectionKey = selectedArtifactKey();

        List<Artifact> matches = new ArrayList<>();
        int matchCount = 0;

        for (Artifact artifact : artifacts) {
            if (!optionMatches(selectedVendor, artifact.vendor())) {
                continue;
            }
            if (!optionMatches(selectedOs, artifact.os())) {
                continue;
            }
            if (!optionMatches(selectedImageType, artifact.imageType())) {
                continue;
            }
            if (!optionMatches(selectedJvm, artifact.jvmImpl())) {
                continue;
            }
            if (!optionMatches(selectedReleaseType, artifact.releaseType())) {
                continue;
            }
            if (!searchQuery.isEmpty() && !matchesSearchQuery(searchQuery, artifact)) {
                continue;
            }

            matchCount++;
            if (matches.size() < MAX_VISIBLE_RESULTS) {
                matches.add(artifact);
            }
        }

        visibleArtifacts = matches;
        visibleItems = buildVisibleItems(matches);
        totalMatchCount = matchCount;

        restoreSelection(previousSelectionKey);
    }

    private void restoreSelection(String previousSelectionKey) {
        if (visibleArtifacts.isEmpty()) {
            listState.select(null);
            return;
        }

        if (previousSelectionKey != null) {
            for (int i = 0; i < visibleArtifacts.size(); i++) {
                if (previousSelectionKey.equals(artifactKey(visibleArtifacts.get(i)))) {
                    listState.select(i);
                    return;
                }
            }
        }

        Integer selected = listState.selected();
        if (selected != null && selected >= 0 && selected < visibleArtifacts.size()) {
            return;
        }

        listState.selectFirst();
    }

    private List<ListItem> buildVisibleItems(List<Artifact> source) {
        List<ListItem> items = new ArrayList<>(source.size());
        for (Artifact artifact : source) {
            Style releaseStyle = "ea".equalsIgnoreCase(artifact.releaseType())
                    ? Style.EMPTY.fg(Color.YELLOW)
                    : Style.EMPTY.fg(Color.GREEN);
            Line line = Line.from(
                    Span.styled(normalizedValue(artifact.vendor()), Style.EMPTY.fg(Color.CYAN)),
                    Span.raw("  J" + normalizedValue(artifact.javaVersion())),
                    Span.styled("  " + normalizedValue(artifact.releaseType()).toUpperCase(Locale.ROOT), releaseStyle),
                    Span.raw("  " + normalizedValue(artifact.os()) + "/" + normalizedValue(artifact.architecture())),
                    Span.raw("  " + normalizedValue(artifact.imageType()) + "/" + normalizedValue(artifact.jvmImpl())),
                    Span.styled("  " + normalizedValue(artifact.version()), Style.EMPTY.fg(Color.DARK_GRAY))
            );
            items.add(ListItem.from(line));
        }
        return items;
    }

    private void render(Frame frame) {
        Rect area = frame.area();

        List<Rect> rows = Layout.vertical()
                .constraints(
                        Constraint.length(5),
                        Constraint.length(3),
                        Constraint.fill(),
                        Constraint.length(2)
                )
                .split(area);

        renderHeader(frame, rows.get(0));
        renderSearch(frame, rows.get(1));

        List<Rect> bodyColumns = Layout.horizontal()
                .constraints(
                        Constraint.percentage(58),
                        Constraint.percentage(42)
                )
                .split(rows.get(2));
        renderArtifactList(frame, bodyColumns.get(0));
        renderDetails(frame, bodyColumns.get(1));

        renderFooter(frame, rows.get(3));
    }

    private void renderHeader(Frame frame, Rect area) {
        List<Line> lines = new ArrayList<>();
        lines.add(Line.from(
                Span.styled("JDKDB Data Browser", Style.EMPTY.bold().fg(Color.CYAN)),
                Span.raw("  API: " + API_BASE_URL)
        ));
        lines.add(Line.from(
                Span.raw("Endpoint: "),
                Span.styled(endpointMode.label(), Style.EMPTY.bold().fg(Color.YELLOW)),
                Span.raw(" [l latest, a all, f refresh]"),
                Span.raw("  Loaded: "),
                Span.styled(formatLoadedSummary(), Style.EMPTY.fg(Color.GREEN))
        ));
        lines.add(Line.from(
                Span.raw("Vendor[v/V]: "),
                Span.styled(selectedOption(vendorOptions, vendorIndex), Style.EMPTY.fg(Color.CYAN)),
                Span.raw("  OS[o/O]: "),
                Span.styled(selectedOption(osOptions, osIndex), Style.EMPTY.fg(Color.CYAN)),
                Span.raw("  Image[i/I]: "),
                Span.styled(selectedOption(imageTypeOptions, imageTypeIndex), Style.EMPTY.fg(Color.CYAN)),
                Span.raw("  JVM[j/J]: "),
                Span.styled(selectedOption(jvmOptions, jvmIndex), Style.EMPTY.fg(Color.CYAN)),
                Span.raw("  Release[r/R]: "),
                Span.styled(selectedOption(releaseTypeOptions, releaseTypeIndex), Style.EMPTY.fg(Color.CYAN))
        ));
        lines.add(Line.from(Span.raw("Reset filters [x], clear search [c], toggle search/list focus [Tab].")));

        Paragraph header = Paragraph.builder()
                .text(Text.from(lines))
                .overflow(Overflow.WRAP_WORD)
                .block(Block.builder()
                        .title("Java SDK Downloads API Browser")
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderColor(Color.CYAN)
                        .build())
                .build();
        frame.renderWidget(header, area);
    }

    private void renderSearch(Frame frame, Rect area) {
        String title = searchFocused
                ? "Search (active - Enter/Esc/Tab to return)"
                : "Search (/ or Tab to edit)";

        TextInput searchInput = TextInput.builder()
                .placeholder("Search vendor, filename, version, os, arch, url, features...")
                .block(Block.builder()
                        .title(title)
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderColor(searchFocused ? Color.YELLOW : Color.DARK_GRAY)
                        .build())
                .build();

        if (searchFocused) {
            searchInput.renderWithCursor(area, frame.buffer(), searchInputState, frame);
        } else {
            frame.renderStatefulWidget(searchInput, area, searchInputState);
        }
    }

    private void renderArtifactList(Frame frame, Rect area) {
        if (visibleItems.isEmpty()) {
            Paragraph empty = Paragraph.builder()
                    .text("No artifacts match the selected filters/search.")
                    .overflow(Overflow.WRAP_WORD)
                    .block(Block.builder()
                            .title("Artifacts")
                            .borders(Borders.ALL)
                            .borderType(BorderType.ROUNDED)
                            .borderColor(Color.CYAN)
                            .build())
                    .build();
            frame.renderWidget(empty, area);
            return;
        }

        String title = String.format(
                Locale.ROOT,
                "Artifacts (showing %,d of %,d matches)",
                visibleArtifacts.size(),
                totalMatchCount
        );
        ListWidget listWidget = ListWidget.builder()
                .items(visibleItems)
                .highlightStyle(Style.EMPTY.reversed().fg(Color.YELLOW))
                .highlightSymbol(">> ")
                .overflow(Overflow.ELLIPSIS)
                .block(Block.builder()
                        .title(title)
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderColor(Color.CYAN)
                        .build())
                .build();
        frame.renderStatefulWidget(listWidget, area, listState);
    }

    private void renderDetails(Frame frame, Rect area) {
        Artifact selected = selectedArtifact();
        List<Line> lines = new ArrayList<>();

        if (selected == null) {
            lines.add(Line.from(Span.raw("Select an artifact to inspect metadata.")));
            lines.add(Line.from(Span.raw("")));
            lines.add(Line.from(Span.raw("Tips:")));
            lines.add(Line.from(Span.raw(" - Use Up/Down or j/k to move")));
            lines.add(Line.from(Span.raw(" - Use PageUp/PageDown for faster movement")));
            lines.add(Line.from(Span.raw(" - Press / to search")));
        } else {
            lines.add(Line.from(Span.styled("Vendor: ", Style.EMPTY.bold()), Span.raw(normalizedValue(selected.vendor()))));
            lines.add(Line.from(Span.styled("Version: ", Style.EMPTY.bold()), Span.raw(normalizedValue(selected.version()))));
            lines.add(Line.from(Span.styled("Java version: ", Style.EMPTY.bold()), Span.raw(normalizedValue(selected.javaVersion()))));
            lines.add(Line.from(Span.styled("Release: ", Style.EMPTY.bold()),
                    Span.raw(normalizedValue(selected.releaseType()).toUpperCase(Locale.ROOT))));
            lines.add(Line.from(Span.styled("Image/JVM: ", Style.EMPTY.bold()),
                    Span.raw(normalizedValue(selected.imageType()) + "/" + normalizedValue(selected.jvmImpl()))));
            lines.add(Line.from(Span.styled("Platform: ", Style.EMPTY.bold()),
                    Span.raw(normalizedValue(selected.os()) + " / " + normalizedValue(selected.architecture()))));
            lines.add(Line.from(Span.styled("File type: ", Style.EMPTY.bold()), Span.raw(normalizedValue(selected.fileType()))));
            lines.add(Line.from(Span.styled("Size: ", Style.EMPTY.bold()), Span.raw(formatSize(selected.size()))));
            lines.add(Line.from(Span.styled("Features: ", Style.EMPTY.bold()), Span.raw(formatFeatures(selected.features()))));
            lines.add(Line.from(Span.raw("")));
            lines.add(Line.from(Span.styled("Filename:", Style.EMPTY.bold())));
            lines.add(Line.from(Span.raw(normalizedValue(selected.filename()))));
            lines.add(Line.from(Span.raw("")));
            lines.add(Line.from(Span.styled("Download URL:", Style.EMPTY.bold())));
            lines.add(Line.from(Span.styled(normalizedValue(selected.url()), Style.EMPTY.fg(Color.CYAN))));
            lines.add(Line.from(Span.raw("")));
            lines.add(Line.from(Span.styled("Checksums:", Style.EMPTY.bold())));
            lines.add(Line.from(Span.raw(" sha256: " + normalizedValue(selected.sha256()))));
            lines.add(Line.from(Span.raw(" sha512: " + normalizedValue(selected.sha512()))));
            lines.add(Line.from(Span.raw(" sha1:   " + normalizedValue(selected.sha1()))));
            lines.add(Line.from(Span.raw(" md5:    " + normalizedValue(selected.md5()))));
        }

        Paragraph details = Paragraph.builder()
                .text(Text.from(lines))
                .overflow(Overflow.WRAP_WORD)
                .block(Block.builder()
                        .title("Metadata details")
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderColor(Color.CYAN)
                        .build())
                .build();
        frame.renderWidget(details, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        String modeText = searchFocused
                ? "Mode: search input"
                : "Mode: list navigation";
        String truncationInfo = totalMatchCount > visibleArtifacts.size()
                ? String.format(
                Locale.ROOT,
                "Showing first %,d rows (narrow filters/search for more).",
                visibleArtifacts.size()
        )
                : "";
        Style statusStyle = errorMessage == null ? Style.EMPTY.fg(Color.GREEN) : Style.EMPTY.fg(Color.RED);

        Paragraph footer = Paragraph.builder()
                .text(Text.from(
                        Line.from(
                                Span.styled(modeText, Style.EMPTY.fg(Color.YELLOW)),
                                Span.raw("  |  q quit  arrows/PgUp/PgDn/Home/End navigate")
                        ),
                        Line.from(
                                Span.styled(statusMessage, statusStyle),
                                Span.raw(truncationInfo.isEmpty() ? "" : "  |  " + truncationInfo)
                        )
                ))
                .overflow(Overflow.WRAP_WORD)
                .build();
        frame.renderWidget(footer, area);
    }

    private Artifact selectedArtifact() {
        Integer selected = listState.selected();
        if (selected == null || selected < 0 || selected >= visibleArtifacts.size()) {
            return null;
        }
        return visibleArtifacts.get(selected);
    }

    private String selectedArtifactKey() {
        Artifact selected = selectedArtifact();
        return selected == null ? null : artifactKey(selected);
    }

    private String artifactKey(Artifact artifact) {
        return normalizedValue(artifact.vendor()) + "|" + normalizedValue(artifact.version()) + "|"
                + normalizedValue(artifact.filename());
    }

    private void resetFilters() {
        vendorIndex = 0;
        osIndex = 0;
        imageTypeIndex = 0;
        jvmIndex = 0;
        releaseTypeIndex = 0;
        searchInputState.clear();
    }

    private String formatLoadedSummary() {
        if (artifacts.isEmpty() || lastLoadedAt == null) {
            return "none";
        }
        return String.format(
                Locale.ROOT,
                "%,d artifacts at %s",
                artifacts.size(),
                TIME_FORMATTER.format(lastLoadedAt)
        );
    }

    private static String formatFeatures(List<String> features) {
        if (features == null || features.isEmpty()) {
            return "-";
        }
        return String.join(", ", features);
    }

    private static String formatSize(Long size) {
        if (size == null || size <= 0) {
            return "unknown";
        }
        double value = size.doubleValue();
        String[] units = {"B", "KiB", "MiB", "GiB", "TiB"};
        int unitIndex = 0;
        while (value >= 1024.0 && unitIndex < units.length - 1) {
            value /= 1024.0;
            unitIndex++;
        }
        if (unitIndex == 0) {
            return String.format(Locale.ROOT, "%,d %s", size, units[unitIndex]);
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex]);
    }

    private static boolean matchesSearchQuery(String query, Artifact artifact) {
        StringBuilder haystack = new StringBuilder(256);
        haystack.append(normalizedValue(artifact.vendor())).append(' ')
                .append(normalizedValue(artifact.filename())).append(' ')
                .append(normalizedValue(artifact.version())).append(' ')
                .append(normalizedValue(artifact.javaVersion())).append(' ')
                .append(normalizedValue(artifact.releaseType())).append(' ')
                .append(normalizedValue(artifact.os())).append(' ')
                .append(normalizedValue(artifact.architecture())).append(' ')
                .append(normalizedValue(artifact.imageType())).append(' ')
                .append(normalizedValue(artifact.jvmImpl())).append(' ')
                .append(normalizedValue(artifact.url())).append(' ')
                .append(normalizedValue(artifact.fileType()));
        if (artifact.features() != null && !artifact.features().isEmpty()) {
            for (String feature : artifact.features()) {
                haystack.append(' ').append(normalizedValue(feature));
            }
        }
        return haystack.toString().toLowerCase(Locale.ROOT).contains(query);
    }

    private static List<String> defaultFilterOptions() {
        return Collections.singletonList("all");
    }

    private static int findOptionIndex(List<String> options, String value) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    private static String selectedOption(List<String> options, int index) {
        if (options.isEmpty()) {
            return "all";
        }
        int safeIndex = clamp(index, 0, options.size() - 1);
        return options.get(safeIndex);
    }

    private static boolean optionMatches(String selected, String actual) {
        if ("all".equalsIgnoreCase(selected)) {
            return true;
        }
        return selected.equalsIgnoreCase(normalizedValue(actual));
    }

    private static int cycleIndex(int current, int size, boolean backwards) {
        if (size <= 1) {
            return 0;
        }
        if (backwards) {
            return current <= 0 ? size - 1 : current - 1;
        }
        return current >= size - 1 ? 0 : current + 1;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static boolean isPlainChar(KeyEvent event, char c) {
        return event.code() == KeyCode.CHAR
                && !event.hasCtrl()
                && !event.hasAlt()
                && event.character() == c;
    }

    private static String normalizedValue(String value) {
        if (value == null) {
            return "-";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "-";
        }
        return trimmed;
    }

    private static String normalizedSortValue(String value) {
        return normalizedValue(value).toLowerCase(Locale.ROOT);
    }

    private static boolean handleTextInputKey(TextInputState state, KeyEvent event) {
        switch (event.code()) {
            case BACKSPACE:
                state.deleteBackward();
                return true;
            case DELETE:
                state.deleteForward();
                return true;
            case LEFT:
                state.moveCursorLeft();
                return true;
            case RIGHT:
                state.moveCursorRight();
                return true;
            case HOME:
                state.moveCursorToStart();
                return true;
            case END:
                state.moveCursorToEnd();
                return true;
            case CHAR:
                if (event.modifiers().ctrl() || event.modifiers().alt()) {
                    return false;
                }
                char c = event.character();
                if (c >= 32 && c < 127) {
                    state.insert(c);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private enum EndpointMode {
        LATEST("/metadata/latest.json", "latest"),
        ALL("/metadata/all.json", "all");

        private final String path;
        private final String label;

        EndpointMode(String path, String label) {
            this.path = path;
            this.label = label;
        }

        String path() {
            return path;
        }

        String label() {
            return label;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Artifact(
            @JsonProperty("vendor") String vendor,
            @JsonProperty("filename") String filename,
            @JsonProperty("file_type") String fileType,
            @JsonProperty("release_type") String releaseType,
            @JsonProperty("version") String version,
            @JsonProperty("java_version") String javaVersion,
            @JsonProperty("jvm_impl") String jvmImpl,
            @JsonProperty("os") String os,
            @JsonProperty("architecture") String architecture,
            @JsonProperty("image_type") String imageType,
            @JsonProperty("features") List<String> features,
            @JsonProperty("url") String url,
            @JsonProperty("md5") String md5,
            @JsonProperty("sha1") String sha1,
            @JsonProperty("sha256") String sha256,
            @JsonProperty("sha512") String sha512,
            @JsonProperty("size") Long size
    ) {
        private Artifact {
            features = features == null ? Collections.emptyList() : features;
        }
    }
}
