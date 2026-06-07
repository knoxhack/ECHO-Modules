package com.knoxhack.echosoundcore.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.registry.SoundCoreSounds;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SoundCoreCatalogValidator {
    private static final String SOUNDS_JSON = "assets/echosoundcore/sounds.json";
    private static final String LANG_JSON = "assets/echosoundcore/lang/en_us.json";
    private static List<String> cachedMissingAssets;
    private static List<String> cachedIssues;

    private SoundCoreCatalogValidator() {
    }

    public static synchronized List<String> missingAssetPaths() {
        if (cachedMissingAssets == null) {
            cachedMissingAssets = List.copyOf(scanMissingAssetPaths());
        }
        return cachedMissingAssets;
    }

    public static synchronized void clearCache() {
        cachedMissingAssets = null;
        cachedIssues = null;
    }

    public static synchronized List<String> issues() {
        if (cachedIssues == null) {
            cachedIssues = List.copyOf(scanIssues());
        }
        return cachedIssues;
    }

    private static List<String> scanMissingAssetPaths() {
        List<String> missing = new ArrayList<>();
        ClassLoader loader = SoundCoreCatalogValidator.class.getClassLoader();
        try (var stream = loader.getResourceAsStream(SOUNDS_JSON)) {
            if (stream == null) {
                missing.add(SOUNDS_JSON);
                return missing;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var eventEntry : root.entrySet()) {
                JsonElement sounds = eventEntry.getValue().getAsJsonObject().get("sounds");
                if (sounds == null || !sounds.isJsonArray()) {
                    continue;
                }
                for (JsonElement element : sounds.getAsJsonArray()) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    String name = element.getAsJsonObject().has("name")
                            ? element.getAsJsonObject().get("name").getAsString()
                            : "";
                    if (!name.startsWith(EchoSoundCore.MODID + ":")) {
                        continue;
                    }
                    String path = "assets/echosoundcore/sounds/"
                            + name.substring((EchoSoundCore.MODID + ":").length()) + ".ogg";
                    if (loader.getResource(path) == null) {
                        missing.add(path);
                    }
                }
            }
        } catch (RuntimeException | java.io.IOException exception) {
            EchoSoundCore.LOGGER.warn("Could not validate SoundCore sound catalog.", exception);
            missing.add("catalog-parse-error:" + exception.getClass().getSimpleName());
        }
        missing.sort(Comparator.naturalOrder());
        return missing;
    }

    private static List<String> scanIssues() {
        Set<String> issues = new LinkedHashSet<>(missingAssetPaths());
        ClassLoader loader = SoundCoreCatalogValidator.class.getClassLoader();
        JsonObject soundsJson = readJsonObject(loader, SOUNDS_JSON, issues);
        JsonObject langJson = readJsonObject(loader, LANG_JSON, issues);
        if (soundsJson == null) {
            return sorted(issues);
        }
        Set<String> soundEvents = new HashSet<>(soundsJson.keySet());
        if (langJson != null) {
            for (var eventEntry : soundsJson.entrySet()) {
                JsonObject event = eventEntry.getValue().isJsonObject() ? eventEntry.getValue().getAsJsonObject() : null;
                if (event == null || !event.has("subtitle")) {
                    continue;
                }
                String subtitle = event.get("subtitle").getAsString();
                if (!langJson.has(subtitle)) {
                    issues.add("missing-lang:" + subtitle);
                }
            }
        }
        for (var holder : SoundCoreSounds.getEntries()) {
            String path = holder.getId().getPath();
            if (!soundEvents.contains(path)) {
                issues.add("missing-sounds-json-event:" + EchoSoundCore.MODID + ":" + path);
            }
        }
        return sorted(issues);
    }

    private static JsonObject readJsonObject(ClassLoader loader, String path, Set<String> issues) {
        try (var stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                issues.add(path);
                return null;
            }
            JsonElement parsed = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException | java.io.IOException exception) {
            EchoSoundCore.LOGGER.warn("Could not validate SoundCore catalog resource {}.", path, exception);
            issues.add("catalog-parse-error:" + path + ":" + exception.getClass().getSimpleName());
            return null;
        }
    }

    private static List<String> sorted(Set<String> issues) {
        List<String> sorted = new ArrayList<>(issues);
        sorted.sort(Comparator.naturalOrder());
        return sorted;
    }
}
