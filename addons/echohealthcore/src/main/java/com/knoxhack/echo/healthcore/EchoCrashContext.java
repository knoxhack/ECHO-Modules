package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;

public record EchoCrashContext(
        String crashId,
        long occurredAtEpochMillis,
        EchoModuleId likelyModule,
        EchoFeatureId affectedFeature,
        String phase,
        String exceptionType,
        String message,
        String stackFingerprint,
        boolean repeated,
        List<String> recentSafeActions,
        List<String> relatedFiles
) {
    public EchoCrashContext {
        crashId = HealthContractGuards.requireText(crashId, "crash id");
        occurredAtEpochMillis = HealthContractGuards.nonNegative(occurredAtEpochMillis, "crash timestamp");
        phase = HealthContractGuards.optionalText(phase);
        exceptionType = HealthContractGuards.optionalText(exceptionType);
        message = HealthContractGuards.optionalText(message);
        stackFingerprint = HealthContractGuards.optionalText(stackFingerprint);
        recentSafeActions = HealthContractGuards.immutableList(recentSafeActions);
        relatedFiles = HealthContractGuards.immutableList(relatedFiles);
    }

    public static EchoCrashContext none() {
        return new EchoCrashContext("none", 0L, null, null, "", "", "", "", false, List.of(), List.of());
    }
}
