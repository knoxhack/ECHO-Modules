package com.knoxhack.echorelictech.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class RelicTechConfig {
    public static final EchoNativeConfigSpec SPEC;

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_RELIC_INSTABILITY;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_RELIC_FAILURES;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_PASSIVE_CONTAINMENT_WARNINGS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_FACTION_REACTION_SCAFFOLD;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_WORLD_HAZARD_BACKFIRES;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_POWERGRID_BACKFIRES;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_PHASE_ANCHOR_CROSS_DIMENSION;
    public static final EchoNativeConfigSpec.IntValue PHASE_ANCHOR_COOLDOWN_TICKS;
    public static final EchoNativeConfigSpec.IntValue PHASE_ANCHOR_INSTABILITY_COST;
    public static final EchoNativeConfigSpec.IntValue GUARDIAN_LENS_SCAN_RADIUS;
    public static final EchoNativeConfigSpec.IntValue ECHO_MIRROR_COOLDOWN_TICKS;
    public static final EchoNativeConfigSpec.IntValue MATTER_STITCHER_COOLDOWN_TICKS;
    public static final EchoNativeConfigSpec.IntValue NULL_BATTERY_MAX_CHARGE;
    public static final EchoNativeConfigSpec.BooleanValue INSTABILITY_DECAY_ENABLED;
    public static final EchoNativeConfigSpec.IntValue INSTABILITY_DECAY_DELAY_TICKS;
    public static final EchoNativeConfigSpec.IntValue INSTABILITY_DECAY_AMOUNT;
    public static final EchoNativeConfigSpec.IntValue MAX_INSTABILITY;
    public static final EchoNativeConfigSpec.IntValue LEVEL1_THRESHOLD;
    public static final EchoNativeConfigSpec.IntValue LEVEL2_THRESHOLD;
    public static final EchoNativeConfigSpec.IntValue LEVEL3_THRESHOLD;
    public static final EchoNativeConfigSpec.IntValue LEVEL4_THRESHOLD;
    public static final EchoNativeConfigSpec.IntValue LEVEL5_THRESHOLD;

    public static final EchoNativeConfigSpec.DoubleValue DAMAGED_FAILURE_CHANCE;
    public static final EchoNativeConfigSpec.DoubleValue STABILIZED_FAILURE_CHANCE;
    public static final EchoNativeConfigSpec.DoubleValue OVERCLOCKED_FAILURE_CHANCE;
    public static final EchoNativeConfigSpec.DoubleValue CONTAINED_FAILURE_CHANCE;
    public static final EchoNativeConfigSpec.DoubleValue CORRUPTED_FAILURE_CHANCE;
    public static final EchoNativeConfigSpec.DoubleValue LEVEL3_INSTABILITY_FAILURE_BONUS;
    public static final EchoNativeConfigSpec.DoubleValue LEVEL4_INSTABILITY_FAILURE_BONUS;
    public static final EchoNativeConfigSpec.DoubleValue LEVEL5_INSTABILITY_FAILURE_BONUS;

    static {
        EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();

        builder.push("general");
        ENABLE_RELIC_INSTABILITY = builder.comment("Enable player relic instability.").define("enableRelicInstability", true);
        ENABLE_RELIC_FAILURES = builder.comment("Enable relic failure system.").define("enableRelicFailures", true);
        ENABLE_PASSIVE_CONTAINMENT_WARNINGS = builder.comment("Enable passive containment warnings.").define("enablePassiveContainmentWarnings", true);
        ENABLE_FACTION_REACTION_SCAFFOLD = builder.comment("Enable faction reaction scaffold.").define("enableFactionReactionScaffold", true);
        ENABLE_WORLD_HAZARD_BACKFIRES = builder.comment("Enable world hazard backfires.").define("enableWorldHazardBackfires", true);
        ENABLE_POWERGRID_BACKFIRES = builder.comment("Enable PowerGrid backfires.").define("enablePowerGridBackfires", true);
        ALLOW_PHASE_ANCHOR_CROSS_DIMENSION = builder.comment("Allow Phase Anchor cross-dimension teleport.").define("allowPhaseAnchorCrossDimension", false);
        PHASE_ANCHOR_COOLDOWN_TICKS = builder.comment("Phase Anchor cooldown in ticks.").defineInRange("phaseAnchorCooldownTicks", 7200, 0, Integer.MAX_VALUE);
        PHASE_ANCHOR_INSTABILITY_COST = builder.comment("Phase Anchor instability cost.").defineInRange("phaseAnchorInstabilityCost", 18, 0, 1000);
        GUARDIAN_LENS_SCAN_RADIUS = builder.comment("Guardian Lens scan radius.").defineInRange("guardianLensScanRadius", 64, 1, 256);
        ECHO_MIRROR_COOLDOWN_TICKS = builder.comment("Echo Mirror cooldown in ticks.").defineInRange("echoMirrorCooldownTicks", 2400, 0, Integer.MAX_VALUE);
        MATTER_STITCHER_COOLDOWN_TICKS = builder.comment("Matter Stitcher cooldown in ticks.").defineInRange("matterStitcherCooldownTicks", 3600, 0, Integer.MAX_VALUE);
        NULL_BATTERY_MAX_CHARGE = builder.comment("Null Battery max charge.").defineInRange("nullBatteryMaxCharge", 8, 1, 64);
        INSTABILITY_DECAY_ENABLED = builder.comment("Enable instability decay.").define("instabilityDecayEnabled", true);
        INSTABILITY_DECAY_DELAY_TICKS = builder.comment("Instability decay delay in ticks.").defineInRange("instabilityDecayDelayTicks", 12000, 0, Integer.MAX_VALUE);
        INSTABILITY_DECAY_AMOUNT = builder.comment("Instability decay amount.").defineInRange("instabilityDecayAmount", 1, 0, 100);
        MAX_INSTABILITY = builder.comment("Max instability.").defineInRange("maxInstability", 100, 1, 1000);
        LEVEL1_THRESHOLD = builder.comment("Level 1 instability threshold.").defineInRange("level1Threshold", 15, 0, 1000);
        LEVEL2_THRESHOLD = builder.comment("Level 2 instability threshold.").defineInRange("level2Threshold", 30, 0, 1000);
        LEVEL3_THRESHOLD = builder.comment("Level 3 instability threshold.").defineInRange("level3Threshold", 50, 0, 1000);
        LEVEL4_THRESHOLD = builder.comment("Level 4 instability threshold.").defineInRange("level4Threshold", 75, 0, 1000);
        LEVEL5_THRESHOLD = builder.comment("Level 5 instability threshold.").defineInRange("level5Threshold", 95, 0, 1000);
        builder.pop();

        builder.push("balance");
        DAMAGED_FAILURE_CHANCE = builder.comment("Damaged failure chance.").defineInRange("damagedFailureChance", 0.20, 0.0, 1.0);
        STABILIZED_FAILURE_CHANCE = builder.comment("Stabilized failure chance.").defineInRange("stabilizedFailureChance", 0.06, 0.0, 1.0);
        OVERCLOCKED_FAILURE_CHANCE = builder.comment("Overclocked failure chance.").defineInRange("overclockedFailureChance", 0.18, 0.0, 1.0);
        CONTAINED_FAILURE_CHANCE = builder.comment("Contained failure chance.").defineInRange("containedFailureChance", 0.03, 0.0, 1.0);
        CORRUPTED_FAILURE_CHANCE = builder.comment("Corrupted failure chance.").defineInRange("corruptedFailureChance", 0.30, 0.0, 1.0);
        LEVEL3_INSTABILITY_FAILURE_BONUS = builder.comment("Level 3 instability failure bonus.").defineInRange("level3InstabilityFailureBonus", 0.08, 0.0, 1.0);
        LEVEL4_INSTABILITY_FAILURE_BONUS = builder.comment("Level 4 instability failure bonus.").defineInRange("level4InstabilityFailureBonus", 0.15, 0.0, 1.0);
        LEVEL5_INSTABILITY_FAILURE_BONUS = builder.comment("Level 5 instability failure bonus.").defineInRange("level5InstabilityFailureBonus", 0.25, 0.0, 1.0);
        builder.pop();

        SPEC = builder.build();
    }

    private RelicTechConfig() {}
}
