/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.console;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;

/**
 * Formats {@link Throwable}s as styled {@link Text}.
 *
 * <p>This is a deliberately lightweight alternative to Rich's traceback rendering.
 * It focuses on readable stack traces suitable for terminals.
 */
public final class RichTraceback {

    private static final Style EXCEPTION_STYLE = Style.EMPTY.fg(Color.RED).bold();
    private static final Style CAUSED_BY_STYLE = Style.EMPTY.fg(Color.YELLOW).bold();
    private static final Style FRAME_STYLE = Style.EMPTY.fg(Color.GRAY);
    private static final Style LOCATION_STYLE = Style.EMPTY.fg(Color.CYAN);

    private RichTraceback() {
    }

    /**
     * Render a {@link Throwable} into styled text.
     *
     * @param throwable the throwable
     * @return styled text
     */
    public static Text render(Throwable throwable) {
        if (throwable == null) {
            return Text.empty();
        }
        List<Line> lines = new ArrayList<>();
        appendThrowable(lines, throwable, false);
        return Text.from(lines);
    }

    private static void appendThrowable(List<Line> out, Throwable t, boolean isCause) {
        if (isCause) {
            out.add(Line.from(Span.styled("Caused by:", CAUSED_BY_STYLE)));
        }

        String header = t.getClass().getName();
        String message = t.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            header = header + ": " + message;
        }
        out.add(Line.from(Span.styled(header, EXCEPTION_STYLE)));

        for (StackTraceElement element : t.getStackTrace()) {
            out.add(renderFrame(element));
        }

        for (Throwable suppressed : t.getSuppressed()) {
            out.add(Line.from(Span.styled("Suppressed:", CAUSED_BY_STYLE)));
            appendThrowable(out, suppressed, false);
        }

        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            out.add(Line.empty());
            appendThrowable(out, cause, true);
        }
    }

    private static Line renderFrame(StackTraceElement element) {
        String method = element.getClassName() + "." + element.getMethodName() + "()";
        String location;
        if (element.isNativeMethod()) {
            location = "(Native Method)";
        } else if (element.getFileName() == null) {
            location = "(Unknown Source)";
        } else if (element.getLineNumber() >= 0) {
            location = "(" + element.getFileName() + ":" + element.getLineNumber() + ")";
        } else {
            location = "(" + element.getFileName() + ")";
        }
        return Line.from(
            Span.styled("  at ", FRAME_STYLE),
            Span.styled(method, FRAME_STYLE),
            Span.raw(" "),
            Span.styled(location, LOCATION_STYLE)
        );
    }
}


