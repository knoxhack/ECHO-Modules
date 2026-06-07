package com.knoxhack.echo.economycore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.List;
import java.util.Map;

public record EchoBarterEntry(
        String barterId,
        List<EchoContentReference> costItems,
        List<EchoCurrencyAmount> currencyCosts,
        List<EchoContentReference> outputs,
        EchoContentGate gate,
        boolean repeatable,
        Map<String, String> attributes
) {
    public EchoBarterEntry {
        barterId = EconomyContractGuards.requireText(barterId, "barter id");
        costItems = EconomyContractGuards.immutableList(costItems);
        currencyCosts = EconomyContractGuards.immutableList(currencyCosts);
        outputs = EconomyContractGuards.immutableList(outputs);
        gate = gate == null ? EchoContentGate.open() : gate;
        attributes = EconomyContractGuards.immutableMap(attributes);
    }
}
