package com.knoxhack.echo.encountercore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoEncounterConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of("echoencountercore");
    public static final EchoFeatureId FEATURE_ENCOUNTERS = EchoFeatureId.of("encounter.definitions");
    public static final EchoFeatureId FEATURE_BOSS_GATES = EchoFeatureId.of("encounter.boss_gates");
    public static final EchoFeatureId FEATURE_PATROLS = EchoFeatureId.of("encounter.faction_patrols");

    private EchoEncounterConstants() {
    }
}
