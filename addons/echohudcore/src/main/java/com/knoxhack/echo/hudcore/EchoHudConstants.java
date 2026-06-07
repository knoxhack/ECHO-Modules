package com.knoxhack.echo.hudcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoHudConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of("echohudcore");
    public static final EchoFeatureId FEATURE_HUD_WIDGETS = EchoFeatureId.of("hud.widgets");
    public static final EchoFeatureId FEATURE_MISSION_TRACKER = EchoFeatureId.of("hud.mission_tracker");
    public static final EchoFeatureId FEATURE_COMPASS_INDICATORS = EchoFeatureId.of("hud.compass_indicators");
    public static final EchoFeatureId FEATURE_HAZARD_METERS = EchoFeatureId.of("hud.hazard_meters");
    public static final EchoFeatureId FEATURE_SAFE_AREA = EchoFeatureId.of("hud.safe_area");

    private EchoHudConstants() {
    }
}
