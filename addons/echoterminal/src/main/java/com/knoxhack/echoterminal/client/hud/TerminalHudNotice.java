package com.knoxhack.echoterminal.client.hud;

public record TerminalHudNotice(
        String sourceLabel,
        String statusLabel,
        String title,
        String detail,
        String footer,
        int accentColor,
        float progress,
        int count) {
    public TerminalHudNotice {
        sourceLabel = clean(sourceLabel, "ECHO");
        statusLabel = clean(statusLabel, "");
        title = clean(title, "Signal updated");
        detail = clean(detail, "");
        footer = clean(footer, "");
        accentColor = accentColor == 0 ? 0xFF66E8FF : accentColor;
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        count = Math.max(1, count);
    }

    public boolean hasProgress() {
        return progress > 0.0F;
    }

    public boolean hasCountBadge() {
        return count > 1;
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
