package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;

import java.util.List;

public final class EchoMetadataConflictDetector {
    private EchoMetadataConflictDetector() {
    }

    public static List<EchoMetadataIssue> idMismatch(
            EchoModuleId inferredId,
            String declaredId,
            String sourcePath
    ) {
        if (declaredId == null || declaredId.isBlank() || inferredId.value().equals(declaredId)) {
            return List.of();
        }
        return List.of(EchoMetadataIssue.of(
                "metadata.id_mismatch",
                EchoDiagnosticSeverity.ERROR,
                inferredId,
                "Metadata declares id '" + declaredId + "' but scanner inferred '" + inferredId.value() + "'.",
                List.of(sourcePath)
        ));
    }
}
