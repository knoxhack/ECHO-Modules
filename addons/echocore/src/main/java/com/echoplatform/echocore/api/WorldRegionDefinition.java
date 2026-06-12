package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record WorldRegionDefinition(
        Identifier id,
        WorldRegionType type,
        String displayName,
        String summary,
        List<Identifier> biomeIds,
        List<Identifier> biomeTags,
        List<Identifier> structureIds,
        List<Identifier> hazardIds,
        Identifier discoveryId,
        int radius,
        Identifier renderProfileId,
        Identifier audioProfileId,
        int sortOrder) {
    public WorldRegionDefinition {
        type = type == null ? WorldRegionType.CUSTOM : type;
        displayName = displayName == null ? "" : displayName;
        summary = summary == null ? "" : summary;
        biomeIds = biomeIds == null ? List.of() : List.copyOf(biomeIds);
        biomeTags = biomeTags == null ? List.of() : List.copyOf(biomeTags);
        structureIds = structureIds == null ? List.of() : List.copyOf(structureIds);
        hazardIds = hazardIds == null ? List.of() : List.copyOf(hazardIds);
        discoveryId = discoveryId == null ? id : discoveryId;
        radius = Math.max(1, radius);
    }

    public String name() {
        return displayName;
    }

    public List<Identifier> biomes() {
        return biomeIds;
    }

    public List<Identifier> structures() {
        return structureIds;
    }

    public List<Identifier> hazards() {
        return hazardIds;
    }

    public Identifier icon() {
        return discoveryId;
    }

    public Identifier mapTexture() {
        return renderProfileId;
    }

    public Identifier ambience() {
        return audioProfileId;
    }

    public boolean biomeBacked() {
        return !biomeIds.isEmpty() || !biomeTags.isEmpty();
    }

    public boolean matchesStructure(Identifier structureId) {
        if (structureId == null) {
            return false;
        }
        return structureIds.contains(structureId)
                || structureIds.stream().anyMatch(id -> id != null && id.getPath().equals(structureId.getPath()));
    }
}
