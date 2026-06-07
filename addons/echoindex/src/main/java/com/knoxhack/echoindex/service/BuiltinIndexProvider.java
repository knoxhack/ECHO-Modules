package com.knoxhack.echoindex.service;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoModuleInfo;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echoindex.IndexIds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum BuiltinIndexProvider implements IIndexContentProvider {
    INSTANCE;

    private static final List<CategorySeed> CATEGORY_SEEDS = List.of(
            new CategorySeed(IndexIds.CATEGORY_ITEMS, "items", Items.CHEST, 10),
            new CategorySeed(IndexIds.CATEGORY_BLOCKS, "blocks", Items.STONE, 20),
            new CategorySeed(IndexIds.CATEGORY_MACHINES, "machines", Items.CRAFTER, 30),
            new CategorySeed(IndexIds.CATEGORY_TOOLS, "tools", Items.IRON_PICKAXE, 40),
            new CategorySeed(IndexIds.CATEGORY_COMBAT, "combat", Items.IRON_SWORD, 50),
            new CategorySeed(IndexIds.CATEGORY_RECIPES, "recipes", Items.CRAFTING_TABLE, 60),
            new CategorySeed(IndexIds.CATEGORY_SOURCES, "sources", Items.SPYGLASS, 65),
            new CategorySeed(IndexIds.CATEGORY_TUTORIALS, "tutorials", Items.BOOK, 70),
            new CategorySeed(IndexIds.CATEGORY_LORE, "lore", Items.WRITABLE_BOOK, 80),
            new CategorySeed(IndexIds.CATEGORY_RESEARCH, "research", Items.AMETHYST_SHARD, 90),
            new CategorySeed(IndexIds.CATEGORY_ROUTES, "routes", Items.COMPASS, 100),
            new CategorySeed(IndexIds.CATEGORY_HAZARDS, "hazards", Items.SHIELD, 110),
            new CategorySeed(IndexIds.CATEGORY_FACTIONS, "factions", Items.EMERALD, 120),
            new CategorySeed(IndexIds.CATEGORY_DIAGNOSTICS, "diagnostics", Items.REDSTONE_TORCH, 130));

    @Override
    public Identifier id() {
        return IndexIds.PROVIDER_BUILTIN;
    }

    @Override
    public IndexContentSnapshot snapshot(IndexBuildContext context) {
        List<IndexCategory> categories = CATEGORY_SEEDS.stream()
                .map(seed -> category(seed.id(), seed.key(), seed.icon(), seed.order()))
                .toList();
        List<IndexEntry> entries = new ArrayList<>();
        for (CategorySeed seed : CATEGORY_SEEDS) {
            entries.add(categoryOverview(seed));
        }
        entries.add(new IndexEntry(
                IndexIds.ENTRY_OVERVIEW,
                IndexIds.CATEGORY_TUTORIALS,
                "echoindex.entry.index_overview",
                "echoindex.entry.index_overview.subtitle",
                "echoindex.entry.index_overview.summary",
                "echoindex.entry.index_overview.body",
                safeStack(Items.BOOK),
                "echoindex",
                List.of("index", "codex", "archive", "recipe"),
                IndexEntryState.DISCOVERED,
                List.of(),
                List.of(),
                List.of(),
                0));
        for (EchoModuleInfo module : EchoCoreServices.moduleReport()) {
            if (module.modId().isBlank()) {
                continue;
            }
            entries.add(moduleEntry(module));
        }
        return new IndexContentSnapshot(id(), categories, entries, List.of(), List.of(), List.of(),
                List.of(), List.of());
    }

    private static IndexCategory category(Identifier id, String key, net.minecraft.world.item.Item icon, int order) {
        return new IndexCategory(
                id,
                "echoindex.category." + key,
                "echoindex.category." + key + ".desc",
                safeStack(icon),
                order,
                "echoindex");
    }

    private static IndexEntry categoryOverview(CategorySeed seed) {
        String key = seed.key();
        return new IndexEntry(
                IndexIds.id("category/" + key),
                seed.id(),
                "echoindex.entry.category." + key,
                "echoindex.entry.category." + key + ".subtitle",
                "echoindex.entry.category." + key + ".summary",
                "echoindex.entry.category." + key + ".body",
                safeStack(seed.icon()),
                "echoindex",
                List.of(key, "index", "overview"),
                IndexEntryState.VISIBLE,
                List.of(IndexIds.ENTRY_OVERVIEW),
                List.of(),
                List.of(),
                0);
    }

    private static IndexEntry moduleEntry(EchoModuleInfo module) {
        return new IndexEntry(
                IndexIds.id("module/" + module.modId()),
                IndexIds.CATEGORY_DIAGNOSTICS,
                module.displayName(),
                module.loaded() ? "Loaded module" : "Missing module",
                module.statusLine(),
                module.ownership(),
                safeStack(module.loaded() ? Items.REDSTONE_TORCH : Items.BARRIER),
                module.modId(),
                List.of("module", module.modId(), module.loaded() ? "loaded" : "missing"),
                module.loaded() ? IndexEntryState.VISIBLE : IndexEntryState.LOCKED,
                List.of(IndexIds.id("category/diagnostics")),
                List.of(),
                List.of(),
                module.loaded() ? 100 : 900);
    }

    private static ItemStack safeStack(Item item) {
        if (item == null) {
            return ItemStack.EMPTY;
        }
        try {
            return new ItemStack(item);
        } catch (RuntimeException exception) {
            return ItemStack.EMPTY;
        }
    }

    private record CategorySeed(Identifier id, String key, net.minecraft.world.item.Item icon, int order) {
    }
}
