package com.knoxhack.echorecovery.api;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;
import java.util.Map;

public record EchoSafeModeProfile(
        String id,
        String name,
        EchoRecoveryMode mode,
        List<EchoModuleId> disabledOptionalModules,
        List<EchoFeatureId> disabledFeatures,
        List<EchoRecoveryActionKind> allowedActions,
        boolean disableCinematicRender,
        boolean reduceParticles,
        boolean useSafeUiScale,
        boolean preserveSaves,
        boolean runValidation,
        boolean exportSupportBundle,
        Map<String, String> attributes
) {
    public EchoSafeModeProfile {
        id = RecoveryContractGuards.requireText(id, "safe mode profile id");
        name = RecoveryContractGuards.requireText(name, "safe mode profile name");
        mode = mode == null ? EchoRecoveryMode.SAFE_MODE : mode;
        disabledOptionalModules = RecoveryContractGuards.immutableList(disabledOptionalModules);
        disabledFeatures = RecoveryContractGuards.immutableList(disabledFeatures);
        allowedActions = RecoveryContractGuards.immutableList(allowedActions);
        preserveSaves = true;
        attributes = RecoveryContractGuards.immutableMap(attributes);
    }

    public static EchoSafeModeProfile standard() {
        return new EchoSafeModeProfile(
                "standard_safe_mode",
                "Standard Safe Mode",
                EchoRecoveryMode.SAFE_MODE,
                List.of(),
                List.of(),
                List.of(
                        EchoRecoveryActionKind.DISABLE_OPTIONAL_MODULE,
                        EchoRecoveryActionKind.DISABLE_CINEMATIC_RENDER,
                        EchoRecoveryActionKind.REDUCE_PARTICLES,
                        EchoRecoveryActionKind.USE_SAFE_UI_SCALE,
                        EchoRecoveryActionKind.CREATE_SNAPSHOT,
                        EchoRecoveryActionKind.EXPORT_SUPPORT_BUNDLE,
                        EchoRecoveryActionKind.PRESERVE_SAVES,
                        EchoRecoveryActionKind.RUN_VALIDATION,
                        EchoRecoveryActionKind.DISABLE_EXPERIMENTAL_FEATURES
                ),
                true,
                true,
                true,
                true,
                true,
                true,
                Map.of()
        );
    }
}
