package com.knoxhack.echo.notificationcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoNotificationRegistry(
        Map<EchoNotificationId, EchoNotificationToast> notifications,
        List<EchoNotificationChannel> channels,
        List<EchoNotificationRule> rules,
        List<EchoDiagnostic> diagnostics
) {
    public EchoNotificationRegistry {
        notifications = NotificationContractGuards.immutableMap(notifications);
        channels = NotificationContractGuards.immutableList(channels);
        rules = NotificationContractGuards.immutableList(rules);
        diagnostics = NotificationContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || notifications.values().stream().anyMatch(EchoNotificationToast::blocking);
    }
}
