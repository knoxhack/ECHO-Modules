package com.knoxhack.echodeepreachprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echodeepreachprotocol.EchoDeepReachProtocol;
import net.minecraft.core.registries.Registries;

/**
 * Biome registry holder for ECHO: Deep Reach Protocol depth zones.
 * Biomes are data-driven under data/echodeepreachprotocol/worldgen/biome.
 */
public final class ModBiomes {
    public static final Object BIOMES = EchoBackendRegistryBridge.create(Registries.BIOME, EchoDeepReachProtocol.MODID);

    private ModBiomes() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BIOMES, eventBus);
    }
}
