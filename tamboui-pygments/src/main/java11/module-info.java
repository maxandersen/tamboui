import dev.tamboui.pygments.SyntaxHighlighterProvider;
import dev.tamboui.pygments.process.ProcessSyntaxHighlighterProvider;

/**
 * External process (pygmentize) syntax highlighting implementation for TamboUI.
 * <p>
 * This module provides a syntax highlighter that uses the external {@code pygmentize}
 * CLI tool (or {@code uvx}/{@code pipx} fallbacks) for syntax highlighting.
 */
module dev.tamboui.pygments {
    requires transitive dev.tamboui.pygments.api;

    exports dev.tamboui.pygments.process;

    provides SyntaxHighlighterProvider with ProcessSyntaxHighlighterProvider;
}
