package com.knoxhack.echoashfallprotocol;

import com.knoxhack.echoashfallprotocol.gameplay.DifficultyProfile;
import com.knoxhack.echoashfallprotocol.worldgen.StructureType;
import com.knoxhack.echocore.api.config.EchoConfigCategory;
import com.knoxhack.echocore.api.config.EchoConfigEntry;
import com.knoxhack.echocore.api.config.EchoConfigModule;
import com.knoxhack.echocore.api.config.EchoConfigProvider;
import com.knoxhack.echocore.api.config.EchoConfigRegistry;
import com.knoxhack.echocore.api.config.EchoConfigSide;
import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Configuration for ECHO: Ashfall Protocol.
 * All gameplay-affecting values are configurable here.
 */
public class Config {
    private static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();
    private static final Map<StructureType, EchoNativeConfigSpec.BooleanValue> STRUCTURE_ENABLED = new EnumMap<>(StructureType.class);
    private static final Map<StructureType, EchoNativeConfigSpec.IntValue> STRUCTURE_SPACING = new EnumMap<>(StructureType.class);
    private static final Map<StructureType, EchoNativeConfigSpec.IntValue> STRUCTURE_SEPARATION = new EnumMap<>(StructureType.class);
    private static final Map<String, EchoNativeConfigSpec.BooleanValue> BIOME_CONTENT_ENABLED = new HashMap<>();

    // === SURVIVAL ===
    static {
        BUILDER.push("survival");
    }

    public static final EchoNativeConfigSpec.IntValue AIR_FILTER_DECAY_RATE = BUILDER
            .comment("Base filter decay rate per second while inside toxic-air hazards (default: 1)")
            .defineInRange("airFilterDecayRate", 1, 0, 10);

    public static final EchoNativeConfigSpec.BooleanValue GLOBAL_TOXIC_AIR = BUILDER
            .comment("If true, toxic air is active everywhere after grace. If false, toxic air is limited to tagged hazard sources (default: false)")
            .define("globalToxicAir", false);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_BIOME_HAZARDS = BUILDER
            .comment("Enable biome-profile hazard zones for toxic, radiation, cryogenic, and Nexus biomes (default: true)")
            .define("enableBiomeHazards", true);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_RADIATION_STORMS = BUILDER
            .comment("Enable radiation storm exposure during environmental RADIATION_STORM events (default: true)")
            .define("enableRadiationStorms", true);

    public static final EchoNativeConfigSpec.DoubleValue TOXIC_HAZARD_FILTER_MULTIPLIER = BUILDER
            .comment("Multiplier for filter drain while inside toxic hazard zones (default: 1.0)")
            .defineInRange("toxicHazardFilterMultiplier", 1.0, 0.0, 5.0);

    public static final EchoNativeConfigSpec.DoubleValue RADIATION_ACCUMULATION_RATE = BUILDER
            .comment("Radiation gained per second near radiation blocks (default: 1.0)")
            .defineInRange("radiationAccumulationRate", 1.0, 0.0, 10.0);

    public static final EchoNativeConfigSpec.DoubleValue RADIATION_HAZARD_MULTIPLIER = BUILDER
            .comment("Multiplier for radiation gained from biome/source/storm hazard intensity (default: 1.0)")
            .defineInRange("radiationHazardMultiplier", 1.0, 0.0, 5.0);

    public static final EchoNativeConfigSpec.DoubleValue RADIATION_DECAY_RATE = BUILDER
            .comment("Radiation lost per second away from radiation sources (default: 0.2)")
            .defineInRange("radiationDecayRate", 0.2, 0.0, 5.0);

    public static final EchoNativeConfigSpec.DoubleValue CRYO_COLD_LOSS_MULTIPLIER = BUILDER
            .comment("Multiplier for body-temperature loss in cryogenic hazard zones (default: 1.0)")
            .defineInRange("cryoColdLossMultiplier", 1.0, 0.0, 5.0);

    public static final EchoNativeConfigSpec.DoubleValue NEXUS_HAZARD_MULTIPLIER = BUILDER
            .comment("Multiplier for Nexus Scar anomaly pressure (default: 1.25)")
            .defineInRange("nexusHazardMultiplier", 1.25, 0.0, 5.0);

