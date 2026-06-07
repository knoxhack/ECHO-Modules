package com.knoxhack.echo.logisticscore;

import com.knoxhack.echo.assetcore.EchoAssetReference;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoLogisticsNetworkProfile(
        EchoLogisticsNetworkId id,
        EchoLogisticsOwnership ownership,
        List<EchoLogisticsStorageNode> storageNodes,
        List<EchoLogisticsRoute> routes,
        List<EchoLogisticsDeliveryJob> deliveryJobs,
        EchoConvoyIntegrationRef convoyIntegration,
        List<EchoAssetReference> diagramAssets,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoLogisticsNetworkProfile {
        Objects.requireNonNull(id, "id");
        storageNodes = LogisticsContractGuards.immutableList(storageNodes);
        routes = LogisticsContractGuards.immutableList(routes);
        deliveryJobs = LogisticsContractGuards.immutableList(deliveryJobs);
        diagramAssets = LogisticsContractGuards.immutableList(diagramAssets);
        diagnostics = LogisticsContractGuards.immutableList(diagnostics);
        attributes = LogisticsContractGuards.immutableMap(attributes);
    }

    public boolean degraded() {
        return deliveryJobs.stream().anyMatch(EchoLogisticsDeliveryJob::blocked)
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
