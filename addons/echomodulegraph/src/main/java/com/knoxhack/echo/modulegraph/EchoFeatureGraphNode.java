package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record EchoFeatureGraphNode(
        EchoFeatureId featureId,
        List<EchoFeatureProviderNode> providers,
        List<EchoFeatureConsumerNode> consumers,
        List<EchoFeatureConsumerNode> requiredBy,
        List<EchoFeatureConsumerNode> optionalFor,
        Set<EchoRuntimeSide> runtimeSides,
        EchoApiStability apiStability,
        EchoFeatureStatus status,
        String deprecationInfo,
        EchoFeatureId replacementFeature,
        Set<EchoFeatureId> conflicts,
        Set<String> permissions,
        Set<String> capabilities,
        List<EchoFeatureGraphIssue> issues
) {
    public EchoFeatureGraphNode {
        Objects.requireNonNull(featureId, "featureId");
        providers = ModuleGraphContractGuards.immutableList(providers);
        consumers = ModuleGraphContractGuards.immutableList(consumers);
        requiredBy = ModuleGraphContractGuards.immutableList(requiredBy);
        optionalFor = ModuleGraphContractGuards.immutableList(optionalFor);
        runtimeSides = ModuleGraphContractGuards.immutableSet(runtimeSides);
        apiStability = apiStability == null ? EchoApiStability.EXPERIMENTAL : apiStability;
        status = status == null ? EchoFeatureStatus.UNKNOWN : status;
        deprecationInfo = ModuleGraphContractGuards.optionalText(deprecationInfo);
        conflicts = ModuleGraphContractGuards.immutableSet(conflicts);
        permissions = ModuleGraphContractGuards.immutableSet(permissions);
        capabilities = ModuleGraphContractGuards.immutableSet(capabilities);
        issues = ModuleGraphContractGuards.immutableList(issues);
    }
}
