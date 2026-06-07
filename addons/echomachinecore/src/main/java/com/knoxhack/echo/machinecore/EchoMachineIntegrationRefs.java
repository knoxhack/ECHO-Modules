package com.knoxhack.echo.machinecore;

import com.knoxhack.echo.assetcore.EchoAssetReference;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.List;
import java.util.Map;

public record EchoMachineIntegrationRefs(
        EchoContentReference lensDiagnosticReference,
        EchoContentReference terminalRemoteStatusReference,
        EchoContentReference powerNodeReference,
        List<EchoFeatureId> optionalFeatures,
        List<EchoAssetReference> displayAssets,
        Map<String, String> attributes
) {
    public EchoMachineIntegrationRefs {
        optionalFeatures = MachineContractGuards.immutableList(optionalFeatures);
        displayAssets = MachineContractGuards.immutableList(displayAssets);
        attributes = MachineContractGuards.immutableMap(attributes);
    }
}
