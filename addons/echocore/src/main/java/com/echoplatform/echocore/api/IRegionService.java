package com.echoplatform.echocore.api;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public interface IRegionService {
    default boolean registerRegionDefinition(WorldRegionDefinition definition) {
        return false;
    }

    default List<WorldRegionDefinition> regions() {
        return List.of();
    }

    default Optional<WorldRegionDefinition> region(Identifier id) {
        return regions().stream().filter(definition -> definition.id().equals(id)).findFirst();
    }

    default List<WorldRegionDefinition> regionDefinitions() {
        return regions();
    }

    default Optional<WorldRegionDefinition> regionDefinition(Identifier id) {
        return region(id);
    }
}
