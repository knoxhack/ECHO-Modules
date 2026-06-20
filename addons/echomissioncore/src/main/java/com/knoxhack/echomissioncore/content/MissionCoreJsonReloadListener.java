package com.knoxhack.echomissioncore.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.MissionRepeatPolicy;
import com.echoplatform.echocore.api.mission.MissionRewardClaimMode;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echomissioncore.EchoMissionCore;
import com.knoxhack.echomissioncore.service.MissionCoreService;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MissionCoreJsonReloadListener extends SimplePreparableReloadListener<MissionCoreJsonReloadListener.LoadedContent> {
    private static final String CHAPTER_DIR = "missioncore/chapters";
    private static final String MISSION_DIR = "missioncore/missions";

    @Override
    protected LoadedContent prepare(ResourceManager manager, ProfilerFiller profiler) {
        return loadContent(manager);
    }

    @Override
    protected void apply(LoadedContent loaded, ResourceManager manager, ProfilerFiller profiler) {
        applyLoadedContent(loaded, "server_reload_listener");
    }

    public static MissionChapterDefinition parseChapterForTests(Identifier id, JsonObject json) {
        return parseChapter(id, json);
    }

    public static MissionDefinition parseMissionForTests(Identifier id, JsonObject json) {
        return parseMission(id, json);
    }

    public static int reloadNow(ResourceManager manager, String source) {
        if (manager == null) {
            return 0;
        }
        LoadedContent loaded = loadContent(manager);
        applyLoadedContent(loaded, source == null || source.isBlank() ? "direct_reload" : source);
        return loaded.missions().size();
    }

    public static int reloadIfPresent(ResourceManager manager, String source) {
        if (manager == null) {
            return 0;
        }
        LoadedContent loaded = loadContent(manager);
        if (loaded.chapters().isEmpty() && loaded.missions().isEmpty()) {
            return 0;
        }
        applyLoadedContent(loaded, source == null || source.isBlank() ? "direct_reload" : source);
        return loaded.missions().size();
    }

    public static int reloadNativeClasspathIfPresent(List<Path> moduleClasspath, String source) {
        LoadedContent loaded = loadNativeClasspath(moduleClasspath);
        if (loaded.chapters().isEmpty() && loaded.missions().isEmpty()) {
            return 0;
        }
        applyLoadedContent(loaded, source == null || source.isBlank() ? "native_classpath" : source);
        return loaded.missions().size();
    }

    public static LoadedContent loadNativeClasspathForTests(List<Path> moduleClasspath) {
        return loadNativeClasspath(moduleClasspath);
    }

    private static LoadedContent loadContent(ResourceManager manager) {
        return new LoadedContent(
                load(manager, CHAPTER_DIR, MissionCoreJsonReloadListener::parseChapter),
                load(manager, MISSION_DIR, MissionCoreJsonReloadListener::parseMission));
    }

    private static LoadedContent loadNativeClasspath(List<Path> moduleClasspath) {
        if (moduleClasspath == null || moduleClasspath.isEmpty()) {
            return new LoadedContent(List.of(), List.of());
        }
        Map<Identifier, MissionChapterDefinition> chapters = new LinkedHashMap<>();
        Map<Identifier, MissionDefinition> missions = new LinkedHashMap<>();
        for (Path path : moduleClasspath) {
            loadNativeClasspathJar(path, chapters, missions);
        }
        return new LoadedContent(List.copyOf(chapters.values()), List.copyOf(missions.values()));
    }

    private static void loadNativeClasspathJar(
            Path path,
            Map<Identifier, MissionChapterDefinition> chapters,
            Map<Identifier, MissionDefinition> missions) {
        if (path == null || !Files.isRegularFile(path) || !path.getFileName().toString().endsWith(".jar")) {
            return;
        }
        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".json")) {
                    continue;
                }
                loadNativeClasspathEntry(path, zip, entry, chapters, missions);
            }
        } catch (IOException | RuntimeException exception) {
            EchoMissionCore.LOGGER.debug("MissionCore skipped Native module classpath entry {}.", path, exception);
        }
    }

    private static void loadNativeClasspathEntry(
            Path jar,
            ZipFile zip,
            ZipEntry entry,
            Map<Identifier, MissionChapterDefinition> chapters,
            Map<Identifier, MissionDefinition> missions) {
        String entryName = entry.getName().replace('\\', '/');
        Identifier chapterId = nativeContentId(entryName, CHAPTER_DIR);
        Identifier missionId = nativeContentId(entryName, MISSION_DIR);
        if (chapterId == null && missionId == null) {
            return;
        }
        try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                throw new JsonParseException("Root must be a JSON object.");
            }
            if (chapterId != null) {
                if (chapters.putIfAbsent(chapterId, parseChapter(chapterId, root.getAsJsonObject())) != null) {
                    EchoMissionCore.LOGGER.warn("Duplicate MissionCore Native classpath chapter {} from {} ignored.",
                            chapterId, entryName);
                }
                return;
            }
            if (missions.putIfAbsent(missionId, parseMission(missionId, root.getAsJsonObject())) != null) {
                EchoMissionCore.LOGGER.warn("Duplicate MissionCore Native classpath mission {} from {} ignored.",
                        missionId, entryName);
            }
        } catch (IOException | RuntimeException exception) {
            EchoMissionCore.LOGGER.warn("Could not parse MissionCore Native classpath JSON {} from {}: {}",
                    entryName, jar, exception.getMessage());
        }
    }

    private static Identifier nativeContentId(String entryName, String directory) {
        String normalized = entryName == null ? "" : entryName.replace('\\', '/');
        String prefix = "data/";
        if (!normalized.startsWith(prefix)) {
            return null;
        }
        String path = normalized.substring(prefix.length());
        int namespaceEnd = path.indexOf('/');
        if (namespaceEnd <= 0) {
            return null;
        }
        String namespace = path.substring(0, namespaceEnd);
        String contentPath = path.substring(namespaceEnd + 1);
        String directoryPrefix = directory + "/";
        if (!contentPath.startsWith(directoryPrefix) || !contentPath.endsWith(".json")) {
            return null;
        }
        contentPath = contentPath.substring(directoryPrefix.length(), contentPath.length() - ".json".length());
        return Identifier.fromNamespaceAndPath(namespace, contentPath);
    }

    private static void applyLoadedContent(LoadedContent loaded, String source) {
        MissionCoreService.INSTANCE.replaceJsonContent(loaded.chapters(), loaded.missions());
        EchoMissionCore.LOGGER.info("Loaded {} MissionCore JSON chapters and {} JSON missions [{}].",
                loaded.chapters().size(), loaded.missions().size(), source);
        notifyTerminalMissionContentLoaded(loaded.missions().size());
    }

    private static void notifyTerminalMissionContentLoaded(int missionCount) {
        try {
            Class<?> integration = Class.forName(
                    "com.knoxhack.echomissioncore.integration.MissionCoreTerminalIntegration");
            integration.getMethod("notifyMissionContentLoaded", int.class).invoke(null, missionCount);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException | LinkageError ignored) {
        }
    }

    private static <T> List<T> load(ResourceManager manager, String directory, Parser<T> parser) {
        Map<Identifier, T> loaded = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(directory, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier id = contentId(resourceId, directory);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                if (loaded.putIfAbsent(id, parser.parse(id, root.getAsJsonObject())) != null) {
                    EchoMissionCore.LOGGER.warn("Duplicate MissionCore JSON id {} from {} ignored.", id, resourceId);
                }
            } catch (IOException | RuntimeException exception) {
                EchoMissionCore.LOGGER.warn("Could not parse MissionCore JSON {}: {}", resourceId, exception.getMessage());
            }
        }
        return List.copyOf(loaded.values());
    }

    private static MissionChapterDefinition parseChapter(Identifier id, JsonObject json) {
        Identifier chapterId = identifier(json, "id", id);
        return new MissionChapterDefinition(
                chapterId,
                string(json, "title", chapterId.getPath()),
                string(json, "summary", string(json, "description", "")),
                integer(json, "order", 0),
                color(json, "accentColor", 0x55FFDD));
    }

    private static MissionDefinition parseMission(Identifier id, JsonObject json) {
        Identifier missionId = identifier(json, "id", id);
        Identifier chapterId = identifier(json, "chapter", identifier(json, "chapterId", Identifier.fromNamespaceAndPath(id.getNamespace(), "missions")));
        MissionDefinition.Builder builder = MissionDefinition.builder(missionId, chapterId)
                .phase(
                        string(json, "phase", string(json, "phaseId", "phase_0")),
                        string(json, "phaseTitle", string(json, "phase", "Missions")),
                        integer(json, "phaseOrder", 0),
                        integer(json, "order", integer(json, "missionOrder", 0)))
                .text(
                        string(json, "title", missionId.getPath()),
                        string(json, "briefing", string(json, "description", "")),
                        string(json, "fieldGuide", ""))
                .category(string(json, "category", ""), string(json, "difficulty", ""))
                .icon(stack(json, "icon", 1))
                .kind(kind(string(json, "kind", "main")))
                .repeatPolicy(booleanValue(json, "repeatable", false) ? MissionRepeatPolicy.REPEATABLE : MissionRepeatPolicy.ONCE)
                .hidden(booleanValue(json, "hidden", false));
        applyMetadata(builder, json);
        for (Identifier prerequisite : identifiers(json, "prerequisites")) {
            builder.prerequisite(prerequisite);
        }
        for (ObjectiveDefinition objective : objectives(missionId, json)) {
            builder.objective(objective);
        }
        for (RewardDefinition reward : rewards(missionId, json)) {
            builder.reward(reward);
        }
        return builder.build();
    }

    private static List<ObjectiveDefinition> objectives(Identifier missionId, JsonObject json) {
        JsonArray array = array(json, "objectives");
        List<ObjectiveDefinition> objectives = new ArrayList<>();
        int index = 0;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            Identifier id = identifier(object, "id", Identifier.fromNamespaceAndPath(missionId.getNamespace(), missionId.getPath() + "/objective_" + index));
            Map<String, String> criteria = stringMap(object, "criteria");
            String target = string(object, "target", "");
            if (!target.isBlank()) {
                criteria.put("target", target);
            }
            objectives.add(new ObjectiveDefinition(
                    id,
                    objectiveType(string(object, "type", "custom")),
                    string(object, "label", id.getPath()),
                    string(object, "detail", ""),
                    stack(object, "icon", 1),
                    integer(object, "required", integer(object, "count", 1)),
                    booleanValue(object, "hidden", false),
                    criteria));
            index++;
        }
        if (objectives.isEmpty()) {
            objectives.add(ObjectiveDefinition.simple(
                    Identifier.fromNamespaceAndPath(missionId.getNamespace(), missionId.getPath() + "/complete"),
                    MissionObjectiveType.CUSTOM,
                    string(json, "objective", "Complete " + string(json, "title", missionId.getPath())),
                    "",
                    stack(json, "icon", 1),
                    1));
        }
        return objectives;
    }

    private static List<RewardDefinition> rewards(Identifier missionId, JsonObject json) {
        JsonArray array = array(json, "rewards");
        List<RewardDefinition> rewards = new ArrayList<>();
        int index = 0;
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            Identifier id = identifier(object, "id", Identifier.fromNamespaceAndPath(missionId.getNamespace(), missionId.getPath() + "/reward_" + index));
            Map<String, String> metadata = stringMap(object, "metadata");
            String itemId = string(object, "item", "");
            int count = Math.max(1, integer(object, "count", 1));
            String label = string(object, "label", "");
            if (!itemId.isBlank()) {
                Identifier item = validatedRewardItemId(itemId);
                metadata.putIfAbsent("item", item.toString());
                metadata.putIfAbsent("count", Integer.toString(count));
                if (label.isBlank()) {
                    label = count + "x " + item;
                }
            }
            int xp = integer(object, "xp", 0);
            int reputation = integer(object, "reputation", 0);
            if (xp > 0) {
                metadata.putIfAbsent("xp", Integer.toString(xp));
            }
            if (reputation != 0) {
                metadata.putIfAbsent("reputation", Integer.toString(reputation));
            }
            rewards.add(new RewardDefinition(
                    id,
                    claimMode(string(object, "claimMode", string(object, "mode", "claimable"))),
                    ItemStack.EMPTY,
                    label,
                    string(object, "detail", ""),
                    metadata));
            index++;
        }
        return rewards;
    }

    private static void applyMetadata(MissionDefinition.Builder builder, JsonObject json) {
        for (Map.Entry<String, String> entry : stringMap(json, "metadata").entrySet()) {
            builder.metadata(entry.getKey(), entry.getValue());
        }
        for (String key : List.of(
                "terminal_route_phase",
                "terminal_route_order",
                "terminal_route_role",
                "terminal_route_visible",
                "terminal_route_prerequisites",
                "terminal_route_gate",
                "terminal_route_anchor",
                "terminal_intel_archives",
                "terminal_intel_routes",
                "terminal_intel_discoveries",
                "terminal_intel_factions",
                "terminal_intel_pois",
                "terminal_source",
                "terminal_chapter",
                "nativeProvider",
                "nativeGameplayHook",
                "terminalAction",
                "requiredPath",
                "completionText")) {
            String value = string(json, key, "");
            if (!value.isBlank()) {
                builder.metadata(key, value);
            }
        }
        JsonObject hooks = json.getAsJsonObject("nativeHooks");
        if (hooks != null) {
            for (String key : List.of("provider", "completionSignal", "terminalAction")) {
                String value = string(hooks, key, "");
                if (!value.isBlank()) {
                    builder.metadata("nativeHooks." + key, value);
                }
            }
            List<String> runtimeEvents = strings(hooks, "runtimeEvents");
            if (!runtimeEvents.isEmpty()) {
                builder.metadata("nativeHooks.runtimeEvents", String.join(",", runtimeEvents));
            }
        }
        List<String> alternateSignals = strings(json, "alternateCompletionSignals");
        if (!alternateSignals.isEmpty()) {
            builder.metadata("alternateCompletionSignals", String.join(",", alternateSignals));
        }
        List<String> deprecatedAliases = strings(json, "deprecatedAliases");
        if (!deprecatedAliases.isEmpty()) {
            builder.metadata("deprecatedAliases", String.join(",", deprecatedAliases));
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

    private static JsonArray array(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    private static List<Identifier> identifiers(JsonObject json, String key) {
        List<Identifier> ids = new ArrayList<>();
        for (JsonElement element : array(json, key)) {
            Identifier id = Identifier.tryParse(element.getAsString());
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static List<String> strings(JsonObject json, String key) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array(json, key)) {
            if (element.isJsonPrimitive()) {
                String value = element.getAsString();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private static Map<String, String> stringMap(JsonObject json, String key) {
        Map<String, String> values = new LinkedHashMap<>();
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonObject()) {
            return values;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                values.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return values;
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        String value = string(json, key, "");
        Identifier parsed = value.isBlank() ? null : Identifier.tryParse(value);
        return parsed == null ? fallback : parsed;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsInt() : fallback;
    }

    private static boolean booleanValue(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsBoolean() : fallback;
    }

    private static int color(JsonObject json, String key, int fallback) {
        String value = string(json, key, "");
        if (value.isBlank()) {
            return integer(json, key, fallback);
        }
        try {
            return Integer.decode(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static ItemStack stack(JsonObject json, String key, int count) {
        String itemId = string(json, key, "");
        if (itemId.isBlank()) {
            return ItemStack.EMPTY;
        }
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        try {
            return new ItemStack(item, Math.max(1, count));
        } catch (RuntimeException | LinkageError exception) {
            EchoMissionCore.LOGGER.debug("MissionCore JSON icon {} deferred because item components are not bound yet.", id);
            return ItemStack.EMPTY;
        }
    }

    private static MissionKind kind(String value) {
        if (value == null || value.isBlank()) {
            return MissionKind.MAIN;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("side".equals(normalized) || "side_op".equals(normalized) || "side-op".equals(normalized)) {
            return MissionKind.SIDE_OP;
        }
        try {
            return MissionKind.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException("Unknown mission kind: " + value);
        }
    }

    private static MissionObjectiveType objectiveType(String value) {
        if (value == null || value.isBlank()) {
            return MissionObjectiveType.CUSTOM;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MissionObjectiveType type : MissionObjectiveType.values()) {
            if (type.id().equals(normalized) || type.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return type;
            }
        }
        throw new JsonParseException("Unknown objective type: " + value);
    }

    private static Identifier validatedRewardItemId(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            throw new JsonParseException("Invalid reward item id: " + itemId);
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) {
            EchoMissionCore.LOGGER.debug("MissionCore JSON reward {} deferred because the item registry is not ready yet.", id);
        }
        return id;
    }

    private static MissionRewardClaimMode claimMode(String value) {
        if (value == null || value.isBlank()) {
            return MissionRewardClaimMode.CLAIMABLE;
        }
        try {
            return MissionRewardClaimMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException("Unknown reward claim mode: " + value);
        }
    }

    public record LoadedContent(List<MissionChapterDefinition> chapters, List<MissionDefinition> missions) {
    }

    @FunctionalInterface
    private interface Parser<T> {
        T parse(Identifier id, JsonObject json);
    }
}
