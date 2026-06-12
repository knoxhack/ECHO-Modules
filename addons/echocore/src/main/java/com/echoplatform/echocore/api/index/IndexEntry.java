package com.echoplatform.echocore.api.index;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record IndexEntry(
        Identifier id,
        Identifier categoryId,
        String titleKey,
        String subtitleKey,
        String summaryKey,
        String bodyKey,
        ItemStack icon,
        String sourceModId,
        List<String> tags,
        IndexEntryState defaultState,
        List<Identifier> relatedEntries,
        List<Identifier> linkedItems,
        List<Identifier> secondaryItems,
        int order,
        Map<String, String> fields) {
    public IndexEntry {
        titleKey = titleKey == null ? "" : titleKey;
        subtitleKey = subtitleKey == null ? "" : subtitleKey;
        summaryKey = summaryKey == null ? "" : summaryKey;
        bodyKey = bodyKey == null ? "" : bodyKey;
        icon = icon == null ? ItemStack.EMPTY : icon.copy();
        sourceModId = sourceModId == null ? "" : sourceModId;
        tags = tags == null ? List.of() : List.copyOf(tags);
        defaultState = defaultState == null ? IndexEntryState.VISIBLE : defaultState;
        relatedEntries = relatedEntries == null ? List.of() : List.copyOf(relatedEntries);
        linkedItems = linkedItems == null ? List.of() : List.copyOf(linkedItems);
        secondaryItems = secondaryItems == null ? List.of() : List.copyOf(secondaryItems);
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }

    public IndexEntry(String id, String title, String category, Map<String, String> fields) {
        this(
                parse(id),
                parse(category),
                title,
                fields == null ? "" : fields.getOrDefault("subtitle", ""),
                fields == null ? "" : fields.getOrDefault("summary", ""),
                fields == null ? "" : fields.getOrDefault("detail", ""),
                ItemStack.EMPTY,
                fields == null ? "" : fields.getOrDefault("sourceModule", ""),
                List.of(),
                state(fields == null ? "" : fields.getOrDefault("state", "")),
                List.of(),
                List.of(),
                List.of(),
                parseInt(fields == null ? "" : fields.getOrDefault("order", "")),
                fields);
    }

    public IndexEntry(Identifier id, Identifier category, String title, String subtitle, String summary,
            String detail, ItemStack icon, String sourceModule, List<String> tags, IndexEntryState state,
            List<Identifier> dependencies, List<Identifier> primaryItems, List<Identifier> secondaryItems,
            int order) {
        this(id, category, title, subtitle, summary, detail, icon, sourceModule, tags, state,
                dependencies, merge(primaryItems, secondaryItems), secondaryItems, order, Map.of());
    }

    public String title() {
        return titleKey;
    }

    public String category() {
        return categoryId == null ? "" : categoryId.toString();
    }

    public int sortOrder() {
        return order;
    }

    public List<Identifier> linkedRecipes() {
        return fields.containsKey("recipe") ? List.of(parse(fields.get("recipe"))) : List.of();
    }

    private static Identifier parse(String value) {
        if (value == null || value.isBlank()) {
            return Identifier.withDefaultNamespace("unknown");
        }
        Identifier parsed = Identifier.tryParse(value);
        return parsed == null ? Identifier.withDefaultNamespace(value.replaceAll("[^a-z0-9_./-]", "_")) : parsed;
    }

    private static IndexEntryState state(String value) {
        if (value == null || value.isBlank()) {
            return IndexEntryState.VISIBLE;
        }
        try {
            return IndexEntryState.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return IndexEntryState.VISIBLE;
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static List<Identifier> merge(List<Identifier> primary, List<Identifier> secondary) {
        LinkedHashSet<Identifier> ids = new LinkedHashSet<>();
        if (primary != null) {
            ids.addAll(primary);
        }
        if (secondary != null) {
            ids.addAll(secondary);
        }
        return List.copyOf(ids);
    }
}
