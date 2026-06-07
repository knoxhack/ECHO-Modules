package com.knoxhack.echo.difficultycore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoDifficultyConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of("echodifficultycore");
    public static final EchoFeatureId FEATURE_DIFFICULTY_PROFILES = EchoFeatureId.of("difficulty.profiles");
    public static final EchoFeatureId FEATURE_ADAPTIVE_DIFFICULTY = EchoFeatureId.of("difficulty.adaptive");
    public static final EchoFeatureId FEATURE_DIFFICULTY_TELEMETRY = EchoFeatureId.of("difficulty.telemetry");

    private EchoDifficultyConstants() {
    }
}
