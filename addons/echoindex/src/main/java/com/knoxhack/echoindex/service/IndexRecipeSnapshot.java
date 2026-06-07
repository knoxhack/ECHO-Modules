package com.knoxhack.echoindex.service;

import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public record IndexRecipeSnapshot(
        List<IndexRecipeCategory> categories,
        List<IndexRecipeView> recipes,
        Map<Identifier, List<IndexRecipeView>> byProvider,
        Map<Identifier, List<IndexRecipeView>> byCategory,
        Map<Item, List<IndexRecipeView>> byOutput,
        Map<Item, List<IndexRecipeView>> byUsage,
        Map<Identifier, IndexRecipeView> byId,
        Map<Identifier, IndexRecipeDisplayMetadata> displayMetadata,
        List<IndexRecipeProviderStats> providerStats,
        List<String> warnings,
        long createdAtMillis,
        long generation,
        String buildReason) {
    public IndexRecipeSnapshot {
        categories = categories == null ? List.of() : List.copyOf(categories);
        recipes = recipes == null ? List.of() : List.copyOf(recipes);
        byProvider = byProvider == null ? Map.of() : Map.copyOf(byProvider);
        byCategory = byCategory == null ? Map.of() : Map.copyOf(byCategory);
        byOutput = byOutput == null ? Map.of() : Map.copyOf(byOutput);
        byUsage = byUsage == null ? Map.of() : Map.copyOf(byUsage);
        byId = byId == null ? Map.of() : Map.copyOf(byId);
        displayMetadata = displayMetadata == null ? Map.of() : Map.copyOf(displayMetadata);
        providerStats = providerStats == null ? List.of() : List.copyOf(providerStats);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        generation = Math.max(0L, generation);
        buildReason = buildReason == null || buildReason.isBlank() ? "initial" : buildReason.strip();
    }

    public static IndexRecipeSnapshot empty() {
        return new IndexRecipeSnapshot(List.of(), List.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of(), List.of(),
                System.currentTimeMillis(), 0L, "empty");
    }

    public List<IndexRecipeView> recipesForProvider(Identifier providerId) {
        return providerId == null ? List.of() : byProvider.getOrDefault(providerId, List.of());
    }

    public List<IndexRecipeView> recipesForCategory(Identifier categoryId) {
        return categoryId == null ? List.of() : byCategory.getOrDefault(categoryId, List.of());
    }

    public List<IndexRecipeView> recipesFor(Item item) {
        return item == null ? List.of() : byOutput.getOrDefault(item, List.of());
    }

    public List<IndexRecipeView> usesFor(Item item) {
        return item == null ? List.of() : byUsage.getOrDefault(item, List.of());
    }

    public Optional<IndexRecipeView> recipe(Identifier id) {
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    public Optional<IndexRecipeDisplayMetadata> metadata(Identifier id) {
        return id == null ? Optional.empty() : Optional.ofNullable(displayMetadata.get(id));
    }

    public boolean sourceCardsLoaded() {
        return providerStats.stream().anyMatch(stats -> stats.sourceFactCount() > 0 || stats.sourceCardCount() > 0);
    }

    public int sourceCardCount() {
        int count = 0;
        for (IndexRecipeView recipe : recipes) {
            if (IndexRecipeSourceKind.isSourceCard(recipe)) {
                count++;
            }
        }
        return count;
    }

    public int usageItemCount() {
        return byUsage.size();
    }

    public int providerCount() {
        return providerStats.size();
    }

    public int rawRecipeCount() {
        return providerStats.stream().mapToInt(IndexRecipeProviderStats::rawRecipeCount).sum();
    }

    public int adaptedRecipeCount() {
        return providerStats.stream().mapToInt(IndexRecipeProviderStats::adaptedRecipeCount).sum();
    }

    public int sourceFactCount() {
        return providerStats.stream().mapToInt(IndexRecipeProviderStats::sourceFactCount).sum();
    }

    public int skippedRecipeCount() {
        return providerStats.stream().mapToInt(IndexRecipeProviderStats::skippedRecipeCount).sum();
    }

    public String lastProviderError() {
        return providerStats.stream()
                .map(IndexRecipeProviderStats::lastError)
                .filter(error -> error != null && !error.isBlank())
                .findFirst()
                .orElse("");
    }

    public boolean providerErrored() {
        return !lastProviderError().isBlank();
    }

    public boolean recipesStillLoading() {
        return recipes.isEmpty() && rawRecipeCount() == 0 && sourceFactCount() == 0 && !providerErrored();
    }

    public long ageSeconds() {
        return Math.max(0L, (System.currentTimeMillis() - createdAtMillis) / 1000L);
    }

    public String healthLine() {
        return "generation " + generation
                + ", reason " + buildReason
                + ", providers " + providerCount()
                + ", raw " + rawRecipeCount()
                + ", adapted " + adaptedRecipeCount()
                + ", usage items " + usageItemCount()
                + ", source facts " + sourceFactCount()
                + ", source cards " + sourceCardCount()
                + ", skipped " + skippedRecipeCount()
                + ", warnings " + warnings.size();
    }

    public static boolean hasRole(IndexRecipeView recipe, IndexSlotRole role) {
        if (recipe == null || role == null) {
            return false;
        }
        for (IndexRecipeSlot slot : recipe.slots()) {
            if (slot.role() == role && (!slot.stacks().isEmpty() || !slot.label().isBlank())) {
                return true;
            }
        }
        return false;
    }
}
