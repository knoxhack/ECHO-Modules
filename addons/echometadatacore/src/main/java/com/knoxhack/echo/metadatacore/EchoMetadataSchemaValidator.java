package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;

import java.util.List;
import java.util.Map;

public final class EchoMetadataSchemaValidator {
    private EchoMetadataSchemaValidator() {
    }

    public static List<EchoMetadataIssue> requireSchema(EchoModuleId moduleId, Map<String, Object> payload, String sourcePath) {
        Object schema = payload.get("schema");
        if (schema instanceof String text && !text.isBlank()) {
            return List.of();
        }
        return List.of(EchoMetadataIssue.of(
                "metadata.schema_missing",
                EchoDiagnosticSeverity.ERROR,
                moduleId,
                "Metadata is missing required field: schema.",
                List.of(sourcePath)
        ));
    }
}
