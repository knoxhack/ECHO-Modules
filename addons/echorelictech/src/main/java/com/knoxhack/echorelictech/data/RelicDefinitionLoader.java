package com.knoxhack.echorelictech.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.api.relic.RelicDefinition;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RelicDefinitionLoader extends SimplePreparableReloadListener<Map<Identifier, RelicDefinition>> {
    private static final Map<Identifier, RelicDefinition> DEFINITIONS = new HashMap<>();
    private static final String DIR = "relics";

    @Override
    protected Map<Identifier, RelicDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, RelicDefinition> result = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonObject()) {
                    var parseResult = RelicDefinition.CODEC.parse(JsonOps.INSTANCE, root);
                    if (parseResult.isSuccess()) {
                        RelicDefinition def = parseResult.getOrThrow();
                        if (validate(entry.getKey(), def)) {
                            result.put(def.id(), def);
                        }
                    } else {
                        EchoRelicTech.LOGGER.error("Failed to parse relic definition {}: {}", entry.getKey(), parseResult.error().map(e -> e.message()).orElse("unknown"));
                    }
                }
            } catch (Exception e) {
                EchoRelicTech.LOGGER.error("Exception parsing relic definition {}", entry.getKey(), e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<Identifier, RelicDefinition> map, ResourceManager manager, ProfilerFiller profiler) {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(map);
        EchoRelicTech.LOGGER.info("Loaded {} relic definitions.", DEFINITIONS.size());
    }

    public static RelicDefinition get(Identifier id) {
        return DEFINITIONS.get(id);
    }

    public static Map<Identifier, RelicDefinition> all() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }

    private static boolean validate(Identifier source, RelicDefinition definition) {
        boolean valid = true;
        if (definition.failureTable() == null) {
            EchoRelicTech.LOGGER.error("Relic definition {} has no failureTable.", source);
            valid = false;
        }
        if (definition.repair().isPresent()) {
            valid &= validateMaterials(source, "repair", definition.repair().get().materials());
        }
        for (var entry : definition.workbenchActions().entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                EchoRelicTech.LOGGER.error("Relic definition {} has a blank workbench action key.", source);
                valid = false;
            }
            valid &= validateMaterials(source, "workbenchActions." + entry.getKey(), entry.getValue().materials());
        }
        return valid;
    }

    private static boolean validateMaterials(Identifier source, String path, java.util.List<RelicDefinition.RepairMaterial> materials) {
        boolean valid = true;
        for (RelicDefinition.RepairMaterial material : materials) {
            Identifier itemId = Identifier.tryParse(material.item());
            if (itemId == null) {
                EchoRelicTech.LOGGER.error("Relic definition {} has invalid item id '{}' in {}.", source, material.item(), path);
                valid = false;
                continue;
            }
            if (BuiltInRegistries.ITEM.get(itemId).isEmpty()) {
                EchoRelicTech.LOGGER.error("Relic definition {} references missing item {} in {}.", source, itemId, path);
                valid = false;
            }
            if (material.count() <= 0) {
                EchoRelicTech.LOGGER.error("Relic definition {} references non-positive material count for {} in {}.", source, itemId, path);
                valid = false;
            }
        }
        return valid;
    }
}
