package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Set;

public record EchoMetadataAsset(
        String id,
        String kind,
        String path,
        boolean required,
        String owner,
        Set<EchoFeatureId> relatedFeatures
) {
    public EchoMetadataAsset {
        id = MetadataContractGuards.requireText(id, "asset id");
        kind = MetadataContractGuards.requireText(kind, "asset kind");
        path = MetadataContractGuards.requireText(path, "asset path");
        owner = MetadataContractGuards.optionalText(owner);
        relatedFeatures = MetadataContractGuards.immutableSet(relatedFeatures);
    }
}
