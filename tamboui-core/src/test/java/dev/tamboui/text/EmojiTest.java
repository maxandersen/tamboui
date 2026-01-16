/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.text.markup.Markup;

class EmojiTest {

    @Test
    @DisplayName("replace basic emoji codes")
    void replaceBasicEmojiCodes() {
        assertThat(Emoji.replace("Hello :smiley:!")).isEqualTo("Hello 😃!");
        assertThat(Emoji.replace(":warning: Alert")).isEqualTo("⚠ Alert");
        // Note: "success" is not in the emoji map, so it won't be replaced
        assertThat(Emoji.replace(":warning: Done")).isEqualTo("⚠ Done");
    }

    @Test
    @DisplayName("replace multiple emoji codes")
    void replaceMultipleEmojiCodes() {
        // Using emojis that exist in the map: cross_mark, warning, info
        assertThat(Emoji.replace(":cross_mark: :warning:"))
            .isEqualTo("❌ ⚠");
    }

    @Test
    @DisplayName("unknown emoji codes are left unchanged")
    void unknownEmojiCodesLeftUnchanged() {
        assertThat(Emoji.replace(":unknown_emoji:")).isEqualTo(":unknown_emoji:");
        assertThat(Emoji.replace("Hello :xyz: world")).isEqualTo("Hello :xyz: world");
    }

    @Test
    @DisplayName("emoji codes are case-insensitive")
    void emojiCodesCaseInsensitive() {
        assertThat(Emoji.replace(":SMILEY:")).isEqualTo("😃");
        assertThat(Emoji.replace(":Warning:")).isEqualTo("⚠");
    }

    @Test
    @DisplayName("text without emoji codes is unchanged")
    void textWithoutEmojiCodesUnchanged() {
        assertThat(Emoji.replace("Hello world")).isEqualTo("Hello world");
        assertThat(Emoji.replace("")).isEqualTo("");
        assertThat(Emoji.replace(null)).isEqualTo("");
    }

    @Test
    @DisplayName("containsEmojiCodes detects emoji codes")
    void containsEmojiCodesDetects() {
        assertThat(Emoji.containsEmojiCodes(":smiley:")).isTrue();
        assertThat(Emoji.containsEmojiCodes("Hello :warning:!")).isTrue();
        assertThat(Emoji.containsEmojiCodes("Hello world")).isFalse();
        assertThat(Emoji.containsEmojiCodes("")).isFalse();
        assertThat(Emoji.containsEmojiCodes(null)).isFalse();
    }

    @Test
    @DisplayName("Markup.render replaces emoji codes by default")
    void markupRenderReplacesEmojiCodes() {
        dev.tamboui.text.Text text = Markup.render(":warning: Alert!");
        assertThat(text.rawContent()).contains("⚠");
    }

    @Test
    @DisplayName("Markup.render can disable emoji replacement")
    void markupRenderCanDisableEmoji() {
        dev.tamboui.text.Text text = Markup.render(":warning: Alert!", dev.tamboui.style.Style.EMPTY, false);
        assertThat(text.rawContent()).isEqualTo(":warning: Alert!");
    }
}

