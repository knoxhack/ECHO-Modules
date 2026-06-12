package com.echoplatform.echocore.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class InMemoryRegionService implements IRegionService {
    private final Map<Identifier, WorldRegionDefinition> regions = new LinkedHashMap<>();

    @Override
    public boolean registerRegionDefinition(WorldRegionDefinition definition) {
        if (definition != null && definition.id() != null) {
            regions.put(definition.id(), definition);
            return true;
        }
        return false;
    }

    @Override
    public List<WorldRegionDefinition> regions() {
        return List.copyOf(regions.values());
    }
}
