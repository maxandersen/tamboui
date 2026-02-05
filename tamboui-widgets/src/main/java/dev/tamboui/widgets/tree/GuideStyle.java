/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tree;

/**
 * Style for tree guide/branch characters.
 * <p>
 * Guide characters show the hierarchical structure of the tree, connecting
 * parent nodes to their children with visual connectors.
 */
public enum GuideStyle {

    /**
     * Unicode box-drawing characters: ├──, │, └──.
     */
    UNICODE("\u251c\u2500\u2500 ", "\u2502   ", "\u2514\u2500\u2500 ", "    "),

    /**
     * ASCII characters: +--, |, +--.
     */
    ASCII("+-- ", "|   ", "+-- ", "    "),

    /**
     * No guide characters.
     */
    NONE("", "", "", "");

    private final String branch;
    private final String vertical;
    private final String lastBranch;
    private final String space;

    GuideStyle(String branch, String vertical, String lastBranch, String space) {
        this.branch = branch;
        this.vertical = vertical;
        this.lastBranch = lastBranch;
        this.space = space;
    }

    /**
     * Returns the branch connector string (for non-last children).
     *
     * @return the branch string
     */
    public String branch() {
        return branch;
    }

    /**
     * Returns the vertical continuation string.
     *
     * @return the vertical string
     */
    public String vertical() {
        return vertical;
    }

    /**
     * Returns the last-branch connector string.
     *
     * @return the last branch string
     */
    public String lastBranch() {
        return lastBranch;
    }

    /**
     * Returns the space string (for children of last items).
     *
     * @return the space string
     */
    public String space() {
        return space;
    }
}
