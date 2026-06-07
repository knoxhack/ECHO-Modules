package com.knoxhack.echowiki.content;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public record GuideBookDefinition(
        Identifier id,
        String moduleId,
        String requiredModId,
        String title,
        String subtitle,
        String summary,
        Identifier icon,
        String accent,
        Identifier collectionId,
        Identifier homeArticleId,
        List<Identifier> chapterArticleIds,
        List<String> tags,
        int sortOrder) {
    public GuideBookDefinition {
        Objects.requireNonNull(id, "Guide book id is required.");
        moduleId = clean(moduleId, id.getNamespace());
        requiredModId = clean(requiredModId, moduleId);
        title = clean(title, readable(id) + " Guide");
        subtitle = clean(subtitle, moduleId);
        summary = clean(summary, "Player-facing field manual for " + title + ".");
        icon = icon == null ? Identifier.fromNamespaceAndPath("minecraft", "written_book") : icon;
        accent = clean(accent, "#FF66E8FF");
        chapterArticleIds = chapterArticleIds == null ? List.of() : chapterArticleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        tags = tags == null ? List.of() : tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
    }

    public List<Identifier> allArticleIds() {
        if (homeArticleId == null) {
            return chapterArticleIds;
        }
        java.util.ArrayList<Identifier> ids = new java.util.ArrayList<>();
        ids.add(homeArticleId);
        for (Identifier chapter : chapterArticleIds) {
            if (!ids.contains(chapter)) {
                ids.add(chapter);
            }
        }
        return List.copyOf(ids);
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.strip();
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private static String readable(Identifier id) {
        String[] parts = id.getPath().replace('/', '_').split("_+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? id.toString() : builder.toString();
    }
}
