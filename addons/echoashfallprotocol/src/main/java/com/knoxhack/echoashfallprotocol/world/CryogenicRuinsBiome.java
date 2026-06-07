package com.knoxhack.echoashfallprotocol.world;

import com.knoxhack.echoashfallprotocol.registry.ModBiomes;
import net.minecraft.world.level.biome.Biome;

/**
 * Cryogenic Ruins Biome - A frozen wasteland with cryo-pod structures.
 * Features: Extreme cold, frozen ash, ice-encased vegetation, glowing lichen.
 * Hazards: Cold exposure damage, cryo-mutants, periodic blizzards.
 * 
 * Note: Biome is registered via JSON in data/echoashfallprotocol/worldgen/biome/cryogenic_ruins.json
 * This class provides constants and helper methods for the biome.
 */
public class CryogenicRuinsBiome {
    
    public static final int SKY_COLOR = 0x87CEEB;       // Pale ice blue
    public static final int FOG_COLOR = 0xE0F7FA;       // Light cyan fog
    public static final int WATER_COLOR = 0x4A90E2;     // Deep ice blue
    public static final int WATER_FOG_COLOR = 0x2E5C8A; // Darker blue underwater
    public static final int GRASS_COLOR = 0xB8D4E3;     // Pale blue-white
    public static final int FOLIAGE_COLOR = 0xA8C4D9;   // Frozen blue
    
    /**
     * Biome creation is handled via JSON data files.
     * This method returns null to allow JSON override.
     */
    public static Biome create() {
        // Biome defined in data/echoashfallprotocol/worldgen/biome/cryogenic_ruins.json
        return null;
    }
    
    /**
     * Check if a position is in the Cryogenic Ruins biome
     */
    public static boolean isInBiome(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        return level.getBiome(pos).unwrapKey()
                .filter(ModBiomes.CRYOGENIC_RUINS::equals)
                .isPresent();
    }
}
