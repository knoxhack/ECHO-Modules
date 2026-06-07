package com.knoxhack.echo.questdirector;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoQuestDirectorConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of("echoquestdirector");
    public static final EchoFeatureId FEATURE_MISSION_SELECTION = EchoFeatureId.of("questdirector.mission_selection");
    public static final EchoFeatureId FEATURE_ROUTE_PACING = EchoFeatureId.of("questdirector.route_pacing");
    public static final EchoFeatureId FEATURE_CAMPAIGN_PRESSURE = EchoFeatureId.of("questdirector.campaign_pressure");

    private EchoQuestDirectorConstants() {
    }
}
