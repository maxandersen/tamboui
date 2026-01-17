/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class SelectorTest {

    @Test
    void typeSelectorMatchesElement() {
        TypeSelector selector = new TypeSelector("Panel");
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void typeSelectorDoesNotMatchDifferentType() {
        TypeSelector selector = new TypeSelector("Panel");
        Styleable element = createStyleable("Button", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void typeSelectorMatchesSubclass() {
        TypeSelector panelSelector = new TypeSelector("BasePanel");
        MyPanel myPanel = new MyPanel();

        // MyPanel extends BasePanel, so Panel selector should match MyPanel
        assertThat(panelSelector.matches(myPanel, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void typeSelectorMatchesExactTypeForSubclass() {
        TypeSelector myPanelSelector = new TypeSelector("MyPanel");
        MyPanel myPanel = new MyPanel();

        // MyPanel selector should also match MyPanel
        assertThat(myPanelSelector.matches(myPanel, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void styleTypesReturnsCorrectOrder() {
        MyPanel myPanel = new MyPanel();
        List<String> types = Styleable.styleTypesOf(myPanel);

        // Order should be parent first (lower precedence), child last (higher precedence)
        assertThat(types).containsExactly("BasePanel", "MyPanel");
    }

    @Test
    void styleTypesForSimpleElementReturnsOnlyOneType() {
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());
        List<String> types = Styleable.styleTypesOf(element);

        // TestStyleable has no Styleable parent classes
        assertThat(types).containsExactly("Panel");
    }

    @Test
    void typeSelectorSpecificity() {
        TypeSelector selector = new TypeSelector("Panel");
        assertThat(selector.specificity()).isEqualTo(1);
    }

    @Test
    void idSelectorMatchesElement() {
        IdSelector selector = new IdSelector("sidebar");
        Styleable element = createStyleable("Panel", "sidebar", Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void idSelectorDoesNotMatchWithoutId() {
        IdSelector selector = new IdSelector("sidebar");
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void idSelectorSpecificity() {
        IdSelector selector = new IdSelector("sidebar");
        assertThat(selector.specificity()).isEqualTo(100);
    }

    @Test
    void classSelectorMatchesElement() {
        ClassSelector selector = new ClassSelector("primary");
        Styleable element = createStyleable("Panel", null, new HashSet<>(Arrays.asList("primary", "large")));

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void classSelectorDoesNotMatchWithoutClass() {
        ClassSelector selector = new ClassSelector("primary");
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void classSelectorSpecificity() {
        ClassSelector selector = new ClassSelector("primary");
        assertThat(selector.specificity()).isEqualTo(10);
    }

    @Test
    void universalSelectorMatchesAnyElement() {
        Styleable element1 = createStyleable("Panel", null, Collections.<String>emptySet());
        Styleable element2 = createStyleable("Button", "id", new HashSet<>(Arrays.asList("class")));

        assertThat(UniversalSelector.INSTANCE.matches(element1, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
        assertThat(UniversalSelector.INSTANCE.matches(element2, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void universalSelectorSpecificity() {
        assertThat(UniversalSelector.INSTANCE.specificity()).isEqualTo(0);
    }

    @Test
    void pseudoClassSelectorMatchesFocused() {
        PseudoClassSelector selector = new PseudoClassSelector("focus");
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.ofFocused(), Collections.<Styleable>emptyList())).isTrue();
        assertThat(selector.matches(element, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void pseudoClassSelectorMatchesHovered() {
        PseudoClassSelector selector = new PseudoClassSelector("hover");
        Styleable element = createStyleable("Panel", null, Collections.<String>emptySet());

        assertThat(selector.matches(element, PseudoClassState.ofHovered(), Collections.<Styleable>emptyList())).isTrue();
    }

    @Test
    void pseudoClassSelectorSpecificity() {
        PseudoClassSelector selector = new PseudoClassSelector("focus");
        assertThat(selector.specificity()).isEqualTo(10);
    }

    @Test
    void compoundSelectorMatchesAllParts() {
        List<Selector> parts = Arrays.<Selector>asList(
                new TypeSelector("Panel"),
                new ClassSelector("primary")
        );
        CompoundSelector selector = new CompoundSelector(parts);

        Styleable matching = createStyleable("Panel", null, new HashSet<>(Arrays.asList("primary")));
        Styleable wrongType = createStyleable("Button", null, new HashSet<>(Arrays.asList("primary")));
        Styleable wrongClass = createStyleable("Panel", null, new HashSet<>(Arrays.asList("secondary")));

        assertThat(selector.matches(matching, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isTrue();
        assertThat(selector.matches(wrongType, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
        assertThat(selector.matches(wrongClass, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void compoundSelectorSpecificity() {
        List<Selector> parts = Arrays.<Selector>asList(
                new TypeSelector("Panel"),
                new ClassSelector("primary"),
                new IdSelector("main")
        );
        CompoundSelector selector = new CompoundSelector(parts);

        // 100 (id) + 10 (class) + 1 (type) = 111
        assertThat(selector.specificity()).isEqualTo(111);
    }

    @Test
    void descendantSelectorMatchesNestedElement() {
        DescendantSelector selector = new DescendantSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );

        Styleable panel = createStyleable("Panel", null, Collections.<String>emptySet());
        Styleable button = createStyleable("Button", null, Collections.<String>emptySet());
        List<Styleable> ancestors = Arrays.asList(panel);

        assertThat(selector.matches(button, PseudoClassState.NONE, ancestors)).isTrue();
    }

    @Test
    void descendantSelectorMatchesDeeplyNestedElement() {
        DescendantSelector selector = new DescendantSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );

        Styleable panel = createStyleable("Panel", null, Collections.<String>emptySet());
        Styleable container = createStyleable("Container", null, Collections.<String>emptySet());
        Styleable button = createStyleable("Button", null, Collections.<String>emptySet());
        List<Styleable> ancestors = Arrays.asList(panel, container);

        assertThat(selector.matches(button, PseudoClassState.NONE, ancestors)).isTrue();
    }

    @Test
    void descendantSelectorDoesNotMatchWithoutAncestor() {
        DescendantSelector selector = new DescendantSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );

        Styleable button = createStyleable("Button", null, Collections.<String>emptySet());

        assertThat(selector.matches(button, PseudoClassState.NONE, Collections.<Styleable>emptyList())).isFalse();
    }

    @Test
    void childSelectorMatchesDirectChild() {
        ChildSelector selector = new ChildSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );

        Styleable panel = createStyleable("Panel", null, Collections.<String>emptySet());
        Styleable button = createStyleable("Button", null, Collections.<String>emptySet());
        List<Styleable> ancestors = Arrays.asList(panel);

        assertThat(selector.matches(button, PseudoClassState.NONE, ancestors)).isTrue();
    }

    @Test
    void childSelectorDoesNotMatchGrandchild() {
        ChildSelector selector = new ChildSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );

        Styleable panel = createStyleable("Panel", null, Collections.<String>emptySet());
        Styleable container = createStyleable("Container", null, Collections.<String>emptySet());
        Styleable button = createStyleable("Button", null, Collections.<String>emptySet());
        List<Styleable> ancestors = Arrays.asList(panel, container);

        assertThat(selector.matches(button, PseudoClassState.NONE, ancestors)).isFalse();
    }

    @Test
    void selectorToCss() {
        assertThat(new TypeSelector("Panel").toCss()).isEqualTo("Panel");
        assertThat(new IdSelector("sidebar").toCss()).isEqualTo("#sidebar");
        assertThat(new ClassSelector("primary").toCss()).isEqualTo(".primary");
        assertThat(UniversalSelector.INSTANCE.toCss()).isEqualTo("*");
        assertThat(new PseudoClassSelector("focus").toCss()).isEqualTo(":focus");

        CompoundSelector compound = new CompoundSelector(Arrays.<Selector>asList(
                new TypeSelector("Panel"),
                new ClassSelector("primary")
        ));
        assertThat(compound.toCss()).isEqualTo("Panel.primary");

        DescendantSelector desc = new DescendantSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );
        assertThat(desc.toCss()).isEqualTo("Panel Button");

        ChildSelector child = new ChildSelector(
                new TypeSelector("Panel"),
                new TypeSelector("Button")
        );
        assertThat(child.toCss()).isEqualTo("Panel > Button");
    }

    private Styleable createStyleable(String type, String id, Set<String> classes) {
        return new TestStyleable(type, id, classes);
    }

    private static class TestStyleable implements Styleable {
        private final String type;
        private final String id;
        private final Set<String> classes;

        TestStyleable(String type, String id, Set<String> classes) {
            this.type = type;
            this.id = id;
            this.classes = classes;
        }

        @Override
        public String styleType() {
            return type;
        }

        @Override
        public Optional<String> cssId() {
            return Optional.ofNullable(id);
        }

        @Override
        public Set<String> cssClasses() {
            return classes;
        }

        @Override
        public Optional<Styleable> cssParent() {
            return Optional.empty();
        }
    }

    /// Base panel class for testing type hierarchy matching.
    private static class BasePanel implements Styleable {
        @Override
        public Optional<String> cssId() {
            return Optional.empty();
        }

        @Override
        public Set<String> cssClasses() {
            return Collections.emptySet();
        }

        @Override
        public Optional<Styleable> cssParent() {
            return Optional.empty();
        }
    }

    /// Subclass of BasePanel for testing type hierarchy matching.
    /// CSS selectors for both "BasePanel" and "MyPanel" should match instances of this class.
    private static class MyPanel extends BasePanel {
        // Inherits all methods from BasePanel
        // styleType() defaults to "MyPanel" (getClass().getSimpleName())
    }
}
