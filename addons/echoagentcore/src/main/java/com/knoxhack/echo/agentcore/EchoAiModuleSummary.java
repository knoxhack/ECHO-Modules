package com.knoxhack.echo.agentcore;

import com.knoxhack.echo.metadatacore.EchoAiMetadata;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleKind;
import com.knoxhack.echo.platformcore.EchoModuleRole;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.List;
import java.util.Set;

public record EchoAiModuleSummary(
        EchoModuleId moduleId,
        String name,
        EchoModuleKind kind,
        Set<EchoModuleRole> roles,
        EchoRuntimeSide side,
        String summary,
        EchoAiMetadata metadata,
        List<String> importantPackages,
        List<String> mainClasses,
        Set<EchoFeatureId> providedFeatures,
        Set<EchoFeatureId> consumedFeatures,
        List<EchoAiDiagnosticHint> diagnosticHints
) {
    public EchoAiModuleSummary {
        if (moduleId == null) {
            throw new IllegalArgumentException("module id is required");
        }
        name = AgentContractGuards.optionalText(name);
        kind = kind == null ? EchoModuleKind.ADDON : kind;
        roles = AgentContractGuards.immutableSet(roles);
        side = side == null ? EchoRuntimeSide.COMMON : side;
        summary = AgentContractGuards.optionalText(summary);
        importantPackages = AgentContractGuards.immutableList(importantPackages);
        mainClasses = AgentContractGuards.immutableList(mainClasses);
        providedFeatures = AgentContractGuards.immutableSet(providedFeatures);
        consumedFeatures = AgentContractGuards.immutableSet(consumedFeatures);
        diagnosticHints = AgentContractGuards.immutableList(diagnosticHints);
    }
}
