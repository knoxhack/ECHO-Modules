package com.knoxhack.echo.notificationcore;

import com.knoxhack.echo.contentcore.EchoContentReference;

import java.util.Map;

public record EchoNotificationAction(
        String actionId,
        String labelTranslationKey,
        EchoContentReference targetReference,
        boolean requiresConfirmation,
        Map<String, String> attributes
) {
    public EchoNotificationAction {
        actionId = NotificationContractGuards.id(actionId, "notification action id");
        labelTranslationKey = NotificationContractGuards.requireText(labelTranslationKey, "notification action label translation key");
        attributes = NotificationContractGuards.immutableMap(attributes);
    }
}
