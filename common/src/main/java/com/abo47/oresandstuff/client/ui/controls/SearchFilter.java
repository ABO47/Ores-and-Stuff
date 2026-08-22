package com.abo47.oresandstuff.client.ui.controls;

import java.util.Locale;

public final class SearchFilter {
    private SearchFilter() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeUserInput(String value) {
        return SearchNormalizer.normalizeUserSearch(value);
    }

    public static String normalizeKey(String value) {
        return SearchNormalizer.normalizeQuery(value);
    }

    public static boolean matches(String query, String id, String displayName) {
        String rawQuery = normalize(query);
        if (rawQuery.isBlank()) {
            return true;
        }
        String compactQuery = normalizeKey(rawQuery);
        String rawId = normalize(id);
        String rawDisplay = normalize(displayName);
        return rawId.contains(rawQuery)
                || rawDisplay.contains(rawQuery)
                || (!compactQuery.isBlank()
                && (normalizeKey(id).contains(compactQuery) || normalizeKey(displayName).contains(compactQuery)));
    }

    public static boolean matches(String query, String value) {
        return matches(query, value, value);
    }

    public static String crop(String value, int max) {
        if (value == null || value.length() <= max) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }
}