    public static final EchoNativeConfigSpec.IntValue SCRUBBER_SAFE_ZONE_RADIUS = BUILDER
            .comment("Atmospheric Scrubber safe-zone radius in blocks (default: 16)")
            .defineInRange("scrubberSafeZoneRadius", 16, 4, 64);

    public static final EchoNativeConfigSpec.IntValue HYDRATION_DECAY_RATE = BUILDER
            .comment("Hydration lost each decay interval (default: 1)")
            .defineInRange("hydrationDecayRate", 1, 0, 5);

    public static final EchoNativeConfigSpec.IntValue HYDRATION_DECAY_INTERVAL_TICKS = BUILDER
            .comment("Ticks between hydration decay pulses (default: 600 = 30 seconds)")
            .defineInRange("hydrationDecayIntervalTicks", 600, 20, 72000);

    public static final EchoNativeConfigSpec.IntValue HYDRATION_WARNING_LEVEL = BUILDER
            .comment("Hydration level where HUD/ECHO warnings begin (default: 40)")
            .defineInRange("hydrationWarningLevel", 40, 0, 100);

    public static final EchoNativeConfigSpec.IntValue HYDRATION_PENALTY_LEVEL = BUILDER
            .comment("Hydration level where movement/mining penalties begin (default: 15)")
            .defineInRange("hydrationPenaltyLevel", 15, 0, 100);

    public static final EchoNativeConfigSpec.IntValue TOXIC_AIR_WARNING_TICKS = BUILDER
            .comment("Ticks of unprotected toxic-air warning before damage starts (default: 100 = 5 seconds)")
            .defineInRange("toxicAirWarningTicks", 100, 0, 1200);

    public static final EchoNativeConfigSpec.IntValue HAZARD_WARNING_COOLDOWN_TICKS = BUILDER
            .comment("Ticks between repeated ECHO hazard warning messages (default: 240 = 12 seconds)")
            .defineInRange("hazardWarningCooldownTicks", 240, 40, 6000);

    public static final EchoNativeConfigSpec.IntValue NEW_PLAYER_GRACE_TICKS = BUILDER
            .comment("Ticks of new-player protection from survival-system hazards (default: 12000 = 10 minutes)")
            .defineInRange("newPlayerGraceTicks", 12000, 0, 72000);

    static {
        BUILDER.pop();
    }

    // === MACHINES ===
    static {
        BUILDER.push("machines");
    }

    public static final EchoNativeConfigSpec.IntValue RECYCLER_PROCESS_TIME = BUILDER
            .comment("Ticks to process one item in Hand Recycler (default: 80)")
            .defineInRange("recyclerProcessTime", 80, 20, 600);

    public static final EchoNativeConfigSpec.DoubleValue GENERATOR_FAILURE_CHANCE = BUILDER
            .comment("Chance per tick of generator failure when running (default: 0.0005)")
            .defineInRange("generatorFailureChance", 0.0005, 0.0, 0.1);

    static {
        BUILDER.pop();
    }

    // === SURVIVAL MUTATIONS ===
    static {
        BUILDER.push("mutations");
    }

    public static final EchoNativeConfigSpec.DoubleValue MUTATION_THRESHOLD = BUILDER
            .comment("Radiation level above which mutations can occur (default: 50.0)")
            .defineInRange("mutationThreshold", 50.0, 10.0, 100.0);

    public static final EchoNativeConfigSpec.DoubleValue MUTATION_SEVERE_THRESHOLD = BUILDER
            .comment("Radiation level required for expedition mutation rolls (default: 80.0)")
            .defineInRange("mutationSevereThreshold", 80.0, 10.0, 100.0);

    public static final EchoNativeConfigSpec.IntValue MUTATION_ROLL_COOLDOWN_TICKS = BUILDER
            .comment("Minimum ticks between radiation mutation rolls (default: 12000 = 10 minutes)")
            .defineInRange("mutationRollCooldownTicks", 12000, 200, 72000);

    static {
        BUILDER.pop();
    }

    // === SMART EVENTS ===
    static {
        BUILDER.push("events");
    }

