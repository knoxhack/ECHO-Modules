package com.knoxhack.echorecovery.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryItemRuleResult;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Items;

public final class RecoveryJsonReloadListener extends SimplePreparableReloadListener<RecoveryContent.LoadedContent> {
    private static final String GRAVE_TYPE_DIR = "echorecovery/recovery_grave_type";
    private static final String RULE_DIR = "echorecovery/recovery_rule";
    private static final String PRESET_DIR = "echorecovery/recovery_preset";
    private static final java.util.Set<String> PRESET_KEYS = java.util.Set.of(
            "enable_graves", "store_items", "store_armor", "store_offhand", "store_xp",
            "max_graves_per_player", "grave_expiration_minutes", "drop_overflow_items",
            "delete_empty_graves", "safe_placement", "safe_placement_radius", "grave_key_enabled",
            "grave_key_required", "grave_key_consumed", "recovery_compass_enabled",
            "recovery_compass_works_cross_dimension", "remote_recovery_enabled", "team_access");

    @Override
    protected RecoveryContent.LoadedContent prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, RecoveryGraveType> graveTypes = new LinkedHashMap<>();
        Map<Identifier, RecoveryRuleDefinition> rules = new LinkedHashMap<>();
        Map<Identifier, RecoveryPreset> presets = new LinkedHashMap<>();

        for (Map.Entry<Identifier, JsonObject> entry : jsonObjects(manager, GRAVE_TYPE_DIR).entrySet()) {
            RecoveryGraveType graveType = parseGraveType(entry.getKey(), entry.getValue());
            putUnique(graveTypes, graveType.id(), graveType, "grave type");
        }
        for (Map.Entry<Identifier, JsonObject> entry : jsonObjects(manager, RULE_DIR).entrySet()) {
            RecoveryRuleDefinition rule = parseRule(entry.getKey(), entry.getValue());
            putUnique(rules, rule.id(), rule, "rule");
        }
        for (Map.Entry<Identifier, JsonObject> entry : jsonObjects(manager, PRESET_DIR).entrySet()) {
            RecoveryPreset preset = parsePreset(entry.getKey(), entry.getValue());
            putUnique(presets, preset.id(), preset, "preset");
        }

