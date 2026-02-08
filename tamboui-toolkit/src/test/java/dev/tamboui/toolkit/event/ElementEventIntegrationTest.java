/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.focus.FocusManager;

import static org.assertj.core.api.Assertions.assertThat;

class ElementEventIntegrationTest {

    @Test
    @DisplayName("styled elements register event handlers each render")
    void styledElementsRegisterHandlers() {
        ElementEventBus bus = new ElementEventBus();
        DefaultRenderContext context = createContext(bus);
        Frame frame = Frame.forTesting(Buffer.empty(Rect.of(10, 1)));

        AtomicBoolean handled = new AtomicBoolean(false);
        ReceivingElement element = new ReceivingElement(handled).id("receiver");

        element.render(frame, frame.area(), context);

        bus.emitTo("receiver", new TestEvent("sender", "payload"));

        assertThat(handled.get()).isTrue();
    }

    @Test
    @DisplayName("styled elements can emit element events")
    void styledElementsCanEmitEvents() {
        ElementEventBus bus = new ElementEventBus();
        DefaultRenderContext context = createContext(bus);
        Frame frame = Frame.forTesting(Buffer.empty(Rect.of(10, 1)));

        AtomicReference<String> captured = new AtomicReference<>();
        bus.subscribe(TestEvent.class, event -> {
            captured.set(event.payload());
            return EventResult.HANDLED;
        });

        EmittingElement element = new EmittingElement().id("emitter");
        element.render(frame, frame.area(), context);

        element.fire("payload");

        assertThat(captured.get()).isEqualTo("payload");
    }

    private DefaultRenderContext createContext(ElementEventBus bus) {
        FocusManager focusManager = new FocusManager();
        ElementRegistry registry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, registry);
        return new DefaultRenderContext(focusManager, router, bus);
    }

    private static final class ReceivingElement extends StyledElement<ReceivingElement> {
        private final AtomicBoolean handled;

        private ReceivingElement(AtomicBoolean handled) {
            this.handled = handled;
            onEvent(TestEvent.class, event -> {
                handled.set(true);
                return EventResult.HANDLED;
            });
        }

        @Override
        protected void renderContent(Frame frame, Rect area, RenderContext context) {
            // no-op
        }
    }

    private static final class EmittingElement extends StyledElement<EmittingElement> {
        @Override
        protected void renderContent(Frame frame, Rect area, RenderContext context) {
            // no-op
        }

        private void fire(String payload) {
            emit(new TestEvent(elementId, payload));
        }
    }

    private static final class TestEvent implements ElementEvent {
        private final String sourceId;
        private final String payload;

        private TestEvent(String sourceId, String payload) {
            this.sourceId = sourceId;
            this.payload = payload;
        }

        @Override
        public String sourceId() {
            return sourceId;
        }

        public String payload() {
            return payload;
        }
    }
}
