package com.knoxhack.echonexusprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.world.NexusTerrainGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class ModWorldgen {
   private static final Object CHUNK_GENERATORS = EchoBackendRegistryBridge.create(Registries.CHUNK_GENERATOR, EchoNexusProtocol.MODID);
   public static final EchoBackendRegistryEntry<MapCodec<NexusTerrainGenerator>> NEXUS_TERRAIN = EchoBackendRegistryBridge.register(CHUNK_GENERATORS,
      "nexus_terrain", () -> NexusTerrainGenerator.CODEC
   );

   private ModWorldgen() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(CHUNK_GENERATORS, eventBus);
   }
}
