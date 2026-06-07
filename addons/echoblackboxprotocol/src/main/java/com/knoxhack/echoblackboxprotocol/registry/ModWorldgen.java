package com.knoxhack.echoblackboxprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblackboxprotocol.world.BlackboxDungeonGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class ModWorldgen {
   private static final Object CHUNK_GENERATORS = EchoBackendRegistryBridge.create(
      Registries.CHUNK_GENERATOR, "echoblackboxprotocol"
   );
   public static final EchoBackendRegistryEntry<MapCodec<BlackboxDungeonGenerator>> BLACKBOX_DUNGEON = EchoBackendRegistryBridge.register(CHUNK_GENERATORS, 
      "blackbox_dungeon", () -> BlackboxDungeonGenerator.CODEC
   );

   private ModWorldgen() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(CHUNK_GENERATORS, eventBus);
   }
}
