///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-markup:LATEST
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.markup.Markup;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.table.Cell;
import dev.tamboui.widgets.table.Row;
import dev.tamboui.widgets.table.Table;
import dev.tamboui.widgets.table.TableState;

import java.io.IOException;
import java.util.List;

/**
 * Demo showcasing Markup functionality:
 * <ul>
 *   <li>Printing markup directly to terminal (ANSI output)</li>
 *   <li>Using markup in TamboUI widgets (Text output)</li>
 * </ul>
 * <p>
 * This demo demonstrates how the same markup syntax can be used
 * both for simple terminal output and for rich text in TUI applications.
 */
public class MarkupDemo {

    private boolean running = true;
    private int exampleIndex = 0;
    private final TableState unicodeTableState = new TableState();

    private static final String[] MARKUP_EXAMPLES = {
        "[bold cyan]Welcome[/] to [green]TamboUI[/] [yellow]Markup[/]!",
        "[bold]Bold[/], [italic]italic[/], [underlined]underlined[/], [dim]dim[/] text",
        "[red]Red[/], [green]Green[/], [blue]Blue[/], [yellow]Yellow[/], [magenta]Magenta[/], [cyan]Cyan[/]",
        "[on red]Red background[/] with [bold on blue]bold blue background[/]",
        "[#ff5733]Hex color[/] and [rgb(255,200,0)]RGB color[/]",
        "[bold red on blue]Combined styles[/] work great!",
        "[link=https://github.com/tamboui/tamboui]Clickable link[/link]",
        ":check: Done! :smile: Hello! :star: Amazing!",
        "[bold]Nested[/] [red]styles[/red] [green]work[/green] [blue]perfectly[/blue]!",
        "Multi [bold]Unicode[/]: 👨‍👩‍👧"
    };

    public static void main(String[] args) throws Exception {
        new MarkupDemo().run();
    }

    public void run() throws Exception {
        // First, print some examples directly to terminal
        printMarkupExamples();

        // Small delay to let user see the printed examples
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // Ignore
        }

