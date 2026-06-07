package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

/**
 * Biome registrations for ECHO: ASHFALL PROTOCOL.
 * Re-compiled to fix stale cache issues.
 */
public class ModBiomes {
    public static final Object BIOMES = EchoBackendRegistryBridge.create(Registries.BIOME, EchoAshfallProtocol.MODID);
    
    // Data-driven Ashfall biomes live in data/echoashfallprotocol/worldgen/biome.
    // Do not register null holders for them; backend registry freezes treat those as unbound values
    // during GameTestServer registry freeze.

    // Exploration 1.1 - Cryogenic Ruins Biome
    public static final EchoBackendRegistryEntry<Biome> CRYOGENIC_RUINS = EchoBackendRegistryBridge.register(BIOMES,
            "cryogenic_ruins", 
            com.knoxhack.echoashfallprotocol.world.CryogenicRuinsBiome::create);

}
