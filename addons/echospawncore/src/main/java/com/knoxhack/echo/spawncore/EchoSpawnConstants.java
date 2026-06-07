package com.knoxhack.echo.spawncore;

import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Set;

public final class EchoSpawnConstants {
    public static final String MOD_ID = "echospawncore";
    public static final String MOD_NAME = "ECHO: SpawnCore";

    public static final EchoFeatureId FEATURE_SPAWN_PROFILES = EchoFeatureId.of("spawn.profiles");
    public static final EchoFeatureId FEATURE_SPAWN_HAZARDS = EchoFeatureId.of("spawn.hazard_rules");
    public static final EchoFeatureId FEATURE_SPAWN_DIFFICULTY = EchoFeatureId.of("spawn.difficulty_scaling");

    public static final Set<EchoFeatureId> PROVIDED_FEATURES = Set.of(
            FEATURE_SPAWN_PROFILES,
            FEATURE_SPAWN_HAZARDS,
            FEATURE_SPAWN_DIFFICULTY
    );

    private EchoSpawnConstants() {
    }
}
