package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record WorldRegionInstance(
        Identifier id,
        Identifier definitionId,
        WorldRegionType type,
        String displayName,
        String summary,
        ResourceKey<Level> dimension,
        BlockPos center,
        int radius,
        List<Identifier> hazardIds,
        boolean discovered) {
    public WorldRegionInstance {
        definitionId = definitionId == null ? id : definitionId;
        type = type == null ? WorldRegionType.CUSTOM : type;
        displayName = displayName == null ? "" : displayName;
        summary = summary == null ? "" : summary;
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        center = center == null ? BlockPos.ZERO : center;
        radius = Math.max(1, radius);
        hazardIds = hazardIds == null ? List.of() : List.copyOf(hazardIds);
    }

    public WorldRegionInstance(Identifier id, WorldRegionType type, String displayName, String summary, boolean discovered) {
        this(id, id, type, displayName, summary, Level.OVERWORLD, BlockPos.ZERO, 1, List.of(), discovered);
    }

    public WorldRegionInstance(
            Identifier id,
            Identifier definitionId,
            WorldRegionType type,
            String displayName,
            ResourceKey<Level> dimension,
            BlockPos center,
            int radius,
            List<Identifier> hazardIds,
            boolean discovered) {
        this(id, definitionId, type, displayName, "", dimension, center, radius, hazardIds, discovered);
    }

    public static WorldRegionInstance of(WorldRegionDefinition definition, boolean discovered) {
        if (definition == null) {
            return new WorldRegionInstance(null, WorldRegionType.CUSTOM, "", "", discovered);
        }
        return new WorldRegionInstance(definition.id(), definition.id(), definition.type(), definition.displayName(),
                definition.summary(), Level.OVERWORLD, BlockPos.ZERO, definition.radius(), definition.hazardIds(), discovered);
    }
}
