/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tree;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TreeState}.
 */
class TreeStateTest {

    @Test
    @DisplayName("Default state has zero selection and offset")
    void defaultState() {
        TreeState state = new TreeState();

        assertThat(state.selected()).isZero();
        assertThat(state.offset()).isZero();
    }

    @Test
    @DisplayName("select sets selected index")
    void select() {
        TreeState state = new TreeState();

        state.select(5);
        assertThat(state.selected()).isEqualTo(5);
    }

    @Test
    @DisplayName("select clamps negative values to zero")
    void selectClampsNegative() {
        TreeState state = new TreeState();

        state.select(-10);
        assertThat(state.selected()).isZero();
    }

    @Test
    @DisplayName("select returns this for chaining")
    void selectReturnsThis() {
        TreeState state = new TreeState();

        assertThat(state.select(3)).isSameAs(state);
    }

    @Test
    @DisplayName("offset sets scroll offset")
    void offset() {
        TreeState state = new TreeState();

        state.offset(10);
        assertThat(state.offset()).isEqualTo(10);
    }

    @Test
    @DisplayName("offset clamps negative values to zero")
    void offsetClampsNegative() {
        TreeState state = new TreeState();

        state.offset(-5);
        assertThat(state.offset()).isZero();
    }

    @Test
    @DisplayName("offset returns this for chaining")
    void offsetReturnsThis() {
        TreeState state = new TreeState();

        assertThat(state.offset(5)).isSameAs(state);
    }

    @Test
    @DisplayName("selectPrevious decrements selection")
    void selectPrevious() {
        TreeState state = new TreeState();
        state.select(5);

        state.selectPrevious();
        assertThat(state.selected()).isEqualTo(4);
    }

    @Test
    @DisplayName("selectPrevious stops at zero")
    void selectPreviousStopsAtZero() {
        TreeState state = new TreeState();
        state.select(0);

        state.selectPrevious();
        assertThat(state.selected()).isZero();
    }

    @Test
    @DisplayName("selectNext increments selection")
    void selectNext() {
        TreeState state = new TreeState();
        state.select(0);

        state.selectNext(10);
        assertThat(state.selected()).isEqualTo(1);
    }

    @Test
    @DisplayName("selectNext stops at max index")
    void selectNextStopsAtMax() {
        TreeState state = new TreeState();
        state.select(5);

        state.selectNext(5);
        assertThat(state.selected()).isEqualTo(5);
    }

    @Test
    @DisplayName("selectFirst resets to zero")
    void selectFirst() {
        TreeState state = new TreeState();
        state.select(10);
        state.offset(20);

        state.selectFirst();

        assertThat(state.selected()).isZero();
        assertThat(state.offset()).isZero();
    }

    @Test
    @DisplayName("selectLast sets to last index")
    void selectLast() {
        TreeState state = new TreeState();

        state.selectLast(15);
        assertThat(state.selected()).isEqualTo(15);
    }

    @Test
    @DisplayName("selectLast clamps negative to zero")
    void selectLastClampsNegative() {
        TreeState state = new TreeState();

        state.selectLast(-1);
        assertThat(state.selected()).isZero();
    }

    @Test
    @DisplayName("scrollToSelected scrolls up when selection above viewport")
    void scrollToSelectedAbove() {
        TreeState state = new TreeState();
        state.offset(10);

        // Selection at row 5, height 1, viewport 10, total 50
        state.scrollToSelected(5, 1, 10, 50);

        assertThat(state.offset()).isEqualTo(5);
    }

    @Test
    @DisplayName("scrollToSelected scrolls down when selection below viewport")
    void scrollToSelectedBelow() {
        TreeState state = new TreeState();
        state.offset(0);

        // Selection at row 15, height 1, viewport 10, total 50
        state.scrollToSelected(15, 1, 10, 50);

        assertThat(state.offset()).isEqualTo(6); // 15 + 1 - 10 = 6
    }

    @Test
    @DisplayName("scrollToSelected does not scroll when selection visible")
    void scrollToSelectedVisible() {
        TreeState state = new TreeState();
        state.offset(5);

        // Selection at row 8, height 1, viewport 10, total 50
        state.scrollToSelected(8, 1, 10, 50);

        assertThat(state.offset()).isEqualTo(5); // unchanged
    }

    @Test
    @DisplayName("scrollToSelected clamps offset to valid range")
    void scrollToSelectedClamps() {
        TreeState state = new TreeState();
        state.offset(100);

        // totalHeight=20, viewport=10 -> max offset is 10
        state.scrollToSelected(5, 1, 10, 20);

        assertThat(state.offset()).isLessThanOrEqualTo(10);
    }
}
