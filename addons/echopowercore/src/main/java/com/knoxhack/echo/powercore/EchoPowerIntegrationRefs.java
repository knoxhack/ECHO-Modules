package com.knoxhack.echo.powercore;

import com.knoxhack.echo.assetcore.EchoAssetReference;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.List;
import java.util.Map;

public record EchoPowerIntegrationRefs(
        EchoContentReference weatherInterferenceReference,
        EchoContentReference nexusCorruptionReference,
        EchoContentReference lensDiagnosticReference,
        EchoContentReference terminalStatusReference,
        List<EchoFeatureId> optionalFeatures,
        List<EchoAssetReference> diagramAssets,
        Map<String, String> attributes
) {
    public EchoPowerIntegrationRefs {
        optionalFeatures = PowerContractGuards.immutableList(optionalFeatures);
        diagramAssets = PowerContractGuards.immutableList(diagramAssets);
        attributes = PowerContractGuards.immutableMap(attributes);
    }

    public boolean hasUiReferences() {
        return lensDiagnosticReference != null || terminalStatusReference != null;
    }
}
