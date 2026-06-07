package com.knoxhack.echo.questdirector;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Map;

public record EchoDirectorSignal(
        EchoDirectorSignalId id,
        EchoDirectorSignalKind kind,
        EchoModuleId sourceModule,
        EchoContentReference targetReference,
        double strength,
        String summary,
        Map<String, String> attributes
) {
    public EchoDirectorSignal {
        kind = kind == null ? EchoDirectorSignalKind.UNKNOWN : kind;
        strength = QuestDirectorContractGuards.nonNegative(strength, "signal strength");
        summary = QuestDirectorContractGuards.optionalText(summary);
        attributes = QuestDirectorContractGuards.immutableMap(attributes);
    }
}
