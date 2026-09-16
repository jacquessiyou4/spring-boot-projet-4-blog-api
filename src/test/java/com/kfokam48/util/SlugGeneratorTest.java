package com.kfokam48.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SlugGeneratorTest {

    @Test
    void toSlug_withSimpleTitle_shouldReturnLowercase() {
        assertThat(SlugGenerator.toSlug("Hello World")).isEqualTo("hello-world");
    }

    @Test
    void toSlug_withAccents_shouldRemoveAccents() {
        assertThat(SlugGenerator.toSlug("Café Résumé")).isEqualTo("cafe-resume");
    }

    @Test
    void toSlug_withSpecialChars_shouldRemoveSpecialChars() {
        assertThat(SlugGenerator.toSlug("Hello! @World# $Test%")).isEqualTo("hello-world-test");
    }

    @Test
    void toSlug_withMultipleSpaces_shouldCollapse() {
        assertThat(SlugGenerator.toSlug("Hello   World")).isEqualTo("hello-world");
    }

    @Test
    void toSlug_withLeadingTrailingSpaces_shouldTrim() {
        assertThat(SlugGenerator.toSlug("  Hello World  ")).isEqualTo("hello-world");
    }

    @Test
    void toSlug_withNull_shouldReturnEmpty() {
        assertThat(SlugGenerator.toSlug(null)).isEmpty();
    }

    @Test
    void toSlug_withEmptyString_shouldReturnEmpty() {
        assertThat(SlugGenerator.toSlug("")).isEmpty();
    }

    @Test
    void toSlug_withLongTitle_shouldTruncate() {
        String longTitle = "a".repeat(300);
        String slug = SlugGenerator.toSlug(longTitle);
        assertThat(slug.length()).isLessThanOrEqualTo(250);
    }

    @Test
    void toSlug_withFrenchAccents_shouldWork() {
        assertThat(SlugGenerator.toSlug("Mon Article avec Éàèù")).isEqualTo("mon-article-avec-eaeu");
    }

    @Test
    void toSlug_withNumbers_shouldKeepNumbers() {
        assertThat(SlugGenerator.toSlug("Test 123 Article")).isEqualTo("test-123-article");
    }
}
