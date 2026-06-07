package com.knoxhack.echoashfallprotocol.worldgen;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.HashSet;
import java.util.Set;

/**
 * Registers mod structure sets to the overworld dimension.
 * This ensures POI structures spawn in their respective biomes.
 */
public class StructureSetRegistration {

    // All POI structure sets that need to be registered
    private static final Set<ResourceKey<StructureSet>> POI_STRUCTURE_SETS = new HashSet<>();

    static {
        // Biome-specific POI structure sets
        POI_STRUCTURE_SETS.add(ResourceKey.create(Registries.STRUCTURE_SET,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "poi_crash_zone_wasteland")));
        POI_STRUCTURE_SETS.add(ResourceKey.create(Registries.STRUCTURE_SET,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "poi_ruined_cityscape")));
        POI_STRUCTURE_SETS.add(ResourceKey.create(Registries.STRUCTURE_SET,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "poi_radiation_zone")));
        POI_STRUCTURE_SETS.add(ResourceKey.create(Registries.STRUCTURE_SET,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "poi_toxic_swamp")));
        POI_STRUCTURE_SETS.add(ResourceKey.create(Registries.STRUCTURE_SET,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "poi_industrial_ruins")));
        POI_STRUCTURE_SETS.add(ResourceKey.create(Registries.STRUCTURE_SET,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "poi_cryogenic_ruins")));
        POI_STRUCTURE_SETS.add(ResourceKey.create(Registries.STRUCTURE_SET,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "poi_ruined_plains")));
        POI_STRUCTURE_SETS.add(ResourceKey.create(Registries.STRUCTURE_SET,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "poi_global")));
        POI_STRUCTURE_SETS.add(ResourceKey.create(Registries.STRUCTURE_SET,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "ashfall_biome_landmarks")));
    }

    public static void onServerAboutToStart(Object event) {
        EchoAshfallProtocol.LOGGER.info("[StructureSetRegistration] Registering POI structure sets to overworld...");

        MinecraftServer server = serverFrom(event);
        if (server == null) {
            EchoAshfallProtocol.LOGGER.debug("[StructureSetRegistration] Ignoring server-start event without a MinecraftServer payload: {}",
                    event == null ? "null" : event.getClass().getName());
            return;
        }
        AshfallAdapterCoreMachineRuntimeHost.bindServer(server);
        var registryAccess = server.registryAccess();
        var structureSetRegistry = registryAccess.lookupOrThrow(Registries.STRUCTURE_SET);

        // Log all registered structure sets for debugging
        int count = 0;
        for (var key : POI_STRUCTURE_SETS) {
            var optional = structureSetRegistry.get(key);
            if (optional.isPresent()) {
                count++;
                EchoAshfallProtocol.LOGGER.info("[StructureSetRegistration] Found structure set: {}", key);
            } else {
                EchoAshfallProtocol.LOGGER.warn("[StructureSetRegistration] Structure set not found: {}", key);
            }
        }

        EchoAshfallProtocol.LOGGER.info("[StructureSetRegistration] Registered {} POI structure sets", count);
    }

    private static MinecraftServer serverFrom(Object event) {
        if (event instanceof MinecraftServer server) {
            return server;
        }
        if (event == null) {
            return null;
        }
        try {
            Object value = event.getClass().getMethod("getServer").invoke(event);
            return value instanceof MinecraftServer server ? server : null;
        } catch (ReflectiveOperationException e) {
            EchoAshfallProtocol.LOGGER.debug("[StructureSetRegistration] Server-start event {} does not expose getServer().",
                    event.getClass().getName());
            return null;
        }
    }
}
