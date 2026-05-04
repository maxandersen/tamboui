/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.rewrite.keyevent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class TambouiKeyEventBreakingApiMigrationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package dev.tamboui.tui.event;\n" +
                "\n" +
                "public enum KeyCode {\n" +
                "    CHAR\n" +
                "}\n",
            "package dev.tamboui.tui.event;\n" +
                "\n" +
                "public class KeyEvent {\n" +
                "    public boolean isChar(char c) {\n" +
                "        return true;\n" +
                "    }\n" +
                "\n" +
                "    public char character() {\n" +
                "        return 'x';\n" +
                "    }\n" +
                "}\n"
        ));
        spec.afterTypeValidationOptions(TypeValidation.none());
        spec.recipe(new TambouiKeyEventBreakingApiMigration());
    }

    @Test
    @DisplayName("rewrites the common direct comparison pattern")
    void rewritesCommonDirectComparisonPattern() {
        rewriteRun(
            java(
                "import dev.tamboui.tui.event.KeyEvent;\n" +
                    "\n" +
                    "class Test {\n" +
                    "    boolean f(KeyEvent event) {\n" +
                    "        return event.character() == 'x';\n" +
                    "    }\n" +
                    "}\n",
                "import dev.tamboui.tui.event.KeyEvent;\n" +
                    "\n" +
                    "class Test {\n" +
                    "    boolean f(KeyEvent event) {\n" +
                    "        return event.isChar('x');\n" +
                    "    }\n" +
                    "}\n"
            )
        );
    }
}
