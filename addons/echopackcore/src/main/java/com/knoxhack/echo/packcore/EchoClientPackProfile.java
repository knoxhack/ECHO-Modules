package com.knoxhack.echo.packcore;

import java.util.List;

public record EchoClientPackProfile(
        String id,
        String name,
        boolean clientOnlyAllowed,
        boolean serverMatchRequired,
        List<EchoPackModuleRequirement> clientRequiredModules,
        List<EchoPackFeatureRequirement> clientRequiredFeatures
) {
    public EchoClientPackProfile {
        id = PackContractGuards.requireText(id, "client profile id");
        name = PackContractGuards.requireText(name, "client profile name");
        clientRequiredModules = PackContractGuards.immutableList(clientRequiredModules);
        clientRequiredFeatures = PackContractGuards.immutableList(clientRequiredFeatures);
    }
}