    public static final EchoNativeConfigSpec.DoubleValue EVENT_REACTIVITY_MULTIPLIER = BUILDER
            .comment("Multiplier for smart event trigger chances (default: 1.0)")
            .defineInRange("eventReactivityMultiplier", 1.0, 0.0, 5.0);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_ASH_STORMS = BUILDER
            .comment("Enable environmental ASH_STORM events (default: true)")
            .define("enableAshStorms", true);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_CRYO_FRONTS = BUILDER
            .comment("Enable environmental CRYO_FRONT events (default: true)")
            .define("enableCryoFronts", true);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_NEXUS_SURGES = BUILDER
            .comment("Enable environmental NEXUS_SURGE events once Nexus instability is present (default: true)")
            .define("enableNexusSurges", true);

    static {
        BUILDER.pop();
    }

    // === ECHO-7 ===
    static {
        BUILDER.push("echo");
    }

    public static final EchoNativeConfigSpec.BooleanValue ECHO_ENABLED = BUILDER
            .comment("Enable ECHO-7 AI guide messages")
            .define("echoEnabled", true);

    public static final EchoNativeConfigSpec.IntValue ECHO_MESSAGE_COOLDOWN = BUILDER
            .comment("Minimum ticks between ECHO-7 context messages (default: 600)")
            .defineInRange("echoMessageCooldown", 600, 100, 6000);

    static {
        BUILDER.pop();
    }

    // === CLIENT PRESENTATION ===
    static {
        BUILDER.push("clientPresentation");
    }

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_ECHO_MAIN_MENU = BUILDER
            .comment("Replace the vanilla title screen and skin pre-game screens with the ECHO terminal shell.")
            .define("enableEchoMainMenu", true);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_ECHO_LOADING_SCREEN = BUILDER
            .comment("Replace the vanilla resource reload loading overlay with the ECHO terminal shell.")
            .define("enableEchoLoadingScreen", true);

    public static final EchoNativeConfigSpec.BooleanValue ECHO_SCANLINE_ANIMATION = BUILDER
            .comment("Animate ECHO terminal and title-screen scanline drift.")
            .define("echoScanlineAnimation", true);

    public static final EchoNativeConfigSpec.DoubleValue ECHO_SCANLINE_INTENSITY = BUILDER
            .comment("Visual intensity multiplier for ECHO scanlines. 0 disables scanlines; 1 keeps the default.")
            .defineInRange("echoScanlineIntensity", 0.0, 0.0, 2.0);

    public static final EchoNativeConfigSpec.DoubleValue HUD_WARNING_INTENSITY = BUILDER
            .comment("Visual intensity multiplier for HUD warning flashes and mutation glitch overlays.")
            .defineInRange("hudWarningIntensity", 1.0, 0.0, 2.0);

    public static final EchoNativeConfigSpec.DoubleValue WEATHER_VISUAL_INTENSITY = BUILDER
            .comment("Visual intensity multiplier for ECHO environmental weather overlays. 0 disables the overlay.")
            .defineInRange("weatherVisualIntensity", 1.0, 0.0, 2.0);

    public static final EchoNativeConfigSpec.DoubleValue WEATHER_PARTICLE_DENSITY = BUILDER
            .comment("Client-side density multiplier for ECHO environmental weather particles. 0 disables extra particles.")
            .defineInRange("weatherParticleDensity", 0.35, 0.0, 3.0);

    public static final EchoNativeConfigSpec.BooleanValue ORBITAL_EVENT_VISUALS = BUILDER
            .comment("Show ECHO-7 visual feedback for Orbital Remnants route events when the addon is loaded.")
            .define("orbitalEventVisuals", true);

    public static final EchoNativeConfigSpec.BooleanValue TERMINAL_ANIMATION = BUILDER
            .comment("Enable animated terminal flicker, glow, and refresh-line effects.")
            .define("terminalAnimation", true);

    public static final EchoNativeConfigSpec.BooleanValue VERBOSE_TOOLTIPS = BUILDER
            .comment("Show extra survival and progression guidance in ECHO tooltips.")
            .define("verboseTooltips", true);

    static {
        BUILDER.pop();
    }

    // === FAST TRAVEL ===
    static {
        BUILDER.push("fastTravel");
    }

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_FAST_TRAVEL = BUILDER
            .comment("Enable Radio Network fast-travel system (default: true)")
            .define("enableFastTravel", true);

    public static final EchoNativeConfigSpec.IntValue FAST_TRAVEL_COOLDOWN_TICKS = BUILDER
            .comment("Ticks between fast-travel uses (default: 6000 = 5 minutes)")
            .defineInRange("fastTravelCooldownTicks", 6000, 200, 72000);

    static {
        BUILDER.pop();
    }

