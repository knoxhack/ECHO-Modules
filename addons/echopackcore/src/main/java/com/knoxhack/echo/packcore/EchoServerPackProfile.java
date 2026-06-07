package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;

public record EchoServerPackProfile(
        String id,
        String name,
        EchoModuleId rootModule,
        boolean dedicatedServerSupported,
        boolean requiresClientMatch,
        List<EchoPackModuleRequirement> serverRequiredModules,
        List<EchoPackFeatureRequirement> serverRequiredFeatures
) {
    public EchoServerPackProfile {
        id = PackContractGuards.requireText(id, "server profile id");
        name = PackContractGuards.requireText(name, "server profile name");
        serverRequiredModules = PackContractGuards.immutableList(serverRequiredModules);
        serverRequiredFeatures = PackContractGuards.immutableList(serverRequiredFeatures);
    }
}
