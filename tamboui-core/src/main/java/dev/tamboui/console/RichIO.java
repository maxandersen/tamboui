/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.console;

import java.io.PrintWriter;

import dev.tamboui.style.Theme;
import dev.tamboui.terminal.TextAnsiRenderer;
import dev.tamboui.text.Text;
import dev.tamboui.text.markup.Markup;

/**
 * A small "Rich-like" I/O utility that can print markup and pretty formatted objects.
 *
 * <p>This is intended for CLI output (stdout/stderr), not full-screen TUIs.
 */
public final class RichIO {

    private final PrintWriter out;
    private final boolean markupEnabled;
    private final Theme theme;
  
    public RichIO(PrintWriter out) {
        this(out, true);
    }

    public RichIO(PrintWriter out, boolean markupEnabled) {
        this(out, markupEnabled, Theme.defaultTheme());
    }

    public RichIO(PrintWriter out, boolean markupEnabled, Theme theme) {
        this.out = out == null ? new PrintWriter(System.out, true) : out;
        this.markupEnabled = markupEnabled;
        this.theme = theme == null ? Theme.defaultTheme() : theme;
    }

    public static RichIO system() {
        return new RichIO(new PrintWriter(System.out, true));
    }

    public static RichIO stderr() {
        return new RichIO(new PrintWriter(System.err, true));
    }

    /**
     * Print an object (strings, {@link Text}, and {@link Throwable} receive special handling).
     * Always prints a trailing newline (similar to Rich's Console.print()).
     *
     * @param object object to print
     */
    public void print(Object object) {
        print(object, true);
    }

    /**
     * Print an object, optionally followed by a newline.
     *
     * @param object object to print
     * @param newline whether to print a trailing newline
     */
    public void print(Object object, boolean newline) {
        Text text = pretty(object);
        String ansi = TextAnsiRenderer.toAnsi(text);
        out.print(ansi);
        if (newline) {
            out.println();
        }
        out.flush();
    }

    /**
     * Convert an object to styled {@link Text}.
     *
     * @param object object
     * @return text
     */
    public Text pretty(Object object) {
        if (object == null) {
            return Text.from("null");
        }
        if (object instanceof Text) {
            return (Text) object;
        }
        if (object instanceof Throwable) {
            return RichTraceback.render((Throwable) object);
        }
        if (object instanceof String) {
            String s = (String) object;
            return markupEnabled ? Markup.render(s, dev.tamboui.style.Style.EMPTY, theme) : Text.from(s);
        }
        return Text.from(String.valueOf(object));
    }
}

