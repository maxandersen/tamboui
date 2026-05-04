/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.rewrite.keyevent;

import java.util.Arrays;
import java.util.List;

import org.openrewrite.Recipe;

/**
 * Composite recipe for the TamboUI KeyEvent breaking API migration.
 */
public final class TambouiKeyEventBreakingApiMigration extends Recipe {

    /**
     * Creates the recipe.
     */
    public TambouiKeyEventBreakingApiMigration() {
    }

    @Override
    public String getDisplayName() {
        return "TamboUI KeyEvent breaking API migration";
    }

    @Override
    public String getDescription() {
        return "Finds and rewrites direct usages of KeyEvent.character() to the new Unicode-safe API.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.<Recipe>asList(
            new FindKeyEventCharacterUsages(),
            new MigrateKeyEventCharacterComparison()
        );
    }
}
