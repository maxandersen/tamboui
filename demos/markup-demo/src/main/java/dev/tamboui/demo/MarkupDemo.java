//DEPS dev.tamboui:tamboui-core:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.console.RichIO;
import dev.tamboui.style.Theme;
import dev.tamboui.text.markup.Markup;
import dev.tamboui.text.markup.MarkupParseException;

import java.util.Arrays;
import java.util.List;

/**
 * Demonstrates Rich / BBCode-style markup rendered to TamboUI {@link dev.tamboui.text.Text}.
 */
public final class MarkupDemo {

    private MarkupDemo() {
        // Demo entrypoint only.
    }

    /**
     * Runs the markup demo.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        RichIO console = RichIO.system();

        List<String> examples = Arrays.asList(
            "Plain text (no markup)",
            "[bold]Bold[/] and [italic]italic[/] and [underline]underline[/]",
            "[bold red]Hello[/] World",
            "[blue][green][red]R[/red]G[/green]B[/blue]",
            "[bold rgb(10, 20, 30)]RGB fg via rgb(...) with spaces[/]",
            "[on blue white]Background demo[/]",
            "[link=https://example.com]Hyperlink demo (OSC8)[/link]",
            "Escaped tag: \\\\[red] literally shows [red] text",
            "[b][on red]What [i]is up[/on red] with you?[/]",
            "[error]Error message[/error] [warning]Warning message[/warning] [success]Success message[/success] [info]Info message[/info]",
            ":warning: Alert! :error: Failed :success: Done :info: Info",
            "[error]:error: Error with emoji[/error] [success]:success: Success with emoji[/success]"
        );

        System.out.println("=== Markup Demo ===\n");

        for (String example : examples) {
            System.out.println("--- input ---");
            System.out.println(example);
            System.out.println("--- output ---");
            try {
                console.print(example);
            } catch (MarkupParseException e) {
                System.out.println("MarkupParseException: " + e.getMessage());
            }
            System.out.println();
        }

        System.out.println("--- escape() helper ---");
        String unsafe = "Hello [red]World[/red]";
        System.out.println("unsafe : " + unsafe);
        System.out.println("escaped: " + Markup.escape(unsafe));
        System.out.println();

        System.out.println("--- Custom Theme ---");
        Theme customTheme = Theme.builder()
            .style("danger", dev.tamboui.style.Style.EMPTY.fg(dev.tamboui.style.Color.RED).bold())
            .style("ok", dev.tamboui.style.Style.EMPTY.fg(dev.tamboui.style.Color.GREEN).bold())
            .build();
        console.print("[danger]Custom danger style[/danger] [ok]Custom OK style[/ok]");
        System.out.println();

        System.out.println("--- mismatched close example ---");
        try {
            console.print("foo[/]");
        } catch (MarkupParseException e) {
            System.out.println("MarkupParseException: " + e.getMessage());
        }

        System.out.println();
        System.out.println("--- exception pretty print ---");
        try {
            throw new RuntimeException("boom", new IllegalArgumentException("bad input"));
        } catch (RuntimeException e) {
            console.print(e);
        }
    }
}


