package com.knoxhack.echo.economycore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.socialcore.EchoFactionId;
import com.knoxhack.echo.socialcore.EchoNpcProfileId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoFactionShop(
        EchoShopId id,
        String title,
        EchoFactionId factionId,
        EchoNpcProfileId npcProfileId,
        List<EchoBarterEntry> barterEntries,
        List<EchoContentReference> tradeReferences,
        List<EchoReputationPricingRule> pricingRules,
        Set<EchoFeatureId> optionalIntegrationFeatures,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoFactionShop {
        Objects.requireNonNull(id, "id");
        title = EconomyContractGuards.requireText(title, "faction shop title");
        barterEntries = EconomyContractGuards.immutableList(barterEntries);
        tradeReferences = EconomyContractGuards.immutableList(tradeReferences);
        pricingRules = EconomyContractGuards.immutableList(pricingRules);
        optionalIntegrationFeatures = EconomyContractGuards.immutableSet(optionalIntegrationFeatures);
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = EconomyContractGuards.immutableList(diagnostics);
        attributes = EconomyContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
