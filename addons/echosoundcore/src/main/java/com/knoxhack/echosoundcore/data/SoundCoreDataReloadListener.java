package com.knoxhack.echosoundcore.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.SoundCoreAudioPriority;
import com.knoxhack.echosoundcore.SoundCoreChapter;
import com.knoxhack.echosoundcore.SoundCoreCombatIntensity;
import com.knoxhack.echosoundcore.api.SoundCoreAmbienceProfile;
import com.knoxhack.echosoundcore.api.SoundCoreMusicProfile;
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

public final class SoundCoreDataReloadListener extends SimplePreparableReloadListener<Map<String, List<Object>>> {
    public static final SoundCoreDataReloadListener INSTANCE = new SoundCoreDataReloadListener();

    private static final String MUSIC_DIR = EchoSoundCore.MODID + "/audio_profiles/music";
    private static final String AMBIENCE_DIR = EchoSoundCore.MODID + "/audio_profiles/ambience";

    private static final Map<Identifier, SoundCoreMusicProfile> LOADED_MUSIC = new LinkedHashMap<>();
    private static final Map<Identifier, SoundCoreAmbienceProfile> LOADED_AMBIENCE = new LinkedHashMap<>();

    private SoundCoreDataReloadListener() {}

    @Override
    protected Map<String, List<Object>> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, List<Object>> result = new LinkedHashMap<>();
        result.put("music", new ArrayList<>());
        result.put("ambience", new ArrayList<>());

        for (Map.Entry<Identifier, Resource> entry : manager.listResources(MUSIC_DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonObject()) {
                    SoundCoreMusicProfile profile = parseMusicProfile(root.getAsJsonObject());
                    result.get("music").add(profile);
                }
            } catch (IOException | RuntimeException e) {
                EchoSoundCore.LOGGER.warn("Could not parse SoundCore music profile {}.", entry.getKey(), e);
            }
        }

        for (Map.Entry<Identifier, Resource> entry : manager.listResources(AMBIENCE_DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonObject()) {
                    SoundCoreAmbienceProfile profile = parseAmbienceProfile(root.getAsJsonObject());
                    result.get("ambience").add(profile);
                }
            } catch (IOException | RuntimeException e) {
                EchoSoundCore.LOGGER.warn("Could not parse SoundCore ambience profile {}.", entry.getKey(), e);
            }
        }

        return result;
    }

    @Override
    protected void apply(Map<String, List<Object>> data, ResourceManager manager, ProfilerFiller profiler) {
        LOADED_MUSIC.clear();
        for (Object o : data.get("music")) {
            SoundCoreMusicProfile p = (SoundCoreMusicProfile) o;
            LOADED_MUSIC.put(p.id(), p);
        }
        LOADED_AMBIENCE.clear();
        for (Object o : data.get("ambience")) {
            SoundCoreAmbienceProfile p = (SoundCoreAmbienceProfile) o;
            LOADED_AMBIENCE.put(p.id(), p);
        }
        EchoSoundCore.LOGGER.info("SoundCore reloaded {} music profile(s) and {} ambience profile(s).", LOADED_MUSIC.size(), LOADED_AMBIENCE.size());
    }

    public static List<SoundCoreMusicProfile> getMusicProfiles() {
        return List.copyOf(LOADED_MUSIC.values());
    }

    public static SoundCoreMusicProfile getMusicProfile(Identifier id) {
        return id == null ? null : LOADED_MUSIC.get(id);
    }

    public static List<SoundCoreAmbienceProfile> getAmbienceProfiles() {
        return List.copyOf(LOADED_AMBIENCE.values());
    }

    public static SoundCoreAmbienceProfile getAmbienceProfile(Identifier id) {
        return id == null ? null : LOADED_AMBIENCE.get(id);
    }

    private static SoundCoreMusicProfile parseMusicProfile(JsonObject json) {
        Identifier id = id(json, "id");
        Identifier sound = id(json, "sound");
        SoundCoreAudioPriority priority = enumVal(json, "priority", SoundCoreAudioPriority.IDLE, SoundCoreAudioPriority.class);
        String category = string(json, "category", "general");
        SoundCoreChapter chapter = enumVal(json, "chapter", SoundCoreChapter.UNKNOWN, SoundCoreChapter.class);
        Identifier biome = optId(json, "biome");
        Identifier region = optId(json, "region");
        Identifier structure = optId(json, "structure");
        Identifier faction = optId(json, "faction");
        SoundCoreCombatIntensity combat = enumVal(json, "combatIntensity", SoundCoreCombatIntensity.NONE, SoundCoreCombatIntensity.class);
        Identifier boss = optId(json, "boss");
        int minDelay = intVal(json, "minDelay", 0);
        int maxDelay = intVal(json, "maxDelay", 0);
        float fadeIn = floatVal(json, "fadeIn", 0.0f);
        float fadeOut = floatVal(json, "fadeOut", 0.0f);
        float weight = floatVal(json, "weight", 1.0f);
        boolean conditions = json.has("conditions") ? json.get("conditions").getAsBoolean() : true;
        return new SoundCoreMusicProfile(id, sound, priority, category, chapter, biome, region, structure, faction, combat, boss, minDelay, maxDelay, fadeIn, fadeOut, weight, conditions);
    }

    private static SoundCoreAmbienceProfile parseAmbienceProfile(JsonObject json) {
        Identifier id = id(json, "id");
        Identifier sound = id(json, "sound");
        String layer = string(json, "layer", "default");
        SoundCoreChapter chapter = enumVal(json, "chapter", SoundCoreChapter.UNKNOWN, SoundCoreChapter.class);
        Identifier biome = optId(json, "biome");
        String hazard = string(json, "hazard", "");
        Identifier structure = optId(json, "structure");
        Identifier faction = optId(json, "faction");
        boolean loop = json.has("loop") ? json.get("loop").getAsBoolean() : true;
        float fadeIn = floatVal(json, "fadeIn", 0.0f);
        float fadeOut = floatVal(json, "fadeOut", 0.0f);
        float volume = floatVal(json, "volume", 1.0f);
        float pitch = floatVal(json, "pitch", 1.0f);
        return new SoundCoreAmbienceProfile(id, sound, layer, chapter, biome, hazard, structure, faction, loop, fadeIn, fadeOut, volume, pitch);
    }

    private static Identifier id(JsonObject json, String key) {
        if (!json.has(key)) throw new IllegalArgumentException("Missing required field: " + key);
        return Identifier.parse(json.get(key).getAsString());
    }

    private static Identifier optId(JsonObject json, String key) {
        if (!json.has(key)) return null;
        String s = json.get(key).getAsString();
        return s.isEmpty() ? null : Identifier.parse(s);
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static int intVal(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static float floatVal(JsonObject json, String key, float fallback) {
        return json.has(key) ? json.get(key).getAsFloat() : fallback;
    }

    private static <E extends Enum<E>> E enumVal(JsonObject json, String key, E fallback, Class<E> clazz) {
        if (!json.has(key)) return fallback;
        try {
            return Enum.valueOf(clazz, json.get(key).getAsString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
