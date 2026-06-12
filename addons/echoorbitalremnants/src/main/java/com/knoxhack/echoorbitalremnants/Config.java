package com.knoxhack.echoorbitalremnants;

import com.echoplatform.echocore.api.config.EchoConfigCategory;
import com.echoplatform.echocore.api.config.EchoConfigEntry;
import com.echoplatform.echocore.api.config.EchoConfigModule;
import com.echoplatform.echocore.api.config.EchoConfigProvider;
import com.echoplatform.echocore.api.config.EchoConfigRegistry;
import com.echoplatform.echocore.api.config.EchoConfigSide;
import java.util.List;
import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public class Config {
    private static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();
    public static final int DEFAULT_HAZARD_DRAIN_MULTIPLIER = 75;
    public static final int DEFAULT_ORBITAL_EVENT_FREQUENCY = 3000;
    public static final int DEFAULT_MACHINE_BASE_DURATION = 140;
    public static final int DEFAULT_MACHINE_CHARGE_REGEN_TICKS = 16;

    public static final EchoNativeConfigSpec.BooleanValue ADVENTURE_LEAN_DEFAULTS = BUILDER
            .comment("Keeps orbital survival tense but recoverable.")
            .define("adventureLeanDefaults", true);

    public static final EchoNativeConfigSpec.EnumValue<DifficultyPreset> DIFFICULTY_PRESET = BUILDER
            .comment("Release v1 tuning preset. ADVENTURE keeps hazards readable; HARD makes orbit less forgiving.")
            .defineEnum("difficultyPreset", DifficultyPreset.ADVENTURE);

    public static final EchoNativeConfigSpec.IntValue OXYGEN_DRAIN_TICKS = BUILDER
            .comment("Ticks between oxygen loss while exposed to vacuum.")
            .defineInRange("survival.oxygenDrainTicks", 40, 10, 400);

    public static final EchoNativeConfigSpec.IntValue RADIATION_GAIN_TICKS = BUILDER
            .comment("Ticks between radiation increases in orbital or Nexus exposure.")
            .defineInRange("survival.radiationGainTicks", 80, 20, 800);

    public static final EchoNativeConfigSpec.IntValue VACUUM_DAMAGE_TICKS = BUILDER
            .comment("Ticks between suffocation/pressure damage after oxygen or suit pressure fails.")
            .defineInRange("survival.vacuumDamageTicks", 80, 20, 400);

    public static final EchoNativeConfigSpec.IntValue HAZARD_DRAIN_MULTIPLIER = BUILDER
            .comment("Route pacing percentage for route hazard oxygen, pressure, and radiation drain.")
            .defineInRange("balance.hazardDrainMultiplier", DEFAULT_HAZARD_DRAIN_MULTIPLIER, 25, 250);

    public static final EchoNativeConfigSpec.IntValue ARRIVAL_CACHE_SUPPORT_MULTIPLIER = BUILDER
            .comment("Balance tuning multiplier for route arrival cache survival support items.")
            .defineInRange("balance.arrivalCacheSupportMultiplier", 2, 1, 5);

    public static final EchoNativeConfigSpec.IntValue DEEP_SITE_THREAT_CHANCE = BUILDER
            .comment("Percent chance that a dense route feature, including Saturn/Titan sites, spawns an ambient threat when checked.")
            .defineInRange("balance.deepSiteThreatChance", 50, 0, 100);

    public static final EchoNativeConfigSpec.IntValue ORBITAL_ALTITUDE = BUILDER
            .comment("Overworld altitude treated as low orbit by the current orbital progression loop.")
            .defineInRange("launch.orbitalAltitude", 296, 256, 319);

    public static final EchoNativeConfigSpec.BooleanValue REQUIRE_FULL_LAUNCH_READINESS = BUILDER
            .comment("If true, the emergency rocket requires launch platform, suit, fuel, oxygen, and navigation parts.")
            .define("launch.requireFullLaunchReadiness", true);

    public static final EchoNativeConfigSpec.IntValue ORBITAL_EVENT_FREQUENCY = BUILDER
            .comment("Average ticks between ambient orbital event warnings; tuned for integrated route pacing.")
            .defineInRange("events.orbitalEventFrequency", DEFAULT_ORBITAL_EVENT_FREQUENCY, 200, 24000);

    public static final EchoNativeConfigSpec.BooleanValue FEATURE_THREATS_ENABLED = BUILDER
            .comment("If true, dense route features may spawn ambient orbital threats.")
            .define("events.featureThreatsEnabled", true);

    public static final EchoNativeConfigSpec.IntValue ROUTE_FEATURE_DENSITY = BUILDER
            .comment("Controls repeatable deep-site density in route dimensions. Higher values place sites more often.")
            .defineInRange("worldgen.routeFeatureDensity", 3, 1, 5);

    public static final EchoNativeConfigSpec.BooleanValue DEEP_SITE_CACHES_ENABLED = BUILDER
            .comment("If true, generated deep route sites include fixed survival caches.")
            .define("worldgen.deepSiteCachesEnabled", true);

    public static final EchoNativeConfigSpec.IntValue MACHINE_BASE_DURATION = BUILDER
            .comment("Base processing duration in ticks for orbital machines. Recipes may override this.")
            .defineInRange("machines.baseDuration", DEFAULT_MACHINE_BASE_DURATION, 20, 2400);

    public static final EchoNativeConfigSpec.IntValue MACHINE_CHARGE_REGEN_TICKS = BUILDER
            .comment("Ticks between one point of passive machine system charge regeneration.")
            .defineInRange("machines.chargeRegenTicks", DEFAULT_MACHINE_CHARGE_REGEN_TICKS, 1, 400);

    public static final EchoNativeConfigSpec.IntValue MACHINE_MAX_CHARGE = BUILDER
            .comment("Maximum internal system charge stored by one machine.")
            .defineInRange("machines.maxCharge", 100, 10, 1000);

    public static final EchoNativeConfigSpec.BooleanValue DIMENSION_UNLOCKS_ENABLED = BUILDER
            .comment("Controls whether later route unlock flags are enforced by ECHO Terminal progression.")
            .define("progression.dimensionUnlocksEnabled", true);

    public static final EchoNativeConfigSpec.BooleanValue MID_GAME_OBJECTIVES_ENABLED = BUILDER
            .comment("If true, Orbit, Moon, Mars, Europa, Saturn, and Titan require route objective chains before the next route opens.")
            .define("progression.midGameObjectivesEnabled", true);

    static final EchoNativeConfigSpec SPEC = BUILDER.build();

    public static void registerEchoConfig() {
        EchoConfigRegistry.register(EchoConfigProvider.of(EchoOrbitalRemnants.MODID, () -> new EchoConfigModule(
                EchoOrbitalRemnants.MODID,
                "Orbital Remnants",
                List.of(
                        new EchoConfigCategory("balance", "Balance", List.of(
                                EchoConfigEntry.booleanSpec("adventure_defaults", "Adventure Defaults",
                                        "Keep orbital survival tense but recoverable.",
                                        EchoConfigSide.COMMON, ADVENTURE_LEAN_DEFAULTS, true, false, false),
                                EchoConfigEntry.enumSpec("difficulty_preset", "Difficulty Preset",
                                        "Release route tuning preset.",
                                        EchoConfigSide.COMMON, DIFFICULTY_PRESET, DifficultyPreset.class,
                                        true, false, false),
                                EchoConfigEntry.intSpec("hazard_drain_multiplier", "Hazard Drain Multiplier",
                                        "Route hazard oxygen, pressure, and radiation drain percentage.",
                                        EchoConfigSide.COMMON, HAZARD_DRAIN_MULTIPLIER, 25, 250,
                                        true, false, false),
                                EchoConfigEntry.intSpec("arrival_cache_support", "Arrival Cache Support",
                                        "Multiplier for route arrival cache support items.",
                                        EchoConfigSide.COMMON, ARRIVAL_CACHE_SUPPORT_MULTIPLIER, 1, 5,
                                        true, false, false))),
                        new EchoConfigCategory("survival", "Survival", List.of(
                                EchoConfigEntry.intSpec("oxygen_drain_ticks", "Oxygen Drain Ticks",
                                        "Ticks between oxygen loss while exposed to vacuum.",
                                        EchoConfigSide.COMMON, OXYGEN_DRAIN_TICKS, 10, 400,
                                        true, false, false),
                                EchoConfigEntry.intSpec("radiation_gain_ticks", "Radiation Gain Ticks",
                                        "Ticks between radiation increases in orbital or Nexus exposure.",
                                        EchoConfigSide.COMMON, RADIATION_GAIN_TICKS, 20, 800,
                                        true, false, false),
                                EchoConfigEntry.intSpec("vacuum_damage_ticks", "Vacuum Damage Ticks",
                                        "Ticks between pressure damage after oxygen or suit pressure fails.",
                                        EchoConfigSide.COMMON, VACUUM_DAMAGE_TICKS, 20, 400,
                                        true, false, false))),
                        new EchoConfigCategory("launch", "Launch", List.of(
                                EchoConfigEntry.intSpec("orbital_altitude", "Orbital Altitude",
                                        "Overworld altitude treated as low orbit.",
                                        EchoConfigSide.COMMON, ORBITAL_ALTITUDE, 256, 319,
                                        true, false, false),
                                EchoConfigEntry.booleanSpec("full_launch_readiness", "Full Launch Readiness",
                                        "Require platform, suit, fuel, oxygen, and navigation parts.",
                                        EchoConfigSide.COMMON, REQUIRE_FULL_LAUNCH_READINESS, true, false, false))),
                        new EchoConfigCategory("events", "Events", List.of(
                                EchoConfigEntry.intSpec("orbital_event_frequency", "Orbital Event Frequency",
                                        "Average ticks between ambient orbital event warnings.",
                                        EchoConfigSide.COMMON, ORBITAL_EVENT_FREQUENCY, 200, 24000,
                                        true, false, false),
                                EchoConfigEntry.booleanSpec("feature_threats", "Feature Threats",
                                        "Dense route features may spawn ambient orbital threats.",
                                        EchoConfigSide.COMMON, FEATURE_THREATS_ENABLED, true, false, false),
                                EchoConfigEntry.intSpec("deep_site_threat_chance", "Deep Site Threat Chance",
                                        "Percent chance for dense route features to spawn threats.",
                                        EchoConfigSide.COMMON, DEEP_SITE_THREAT_CHANCE, 0, 100,
                                        true, false, false))),
                        new EchoConfigCategory("worldgen", "Worldgen", List.of(
                                EchoConfigEntry.intSpec("route_feature_density", "Route Feature Density",
                                        "Repeatable deep-site density in route dimensions.",
                                        EchoConfigSide.COMMON, ROUTE_FEATURE_DENSITY, 1, 5,
                                        true, true, true),
                                EchoConfigEntry.booleanSpec("deep_site_caches", "Deep Site Caches",
                                        "Generated deep route sites include fixed survival caches.",
                                        EchoConfigSide.COMMON, DEEP_SITE_CACHES_ENABLED, true, true, true))),
                        new EchoConfigCategory("machines", "Machines", List.of(
                                EchoConfigEntry.intSpec("machine_base_duration", "Machine Base Duration",
                                        "Base processing duration in ticks for orbital machines.",
                                        EchoConfigSide.COMMON, MACHINE_BASE_DURATION, 20, 2400,
                                        true, false, false),
                                EchoConfigEntry.intSpec("charge_regen_ticks", "Charge Regen Ticks",
                                        "Ticks between passive system charge regeneration.",
                                        EchoConfigSide.COMMON, MACHINE_CHARGE_REGEN_TICKS, 1, 400,
                                        true, false, false),
                                EchoConfigEntry.intSpec("machine_max_charge", "Machine Max Charge",
                                        "Maximum internal system charge stored by one machine.",
                                        EchoConfigSide.COMMON, MACHINE_MAX_CHARGE, 10, 1000,
                                        true, false, false))),
                        new EchoConfigCategory("progression", "Progression", List.of(
                                EchoConfigEntry.booleanSpec("dimension_unlocks", "Dimension Unlocks",
                                        "Enforce later route unlock flags through terminal progression.",
                                        EchoConfigSide.COMMON, DIMENSION_UNLOCKS_ENABLED, true, false, false),
                                EchoConfigEntry.booleanSpec("mid_game_objectives", "Mid-Game Objectives",
                                        "Require objective chains before the next route opens.",
                                        EchoConfigSide.COMMON, MID_GAME_OBJECTIVES_ENABLED, true, false, false)))))));
    }

    public static int tunedMachineDuration(int recipeDuration) {
        int scaled = Math.max(1, recipeDuration) * MACHINE_BASE_DURATION.get() / 160;
        return switch (DIFFICULTY_PRESET.get()) {
            case CASUAL -> Math.max(1, scaled * 3 / 4);
            case HARD -> Math.max(1, scaled * 5 / 4);
            case ADVENTURE -> Math.max(1, scaled);
        };
    }

    public static int tunedMachineChargeRegenTicks() {
        int configured = MACHINE_CHARGE_REGEN_TICKS.get();
        return switch (DIFFICULTY_PRESET.get()) {
            case CASUAL -> Math.max(1, configured * 3 / 4);
            case HARD -> Math.max(1, configured * 5 / 4);
            case ADVENTURE -> Math.max(1, configured);
        };
    }

    public static int tunedSurvivalInterval(EchoNativeConfigSpec.IntValue configured) {
        int ticks = configured.get();
        return switch (DIFFICULTY_PRESET.get()) {
            case CASUAL -> Math.max(1, ticks * 3 / 2);
            case HARD -> Math.max(1, ticks * 2 / 3);
            case ADVENTURE -> Math.max(1, ticks);
        };
    }

    public static int tunedHazardDrain(int amount) {
        int scaled = Math.max(0, amount) * HAZARD_DRAIN_MULTIPLIER.get() / 100;
        scaled = amount > 0 ? Math.max(1, scaled) : 0;
        return switch (DIFFICULTY_PRESET.get()) {
            case CASUAL -> Math.max(1, scaled * 3 / 4);
            case HARD -> Math.max(1, scaled * 5 / 4);
            case ADVENTURE -> scaled;
        };
    }

    public static int tunedOrbitalEventFrequency() {
        int ticks = ORBITAL_EVENT_FREQUENCY.get();
        return switch (DIFFICULTY_PRESET.get()) {
            case CASUAL -> Math.max(1, ticks * 3 / 2);
            case HARD -> Math.max(1, ticks * 2 / 3);
            case ADVENTURE -> Math.max(1, ticks);
        };
    }

    public enum DifficultyPreset {
        CASUAL,
        ADVENTURE,
        HARD
    }
}

