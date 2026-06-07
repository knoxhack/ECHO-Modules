package com.knoxhack.echo.inputcore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;
import java.util.Map;

public record EchoInputProfile(
        EchoModuleId ownerModule,
        List<EchoInputContext> contexts,
        List<EchoRadialMenuContract> radialMenus,
        List<EchoInputConflict> conflicts,
        Map<String, String> attributes
) {
    public EchoInputProfile {
        contexts = InputContractGuards.immutableList(contexts);
        radialMenus = InputContractGuards.immutableList(radialMenus);
        conflicts = InputContractGuards.immutableList(conflicts);
        attributes = InputContractGuards.immutableMap(attributes);
    }

    public boolean hasBlockingConflicts() {
        return conflicts.stream().anyMatch(EchoInputConflict::blocking);
    }
}
