package com.knoxhack.echo.notificationcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoNotificationConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of("echonotificationcore");
    public static final EchoFeatureId FEATURE_TOASTS = EchoFeatureId.of("notifications.toasts");
    public static final EchoFeatureId FEATURE_SYSTEM_ALERTS = EchoFeatureId.of("notifications.system_alerts");
    public static final EchoFeatureId FEATURE_MISSION_UPDATES = EchoFeatureId.of("notifications.mission_updates");
    public static final EchoFeatureId FEATURE_TUTORIAL_HINTS = EchoFeatureId.of("notifications.tutorial_hints");

    private EchoNotificationConstants() {
    }
}
