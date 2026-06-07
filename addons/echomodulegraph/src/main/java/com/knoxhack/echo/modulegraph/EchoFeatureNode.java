package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EchoFeatureNode(
        EchoFeatureId featureId,
        String label,
        Set<EchoModuleId> providers,
        Set<EchoModuleId> consumers,
        Set<EchoModuleId> requiredConsumers,
        Set<EchoModuleId> optionalConsumers,
        boolean required,
        Set<EchoRuntimeSide> runtimeSides,
        String apiStability,
        EchoFeatureStatus status,
        String deprecationInfo,
        EchoFeatureId replacementFeature,
        Set<EchoFeatureId> conflicts,
        Set<String> permissions,
        Set<String> capabilities,
        List<EchoModuleGraphIssue> issues
) {
    public EchoFeatureNode {
        Objects.requireNonNull(featureId, "featureId");
        label = label == null || label.isBlank() ? featureId.value() : label.trim();
        providers = ModuleGraphContractGuards.immutableSet(providers);
        consumers = ModuleGraphContractGuards.immutableSet(consumers);
        requiredConsumers = ModuleGraphContractGuards.immutableSet(requiredConsumers);
        optionalConsumers = ModuleGraphContractGuards.immutableSet(optionalConsumers);
        runtimeSides = ModuleGraphContractGuards.immutableSet(runtimeSides);
        apiStability = ModuleGraphContractGuards.optionalText(apiStability);
        status = status == null ? EchoFeatureStatus.UNKNOWN : status;
        deprecationInfo = ModuleGraphContractGuards.optionalText(deprecationInfo);
        conflicts = ModuleGraphContractGuards.immutableSet(conflicts);
        permissions = ModuleGraphContractGuards.immutableSet(permissions);
        capabilities = ModuleGraphContractGuards.immutableSet(capabilities);
        issues = ModuleGraphContractGuards.immutableList(issues);
    }

    public boolean missingProvider() {
        return required && providers.isEmpty();
    }
}
