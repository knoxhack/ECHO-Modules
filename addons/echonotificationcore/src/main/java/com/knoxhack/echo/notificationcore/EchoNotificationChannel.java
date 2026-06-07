package com.knoxhack.echo.notificationcore;

import com.knoxhack.echo.contentcore.EchoContentGate;

import java.util.Map;

public record EchoNotificationChannel(
        String channelId,
        String nameTranslationKey,
        EchoNotificationPriority minimumPriority,
        boolean enabledByDefault,
        EchoContentGate visibilityGate,
        Map<String, String> attributes
) {
    public EchoNotificationChannel {
        channelId = NotificationContractGuards.id(channelId, "notification channel id");
        nameTranslationKey = NotificationContractGuards.requireText(nameTranslationKey, "notification channel name translation key");
        minimumPriority = minimumPriority == null ? EchoNotificationPriority.LOW : minimumPriority;
        visibilityGate = visibilityGate == null ? EchoContentGate.open() : visibilityGate;
        attributes = NotificationContractGuards.immutableMap(attributes);
    }
}
