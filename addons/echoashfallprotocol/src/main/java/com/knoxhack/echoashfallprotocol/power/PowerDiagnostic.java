package com.knoxhack.echoashfallprotocol.power;

import com.knoxhack.echoashfallprotocol.block.entity.LoadDistributorBlockEntity;

public record PowerDiagnostic(
        int localBuffer,
        int localCapacity,
        int networkStored,
        int networkCapacity,
        int transferLimit,
        int estimatedDemand,
        LoadDistributorBlockEntity.PriorityMode priorityMode,
        PowerIssue issue
) {
    public boolean isPowered() {
        return !issue.isBlocking();
    }

    public String hintKey() {
        return issue.hintKey();
    }
}
