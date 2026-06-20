package com.knoxhack.echoindex.client;

import com.echoplatform.echocore.api.index.IndexRecipeCategory;
import com.echoplatform.echocore.api.index.IndexRecipeSlot;
import com.echoplatform.echocore.api.index.IndexRecipeView;
import com.echoplatform.echocore.api.index.IndexSlotRole;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echoindex.Config;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.service.ClientIndexState;
import com.knoxhack.echoindex.service.IndexRecipeDisplayMetadata;
import com.knoxhack.echoindex.service.IndexRecipeSnapshot;
import com.knoxhack.echoindex.service.IndexService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class IndexRecipeCache {
    private static CacheSnapshot cached = CacheSnapshot.blank();
    private static long providerRefreshCount;

    private IndexRecipeCache() {
    }

    public static CacheSnapshot snapshot() {
        Player player = clientPlayer();
        IndexRecipeSnapshot recipeSnapshot = IndexService.INSTANCE.recipeSnapshot(player);
        long itemRevision = IndexService.INSTANCE.itemCatalogRevision();
        long recipeGeneration = recipeSnapshot.generation();
        long clientStateRevision = ClientIndexState.revision();
        long favoriteRevision = IndexFavoriteStore.revision();
        long historyRevision = IndexHistoryStore.revision();
        if (!cached.empty()
                && cached.itemRevision() == itemRevision
                && cached.recipeGeneration() == recipeGeneration
                && cached.clientStateRevision() == clientStateRevision
                && cached.favoriteRevision() == favoriteRevision
                && cached.historyRevision() == historyRevision) {
            return cached;
        }
        long start = System.nanoTime();
        CacheSnapshot built = build(player, recipeSnapshot, itemRevision, recipeGeneration,
                clientStateRevision, favoriteRevision, historyRevision);
        providerRefreshCount++;
        long elapsedMs = Math.max(0L, (System.nanoTime() - start) / 1_000_000L);
        cached = built.withStats(elapsedMs, providerRefreshCount);
        recordNativeCacheReadiness(cached);
        if (Config.DEBUG_PROVIDERS.get()) {
            EchoIndex.LOGGER.info("ECHO: ScreenCore Index cache rebuilt in {} ms: {} item(s), {} recipe(s), {} machine(s).",
                    elapsedMs, cached.items().size(), cached.recipes().size(), cached.machines().size());
        }
        return cached;
    }

    public static void invalidate() {
        cached = CacheSnapshot.blank();
    }

    private static void recordNativeCacheReadiness(CacheSnapshot snapshot) {
        if (snapshot == null || snapshot.empty() || !EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive()) {
            return;
        }
        try {
            IndexNativeSessionBridge.recordNativeReadinessSnapshot("index_recipe_cache_rebuilt", Map.of(
                    "source", "index_recipe_cache",
                    "eventType", "index_recipe_cache_rebuilt",
                    "itemCount", snapshot.items().size(),
                    "recipeCount", snapshot.recipes().size(),
                    "machineCount", snapshot.machines().size(),
                    "providerRefreshCount", snapshot.providerRefreshCount()));
        } catch (RuntimeException | LinkageError exception) {
            EchoIndex.LOGGER.debug("ECHO: Index native readiness snapshot skipped after cache rebuild.", exception);
        }
    }

    private static Player clientPlayer() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft == null ? null : minecraft.player;
        } catch (RuntimeException | LinkageError exception) {
            return null;
        }
    }

    private static CacheSnapshot build(Player player, IndexRecipeSnapshot snapshot, long itemRevision,
            long recipeGeneration, long clientStateRevision, long favoriteRevision, long historyRevision) {
        Map<Identifier, List<IndexRecipeView>> recipesByOutput = snapshot.byOutput().entrySet().stream()
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(IndexService.itemId(entry.getKey()), entry.getValue()),
                        LinkedHashMap::putAll);
        Map<Identifier, List<IndexRecipeView>> recipesByUsage = snapshot.byUsage().entrySet().stream()
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(IndexService.itemId(entry.getKey()), entry.getValue()),
                        LinkedHashMap::putAll);

        List<IndexItemData> items = new ArrayList<>();
        for (ItemStack stack : IndexService.INSTANCE.itemCatalog(player)) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            Identifier id = IndexService.itemId(stack.getItem());
            List<String> tags = itemTags(stack);
            int recipeCount = recipesByOutput.getOrDefault(id, List.of()).size();
            int usageCount = recipesByUsage.getOrDefault(id, List.of()).size();
            String category = categoryKey(stack, id);
            boolean favorite = IndexFavoriteStore.contains("item", id.toString()) || ClientIndexState.isBookmarked(id);
            String name = safeStackName(stack, id);
            items.add(new IndexItemData(
                    id.toString(),
                    id.toString(),
                    name,
                    name,
                    id.getNamespace(),
                    IndexAddonPresentation.displayName(id.getNamespace()),
                    categoryLabel(category),
                    category,
                    tags,
                    id.toString(),
                    recipeCount > 0,
                    usageCount > 0,
                    recipeCount,
                    usageCount,
                    favorite,
                    ClientIndexState.isBookmarked(id),
                    false,
                    stack.getMaxStackSize(),
                    stack.isDamageableItem() ? stack.getMaxDamage() : 0,
                    stack.getRarity().name().toLowerCase(Locale.ROOT),
                    List.of(id.toString(), IndexAddonPresentation.displayName(id.getNamespace()), categoryLabel(category))));
        }

        Map<Identifier, IndexRecipeCategory> categories = new LinkedHashMap<>();
        for (IndexRecipeCategory category : snapshot.categories()) {
            categories.put(category.id(), category);
        }

        ArrayList<IndexRecipeData> recipes = new ArrayList<>();
        LinkedHashMap<String, MachineBuilder> machineBuilders = new LinkedHashMap<>();
        for (IndexRecipeView recipe : snapshot.recipes()) {
            if (recipe == null || recipe.id() == null) {
                continue;
            }
            IndexRecipeCategory category = categories.get(recipe.categoryId());
            IndexRecipeDisplayMetadata metadata = snapshot.metadata(recipe.id()).orElse(null);
            MachineDescriptor machine = machineDescriptor(recipe, category);
            machineBuilders.computeIfAbsent(machine.id(), ignored -> new MachineBuilder(machine))
                    .add(recipe);
            List<Map<String, Object>> inputs = slotRows(recipe, IndexSlotRole.INPUT);
            List<Map<String, Object>> outputs = slotRows(recipe, IndexSlotRole.OUTPUT);
            String type = recipe.categoryId().toString();
            String typeName = category == null ? IndexAddonPresentation.categoryLabel(recipe.categoryId()) : category.title();
            recipes.add(new IndexRecipeData(
                    recipe.id().toString(),
                    type,
                    typeName,
                    recipe.sourceModId(),
                    IndexAddonPresentation.displayName(recipe.sourceModId()),
                    machine.id(),
                    machine.name(),
                    recipe.title(),
                    firstIcon(outputs, machine.icon()),
                    inputs,
                    outputs,
                    summarize(inputs),
                    summarize(outputs),
                    Math.max(0, recipe.processTicks()),
                    0,
                    "",
                    IndexFavoriteStore.contains("recipe", recipe.id().toString()) || ClientIndexState.isRecipePinned(recipe.id()),
                    recipe.locked(),
                    metadata == null ? "generic" : metadata.type().name().toLowerCase(Locale.ROOT),
                    recipe.notes(),
                    Config.DEBUG_RECIPE_PARSING.get() ? debugInfo(recipe, metadata) : ""));
        }

        List<IndexMachineData> machines = machineBuilders.values().stream()
                .map(MachineBuilder::toData)
                .sorted(Comparator.comparing(IndexMachineData::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<IndexModData> mods = buildMods(items, recipes, machines);
        Map<String, IndexItemData> itemById = byId(items, IndexItemData::id);
        Map<String, IndexRecipeData> recipeById = byId(recipes, IndexRecipeData::id);
        Map<String, IndexMachineData> machineById = byId(machines, IndexMachineData::id);
        Map<String, IndexModData> modById = byId(mods, IndexModData::id);
        return new CacheSnapshot(false, itemRevision, recipeGeneration, clientStateRevision, favoriteRevision, historyRevision,
                0L, providerRefreshCount, snapshot.healthLine(), snapshot.warnings(), items, recipes, machines, mods,
                itemById, recipeById, machineById, modById);
    }

    private static List<IndexModData> buildMods(List<IndexItemData> items, List<IndexRecipeData> recipes,
            List<IndexMachineData> machines) {
        LinkedHashMap<String, ModBuilder> builders = new LinkedHashMap<>();
        for (IndexItemData item : items) {
            builders.computeIfAbsent(item.modId(), ModBuilder::new).items++;
        }
        for (IndexRecipeData recipe : recipes) {
            builders.computeIfAbsent(recipe.modId(), ModBuilder::new).recipes++;
        }
        for (IndexMachineData machine : machines) {
            builders.computeIfAbsent(machine.modId(), ModBuilder::new).machines++;
        }
        return builders.values().stream()
                .map(ModBuilder::toData)
                .sorted(Comparator.comparing(IndexModData::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static MachineDescriptor machineDescriptor(IndexRecipeView recipe, IndexRecipeCategory category) {
        ItemStack stack = recipe.machine();
        if (stack == null || stack.isEmpty()) {
            stack = firstStack(recipe, IndexSlotRole.MACHINE);
        }
        if (stack != null && !stack.isEmpty()) {
            Identifier id = IndexService.itemId(stack.getItem());
            return new MachineDescriptor(id.toString(), safeStackName(stack, id),
                    id.getNamespace(), IndexAddonPresentation.displayName(id.getNamespace()), id.toString());
        }
        String categoryId = recipe.categoryId().toString();
        String name = category == null ? IndexAddonPresentation.categoryLabel(recipe.categoryId()) : category.title();
        String icon = category == null || category.icon().isEmpty()
                ? "minecraft:crafting_table"
                : IndexService.itemId(category.icon().getItem()).toString();
        return new MachineDescriptor("type:" + categoryId, name, recipe.sourceModId(),
                IndexAddonPresentation.displayName(recipe.sourceModId()), icon);
    }

    private static List<Map<String, Object>> slotRows(IndexRecipeView recipe, IndexSlotRole role) {
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        for (IndexRecipeSlot slot : recipe.slots()) {
            if (slot.role() != role) {
                continue;
            }
            if (slot.stacks().isEmpty()) {
                rows.add(slotMap("", slot.label(), 0, List.of()));
                continue;
            }
            List<Map<String, Object>> alternatives = slot.stacks().stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .map(stack -> {
                        Identifier id = IndexService.itemId(stack.getItem());
                        return slotMap(id.toString(), safeStackName(stack, id),
                                Math.max(1, stack.getCount()), List.of());
                    })
                    .toList();
            if (!alternatives.isEmpty()) {
                Map<String, Object> first = new LinkedHashMap<>(alternatives.getFirst());
                first.put("alternatives", alternatives);
                first.put("alternativeCount", alternatives.size());
                rows.add(first);
            }
        }
        return List.copyOf(rows);
    }

    private static Map<String, Object> slotMap(String id, String label, int count, List<Map<String, Object>> alternatives) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("item", id);
        map.put("icon", id);
        map.put("label", label == null || label.isBlank() ? id : label);
        map.put("count", count);
        map.put("alternatives", alternatives);
        return map;
    }

    private static String summarize(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> String.valueOf(row.getOrDefault("label", "")))
                .filter(value -> !value.isBlank())
                .limit(4)
                .reduce((left, right) -> left + ", " + right)
                .orElse("None");
    }

    private static ItemStack firstStack(IndexRecipeView recipe, IndexSlotRole role) {
        for (IndexRecipeSlot slot : recipe.slots()) {
            if (slot.role() != role) {
                continue;
            }
            for (ItemStack stack : slot.stacks()) {
                if (stack != null && !stack.isEmpty()) {
                    return stack.copy();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static String firstIcon(List<Map<String, Object>> outputs, String fallback) {
        for (Map<String, Object> row : outputs) {
            String icon = String.valueOf(row.getOrDefault("icon", ""));
            if (!icon.isBlank()) {
                return icon;
            }
        }
        return fallback == null || fallback.isBlank() ? "minecraft:crafting_table" : fallback;
    }

    private static String safeStackName(ItemStack stack, Identifier id) {
        if (stack == null || stack.isEmpty()) {
            return fallbackStackName(id);
        }
        if (EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive()) {
            return fallbackStackName(id);
        }
        try {
            String name = stack.getHoverName().getString();
            return name == null || name.isBlank() ? fallbackStackName(id) : name;
        } catch (RuntimeException | LinkageError exception) {
            return fallbackStackName(id);
        }
    }

    private static String fallbackStackName(Identifier id) {
        if (id == null) {
            return "Unknown Item";
        }
        String path = id.getPath();
        if (path == null || path.isBlank()) {
            return id.toString();
        }
        String[] parts = path.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? id.toString() : builder.toString();
    }

    private static List<String> itemTags(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().tags()
                .map(TagKey::location)
                .map(Identifier::toString)
                .sorted()
                .toList();
    }

    private static String categoryKey(ItemStack stack, Identifier id) {
        Item item = stack.getItem();
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String name = safeStackName(stack, id).toLowerCase(Locale.ROOT);
        String haystack = path + " " + name;
        if (item instanceof BlockItem && hasAny(haystack, "machine", "station", "bench", "fabricator", "generator",
                "press", "grinder", "refinery", "smelter", "terminal", "console", "purifier", "charger")) {
            return "machines";
        }
        if (item instanceof BlockItem) {
            return "blocks";
        }
        if (hasAny(haystack, "helmet", "chestplate", "leggings", "boots", "armor")) {
            return "armor";
        }
        if (hasAny(haystack, "sword", "bow", "shield", "rifle", "gun", "blade", "hammer", "staff", "dagger")) {
            return "weapons";
        }
        if (stack.isDamageableItem()) {
            return "tools";
        }
        if (hasAny(haystack, "circuit", "wire", "plate", "gear", "scrap", "membrane", "component", "cell")) {
            return "components";
        }
        return "items";
    }

    private static String categoryLabel(String category) {
        return switch (category) {
            case "blocks" -> "Blocks";
            case "tools" -> "Tools";
            case "weapons" -> "Weapons";
            case "armor" -> "Armor";
            case "machines" -> "Machines";
            case "components" -> "Components";
            default -> "Items";
        };
    }

    private static boolean hasAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String debugInfo(IndexRecipeView recipe, IndexRecipeDisplayMetadata metadata) {
        return recipe.id() + " | layout=" + (metadata == null ? "none" : metadata.type())
                + " | slots=" + recipe.slots().size();
    }

    private static <T> Map<String, T> byId(List<T> values, IdGetter<T> getter) {
        LinkedHashMap<String, T> map = new LinkedHashMap<>();
        for (T value : values) {
            map.put(getter.id(value), value);
        }
        return Map.copyOf(map);
    }

    public record CacheSnapshot(
            boolean empty,
            long itemRevision,
            long recipeGeneration,
            long clientStateRevision,
            long favoriteRevision,
            long historyRevision,
            long buildTimeMs,
            long providerRefreshCount,
            String healthLine,
            List<String> warnings,
            List<IndexItemData> items,
            List<IndexRecipeData> recipes,
            List<IndexMachineData> machines,
            List<IndexModData> mods,
            Map<String, IndexItemData> itemById,
            Map<String, IndexRecipeData> recipeById,
            Map<String, IndexMachineData> machineById,
            Map<String, IndexModData> modById) {
        static CacheSnapshot blank() {
            return new CacheSnapshot(true, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE,
                    0L, 0L, "empty", List.of(), List.of(), List.of(), List.of(), List.of(),
                    Map.of(), Map.of(), Map.of(), Map.of());
        }

        CacheSnapshot withStats(long elapsedMs, long refreshes) {
            return new CacheSnapshot(false, itemRevision, recipeGeneration, clientStateRevision, favoriteRevision,
                    historyRevision, elapsedMs, refreshes, healthLine, warnings, items, recipes, machines, mods,
                    itemById, recipeById, machineById, modById);
        }

        Map<String, Object> stats(IndexUiState state) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("itemCount", items.size());
            map.put("recipeCount", recipes.size());
            map.put("machineCount", machines.size());
            map.put("modCount", mods.size());
            map.put("visibleItemCount", IndexSearchService.filterItems(items, state).size());
            map.put("visibleRecipeCount", IndexSearchService.filterRecipes(recipes, state).size());
            map.put("visibleMachineCount", IndexSearchService.filterMachines(machines, state).size());
            map.put("cacheBuildTimeMs", buildTimeMs);
            map.put("providerRefreshCount", providerRefreshCount);
            map.put("lastReload", healthLine);
            map.put("warningCount", warnings.size());
            return map;
        }
    }

    public record IndexItemData(
            String id,
            String registryId,
            String name,
            String displayName,
            String modId,
            String modName,
            String category,
            String categoryKey,
            List<String> tags,
            String icon,
            boolean hasRecipes,
            boolean hasUsages,
            int recipeCount,
            int usageCount,
            boolean favorite,
            boolean bookmarked,
            boolean locked,
            int stackSize,
            int durability,
            String rarity,
            List<String> tooltipLines) {
        Map<String, Object> toMap() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("registryId", registryId);
            map.put("name", name);
            map.put("displayName", displayName);
            map.put("modId", modId);
            map.put("modName", modName);
            map.put("category", category);
            map.put("categoryKey", categoryKey);
            map.put("tags", tags);
            map.put("tagSummary", String.join(", ", tags.stream().limit(3).toList()));
            map.put("icon", icon);
            map.put("hasRecipes", hasRecipes);
            map.put("hasUsages", hasUsages);
            map.put("recipeCount", recipeCount);
            map.put("usageCount", usageCount);
            map.put("isFavorite", favorite);
            map.put("isBookmarked", bookmarked);
            map.put("isLocked", locked);
            map.put("stackSize", stackSize);
            map.put("durability", durability);
            map.put("rarity", rarity);
            map.put("tooltipLines", tooltipLines);
            return map;
        }
    }

    public record IndexRecipeData(
            String id,
            String type,
            String typeName,
            String modId,
            String modName,
            String machineId,
            String machineName,
            String title,
            String icon,
            List<Map<String, Object>> inputs,
            List<Map<String, Object>> outputs,
            String inputSummary,
            String outputSummary,
            int processingTime,
            int energyCost,
            String fluidCost,
            boolean bookmarked,
            boolean locked,
            String layoutType,
            List<String> notes,
            String debugInfo) {
        Map<String, Object> toMap() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("type", type);
            map.put("typeName", typeName);
            map.put("modId", modId);
            map.put("modName", modName);
            map.put("machineId", machineId);
            map.put("machineName", machineName);
            map.put("title", title);
            map.put("icon", icon);
            map.put("inputs", inputs);
            map.put("outputs", outputs);
            map.put("inputSummary", inputSummary);
            map.put("outputSummary", outputSummary);
            map.put("processingTime", processingTime);
            map.put("energyCost", energyCost);
            map.put("fluidCost", fluidCost);
            map.put("isBookmarked", bookmarked);
            map.put("isLocked", locked);
            map.put("layoutType", layoutType);
            map.put("notes", notes);
            map.put("debugInfo", debugInfo);
            return map;
        }
    }

    public record IndexMachineData(
            String id,
            String name,
            String modId,
            String modName,
            String icon,
            List<String> recipeTypes,
            int inputSlots,
            int outputSlots,
            boolean energyRequired,
            boolean fluidRequired,
            int recipeCount,
            boolean locked,
            boolean bookmarked,
            String description) {
        Map<String, Object> toMap() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("name", name);
            map.put("modId", modId);
            map.put("modName", modName);
            map.put("icon", icon);
            map.put("recipeTypes", recipeTypes);
            map.put("recipeTypeSummary", String.join(", ", recipeTypes));
            map.put("inputSlots", inputSlots);
            map.put("outputSlots", outputSlots);
            map.put("energyRequired", energyRequired);
            map.put("fluidRequired", fluidRequired);
            map.put("recipeCount", recipeCount);
            map.put("isLocked", locked);
            map.put("isBookmarked", bookmarked);
            map.put("description", description);
            return map;
        }
    }

    public record IndexModData(String id, String name, String shortLabel, String icon, int itemCount,
            int recipeCount, int machineCount, String version, int accent) {
        Map<String, Object> toMap() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("name", name);
            map.put("shortLabel", shortLabel);
            map.put("icon", icon);
            map.put("itemCount", itemCount);
            map.put("recipeCount", recipeCount);
            map.put("machineCount", machineCount);
            map.put("version", version);
            map.put("accent", accent);
            return map;
        }
    }

    private record MachineDescriptor(String id, String name, String modId, String modName, String icon) {
    }

    private static final class MachineBuilder {
        private final MachineDescriptor descriptor;
        private final Set<String> recipeTypes = new LinkedHashSet<>();
        private int recipeCount;
        private int inputSlots;
        private int outputSlots;

        private MachineBuilder(MachineDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        private void add(IndexRecipeView recipe) {
            recipeCount++;
            recipeTypes.add(IndexAddonPresentation.categoryLabel(recipe.categoryId()));
            int inputs = 0;
            int outputs = 0;
            for (IndexRecipeSlot slot : recipe.slots()) {
                if (slot.role() == IndexSlotRole.INPUT) {
                    inputs++;
                } else if (slot.role() == IndexSlotRole.OUTPUT) {
                    outputs++;
                }
            }
            inputSlots = Math.max(inputSlots, inputs);
            outputSlots = Math.max(outputSlots, outputs);
        }

        private IndexMachineData toData() {
            return new IndexMachineData(descriptor.id(), descriptor.name(), descriptor.modId(), descriptor.modName(),
                    descriptor.icon(), List.copyOf(recipeTypes), inputSlots, outputSlots, false, false, recipeCount,
                    false, IndexFavoriteStore.contains("machine", descriptor.id()),
                    "Supports " + recipeCount + " indexed recipe" + (recipeCount == 1 ? "" : "s") + ".");
        }
    }

    private static final class ModBuilder {
        private final String modId;
        private int items;
        private int recipes;
        private int machines;

        private ModBuilder(String modId) {
            this.modId = modId;
        }

        private IndexModData toData() {
            IndexAddonPresentation.Style style = IndexAddonPresentation.style(modId);
            String icon = style.icon().isEmpty() ? "minecraft:book" : IndexService.itemId(style.icon().getItem()).toString();
            return new IndexModData(modId, style.displayName(), style.shortLabel(), icon, items, recipes, machines,
                    style.version(), style.accent());
        }
    }

    @FunctionalInterface
    private interface IdGetter<T> {
        String id(T value);
    }
}
