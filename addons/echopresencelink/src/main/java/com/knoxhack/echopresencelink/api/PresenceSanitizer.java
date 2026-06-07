package com.knoxhack.echopresencelink.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class PresenceSanitizer {
    private PresenceSanitizer() {
    }

    public static String text(String value, int maxLength, String fallback) {
        String raw = value == null ? "" : value;
        StringBuilder builder = new StringBuilder(raw.length());
        boolean skipFormattingCode = false;
        for (int index = 0; index < raw.length(); index++) {
            char ch = raw.charAt(index);
            if (skipFormattingCode) {
                skipFormattingCode = false;
                continue;
            }
            if (ch == '\u00A7') {
                skipFormattingCode = true;
                continue;
            }
            if (Character.isWhitespace(ch)) {
                builder.append(' ');
                continue;
            }
            int type = Character.getType(ch);
            if (!Character.isISOControl(ch) && type != Character.FORMAT) {
                builder.append(ch);
            }
        }
        String cleaned = builder.toString().strip().replaceAll(" {2,}", " ");
        if (cleaned.isBlank()) {
            cleaned = fallback == null ? "" : fallback.strip();
        }
        int limit = Math.max(0, maxLength);
        if (limit > 0 && cleaned.length() > limit) {
            cleaned = cleaned.substring(0, limit).strip();
        }
        return cleaned;
    }

    public static String assetKey(String value, String fallback) {
        String key = text(value, 64, fallback).toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (key.isBlank()) {
            key = text(fallback, 64, "").toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9_]", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_+|_+$", "");
        }
        return key;
    }

    public static String url(String value) {
        String cleaned = text(value, 512, "");
        if (cleaned.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(cleaned);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            return ("https".equals(scheme) || "http".equals(scheme)) && uri.getHost() != null ? cleaned : "";
        } catch (URISyntaxException exception) {
            return "";
        }
    }
}
