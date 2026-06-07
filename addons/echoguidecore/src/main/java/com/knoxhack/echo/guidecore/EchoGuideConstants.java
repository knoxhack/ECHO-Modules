package com.knoxhack.echo.guidecore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoGuideConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of("echoguidecore");
    public static final EchoFeatureId FEATURE_GUIDE_PAGES = EchoFeatureId.of("guide.pages");
    public static final EchoFeatureId FEATURE_GUIDE_SEARCH = EchoFeatureId.of("guide.search");
    public static final EchoFeatureId FEATURE_GUIDE_UNLOCK_VISIBILITY = EchoFeatureId.of("guide.unlock_visibility");

    private EchoGuideConstants() {
    }
}
