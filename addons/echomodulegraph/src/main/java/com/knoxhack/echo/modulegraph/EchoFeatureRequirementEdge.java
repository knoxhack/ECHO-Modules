package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Objects;

public record EchoFeatureRequirementEdge(
        EchoFeatureId featureId,
        EchoModuleId consumerModuleId,
        EchoModuleId providerModuleId,
        boolean required,
        String versionRange,
        String reason
) {
    public EchoFeatureRequirementEdge {
        Objects.requireNonNull(featureId, "featureId");
        Objects.requireNonNull(consumerModuleId, "consumerModuleId");
        versionRange = ModuleGraphContractGuards.optionalText(versionRange);
        reason = ModuleGraphContractGuards.optionalText(reason);
    }
}
