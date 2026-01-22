//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-pygments-graalpy:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Rect;
import dev.tamboui.pygments.Result;
import dev.tamboui.pygments.SyntaxHighlighter;
import dev.tamboui.pygments.graalpy.GraalPySyntaxHighlighter;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.Tags;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.elements.ListElement;
import dev.tamboui.toolkit.elements.RichTextAreaElement;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.BindingSets;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo showcasing syntax highlighting with GraalPy (embedded Python).
 * <p>
 * This demo uses GraalPy to run Pygments directly in the JVM without
 * requiring any external Python installation.
 * <p>
 * <b>Note:</b> Requires Java 21+. First highlight may be slow due to
 * Python runtime initialization.
 */
public final class GraalPyDemo implements Element {

    private static final List<Sample> SAMPLES = Arrays.asList(
        new Sample(
            "Java",
            "Hello.java",
            """
                package dev.tamboui.demo;

                class Hello {
                  static String greet(String name) {
                    // Ternary operator example
                    return name == null ? "Hello, world" : "Hello, " + name;
                  }

                  public static void main(String[] args) {
                    System.out.println(greet(args.length > 0 ? args[0] : null));
                    int n = 42;
                  }
                }
                """
        ),
        new Sample(
            "Python",
            "hello.py",
            """
                from dataclasses import dataclass

                @dataclass
                class User:
                    name: str

                def greet(user: User | None) -> str:
                    # This is a comment
                    return f"Hello, {user.name if user else 'world'}"\\
                        .strip()

                print(greet(User('Ada')))
                """
        ),
        new Sample(
            "JavaScript",
            "app.js",
            """
                export function greet(name) {
                  // nullish coalescing
                  return `Hello, ${name ?? 'world'}`;
                }

                console.log(greet(null));
                """
        ),
        new Sample(
            "Rust",
            "main.rs",
            """
                fn greet(name: Option<&str>) -> String {
                    // This is a comment
                    format!("Hello, {}", name.unwrap_or("world"))
                }

                fn main() {
                    println!("{}", greet(None));
                    let n: i32 = 42;
                    println!("n={}", n);
                }
                """
        ),
        new Sample(
            "Markdown",
            "README.md",
            """
                # TamboUI Demo

                A **terminal UI** framework for _Java_.

                ## Features

                - Syntax highlighting
                - Rich text rendering
                - [Links](https://tamboui.dev)

                > Blockquote support!
                """
        ),
        new Sample(
            "Kotlin",
            "Main.kt",
            """
                data class User(val name: String)

                fun greet(user: User?): String {
                    // Kotlin null-safe access
                    return "Hello, ${user?.name ?: "world"}"
                }

                fun main() {
                    println(greet(User("Alice")))
                    println(greet(null))
                }
                """
        ),
        new Sample(
            "HTML",
            "index.html",
            """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>TamboUI</title>
                  <style>
                    body { font-family: sans-serif; }
                  </style>
                </head>
                <body>
                  <h1>Hello, World!</h1>
                  <script>console.log('Ready');</script>
                </body>
                </html>
                """
        )
    );

    private final RichTextAreaElement codeArea;
    private final ListElement<Sample> sampleList;
    private final GraalPySyntaxHighlighter highlighter;

    private final Text[] highlightedCache;
    private final String[] subtitleCache;
    private int lastSelected = -1;
    private Text cachedText = Text.empty();
    private String cachedSubtitle = "";
    private String cachedTitle = "";
    private boolean initializing = false;

    public GraalPyDemo() {
        highlighter = new GraalPySyntaxHighlighter();

        codeArea = new RichTextAreaElement()
            .wrapCharacter()
            .scrollbar(RichTextAreaElement.ScrollBarPolicy.AS_NEEDED)
            .rounded()
            .focusable()
            .focusedBorderColor(Color.CYAN)
            .fill();

        sampleList = new ListElement<Sample>()
            .data(SAMPLES, s -> row(
                text(s.title).bold(),
                spacer(),
                text(s.filename).dim()
            ))
            .title("Samples")
            .rounded()
            .scrollbar(ListElement.ScrollBarPolicy.AS_NEEDED)
            .highlightSymbol("› ")
            .highlightColor(Color.CYAN)
            .autoScroll()
            .focusable()
            .id("samples");

        highlightedCache = new Text[SAMPLES.size()];
        subtitleCache = new String[SAMPLES.size()];
    }

