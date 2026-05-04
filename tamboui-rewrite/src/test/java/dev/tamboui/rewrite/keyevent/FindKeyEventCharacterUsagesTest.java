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

class FindKeyEventCharacterUsagesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package dev.tamboui.tui.event;\n" +
                "\n" +
                "public class KeyEvent {\n" +
                "    public char character() {\n" +
                "        return 'x';\n" +
                "    }\n" +
                "}\n"
        ));
        spec.afterTypeValidationOptions(TypeValidation.none());
        spec.expectedCyclesThatMakeChanges(1);
        spec.recipe(new FindKeyEventCharacterUsages());
    }

    @Test
    @DisplayName("finds KeyEvent.character() usages")
    void findsKeyEventCharacterUsages() {
        rewriteRun(
            java(
                "import dev.tamboui.tui.event.KeyEvent;\n" +
                    "\n" +
                    "class Test {\n" +
                    "    char f(KeyEvent event) {\n" +
                    "        return event.character();\n" +
                    "    }\n" +
                    "}\n",
                "import dev.tamboui.tui.event.KeyEvent;\n" +
                    "\n" +
                    "class Test {\n" +
                    "    char f(KeyEvent event) {\n" +
                    "        return /*~~>*/event.character();\n" +
                    "    }\n" +
                    "}\n"
            )
        );
    }
}
