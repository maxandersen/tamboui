/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.rewrite.keyevent;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

/**
 * Marks usages of {@code KeyEvent.character()} so breaking API migrations can be reviewed.
 */
public final class FindKeyEventCharacterUsages extends Recipe {

    /**
     * Creates the recipe.
     */
    public FindKeyEventCharacterUsages() {
    }

    private static final MethodMatcher CHARACTER = new MethodMatcher("dev.tamboui.tui.event.KeyEvent character()");

    @Override
    public String getDisplayName() {
        return "Find KeyEvent.character() usages";
    }

    @Override
    public String getDescription() {
        return "Marks every usage of KeyEvent.character() so it can be migrated manually or by follow-up recipes.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation visited = super.visitMethodInvocation(method, ctx);
                if (CHARACTER.matches(visited)) {
                    return SearchResult.found(visited);
                }
                return visited;
            }
        };
    }
}
