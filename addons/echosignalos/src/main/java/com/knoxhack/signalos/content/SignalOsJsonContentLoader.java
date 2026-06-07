package com.knoxhack.signalos.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.TerminalArchiveRecord;
import com.knoxhack.signalos.api.TerminalChapter;
import com.knoxhack.signalos.api.TerminalIds;
import com.knoxhack.signalos.api.TerminalMission;
import com.knoxhack.signalos.api.SignalOsApp;
import com.knoxhack.signalos.api.SignalOsDataRecord;
import com.knoxhack.signalos.api.SignalOsDriveData;
import com.knoxhack.signalos.api.SignalOsNetLink;
import com.knoxhack.signalos.api.SignalOsNetPage;
import com.knoxhack.signalos.api.SignalOsNetSite;
import com.knoxhack.signalos.service.SignalOsComputerNetworkService;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class SignalOsJsonContentLoader
        extends SimplePreparableReloadListener<SignalOsContentRegistry.LoadedContent> {
    private static final String CHAPTER_DIR = "signalos/chapters";
    private static final String MISSION_DIR = "signalos/missions";
    private static final String ARCHIVE_DIR = "signalos/archives";
    private static final String APP_DIR = "signalos/apps";
    private static final String DATA_RECORD_DIR = "signalos/data_records";
    private static final String DRIVE_TEMPLATE_DIR = "signalos/drive_templates";
    private static final String NET_SITE_DIR = "signalos/net_sites";

    @Override
    protected SignalOsContentRegistry.LoadedContent prepare(ResourceManager manager, ProfilerFiller profiler) {
        LoadedEntries<TerminalChapter> chapters = load(manager, CHAPTER_DIR, SignalOsJsonContentLoader::parseChapter);
        LoadedEntries<TerminalMission> missions = load(manager, MISSION_DIR, SignalOsJsonContentLoader::parseMission);
        LoadedEntries<TerminalArchiveRecord> archives = load(manager, ARCHIVE_DIR, SignalOsJsonContentLoader::parseArchive);
        LoadedEntries<SignalOsApp> apps = load(manager, APP_DIR, SignalOsJsonContentLoader::parseApp);
        LoadedEntries<SignalOsDataRecord> dataRecords =
                load(manager, DATA_RECORD_DIR, SignalOsJsonContentLoader::parseDataRecord);
        LoadedEntries<SignalOsDriveData> driveTemplates =
                load(manager, DRIVE_TEMPLATE_DIR, SignalOsJsonContentLoader::parseDriveTemplate);
        LoadedEntries<SignalOsNetSite> netSites =
                load(manager, NET_SITE_DIR, SignalOsJsonContentLoader::parseNetSite);
        SignalOsContentRegistry.LoadReport report = chapters.report()
                .plus(missions.report())
                .plus(archives.report())
                .plus(apps.report())
                .plus(dataRecords.report())
                .plus(driveTemplates.report())
                .plus(netSites.report());
        return validateReferences(chapters.values(), missions.values(), archives.values(), apps.values(),
                dataRecords.values(), driveTemplates.values(), netSites.values(), report);
    }

    @Override
    protected void apply(SignalOsContentRegistry.LoadedContent loaded, ResourceManager manager, ProfilerFiller profiler) {
        SignalOsContentRegistry.replaceJsonContent(loaded);
        SignalOsComputerNetworkService.invalidateCache();
    }

    public static TerminalChapter parseChapterForTests(Identifier id, JsonObject json) {
        return parseChapter(id, json);
    }

    public static TerminalMission parseMissionForTests(Identifier id, JsonObject json) {
        return parseMission(id, json);
    }

    public static TerminalArchiveRecord parseArchiveForTests(Identifier id, JsonObject json) {
        return parseArchive(id, json);
    }

    public static SignalOsApp parseAppForTests(Identifier id, JsonObject json) {
        return parseApp(id, json);
    }

    public static SignalOsDataRecord parseDataRecordForTests(Identifier id, JsonObject json) {
        return parseDataRecord(id, json);
    }

    public static SignalOsNetSite parseNetSiteForTests(Identifier id, JsonObject json) {
        return parseNetSite(id, json);
    }

    public static SignalOsContentRegistry.LoadedContent validateReferencesForTests(
            SignalOsContentRegistry.LoadedContent loaded) {
        return validateReferences(loaded.chapters(), loaded.missions(), loaded.archives(), loaded.report());
    }

    private static <T> LoadedEntries<T> load(ResourceManager manager, String directory, Parser<T> parser) {
        Map<Identifier, T> target = new LinkedHashMap<>();
        int discovered = 0;
        int parsedCount = 0;
        int duplicates = 0;
        int failed = 0;
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(directory, SignalOsJsonContentLoader::isJson).entrySet()) {
            discovered++;
            Identifier resourceId = entry.getKey();
            Identifier id = contentId(resourceId, directory);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                T parsedValue = parser.parse(id, root.getAsJsonObject());
                if (target.putIfAbsent(id, parsedValue) != null) {
                    duplicates++;
                    SignalOS.LOGGER.warn("Duplicate SignalOS data id {} from {} ignored.", id, resourceId);
                } else {
                    parsedCount++;
                }
            } catch (IOException exception) {
                failed++;
                SignalOS.LOGGER.warn("Could not read SignalOS data file {}: {}", resourceId, exception.getMessage());
            } catch (RuntimeException exception) {
                failed++;
                SignalOS.LOGGER.warn("Could not parse SignalOS data file {}: {}", resourceId, exception.getMessage());
            }
        }
        return new LoadedEntries<>(target, new SignalOsContentRegistry.LoadReport(discovered, parsedCount, duplicates, failed, 0));
    }

    private static boolean isJson(Identifier id) {
        return id.getPath().endsWith(".json");
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

    private static TerminalChapter parseChapter(Identifier id, JsonObject json) {
        TerminalChapter.Builder builder = TerminalChapter.builder(id)
                .title(string(json, "title", id.getPath()))
                .section(string(json, "section", "progress"))
                .order(integer(json, "order", 0))
                .accentColor(integer(json, "accentColor", 0x66E8FF))
                .visible(bool(json, "visible", true));
        String icon = string(json, "icon", "");
        if (!icon.isBlank()) {
            builder.icon(icon);
        }
        JsonArray pages = array(json, "pages");
        if (pages != null) {
            for (int i = 0; i < pages.size(); i++) {
                builder.page(stringElement(pages.get(i), "pages[" + i + "]"));
            }
        }
        return builder.build();
    }

    private static TerminalMission parseMission(Identifier id, JsonObject json) {
        TerminalMission.Builder builder = TerminalMission.builder(id)
                .chapter(requiredString(json, "chapter"))
                .title(string(json, "title", id.getPath()))
                .description(string(json, "description", ""))
                .order(integer(json, "order", 0))
                .rewardClaim(bool(json, "rewardClaim", true));
        String icon = string(json, "icon", "");
        if (!icon.isBlank()) {
            builder.icon(icon);
        }
        String advancement = string(json, "completionAdvancement", "");
        if (!advancement.isBlank()) {
            builder.completionAdvancement(advancement);
        }
        JsonArray objectives = array(json, "objectives");
        if (objectives != null) {
            for (int i = 0; i < objectives.size(); i++) {
                builder.objective(nonBlankStringElement(objectives.get(i), "objectives[" + i + "]"));
            }
        }
        JsonArray rewards = array(json, json.has("displayRewards") ? "displayRewards" : "rewards");
        if (rewards != null) {
            String rewardKey = json.has("displayRewards") ? "displayRewards" : "rewards";
            for (int i = 0; i < rewards.size(); i++) {
                JsonElement rewardElement = rewards.get(i);
                if (!rewardElement.isJsonObject()) {
                    throw new JsonParseException("Field '" + rewardKey + "[" + i + "]' must be an object.");
                }
                JsonObject reward = rewardElement.getAsJsonObject();
                int count = integer(reward, rewardKey + "[" + i + "].count", "count", 1);
                if (count <= 0) {
                    throw new JsonParseException("Field '" + rewardKey + "[" + i + "].count' must be at least 1.");
                }
                builder.reward(requiredString(reward, rewardKey + "[" + i + "].item", "item"),
                        count,
                        string(reward, rewardKey + "[" + i + "].label", "label", ""));
            }
        }
        return builder.build();
    }

    private static TerminalArchiveRecord parseArchive(Identifier id, JsonObject json) {
        TerminalArchiveRecord.Builder builder = TerminalArchiveRecord.builder(id)
                .chapter(requiredString(json, "chapter"))
                .title(string(json, "title", id.getPath()))
                .group(string(json, "group", ""))
                .status(string(json, "status", "OPEN"))
                .order(integer(json, "order", 0))
                .locked(bool(json, "locked", false));
        JsonArray lines = array(json, "lines");
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                builder.line(nonBlankStringElement(lines.get(i), "lines[" + i + "]"));
            }
        }
        return builder.build();
    }

    private static SignalOsApp parseApp(Identifier id, JsonObject json) {
        SignalOsApp.Builder builder = SignalOsApp.builder(id)
                .title(string(json, "title", id.getPath()))
                .type(string(json, "type", "custom"))
                .summary(string(json, "summary", ""))
                .order(integer(json, "order", 0))
                .accentColor(integer(json, "accentColor", 0x66E8FF))
                .permission(string(json, "permission", "user"))
                .view(string(json, "view", ""))
                .recordTypes(stringList(json, "recordTypes"))
                .recordSources(stringList(json, "recordSources"))
                .includeArchived(bool(json, "includeArchived", false))
                .emptyText(string(json, "emptyText", "NO RECORDS AVAILABLE"));
        String icon = string(json, "icon", "");
        if (!icon.isBlank()) {
            builder.icon(icon);
        }
        return builder.build();
    }

    private static SignalOsDataRecord parseDataRecord(Identifier id, JsonObject json) {
        String body = string(json, "body", "");
        JsonArray lines = array(json, "lines");
        if (lines != null) {
            StringBuilder joined = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (!joined.isEmpty()) {
                    joined.append('\n');
                }
                joined.append(nonBlankStringElement(lines.get(i), "lines[" + i + "]"));
            }
            body = joined.toString();
        }
        return new SignalOsDataRecord(
                id,
                string(json, "title", id.getPath()),
                string(json, "type", "record"),
                string(json, "source", id.getNamespace()),
                body,
                integer(json, "order", 0),
                bool(json, "archived", false),
                stringMap(json, "metadata"));
    }

    private static SignalOsDriveData parseDriveTemplate(Identifier id, JsonObject json) {
        int schemaVersion = integer(json, "schemaVersion", SignalOsDriveData.LEGACY_SCHEMA_VERSION);
        if (schemaVersion != SignalOsDriveData.CURRENT_SCHEMA_VERSION) {
            throw new JsonParseException("SignalOS drive template '" + id + "' must declare schemaVersion "
                    + SignalOsDriveData.CURRENT_SCHEMA_VERSION + ".");
        }
        String label = string(json, "label", id.getPath());
        Map<Identifier, SignalOsDataRecord> records = new LinkedHashMap<>();
        JsonArray recordArray = array(json, "records");
        if (recordArray != null) {
            for (int i = 0; i < recordArray.size(); i++) {
                JsonElement element = recordArray.get(i);
                if (!element.isJsonObject()) {
                    throw new JsonParseException("Field 'records[" + i + "]' must be an object.");
                }
                JsonObject recordJson = element.getAsJsonObject();
                String recordId = string(recordJson, "id", id + "/record_" + i);
                SignalOsDataRecord record = parseDataRecord(TerminalIds.parse(recordId, "drive template record"), recordJson);
                records.putIfAbsent(record.id(), record);
            }
        }
        return new SignalOsDriveData(schemaVersion, label, records.values().stream().toList(),
                stringMap(json, "settings"), stringMap(json, "session"));
    }

    private static SignalOsNetSite parseNetSite(Identifier id, JsonObject json) {
        String address = string(json, "address", id.getPath().replace('/', '.'));
        List<String> tags = stringList(json, "tags");
        java.util.ArrayList<SignalOsNetPage> pages = new java.util.ArrayList<>();
        JsonArray pageArray = array(json, "pages");
        if (pageArray != null) {
            for (int i = 0; i < pageArray.size(); i++) {
                JsonElement element = pageArray.get(i);
                if (!element.isJsonObject()) {
                    throw new JsonParseException("Field 'pages[" + i + "]' must be an object.");
                }
                pages.add(parseNetPage(element.getAsJsonObject(), "pages[" + i + "]", i * 10));
            }
        }
        return new SignalOsNetSite(
                id,
                address,
                string(json, "title", address),
                string(json, "summary", ""),
                integer(json, "requiredTier", 0),
                tags,
                pages,
                integer(json, "order", 0));
    }

    private static SignalOsNetPage parseNetPage(JsonObject json, String fieldLabel, int fallbackOrder) {
        String body = string(json, fieldLabel + ".body", "body", "");
        JsonArray lines = array(json, "lines");
        if (lines != null) {
            StringBuilder joined = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (!joined.isEmpty()) {
                    joined.append('\n');
                }
                joined.append(nonBlankStringElement(lines.get(i), fieldLabel + ".lines[" + i + "]"));
            }
            body = joined.toString();
        }
        java.util.ArrayList<SignalOsNetLink> links = new java.util.ArrayList<>();
        JsonArray linkArray = array(json, "links");
        if (linkArray != null) {
            for (int i = 0; i < linkArray.size(); i++) {
                JsonElement element = linkArray.get(i);
                if (!element.isJsonObject()) {
                    throw new JsonParseException("Field '" + fieldLabel + ".links[" + i + "]' must be an object.");
                }
                JsonObject link = element.getAsJsonObject();
                links.add(new SignalOsNetLink(
                        string(link, fieldLabel + ".links[" + i + "].label", "label", "Link"),
                        requiredPlainString(link, fieldLabel + ".links[" + i + "].address", "address")));
            }
        }
        return new SignalOsNetPage(
                string(json, fieldLabel + ".path", "path", "/"),
                string(json, fieldLabel + ".title", "title", ""),
                body,
                links,
                integer(json, fieldLabel + ".order", "order", fallbackOrder));
    }

    private static String requiredString(JsonObject json, String key) {
        return requiredString(json, key, key);
    }

    private static String requiredString(JsonObject json, String fieldLabel, String key) {
        String value = string(json, fieldLabel, key, "");
        if (value.isBlank()) {
            throw new JsonParseException("Missing required field '" + fieldLabel + "'.");
        }
        TerminalIds.parse(value, fieldLabel);
        return value;
    }

    private static String string(JsonObject json, String key, String fallback) {
        return string(json, key, key, fallback);
    }

    private static String string(JsonObject json, String fieldLabel, String key, String fallback) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        return stringElement(element, fieldLabel);
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return integer(json, key, key, fallback);
    }

    private static int integer(JsonObject json, String fieldLabel, String key, int fallback) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("Field '" + fieldLabel + "' must be an integer.");
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException exception) {
            throw new JsonParseException("Field '" + fieldLabel + "' must be an integer.", exception);
        }
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException("Field '" + key + "' must be a boolean.");
        }
        return element.getAsBoolean();
    }

    private static JsonArray array(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonArray()) {
            throw new JsonParseException("Field '" + key + "' must be an array.");
        }
        return element.getAsJsonArray();
    }

    private static java.util.List<String> stringList(JsonObject json, String key) {
        JsonArray values = array(json, key);
        if (values == null) {
            return java.util.List.of();
        }
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            result.add(nonBlankStringElement(values.get(i), key + "[" + i + "]"));
        }
        return result;
    }

    private static Map<String, String> stringMap(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return Map.of();
        }
        if (!element.isJsonObject()) {
            throw new JsonParseException("Field '" + key + "' must be an object.");
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            result.put(entry.getKey(), stringElement(entry.getValue(), key + "." + entry.getKey()));
        }
        return result;
    }

    private static String stringElement(JsonElement element, String fieldLabel) {
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            throw new JsonParseException("Field '" + fieldLabel + "' must be a string.");
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (!primitive.isString()) {
            throw new JsonParseException("Field '" + fieldLabel + "' must be a string.");
        }
        return primitive.getAsString();
    }

    private static String nonBlankStringElement(JsonElement element, String fieldLabel) {
        String value = stringElement(element, fieldLabel).strip();
        if (value.isBlank()) {
            throw new JsonParseException("Field '" + fieldLabel + "' must not be blank.");
        }
        return value;
    }

    private static String requiredPlainString(JsonObject json, String fieldLabel, String key) {
        String value = string(json, fieldLabel, key, "");
        if (value.isBlank()) {
            throw new JsonParseException("Missing required field '" + fieldLabel + "'.");
        }
        return value;
    }

    private static SignalOsContentRegistry.LoadedContent validateReferences(
            Map<Identifier, TerminalChapter> chapters,
            Map<Identifier, TerminalMission> missions,
            Map<Identifier, TerminalArchiveRecord> archives,
            Map<Identifier, SignalOsApp> apps,
            Map<Identifier, SignalOsDataRecord> dataRecords,
            Map<Identifier, SignalOsDriveData> driveTemplates,
            Map<Identifier, SignalOsNetSite> netSites,
            SignalOsContentRegistry.LoadReport report) {
        Map<Identifier, TerminalMission> validMissions = new LinkedHashMap<>();
        Map<Identifier, TerminalArchiveRecord> validArchives = new LinkedHashMap<>();
        Map<Identifier, SignalOsNetSite> validNetSites = new LinkedHashMap<>();
        int rejected = 0;
        Set<Identifier> jsonChapterIds = chapters.keySet();
        Set<String> seenNetAddresses = new java.util.LinkedHashSet<>();

        for (Map.Entry<Identifier, TerminalMission> entry : missions.entrySet()) {
            Identifier chapterId = entry.getValue().chapterId();
            if (hasResolvableChapter(chapterId, jsonChapterIds)) {
                if (hasResolvableRewardTargets(entry.getKey(), entry.getValue())) {
                    validMissions.put(entry.getKey(), entry.getValue());
                } else {
                    rejected++;
                }
            } else {
                rejected++;
                SignalOS.LOGGER.warn("SignalOS mission {} references missing chapter {}; mission skipped.",
                        entry.getKey(), chapterId);
            }
        }
        for (Map.Entry<Identifier, TerminalArchiveRecord> entry : archives.entrySet()) {
            Identifier chapterId = entry.getValue().chapterId();
            if (hasResolvableChapter(chapterId, jsonChapterIds)) {
                validArchives.put(entry.getKey(), entry.getValue());
            } else {
                rejected++;
                SignalOS.LOGGER.warn("SignalOS archive {} references missing chapter {}; archive skipped.",
                        entry.getKey(), chapterId);
            }
        }
        for (Map.Entry<Identifier, SignalOsNetSite> entry : netSites.entrySet()) {
            SignalOsNetSite site = entry.getValue();
            boolean duplicateAddress = !seenNetAddresses.add(site.address());
            boolean duplicatePageAddress = false;
            Set<String> pageAddresses = new java.util.LinkedHashSet<>();
            for (SignalOsNetPage page : site.pages()) {
                if (!pageAddresses.add(site.pageAddress(page))) {
                    duplicatePageAddress = true;
                    break;
                }
            }
            if (duplicateAddress || duplicatePageAddress) {
                rejected++;
                SignalOS.LOGGER.warn("SignalOS net site {} uses duplicate SignalNet address data; site skipped.",
                        entry.getKey());
            } else {
                validNetSites.put(entry.getKey(), site);
            }
        }
        return new SignalOsContentRegistry.LoadedContent(chapters, validMissions, validArchives, apps,
                dataRecords, driveTemplates, validNetSites,
                report.withRejectedReferences(rejected));
    }

    private static SignalOsContentRegistry.LoadedContent validateReferences(
            Map<Identifier, TerminalChapter> chapters,
            Map<Identifier, TerminalMission> missions,
            Map<Identifier, TerminalArchiveRecord> archives,
            SignalOsContentRegistry.LoadReport report) {
        return validateReferences(chapters, missions, archives, Map.of(), Map.of(), Map.of(), Map.of(), report);
    }

    private static boolean hasResolvableChapter(Identifier chapterId, Set<Identifier> jsonChapterIds) {
        return chapterId != null && (jsonChapterIds.contains(chapterId) || SignalOsContentRegistry.hasNonJsonChapter(chapterId));
    }

    private static boolean hasResolvableRewardTargets(Identifier missionId, TerminalMission mission) {
        if (mission == null) {
            return false;
        }
        for (TerminalMission.Reward reward : mission.rewards()) {
            if (!reward.hasRegisteredItem()) {
                SignalOS.LOGGER.warn("SignalOS mission {} references missing reward item {}; mission skipped.",
                        missionId, reward.itemId());
                return false;
            }
        }
        return true;
    }

    @FunctionalInterface
    private interface Parser<T> {
        T parse(Identifier id, JsonObject json);
    }

    private record LoadedEntries<T>(Map<Identifier, T> values, SignalOsContentRegistry.LoadReport report) {
    }
}
