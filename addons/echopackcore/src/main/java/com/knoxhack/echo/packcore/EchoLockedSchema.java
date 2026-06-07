package com.knoxhack.echo.packcore;

import com.knoxhack.echo.schemacore.EchoSchemaId;

import java.util.Objects;

public record EchoLockedSchema(
        EchoSchemaId schemaId,
        String version,
        EchoLockfileStatus status,
        String sourcePath
) {
    public EchoLockedSchema {
        Objects.requireNonNull(schemaId, "schemaId");
        version = PackContractGuards.optionalText(version);
        status = status == null ? EchoLockfileStatus.LOCKED : status;
        sourcePath = PackContractGuards.optionalText(sourcePath);
    }
}
