/**
 * Syntax highlighting API for TamboUI.
 * <p>
 * This module provides the core API for syntax highlighting, including interfaces
 * for syntax highlighters and providers, as well as shared utilities for parsing
 * Pygments output.
 */
module dev.tamboui.pygments.api {
    requires transitive dev.tamboui.core;

    exports dev.tamboui.pygments;

    uses dev.tamboui.pygments.SyntaxHighlighterProvider;
}
