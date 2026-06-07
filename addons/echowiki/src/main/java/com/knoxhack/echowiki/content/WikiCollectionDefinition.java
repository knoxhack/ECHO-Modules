package com.knoxhack.echowiki.content;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public record WikiCollectionDefinition(
        Identifier id,
        String title,
        String summary,
        String category,
        List<Identifier> articles,
        int sortOrder) {
    public WikiCollectionDefinition {
        Objects.requireNonNull(id, "Wiki collection id is required.");
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        summary = summary == null ? "" : summary.strip();
        category = category == null || category.isBlank() ? "general" : category.strip();
        articles = articles == null ? List.of() : articles.stream().filter(Objects::nonNull).distinct().toList();
    }
}
