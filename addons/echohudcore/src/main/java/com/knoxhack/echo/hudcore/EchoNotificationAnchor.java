package com.knoxhack.echo.hudcore;

import java.util.Map;

public record EchoNotificationAnchor(
        String anchorId,
        EchoHudAnchor anchor,
        int maxVisibleNotifications,
        int marginX,
        int marginY,
        Map<String, String> attributes
) {
    public EchoNotificationAnchor {
        anchorId = HudContractGuards.id(anchorId, "notification anchor id");
        anchor = anchor == null ? EchoHudAnchor.NOTIFICATION_STACK : anchor;
        maxVisibleNotifications = HudContractGuards.nonNegative(maxVisibleNotifications, "max visible notifications");
        marginX = HudContractGuards.nonNegative(marginX, "notification anchor margin x");
        marginY = HudContractGuards.nonNegative(marginY, "notification anchor margin y");
        attributes = HudContractGuards.immutableMap(attributes);
    }
}