        // Then, show the TUI with markup in widgets
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Handle resize
            backend.onResize(() -> {
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    // Ignore
                }
            });

            // Initial draw
            terminal.draw(this::ui);

            // Event loop
            while (running) {
                int c = backend.read(100);
                if (c == -1) {
                    continue;
                }
                if (c == -2) {
                    // Timeout - could be used for animations
                    continue;
                }

                boolean needsRedraw = handleInput(c);
                if (needsRedraw) {
                    terminal.draw(this::ui);
                }
            }
        }
    }

    /**
     * Print markup examples directly to terminal using ANSI output.
     */
    private void printMarkupExamples() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Markup Examples - Direct Terminal Output (ANSI)");
        System.out.println("=".repeat(60) + "\n");

        // Using Markup.println() convenience method
        Markup.println("[bold cyan]Example 1:[/] Basic styling");
        Markup.println("[bold]Bold[/], [italic]italic[/], [underlined]underlined[/]");
        System.out.println();

        // Using Markup.toAnsi() for more control
        System.out.print(Markup.toAnsi("[bold green]Example 2:[/] Colors - "));
        System.out.println(Markup.toAnsi("[red]Red[/] [green]Green[/] [blue]Blue[/]"));
        System.out.println();

        // Background colors
        Markup.println("[bold]Example 3:[/] Background colors");
        Markup.println("[on red]Red background[/] [on green]Green background[/] [on blue]Blue background[/]");
        System.out.println();

        // Combined styles
        Markup.println("[bold]Example 4:[/] Combined styles");
        Markup.println("[bold red on blue]Bold red on blue[/] [italic green on yellow]Italic green on yellow[/]");
        System.out.println();

        // Emojis
        Markup.println("[bold]Example 5:[/] Emoji shortcodes");
        Markup.println(":check: Done! :smile: Hello! :star: Amazing! :heart: Love!");
        System.out.println();

        // Links
        Markup.println("[bold]Example 6:[/] Terminal hyperlinks");
        Markup.println("[link=https://github.com/tamboui/tamboui]Click this link (if your terminal supports it)[/link]");
        System.out.println();

        System.out.println("=".repeat(60));
        System.out.println("Switching to TUI demo in 2 seconds...");
        System.out.println("=".repeat(60) + "\n");
    }

    private boolean handleInput(int c) {
        return switch (c) {
            case 'q', 'Q', 3 -> { // q, Q, or Ctrl+C
                running = false;
                yield true;
            }
            case 'n', 'N' -> { // Next example
                exampleIndex = (exampleIndex + 1) % MARKUP_EXAMPLES.length;
                yield true;
            }
            case 'p', 'P' -> { // Previous example
                exampleIndex = (exampleIndex - 1 + MARKUP_EXAMPLES.length) % MARKUP_EXAMPLES.length;
                yield true;
            }
            default -> false;
        };
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Header
                Constraint.fill(),     // Main content
                Constraint.length(3)   // Footer
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderMainContent(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Markup.toLine("[bold cyan]TamboUI[/] [yellow]Markup[/] Demo")
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        // Split into 4 sections: example, ANSI comparison, unicode table, documentation
        // Change to vertical layout: first row is split horizontally for example/comparison
        var sections = Layout.vertical()
            .constraints(
                Constraint.percentage(40),  // Top: Example + ANSI comparison (side by side)
                Constraint.percentage(30),  // Unicode table
                Constraint.fill()           // Documentation
            )
            .spacing(1)
            .split(area);

        // -- Split the top section horizontally for example and ANSI comparison --
        var topRow = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),   // Example
                Constraint.percentage(50)    // ANSI comparison
            )
            .split(sections.get(0));

        // Left: Current example with markup in widget
        renderMarkupExample(frame, topRow.get(0));

        // Right: Show ANSI output comparison
        renderAnsiComparison(frame, topRow.get(1));

        // Second: Unicode width table (full width)
        renderUnicodeTable(frame, sections.get(1));

        // Bottom: Documentation (full width)
        renderDocumentation(frame, sections.get(2));
    }

    /**
     * Render the current markup example using Markup.toText() for TamboUI.
     */
    private void renderMarkupExample(Frame frame, Rect area) {
        String markup = MARKUP_EXAMPLES[exampleIndex];

        // Convert markup to Text for use in Paragraph widget
        var text = Markup.toText(markup);

        Paragraph paragraph = Paragraph.builder()
            .text(text)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from(
                    Markup.toLine("[bold green]Markup Example[/]")
                ).centered())
                .titleBottom(Title.from(
                    Markup.toLine("[dim]Example " + (exampleIndex + 1) + " of " + MARKUP_EXAMPLES.length + "[/dim]")
                ).centered())
                .build())
            .build();

        frame.renderWidget(paragraph, area);
    }

    /**
     * Render a comparison showing the ANSI output.
     */
    private void renderAnsiComparison(Frame frame, Rect area) {
        String markup = MARKUP_EXAMPLES[exampleIndex];

        // Show both the markup source and indicate it's rendered as ANSI
        var comparisonText = Markup.toText(
            "[bold]Markup Source:[/]\n" +
            "[dim]" + markup.replace("[", "[[") + "[/dim]\n" +
            "\n" +
            "[bold]ANSI Output:[/] (rendered above)\n" +
            "[dim]Use Markup.toAnsi() for terminal output[/dim]"
        );

        Paragraph comparison = Paragraph.builder()
            .text(comparisonText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                .title(Title.from(
                    Markup.toLine("[bold yellow]ANSI vs Text Output[/]")
                ).centered())
                .build())
            .build();

        frame.renderWidget(comparison, area);
    }

    /**
     * Render a table showing Unicode characters with various widths.
     */
    private void renderUnicodeTable(Frame frame, Rect area) {
        // Create header row with markup
        Row header = Row.from(
            Cell.from(Markup.toText("[bold]Type[/]")),
            Cell.from(Markup.toText("[bold]Character[/]")),
            Cell.from(Markup.toText("[bold]Example[/]")),
            Cell.from(Markup.toText("[bold]Width[/]"))
        ).style(Style.EMPTY.fg(Color.YELLOW));

        // Create data rows with various Unicode characters
        List<Row> rows = new java.util.ArrayList<>();
        
        // Full-width characters (East Asian)
        rows.add(Row.from(
            Cell.from(Markup.toText("[cyan]Full-width[/]")),
            Cell.from("中"),
            Cell.from(Markup.toText("[green]中文[/] [yellow]日本語[/] [magenta]한국어[/]")),
            Cell.from(Markup.toText("[dim]2 cols[/]"))
        ));
        
        // Half-width characters (ASCII/Latin)
        rows.add(Row.from(
            Cell.from(Markup.toText("[cyan]Half-width[/]")),
            Cell.from("A"),
            Cell.from(Markup.toText("[green]ASCII[/] [yellow]Latin[/] [magenta]123[/]")),
            Cell.from(Markup.toText("[dim]1 col[/]"))
        ));
        
        // Emoji (typically 2 columns)
        rows.add(Row.from(
            Cell.from(Markup.toText("[cyan]Emoji[/]")),
            Cell.from("😀"),
            Cell.from(Markup.toText("[green]:smile:[/] [yellow]:star:[/] [magenta]:heart:[/]")),
            Cell.from(Markup.toText("[dim]2 cols[/]"))
        ));
        
        // Zero-width characters
        rows.add(Row.from(
            Cell.from(Markup.toText("[cyan]Zero-width[/]")),
            Cell.from("​"), // Zero-width space
            Cell.from(Markup.toText("[green]Combining[/] [yellow]marks[/] [magenta]éñ[/]")),
            Cell.from(Markup.toText("[dim]0 cols[/]"))
        ));
        
        // Ambiguous width characters
        rows.add(Row.from(
            Cell.from(Markup.toText("[cyan]Ambiguous[/]")),
            Cell.from("Ω"),
            Cell.from(Markup.toText("[green]Greek[/] [yellow]Ωαβ[/] [magenta]Symbols[/]")),
            Cell.from(Markup.toText("[dim]1-2 cols[/]"))
        ));
        
        // Wide characters (some emoji)
        rows.add(Row.from(
            Cell.from(Markup.toText("[cyan]Wide[/]")),
            Cell.from("🦄"),
            Cell.from(Markup.toText("[green]Unicorn[/] [yellow]🦄🦋[/] [magenta]Complex[/]")),
            Cell.from(Markup.toText("[dim]2 cols[/]"))
        ));
        
        // Combining characters
        rows.add(Row.from(
            Cell.from(Markup.toText("[cyan]Combining[/]")),
            Cell.from("👨‍👩‍👧"),
            Cell.from(Markup.toText("[green]Family[/] [yellow]👨‍👩‍👧[/] [magenta]Combined[/]")),
            Cell.from(Markup.toText("[dim]1 col[/]"))
        ));

        Table table = Table.builder()
            .header(header)
            .rows(rows)
            .widths(
                Constraint.percentage(20),
                Constraint.length(8),
                Constraint.fill(),
                Constraint.length(10)
            )
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .title(Title.from(
                    Markup.toLine("[bold magenta]Unicode Width Examples[/]")
                ).centered())
                .build())
            .build();

        frame.renderStatefulWidget(table, area, unicodeTableState);
    }

    /**
     * Render documentation showing markup syntax.
     */
    private void renderDocumentation(Frame frame, Rect area) {
        // Create documentation text using markup
        var docText = Markup.toText(
            "[bold]Markup Syntax:[/]\n" +
            "\n" +
            "[bold]Modifiers:[/]\n" +
            "  [bold]bold[/], [italic]italic[/], [underlined]underlined[/]\n" +
            "\n" +
            "[bold]Colors:[/]\n" +
            "  [red]red[/], [green]green[/], [blue]blue[/]\n" +
            "  [on red]on red[/] (background)\n" +
            "\n" +
            "[bold]Advanced:[/]\n" +
            "  [#ff5733]hex colors[/]\n" +
            "  [rgb(255,200,0)]RGB colors[/]\n" +
            "  :smile: emoji shortcodes\n" +
            "  [link=url]links[/link]\n" +
            "\n" +
            "[bold]Usage:[/]\n" +
            "  [cyan]Markup.println()[/] - print to terminal\n" +
            "  [cyan]Markup.toText()[/] - use in widgets\n" +
            "  [cyan]Markup.toAnsi()[/] - get ANSI string"
        );

        Paragraph docParagraph = Paragraph.builder()
            .text(docText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.BLUE))
                .title(Title.from(
                    Markup.toLine("[bold blue]Documentation[/]")
                ).centered())
                .build())
            .build();

        frame.renderWidget(docParagraph, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        var helpText = Markup.toText(
            "[bold yellow]n/p[/] [dim]Next/Previous example  [/dim]" +
            "[bold yellow]q[/] [dim]Quit[/dim]"
        );

        Paragraph footer = Paragraph.builder()
            .text(helpText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(footer, area);
    }
}
