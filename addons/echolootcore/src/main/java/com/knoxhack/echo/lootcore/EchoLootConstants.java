package com.knoxhack.echo.lootcore;

import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;

public final class EchoLootConstants {
    public static final EchoModuleId MODULE_ID = EchoModuleId.of(EchoLootCore.MODID);
    public static final EchoFeatureId FEATURE_LOOT_POOLS = EchoFeatureId.of("loot.pools");
    public static final EchoFeatureId FEATURE_MISSION_REWARD_POOLS = EchoFeatureId.of("loot.mission_reward_pools");
    public static final EchoFeatureId FEATURE_ANTI_DUPLICATION = EchoFeatureId.of("loot.anti_duplication");

    private EchoLootConstants() {
    }
}
