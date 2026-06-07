package com.knoxhack.echo.agentcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnosticCode;
import com.knoxhack.echo.validationcore.EchoDiagnosticSeverity;
import com.knoxhack.echo.validationcore.EchoValidationCategory;

import java.util.List;

public record EchoAiDiagnosticHint(
        EchoDiagnosticCode code,
        EchoDiagnosticSeverity severity,
        EchoValidationCategory category,
        EchoModuleId likelyModule,
        EchoFeatureId affectedFeature,
        String summary,
        String suggestedLane,
        List<String> likelyFiles,
        List<String> promptHints
) {
    public EchoAiDiagnosticHint {
        summary = AgentContractGuards.optionalText(summary);
        suggestedLane = AgentContractGuards.optionalText(suggestedLane);
        likelyFiles = AgentContractGuards.immutableList(likelyFiles);
        promptHints = AgentContractGuards.immutableList(promptHints);
    }
}
