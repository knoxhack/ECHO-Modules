package com.knoxhack.echo.powercore;

import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoPowerNode(
        EchoPowerNodeId id,
        EchoPowerNodeKind kind,
        EchoPowerFlowMode flowMode,
        EchoModuleId ownerModule,
        EchoContentReference contentReference,
        EchoPowerStorageProfile storageProfile,
        EchoPowerTransferProfile transferProfile,
        List<EchoPowerInstability> instability,
        Map<String, String> attributes
) {
    public EchoPowerNode {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoPowerNodeKind.UNKNOWN : kind;
        flowMode = flowMode == null ? EchoPowerFlowMode.UNKNOWN : flowMode;
        instability = PowerContractGuards.immutableList(instability);
        attributes = PowerContractGuards.immutableMap(attributes);
    }

    public boolean canStore() {
        return storageProfile != null && storageProfile.storesEnergy();
    }

    public boolean canTransfer() {
        return transferProfile != null && transferProfile.transfersEnergy();
    }
}