    // === LOOT AND PROGRESSION ===
    static {
        BUILDER.push("loot");
    }

    public static final EchoNativeConfigSpec.DoubleValue SCHEMATIC_DROP_RATE = BUILDER
            .comment("Base chance for schematic fragment drops (default: 0.05 = 5%)")
            .defineInRange("schematicDropRate", 0.05, 0.0, 0.5);

    static {
        BUILDER.pop();
    }

    // === STRUCTURES ===
    static {
        BUILDER.push("structures");
    }

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_PROCEDURAL_STRUCTURES = BUILDER
            .comment("Enable Java-driven procedural structure generation. Datapack jigsaw POIs are controlled by worldgen JSON, not this value.")
            .define("enableProceduralStructures", true);

    public static final EchoNativeConfigSpec.DoubleValue STRUCTURE_GLOBAL_DENSITY_MULTIPLIER = BUILDER
            .comment("Multiplier for Java-driven procedural structure density. 1.0 keeps default spacing; 2.0 is roughly twice as dense; 0.5 is roughly half as dense.")
            .defineInRange("globalDensityMultiplier", 1.0, 0.05, 10.0);

    static {
        defineStructureConfig(StructureType.DROP_POD, 20, 5);
        defineStructureConfig(StructureType.BIO_LAB, 28, 7);
        defineStructureConfig(StructureType.DATA_CENTER, 36, 9);
        defineStructureConfig(StructureType.MILITARY_VAULT, 56, 14);
        defineStructureConfig(StructureType.REACTOR_RUIN, 72, 18);
        defineStructureConfig(StructureType.SUBWAY_STATION, 32, 8);
        defineStructureConfig(StructureType.SEWER_JUNCTION, 26, 6);
        defineStructureConfig(StructureType.TRAIN_YARD, 40, 10);
        defineStructureConfig(StructureType.SATELLITE_ARRAY, 48, 12);
        defineStructureConfig(StructureType.RADIO_TOWER, 44, 11);
        defineStructureConfig(StructureType.RELAY_STATION, 48, 12);
        defineStructureConfig(StructureType.OBSERVATION_POST, 60, 15);
        defineStructureConfig(StructureType.RADWARDEN_OUTPOST, 50, 12);
        defineStructureConfig(StructureType.CRASHBREAK_SALVAGE_YARD, 42, 10);
        defineStructureConfig(StructureType.SPOREBOUND_SANCTUM, 46, 11);
        defineStructureConfig(StructureType.CRYOGENIC_RUINS, 54, 13);
        defineStructureConfig(StructureType.DERELICT_WORKSHOP, 38, 9);
        defineStructureConfig(StructureType.ABANDONED_MINE, 58, 14);
        BUILDER.pop();
    }

    // === BIOMES ===
    static {
        BUILDER.push("biomes");
        BUILDER.comment("These toggles control Java-driven biome content checks only. They do not remove biomes from datapack terrain generation or existing worlds.");
        defineBiomeContentToggle("the_wasteland", "theWasteland");
        defineBiomeContentToggle("ruined_plains", "ruinedPlains");
        defineBiomeContentToggle("crash_zone_wasteland", "crashZoneWasteland");
        defineBiomeContentToggle("industrial_ruins", "industrialRuins");
        defineBiomeContentToggle("toxic_swamp", "toxicSwamp");
        defineBiomeContentToggle("ruined_cityscape", "ruinedCityscape");
        defineBiomeContentToggle("radiation_zone", "radiationZone");
        defineBiomeContentToggle("cryogenic_ruins", "cryogenicRuins");
        defineBiomeContentToggle("nexus_scar", "nexusScar");
        BUILDER.pop();
    }

    // === EXPLORATION 1.1: LEGACY POI SPAWN RATES ===
    static {
        BUILDER.push("legacyPoi");
    }

    public static final EchoNativeConfigSpec.IntValue FACTION_HUB_SPACING = BUILDER
            .comment("Legacy Java POI spacing reference. Active jigsaw POI density is controlled by data/echoashfallprotocol/worldgen/structure_set JSON.")
            .defineInRange("factionHubSpacing", 40, 20, 100);

