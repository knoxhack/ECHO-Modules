package com.knoxhack.echo.notificationcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record EchoNotificationToast(
        EchoNotificationId id,
        EchoNotificationKind kind,
        EchoModuleId sourceModule,
        String titleTranslationKey,
        String bodyTranslationKey,
        EchoNotificationSeverity severity,
        EchoNotificationPriority priority,
        long durationTicks,
        EchoNotificationDeliveryState deliveryState,
        EchoContentReference sourceReference,
        EchoContentReference iconReference,
        EchoContentGate deliveryGate,
        Set<EchoFeatureId> optionalIntegrationFeatures,
        List<EchoNotificationAction> actions,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoNotificationToast {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoNotificationKind.UNKNOWN : kind;
        titleTranslationKey = NotificationContractGuards.requireText(titleTranslationKey, "notification title translation key");
        bodyTranslationKey = NotificationContractGuards.optionalText(bodyTranslationKey);
        severity = severity == null ? EchoNotificationSeverity.UNKNOWN : severity;
        priority = priority == null ? EchoNotificationPriority.UNKNOWN : priority;
        durationTicks = NotificationContractGuards.nonNegative(durationTicks, "notification duration ticks");
        deliveryState = deliveryState == null ? EchoNotificationDeliveryState.UNKNOWN : deliveryState;
        deliveryGate = deliveryGate == null ? EchoContentGate.open() : deliveryGate;
        optionalIntegrationFeatures = NotificationContractGuards.immutableSet(optionalIntegrationFeatures);
        actions = NotificationContractGuards.immutableList(actions);
        diagnostics = NotificationContractGuards.immutableList(diagnostics);
        attributes = NotificationContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return deliveryGate.blocksWhenMissing()
                || severity.attentionRequired()
                || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
