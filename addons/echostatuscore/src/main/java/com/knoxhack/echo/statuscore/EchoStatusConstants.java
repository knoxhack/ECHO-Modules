package com.knoxhack.echo.statuscore;

import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Set;

public final class EchoStatusConstants {
    public static final String MOD_ID = "echostatuscore";
    public static final String MOD_NAME = "ECHO: StatusCore";

    public static final EchoFeatureId FEATURE_STATUS_EFFECTS = EchoFeatureId.of("status.effects");
    public static final EchoFeatureId FEATURE_STATUS_EXPOSURE = EchoFeatureId.of("status.exposure");
    public static final EchoFeatureId FEATURE_STATUS_RESISTANCE = EchoFeatureId.of("status.resistance");

    public static final Set<EchoFeatureId> PROVIDED_FEATURES = Set.of(
            FEATURE_STATUS_EFFECTS,
            FEATURE_STATUS_EXPOSURE,
            FEATURE_STATUS_RESISTANCE
    );

    private EchoStatusConstants() {
    }
}
