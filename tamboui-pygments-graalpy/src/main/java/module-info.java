import dev.tamboui.pygments.SyntaxHighlighterProvider;
import dev.tamboui.pygments.graalpy.GraalPySyntaxHighlighterProvider;

/**
 * GraalPy-based syntax highlighting for TamboUI.
 * <p>
 * This module provides a self-contained syntax highlighter using GraalPy
 * (embedded Python runtime). No external Python installation required.
 */
@SuppressWarnings({"requires-transitive-automatic", "requires-automatic"})
module dev.tamboui.pygments.graalpy {
    requires transitive dev.tamboui.pygments.api;
    requires org.graalvm.polyglot;
    requires org.graalvm.python.embedding;

    exports dev.tamboui.pygments.graalpy;

    provides SyntaxHighlighterProvider with GraalPySyntaxHighlighterProvider;
}
