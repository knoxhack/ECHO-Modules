package com.knoxhack.echodatacore.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.knoxhack.echocore.api.DataKeyMetadata;
import com.knoxhack.echocore.api.DataScope;
import com.knoxhack.echocore.api.DataValueKind;
import com.knoxhack.echodatacore.DataCoreDataService;
import com.knoxhack.echodatacore.EchoDataCore;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class DataCoreJsonReloadListener extends SimplePreparableReloadListener<Map<Identifier, DataKeyMetadata>> {
    private static final String DATA_KEY_DIR = "echodatacore/data_keys";
    private static volatile MinecraftServer currentServer;

    public static void bindServer(MinecraftServer server) {
        currentServer = server;
    }

    @Override
    protected Map<Identifier, DataKeyMetadata> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, DataKeyMetadata> keys = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(DATA_KEY_DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            Identifier fallbackId = contentId(resourceId);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) {
                    throw new JsonParseException("Root must be a JSON object.");
                }
                DataKeyMetadata meta = parseKey(fallbackId, root.getAsJsonObject(), "datapack:" + resourceId);
                if (keys.put(meta.id(), meta) != null) {
                    EchoDataCore.LOGGER.warn("Duplicate DataCore key metadata {} from {} replaced earlier data entry.",
                            meta.id(), resourceId);
                }
            } catch (IOException | RuntimeException exception) {
                EchoDataCore.LOGGER.warn("Could not parse DataCore key metadata file {}.", resourceId, exception);
            }
        }
        return keys;
    }

    @Override
    protected void apply(Map<Identifier, DataKeyMetadata> content, ResourceManager manager, ProfilerFiller profiler) {
        DataCoreDataService.INSTANCE.replaceDatapackMetadata(content);
        MinecraftServer server = currentServer;
        if (server != null) {
            DataCoreDataService.INSTANCE.broadcastMetadataSync(server.getPlayerList().getPlayers());
        }
        EchoDataCore.LOGGER.info("DataCore loaded {} datapack data key metadata entries.", content.size());
    }

    public static DataKeyMetadata parseKeyForTests(Identifier fallbackId, JsonObject json) {
        return parseKey(fallbackId, json, "test");
    }

    private static DataKeyMetadata parseKey(Identifier fallbackId, JsonObject json, String source) {
        Identifier id = identifier(json, "id", fallbackId);
        DataScope scope = enumValue(json, "scope", DataScope.PLAYER, DataScope.class);
        DataValueKind kind = enumValue(json, "kind", DataValueKind.FLAG, DataValueKind.class);
        return new DataKeyMetadata(
                id,
                scope,
                kind,
                bool(json, "synced", true),
                string(json, "title", ""),
                string(json, "description", ""),
                string(json, "owner", id.getNamespace()),
                string(json, "legacyRoot", ""),
                string(json, "legacyField", ""),
                defaultValue(json),
                source);
    }

    private static Identifier contentId(Identifier resourceId) {
        String path = resourceId.getPath();
        String prefix = DATA_KEY_DIR + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return Identifier.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }

    private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
        String value = string(json, key, "");
        return value.isBlank() ? fallback : Identifier.parse(value);
    }

    private static <E extends Enum<E>> E enumValue(JsonObject json, String key, E fallback, Class<E> type) {
        String value = string(json, key, fallback.name());
        String normalized = value.toUpperCase(Locale.ROOT).replace('-', '_');
        return Enum.valueOf(type, normalized);
    }

    private static String defaultValue(JsonObject json) {
        JsonElement element = json == null ? null : json.get("default");
        if (element == null || element.isJsonNull()) {
            return "";
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return element.toString();
    }

    private static String string(JsonObject json, String key, String fallback) {
        if (json == null) {
            return fallback;
        }
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        if (json == null) {
            return fallback;
        }
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
    }
}
