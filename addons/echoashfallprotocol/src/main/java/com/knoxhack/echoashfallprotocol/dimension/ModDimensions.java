package com.knoxhack.echoashfallprotocol.dimension;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Registry for custom dimensions.
 * Contains the Pre-Fall Archives pocket dimension for the final boss encounter.
 */
public class ModDimensions {

    public static final ResourceKey<Level> PREFALL_ARCHIVES = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "prefall_archives")
    );

    public static final ResourceKey<DimensionType> PREFALL_ARCHIVES_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "prefall_archives")
    );

    /**
     * Check if a level is the Pre-Fall Archives dimension
     */
    public static boolean isPrefallArchives(Level level) {
        return level.dimension().equals(PREFALL_ARCHIVES);
    }
}
