package com.knoxhack.echo.cinematiccore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.List;
import java.util.Map;

public record EchoCinematicCameraPath(
        String pathId,
        EchoContentReference cameraProfileReference,
        List<EchoContentReference> anchorReferences,
        boolean locksPlayerControl,
        Map<String, String> attributes
) {
    public EchoCinematicCameraPath {
        pathId = CinematicContractGuards.normalizedId(pathId, "camera path id");
        anchorReferences = CinematicContractGuards.immutableList(anchorReferences);
        attributes = CinematicContractGuards.immutableMap(attributes);
    }
}
