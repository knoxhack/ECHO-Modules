package com.knoxhack.echoorbitalremnants.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.world.RouteTerrainGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class ModWorldgen {
    private static final Object CHUNK_GENERATORS =
            EchoBackendRegistryBridge.create(Registries.CHUNK_GENERATOR, EchoOrbitalRemnants.MODID);

    public static final EchoBackendRegistryEntry<MapCodec<RouteTerrainGenerator>> ROUTE_TERRAIN =
            EchoBackendRegistryBridge.register(CHUNK_GENERATORS, "route_terrain", () -> RouteTerrainGenerator.CODEC);

    private ModWorldgen() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(CHUNK_GENERATORS, eventBus);
    }
}