    public static final EchoNativeConfigSpec.IntValue WORLD_POI_SPACING = BUILDER
            .comment("Legacy Java POI spacing reference. Active jigsaw POI density is controlled by data/echoashfallprotocol/worldgen/structure_set JSON.")
            .defineInRange("worldPoiSpacing", 25, 10, 60);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_CRYOGENIC_RUINS = BUILDER
            .comment("Legacy compatibility toggle. Use biomes.cryogenicRuins for Java-driven biome content; datapack terrain generation is not removed by this value.")
            .define("enableCryogenicRuins", true);

    static {
        BUILDER.pop();
    }

    // === COMPANION DRONE ===
    static {
        BUILDER.push("drone");
    }

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_COMPANION_DRONE_UTILITY = BUILDER
            .comment("Enable ECHO Field Assistant utility behavior for the companion drone (default: true)")
            .define("enableCompanionDroneUtility", true);

    public static final EchoNativeConfigSpec.BooleanValue ALLOW_ONE_DRONE_PER_PLAYER = BUILDER
            .comment("Keep one active companion drone per player and remove duplicate owned drones on relink (default: true)")
            .define("allowOneDronePerPlayer", true);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_DRONE_BATTERY = BUILDER
            .comment("Enable generous companion drone battery drain/recharge behavior (default: true)")
            .define("enableBattery", true);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_DRONE_SIGNAL = BUILDER
            .comment("Enable distance-based companion drone signal quality (default: true)")
            .define("enableSignal", true);

    public static final EchoNativeConfigSpec.ConfigValue<String> DRONE_MESSAGE_VERBOSITY = BUILDER
            .comment("Drone warning verbosity: SILENT, MINIMAL, NORMAL, or TACTICAL (default: NORMAL)")
            .define("droneMessageVerbosity", "NORMAL");

    public static final EchoNativeConfigSpec.IntValue DRONE_SCAN_RADIUS_BASE = BUILDER
            .comment("Base companion drone scan radius in blocks (default: 16)")
            .defineInRange("scanRadiusBase", 16, 4, 48);

    public static final EchoNativeConfigSpec.IntValue DRONE_SCAN_RADIUS_SENSOR_LENS = BUILDER
            .comment("Companion drone scan radius with the Sensor Lens upgrade (default: 24)")
            .defineInRange("scanRadiusSensorLens", 24, 4, 64);

    public static final EchoNativeConfigSpec.IntValue DRONE_SCAN_COOLDOWN_TICKS = BUILDER
            .comment("Ticks between manual area scans (default: 200 = 10 seconds)")
            .defineInRange("scanCooldownTicks", 200, 20, 72000);

    public static final EchoNativeConfigSpec.IntValue DRONE_SCAN_MAX_BLOCK_CHECKS = BUILDER
            .comment("Maximum loaded block positions checked per scan (default: 8192)")
            .defineInRange("scanMaxBlockChecks", 8192, 128, 65536);

    public static final EchoNativeConfigSpec.IntValue DRONE_SCAN_MAX_RESULTS = BUILDER
            .comment("Maximum scan results summarized per scan (default: 12)")
            .defineInRange("scanMaxResults", 12, 1, 64);

    public static final EchoNativeConfigSpec.IntValue DRONE_SCAN_MAX_MARKERS = BUILDER
            .comment("Maximum temporary client markers created per scan (default: 10)")
            .defineInRange("scanMaxMarkers", 10, 0, 32);

    public static final EchoNativeConfigSpec.IntValue DRONE_MARKER_DURATION_TICKS = BUILDER
            .comment("Lifetime of temporary scan markers in ticks (default: 220 = 11 seconds)")
            .defineInRange("markerDurationTicks", 220, 20, 1200);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_HAZARD_WARNINGS = BUILDER
            .comment("Enable passive companion drone hazard warnings (default: true)")
            .define("enableHazardWarnings", true);

    public static final EchoNativeConfigSpec.IntValue DRONE_WARNING_COOLDOWN_TICKS = BUILDER
            .comment("Ticks between passive warning messages per drone (default: 240 = 12 seconds)")
            .defineInRange("warningCooldownTicks", 240, 40, 72000);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_HOSTILE_WARNINGS = BUILDER
            .comment("Enable passive hostile proximity warnings (default: true)")
            .define("enableHostileWarnings", true);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_DRONE_MISSION_HINTS = BUILDER
            .comment("Enable mission/objective hints from the companion drone when available (default: true)")
            .define("enableMissionHints", true);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_SALVAGE_MODE = BUILDER
            .comment("Enable companion drone salvage assistant mode (default: true)")
            .define("enableSalvageMode", true);

