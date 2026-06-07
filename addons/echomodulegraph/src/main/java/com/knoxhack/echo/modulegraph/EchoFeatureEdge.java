package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Objects;

public record EchoFeatureEdge(
        EchoFeatureId featureId,
        EchoModuleId fromModule,
        EchoModuleId toModule,
        EchoFeatureEdgeKind kind,
        boolean required,
        String reason
) {
    public EchoFeatureEdge {
        Objects.requireNonNull(featureId, "featureId");
        Objects.requireNonNull(fromModule, "fromModule");
        kind = kind == null ? EchoFeatureEdgeKind.CONSUMES : kind;
        reason = ModuleGraphContractGuards.optionalText(reason);
    }
}
