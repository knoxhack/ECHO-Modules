package com.knoxhack.echoterminal.api;

public record TerminalAddonMetric(
        String label,
        String value,
        String detail,
        int color) {
    public TerminalAddonMetric {
        label = clean(label, "Metric");
        value = clean(value, "-");
        detail = clean(detail, "");
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }
}
