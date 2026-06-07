package com.knoxhack.echo.vehiclecore;

import com.knoxhack.echo.assetcore.EchoAssetReference;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.List;
import java.util.Map;

public record EchoVehicleHookRefs(
        EchoContentReference mountUiReference,
        EchoContentReference cameraHookReference,
        EchoContentReference renderProfileReference,
        EchoContentReference soundProfileReference,
        List<EchoFeatureId> optionalFeatures,
        List<EchoAssetReference> assetReferences,
        Map<String, String> attributes
) {
    public EchoVehicleHookRefs {
        optionalFeatures = VehicleContractGuards.immutableList(optionalFeatures);
        assetReferences = VehicleContractGuards.immutableList(assetReferences);
        attributes = VehicleContractGuards.immutableMap(attributes);
    }

    public boolean hasClientFacingReferences() {
        return mountUiReference != null || cameraHookReference != null
                || renderProfileReference != null || soundProfileReference != null;
    }
}
