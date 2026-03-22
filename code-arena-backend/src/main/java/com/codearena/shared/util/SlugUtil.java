package com.codearena.shared.util;

public final class SlugUtil {
    private SlugUtil() {
    }

    /**
     * Creates slug from input text.
     *
     * @param text source text
     * @return slug text
     */
    public static String toSlug(String text) {
        // TODO: Improve transliteration and uniqueness strategy.
        return text == null ? "" : text.trim().toLowerCase().replace(" ", "-");
    }
}
