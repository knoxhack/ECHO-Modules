package com.knoxhack.echo.contentcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.schemacore.EchoSchemaDocumentKind;
import com.knoxhack.echo.schemacore.EchoSchemaId;
import com.knoxhack.echo.schemacore.EchoSchemaVersion;

import java.util.Map;

public record EchoContentSource(
        String sourceId,
        EchoContentSourceKind sourceKind,
        EchoModuleId moduleId,
        EchoContentPackRef packRef,
        String path,
        EchoSchemaDocumentKind schemaDocumentKind,
        EchoSchemaId schemaId,
        EchoSchemaVersion schemaVersion,
        boolean generated,
        String summary,
        Map<String, String> attributes
) {
    public EchoContentSource {
        sourceId = ContentContractGuards.requireText(sourceId, "content source id");
        sourceKind = sourceKind == null ? EchoContentSourceKind.UNKNOWN : sourceKind;
        path = ContentContractGuards.optionalText(path);
        summary = ContentContractGuards.optionalText(summary);
        attributes = ContentContractGuards.immutableMap(attributes);
    }

    public boolean schemaAware() {
        return schemaDocumentKind != null || schemaId != null || schemaVersion != null;
    }

    public boolean fileBacked() {
        return !path.isEmpty();
    }
}
