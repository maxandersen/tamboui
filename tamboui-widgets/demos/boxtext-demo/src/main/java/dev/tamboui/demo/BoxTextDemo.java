///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.boxtext.BoxText;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.IOException;

/**
 * Demo TUI application showcasing the {@link BoxText} widget.
 *
 * <p>Shows multiple examples:
 * <ul>
 *   <li>Basic rendering (A-Z / 0-9)</li>
 *   <li>Block integration</li>
 *   <li>Theme / color cycling</li>
 *   <li>Clipping in narrow areas</li>
 *   <li>Multi-line input (3 rows per input line)</li>
 * </ul>
 */
public final class BoxTextDemo {

    private static final Theme[] THEMES = new Theme[] {
        new Theme(Color.CYAN, Color.YELLOW, Color.GREEN, Color.DARK_GRAY),
        new Theme(Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.BLUE),
        new Theme(Color.GREEN, Color.WHITE, Color.CYAN, Color.DARK_GRAY),
    };

    private boolean running = true;
    private int themeIndex = 0;
    private boolean uppercase = true;

    public static void main(String[] args) throws Exception {
        new BoxTextDemo().run();
    }

    public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            backend.onResize(() -> {
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    // Ignore
                }
            });

            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                } else if (c == 'c' || c == 'C') {
                    themeIndex = (themeIndex + 1) % THEMES.length;
                } else if (c == 'u' || c == 'U') {
                    uppercase = !uppercase;
                }
            }
        }
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
        renderMain(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        Theme theme = THEMES[themeIndex];

        Block header = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(theme.accent))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().fg(theme.accent),
                    Span.raw("BoxText Demo ").fg(theme.primary)
                )
            ).centered())
            .build();

        frame.renderWidget(header, area);
    }

    private void renderMain(Frame frame, Rect area) {
        Theme theme = THEMES[themeIndex];

        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .spacing(1)
            .split(area);

        var top = Layout.horizontal()
            .constraints(Constraint.percentage(50), Constraint.percentage(50))
            .spacing(1)
            .split(rows.get(0));

        var bottom = Layout.horizontal()
            .constraints(Constraint.percentage(50), Constraint.percentage(50))
            .spacing(1)
            .split(rows.get(1));

        renderExample(frame, top.get(0),
            "Basics (A-Z / 0-9)",
            "Tamboui",
            Style.EMPTY.fg(theme.primary).bold(),
            theme.accent,
            theme.background);

        renderExample(frame, top.get(1),
            "Numbers",
            "0123456789",
            Style.EMPTY.fg(theme.secondary),
            theme.secondary,
            theme.background);

        renderExample(frame, bottom.get(0),
            "Clipping (narrow area)",
            uppercase ? "ABCDEFGHIJKLMNOPQRSTUVWXYZ" : "abcdefghijklmnopqrstuvwxyz",
            Style.EMPTY.fg(theme.accent),
            theme.primary,
            theme.background);

        renderExample(frame, bottom.get(1),
            "Multiline + uppercase: " + (uppercase ? "ON" : "OFF"),
            "Box\nText",
            Style.EMPTY.fg(theme.primary).bg(theme.background),
            theme.accent,
            theme.background);
    }

    private void renderExample(Frame frame,
                               Rect area,
                               String title,
                               String text,
                               Style textStyle,
                               Color borderColor,
                               Color background) {
        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(borderColor))
            .title(Title.from(title).centered())
            .build();

        BoxText widget = BoxText.builder()
            .text(text)
            .style(textStyle.bg(background))
            .uppercase(uppercase)
            .block(block)
            .build();

        frame.renderWidget(widget, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Theme theme = THEMES[themeIndex];

        Line help = Line.from(
            Span.raw(" c").bold().fg(theme.secondary),
            Span.raw(" Cycle theme  ").dim(),
            Span.raw("u").bold().fg(theme.secondary),
            Span.raw(" Toggle uppercase  ").dim(),
            Span.raw("q").bold().fg(theme.secondary),
            Span.raw(" Quit").dim()
        );

        Paragraph footer = Paragraph.builder()
            .text(Text.from(help))
            .style(Style.EMPTY.fg(Color.WHITE))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(theme.background))
                .build())
            .build();

        frame.renderWidget(footer, area);
    }

    private static final class Theme {
        private final Color accent;
        private final Color primary;
        private final Color secondary;
        private final Color background;

        private Theme(Color accent, Color primary, Color secondary, Color background) {
            this.accent = accent;
            this.primary = primary;
            this.secondary = secondary;
            this.background = background;
        }
    }
}

