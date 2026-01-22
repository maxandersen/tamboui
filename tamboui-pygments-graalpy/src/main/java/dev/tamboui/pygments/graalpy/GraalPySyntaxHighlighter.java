/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments.graalpy;

import dev.tamboui.pygments.RawTokenParser;
import dev.tamboui.pygments.Result;
import dev.tamboui.pygments.SyntaxHighlighter;
import dev.tamboui.pygments.TokenStyleResolver;
import dev.tamboui.text.Text;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.python.embedding.GraalPyResources;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Syntax highlighter implementation using GraalPy (embedded Python runtime).
 * <p>
 * This implementation is self-contained and does not require external Python tools.
 * It uses GraalVM's polyglot capabilities to run Python code directly in the JVM.
 * <p>
 * <b>Note:</b> The first call will be slower due to Python runtime initialization
 * and Pygments module loading. Subsequent calls are fast.
 */
public final class GraalPySyntaxHighlighter implements SyntaxHighlighter {

    private static final String PYTHON = "python";

    private volatile Context context;
    private volatile Value highlightFunction;
    private volatile Value getLexerFunction;
    private volatile boolean initialized;
    private volatile String initError;

    private final Object initLock = new Object();
    private final ExecutorService executor;

    /**
     * Creates a new GraalPy-based syntax highlighter.
     */
    public GraalPySyntaxHighlighter() {
        // Single-threaded executor for Python operations (Python is not thread-safe)
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "graalpy-highlighter");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public Text highlight(String filename, String source) {
        return highlight(filename, source, DEFAULT_TIMEOUT, TokenStyleResolver.defaultResolver());
    }

    @Override
    public Text highlight(String filename, String source, TokenStyleResolver resolver) {
        return highlight(filename, source, DEFAULT_TIMEOUT, resolver);
    }

    @Override
    public Text highlight(String filename, String source, Duration timeout) {
        return highlight(filename, source, timeout, TokenStyleResolver.defaultResolver());
    }

    @Override
    public Text highlight(String filename, String source, Duration timeout, TokenStyleResolver resolver) {
        return highlightWithInfo(filename, source, timeout, resolver).text();
    }

    @Override
    public Result highlightWithInfo(String filename, String source, Duration timeout) {
        return highlightWithInfo(filename, source, timeout, TokenStyleResolver.defaultResolver());
    }

