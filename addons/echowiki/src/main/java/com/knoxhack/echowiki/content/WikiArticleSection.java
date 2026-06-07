package com.knoxhack.echowiki.content;

public record WikiArticleSection(
        String type,
        String title,
        String body,
        String tone,
        String image,
        String imageFit,
        String icon,
        String item,
        int count,
        String targetKind,
        String target,
        String label,
        String subtitle) {
    public WikiArticleSection(String title, String body, String tone) {
        this(defaultType(tone), title, body, tone, "", "cover", "", "", 1, "", "", "", "");
    }

    public WikiArticleSection {
        type = clean(type, defaultType(tone));
        title = clean(title, "");
        body = clean(body, "");
        tone = clean(tone, "body");
        image = clean(image, "");
        imageFit = clean(imageFit, "cover");
        icon = clean(icon, "");
        item = clean(item, "");
        count = Math.max(0, count);
        targetKind = clean(targetKind, "");
        target = clean(target, "");
        label = clean(label, "");
        subtitle = clean(subtitle, "");
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private static String defaultType(String tone) {
        String cleanTone = tone == null ? "" : tone.strip();
        return cleanTone.isBlank() || "body".equalsIgnoreCase(cleanTone) ? "paragraph" : "callout";
    }
}
