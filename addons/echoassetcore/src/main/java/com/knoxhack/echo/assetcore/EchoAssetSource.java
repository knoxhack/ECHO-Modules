package com.knoxhack.echo.assetcore;

import com.knoxhack.echo.contentcore.EchoContentSource;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.schemacore.EchoSchemaDocumentKind;
import com.knoxhack.echo.schemacore.EchoSchemaId;

import java.util.Map;

public record EchoAssetSource(
        String sourceId,
        EchoModuleId moduleId,
        EchoAssetPath path,
        EchoContentSource contentSource,
        EchoSchemaDocumentKind schemaDocumentKind,
        EchoSchemaId schemaId,
        boolean generated,
        String summary,
        Map<String, String> attributes
) {
    public EchoAssetSource {
        sourceId = AssetContractGuards.requireText(sourceId, "asset source id");
        summary = AssetContractGuards.optionalText(summary);
        attributes = AssetContractGuards.immutableMap(attributes);
    }

    public boolean fileBacked() {
        return path != null;
    }

    public boolean schemaAware() {
        return schemaDocumentKind != null || schemaId != null;
    }
}