    public static final EchoNativeConfigSpec.IntValue DRONE_SALVAGE_RADIUS = BUILDER
            .comment("Dropped-item salvage search radius in blocks (default: 10)")
            .defineInRange("salvageRadius", 10, 2, 32);

    public static final EchoNativeConfigSpec.IntValue DRONE_SALVAGE_MAX_CARRY_STACKS = BUILDER
            .comment("Maximum item stacks the early companion drone may carry before returning (default: 1)")
            .defineInRange("salvageMaxCarryStacks", 1, 1, 9);

    public static final EchoNativeConfigSpec.IntValue DRONE_PICKUP_COOLDOWN_TICKS = BUILDER
            .comment("Ticks between salvage pickups (default: 40 = 2 seconds)")
            .defineInRange("pickupCooldown", 40, 5, 1200);

    public static final EchoNativeConfigSpec.BooleanValue DRONE_ALLOW_NON_SCRAP_PICKUP = BUILDER
            .comment("Allow salvage mode to collect non-tagged dropped items when no scrap tag is present (default: false)")
            .define("allowNonScrapPickup", false);

    public static final EchoNativeConfigSpec.IntValue DRONE_SCOUT_MAX_DISTANCE = BUILDER
            .comment("Maximum Scout Ahead distance in blocks (default: 24)")
            .defineInRange("scoutMaxDistance", 24, 4, 64);

    public static final EchoNativeConfigSpec.IntValue DRONE_SCOUT_SCAN_RADIUS = BUILDER
            .comment("Scan radius used at the Scout Ahead target point (default: 16)")
            .defineInRange("scoutScanRadius", 16, 4, 48);

    public static final EchoNativeConfigSpec.IntValue DRONE_SCOUT_COOLDOWN_TICKS = BUILDER
            .comment("Ticks between Scout Ahead scans (default: 300 = 15 seconds)")
            .defineInRange("scoutCooldownTicks", 300, 20, 72000);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_DRONE_DEBUG_COMMANDS = BUILDER
            .comment("Enable extra /echo drone debug commands (default: false)")
            .define("enableDroneDebugCommands", false);

    public static final EchoNativeConfigSpec.BooleanValue LOG_DRONE_STATE_CHANGES = BUILDER
            .comment("Log companion drone spawn/link/recover/state transitions (default: false)")
            .define("logDroneStateChanges", false);

    static {
        BUILDER.pop();
    }

    // === DIFFICULTY PROFILE ===
    static {
        BUILDER.push("difficulty");
    }

    public static final EchoNativeConfigSpec.EnumValue<DifficultyProfile> DEFAULT_DIFFICULTY = BUILDER
            .comment("Default difficulty profile for new worlds (gamerule can override per-world)")
            .defineEnum("defaultDifficulty", DifficultyProfile.NORMAL);

    static {
        BUILDER.pop();
    }

    static final EchoNativeConfigSpec SPEC = BUILDER.build();

