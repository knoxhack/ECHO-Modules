package com.knoxhack.echo.cameracore;

import com.knoxhack.echo.assetcore.EchoAssetReference;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoCameraProfile(
        EchoCameraProfileId id,
        EchoCameraMode mode,
        EchoModuleId ownerModule,
        List<EchoCameraTargetRef> targets,
        EchoContentReference npcConversationReference,
        EchoContentReference vehicleCameraReference,
        EchoCameraShakeProfile shakeProfile,
        EchoCameraSafetyConstraint safetyConstraint,
        List<EchoAssetReference> creatorToolAssets,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoCameraProfile {
        Objects.requireNonNull(id, "id");
        mode = mode == null ? EchoCameraMode.UNKNOWN : mode;
        targets = CameraContractGuards.immutableList(targets);
        creatorToolAssets = CameraContractGuards.immutableList(creatorToolAssets);
        diagnostics = CameraContractGuards.immutableList(diagnostics);
        attributes = CameraContractGuards.immutableMap(attributes);
    }

    public boolean degraded() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
