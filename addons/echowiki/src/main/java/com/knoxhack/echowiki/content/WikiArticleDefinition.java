package com.knoxhack.echowiki.content;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public record WikiArticleDefinition(
        Identifier id,
        String title,
        String category,
        String summary,
        List<WikiArticleSection> sections,
        List<String> tags,
        Identifier icon,
        Identifier heroArt,
        List<Identifier> relatedArticles,
        List<Identifier> relatedItems,
        List<Identifier> relatedRecipes,
        List<Identifier> relatedMissions,
        List<Identifier> relatedRegions,
        List<Identifier> relatedHazards,
        List<Identifier> relatedFactions,
        Identifier unlockDiscovery,
        int spoilerLevel,
        int sortOrder) {
    public WikiArticleDefinition {
        Objects.requireNonNull(id, "Wiki article id is required.");
        title = clean(title, readable(id));
        category = clean(category, "general");
        summary = clean(summary, "ECHO Survival Codex article.");
        sections = sections == null ? List.of() : sections.stream().filter(Objects::nonNull).toList();
        tags = tags == null ? List.of() : tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
        icon = icon == null ? Identifier.fromNamespaceAndPath("minecraft", "book") : icon;
        relatedArticles = cleanIds(relatedArticles);
        relatedItems = cleanIds(relatedItems);
        relatedRecipes = cleanIds(relatedRecipes);
        relatedMissions = cleanIds(relatedMissions);
        relatedRegions = cleanIds(relatedRegions);
        relatedHazards = cleanIds(relatedHazards);
        relatedFactions = cleanIds(relatedFactions);
        spoilerLevel = Math.max(0, spoilerLevel);
    }

    public boolean lockedByDiscovery() {
        return unlockDiscovery != null;
    }

    private static List<Identifier> cleanIds(List<Identifier> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).distinct().toList();
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
