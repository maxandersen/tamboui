///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//FILES calculator.tcss=../../../../resources/calculator.tcss
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.widgets.boxtext.BoxText;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.Borders;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * A Textual-inspired calculator demo.
 *
 * <p>Keyboard and mouse supported. Styling is loaded from {@code /calculator.tcss}.
 * The display uses {@link BoxText} for a calculator-style large digits look.
 *
 * <p>Controls:
 * <ul>
 *   <li>0-9, ., +, -, *, /, %</li>
 *   <li>Enter or = for equals</li>
 *   <li>c for clear (AC/C semantics)</li>
 *   <li>q to quit</li>
 * </ul>
 */
public final class CalculatorDemo implements Element {

    private static final MathContext MATH = MathContext.DECIMAL64;

    private String numbers = "0";
    private String value = "";
    private BigDecimal left = BigDecimal.ZERO;
    private BigDecimal right = BigDecimal.ZERO;
    private Operator operator = Operator.PLUS;

    private final StyleEngine styleEngine;

    public CalculatorDemo() {
        styleEngine = StyleEngine.create();
        try {
            styleEngine.loadStylesheet("calculator", "/calculator.tcss");
            styleEngine.setActiveStylesheet("calculator");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load calculator.tcss", e);
        }
    }

    public static void main(String[] args) throws Exception {
        var demo = new CalculatorDemo();
        demo.run();
    }

    public void run() throws Exception {
        var config = TuiConfig.builder()
            .mouseCapture(true)
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.styleEngine(styleEngine);
            runner.run(() -> this);
        }
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        // Outer container, styled via #calculator
        panel(() -> column(
            // Display (top)
            panel(() -> new BoxTextDisplay(numbers))
                .id("numbers")
                .length(5),

            // Buttons (bottom)
            grid(
                // Row 1: AC/C, +/-, %, ÷
                button(clearLabel(), "ac", this::pressClear).addClass("secondaryops"),
                button("+/-", "plus-minus", () -> press("plus-minus")).addClass("secondaryops"),
                button("%", "percent", () -> press("percent")).addClass("secondaryops"),
                button("÷", "divide", () -> press("divide")).addClass("primaryops"),

                // Row 2: 7 8 9 ×
                button("7", "number-7", () -> pressNumber('7')).addClass("numbers"),
                button("8", "number-8", () -> pressNumber('8')).addClass("numbers"),
                button("9", "number-9", () -> pressNumber('9')).addClass("numbers"),
                button("×", "multiply", () -> press("multiply")).addClass("primaryops"),

                // Row 3: 4 5 6 -
                button("4", "number-4", () -> pressNumber('4')).addClass("numbers"),
                button("5", "number-5", () -> pressNumber('5')).addClass("numbers"),
                button("6", "number-6", () -> pressNumber('6')).addClass("numbers"),
                button("-", "minus", () -> press("minus")).addClass("primaryops"),

                // Row 4: 1 2 3 +
                button("1", "number-1", () -> pressNumber('1')).addClass("numbers"),
                button("2", "number-2", () -> pressNumber('2')).addClass("numbers"),
                button("3", "number-3", () -> pressNumber('3')).addClass("numbers"),
                button("+", "plus", () -> press("plus")).addClass("primaryops"),


                // Row 5: 0, [blank], ., =
                spacer(), button("0", "number-0", () -> pressNumber('0')).addClass("numbers"),
                button(".", "point", () -> press("point")).addClass("numbers"),
                button("=", "equals", () -> press("equals")).addClass("primaryops")
            )
                .id("keypad")

                .gridSize(4,4)
                .gutter(1, 1)

                .fill()
        ))
            .id("calculator")
            .borderless()
            .fill()
            .render(frame, area, context);
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        if (event.isQuit()) {
            return EventResult.UNHANDLED;
        }

        if (event.code() == KeyCode.CHAR) {
            char ch = event.character();
            if (Character.isDigit(ch)) {
                pressNumber(ch);
                return EventResult.HANDLED;
            }
            switch (ch) {
                case '.':
                    press("point");
                    return EventResult.HANDLED;
                case '+':
                    press("plus");
                    return EventResult.HANDLED;
                case '-':
                    press("minus");
                    return EventResult.HANDLED;
                case '*':
                    press("multiply");
                    return EventResult.HANDLED;
                case '/':
                    press("divide");
                    return EventResult.HANDLED;
                case '%':
                    press("percent");
                    return EventResult.HANDLED;
                case '=':
                    press("equals");
                    return EventResult.HANDLED;
                case 'c':
                case 'C':
                    pressClear();
                    return EventResult.HANDLED;
            }
        }

