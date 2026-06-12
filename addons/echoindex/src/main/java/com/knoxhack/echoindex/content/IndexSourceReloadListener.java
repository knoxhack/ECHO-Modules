package com.knoxhack.echoindex.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.echoplatform.echocore.api.index.IndexSourceKind;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echoindex.service.IndexSourceRecipeProvider;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;

public final class IndexSourceReloadListener
        extends SimplePreparableReloadListener<List<IndexSourceRecipeProvider.SourceFact>> {
    private static final String SOURCE_FACT_DIR = "echo_index/source_facts";
    private static final String LOOT_TABLE_DIR = "loot_table";
    private static final String WORLDGEN_DIR = "worldgen";

    @Override
    protected List<IndexSourceRecipeProvider.SourceFact> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, IndexSourceRecipeProvider.SourceFact> facts = new LinkedHashMap<>();
        readExplicitSourceFacts(manager, facts);
        scanLootTables(manager, facts);
        scanWorldgen(manager, facts);
        return List.copyOf(facts.values());
    }

    private static void readExplicitSourceFacts(ResourceManager manager,
            Map<String, IndexSourceRecipeProvider.SourceFact> facts) {
        scan(manager, SOURCE_FACT_DIR, (resourceId, json) -> {
            if (!json.isJsonObject()) {
                EchoIndex.LOGGER.warn("Index source fact {} ignored because the root is not an object.", resourceId);
                return;
            }
            JsonObject object = json.getAsJsonObject();
            Set<Identifier> itemIds = itemIds(object);
            if (itemIds.isEmpty()) {
                EchoIndex.LOGGER.warn("Index source fact {} ignored because it has no valid item/items.", resourceId);
                return;
            }
            Identifier sourceId = identifier(object, "source",
                    identifier(object, "source_id", identifier(object, "id", contentId(resourceId, SOURCE_FACT_DIR))));
            IndexSourceKind kind = kind(object);
            String title = string(object, "title", kind.label());
            List<String> notes = stringList(object, "notes");
            Identifier iconItemId = optionalItemId(identifier(object, "icon", null));
            String sourceMod = string(object, "source_mod", string(object, "sourceMod", resourceId.getNamespace()));
            for (Identifier itemId : itemIds) {
                add(facts, new IndexSourceRecipeProvider.SourceFact(
                        itemId,
                        sourceId,
                        kind,
                        title,
                        notes,
                        ItemStack.EMPTY,
                        iconItemId,
                        sourceMod));
            }
        });
    }

    @Override
    protected void apply(List<IndexSourceRecipeProvider.SourceFact> payload, ResourceManager manager, ProfilerFiller profiler) {
        IndexSourceRecipeProvider.INSTANCE.replaceSources(payload);
        IndexService.INSTANCE.invalidateRecipes("source resources reloaded");
        EchoIndex.LOGGER.debug("ECHO: Index loaded {} source card fact(s).", payload.size());
    }

    private static void scanLootTables(ResourceManager manager, Map<String, IndexSourceRecipeProvider.SourceFact> facts) {
        scan(manager, LOOT_TABLE_DIR, (resourceId, json) -> {
            Set<Identifier> itemIds = collectItemIds(json);
            Identifier blockItemId = blockLootItemId(resourceId);
            if (blockItemId != null) {
                itemIds.add(blockItemId);
            }
            boolean blockLoot = isBlockLoot(resourceId);
            for (Identifier itemId : itemIds) {
                add(facts, new IndexSourceRecipeProvider.SourceFact(
                        itemId,
                        resourceId,
                        blockLoot ? IndexSourceKind.BLOCK_DROP : IndexSourceKind.LOOT_TABLE,
                        blockLoot ? "Block Drop" : "Loot Source",
                        lootNotes(resourceId, blockLoot),
                        ItemStack.EMPTY,
                        blockLoot ? itemId : Identifier.fromNamespaceAndPath("minecraft", "chest"),
                        resourceId.getNamespace()));
            }
        });
    }

    private static void scanWorldgen(ResourceManager manager, Map<String, IndexSourceRecipeProvider.SourceFact> facts) {
        scan(manager, WORLDGEN_DIR, (resourceId, json) -> {
            for (Identifier itemId : collectItemIds(json)) {
                add(facts, new IndexSourceRecipeProvider.SourceFact(
                        itemId,
                        resourceId,
                        IndexSourceKind.WORLDGEN,
                        "World Generation",
                        List.of("Referenced by world generation.", "Definition: " + displayId(resourceId)),
                        ItemStack.EMPTY,
                        Identifier.fromNamespaceAndPath("minecraft", "grass_block"),
                        resourceId.getNamespace()));
            }
        });
    }

    private static void scan(ResourceManager manager, String directory, JsonConsumer consumer) {
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(directory, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                consumer.accept(resourceId, JsonParser.parseReader(reader));
            } catch (IOException | RuntimeException exception) {
                EchoIndex.LOGGER.debug("Could not inspect Index source data {}.", resourceId, exception);
            }
        }
    }

    private static Set<Identifier> collectItemIds(JsonElement element) {
        Set<Identifier> ids = new LinkedHashSet<>();
        collectItemIds(element, ids);
        return ids;
    }

    private static Set<Identifier> itemIds(JsonObject json) {
        Set<Identifier> ids = new LinkedHashSet<>();
        Identifier single = identifier(json, "item", null);
        if (single != null && BuiltInRegistries.ITEM.getOptional(single).isPresent()) {
            ids.add(single);
        }
        JsonArray array = array(json, "items");
        if (array != null) {
            for (JsonElement element : array) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }
                Identifier id = Identifier.tryParse(element.getAsString());
                if (id != null && BuiltInRegistries.ITEM.getOptional(id).isPresent()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private static List<String> stringList(JsonObject json, String key) {
        JsonArray array = array(json, key);
        if (array == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                String value = element.getAsString();
                if (!value.isBlank()) {
                    values.add(value.strip());
                }
            }
        }
        return values;
    }

    private static IndexSourceKind kind(JsonObject json) {
        String raw = string(json, "kind", "source_card").trim().toLowerCase(java.util.Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return switch (raw) {
            case "block", "drop", "block_drop", "block_loot" -> IndexSourceKind.BLOCK_DROP;
            case "loot", "loot_table", "loot_source" -> IndexSourceKind.LOOT_TABLE;
            case "world", "worldgen", "world_generation" -> IndexSourceKind.WORLDGEN;
            case "structure", "poi" -> IndexSourceKind.STRUCTURE;
            case "trade", "trader", "merchant" -> IndexSourceKind.TRADER;
            case "cache", "loot_cache", "supply_cache" -> IndexSourceKind.CACHE;
            case "mission", "mission_reward", "reward" -> IndexSourceKind.MISSION_REWARD;
            case "route", "route_unlock", "route_record" -> IndexSourceKind.ROUTE_UNLOCK;
            case "research", "schematic" -> IndexSourceKind.RESEARCH;
            case "machine", "process" -> IndexSourceKind.MACHINE;
            case "unknown" -> IndexSourceKind.UNKNOWN;
            default -> IndexSourceKind.SOURCE_CARD;
        };
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        String value = string(json, key, "");
        Identifier id = value.isBlank() ? null : Identifier.tryParse(value);
        return id == null ? fallback : id;
    }

    private static Identifier optionalItemId(Identifier id) {
        return id != null && BuiltInRegistries.ITEM.getOptional(id).isPresent() ? id : null;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static JsonArray array(JsonObject json, String key) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static void collectItemIds(JsonElement element, Set<Identifier> ids) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            Identifier id = Identifier.tryParse(element.getAsString());
            if (id != null && BuiltInRegistries.ITEM.getOptional(id).isPresent()) {
                ids.add(id);
            }
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectItemIds(child, ids);
            }
            return;
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> child : element.getAsJsonObject().entrySet()) {
                collectItemIds(child.getValue(), ids);
            }
        }
    }

    private static Identifier blockLootItemId(Identifier resourceId) {
        String prefix = LOOT_TABLE_DIR + "/blocks/";
        String path = resourceId.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return null;
        }
        Identifier itemId = Identifier.fromNamespaceAndPath(
                resourceId.getNamespace(),
                path.substring(prefix.length(), path.length() - ".json".length()));
        return BuiltInRegistries.ITEM.getOptional(itemId).isPresent() ? itemId : null;
    }

    private static boolean isBlockLoot(Identifier resourceId) {
        return resourceId.getPath().startsWith(LOOT_TABLE_DIR + "/blocks/");
    }

    private static List<String> lootNotes(Identifier resourceId, boolean blockLoot) {
        List<String> notes = new ArrayList<>();
        notes.add(blockLoot ? "Drops from a block loot table." : "Referenced by a loot table.");
        notes.add("Loot table: " + displayId(resourceId));
        return notes;
    }

    private static void add(Map<String, IndexSourceRecipeProvider.SourceFact> facts,
            IndexSourceRecipeProvider.SourceFact fact) {
        facts.putIfAbsent(fact.itemId() + "|" + fact.kind() + "|" + fact.title() + "|" + fact.sourceId(), fact);
    }

    private static Identifier contentId(Identifier resourceId, String directory) {
        String path = resourceId.getPath();
        if (path.startsWith(directory + "/")) {
            path = path.substring(directory.length() + 1);
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }

    private static String displayId(Identifier resourceId) {
        String path = resourceId.getPath();
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return resourceId.getNamespace() + ":" + path;
    }

    @FunctionalInterface
    private interface JsonConsumer {
        void accept(Identifier resourceId, JsonElement json);
    }
}
