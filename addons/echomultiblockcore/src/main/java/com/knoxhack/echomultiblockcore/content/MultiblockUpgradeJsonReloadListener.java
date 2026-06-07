package com.knoxhack.echomultiblockcore.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.MultiblockUpgradeDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockUpgradeRegistry;
import com.knoxhack.echomultiblockcore.api.UpgradeModifier;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class MultiblockUpgradeJsonReloadListener extends SimplePreparableReloadListener<Map<Identifier, MultiblockUpgradeDefinition>> {
    private static final String DIRECTORY = "echo_multiblock_upgrades";

    @Override
    protected Map<Identifier, MultiblockUpgradeDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, MultiblockUpgradeDefinition> loaded = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier fallbackId = contentId(resourceId);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                MultiblockUpgradeDefinition definition = parseUpgrade(fallbackId, root.getAsJsonObject());
                if (loaded.put(definition.id(), definition) != null) {
                    EchoMultiblockCore.LOGGER.warn("Duplicate multiblock upgrade id {} from {} replaced an earlier entry.",
                            definition.id(), resourceId);
                }
            } catch (IOException | RuntimeException exception) {
                EchoMultiblockCore.LOGGER.warn("Could not parse multiblock upgrade file {}.", resourceId, exception);
            }
        }
        return loaded;
    }

    @Override
    protected void apply(Map<Identifier, MultiblockUpgradeDefinition> content, ResourceManager manager, ProfilerFiller profiler) {
        if ((content == null || content.isEmpty()) && !MultiblockUpgradeRegistry.snapshot().isEmpty()) {
            EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore reload produced no valid upgrades; keeping last good registry.");
            return;
        }
        MultiblockUpgradeRegistry.replaceUpgrades(content);
    }

    public static MultiblockUpgradeDefinition parseUpgradeForTests(Identifier fallbackId, JsonObject json) {
        return parseUpgrade(fallbackId, json);
    }

    private static MultiblockUpgradeDefinition parseUpgrade(Identifier fallbackId, JsonObject json) {
        Identifier id = identifier(json, "id", fallbackId);
        return new MultiblockUpgradeDefinition(
                id,
                string(json, "display_name", id.getPath().replace('_', ' ')),
                string(json, "category", "general"),
                identifier(json, "item", id),
                identifiers(json.get("allowed_multiblocks")),
                modifiers(json.get("modifiers")),
                strings(json.get("notes")));
    }

    private static List<UpgradeModifier> modifiers(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("$.modifiers: expected an array.");
        }
        List<UpgradeModifier> values = new ArrayList<>();
        int index = 0;
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                throw new JsonParseException("$.modifiers[" + index + "]: expected an object.");
            }
            JsonObject json = entry.getAsJsonObject();
            UpgradeModifier.Type type = UpgradeModifier.Type.byName(string(json, "type", "speed_multiplier"));
            values.add(new UpgradeModifier(type, number(json, "value", 0.0D)));
            index++;
        }
        return List.copyOf(values);
    }

    private static List<Identifier> identifiers(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("Expected array of identifiers.");
        }
        List<Identifier> values = new ArrayList<>();
        for (JsonElement value : element.getAsJsonArray()) {
            if (!value.isJsonNull()) {
                values.add(Identifier.parse(value.getAsString()));
            }
        }
        return List.copyOf(values);
    }

    private static List<String> strings(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("Expected array of strings.");
        }
        List<String> values = new ArrayList<>();
        for (JsonElement value : element.getAsJsonArray()) {
            String text = value.getAsString();
            if (!text.isBlank()) {
                values.add(text.strip());
            }
        }
        return List.copyOf(values);
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        String value = string(json, key, "");
        return value.isBlank() ? fallback : Identifier.parse(value);
    }

    private static String string(JsonObject json, String key, String fallback) {
        if (json == null) {
            return fallback;
        }
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static double number(JsonObject json, String key, double fallback) {
        if (json == null) {
            return fallback;
        }
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsDouble();
    }

    private static Identifier contentId(Identifier resourceId) {
        String path = resourceId.getPath();
        String prefix = DIRECTORY + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path.toLowerCase(Locale.ROOT));
    }
}
