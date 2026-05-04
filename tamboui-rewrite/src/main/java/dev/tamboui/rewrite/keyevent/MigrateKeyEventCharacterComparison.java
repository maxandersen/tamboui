/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.rewrite.keyevent;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

/**
 * Rewrites direct {@code KeyEvent.character() == 'x'} comparisons to {@code KeyEvent.isChar('x')}.
 */
public final class MigrateKeyEventCharacterComparison extends Recipe {

    private static final MethodMatcher CHARACTER = new MethodMatcher("dev.tamboui.tui.event.KeyEvent character()");
    private static final JavaTemplate IS_CHAR = JavaTemplate.builder("#{any(dev.tamboui.tui.event.KeyEvent)}.isChar(#{any()})").build();

    /**
     * Creates the recipe.
     */
    public MigrateKeyEventCharacterComparison() {
    }

    @Override
    public String getDisplayName() {
        return "Rewrite KeyEvent.character() comparisons";
    }

    @Override
    public String getDescription() {
        return "Rewrites direct KeyEvent.character() equality checks to the new KeyEvent.isChar(char) API.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                J visited = super.visitBinary(binary, ctx);
                if (!(visited instanceof J.Binary)) {
                    return visited;
                }

                J.Binary visitedBinary = (J.Binary) visited;
                if (visitedBinary.getOperator() != J.Binary.Type.Equal) {
                    return visitedBinary;
                }

                J rewritten = rewriteDirect(visitedBinary);
                return rewritten != null ? rewritten : visitedBinary;
            }

            private J rewriteDirect(J.Binary binary) {
                J.MethodInvocation characterCall = characterCall(binary.getLeft());
                J.Literal literal = literal(binary.getRight());
                if (characterCall == null || literal == null) {
                    characterCall = characterCall(binary.getRight());
                    literal = literal(binary.getLeft());
                }
                if (characterCall == null || literal == null || characterCall.getSelect() == null) {
                    return null;
                }
                return IS_CHAR.apply(getCursor(), binary.getCoordinates().replace(), characterCall.getSelect(), literal);
            }

            private J.MethodInvocation characterCall(J expression) {
                if (!(expression instanceof J.MethodInvocation)) {
                    return null;
                }
                J.MethodInvocation method = (J.MethodInvocation) expression;
                return CHARACTER.matches(method) ? method : null;
            }

            private J.Literal literal(J expression) {
                if (!(expression instanceof J.Literal)) {
                    return null;
                }
                J.Literal literal = (J.Literal) expression;
                return literal.getValue() instanceof Character ? literal : null;
            }
        };
    }
}
