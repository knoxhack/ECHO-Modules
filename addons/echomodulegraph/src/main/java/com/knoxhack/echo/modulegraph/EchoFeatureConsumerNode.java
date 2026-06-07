package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.Objects;
import java.util.Set;

public record EchoFeatureConsumerNode(
        EchoModuleId moduleId,
        boolean required,
        String versionRange,
        String reason,
        Set<EchoRuntimeSide> runtimeSides,
        boolean enabled
) {
    public EchoFeatureConsumerNode {
        Objects.requireNonNull(moduleId, "moduleId");
        versionRange = ModuleGraphContractGuards.optionalText(versionRange);
        reason = ModuleGraphContractGuards.optionalText(reason);
        runtimeSides = ModuleGraphContractGuards.immutableSet(runtimeSides);
    }
}
