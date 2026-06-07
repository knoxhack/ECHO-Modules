package com.knoxhack.echo.socialcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoSocialConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of(EchoSocialCore.MODID);
    public static final EchoFeatureId FEATURE_FACTIONS = EchoFeatureId.of("social.factions");
    public static final EchoFeatureId FEATURE_REPUTATION = EchoFeatureId.of("social.reputation");
    public static final EchoFeatureId FEATURE_DIALOGUE_TREES = EchoFeatureId.of("social.dialogue_trees");
    public static final EchoFeatureId FEATURE_NPC_PROFILES = EchoFeatureId.of("social.npc_profiles");
    public static final EchoFeatureId FEATURE_VILLAGER_REPLACEMENT_PLAN = EchoFeatureId.of("social.villager_replacement_plan");

    private EchoSocialConstants() {
    }
}
