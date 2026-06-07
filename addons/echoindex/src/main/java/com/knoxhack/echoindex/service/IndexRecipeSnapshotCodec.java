package com.knoxhack.echoindex.service;

import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexMachineLayout;
import com.knoxhack.echocore.api.index.IndexMachineLayoutGauge;
import com.knoxhack.echocore.api.index.IndexMachineLayoutSlot;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class IndexRecipeSnapshotCodec {
    private static final int MAX_QUERY_RECIPES = 384;
    private static final int MAX_QUERY_USES = 384;
    private static final int MAX_QUERY_SOURCES = 384;

    private IndexRecipeSnapshotCodec() {
    }

    public static CompoundTag encode(IndexRecipeSnapshot snapshot) {
        CompoundTag root = new CompoundTag();
        if (snapshot == null) {
            return root;
        }
        writeHealth(root, snapshot);
        root.put("categories", encodeCategories(snapshot.categories()));
        root.put("recipes", encodeRecipes(snapshot));
        root.put("stats", encodeStats(snapshot.providerStats()));
        writeStrings(root, "warning", snapshot.warnings());
        return root;
    }

    public static IndexRecipeSnapshot decode(CompoundTag root) {
        if (root == null || root.isEmpty()) {
            return IndexRecipeSnapshot.empty();
        }
        List<IndexRecipeCategory> categories = decodeCategories(root.getListOrEmpty("categories"));
        List<RecipeWithProvider> decodedRecipes = decodeRecipes(root.getListOrEmpty("recipes"));
        List<IndexRecipeView> recipes = decodedRecipes.stream().map(RecipeWithProvider::recipe).toList();
        List<IndexRecipeProviderStats> stats = decodeStats(root.getListOrEmpty("stats"));
        List<String> warnings = readStrings(root, "warning");

        Map<Identifier, List<IndexRecipeView>> byProvider = new LinkedHashMap<>();
        Map<Identifier, List<IndexRecipeView>> byCategory = new LinkedHashMap<>();
        Map<Item, List<IndexRecipeView>> byOutput = new LinkedHashMap<>();
        Map<Item, List<IndexRecipeView>> byUsage = new LinkedHashMap<>();
        Map<Identifier, IndexRecipeView> byId = new LinkedHashMap<>();
        for (RecipeWithProvider decoded : decodedRecipes) {
            IndexRecipeView recipe = decoded.recipe();
            byId.put(recipe.id(), recipe);
            byProvider.computeIfAbsent(decoded.providerId(), ignored -> new ArrayList<>()).add(recipe);
            byCategory.computeIfAbsent(recipe.categoryId(), ignored -> new ArrayList<>()).add(recipe);
            addItems(byOutput, recipe.itemsForRole(IndexSlotRole.OUTPUT), recipe);
            if (!IndexRecipeSourceKind.isSourceCard(recipe)) {
                addItems(byUsage, recipe.itemsForRole(IndexSlotRole.INPUT), recipe);
                addItems(byUsage, recipe.itemsForRole(IndexSlotRole.CATALYST), recipe);
                addItems(byUsage, recipe.itemsForRole(IndexSlotRole.MACHINE), recipe);
            }
        }
        Map<Identifier, IndexRecipeDisplayMetadata> metadata = decodeDisplayMetadata(root.getListOrEmpty("recipes"));
        return new IndexRecipeSnapshot(categories, recipes, freezeIdentifierIndex(byProvider),
                freezeIdentifierIndex(byCategory), freezeItemIndex(byOutput), freezeItemIndex(byUsage), byId, metadata,
                stats, warnings, root.getLongOr("created_at", System.currentTimeMillis()),
                root.getLongOr("generation", 0L), root.getStringOr("reason", "server sync"));
    }

    public static CompoundTag encodeHealth(IndexRecipeSnapshot snapshot) {
        CompoundTag root = new CompoundTag();
        if (snapshot == null) {
            return root;
        }
        writeHealth(root, snapshot);
        root.put("stats", encodeStats(snapshot.providerStats()));
        writeStrings(root, "warning", snapshot.warnings());
        return root;
    }

    public static CompoundTag encodeQueryResult(Identifier itemId, IndexRecipeSnapshot snapshot,
            List<IndexRecipeView> recipes, List<IndexRecipeView> uses, List<IndexRecipeView> sources, String warning) {
        CompoundTag root = new CompoundTag();
        if (snapshot != null) {
            writeHealth(root, snapshot);
        }
        if (itemId != null) {
            root.putString("item", itemId.toString());
        }
        Map<Identifier, IndexRecipeDisplayMetadata> metadata = snapshot == null ? Map.of() : snapshot.displayMetadata();
        List<IndexRecipeView> limitedRecipes = limitQueryViews(recipes, MAX_QUERY_RECIPES);
        List<IndexRecipeView> limitedUses = limitQueryViews(uses, MAX_QUERY_USES);
        List<IndexRecipeView> limitedSources = limitQueryViews(sources, MAX_QUERY_SOURCES);
        int recipeCount = count(recipes);
        int useCount = count(uses);
        int sourceCount = count(sources);
        root.put("recipes", encodeRecipeViews(limitedRecipes, metadata));
        root.put("uses", encodeRecipeViews(limitedUses, metadata));
        root.put("sources", encodeRecipeViews(limitedSources, metadata));
        root.putInt("recipe_count", recipeCount);
        root.putInt("use_count", useCount);
        root.putInt("source_count", sourceCount);
        root.putInt("visible_recipe_count", limitedRecipes.size());
        root.putInt("visible_use_count", limitedUses.size());
        root.putInt("visible_source_count", limitedSources.size());
        String queryWarning = queryWarning(warning,
                recipeCount, limitedRecipes.size(),
                useCount, limitedUses.size(),
                sourceCount, limitedSources.size());
        if (!queryWarning.isBlank()) {
            root.putString("query_warning", queryWarning);
        }
        return root;
    }

    public static List<IndexRecipeView> decodeRecipeViews(ListTag list) {
        return decodeRecipes(list).stream().map(RecipeWithProvider::recipe).toList();
    }

    public static Map<Identifier, IndexRecipeDisplayMetadata> decodeDisplayMetadata(ListTag list) {
        Map<Identifier, IndexRecipeDisplayMetadata> metadata = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompoundOrEmpty(i);
            Identifier recipeId = Identifier.tryParse(tag.getStringOr("id", ""));
            CompoundTag displayTag = tag.getCompoundOrEmpty("display");
            if (recipeId == null || displayTag.isEmpty()) {
                continue;
            }
            IndexRecipeDisplayMetadata decoded = decodeDisplay(recipeId, displayTag);
            if (decoded.vanillaLayout() || decoded.hasMachineLayout()) {
                metadata.put(recipeId, decoded);
            }
        }
        return Map.copyOf(metadata);
    }

    public static List<IndexRecipeProviderStats> decodeProviderStats(ListTag list) {
        return decodeStats(list);
    }

    public static List<String> decodeWarnings(CompoundTag tag) {
        return readStrings(tag, "warning");
    }

    public static ListTag encodeRecipeViews(List<IndexRecipeView> recipes) {
        return encodeRecipeViews(recipes, Map.of());
    }

    public static ListTag encodeRecipeViews(List<IndexRecipeView> recipes,
            Map<Identifier, IndexRecipeDisplayMetadata> metadata) {
        ListTag list = new ListTag();
        if (recipes == null) {
            return list;
        }
        for (IndexRecipeView recipe : recipes) {
            if (recipe == null || recipe.id() == null || recipe.categoryId() == null) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("id", recipe.id().toString());
            tag.putString("provider", Identifier.withDefaultNamespace("query").toString());
            tag.putString("category", recipe.categoryId().toString());
            tag.putString("title", recipe.title());
            tag.put("machine", encodeStack(recipe.machine()));
            tag.put("slots", encodeSlots(recipe.slots()));
            writeStrings(tag, "note", recipe.notes());
            tag.putInt("ticks", recipe.processTicks());
            tag.putBoolean("locked", recipe.locked());
            tag.putString("source_mod", recipe.sourceModId());
            IndexRecipeDisplayMetadata display = metadata == null ? null : metadata.get(recipe.id());
            if (display != null && (display.vanillaLayout() || display.hasMachineLayout())) {
                tag.put("display", encodeDisplay(display));
            }
            list.add(tag);
        }
        return list;
    }

    private static void writeHealth(CompoundTag root, IndexRecipeSnapshot snapshot) {
        root.putLong("created_at", snapshot.createdAtMillis());
        root.putLong("generation", snapshot.generation());
        root.putString("reason", snapshot.buildReason());
        root.putInt("raw_recipe_count", snapshot.rawRecipeCount());
        root.putInt("adapted_recipe_count", snapshot.adaptedRecipeCount());
        root.putInt("source_card_count", snapshot.sourceCardCount());
        root.putInt("source_fact_count", snapshot.sourceFactCount());
        root.putInt("usage_item_count", snapshot.usageItemCount());
        root.putInt("provider_count", snapshot.providerCount());
        root.putInt("skipped_recipe_count", snapshot.skippedRecipeCount());
        root.putInt("warning_count", snapshot.warnings().size());
        root.putString("last_provider_error", snapshot.lastProviderError());
    }

    private static List<IndexRecipeView> limitQueryViews(List<IndexRecipeView> views, int limit) {
        if (views == null || views.isEmpty()) {
            return List.of();
        }
        return views.stream()
                .filter(recipe -> recipe != null)
                .limit(Math.max(0, limit))
                .toList();
    }

    private static int count(List<IndexRecipeView> views) {
        return views == null ? 0 : views.size();
    }

    private static String queryWarning(String warning,
            int recipeCount, int visibleRecipeCount,
            int useCount, int visibleUseCount,
            int sourceCount, int visibleSourceCount) {
        StringBuilder builder = new StringBuilder();
        if (warning != null && !warning.isBlank()) {
            builder.append(warning.strip());
        }
        appendLimitWarning(builder, "recipes", recipeCount, visibleRecipeCount);
        appendLimitWarning(builder, "uses", useCount, visibleUseCount);
        appendLimitWarning(builder, "sources", sourceCount, visibleSourceCount);
        return builder.toString();
    }

    private static void appendLimitWarning(StringBuilder builder, String label, int total, int visible) {
        if (total <= visible) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append("Showing first ")
                .append(visible)
                .append(" of ")
                .append(total)
                .append(' ')
                .append(label)
                .append('.');
    }

    private static ListTag encodeCategories(List<IndexRecipeCategory> categories) {
        ListTag list = new ListTag();
        for (IndexRecipeCategory category : categories) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", category.id().toString());
            tag.putString("title", category.title());
            tag.put("icon", encodeStack(category.icon()));
            tag.putInt("color", category.accentColor());
            tag.putInt("order", category.order());
            list.add(tag);
        }
        return list;
    }

    private static List<IndexRecipeCategory> decodeCategories(ListTag list) {
        List<IndexRecipeCategory> categories = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompoundOrEmpty(i);
            Identifier id = Identifier.tryParse(tag.getStringOr("id", ""));
            if (id == null) {
                continue;
            }
            categories.add(new IndexRecipeCategory(id, tag.getStringOr("title", id.getPath()),
                    decodeStack(tag.getCompoundOrEmpty("icon")), tag.getIntOr("color", 0xFF66E8FF),
                    tag.getIntOr("order", 0)));
        }
        return categories;
    }

    private static ListTag encodeRecipes(IndexRecipeSnapshot snapshot) {
        Map<Identifier, Identifier> providers = new LinkedHashMap<>();
        snapshot.byProvider().forEach((providerId, recipes) -> recipes.forEach(recipe -> providers.put(recipe.id(), providerId)));
        ListTag list = new ListTag();
        for (IndexRecipeView recipe : snapshot.recipes()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", recipe.id().toString());
            tag.putString("provider", providers.getOrDefault(recipe.id(), Identifier.withDefaultNamespace("unknown")).toString());
            tag.putString("category", recipe.categoryId().toString());
            tag.putString("title", recipe.title());
            tag.put("machine", encodeStack(recipe.machine()));
            tag.put("slots", encodeSlots(recipe.slots()));
            writeStrings(tag, "note", recipe.notes());
            tag.putInt("ticks", recipe.processTicks());
            tag.putBoolean("locked", recipe.locked());
            tag.putString("source_mod", recipe.sourceModId());
            IndexRecipeDisplayMetadata metadata = snapshot.displayMetadata().get(recipe.id());
            if (metadata != null && (metadata.vanillaLayout() || metadata.hasMachineLayout())) {
                tag.put("display", encodeDisplay(metadata));
            }
            list.add(tag);
        }
        return list;
    }

    private static List<RecipeWithProvider> decodeRecipes(ListTag list) {
        List<RecipeWithProvider> recipes = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompoundOrEmpty(i);
            Identifier id = Identifier.tryParse(tag.getStringOr("id", ""));
            Identifier category = Identifier.tryParse(tag.getStringOr("category", ""));
            if (id == null || category == null) {
                continue;
            }
            Identifier provider = Identifier.tryParse(tag.getStringOr("provider", ""));
            if (provider == null) {
                provider = Identifier.withDefaultNamespace("unknown");
            }
            recipes.add(new RecipeWithProvider(provider, new IndexRecipeView(id, category,
                    tag.getStringOr("title", id.getPath()),
                    decodeStack(tag.getCompoundOrEmpty("machine")),
                    decodeSlots(tag.getListOrEmpty("slots")),
                    readStrings(tag, "note"),
                    tag.getIntOr("ticks", 0),
                    tag.getBooleanOr("locked", false),
                    tag.getStringOr("source_mod", id.getNamespace()))));
        }
        return recipes;
    }

    private static ListTag encodeSlots(List<IndexRecipeSlot> slots) {
        ListTag list = new ListTag();
        for (IndexRecipeSlot slot : slots) {
            CompoundTag tag = new CompoundTag();
            tag.putString("role", slot.role().name());
            tag.putString("label", slot.label());
            ListTag stacks = new ListTag();
            for (ItemStack stack : slot.stacks()) {
                stacks.add(encodeStack(stack));
            }
            tag.put("stacks", stacks);
            list.add(tag);
        }
        return list;
    }

    private static List<IndexRecipeSlot> decodeSlots(ListTag list) {
        List<IndexRecipeSlot> slots = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompoundOrEmpty(i);
            IndexSlotRole role;
            try {
                role = IndexSlotRole.valueOf(tag.getStringOr("role", "INFO"));
            } catch (RuntimeException exception) {
                role = IndexSlotRole.INFO;
            }
            List<ItemStack> stacks = new ArrayList<>();
            ListTag stackTags = tag.getListOrEmpty("stacks");
            for (int j = 0; j < stackTags.size(); j++) {
                ItemStack stack = decodeStack(stackTags.getCompoundOrEmpty(j));
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
            slots.add(new IndexRecipeSlot(role, stacks, tag.getStringOr("label", "")));
        }
        return slots;
    }

    private static CompoundTag encodeDisplay(IndexRecipeDisplayMetadata metadata) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", metadata.type().name());
        tag.putInt("width", metadata.width());
        tag.putInt("height", metadata.height());
        tag.put("machine", encodeStack(metadata.machine()));
        tag.put("output", encodeStack(metadata.output()));
        ListTag cells = new ListTag();
        for (List<ItemStack> cell : metadata.cells()) {
            CompoundTag cellTag = new CompoundTag();
            ListTag stacks = new ListTag();
            for (ItemStack stack : cell) {
                stacks.add(encodeStack(stack));
            }
            cellTag.put("stacks", stacks);
            cells.add(cellTag);
        }
        tag.put("cells", cells);
        if (metadata.machineLayout() != null && !metadata.machineLayout().empty()) {
            tag.put("machine_layout", encodeMachineLayout(metadata.machineLayout()));
        }
        return tag;
    }

    private static IndexRecipeDisplayMetadata decodeDisplay(Identifier recipeId, CompoundTag tag) {
        IndexRecipeLayoutType type;
        try {
            type = IndexRecipeLayoutType.valueOf(tag.getStringOr("type", "GENERIC"));
        } catch (RuntimeException exception) {
            type = IndexRecipeLayoutType.GENERIC;
        }
        List<List<ItemStack>> cells = new ArrayList<>();
        ListTag cellTags = tag.getListOrEmpty("cells");
        for (int i = 0; i < cellTags.size(); i++) {
            CompoundTag cellTag = cellTags.getCompoundOrEmpty(i);
            List<ItemStack> stacks = new ArrayList<>();
            ListTag stackTags = cellTag.getListOrEmpty("stacks");
            for (int j = 0; j < stackTags.size(); j++) {
                ItemStack stack = decodeStack(stackTags.getCompoundOrEmpty(j));
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
            cells.add(List.copyOf(stacks));
        }
        return new IndexRecipeDisplayMetadata(recipeId, type, tag.getIntOr("width", 0),
                tag.getIntOr("height", 0), cells, decodeStack(tag.getCompoundOrEmpty("machine")),
                decodeStack(tag.getCompoundOrEmpty("output")), decodeMachineLayout(recipeId, tag.getCompoundOrEmpty("machine_layout")));
    }

    private static CompoundTag encodeMachineLayout(IndexMachineLayout layout) {
        CompoundTag tag = new CompoundTag();
        tag.putString("recipe", layout.recipeId().toString());
        tag.putString("template", layout.templateId());
        tag.putString("title", layout.title());
        tag.putInt("width", layout.width());
        tag.putInt("height", layout.height());
        tag.putBoolean("exact", layout.exact());
        ListTag slots = new ListTag();
        for (IndexMachineLayoutSlot slot : layout.slots()) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("recipe_slot", slot.recipeSlotIndex());
            slotTag.putString("role", slot.role().name());
            slotTag.putString("label", slot.label());
            slotTag.putInt("x", slot.x());
            slotTag.putInt("y", slot.y());
            slotTag.putInt("size", slot.size());
            slotTag.putBoolean("optional", slot.optional());
            slots.add(slotTag);
        }
        tag.put("slots", slots);
        ListTag gauges = new ListTag();
        for (IndexMachineLayoutGauge gauge : layout.gauges()) {
            CompoundTag gaugeTag = new CompoundTag();
            gaugeTag.putString("kind", gauge.kind());
            gaugeTag.putString("label", gauge.label());
            gaugeTag.putInt("x", gauge.x());
            gaugeTag.putInt("y", gauge.y());
            gaugeTag.putInt("width", gauge.width());
            gaugeTag.putInt("height", gauge.height());
            gaugeTag.putInt("color", gauge.color());
            gauges.add(gaugeTag);
        }
        tag.put("gauges", gauges);
        return tag;
    }

    private static IndexMachineLayout decodeMachineLayout(Identifier recipeId, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        List<IndexMachineLayoutSlot> slots = new ArrayList<>();
        ListTag slotTags = tag.getListOrEmpty("slots");
        for (int i = 0; i < slotTags.size(); i++) {
            CompoundTag slotTag = slotTags.getCompoundOrEmpty(i);
            IndexSlotRole role;
            try {
                role = IndexSlotRole.valueOf(slotTag.getStringOr("role", "INFO"));
            } catch (RuntimeException exception) {
                role = IndexSlotRole.INFO;
            }
            slots.add(new IndexMachineLayoutSlot(
                    slotTag.getIntOr("recipe_slot", -1),
                    role,
                    slotTag.getStringOr("label", ""),
                    slotTag.getIntOr("x", 0),
                    slotTag.getIntOr("y", 0),
                    slotTag.getIntOr("size", 18),
                    slotTag.getBooleanOr("optional", false)));
        }
        List<IndexMachineLayoutGauge> gauges = new ArrayList<>();
        ListTag gaugeTags = tag.getListOrEmpty("gauges");
        for (int i = 0; i < gaugeTags.size(); i++) {
            CompoundTag gaugeTag = gaugeTags.getCompoundOrEmpty(i);
            gauges.add(new IndexMachineLayoutGauge(
                    gaugeTag.getStringOr("kind", "progress"),
                    gaugeTag.getStringOr("label", ""),
                    gaugeTag.getIntOr("x", 0),
                    gaugeTag.getIntOr("y", 0),
                    gaugeTag.getIntOr("width", 0),
                    gaugeTag.getIntOr("height", 0),
                    gaugeTag.getIntOr("color", 0xFF66E8FF)));
        }
        return new IndexMachineLayout(recipeId,
                tag.getStringOr("template", "generic"),
                tag.getStringOr("title", ""),
                tag.getIntOr("width", 1),
                tag.getIntOr("height", 1),
                tag.getBooleanOr("exact", false),
                slots,
                gauges);
    }

    private static ListTag encodeStats(List<IndexRecipeProviderStats> stats) {
        ListTag list = new ListTag();
        for (IndexRecipeProviderStats stat : stats) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", stat.providerId().toString());
            tag.putInt("categories", stat.categoryCount());
            tag.putInt("raw", stat.rawRecipeCount());
            tag.putInt("adapted", stat.adaptedRecipeCount());
            tag.putInt("source_facts", stat.sourceFactCount());
            tag.putInt("source_cards", stat.sourceCardCount());
            tag.putInt("skipped", stat.skippedRecipeCount());
            tag.putString("error", stat.lastError());
            list.add(tag);
        }
        return list;
    }

    private static List<IndexRecipeProviderStats> decodeStats(ListTag list) {
        List<IndexRecipeProviderStats> stats = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompoundOrEmpty(i);
            Identifier id = Identifier.tryParse(tag.getStringOr("id", ""));
            if (id == null) {
                continue;
            }
            stats.add(new IndexRecipeProviderStats(id, tag.getIntOr("categories", 0),
                    tag.getIntOr("raw", 0), tag.getIntOr("adapted", 0),
                    tag.getIntOr("source_facts", 0), tag.getIntOr("source_cards", 0),
                    tag.getIntOr("skipped", 0), tag.getStringOr("error", "")));
        }
        return stats;
    }

    private static CompoundTag encodeStack(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        if (stack == null || stack.isEmpty()) {
            return tag;
        }
        tag.putString("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        tag.putInt("count", Math.max(1, stack.getCount()));
        return tag;
    }

    private static ItemStack decodeStack(CompoundTag tag) {
        Identifier id = Identifier.tryParse(tag.getStringOr("item", ""));
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, Math.max(1, tag.getIntOr("count", 1)));
    }

    private static void writeStrings(CompoundTag tag, String prefix, List<String> values) {
        tag.putInt(prefix + "_count", values.size());
        for (int i = 0; i < values.size(); i++) {
            tag.putString(prefix + "_" + i, values.get(i));
        }
    }

    private static List<String> readStrings(CompoundTag tag, String prefix) {
        int count = tag.getIntOr(prefix + "_count", 0);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String value = tag.getStringOr(prefix + "_" + i, "");
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private static void addItems(Map<Item, List<IndexRecipeView>> index, Iterable<Item> items, IndexRecipeView recipe) {
        for (Item item : items) {
            if (item != null && item != Items.AIR) {
                index.computeIfAbsent(item, ignored -> new ArrayList<>()).add(recipe);
            }
        }
    }

    private static Map<Item, List<IndexRecipeView>> freezeItemIndex(Map<Item, List<IndexRecipeView>> mutable) {
        Map<Item, List<IndexRecipeView>> frozen = new LinkedHashMap<>();
        mutable.forEach((id, recipes) -> frozen.put(id, List.copyOf(recipes)));
        return frozen;
    }

    private static Map<Identifier, List<IndexRecipeView>> freezeIdentifierIndex(
            Map<Identifier, List<IndexRecipeView>> mutable) {
        Map<Identifier, List<IndexRecipeView>> frozen = new LinkedHashMap<>();
        mutable.forEach((id, recipes) -> frozen.put(id, List.copyOf(recipes)));
        return frozen;
    }

    private record RecipeWithProvider(Identifier providerId, IndexRecipeView recipe) {
    }
}
