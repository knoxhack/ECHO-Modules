package com.knoxhack.echoworldcore.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.echoplatform.echocore.api.WorldHazardDefinition;
import com.echoplatform.echocore.api.WorldRegionDefinition;
import com.echoplatform.echocore.api.WorldRegionType;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.knoxhack.echoworldcore.service.WorldRegionService;
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

public final class WorldCoreJsonReloadListener extends SimplePreparableReloadListener<WorldCoreJsonReloadListener.LoadedContent> {
    private static final String HAZARD_DIR = "echoworldcore/world_hazards";
    private static final String REGION_DIR = "echoworldcore/world_regions";

    @Override
    protected LoadedContent prepare(ResourceManager manager, ProfilerFiller profiler) {
        List<String> warnings = new ArrayList<>();
        Map<Identifier, WorldHazardDefinition> hazards = loadHazards(manager, warnings);
        Map<Identifier, WorldRegionDefinition> regions = loadRegions(manager, warnings);
        return new LoadedContent(hazards, regions, warnings);
    }

    @Override
    protected void apply(LoadedContent content, ResourceManager manager, ProfilerFiller profiler) {
        WorldRegionService.INSTANCE.replaceDataDefinitions(content.hazards(), content.regions(), content.warnings());
        EchoWorldCore.LOGGER.info("WorldCore loaded {} data region definition(s), {} data hazard definition(s), and {} warning(s).",
                content.regions().size(), content.hazards().size(), content.warnings().size());
    }

    public static WorldHazardDefinition parseHazardForTests(Identifier fallbackId, JsonObject json) {
        return parseHazard(fallbackId, json);
    }

    public static WorldRegionDefinition parseRegionForTests(Identifier fallbackId, JsonObject json) {
        return parseRegion(fallbackId, json);
    }

    private static Map<Identifier, WorldHazardDefinition> loadHazards(ResourceManager manager, List<String> warnings) {
        Map<Identifier, WorldHazardDefinition> hazards = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(HAZARD_DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier fallbackId = contentId(resourceId, HAZARD_DIR);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                WorldHazardDefinition hazard = parseHazard(fallbackId, root.getAsJsonObject());
                if (hazards.put(hazard.id(), hazard) != null) {
                    warnings.add("Duplicate WorldCore hazard id " + hazard.id() + " from " + resourceId
                            + " replaced earlier data entry.");
                    EchoWorldCore.LOGGER.warn("Duplicate WorldCore hazard id {} from {} replaced earlier data entry.",
                            hazard.id(), resourceId);
                }
            } catch (IOException | RuntimeException exception) {
                warnings.add("Could not parse WorldCore hazard file " + resourceId + ": " + exception.getMessage());
                EchoWorldCore.LOGGER.warn("Could not parse WorldCore hazard file {}.", resourceId, exception);
            }
        }
        return hazards;
    }

    private static Map<Identifier, WorldRegionDefinition> loadRegions(ResourceManager manager, List<String> warnings) {
        Map<Identifier, WorldRegionDefinition> regions = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(REGION_DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier fallbackId = contentId(resourceId, REGION_DIR);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                WorldRegionDefinition region = parseRegion(fallbackId, root.getAsJsonObject());
                if (regions.put(region.id(), region) != null) {
                    warnings.add("Duplicate WorldCore region id " + region.id() + " from " + resourceId
                            + " replaced earlier data entry.");
                    EchoWorldCore.LOGGER.warn("Duplicate WorldCore region id {} from {} replaced earlier data entry.",
                            region.id(), resourceId);
                }
            } catch (IOException | RuntimeException exception) {
                warnings.add("Could not parse WorldCore region file " + resourceId + ": " + exception.getMessage());
                EchoWorldCore.LOGGER.warn("Could not parse WorldCore region file {}.", resourceId, exception);
            }
        }
        return regions;
    }

    private static WorldHazardDefinition parseHazard(Identifier fallbackId, JsonObject json) {
        Identifier id = identifier(json, "id", fallbackId);
        int severity = integer(json, "defaultSeverity", integer(json, "severity", 0));
        if (severity < 0 || severity > 100) {
            throw new JsonParseException("WorldCore hazard " + id + " defaultSeverity must be between 0 and 100.");
        }
        return new WorldHazardDefinition(
                id,
                string(json, "displayName", string(json, "name", id.getPath())),
                string(json, "summary", ""),
                severity,
                bool(json, "ticking", false));
    }

    private static WorldRegionDefinition parseRegion(Identifier fallbackId, JsonObject json) {
        Identifier id = identifier(json, "id", fallbackId);
        int radius = integer(json, "radius", 96);
        if (radius < 16) {
            throw new JsonParseException("WorldCore region " + id + " radius must be at least 16.");
        }
        return new WorldRegionDefinition(
                id,
                regionType(id, string(json, "type", WorldRegionType.ANOMALY_ZONE.name())),
                string(json, "displayName", string(json, "name", id.getPath())),
                string(json, "summary", ""),
                identifierList(json, "biomeIds"),
                identifierList(json, "biomeTags"),
                identifierList(json, "structureIds"),
                identifierList(json, "hazardIds"),
                identifier(json, "discoveryId", id),
                radius,
                nullableIdentifier(json, "renderProfileId"),
                nullableIdentifier(json, "audioProfileId"),
                integer(json, "sortOrder", 0));
    }

    private static WorldRegionType regionType(Identifier id, String raw) {
        String normalized = raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return WorldRegionType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException("WorldCore region " + id + " has unknown type '" + raw + "'.", exception);
        }
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

    private static List<Identifier> identifierList(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("WorldCore field '" + key + "' must be an array of identifiers.");
        }
        JsonArray array = element.getAsJsonArray();
        List<Identifier> values = new ArrayList<>();
        for (JsonElement value : array) {
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

    private static Identifier nullableIdentifier(JsonObject json, String key) {
        String value = string(json, key, "");
        return value.isBlank() ? null : Identifier.parse(value);
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

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        if (json == null) {
            return fallback;
        }
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
    }

    public record LoadedContent(
            Map<Identifier, WorldHazardDefinition> hazards,
            Map<Identifier, WorldRegionDefinition> regions,
            List<String> warnings) {
        public LoadedContent {
            hazards = Map.copyOf(hazards == null ? Map.of() : hazards);
            regions = Map.copyOf(regions == null ? Map.of() : regions);
            warnings = List.copyOf(warnings == null ? List.of() : warnings);
        }
    }
}