    public static void registerEchoConfig() {
        List<EchoConfigCategory> categories = List.of(
                new EchoConfigCategory("survival", "Survival", List.of(
                        EchoConfigEntry.booleanSpec("global_toxic_air", "Global Toxic Air",
                                "Make toxic air active everywhere after grace expires.",
                                EchoConfigSide.COMMON, GLOBAL_TOXIC_AIR, true, false, false),
                        EchoConfigEntry.booleanSpec("biome_hazards", "Biome Hazards",
                                "Enable biome-profile toxic, radiation, cryogenic, and Nexus hazards.",
                                EchoConfigSide.COMMON, ENABLE_BIOME_HAZARDS, true, false, false),
                        EchoConfigEntry.booleanSpec("radiation_storms", "Radiation Storms",
                                "Enable radiation exposure during environmental radiation storms.",
                                EchoConfigSide.COMMON, ENABLE_RADIATION_STORMS, true, false, false),
                        EchoConfigEntry.intSpec("new_player_grace", "New Player Grace",
                                "Ticks of new-player protection from survival hazards.",
                                EchoConfigSide.COMMON, NEW_PLAYER_GRACE_TICKS, 0, 72000,
                                true, false, false),
                        EchoConfigEntry.intSpec("scrubber_safe_zone", "Scrubber Safe Zone",
                                "Atmospheric Scrubber safe-zone radius in blocks.",
                                EchoConfigSide.COMMON, SCRUBBER_SAFE_ZONE_RADIUS, 4, 64,
                                true, false, false))),
                new EchoConfigCategory("machines", "Machines", List.of(
                        EchoConfigEntry.intSpec("recycler_process_time", "Recycler Process Time",
                                "Ticks to process one item in the Hand Recycler.",
                                EchoConfigSide.COMMON, RECYCLER_PROCESS_TIME, 20, 600,
                                true, false, false),
                        EchoConfigEntry.doubleSpec("generator_failure_chance", "Generator Failure Chance",
                                "Chance per tick of generator failure while running.",
                                EchoConfigSide.COMMON, GENERATOR_FAILURE_CHANCE, 0.0D, 0.1D,
                                true, false, false))),
                new EchoConfigCategory("echo", "ECHO", List.of(
                        EchoConfigEntry.booleanSpec("echo_enabled", "ECHO Guidance",
                                "Enable ECHO-7 guidance messages.",
                                EchoConfigSide.COMMON, ECHO_ENABLED, true, false, false),
                        EchoConfigEntry.intSpec("echo_message_cooldown", "Message Cooldown",
                                "Ticks between ECHO guidance messages.",
                                EchoConfigSide.COMMON, ECHO_MESSAGE_COOLDOWN, 100, 6000,
                                true, false, false),
                        EchoConfigEntry.booleanSpec("terminal_animation", "Terminal Animation",
                                "Enable terminal animation treatments.",
                                EchoConfigSide.COMMON, TERMINAL_ANIMATION, true, false, false),
                        EchoConfigEntry.booleanSpec("verbose_tooltips", "Verbose Tooltips",
                                "Show extra ECHO details in tooltips.",
                                EchoConfigSide.COMMON, VERBOSE_TOOLTIPS, true, false, false))),
                new EchoConfigCategory("drone", "Companion Drone", List.of(
                        EchoConfigEntry.booleanSpec("drone_utility", "Field Assistant Mode",
                                "Enable companion drone utility behavior.",
                                EchoConfigSide.COMMON, ENABLE_COMPANION_DRONE_UTILITY, true, false, false),
                        EchoConfigEntry.booleanSpec("drone_battery", "Battery",
                                "Enable companion drone battery behavior.",
                                EchoConfigSide.COMMON, ENABLE_DRONE_BATTERY, true, false, false),
                        EchoConfigEntry.booleanSpec("drone_signal", "Signal",
                                "Enable distance-based companion drone signal quality.",
                                EchoConfigSide.COMMON, ENABLE_DRONE_SIGNAL, true, false, false),
                        EchoConfigEntry.intSpec("drone_scan_radius", "Scan Radius",
                                "Base companion drone scan radius.",
                                EchoConfigSide.COMMON, DRONE_SCAN_RADIUS_BASE, 4, 48,
                                true, false, false),
                        EchoConfigEntry.intSpec("drone_scan_cooldown", "Scan Cooldown",
                                "Ticks between manual area scans.",
                                EchoConfigSide.COMMON, DRONE_SCAN_COOLDOWN_TICKS, 20, 72000,
                                true, false, false),
                        EchoConfigEntry.intSpec("drone_marker_duration", "Marker Duration",
                                "Ticks temporary scan markers remain visible.",
                                EchoConfigSide.COMMON, DRONE_MARKER_DURATION_TICKS, 20, 1200,
                                true, false, false),
                        EchoConfigEntry.booleanSpec("drone_warnings", "Hazard Warnings",
                                "Enable passive companion drone hazard warnings.",
                                EchoConfigSide.COMMON, ENABLE_HAZARD_WARNINGS, true, false, false),
                        EchoConfigEntry.booleanSpec("drone_salvage", "Salvage Mode",
                                "Enable companion drone salvage assistant behavior.",
                                EchoConfigSide.COMMON, ENABLE_SALVAGE_MODE, true, false, false),
                        EchoConfigEntry.intSpec("drone_scout_distance", "Scout Distance",
                                "Maximum Scout Ahead distance in blocks.",
                                EchoConfigSide.COMMON, DRONE_SCOUT_MAX_DISTANCE, 4, 64,
                                true, false, false))),
                new EchoConfigCategory("worldgen", "Worldgen", List.of(
                        EchoConfigEntry.booleanSpec("procedural_structures", "Procedural Structures",
                                "Enable Ashfall Java-driven procedural structures.",
                                EchoConfigSide.COMMON, ENABLE_PROCEDURAL_STRUCTURES, true, true, true),
                        EchoConfigEntry.doubleSpec("structure_density", "Structure Density",
                                "Global multiplier for Ashfall structure density.",
                                EchoConfigSide.COMMON, STRUCTURE_GLOBAL_DENSITY_MULTIPLIER, 0.05D, 10.0D,
                                true, true, true),
                        EchoConfigEntry.intSpec("faction_hub_spacing", "Faction Hub Spacing",
                                "Chunk spacing for faction hubs.",
                                EchoConfigSide.COMMON, FACTION_HUB_SPACING, 20, 100,
                                true, true, true),
                        EchoConfigEntry.intSpec("world_poi_spacing", "World POI Spacing",
                                "Chunk spacing for world POIs.",
                                EchoConfigSide.COMMON, WORLD_POI_SPACING, 10, 60,
                                true, true, true))),
                new EchoConfigCategory("difficulty", "Difficulty", List.of(
                        EchoConfigEntry.enumSpec("default_difficulty", "Default Difficulty",
                                "Default difficulty profile for new worlds.",
                                EchoConfigSide.COMMON, DEFAULT_DIFFICULTY, DifficultyProfile.class,
                                true, true, false))));
        EchoConfigRegistry.register(EchoConfigProvider.of(EchoAshfallProtocol.MODID,
                () -> new EchoConfigModule(EchoAshfallProtocol.MODID, "Ashfall Protocol", categories)));
    }

