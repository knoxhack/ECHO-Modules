package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoCompatibilityIssue;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;
import com.knoxhack.echo.platformcore.EchoTrustLevel;

import java.util.Objects;
import java.util.Set;

public record EchoCompatibilityMatrix(
        EchoAdapterId adapterId,
        String minecraftVersionRange,
        String loaderVersionRange,
        boolean supportsNeoForge,
        boolean supportsEchoNative,
        boolean supportsClient,
        boolean supportsServer,
        Set<EchoRuntimeSide> supportedSides,
        Set<EchoTrustLevel> acceptedTrustLevels,
        Set<EchoCompatibilityIssue> issues
) {
    public EchoCompatibilityMatrix {
        Objects.requireNonNull(adapterId, "adapterId");
        minecraftVersionRange = AdapterContractGuards.optionalText(minecraftVersionRange);
        loaderVersionRange = AdapterContractGuards.optionalText(loaderVersionRange);
        supportedSides = AdapterContractGuards.immutableSet(supportedSides);
        acceptedTrustLevels = AdapterContractGuards.immutableSet(acceptedTrustLevels);
        issues = AdapterContractGuards.immutableSet(issues);
    }
}