        return new RecoveryContent.LoadedContent(graveTypes, rules, presets);
    }

    @Override
    protected void apply(RecoveryContent.LoadedContent loaded, ResourceManager manager, ProfilerFiller profiler) {
        RecoveryContent.replaceJsonContent(loaded);
    }

    public static RecoveryGraveType parseGraveTypeForTests(Identifier id, JsonObject json) {
        return parseGraveType(id, json);
    }

    public static RecoveryRuleDefinition parseRuleForTests(Identifier id, JsonObject json) {
        return parseRule(id, json);
    }

    public static RecoveryPreset parsePresetForTests(Identifier id, JsonObject json) {
        return parsePreset(id, json);
    }

    private static RecoveryGraveType parseGraveType(Identifier fileId, JsonObject json) {
        Identifier id = identifier(string(json, "id", fileId.toString()), "grave type id");
        Identifier block = optionalIdentifier(string(json, "block", EchoRecovery.MODID + ":grave"), "grave type block");
        if (block != null && BuiltInRegistries.BLOCK.getOptional(block).isEmpty()) {
            throw new JsonParseException("Unknown grave type block '" + block + "' in " + id);
        }
        Identifier texture = optionalIdentifier(string(json, "texture", "minecraft:block/stone"), "grave type texture");
        return new RecoveryGraveType(
                id,
                string(json, "display_name", string(json, "displayName", id.toString())),
                block,
                texture,
                bool(json, "contaminated", false),
                strings(json, "hazard_notes"));
    }

    private static RecoveryRuleDefinition parseRule(Identifier fileId, JsonObject json) {
        Identifier id = identifier(string(json, "id", fileId.toString()), "rule id");
        RecoveryItemRuleResult action = action(string(json, "action", string(json, "result", "")), id);
        int priority = integer(json, "priority", 0);
        Selector selector = selector(json, id);
        return new RecoveryRuleDefinition(id, action, selector.kind(), selector.id(), priority);
    }

    private static RecoveryPreset parsePreset(Identifier fileId, JsonObject json) {
        Identifier id = identifier(string(json, "id", fileId.toString()), "preset id");
        Map<String, String> values = new LinkedHashMap<>();
        JsonObject valuesJson = object(json, "values");
        if (valuesJson != null) {
            for (Map.Entry<String, JsonElement> entry : valuesJson.entrySet()) {
                if (!PRESET_KEYS.contains(entry.getKey())) {
                    throw new JsonParseException("Unknown Recovery preset key '" + entry.getKey() + "' in " + id);
                }
                if (entry.getValue() != null && !entry.getValue().isJsonNull()) {
                    values.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        return new RecoveryPreset(id, string(json, "display_name", id.toString()), values);
    }

    private static Selector selector(JsonObject json, Identifier ruleId) {
        String selector = string(json, "selector", "");
        if (!selector.isBlank()) {
            if (selector.startsWith("#")) {
                return new Selector(RecoveryRuleDefinition.SelectorKind.TAG, identifier(selector.substring(1), "tag selector"));
            }
            Identifier itemId = identifier(selector, "item selector");
            if (BuiltInRegistries.ITEM.getOptional(itemId).isEmpty() || BuiltInRegistries.ITEM.getValue(itemId) == Items.AIR) {
                throw new JsonParseException("Unknown item selector '" + selector + "' in rule " + ruleId);
            }
            return new Selector(RecoveryRuleDefinition.SelectorKind.ITEM, itemId);
        }
        String item = string(json, "item", "");
        if (!item.isBlank()) {
            Identifier itemId = identifier(item, "item selector");
            if (BuiltInRegistries.ITEM.getOptional(itemId).isEmpty() || BuiltInRegistries.ITEM.getValue(itemId) == Items.AIR) {
                throw new JsonParseException("Unknown item selector '" + item + "' in rule " + ruleId);
            }
            return new Selector(RecoveryRuleDefinition.SelectorKind.ITEM, itemId);
        }
        String tag = string(json, "tag", "");
        if (!tag.isBlank()) {
            return new Selector(RecoveryRuleDefinition.SelectorKind.TAG, identifier(stripHash(tag), "tag selector"));
        }
        String mod = string(json, "mod", "");
        if (!mod.isBlank()) {
            return new Selector(RecoveryRuleDefinition.SelectorKind.MOD, Identifier.fromNamespaceAndPath(mod, "all"));
        }
        throw new JsonParseException("Recovery rule " + ruleId + " is missing an item, tag, mod, or selector.");
    }

    private static RecoveryItemRuleResult action(String value, Identifier ruleId) {
        if (value == null || value.isBlank()) {
            throw new JsonParseException("Recovery rule " + ruleId + " is missing action.");
        }
        try {
            return RecoveryItemRuleResult.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException("Unknown recovery rule action '" + value + "' in " + ruleId, exception);
        }
    }

    private static Map<Identifier, JsonObject> jsonObjects(ResourceManager manager, String directory) {
        Map<Identifier, JsonObject> objects = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(directory, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier id = contentId(resourceId, directory);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                objects.put(id, root.getAsJsonObject());
            } catch (IOException | RuntimeException exception) {
                EchoRecovery.LOGGER.warn("Could not parse Recovery data file {}: {}", resourceId, exception.getMessage());
            }
        }
        return objects;
    }

    private static Identifier contentId(Identifier resourceId, String directory) {
        String path = resourceId.getPath();
        String prefix = directory + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }

    private static <T> void putUnique(Map<Identifier, T> target, Identifier id, T value, String type) {
        if (target.containsKey(id)) {
            throw new JsonParseException("Duplicate Recovery " + type + " id: " + id);
        }
        target.put(id, value);
    }

    private static JsonObject object(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsInt();
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
    }

    private static java.util.List<String> strings(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonArray()) {
            return java.util.List.of();
        }
        java.util.List<String> values = new java.util.ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            if (child != null && !child.isJsonNull()) {
                values.add(child.getAsString());
            }
        }
        return java.util.List.copyOf(values);
    }

    private static Identifier identifier(String value, String fieldName) {
        Identifier id = Identifier.tryParse(value == null ? "" : value.trim());
        if (id == null) {
            throw new JsonParseException("Invalid " + fieldName + ": " + value);
        }
        return id;
    }

    private static Identifier optionalIdentifier(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return identifier(value, fieldName);
    }

    private static String stripHash(String value) {
        return value != null && value.startsWith("#") ? value.substring(1) : value;
    }

    private record Selector(RecoveryRuleDefinition.SelectorKind kind, Identifier id) {
    }
}
