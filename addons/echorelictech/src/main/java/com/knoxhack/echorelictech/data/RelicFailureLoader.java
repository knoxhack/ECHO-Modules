package com.knoxhack.echorelictech.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.api.failure.FailureTable;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RelicFailureLoader extends SimplePreparableReloadListener<Map<Identifier, FailureTable>> {
    private static final Map<Identifier, FailureTable> TABLES = new HashMap<>();
    private static final String DIR = "relic_failures";

    @Override
    protected Map<Identifier, FailureTable> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, FailureTable> result = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : manager.listResources(DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root.isJsonObject()) {
                    var parseResult = FailureTable.CODEC.parse(JsonOps.INSTANCE, root);
                    if (parseResult.isSuccess()) {
                        FailureTable table = parseResult.getOrThrow();
                        result.put(table.id(), table);
                    } else {
                        EchoRelicTech.LOGGER.error("Failed to parse failure table {}: {}", entry.getKey(), parseResult.error().map(e -> e.message()).orElse("unknown"));
                    }
                }
            } catch (Exception e) {
                EchoRelicTech.LOGGER.error("Exception parsing failure table {}", entry.getKey(), e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<Identifier, FailureTable> map, ResourceManager manager, ProfilerFiller profiler) {
        TABLES.clear();
        TABLES.putAll(map);
        EchoRelicTech.LOGGER.info("Loaded {} relic failure tables.", TABLES.size());
    }

    public static FailureTable get(Identifier id) {
        return TABLES.get(id);
    }

    public static Map<Identifier, FailureTable> all() {
        return Collections.unmodifiableMap(TABLES);
    }
}
