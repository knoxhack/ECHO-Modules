package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EchoMetadataFieldValidator {
    private EchoMetadataFieldValidator() {
    }

    public static List<EchoMetadataIssue> requireFields(
            EchoModuleId moduleId,
            Map<String, Object> payload,
            String sourcePath,
            String codePrefix,
            List<String> requiredFields
    ) {
        List<EchoMetadataIssue> issues = new ArrayList<>();
        for (String field : requiredFields) {
            Object value = payload.get(field);
            if (value == null || value instanceof String text && text.isBlank()) {
                issues.add(EchoMetadataIssue.of(
                        codePrefix + "." + field + "_missing",
                        EchoDiagnosticSeverity.ERROR,
                        moduleId,
                        "Metadata is missing required field: " + field + ".",
                        List.of(sourcePath)
                ));
            }
        }
        return List.copyOf(issues);
    }
}
