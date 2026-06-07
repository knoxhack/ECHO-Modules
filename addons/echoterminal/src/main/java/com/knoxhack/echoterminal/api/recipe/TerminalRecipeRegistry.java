package com.knoxhack.echoterminal.api.recipe;

import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.TerminalApiIds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public final class TerminalRecipeRegistry {
    private static final int SNAPSHOT_CACHE_TICKS = 20;
    private static final Map<Identifier, TerminalRecipeProvider> PROVIDERS = new ConcurrentHashMap<>();
    private static final List<Runnable> CHANGE_LISTENERS = new CopyOnWriteArrayList<>();
    private static final AtomicLong REVISION = new AtomicLong();
    private static volatile List<TerminalRecipeProvider> sortedProviders = List.of();
    private static volatile CachedSnapshot cachedSnapshot;

    private TerminalRecipeRegistry() {
    }

    public static void register(TerminalRecipeProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Terminal recipe provider is required.");
        }
        Identifier id = TerminalApiIds.requireLowercase(provider.id(), "Terminal recipe provider");
        TerminalRecipeProvider previous = PROVIDERS.putIfAbsent(id, provider);
        if (previous != null && previous != provider) {
            throw new IllegalArgumentException("Duplicate terminal recipe provider id: " + id);
        }
        if (previous == null) {
            ensureSorted();
            notifyChanged();
        }
    }

    public static List<TerminalRecipeProvider> providers() {
        return sortedProviders;
    }

    public static long revision() {
        return REVISION.get();
    }

    public static void addChangeListener(Runnable listener) {
        if (listener != null && !CHANGE_LISTENERS.contains(listener)) {
            CHANGE_LISTENERS.add(listener);
        }
    }

    public static void removeChangeListener(Runnable listener) {
        CHANGE_LISTENERS.remove(listener);
    }

    public static List<TerminalRecipeCategory> categories(Player player) {
        return snapshot(player).categories();
    }

    public static List<TerminalRecipeEntry> recipes(Player player) {
        return snapshot(player).recipes();
    }

    public static TerminalRecipeSnapshot snapshot(Player player) {
        long revision = revision();
        int playerKey = player == null ? 0 : System.identityHashCode(player);
        int tickBucket = player == null ? -1 : player.tickCount / SNAPSHOT_CACHE_TICKS;
        CachedSnapshot cached = cachedSnapshot;
        if (cached != null && cached.matches(revision, playerKey, tickBucket)) {
            return cached.snapshot();
        }
        List<TerminalRecipeCategory> categories = new ArrayList<>();
        List<TerminalRecipeEntry> recipes = new ArrayList<>();
        Set<Identifier> seenCategories = new LinkedHashSet<>();
        Set<Identifier> seenRecipes = new LinkedHashSet<>();
        for (TerminalRecipeProvider provider : sortedProviders) {
            try {
                for (TerminalRecipeCategory category : provider.categories(player)) {
                    if (category == null) {
                        continue;
                    }
                    if (seenCategories.add(category.id())) {
                        categories.add(category);
                    } else {
                        EchoTerminal.LOGGER.warn("Terminal recipe category {} from provider {} was ignored because it is duplicated.",
                                category.id(), provider.id());
                    }
                }
            } catch (RuntimeException | LinkageError exception) {
                EchoTerminal.LOGGER.warn("Terminal recipe provider {} failed while listing categories.", provider.id(), exception);
            }
            try {
                for (TerminalRecipeEntry recipe : provider.recipes(player)) {
                    if (recipe == null) {
                        continue;
                    }
                    if (seenRecipes.add(recipe.id())) {
                        recipes.add(recipe);
                    } else {
                        EchoTerminal.LOGGER.warn("Terminal recipe {} from provider {} was ignored because it is duplicated.",
                                recipe.id(), provider.id());
                    }
                }
            } catch (RuntimeException | LinkageError exception) {
                EchoTerminal.LOGGER.warn("Terminal recipe provider {} failed while listing recipes.", provider.id(), exception);
            }
        }
        categories.sort(Comparator
                .comparingInt(TerminalRecipeCategory::order)
                .thenComparing(category -> category.id().toString()));
        recipes.sort(Comparator.comparing(entry -> entry.id().toString()));
        TerminalRecipeSnapshot snapshot = buildSnapshot(categories, recipes, sortedProviders.size());
        cachedSnapshot = new CachedSnapshot(revision, playerKey, tickBucket, snapshot);
        return snapshot;
    }

    private static TerminalRecipeSnapshot buildSnapshot(
            List<TerminalRecipeCategory> categories, List<TerminalRecipeEntry> recipes, int providerCount) {
        Map<Identifier, TerminalRecipeCategory> categoryMap = new LinkedHashMap<>();
        for (TerminalRecipeCategory category : categories) {
            categoryMap.put(category.id(), category);
        }
        Map<Item, List<TerminalRecipeEntry>> recipesByOutput = new LinkedHashMap<>();
        Map<Item, List<TerminalRecipeEntry>> usesByItem = new LinkedHashMap<>();
        Map<Identifier, Integer> countsByCategory = new LinkedHashMap<>();
        for (TerminalRecipeEntry recipe : recipes) {
            countsByCategory.merge(recipe.categoryId(), 1, Integer::sum);
            for (Item item : recipe.itemsForRole(TerminalRecipeSlot.Role.OUTPUT)) {
                addIndexed(recipesByOutput, item, recipe);
            }
            addUses(recipe, usesByItem, TerminalRecipeSlot.Role.INPUT);
            addUses(recipe, usesByItem, TerminalRecipeSlot.Role.CATALYST);
            addUses(recipe, usesByItem, TerminalRecipeSlot.Role.MACHINE);
            addUses(recipe, usesByItem, TerminalRecipeSlot.Role.INFO);
        }
        return new TerminalRecipeSnapshot(categories, recipes, categoryMap, recipesByOutput, usesByItem,
                countsByCategory, providerCount);
    }

    private static void addUses(
            TerminalRecipeEntry recipe, Map<Item, List<TerminalRecipeEntry>> usesByItem, TerminalRecipeSlot.Role role) {
        for (Item item : recipe.itemsForRole(role)) {
            addIndexed(usesByItem, item, recipe);
        }
    }

    private static void addIndexed(Map<Item, List<TerminalRecipeEntry>> index, Item item, TerminalRecipeEntry recipe) {
        List<TerminalRecipeEntry> recipes = index.computeIfAbsent(item, ignored -> new ArrayList<>());
        if (!recipes.contains(recipe)) {
            recipes.add(recipe);
        }
    }

    public static void ensureSorted() {
        List<TerminalRecipeProvider> providers = new ArrayList<>(PROVIDERS.values());
        providers.sort(Comparator.comparing(provider -> provider.id().toString()));
        sortedProviders = List.copyOf(providers);
    }

    public static void clearForTests() {
        PROVIDERS.clear();
        sortedProviders = List.of();
        notifyChanged();
    }

    public static void withClearedForTests(Runnable runnable) {
        Map<Identifier, TerminalRecipeProvider> snapshot = Map.copyOf(PROVIDERS);
        List<TerminalRecipeProvider> sortedSnapshot = sortedProviders;
        PROVIDERS.clear();
        sortedProviders = List.of();
        notifyChanged();
        try {
            runnable.run();
        } finally {
            PROVIDERS.clear();
            PROVIDERS.putAll(snapshot);
            sortedProviders = sortedSnapshot;
            notifyChanged();
        }
    }

    private static void notifyChanged() {
        cachedSnapshot = null;
        REVISION.incrementAndGet();
        for (Runnable listener : CHANGE_LISTENERS) {
            try {
                listener.run();
            } catch (RuntimeException exception) {
                EchoTerminal.LOGGER.warn("Terminal recipe registry change listener failed.", exception);
            }
        }
    }

    private record CachedSnapshot(long revision, int playerKey, int tickBucket, TerminalRecipeSnapshot snapshot) {
        private boolean matches(long revision, int playerKey, int tickBucket) {
            return this.revision == revision && this.playerKey == playerKey && this.tickBucket == tickBucket;
        }
    }
}
