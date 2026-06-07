package com.knoxhack.echo.schemacore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.Objects;
import java.util.Set;

public record EchoSchemaOwner(
        EchoModuleId moduleId,
        String team,
        String contact,
        Set<EchoRuntimeSide> supportedSides
) {
    public EchoSchemaOwner {
        Objects.requireNonNull(moduleId, "moduleId");
        team = SchemaContractGuards.optionalText(team);
        contact = SchemaContractGuards.optionalText(contact);
        supportedSides = SchemaContractGuards.immutableSet(supportedSides);
    }
}
