package com.knoxhack.echo.cinematiccore;

import com.knoxhack.echo.assetcore.EchoAssetReference;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoCinematicSequence(
        EchoCinematicSequenceId id,
        EchoCinematicSequenceKind kind,
        EchoModuleId ownerModule,
        EchoCinematicTrigger trigger,
        EchoCinematicCameraPath cameraPath,
        EchoCinematicPacing pacing,
        List<EchoContentReference> relatedContent,
        List<EchoAssetReference> storyboardAssets,
        boolean screenshotModeAllowed,
        boolean cinematicModeAllowed,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoCinematicSequence {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoCinematicSequenceKind.UNKNOWN : kind;
        relatedContent = CinematicContractGuards.immutableList(relatedContent);
        storyboardAssets = CinematicContractGuards.immutableList(storyboardAssets);
        diagnostics = CinematicContractGuards.immutableList(diagnostics);
        attributes = CinematicContractGuards.immutableMap(attributes);
    }

    public boolean degraded() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
