package com.knoxhack.echorecovery.api;

import java.util.Optional;

public interface EchoCrashLoopDetector {
    Optional<EchoRecoveryRecommendation> detect(EchoRecoveryContext context);

    default boolean likelyCrashLoop(EchoRecoveryContext context) {
        return context != null && context.triggers().contains(EchoRecoveryTrigger.REPEATED_LAUNCH_FAILURE);
    }

    static EchoCrashLoopDetector repeatedLaunchFailure() {
        return context -> {
            if (context == null || !context.triggers().contains(EchoRecoveryTrigger.REPEATED_LAUNCH_FAILURE)) {
                return Optional.empty();
            }
            EchoRecoveryAction action = EchoRecoveryAction.of(
                    "run_validation_after_launch_failures",
                    EchoRecoveryActionKind.RUN_VALIDATION,
                    EchoRecoveryRisk.LOW,
                    "Run validation before changing the install."
            );
            return Optional.of(new EchoRecoveryRecommendation(
                    "repeated_launch_failure_validation",
                    EchoRecoveryTrigger.REPEATED_LAUNCH_FAILURE,
                    EchoRecoveryMode.RECOVERY_MODE,
                    EchoRecoveryRisk.LOW,
                    "Repeated launch failures detected",
                    "ECHO can validate the pack and prepare a support bundle before changing anything.",
                    "This recommendation is intentionally non-destructive and should run before disabling modules.",
                    0.85D,
                    java.util.List.of(action),
                    context.affectedModules(),
                    context.affectedFeatures(),
                    context.diagnostics()
            ));
        };
    }
}
