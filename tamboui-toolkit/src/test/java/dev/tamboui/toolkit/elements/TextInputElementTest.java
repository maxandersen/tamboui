/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.event.ElementEventBus;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.EventRouter;
import dev.tamboui.toolkit.event.TextInputChangedEvent;
import dev.tamboui.toolkit.event.TextInputSubmittedEvent;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TextInputElement.
 */
class TextInputElementTest {

    @Test
    @DisplayName("styleAttributes exposes title")
    void styleAttributes_exposesTitle() {
        assertThat(textInput().title("Username").styleAttributes()).containsEntry("title", "Username");
    }

    @Test
    @DisplayName("styleAttributes exposes placeholder")
    void styleAttributes_exposesPlaceholder() {
        assertThat(textInput().placeholder("Enter...").styleAttributes()).containsEntry("placeholder", "Enter...");
    }

    @Test
    @DisplayName("styleAttributes with empty placeholder does not expose it")
    void styleAttributes_emptyPlaceholderNotExposed() {
        assertThat(textInput().title("Name").styleAttributes()).doesNotContainKey("placeholder");
    }

    @Test
    @DisplayName("Attribute selector [title] affects TextInput border color")
    void attributeSelector_title_affectsBorderColor() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", "TextInputElement[title=\"Username\"] { border-color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        textInput().title("Username").render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.CYAN);
    }

    @Test
    @DisplayName("Attribute selector [placeholder] affects TextInput border color")
    void attributeSelector_placeholder_affectsBorderColor() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", "TextInputElement[placeholder=\"Enter...\"] { border-color: yellow; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Need rounded() to enable border rendering for border-color to take effect
        textInput().placeholder("Enter...").rounded().render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.YELLOW);
    }

    @Test
    @DisplayName("Emits TextInputChangedEvent when text changes")
    void emitsTextInputChangedEvent() {
        ElementEventBus bus = new ElementEventBus();
        DefaultRenderContext context = createContext(bus);

        AtomicReference<TextInputChangedEvent> captured = new AtomicReference<>();
        bus.subscribe(TextInputChangedEvent.class, event -> {
            captured.set(event);
            return EventResult.HANDLED;
        });

        Rect area = new Rect(0, 0, 10, 1);
        Frame frame = Frame.forTesting(Buffer.empty(area));
        TextInputElement element = textInput().id("input");

        element.render(frame, area, context);
        element.handleKeyEvent(new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, 'A'), true);

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().sourceId()).isEqualTo("input");
        assertThat(captured.get().text()).isEqualTo("A");
    }

    @Test
    @DisplayName("Emits TextInputSubmittedEvent on confirm")
    void emitsTextInputSubmittedEvent() {
        ElementEventBus bus = new ElementEventBus();
        DefaultRenderContext context = createContext(bus);

        AtomicReference<TextInputSubmittedEvent> captured = new AtomicReference<>();
        bus.subscribe(TextInputSubmittedEvent.class, event -> {
            captured.set(event);
            return EventResult.HANDLED;
        });

        Rect area = new Rect(0, 0, 10, 1);
        Frame frame = Frame.forTesting(Buffer.empty(area));
        TextInputElement element = textInput().id("submit");

        element.render(frame, area, context);
        element.handleKeyEvent(new KeyEvent(KeyCode.ENTER, KeyModifiers.NONE, '\n'), true);

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().sourceId()).isEqualTo("submit");
    }

    private DefaultRenderContext createContext(ElementEventBus bus) {
        FocusManager focusManager = new FocusManager();
        ElementRegistry registry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, registry);
        return new DefaultRenderContext(focusManager, router, bus);
    }
}
