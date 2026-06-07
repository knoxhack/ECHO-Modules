package com.knoxhack.echo.cameracore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoCameraTargetRef(
        EchoCameraAnchorKind kind,
        EchoContentReference targetReference,
        String anchorName,
        Map<String, String> attributes
) {
    public EchoCameraTargetRef {
        kind = kind == null ? EchoCameraAnchorKind.UNKNOWN : kind;
        anchorName = CameraContractGuards.optionalText(anchorName);
        attributes = CameraContractGuards.immutableMap(attributes);
    }
}