    private static void defineStructureConfig(StructureType type, int defaultSpacing, int defaultSeparation) {
        String sectionName = type.getName();
        BUILDER.push(sectionName);
        STRUCTURE_ENABLED.put(type, BUILDER
                .comment("Enable Java-driven procedural generation for " + sectionName + ".")
                .define("enabled", true));
        STRUCTURE_SPACING.put(type, BUILDER
                .comment("Chunks between generation attempts for " + sectionName + ". Must be greater than separation.")
                .defineInRange("spacing", defaultSpacing, defaultSeparation + 1, 256));
        STRUCTURE_SEPARATION.put(type, BUILDER
                .comment("Minimum chunk separation for " + sectionName + ". Kept below spacing by range and runtime clamping.")
                .defineInRange("separation", defaultSeparation, 1, defaultSpacing - 1));
        BUILDER.pop();
    }

    private static void defineBiomeContentToggle(String biomePath, String configKey) {
        BIOME_CONTENT_ENABLED.put(biomePath, BUILDER
                .comment("Enable Java-driven structures and events that target echoashfallprotocol:" + biomePath + ".")
                .define(configKey, true));
    }

    public static boolean isStructureEnabled(StructureType type) {
        if (!ENABLE_PROCEDURAL_STRUCTURES.get()) {
            return false;
        }
        EchoNativeConfigSpec.BooleanValue value = STRUCTURE_ENABLED.get(type);
        return value == null || value.get();
    }

    public static int getStructureSpacing(StructureType type) {
        EchoNativeConfigSpec.IntValue value = STRUCTURE_SPACING.get(type);
        int configuredSpacing = value != null ? value.get() : 32;
        int densityAdjustedSpacing = Math.max(2, (int) Math.round(configuredSpacing / STRUCTURE_GLOBAL_DENSITY_MULTIPLIER.get()));
        return Math.max(densityAdjustedSpacing, getRawStructureSeparation(type) + 1);
    }

    public static int getStructureSeparation(StructureType type) {
        return Math.min(getRawStructureSeparation(type), getStructureSpacing(type) - 1);
    }

    public static boolean isBiomeContentEnabled(String biomePath) {
        if (biomePath == null || biomePath.isBlank()) {
            return false;
        }
        String normalizedPath = biomePath.toLowerCase(Locale.ROOT);
        EchoNativeConfigSpec.BooleanValue value = BIOME_CONTENT_ENABLED.get(normalizedPath);
        return value == null || value.get();
    }

    private static int getRawStructureSeparation(StructureType type) {
        EchoNativeConfigSpec.IntValue value = STRUCTURE_SEPARATION.get(type);
        return value != null ? value.get() : 1;
    }
}
