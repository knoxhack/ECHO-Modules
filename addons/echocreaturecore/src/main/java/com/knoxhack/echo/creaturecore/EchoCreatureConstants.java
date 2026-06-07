package com.knoxhack.echo.creaturecore;

import com.knoxhack.echo.platformcore.EchoFeatureId;

import java.util.Set;

public final class EchoCreatureConstants {
    public static final String MOD_ID = "echocreaturecore";
    public static final String MOD_NAME = "ECHO: CreatureCore";

    public static final EchoFeatureId FEATURE_CREATURE_ARCHETYPES = EchoFeatureId.of("creature.archetypes");
    public static final EchoFeatureId FEATURE_CREATURE_AI_PROFILES = EchoFeatureId.of("creature.ai_profiles");
    public static final EchoFeatureId FEATURE_CREATURE_SCAN_METADATA = EchoFeatureId.of("creature.scan_metadata");

    public static final Set<EchoFeatureId> PROVIDED_FEATURES = Set.of(
            FEATURE_CREATURE_ARCHETYPES,
            FEATURE_CREATURE_AI_PROFILES,
            FEATURE_CREATURE_SCAN_METADATA
    );

    private EchoCreatureConstants() {
    }
}
