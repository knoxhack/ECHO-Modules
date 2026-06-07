package com.knoxhack.echothemecore.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echothemecore.EchoThemeCore;
import com.knoxhack.echothemecore.api.EchoThemeRenderPreset;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class RenderPresetReloadListener extends SimplePreparableReloadListener<Map<Identifier, EchoThemeRenderPreset>> {
    private static final List<String> PRESET_DIRS = List.of("render_presets", EchoThemeCore.MODID + "/render_presets");

    @Override
    protected Map<Identifier, EchoThemeRenderPreset> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, EchoThemeRenderPreset> presets = new LinkedHashMap<>();
        for (String presetDir : PRESET_DIRS) {
            for (Map.Entry<Identifier, Resource> entry : manager.listResources(presetDir, id -> id.getPath().endsWith(".json")).entrySet()) {
                Identifier resourceId = entry.getKey();
                Identifier fallbackId = contentId(resourceId, presetDir);
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement root = JsonParser.parseReader(reader);
                    if (!root.isJsonObject()) {
                        throw new JsonParseException("Root must be a JSON object.");
                    }
                    EchoThemeRenderPreset preset = parsePreset(fallbackId, root.getAsJsonObject());
                    if (presets.putIfAbsent(preset.id(), preset) != null) {
                        EchoThemeCore.LOGGER.warn("Duplicate ThemeCore render preset id {} from {} ignored.", preset.id(), resourceId);
                    } else if (ThemeCoreConfig.debugThemeLogging()) {
                        EchoThemeCore.LOGGER.info("Loaded ThemeCore render preset {} from {}.", preset.id(), resourceId);
                    }
                } catch (IOException | RuntimeException exception) {
                    EchoThemeCore.LOGGER.warn("Could not parse ThemeCore render preset file {}.", resourceId, exception);
                }
            }
        }
        return presets;
    }

    @Override
    protected void apply(Map<Identifier, EchoThemeRenderPreset> presets, ResourceManager manager, ProfilerFiller profiler) {
        RenderPresetRegistry.replaceLoaded(presets);
    }

    public static EchoThemeRenderPreset parsePresetForTests(Identifier fallbackId, JsonObject json) {
        return parsePreset(fallbackId, json);
    }

    private static EchoThemeRenderPreset parsePreset(Identifier fallbackId, JsonObject json) {
        Identifier id = identifier(json, "id", fallbackId);
        Identifier theme = identifier(json, "theme", ThemeRegistry.ECHO_PLATFORM_ID);
        String type = string(json, "type", "custom");
        return new EchoThemeRenderPreset(
            id,
            theme,
            type,
            stringMap(object(json, "colors")),
            effects(json.get("effects")),
            stringMap(object(json, "metadata"))
        );
    }

    private static List<EchoThemeRenderPreset.Effect> effects(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        List<EchoThemeRenderPreset.Effect> result = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                continue;
            }
            JsonObject json = entry.getAsJsonObject();
            result.add(new EchoThemeRenderPreset.Effect(
                string(json, "type", "unknown"),
                integer(json, "duration_ticks", 0),
                string(json, "strength", ""),
                string(json, "color", ""),
                string(json, "particle_style", ""),
                integer(json, "count", 0),
                stringMap(json)
            ));
        }
        return List.copyOf(result);
    }

    private static Identifier contentId(Identifier resourceId, String folder) {
        String path = resourceId.getPath();
        String prefix = folder + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }

    private static JsonObject object(JsonObject json, String key) {
        JsonElement element = json == null ? null : json.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json == null ? null : json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement element = json == null ? null : json.get(key);
        return element == null || element.isJsonNull() ? fallback : Math.max(0, element.getAsInt());
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        Identifier parsed = Identifier.tryParse(string(json, key, ""));
        return parsed == null ? fallback : parsed;
    }

    private static Map<String, String> stringMap(JsonObject json) {
        if (json == null) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isJsonNull()) {
                result.put(entry.getKey(), scalarString(entry.getValue()));
            }
        }
        return result;
    }

    private static String scalarString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.isJsonPrimitive() ? element.getAsString() : element.toString();
    }
}
