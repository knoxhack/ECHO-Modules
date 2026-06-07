package com.knoxhack.echo.powercore;

import java.util.Map;

public record EchoPowerTransferProfile(
        String tier,
        long maxThroughputPerTick,
        double lossPerBlock,
        boolean bidirectional,
        boolean acceptsWirelessRelay,
        Map<String, String> attributes
) {
    public EchoPowerTransferProfile {
        tier = PowerContractGuards.optionalText(tier);
        maxThroughputPerTick = PowerContractGuards.nonNegative(maxThroughputPerTick, "max throughput per tick");
        lossPerBlock = PowerContractGuards.nonNegative(lossPerBlock, "loss per block");
        attributes = PowerContractGuards.immutableMap(attributes);
    }

    public boolean transfersEnergy() {
        return maxThroughputPerTick > 0L;
    }
}
