/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tree;

import java.util.List;

/**
 * Interface for accessing tree data in a decoupled manner.
 * <p>
 * A TreeModel provides abstract access to a tree structure without requiring
 * a specific node implementation. This allows working with domain objects
 * directly without wrapping them in a specialized tree node class.
 *
 * <pre>{@code
 * // Example: Using TreeModel with File system
 * TreeModel<File> fileTree = new TreeModel<File>() {
 *     private final Set<File> expanded = new HashSet<>();
 *
 *     public File root() { return new File("/home"); }
 *     public List<File> children(File dir) {
 *         return Arrays.asList(dir.listFiles());
 *     }
 *     public boolean isLeaf(File f) { return f.isFile(); }
 *     public boolean isExpanded(File f) { return expanded.contains(f); }
 *     public void setExpanded(File f, boolean exp) {
 *         if (exp) expanded.add(f); else expanded.remove(f);
 *     }
 * };
 * }</pre>
 *
 * @param <T> the type of tree nodes
 * @see TreeNode
 */
public interface TreeModel<T> {

    /**
     * Returns the root node of the tree.
     *
     * @return the root node
     */
    T root();

    /**
     * Returns the children of the given parent node.
     *
     * @param parent the parent node
     * @return the list of child nodes, may be empty but not null
     */
    List<T> children(T parent);

    /**
     * Returns whether the given node is a leaf (has no children).
     *
     * @param node the node to check
     * @return true if the node is a leaf
     */
    boolean isLeaf(T node);

    /**
     * Returns whether the given node is currently expanded.
     *
     * @param node the node to check
     * @return true if the node is expanded
     */
    boolean isExpanded(T node);

    /**
     * Sets the expanded state of a node.
     *
     * @param node the node to modify
     * @param expanded true to expand, false to collapse
     */
    void setExpanded(T node, boolean expanded);
}
