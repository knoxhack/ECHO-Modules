package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoApiStability;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleVersion;
import com.knoxhack.echo.platformcore.EchoTrustLevel;

import java.util.List;
import java.util.Objects;

public record EchoLockedModule(
        EchoModuleId moduleId,
        EchoModuleId requestedId,
        String requirement,
        EchoLockfileStatus status,
        EchoModuleVersion version,
        String path,
        String artifactName,
        String jarName,
        String jarPath,
        String source,
        EchoLockfileChecksum checksum,
        boolean required,
        EchoTrustLevel trustLevel,
        EchoApiStability apiStability,
        List<String> checksumInputs
) {
    public EchoLockedModule {
        Objects.requireNonNull(moduleId, "moduleId");
        requestedId = requestedId == null ? moduleId : requestedId;
        requirement = PackContractGuards.optionalText(requirement);
        status = status == null ? EchoLockfileStatus.LOCKED : status;
        version = version == null ? EchoModuleVersion.of("unknown") : version;
        path = PackContractGuards.optionalText(path);
        artifactName = PackContractGuards.optionalText(artifactName);
        jarName = PackContractGuards.optionalText(jarName);
        jarPath = PackContractGuards.optionalText(jarPath);
        source = PackContractGuards.optionalText(source);
        checksum = checksum == null ? new EchoLockfileChecksum("", EchoLockfileChecksumMode.UNKNOWN, "", List.of()) : checksum;
        trustLevel = trustLevel == null ? EchoTrustLevel.EXPERIMENTAL : trustLevel;
        apiStability = apiStability == null ? EchoApiStability.EXPERIMENTAL : apiStability;
        checksumInputs = PackContractGuards.immutableList(checksumInputs);
    }
}
