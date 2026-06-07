package com.knoxhack.echo.inputcore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.List;
import java.util.Map;

public record EchoRadialMenuContract(
        String menuId,
        EchoInputBindingId openBinding,
        List<EchoContentReference> actionReferences,
        boolean pausesGameWhenOpen,
        boolean controllerReady,
        Map<String, String> attributes
) {
    public EchoRadialMenuContract {
        menuId = InputContractGuards.normalizedId(menuId, "radial menu id");
        actionReferences = InputContractGuards.immutableList(actionReferences);
        attributes = InputContractGuards.immutableMap(attributes);
    }
}
