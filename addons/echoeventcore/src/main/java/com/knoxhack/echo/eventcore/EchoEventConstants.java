package com.knoxhack.echo.eventcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoEventConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of("echoeventcore");
    public static final EchoFeatureId FEATURE_WORLD_EVENTS = EchoFeatureId.of("event.world_events");
    public static final EchoFeatureId FEATURE_EVENT_SCHEDULER = EchoFeatureId.of("event.scheduler");
    public static final EchoFeatureId FEATURE_EVENT_VALIDATION = EchoFeatureId.of("event.validation");

    private EchoEventConstants() {
    }
}
