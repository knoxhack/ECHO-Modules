package com.knoxhack.echo.assetcore;

import com.knoxhack.echo.contentcore.EchoContentId;
import com.knoxhack.echo.contentcore.EchoContentOwner;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;

import java.util.Map;
import java.util.Objects;

public record EchoAssetOwner(
        EchoModuleId moduleId,
        EchoPackId packId,
        EchoContentId contentId,
        EchoContentOwner contentOwner,
        String publisher,
        boolean official,
        Map<String, String> attributes
) {
    public EchoAssetOwner {
        Objects.requireNonNull(moduleId, "moduleId");
        publisher = AssetContractGuards.optionalText(publisher);
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public static EchoAssetOwner module(EchoModuleId moduleId) {
        return new EchoAssetOwner(moduleId, null, null, null, "", false, Map.of());
    }
}
