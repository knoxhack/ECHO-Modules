package com.knoxhack.echo.assetcore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoAssetReference(
        EchoAssetId assetId,
        EchoAssetKind kind,
        EchoAssetPath path,
        EchoAssetOwner owner,
        EchoModuleId declaringModule,
        EchoAssetSource source,
        List<EchoAssetVariant> variants,
        EchoContentReference contentReference,
        boolean required,
        Map<String, String> attributes
) {
    public EchoAssetReference {
        Objects.requireNonNull(assetId, "assetId");
        kind = kind == null ? EchoAssetKind.UNKNOWN : kind;
        variants = AssetContractGuards.immutableList(variants);
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public boolean missingPath() {
        return path == null || path.value().isBlank();
    }

    public boolean textureLike() {
        return kind.textureLike();
    }
}
