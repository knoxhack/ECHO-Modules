package com.knoxhack.echomultiblockcore.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionParseResult;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionRegistry;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class MultiblockProgressionJsonReloadListener extends SimplePreparableReloadListener<Map<Identifier, MultiblockProgressionDefinition>> {
    private static final String DIRECTORY = "echo_multiblock_progression";

    @Override
    protected Map<Identifier, MultiblockProgressionDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, MultiblockProgressionDefinition> loaded = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier fallbackId = contentId(resourceId);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                MultiblockProgressionParseResult result = parseProgressionResult(resourceId, fallbackId, root.getAsJsonObject(), false);
                result.warnings().forEach(warning -> EchoMultiblockCore.LOGGER.warn(
                        "Progression warning in {} [{}]: {}", resourceId, result.progressionId(), warning));
                result.errors().forEach(error -> EchoMultiblockCore.LOGGER.warn(
                        "Progression error in {} [{}]: {}", resourceId, result.progressionId(), error));
                if (!result.valid()) {
                    continue;
                }
                MultiblockProgressionDefinition definition = result.definition();
                if (loaded.put(definition.id(), definition) != null) {
                    EchoMultiblockCore.LOGGER.warn("Duplicate progression id {} from {} replaced an earlier entry.",
                            definition.id(), resourceId);
                }
            } catch (IOException | RuntimeException exception) {
                EchoMultiblockCore.LOGGER.warn("Could not parse multiblock progression file {}.", resourceId, exception);
            }
        }
        return loaded;
    }

    @Override
    protected void apply(Map<Identifier, MultiblockProgressionDefinition> content, ResourceManager manager, ProfilerFiller profiler) {
        if ((content == null || content.isEmpty()) && !MultiblockProgressionRegistry.snapshot().isEmpty()) {
            EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore reload produced no valid progression data; keeping last good registry.");
            return;
        }
        MultiblockProgressionRegistry.replaceProgressions(content);
        MultiblockProgressionRegistry.all().forEach(definition -> {
            List<String> warnings = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            validate(definition, warnings, errors, true);
            warnings.forEach(warning -> EchoMultiblockCore.LOGGER.warn(
                    "Progression warning in {} [{}]: {}", definition.id(), definition.id(), warning));
            errors.forEach(error -> EchoMultiblockCore.LOGGER.warn(
                    "Progression error in {} [{}]: {}", definition.id(), definition.id(), error));
        });
    }

    public static MultiblockProgressionDefinition parseProgressionForTests(Identifier fallbackId, JsonObject json) {
        MultiblockProgressionParseResult result = parseProgressionResult(fallbackId, fallbackId, json, true);
        if (!result.valid()) {
            throw new JsonParseException(String.join("; ", result.errors()));
        }
        return result.definition();
    }

    public static MultiblockProgressionParseResult parseProgressionResultForTests(Identifier fallbackId, JsonObject json) {
        return parseProgressionResult(fallbackId, fallbackId, json, true);
    }

    private static MultiblockProgressionParseResult parseProgressionResult(Identifier resourceId, Identifier fallbackId, JsonObject json) {
        return parseProgressionResult(resourceId, fallbackId, json, true);
    }

    private static MultiblockProgressionParseResult parseProgressionResult(Identifier resourceId, Identifier fallbackId, JsonObject json, boolean resolveLinks) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Identifier id = fallbackId;
        MultiblockProgressionDefinition definition = null;
        try {
            definition = parseProgression(fallbackId, json);
            id = definition.id();
            validate(definition, warnings, errors, resolveLinks);
        } catch (RuntimeException exception) {
            errors.add("$.root: " + exception.getMessage());
        }
        return new MultiblockProgressionParseResult(resourceId, id, errors.isEmpty() ? definition : null, warnings, errors);
    }

    private static MultiblockProgressionDefinition parseProgression(Identifier fallbackId, JsonObject json) {
        Identifier id = identifier(json, "id", fallbackId);
        Identifier facilityId = identifier(json, "facility", identifier(json, "facility_id", id));
        return new MultiblockProgressionDefinition(
                id,
                facilityId,
                integer(json, "tier", 0),
                identifiers(json.get("prerequisites")),
                identifiers(json.get("featured_recipes")),
                identifiers(json.get("reward_items")),
                identifierOptional(json, "advancement", Identifier.fromNamespaceAndPath(id.getNamespace(), "multiblock/" + id.getPath())),
                string(json, "title", string(json, "display_name", facilityId.getPath().replace('_', ' '))),
                string(json, "guide", string(json, "guide_text", "")));
    }

    private static void validate(MultiblockProgressionDefinition definition, List<String> warnings, List<String> errors, boolean resolveLinks) {
        if (definition.facilityId() == null) {
            errors.add("$.facility: missing facility id.");
            return;
        }
        if (resolveLinks && MultiblockContent.definition(definition.facilityId()).isEmpty()) {
            warnings.add("$.facility: unknown or not-yet-loaded multiblock id " + definition.facilityId() + ".");
        }
        for (Identifier prerequisite : definition.prerequisites()) {
            if (prerequisite.equals(definition.facilityId())) {
                errors.add("$.prerequisites: facility cannot require itself.");
            }
            if (resolveLinks && MultiblockContent.definition(prerequisite).isEmpty()) {
                warnings.add("$.prerequisites: unknown or not-yet-loaded multiblock id " + prerequisite + ".");
            }
        }
        for (Identifier recipe : definition.featuredRecipes()) {
            if (resolveLinks && AutomationRecipeRegistry.byId(recipe).isEmpty()) {
                warnings.add("$.featured_recipes: recipe " + recipe + " is not currently loaded.");
            }
        }
        for (Identifier reward : definition.rewardItems()) {
            if (BuiltInRegistries.ITEM.getOptional(reward).isEmpty()) {
                warnings.add("$.reward_items: unknown item id " + reward + ".");
            }
        }
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

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        String value = string(json, key, "");
        return value.isBlank() ? fallback : Identifier.parse(value);
    }

    private static Identifier identifierOptional(JsonObject json, String key, Identifier fallback) {
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

    private static int integer(JsonObject json, String key, int fallback) {
        if (json == null) {
            return fallback;
        }
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsInt();
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
