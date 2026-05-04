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

class MigrateKeyEventCharacterComparisonTest implements RewriteTest {

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
                "\n" +
                "    public KeyCode code() {\n" +
                "        return KeyCode.CHAR;\n" +
                "    }\n" +
                "}\n"
        ));
        spec.afterTypeValidationOptions(TypeValidation.none());
        spec.recipe(new MigrateKeyEventCharacterComparison());
    }

    @Test
    @DisplayName("rewrites direct character equality to isChar")
    void rewritesDirectCharacterEquality() {
        rewriteRun(
            java(
                "import dev.tamboui.tui.event.KeyEvent;\n" +
                    "\n" +
                    "class Test {\n" +
                    "    boolean f(KeyEvent event) {\n" +
                    "        if (event.character() == 'x') {\n" +
                    "            return true;\n" +
                    "        }\n" +
                    "        return false;\n" +
                    "    }\n" +
                    "}\n",
                "import dev.tamboui.tui.event.KeyEvent;\n" +
                    "\n" +
                    "class Test {\n" +
                    "    boolean f(KeyEvent event) {\n" +
                    "        if (event.isChar('x')) {\n" +
                    "            return true;\n" +
                    "        }\n" +
                    "        return false;\n" +
                    "    }\n" +
                    "}\n"
            )
        );
    }

    @Test
    @DisplayName("rewrites guarded comparisons without touching the guard")
    void rewritesGuardedComparisonsWithoutTouchingTheGuard() {
        rewriteRun(
            java(
                "import dev.tamboui.tui.event.KeyCode;\n" +
                    "import dev.tamboui.tui.event.KeyEvent;\n" +
                    "\n" +
                    "class Test {\n" +
                    "    boolean f(KeyEvent event) {\n" +
                    "        if (event.code() == KeyCode.CHAR && event.character() == 'x') {\n" +
                    "            return true;\n" +
                    "        }\n" +
                    "        return false;\n" +
                    "    }\n" +
                    "}\n",
                "import dev.tamboui.tui.event.KeyCode;\n" +
                    "import dev.tamboui.tui.event.KeyEvent;\n" +
                    "\n" +
                    "class Test {\n" +
                    "    boolean f(KeyEvent event) {\n" +
                    "        if (event.code() == KeyCode.CHAR && event.isChar('x')) {\n" +
                    "            return true;\n" +
                    "        }\n" +
                    "        return false;\n" +
                    "    }\n" +
                    "}\n"
            )
        );
    }
}
