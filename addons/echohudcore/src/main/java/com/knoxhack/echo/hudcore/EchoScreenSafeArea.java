package com.knoxhack.echo.hudcore;

import java.util.Map;

public record EchoScreenSafeArea(
        String safeAreaId,
        int leftInset,
        int topInset,
        int rightInset,
        int bottomInset,
        boolean respectsChat,
        boolean respectsBossBars,
        boolean respectsSubtitles,
        Map<String, String> attributes
) {
    public EchoScreenSafeArea {
        safeAreaId = HudContractGuards.id(safeAreaId, "screen safe area id");
        leftInset = HudContractGuards.nonNegative(leftInset, "left inset");
        topInset = HudContractGuards.nonNegative(topInset, "top inset");
        rightInset = HudContractGuards.nonNegative(rightInset, "right inset");
        bottomInset = HudContractGuards.nonNegative(bottomInset, "bottom inset");
        attributes = HudContractGuards.immutableMap(attributes);
    }
}
