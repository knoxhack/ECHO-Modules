package com.knoxhack.echo.creaturecore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoParticleAnchor(
        String anchorId,
        EchoContentReference particleProfileReference,
        String boneName,
        double offsetX,
        double offsetY,
        double offsetZ,
        Map<String, String> attributes
) {
    public EchoParticleAnchor {
        anchorId = CreatureContractGuards.requireText(anchorId, "particle anchor id");
        boneName = CreatureContractGuards.optionalText(boneName);
        offsetX = finite(offsetX, "particle anchor offset x");
        offsetY = finite(offsetY, "particle anchor offset y");
        offsetZ = finite(offsetZ, "particle anchor offset z");
        attributes = CreatureContractGuards.immutableMap(attributes);
    }

    private static double finite(double value, String fieldName) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
        return value;
    }
}
