package com.knoxhack.echoindex.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexRelation;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echocore.api.index.IndexVisibility;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.IndexIds;
import com.knoxhack.echoindex.service.IndexService;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class IndexJsonReloadListener extends SimplePreparableReloadListener<IndexJsonReloadListener.Payload> {
    private static final String CATEGORY_DIR = "echo_index/categories";
    private static final String ENTRY_DIR = "echo_index/entries";
    private static final String RECIPE_CARD_DIR = "echo_index/recipe_cards";
    private static final String RELATION_DIR = "echo_index/relations";

    @Override
    protected Payload prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, IndexCategory> categories = new LinkedHashMap<>();
        Map<Identifier, IndexEntry> entries = new LinkedHashMap<>();
        Map<Identifier, IndexRecipeCategory> recipeCategories = new LinkedHashMap<>();
        List<IndexRecipeView> recipes = new ArrayList<>();
        List<IndexRelation> relations = new ArrayList<>();
        read(manager, CATEGORY_DIR, (resourceId, json) -> {
            IndexCategory category = parseCategory(resourceId, json);
            categories.put(category.id(), category);
        });
        read(manager, ENTRY_DIR, (resourceId, json) -> {
            IndexEntry entry = parseEntry(resourceId, json);
            entries.put(entry.id(), entry);
        });
        read(manager, RECIPE_CARD_DIR, (resourceId, json) -> {
            IndexRecipeView recipe = parseRecipeCard(resourceId, json, recipeCategories);
            recipes.add(recipe);
        });
        read(manager, RELATION_DIR, (resourceId, json) -> relations.add(parseRelation(resourceId, json)));
        IndexContentSnapshot content = new IndexContentSnapshot(
                IndexIds.PROVIDER_DATAPACK,
                List.of(),
                List.of(),
                List.copyOf(recipeCategories.values()),
                recipes,
                List.of(),
                relations,
                List.of());
        return new Payload(categories, entries, content);
    }

    @Override
    protected void apply(Payload payload, ResourceManager manager, ProfilerFiller profiler) {
        IndexService.INSTANCE.replaceDataDriven(payload.categories(), payload.entries(), payload.content());
    }

    public static IndexCategory parseCategoryForTests(Identifier resourceId, JsonObject json) {
        return parseCategory(resourceId, json);
    }

    public static IndexEntry parseEntryForTests(Identifier resourceId, JsonObject json) {
        return parseEntry(resourceId, json);
    }

    private static void read(ResourceManager manager, String directory, JsonConsumer consumer) {
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(directory, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                consumer.accept(resourceId, root.getAsJsonObject());
            } catch (IOException | RuntimeException exception) {
                EchoIndex.LOGGER.warn("Could not parse Index definition {}.", resourceId, exception);
            }
        }
    }

    private static IndexCategory parseCategory(Identifier resourceId, JsonObject json) {
        Identifier id = identifier(json, "id", contentId(resourceId, CATEGORY_DIR));
        return new IndexCategory(
                id,
                string(json, "title", "echoindex.category." + id.getPath().replace('/', '.')),
                string(json, "description", ""),
                stack(json, "icon"),
                integer(json, "sort_order", integer(json, "sortOrder", 100)),
                string(json, "source_mod", string(json, "sourceMod", id.getNamespace())));
    }

    private static IndexEntry parseEntry(Identifier resourceId, JsonObject json) {
        Identifier id = identifier(json, "id", contentId(resourceId, ENTRY_DIR));
        Identifier category = identifier(json, "category", Identifier.fromNamespaceAndPath(EchoIndex.MODID, "tutorials"));
        return new IndexEntry(
                id,
                category,
                string(json, "title", "echoindex.entry." + id.getPath().replace('/', '.')),
                string(json, "subtitle", ""),
                string(json, "summary", ""),
                string(json, "body", ""),
                stack(json, "icon"),
                string(json, "source_mod", string(json, "sourceMod", id.getNamespace())),
                stringList(json, "tags"),
                state(json, "default_state", state(json, "defaultState", IndexEntryState.VISIBLE)),
                identifiers(json, "related"),
                identifiers(json, "linked_items"),
                identifiers(json, "linked_recipes"),
                integer(json, "sort_order", integer(json, "sortOrder", 100)));
    }

    private static IndexRecipeView parseRecipeCard(Identifier resourceId, JsonObject json,
            Map<Identifier, IndexRecipeCategory> recipeCategories) {
        Identifier id = identifier(json, "id", contentId(resourceId, RECIPE_CARD_DIR));
        Identifier categoryId = identifier(json, "category", identifier(json, "category_id",
                Identifier.fromNamespaceAndPath(id.getNamespace(), "recipe_cards")));
        ItemStack machine = stack(json, "machine", Items.CRAFTING_TABLE);
        recipeCategories.putIfAbsent(categoryId, new IndexRecipeCategory(
                categoryId,
                string(json, "category_title", string(json, "categoryTitle", categoryId.getPath())),
                stack(json, "category_icon", machine),
                integer(json, "accent_color", integer(json, "accentColor", 0xFF66E8FF)),
                integer(json, "category_order", integer(json, "categoryOrder", 500))));

        List<IndexRecipeSlot> slots = recipeSlots(json);
        if (slots.isEmpty() && !machine.isEmpty()) {
            slots = List.of(IndexRecipeSlot.machine(machine));
        }
        return new IndexRecipeView(
                id,
                categoryId,
                string(json, "title", id.getPath()),
                machine,
                slots,
                stringList(json, "notes"),
                integer(json, "process_ticks", integer(json, "processTicks", 0)),
                bool(json, "locked", false),
                string(json, "source_mod", string(json, "sourceMod", id.getNamespace())));
    }

    private static IndexRelation parseRelation(Identifier resourceId, JsonObject json) {
        Identifier id = identifier(json, "id", contentId(resourceId, RELATION_DIR));
        Identifier from = identifier(json, "from", identifier(json, "from_id", null));
        Identifier to = identifier(json, "to", identifier(json, "to_id", null));
        return new IndexRelation(
                id,
                from,
                to,
                string(json, "kind", "related"),
                string(json, "label", ""),
                visibility(json),
                string(json, "source_mod", string(json, "sourceMod", id.getNamespace())));
    }

    private static List<IndexRecipeSlot> recipeSlots(JsonObject json) {
        List<IndexRecipeSlot> slots = new ArrayList<>();
        JsonArray explicitSlots = array(json, "slots");
        if (explicitSlots != null) {
            for (JsonElement element : explicitSlots) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject slot = element.getAsJsonObject();
                List<ItemStack> stacks = stacks(slot);
                String label = string(slot, "label", "");
                if (!stacks.isEmpty() || !label.isBlank()) {
                    slots.add(new IndexRecipeSlot(role(slot), stacks, label));
                }
            }
        }
        addSimpleSlot(slots, json, "input", IndexSlotRole.INPUT, "");
        addSimpleSlot(slots, json, "inputs", IndexSlotRole.INPUT, "");
        addSimpleSlot(slots, json, "catalyst", IndexSlotRole.CATALYST, "Catalyst");
        addSimpleSlot(slots, json, "catalysts", IndexSlotRole.CATALYST, "Catalyst");
        addSimpleSlot(slots, json, "output", IndexSlotRole.OUTPUT, "");
        addSimpleSlot(slots, json, "outputs", IndexSlotRole.OUTPUT, "");
        addSimpleSlot(slots, json, "info", IndexSlotRole.INFO, string(json, "info", ""));
        return slots;
    }

    private static void addSimpleSlot(List<IndexRecipeSlot> slots, JsonObject json, String key,
            IndexSlotRole role, String label) {
        JsonElement element = json == null ? null : json.get(key);
        if (element == null || element.isJsonNull()) {
            return;
        }
        List<ItemStack> stacks = stacks(element);
        String safeLabel = label == null ? "" : label;
        if (!stacks.isEmpty() || !safeLabel.isBlank()) {
            slots.add(new IndexRecipeSlot(role, stacks, safeLabel));
        }
    }

    private static IndexSlotRole role(JsonObject json) {
        String raw = string(json, "role", "info").trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return switch (raw) {
            case "input", "ingredient" -> IndexSlotRole.INPUT;
            case "output", "result" -> IndexSlotRole.OUTPUT;
            case "catalyst", "fuel" -> IndexSlotRole.CATALYST;
            case "machine", "station" -> IndexSlotRole.MACHINE;
            default -> IndexSlotRole.INFO;
        };
    }

    private static IndexVisibility visibility(JsonObject json) {
        String raw = string(json, "visibility", "visible").trim().toUpperCase(Locale.ROOT);
        try {
            return IndexVisibility.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            return IndexVisibility.VISIBLE;
        }
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

    private static ItemStack stack(JsonObject json, String key) {
        return stack(json, key, Items.BOOK);
    }

    private static ItemStack stack(JsonObject json, String key, Item fallback) {
        ItemStack fallbackStack = fallback == null ? ItemStack.EMPTY : safeStack(fallback);
        return stack(json, key, fallbackStack);
    }

    private static ItemStack stack(JsonObject json, String key, ItemStack fallback) {
        Identifier id = identifier(json, key, null);
        return id == null ? fallback.copy() : stack(id, fallback);
    }

    private static ItemStack stack(Identifier id, ItemStack fallback) {
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        return item == null ? fallback.copy() : safeStack(item);
    }

    private static ItemStack safeStack(Item item) {
        try {
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        } catch (RuntimeException exception) {
            return ItemStack.EMPTY;
        }
    }

    private static List<ItemStack> stacks(JsonObject json) {
        Set<Identifier> ids = new LinkedHashSet<>();
        Identifier single = identifier(json, "item", null);
        if (single != null) {
            ids.add(single);
        }
        JsonArray array = array(json, "items");
        if (array != null) {
            for (JsonElement element : array) {
                ids.addAll(itemIds(element));
            }
        }
        return ids.stream()
                .map(id -> stack(id, ItemStack.EMPTY))
                .filter(stack -> !stack.isEmpty())
                .toList();
    }

    private static List<ItemStack> stacks(JsonElement element) {
        return itemIds(element).stream()
                .map(id -> stack(id, ItemStack.EMPTY))
                .filter(stack -> !stack.isEmpty())
                .toList();
    }

    private static Set<Identifier> itemIds(JsonElement element) {
        Set<Identifier> ids = new LinkedHashSet<>();
        if (element == null || element.isJsonNull()) {
            return ids;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            Identifier id = Identifier.tryParse(element.getAsString());
            if (id != null) {
                ids.add(id);
            }
            return ids;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                ids.addAll(itemIds(child));
            }
            return ids;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            Identifier single = identifier(object, "item", null);
            if (single != null) {
                ids.add(single);
            }
            JsonArray items = array(object, "items");
            if (items != null) {
                for (JsonElement child : items) {
                    ids.addAll(itemIds(child));
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
                    values.add(value);
                }
            }
        }
        return values;
    }

    private static List<Identifier> identifiers(JsonObject json, String key) {
        JsonArray array = array(json, key);
        if (array == null) {
            return List.of();
        }
        List<Identifier> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                Identifier id = Identifier.tryParse(element.getAsString());
                if (id != null) {
                    values.add(id);
                }
            }
        }
        return values;
    }

    private static IndexEntryState state(JsonObject json, String key, IndexEntryState fallback) {
        String value = string(json, key, "");
        if (value.isBlank()) {
            return fallback;
        }
        try {
            return IndexEntryState.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        String value = string(json, key, "");
        Identifier id = value.isBlank() ? null : Identifier.tryParse(value);
        return id == null ? fallback : id;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
    }

    private static JsonArray array(JsonObject json, String key) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    @FunctionalInterface
    private interface JsonConsumer {
        void accept(Identifier resourceId, JsonObject json);
    }

    public record Payload(
            Map<Identifier, IndexCategory> categories,
            Map<Identifier, IndexEntry> entries,
            IndexContentSnapshot content) {
    }
}
