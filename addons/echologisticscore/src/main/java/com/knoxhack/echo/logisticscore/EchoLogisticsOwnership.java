package com.knoxhack.echo.logisticscore;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;

import java.util.Map;

public record EchoLogisticsOwnership(
        EchoModuleId ownerModule,
        EchoPackId ownerPack,
        String teamOrFactionId,
        boolean sharedNetwork,
        Map<String, String> attributes
) {
    public EchoLogisticsOwnership {
        teamOrFactionId = LogisticsContractGuards.optionalText(teamOrFactionId);
        attributes = LogisticsContractGuards.immutableMap(attributes);
    }
}
