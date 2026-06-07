package com.knoxhack.echo.logisticscore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.List;
import java.util.Map;

public record EchoConvoyIntegrationRef(
        EchoContentReference convoyRouteReference,
        EchoContentReference convoyVehicleReference,
        EchoContentReference convoyAmbushEventReference,
        List<EchoFeatureId> optionalFeatures,
        Map<String, String> attributes
) {
    public EchoConvoyIntegrationRef {
        optionalFeatures = LogisticsContractGuards.immutableList(optionalFeatures);
        attributes = LogisticsContractGuards.immutableMap(attributes);
    }

    public boolean configured() {
        return convoyRouteReference != null || convoyVehicleReference != null || convoyAmbushEventReference != null;
    }
}
