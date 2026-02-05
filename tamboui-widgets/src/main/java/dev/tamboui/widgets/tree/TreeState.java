/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tree;

/**
 * State for a tree widget, tracking selection and scroll position.
 * <p>
 * The state tracks:
 * <ul>
 *   <li>Selected index in the flattened visible node list</li>
 *   <li>Scroll offset for viewport scrolling</li>
 * </ul>
 */
public final class TreeState {

    private int selected;
    private int scrollOffset;

    /**
     * Creates a new tree state with default values.
     */
    public TreeState() {
        this.selected = 0;
        this.scrollOffset = 0;
    }

    /**
     * Returns the selected index in the flattened visible list.
     *
     * @return the selected index
     */
    public int selected() {
        return selected;
    }

    /**
     * Sets the selected index.
     *
     * @param index the index to select
     * @return this state for chaining
     */
    public TreeState select(int index) {
        this.selected = Math.max(0, index);
        return this;
    }

    /**
     * Returns the scroll offset in rows.
     *
     * @return the scroll offset
     */
    public int offset() {
        return scrollOffset;
    }

    /**
     * Sets the scroll offset.
     *
     * @param offset the scroll offset
     * @return this state for chaining
     */
    public TreeState offset(int offset) {
        this.scrollOffset = Math.max(0, offset);
        return this;
    }

    /**
     * Adjusts scroll offset to keep the selected item visible within the viewport.
     *
     * @param selectedTop the Y position of the selected item (from top of content)
     * @param selectedHeight the height of the selected item
     * @param viewportHeight the visible viewport height
     * @param totalHeight the total content height
     */
    public void scrollToSelected(int selectedTop, int selectedHeight, int viewportHeight, int totalHeight) {
        int selectedBottom = selectedTop + selectedHeight;

        if (selectedTop < scrollOffset) {
            scrollOffset = selectedTop;
        } else if (selectedBottom > scrollOffset + viewportHeight) {
            scrollOffset = selectedBottom - viewportHeight;
        }

        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, totalHeight - viewportHeight)));
    }

    /**
     * Selects the previous item if possible.
     */
    public void selectPrevious() {
        if (selected > 0) {
            selected--;
        }
    }

    /**
     * Selects the next item if possible.
     *
     * @param maxIndex the maximum valid index
     */
    public void selectNext(int maxIndex) {
        if (selected < maxIndex) {
            selected++;
        }
    }

    /**
     * Selects the first item.
     */
    public void selectFirst() {
        selected = 0;
        scrollOffset = 0;
    }

    /**
     * Selects the last item.
     *
     * @param lastIndex the index of the last item
     */
    public void selectLast(int lastIndex) {
        selected = Math.max(0, lastIndex);
    }
}
