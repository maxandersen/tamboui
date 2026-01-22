/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments.process;

import dev.tamboui.pygments.RawTokenParser;
import dev.tamboui.pygments.Result;
import dev.tamboui.pygments.SyntaxHighlighter;
import dev.tamboui.pygments.TokenStyleResolver;
import dev.tamboui.text.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Syntax highlighter implementation that uses the {@code pygmentize} CLI with the {@code raw} formatter.
 * <p>
 * It will first try {@code pygmentize}, then {@code uvx}, then {@code pipx} to find a working command.
 * <p>
 * The raw formatter emits one token per line in the form:
 * {@code Token.Keyword\t'repr(token_text)'}.
 * This class parses that output and converts it to {@link Text} by applying styles
 * via a {@link TokenStyleResolver}.
 */
public final class ProcessSyntaxHighlighter implements SyntaxHighlighter {

    private static final String DEFAULT_BIN = "pygmentize";
    private static final String UVX_BIN = "uvx";
    private static final String PIPX_BIN = "pipx";

    private volatile Invoker defaultInvoker;

    /**
     * Creates a new process-based syntax highlighter.
     */
    public ProcessSyntaxHighlighter() {
        // Instance-based to cache the detected invoker
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
        Result result = highlightWithInfo(filename, source, timeout, resolver);
        return result.text();
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

        Invoker invoker = resolveInvoker();
        if (invoker == null) {
            return new Result(Text.raw(source), null, false, "pygmentize not available (tried pygmentize, uvx, pipx)");
        }

        String lexer;
        try {
            lexer = inferLexer(filename, invoker, timeout);
        } catch (IOException | InterruptedException e) {
            return new Result(Text.raw(source), null, false, e.getMessage());
        }

        if (lexer == null || lexer.isEmpty()) {
            return new Result(Text.raw(source), null, false, "no lexer inferred from filename");
        }

        String raw;
        try {
            raw = tokenizeToRaw(lexer, source, invoker, timeout);
        } catch (IOException | InterruptedException e) {
            return new Result(Text.raw(source), lexer, false, e.getMessage());
        }

        try {
            Text text = RawTokenParser.parse(raw, resolver);
            return new Result(text, lexer, true, null);
        } catch (RuntimeException e) {
            return new Result(Text.raw(source), lexer, false, "failed to parse pygmentize output: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return resolveInvoker() != null;
    }

    private static String inferLexer(String filename, Invoker invoker, Duration timeout) throws IOException, InterruptedException {
        // First try extension mapping (fast, no process call)
        String lexer = inferLexerFromExtension(filename);
        if (lexer != null) {
            return lexer;
        }
        // Fallback to pygmentize inference (slower, but supports all file types)
        return inferLexerFromPygmentize(filename, invoker, timeout);
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

    private static String inferLexerFromPygmentize(String filename, Invoker invoker, Duration timeout) throws IOException, InterruptedException {
        // `pygmentize -N filename` prints a lexer name (or "text" / empty)
        ProcessResult pr = run(invoker.command("-N", filename), null, timeout, invoker.filterNotes());
        if (pr.exitCode != 0) {
            return null;
        }
        String out = pr.stdout.trim();
        if (out.isEmpty()) {
            return null;
        }
        // sometimes outputs like "text\n" or "java\n"
        String lexer = out.toLowerCase(Locale.ROOT);
        // Don't return "text" as it means no lexer was found
        return "text".equals(lexer) ? null : lexer;
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

    private static String tokenizeToRaw(String lexer, String source, Invoker invoker, Duration timeout) throws IOException, InterruptedException {
        List<String> cmd = invoker.command("-l", lexer, "-f", "raw");

        ProcessResult pr = run(cmd, source, timeout, invoker.filterNotes());
        if (pr.exitCode != 0) {
            throw new IOException("pygmentize failed: " + pr.stderr.trim());
        }
        return pr.stdout;
    }

    private static ProcessResult run(List<String> command, String stdin, Duration timeout, boolean filterNotes)
        throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        Process p = pb.start();

        if (stdin != null) {
            try (OutputStream os = p.getOutputStream()) {
                os.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            p.getOutputStream().close();
        }

        boolean finished = p.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("pygmentize timed out");
        }

        String out = readAll(p.getInputStream());
        String err = readAll(p.getErrorStream());
        if (filterNotes) {
            out = stripNoteLines(out);
            err = stripNoteLines(err);
        }
        return new ProcessResult(p.exitValue(), out, err);
    }

    private static String stripNoteLines(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String[] lines = s.split("\n", -1);
        StringBuilder sb = new StringBuilder(s.length());
        for (String line : lines) {
            if (line.startsWith("NOTE:")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        // Preserve the behavior of split(..., -1) which always leaves a trailing empty element if s ended with '\n'.
        // Since we always append '\n' above, remove one if the original didn't end with it.
        if (!s.endsWith("\n") && sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private Invoker resolveInvoker() {
        return resolveDefaultInvoker();
    }

    private Invoker resolveDefaultInvoker() {
        if (defaultInvoker != null) {
            return defaultInvoker;
        }
        synchronized (this) {
            Invoker detected = null;
            // Prefer a real pygmentize first.
            if (canRunDirect(DEFAULT_BIN, Duration.ofSeconds(2), "-V")) {
                detected = Invoker.direct(DEFAULT_BIN);
            } else if (canRunDirect(UVX_BIN, Duration.ofSeconds(2), "--version")) {
                // Fallback 1: uvx --from pygments pygmentize ...
                detected = Invoker.uvx();
            } else if (canRunDirect(PIPX_BIN, Duration.ofSeconds(2), "--version")) {
                // Fallback 2: pipx run --spec pygments pygmentize ...
                detected = Invoker.pipx();
            }
            defaultInvoker = detected;
            return detected;
        }
    }

    /**
     * Can the command run and return a success exit code?
     *
     * @param bin     the command to run
     * @param timeout the timeout
     * @param args    the arguments to pass to the command (--version, -V, etc.)
     * @return true if the command can be run and returns a success exit code
     */
    private static boolean canRunDirect(String bin, Duration timeout, String... args) {
        try {
            List<String> cmd = new ArrayList<>(1 + args.length);
            cmd.add(bin);
            cmd.addAll(Arrays.asList(args));
            ProcessResult pr = run(cmd, null, timeout, false);
            if (pr.exitCode == 0) {
                return true;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return false;
    }

    private static final class Invoker {
        private final List<String> prefix;
        private final boolean filterNotes;

        private Invoker(List<String> prefix, boolean filterNotes) {
            this.prefix = prefix;
            this.filterNotes = filterNotes;
        }

        static Invoker direct(String bin) {
            return new Invoker(Collections.singletonList(bin), false);
        }

        static Invoker uvx() {
            return new Invoker(Arrays.asList(UVX_BIN, "--from", "pygments", "pygmentize"), false);
        }

        static Invoker pipx() {
            // pipx may print "NOTE:" lines; strip them from stdout/stderr.
            return new Invoker(Arrays.asList(PIPX_BIN, "run", "--spec", "pygments", "pygmentize"), true);
        }

        List<String> command(String... args) {
            List<String> cmd = new ArrayList<>(prefix.size() + args.length);
            cmd.addAll(prefix);
            cmd.addAll(Arrays.asList(args));
            return cmd;
        }

        boolean filterNotes() {
            return filterNotes;
        }
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int read;
        while ((read = is.read(buf)) >= 0) {
            baos.write(buf, 0, read);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static final class ProcessResult {
        final int exitCode;
        final String stdout;
        final String stderr;

        ProcessResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout != null ? stdout : "";
            this.stderr = stderr != null ? stderr : "";
        }
    }
}
