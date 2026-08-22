package com.abo47.oresandstuff.client.ui.controls;


import java.util.Locale;

public final class SearchNormalizer {
    private SearchNormalizer() {
    }

    public static String normalizeUserSearch(String value) {
        if (value == null) {
            return "";
        }
        String raw = value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .toLowerCase(Locale.ROOT);
        while (raw.endsWith("_")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    public static String normalizeQuery(String value) {
        String raw = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                normalized.append(c);
            }
        }
        return normalized.toString();
    }

    public static String normalizeHexInput(String value) {
        if (value == null) {
            return "";
        }
        String raw = value.trim().toUpperCase(Locale.ROOT);
        if (raw.startsWith("0X")) {
            raw = "#" + raw.substring(2);
        }
        if (!raw.startsWith("#")) {
            raw = "#" + raw;
        }
        String rawBody = raw.substring(1);
        StringBuilder bodyBuilder = new StringBuilder(Math.min(8, rawBody.length()));
        for (int i = 0; i < rawBody.length() && bodyBuilder.length() < 8; i++) {
            char c = rawBody.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
                bodyBuilder.append(c);
            }
        }
        String body = bodyBuilder.toString();
        if (body.length() > 8) {
            body = body.substring(0, 8);
        }
        return "#" + body;
    }

    public static int parseHexColor(String value, int fallback) {
        String norm = normalizeHexInput(value);
        if (norm.length() != 7 && norm.length() != 9) {
            return fallback;
        }
        try {
            String hex = norm.substring(1);
            if (hex.length() == 6) {
                return (0xFF << 24) | Integer.parseInt(hex, 16);
            }
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static String toHexColor(int color) {
        return "#" + String.format(Locale.ROOT, "%08X", color);
    }
}