    public static void main(String[] args) throws Exception {
        var config = TuiConfig.builder()
            .mouseCapture(true)
            .bindings(BindingSets.vim())
            .build();

        try (var runner = ToolkitRunner.builder()
            .config(config)
            .bindings(BindingSets.vim())
            .build()) {
            var demo = new GraalPyDemo();
            runner.run(() -> demo);
        } finally {
            // Clean up GraalPy resources
        }
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        int selected = Math.min(Math.max(0, sampleList.selected()), SAMPLES.size() - 1);
        if (selected != lastSelected) {
            lastSelected = selected;
        }

        Sample sample = SAMPLES.get(selected);
        cachedTitle = sample.title + " — " + sample.filename;

        // Highlight each sample at most once (lazy cache).
        if (highlightedCache[selected] == null) {
            // Show initializing message on first highlight
            if (!initializing && subtitleCache[selected] == null) {
                initializing = true;
                subtitleCache[selected] = "Initializing GraalPy (first run may take a moment)...";
            }

            Result result = highlighter.highlightWithInfo(
                sample.filename,
                sample.source,
                Duration.ofSeconds(60) // Longer timeout for GraalPy init
            );

            initializing = false;
            subtitleCache[selected] = result.highlighted()
                ? "GraalPy: lexer=" + result.lexer().orElse("?")
                : ("GraalPy: off (" + result.message().orElse("unknown") + ")");
            highlightedCache[selected] = addLineNumbers(result.text());
        }

        cachedSubtitle = subtitleCache[selected] != null ? subtitleCache[selected] : "";
        cachedText = highlightedCache[selected] != null ? highlightedCache[selected] : Text.raw(sample.source);

        column(
            panel(() -> row(
                text(" Syntax highlighting (GraalPy) ").bold(),
                spacer(1),
                text(" [j/k] Navigate ").dim(),
                text(" [q] Quit ").dim()
            )).rounded().length(3),
            text(cachedSubtitle).dim().length(1),
            row(
                sampleList.length(34),
                spacer(1),
                panel(() -> codeArea.text(cachedText))
                    .title(cachedTitle)
                    .rounded()
                    .fill()
            ).fill()
        ).render(frame, area, context);
    }

    private static Text addLineNumbers(Text text) {
        List<Line> in = text.lines();
        if (in.isEmpty()) {
            return text;
        }

        int digits = String.valueOf(in.size()).length();
        Style lnStyle = Style.EMPTY
            .fg(Color.GRAY)
            .dim()
            .withExtension(Tags.class, Tags.of("syntax-line-number"));

        List<Line> out = new ArrayList<>(in.size());
        for (int i = 0; i < in.size(); i++) {
            Line line = in.get(i);
            String ln = padLeft(String.valueOf(i + 1), digits);
            List<Span> spans = new ArrayList<>(2 + line.spans().size());
            spans.add(Span.styled(ln, lnStyle));
            spans.add(Span.styled(" │ ", lnStyle));
            spans.addAll(line.spans());
            out.add(Line.from(spans));
        }
        return Text.from(out);
    }

    private static String padLeft(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        StringBuilder sb = new StringBuilder(width);
        for (int i = s.length(); i < width; i++) {
            sb.append(' ');
        }
        sb.append(s);
        return sb.toString();
    }

    private static final class Sample {
        final String title;
        final String filename;
        final String source;

        Sample(String title, String filename, String source) {
            this.title = title;
            this.filename = filename;
            this.source = source;
        }
    }
}
