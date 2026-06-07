package com.knoxhack.echo.machinecore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoMachineUpgradeSlot(
        String slotId,
        String category,
        int maxLevel,
        boolean optional,
        EchoContentReference upgradeReference,
        Map<String, String> attributes
) {
    public EchoMachineUpgradeSlot {
        slotId = MachineContractGuards.normalizedId(slotId, "upgrade slot id");
        category = MachineContractGuards.optionalText(category);
        maxLevel = Math.max(1, maxLevel);
        attributes = MachineContractGuards.immutableMap(attributes);
    }
}
