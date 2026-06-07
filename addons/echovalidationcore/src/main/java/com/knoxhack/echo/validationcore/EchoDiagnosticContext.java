package com.knoxhack.echo.validationcore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.schemacore.EchoSchemaDescriptor;
import com.knoxhack.echo.schemacore.EchoSchemaDocumentKind;

import java.util.Map;

public record EchoDiagnosticContext(
        EchoValidationScope scope,
        EchoModuleId moduleId,
        EchoPackId packId,
        EchoRuntimeSide side,
        EchoSchemaDescriptor schemaDescriptor,
        EchoSchemaDocumentKind documentKind,
        String targetPath,
        Map<String, String> attributes
) {
    public EchoDiagnosticContext {
        scope = scope == null ? EchoValidationScope.UNKNOWN : scope;
        side = side == null ? EchoRuntimeSide.COMMON : side;
        targetPath = ValidationContractGuards.optionalText(targetPath);
        attributes = ValidationContractGuards.immutableStringMap(attributes);
    }

    public static EchoDiagnosticContext workspace() {
        return new EchoDiagnosticContext(
                EchoValidationScope.WORKSPACE,
                null,
                null,
                EchoRuntimeSide.COMMON,
                null,
                null,
                "",
                Map.of()
        );
    }
}
