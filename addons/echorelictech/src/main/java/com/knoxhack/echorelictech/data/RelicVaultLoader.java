package com.knoxhack.echorelictech.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echorelictech.EchoRelicTech;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.*;

public class RelicVaultLoader extends SimplePreparableReloadListener<Map<Identifier, RelicVaultInfo>> {
    private static final Map<Identifier, RelicVaultInfo> VAULTS = new HashMap<>();
    private static final String DIR = "relic_vaults";

    @Override
    protected Map<Identifier, RelicVaultInfo> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, RelicVaultInfo> result = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    String idStr = obj.has("id") ? obj.get("id").getAsString() : entry.getKey().toString();
                    String displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : idStr;
                    String tier = obj.has("tier") ? obj.get("tier").getAsString() : "FIELD";
                    String lootTable = obj.has("lootTable") ? obj.get("lootTable").getAsString() : "";
                    String materialLootTable = obj.has("materialLootTable") ? obj.get("materialLootTable").getAsString() : "";
                    String securityLevel = obj.has("securityLevel") ? obj.get("securityLevel").getAsString() : "LOW";
                    int minY = intOr(obj, "minY", -64);
                    int maxY = intOr(obj, "maxY", 64);
                    int spawnWeight = intOr(obj, "spawnWeight", 1);
                    List<String> requiredBiomeTags = stringList(obj, "requiredBiomeTags");
                    List<String> excludedBiomes = stringList(obj, "excludedBiomes");
                    String markerText = obj.has("markerText") ? obj.get("markerText").getAsString() : displayName;
                    String progressionPhase = obj.has("progressionPhase") ? obj.get("progressionPhase").getAsString() : "relic_ops";
                    List<String> notes = stringList(obj, "notes");
                    Identifier parsedId = Identifier.parse(idStr);
                    if (lootTable.isBlank() || materialLootTable.isBlank() || spawnWeight <= 0 || minY > maxY) {
                        EchoRelicTech.LOGGER.error("Invalid relic vault {} from {}: loot tables, spawnWeight, or Y bounds are invalid.", idStr, entry.getKey());
                        continue;
                    }
                    result.put(parsedId, new RelicVaultInfo(idStr, displayName, tier, lootTable, materialLootTable,
                            securityLevel, minY, maxY, spawnWeight, requiredBiomeTags, excludedBiomes,
                            markerText, progressionPhase, notes));
                }
            } catch (Exception e) {
                EchoRelicTech.LOGGER.error("Failed to parse relic vault {}", entry.getKey(), e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<Identifier, RelicVaultInfo> map, ResourceManager manager, ProfilerFiller profiler) {
        VAULTS.clear();
        VAULTS.putAll(map);
        EchoRelicTech.LOGGER.info("Loaded {} relic vault definitions.", VAULTS.size());
    }

    public static RelicVaultInfo get(Identifier id) {
        return VAULTS.get(id);
    }

    public static List<RelicVaultInfo> all() {
        return List.copyOf(VAULTS.values());
    }

    private static int intOr(JsonObject obj, String key, int fallback) {
        return obj.has(key) ? obj.get(key).getAsInt() : fallback;
    }

    private static List<String> stringList(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : obj.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) {
                values.add(element.getAsString());
            }
        }
        return values;
    }
}
