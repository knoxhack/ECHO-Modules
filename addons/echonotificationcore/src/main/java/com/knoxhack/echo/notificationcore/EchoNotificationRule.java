package com.knoxhack.echo.notificationcore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Map;
import java.util.Set;

public record EchoNotificationRule(
        String ruleId,
        EchoNotificationKind kind,
        EchoNotificationPriority priority,
        EchoContentGate triggerGate,
        Set<EchoFeatureId> requiredFeatures,
        boolean suppressDuplicates,
        long cooldownTicks,
        Map<String, String> attributes
) {
    public EchoNotificationRule {
        ruleId = NotificationContractGuards.id(ruleId, "notification rule id");
        kind = kind == null ? EchoNotificationKind.UNKNOWN : kind;
        priority = priority == null ? EchoNotificationPriority.NORMAL : priority;
        triggerGate = triggerGate == null ? EchoContentGate.open() : triggerGate;
        requiredFeatures = NotificationContractGuards.immutableSet(requiredFeatures);
        cooldownTicks = NotificationContractGuards.nonNegative(cooldownTicks, "notification cooldown ticks");
        attributes = NotificationContractGuards.immutableMap(attributes);
    }
}
