package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoRuntimeSide;

import java.util.Map;
import java.util.Objects;

public record EchoAdapterContext(
        EchoModuleId ownerModuleId,
        EchoRuntimeSide side,
        EchoMinecraftVersionAdapter minecraftVersion,
        String environmentName,
        boolean developmentEnvironment,
        Map<String, String> properties
) {
    public EchoAdapterContext {
        Objects.requireNonNull(ownerModuleId, "ownerModuleId");
        side = side == null ? EchoRuntimeSide.COMMON : side;
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        environmentName = AdapterContractGuards.optionalText(environmentName);
        properties = AdapterContractGuards.immutableMap(properties);
    }
}
