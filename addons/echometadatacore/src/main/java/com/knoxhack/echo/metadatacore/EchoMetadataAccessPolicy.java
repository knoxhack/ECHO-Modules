package com.knoxhack.echo.metadatacore;

import com.knoxhack.echo.platformcore.EchoPermission;

import java.util.List;
import java.util.Set;

public record EchoMetadataAccessPolicy(
        Set<EchoPermission> requestedPermissions,
        List<String> readablePaths,
        List<String> writablePaths,
        boolean requiresConfirmationForWriteActions,
        String notes
) {
    public EchoMetadataAccessPolicy {
        requestedPermissions = MetadataContractGuards.immutableSet(requestedPermissions);
        readablePaths = MetadataContractGuards.immutableList(readablePaths);
        writablePaths = MetadataContractGuards.immutableList(writablePaths);
        notes = MetadataContractGuards.optionalText(notes);
    }

    public static EchoMetadataAccessPolicy readOnly(Set<EchoPermission> requestedPermissions) {
        return new EchoMetadataAccessPolicy(requestedPermissions, List.of(), List.of(), true, "Read-only metadata access.");
    }
}
