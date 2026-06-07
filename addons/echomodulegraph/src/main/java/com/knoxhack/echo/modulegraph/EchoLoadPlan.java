package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record EchoLoadPlan(
        List<EchoModuleId> loadOrder,
        Map<EchoModuleId, List<EchoModuleId>> dependencyEdges,
        Set<EchoModuleId> blockedModules,
        Set<EchoModuleId> degradedModules,
        List<EchoModuleGraphIssue> issues
) {
    public EchoLoadPlan {
        loadOrder = ModuleGraphContractGuards.immutableList(loadOrder);
        dependencyEdges = dependencyEdges == null ? Map.of() : dependencyEdges.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        blockedModules = ModuleGraphContractGuards.immutableSet(blockedModules);
        degradedModules = ModuleGraphContractGuards.immutableSet(degradedModules);
        issues = ModuleGraphContractGuards.immutableList(issues);
    }

    public boolean valid() {
        return issues.stream().noneMatch(issue -> issue.toDiagnostic().blocking());
    }

    public static EchoLoadPlan empty() {
        return new EchoLoadPlan(List.of(), Map.of(), Set.of(), Set.of(), List.of());
    }
}
