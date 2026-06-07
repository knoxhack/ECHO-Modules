package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Objects;

public record EchoModuleGraphEdge(
        EchoModuleId fromModule,
        EchoModuleId toModule,
        EchoModuleGraphEdgeKind kind,
        EchoFeatureId featureId,
        String versionRange,
        boolean required,
        boolean present,
        String status,
        String reason
) {
    public EchoModuleGraphEdge {
        Objects.requireNonNull(fromModule, "fromModule");
        Objects.requireNonNull(kind, "kind");
        versionRange = ModuleGraphContractGuards.optionalText(versionRange);
        status = status == null || status.isBlank() ? "unknown" : status.trim();
        reason = ModuleGraphContractGuards.optionalText(reason);
    }
}