        if (event.isConfirm() || event.isSelect()) {
            // Treat Enter as equals
            press("equals");
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    // ═══════════════════════════════════════════════════════════════
    // Calculator logic (ported from Textual example)
    // ═══════════════════════════════════════════════════════════════

    private boolean showAC() {
        return (value.isEmpty() || value.equals("0")) && numbers.equals("0");
    }

    private String clearLabel() {
        return showAC() ? "AC" : "C";
    }

    private void pressClear() {
        if (showAC()) {
            pressedAc();
        } else {
            pressedC();
        }
    }

    private void pressNumber(char digit) {
        String v = value;
        if ("0".equals(v)) {
            v = "";
        }
        // Python's lstrip("0") behavior, but keep single "0" if that's all we have.
        if (!v.contains(".")) {
            while (v.length() > 1 && v.startsWith("0")) {
                v = v.substring(1);
            }
        }
        v = v + digit;
        value = v;
        numbers = v.isEmpty() ? "0" : v;
    }

    private void press(String action) {
        switch (action) {
            case "plus-minus":
                plusMinusPressed();
                break;
            case "percent":
                percentPressed();
                break;
            case "point":
                pressedPoint();
                break;
            case "ac":
                pressClear();
                break;
            case "c":
                pressedC();
                break;
            case "plus":
            case "minus":
            case "divide":
            case "multiply":
                pressedOp(Operator.fromId(action));
                break;
            case "equals":
                pressedEquals();
                break;
        }
    }

    private void plusMinusPressed() {
        BigDecimal v = parse(value);
        v = v.negate();
        value = format(v);
        numbers = value;
    }

    private void percentPressed() {
        BigDecimal v = parse(value);
        v = v.divide(new BigDecimal("100"), MATH);
        value = format(v);
        numbers = value;
    }

    private void pressedPoint() {
        if (!value.contains(".")) {
            value = (value.isEmpty() ? "0" : value) + ".";
            numbers = value;
        }
    }

    private void pressedAc() {
        value = "";
        left = BigDecimal.ZERO;
        right = BigDecimal.ZERO;
        operator = Operator.PLUS;
        numbers = "0";
    }

    private void pressedC() {
        value = "";
        numbers = "0";
    }

    private void pressedOp(Operator op) {
        right = parse(value);
        doMath();
        operator = op;
    }

    private void pressedEquals() {
        if (!value.isEmpty()) {
            right = parse(value);
        }
        doMath();
    }

    private void doMath() {
        try {
            switch (operator) {
                case PLUS:
                    left = left.add(right, MATH);
                    break;
                case MINUS:
                    left = left.subtract(right, MATH);
                    break;
                case DIVIDE:
                    left = left.divide(right, MATH);
                    break;
                case MULTIPLY:
                    left = left.multiply(right, MATH);
                    break;
            }
            numbers = format(left);
            value = "";
        } catch (Exception e) {
            numbers = "Error";
            value = "";
            left = BigDecimal.ZERO;
            right = BigDecimal.ZERO;
            operator = Operator.PLUS;
        }
    }

    private static BigDecimal parse(String s) {
        if (s == null || s.isEmpty() || s.equals(".")) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(s);
    }

    private static String format(BigDecimal v) {
        if (v == null) {
            return "0";
        }
        BigDecimal stripped = v.stripTrailingZeros();
        String out = stripped.toPlainString();
        // Normalize "-0" to "0"
        if (out.equals("-0")) {
            return "0";
        }
        return out;
    }

    private enum Operator {
        PLUS, MINUS, DIVIDE, MULTIPLY;

        static Operator fromId(String id) {
            String k = id.toLowerCase(Locale.ROOT);
            switch (k) {
                case "plus":
                    return PLUS;
                case "minus":
                    return MINUS;
                case "divide":
                    return DIVIDE;
                case "multiply":
                    return MULTIPLY;
                default:
                    return PLUS;
            }
        }
    }

    /**
     * A CSS-stylable element that renders {@link BoxText} and respects {@code text-align}.
     */
    private static final class BoxTextDisplay extends StyledElement<BoxTextDisplay> {
        private final String text;

        private BoxTextDisplay(String text) {
            this.text = text != null ? text : "";
        }

        @Override
        public String styleType() {
            return "Digits";
        }

        @Override
        protected void renderContent(Frame frame, Rect area, RenderContext context) {
            // Fill background with current style (from parent Panel/#numbers)
            frame.buffer().setStyle(area, context.currentStyle());

            Alignment align = styleResolver(context).resolve(StandardProperties.TEXT_ALIGN, Alignment.RIGHT);

            // Render BoxText in-place (no extra block here; parent panel provides border/padding)
            BoxText.builder()
                .text(text)
                .style(context.currentStyle())
                .uppercase(true)
                .alignment(align)
                .build()
                .render(area, frame.buffer());
        }
    }

    /**
     * A small focusable/clickable button element for the calculator keypad.
     */
    private static final class CalcButton extends StyledElement<CalcButton> {
        private final String label;
        private final Runnable onPress;

        private CalcButton(String label, Runnable onPress) {
            this.label = label;
            this.onPress = onPress;
            focusable();
        }

        @Override
        public String styleType() {
            return "CalcButton";
        }

        @Override
        public EventResult handleMouseEvent(MouseEvent event) {
            if (event.isClick()) {
                onPress.run();
                return EventResult.HANDLED;
            }
            return super.handleMouseEvent(event);
        }

        @Override
        public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
            if (focused && (event.isSelect() || event.isConfirm())) {
                onPress.run();
                return EventResult.HANDLED;
            }
            return super.handleKeyEvent(event, focused);
        }

        @Override
        protected void renderContent(Frame frame, Rect area, RenderContext context) {
            // Draw a rounded block for the button.
            Block block = Block.builder()
                .borders(Borders.ALL)
                .style(context.currentStyle())
                .styleResolver(styleResolver(context))
                .build();
            frame.renderWidget(block, area);

            Rect inner = block.inner(area);
            if (inner.isEmpty()) {
                return;
            }

            Paragraph p = Paragraph.builder()
                .text(Text.from(label))
                .alignment(Alignment.CENTER)
                .style(context.currentStyle())
                .build();
            frame.renderWidget(p, inner);
        }
    }

    private static CalcButton button(String label, String id, Runnable onPress) {
        return new CalcButton(label, onPress).id(id);
    }
}