    @Override
    public Result highlightWithInfo(String filename, String source, Duration timeout, TokenStyleResolver resolver) {
        Objects.requireNonNull(resolver, "resolver");

        if (source == null || source.isEmpty()) {
            return new Result(Text.empty(), null, false, null);
        }
        if (filename == null || filename.trim().isEmpty()) {
            return new Result(Text.raw(source), null, false, "filename is required for lexer inference");
        }

        // Try to initialize if not done yet
        if (!ensureInitialized(timeout)) {
            return new Result(Text.raw(source), null, false, initError != null ? initError : "GraalPy initialization failed");
        }

        // First try extension-based lexer (fast, no Python call)
        String lexer = inferLexerFromExtension(filename);

        // If no lexer found, try Python-based inference
        if (lexer == null) {
            try {
                lexer = inferLexerViaPython(filename, timeout);
            } catch (Exception e) {
                return new Result(Text.raw(source), null, false, "failed to infer lexer: " + e.getMessage());
            }
        }

        if (lexer == null || lexer.isEmpty()) {
            return new Result(Text.raw(source), null, false, "no lexer inferred from filename");
        }

        // Highlight the source
        String raw;
        try {
            raw = highlightViaPython(lexer, source, timeout);
        } catch (Exception e) {
            return new Result(Text.raw(source), lexer, false, "highlighting failed: " + e.getMessage());
        }

        try {
            Text text = RawTokenParser.parse(raw, resolver);
            return new Result(text, lexer, true, null);
        } catch (RuntimeException e) {
            return new Result(Text.raw(source), lexer, false, "failed to parse pygments output: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        // Check if GraalPy is available by trying to create a context
        try {
            try (Context testContext = GraalPyResources.createContext()) {
                testContext.eval(PYTHON, "1+1");
                return true;
            }
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Closes the Python context and releases resources.
     * <p>
     * After calling this method, the highlighter cannot be used anymore.
     */
    public void close() {
        executor.shutdown();
        synchronized (initLock) {
            if (context != null) {
                try {
                    context.close();
                } catch (Exception ignored) {
                    // Ignore close errors
                }
                context = null;
                highlightFunction = null;
                getLexerFunction = null;
                initialized = false;
            }
        }
    }

    private boolean ensureInitialized(Duration timeout) {
        if (initialized) {
            return initError == null;
        }

        synchronized (initLock) {
            if (initialized) {
                return initError == null;
            }

            try {
                Future<Void> future = executor.submit(() -> {
                    doInitialize();
                    return null;
                });
                future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                return initError == null;
            } catch (TimeoutException e) {
                initError = "initialization timed out";
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                initError = "initialization interrupted";
                return false;
            } catch (ExecutionException e) {
                initError = "initialization failed: " + e.getCause().getMessage();
                return false;
            }
        }
    }

    private void doInitialize() {
        try {
            // Use GraalPyResources to create a context with pre-installed pygments
            // (pygments is installed via the Gradle plugin during build)
            context = GraalPyResources.createContext();

            // Create the highlight function
            highlightFunction = context.eval(PYTHON, """
                def _highlight(lexer_name, code):
                    from pygments import highlight
                    from pygments.lexers import get_lexer_by_name
                    from pygments.formatters import RawTokenFormatter
                    lexer = get_lexer_by_name(lexer_name)
                    # RawTokenFormatter returns bytes, decode to string
                    return highlight(code, lexer, RawTokenFormatter()).decode('utf-8')
                _highlight
                """);

            // Create the lexer inference function
            getLexerFunction = context.eval(PYTHON, """
                def _get_lexer_for_filename(filename):
                    from pygments.lexers import get_lexer_for_filename, ClassNotFound
                    try:
                        lexer = get_lexer_for_filename(filename)
                        return lexer.aliases[0] if lexer.aliases else lexer.name.lower()
                    except ClassNotFound:
                        return None
                _get_lexer_for_filename
                """);

            initialized = true;
            initError = null;
        } catch (PolyglotException e) {
            initError = "Python error: " + e.getMessage();
            initialized = true; // Mark as initialized but with error
        } catch (Exception e) {
            initError = "initialization error: " + e.getMessage();
            initialized = true; // Mark as initialized but with error
        }
    }

    private String inferLexerViaPython(String filename, Duration timeout) throws Exception {
        Future<String> future = executor.submit(() -> {
            Value result = getLexerFunction.execute(filename);
            if (result.isNull()) {
                return null;
            }
            return result.asString();
        });

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("lexer inference timed out");
        }
    }

    private String highlightViaPython(String lexer, String source, Duration timeout) throws Exception {
        Future<String> future = executor.submit(() -> {
            Value result = highlightFunction.execute(lexer, source);
            return result.asString();
        });

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("highlighting timed out");
        }
    }

    private static String inferLexerFromExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= filename.length() - 1) {
            return null;
        }
        String extension = filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        return EXTENSION_TO_LEXER.get(extension);
    }

    private static final Map<String, String> EXTENSION_TO_LEXER = createExtensionMap();

    private static Map<String, String> createExtensionMap() {
        Map<String, String> map = new HashMap<>();
        // Common languages
        map.put("java", "java");
        map.put("py", "python");
        map.put("js", "javascript");
        map.put("jsx", "javascript");
        map.put("ts", "typescript");
        map.put("tsx", "typescript");
        map.put("rs", "rust");
        map.put("go", "go");
        map.put("c", "c");
        map.put("h", "c");
        map.put("cpp", "cpp");
        map.put("cc", "cpp");
        map.put("cxx", "cpp");
        map.put("hpp", "cpp");
        map.put("cs", "csharp");
        map.put("php", "php");
        map.put("rb", "ruby");
        map.put("swift", "swift");
        map.put("kt", "kotlin");
        map.put("scala", "scala");
        map.put("clj", "clojure");
        map.put("hs", "haskell");
        map.put("ml", "ocaml");
        map.put("fs", "fsharp");
        map.put("erl", "erlang");
        map.put("ex", "elixir");
        map.put("exs", "elixir");
        map.put("lua", "lua");
        map.put("pl", "perl");
        map.put("pm", "perl");
        map.put("r", "r");
        map.put("sh", "bash");
        map.put("bash", "bash");
        map.put("zsh", "bash");
        // Data formats
        map.put("json", "json");
        map.put("xml", "xml");
        map.put("html", "html");
        map.put("htm", "html");
        map.put("css", "css");
        map.put("yaml", "yaml");
        map.put("yml", "yaml");
        map.put("toml", "toml");
        map.put("ini", "ini");
        map.put("properties", "properties");
        map.put("sql", "sql");
        // Markup
        map.put("md", "markdown");
        map.put("markdown", "markdown");
        map.put("rst", "rst");
        map.put("tex", "latex");
        // Config/build files
        map.put("gradle", "groovy");
        map.put("groovy", "groovy");
        map.put("makefile", "makefile");
        map.put("mk", "makefile");
        map.put("cmake", "cmake");
        // Shell scripts
        map.put("ps1", "powershell");
        map.put("bat", "batch");
        map.put("cmd", "batch");
        return Collections.unmodifiableMap(map);
    }
}
