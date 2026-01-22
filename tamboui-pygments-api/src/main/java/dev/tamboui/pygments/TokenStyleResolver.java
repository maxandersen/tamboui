/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.Tags;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves Pygments token types to styles.
 * <p>
 * This interface allows customization of how different token types are rendered.
 */
@FunctionalInterface
public interface TokenStyleResolver {

    /**
     * Resolves a Pygments token type to a style.
     *
     * @param tokenType full Pygments token type (e.g. {@code Token.Keyword}, {@code Token.Literal.String})
     * @return the style to use for the given token type
     */
    Style resolve(String tokenType);

    /**
     * Returns the default style resolver with commonly used syntax highlighting colors.
     *
     * @return the default style resolver
     */
    static TokenStyleResolver defaultResolver() {
        return DefaultTokenStyleResolver.INSTANCE;
    }
}

/**
 * Default implementation of TokenStyleResolver with commonly used syntax highlighting colors.
 */
final class DefaultTokenStyleResolver implements TokenStyleResolver {

    static final TokenStyleResolver INSTANCE = new DefaultTokenStyleResolver();

    private final Map<String, Style> tokenStyles;

    private DefaultTokenStyleResolver() {
        Map<String, Style> map = new HashMap<>();
        // Token.Comment: "$text 60%"
        map.put("Token.Comment", Style.EMPTY.gray().dim().withExtension(Tags.class, Tags.of("syntax-comment")));
        // Token.Error: "$text-error on $error-muted"
        map.put("Token.Error", Style.EMPTY.red().bg(Color.rgb(0x2c, 0x1e, 0x1e)).withExtension(Tags.class, Tags.of("syntax-error")));
        // Token.Generic.Strong: "bold"
        map.put("Token.Generic.Strong", Style.EMPTY.bold());
        // Token.Generic.Emph: "italic"
        map.put("Token.Generic.Emph", Style.EMPTY.italic());
        // Token.Generic.Error: "$text-error on $error-muted"
        map.put("Token.Generic.Error", Style.EMPTY.red().bg(Color.rgb(0x2c, 0x1e, 0x1e)).withExtension(Tags.class, Tags.of("syntax-error")));
        // Token.Generic.Heading: "$text-primary underline"
        map.put("Token.Generic.Heading", Style.EMPTY.underlineColor(Color.WHITE).withExtension(Tags.class, Tags.of("syntax-heading")));
        // Token.Generic.Subheading: "$text-primary"
        map.put("Token.Generic.Subheading", Style.EMPTY.fg(Color.WHITE).withExtension(Tags.class, Tags.of("syntax-subheading")));
        // Token.Keyword: "$text-accent"
        map.put("Token.Keyword", Style.EMPTY.fg(Color.CYAN).withExtension(Tags.class, Tags.of("syntax-keyword")));
        // Token.Keyword.Constant: "bold $text-success 80%"
        map.put("Token.Keyword.Constant", Style.EMPTY.bold().fg(Color.rgb(0xa9, 0xdc, 0x76)).withExtension(Tags.class, Tags.of("syntax-constant")));
        // Token.Keyword.Namespace: "$text-error"
        map.put("Token.Keyword.Namespace", Style.EMPTY.fg(Color.RED).withExtension(Tags.class, Tags.of("syntax-namespace")));
        // Token.Keyword.Type: "bold"
        map.put("Token.Keyword.Type", Style.EMPTY.bold());
        // Token.Literal.Number: "$text-warning"
        map.put("Token.Literal.Number", Style.EMPTY.fg(Color.YELLOW).withExtension(Tags.class, Tags.of("syntax-number")));
        // Token.Literal.String.Backtick: "$text 60%"
        map.put("Token.Literal.String.Backtick", Style.EMPTY.fg(Color.GRAY).withExtension(Tags.class, Tags.of("syntax-string-backtick")));
        // Token.Literal.String: "$text-success 90%"
        map.put("Token.Literal.String", Style.EMPTY.fg(Color.rgb(0x8d, 0xcf, 0x8c)).withExtension(Tags.class, Tags.of("syntax-string")));
        // Token.Literal.String.Doc: "$text-success 80% italic"
        map.put("Token.Literal.String.Doc", Style.EMPTY.fg(Color.rgb(0xa9, 0xdc, 0x76)).italic().withExtension(Tags.class, Tags.of("syntax-string-doc")));
        // Token.Literal.String.Double: "$text-success 90%"
        map.put("Token.Literal.String.Double", Style.EMPTY.fg(Color.rgb(0x8d, 0xcf, 0x8c)).withExtension(Tags.class, Tags.of("syntax-string-double")));
        // Token.Name: "$text-primary"
        map.put("Token.Name", Style.EMPTY.fg(Color.WHITE).withExtension(Tags.class, Tags.of("syntax-name")));
        // Token.Name.Attribute: "$text-warning"
        map.put("Token.Name.Attribute", Style.EMPTY.fg(Color.YELLOW).withExtension(Tags.class, Tags.of("syntax-attribute")));
        // Token.Name.Builtin: "$text-accent"
        map.put("Token.Name.Builtin", Style.EMPTY.fg(Color.CYAN).withExtension(Tags.class, Tags.of("syntax-builtin")).withExtension(Tags.class, Tags.of("syntax-identifier")));
        // Token.Name.Builtin.Pseudo: "italic"
        map.put("Token.Name.Builtin.Pseudo", Style.EMPTY.italic().withExtension(Tags.class, Tags.of("syntax-builtin-pseudo")));
        // Token.Name.Class: "$text-warning bold"
        map.put("Token.Name.Class", Style.EMPTY.fg(Color.YELLOW).bold().withExtension(Tags.class, Tags.of("syntax-class")));
        // Token.Name.Constant: "$text-error"
        map.put("Token.Name.Constant", Style.EMPTY.fg(Color.RED).withExtension(Tags.class, Tags.of("syntax-constant")));
        // Token.Name.Decorator: "$text-primary bold"
        map.put("Token.Name.Decorator", Style.EMPTY.fg(Color.WHITE).bold().withExtension(Tags.class, Tags.of("syntax-decorator")));
        // Token.Name.Function: "$text-warning underline"
        map.put("Token.Name.Function", Style.EMPTY.fg(Color.YELLOW).underlineColor(Color.YELLOW).withExtension(Tags.class, Tags.of("syntax-function")));
        // Token.Name.Function.Magic: "$text-warning underline"
        map.put("Token.Name.Function.Magic", Style.EMPTY.fg(Color.YELLOW).underlineColor(Color.YELLOW).withExtension(Tags.class, Tags.of("syntax-function-magic")));
        // Token.Name.Tag: "$text-primary bold"
        map.put("Token.Name.Tag", Style.EMPTY.fg(Color.WHITE).bold().withExtension(Tags.class, Tags.of("syntax-tag")));
        // Token.Name.Variable: "$text-secondary"
        map.put("Token.Name.Variable", Style.EMPTY.fg(Color.GRAY).withExtension(Tags.class, Tags.of("syntax-variable")));
        // Token.Name.Namespace
        map.put("Token.Name.Namespace", Style.EMPTY.fg(Color.RED).withExtension(Tags.class, Tags.of("syntax-namespace")));
        // Token.Number: "$text-warning"
        map.put("Token.Number", Style.EMPTY.fg(Color.YELLOW).withExtension(Tags.class, Tags.of("syntax-number")));
        // Token.Operator: "bold"
        map.put("Token.Operator", Style.EMPTY.bold().withExtension(Tags.class, Tags.of("syntax-operator")));
        // Token.Operator.Word: "bold $text-error"
        map.put("Token.Operator.Word", Style.EMPTY.bold().fg(Color.RED).withExtension(Tags.class, Tags.of("syntax-operator-word")));
        // Token.String: "$text-success"
        map.put("Token.String", Style.EMPTY.fg(Color.GREEN).withExtension(Tags.class, Tags.of("syntax-string")));
        // Token.Whitespace: ""
        map.put("Token.Text.Whitespace", Style.EMPTY.withExtension(Tags.class, Tags.of("syntax-whitespace")));
        // Token.Punctuation: ""
        map.put("Token.Text.Punctuation", Style.EMPTY.withExtension(Tags.class, Tags.of("syntax-punctuation")));

        this.tokenStyles = Collections.unmodifiableMap(map);
    }

    @Override
    public Style resolve(String tokenType) {
        Style style = tokenStyles.get(tokenType);
        return style != null ? style : Style.EMPTY;
    }
}
