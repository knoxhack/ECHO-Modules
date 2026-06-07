package com.knoxhack.echoruntimeguard;

import com.knoxhack.echocore.api.config.EchoConfigCategory;
import com.knoxhack.echocore.api.config.EchoConfigEntry;
import com.knoxhack.echocore.api.config.EchoConfigModule;
import com.knoxhack.echocore.api.config.EchoConfigProvider;
import com.knoxhack.echocore.api.config.EchoConfigRegistry;
import com.knoxhack.echocore.api.config.EchoConfigSide;
import com.knoxhack.echoruntimeguard.api.ParticleMode;
import com.knoxhack.echoruntimeguard.api.RuntimeMode;
import java.util.List;
import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;

public final class RuntimeGuardConfig {
    private static final EchoNativeConfigSpec.Builder COMMON = new EchoNativeConfigSpec.Builder();
    private static final EchoNativeConfigSpec.Builder CLIENT = new EchoNativeConfigSpec.Builder();

    public static final EchoNativeConfigSpec.BooleanValue ENABLED;
    public static final EchoNativeConfigSpec.EnumValue<RuntimeMode> RUNTIME_MODE;
    public static final EchoNativeConfigSpec.BooleanValue AUTO_EMERGENCY_MODE;
    public static final EchoNativeConfigSpec.BooleanValue DEBUG_LOGGING;
    public static final EchoNativeConfigSpec.IntValue REPORT_INTERVAL_SECONDS;

    public static final EchoNativeConfigSpec.BooleanValue TPS_GUARD_ENABLED;
    public static final EchoNativeConfigSpec.DoubleValue WARNING_TPS;
    public static final EchoNativeConfigSpec.DoubleValue EMERGENCY_TPS;
    public static final EchoNativeConfigSpec.DoubleValue CRITICAL_TPS;
    public static final EchoNativeConfigSpec.DoubleValue EMERGENCY_RECOVERY_TPS;
    public static final EchoNativeConfigSpec.IntValue EMERGENCY_TRIGGER_SECONDS;

    public static final EchoNativeConfigSpec.BooleanValue SMART_TICK_ENABLED;
    public static final EchoNativeConfigSpec.IntValue NEARBY_ACTIVE_TICK_RATE;
    public static final EchoNativeConfigSpec.IntValue NEARBY_IDLE_TICK_RATE;
    public static final EchoNativeConfigSpec.IntValue FAR_TICK_RATE;
    public static final EchoNativeConfigSpec.IntValue VERY_FAR_TICK_RATE;
    public static final EchoNativeConfigSpec.IntValue FAR_DISTANCE_BLOCKS;
    public static final EchoNativeConfigSpec.IntValue VERY_FAR_DISTANCE_BLOCKS;
    public static final EchoNativeConfigSpec.BooleanValue SLEEP_IDLE_BLOCK_ENTITIES;

    public static final EchoNativeConfigSpec.BooleanValue BLOCK_ENTITY_GUARD_ENABLED;
    public static final EchoNativeConfigSpec.IntValue MAX_ACTIVE_BLOCK_ENTITIES_PER_CHUNK;
    public static final EchoNativeConfigSpec.IntValue MAX_ACTIVE_BLOCK_ENTITIES_PER_PLAYER_AREA;
    public static final EchoNativeConfigSpec.BooleanValue THROTTLE_DISTANT_BLOCK_ENTITIES;
    public static final EchoNativeConfigSpec.BooleanValue SLEEP_WHEN_NO_PLAYER_NEARBY;

    public static final EchoNativeConfigSpec.BooleanValue MULTIBLOCK_SCHEDULER_ENABLED;
    public static final EchoNativeConfigSpec.IntValue MAX_VALIDATIONS_PER_TICK;
    public static final EchoNativeConfigSpec.IntValue MAX_BLOCKS_SCANNED_PER_TICK;
    public static final EchoNativeConfigSpec.IntValue IDLE_RECHECK_SECONDS;
    public static final EchoNativeConfigSpec.BooleanValue VALIDATE_ONLY_WHEN_DIRTY;

    public static final EchoNativeConfigSpec.BooleanValue PARTICLE_BUDGET_ENABLED;
    public static final EchoNativeConfigSpec.EnumValue<ParticleMode> PARTICLE_MODE;
    public static final EchoNativeConfigSpec.IntValue POTATO_PARTICLE_BUDGET;
    public static final EchoNativeConfigSpec.IntValue BALANCED_PARTICLE_BUDGET;
    public static final EchoNativeConfigSpec.IntValue CINEMATIC_PARTICLE_BUDGET;
    public static final EchoNativeConfigSpec.IntValue EMERGENCY_PARTICLE_BUDGET;
    public static final EchoNativeConfigSpec.BooleanValue REDUCE_FAR_PARTICLES;
    public static final EchoNativeConfigSpec.BooleanValue REDUCE_DECORATIVE_PARTICLES_FIRST;

    public static final EchoNativeConfigSpec.BooleanValue LENS_GUARD_ENABLED;
    public static final EchoNativeConfigSpec.IntValue LENS_SCAN_COOLDOWN_MS;
    public static final EchoNativeConfigSpec.IntValue LENS_MAX_BLOCKS_PER_SCAN;
    public static final EchoNativeConfigSpec.IntValue LENS_MAX_ENTITIES_PER_SCAN;
    public static final EchoNativeConfigSpec.IntValue LENS_DEEP_SCAN_BUDGET_PER_TICK;
    public static final EchoNativeConfigSpec.BooleanValue DISABLE_PASSIVE_SCANS_WHEN_INACTIVE;

    public static final EchoNativeConfigSpec.BooleanValue HOLOMAP_GUARD_ENABLED;
    public static final EchoNativeConfigSpec.IntValue HOLOMAP_MARKER_REFRESH_SECONDS;
    public static final EchoNativeConfigSpec.IntValue HOLOMAP_DYNAMIC_ROUTE_REFRESH_SECONDS;
    public static final EchoNativeConfigSpec.IntValue HOLOMAP_MAX_ANIMATED_MARKERS;
    public static final EchoNativeConfigSpec.BooleanValue HOLOMAP_SYNC_DIRTY_ONLY;
    public static final EchoNativeConfigSpec.BooleanValue SLEEP_HOLOMAP_WHEN_CLOSED;

    public static final EchoNativeConfigSpec.BooleanValue NETWORK_GUARD_ENABLED;
    public static final EchoNativeConfigSpec.IntValue WARN_PACKETS_PER_SECOND;
    public static final EchoNativeConfigSpec.IntValue WARN_BYTES_PER_SECOND;
    public static final EchoNativeConfigSpec.BooleanValue BATCH_NONCRITICAL_PACKETS;
    public static final EchoNativeConfigSpec.BooleanValue RATE_LIMIT_DUPLICATE_PAYLOADS;
    public static final EchoNativeConfigSpec.BooleanValue SYNC_UI_ONLY_WHEN_OPEN;

    public static final EchoNativeConfigSpec.BooleanValue ENTITY_GUARD_ENABLED;
    public static final EchoNativeConfigSpec.BooleanValue THROTTLE_FAR_ENTITY_AI;
    public static final EchoNativeConfigSpec.IntValue FAR_ENTITY_AI_TICK_RATE;
    public static final EchoNativeConfigSpec.IntValue VERY_FAR_ENTITY_AI_TICK_RATE;
    public static final EchoNativeConfigSpec.IntValue MAX_ACTIVE_HOSTILE_AI_PER_PLAYER;
    public static final EchoNativeConfigSpec.DoubleValue PATHFINDING_COOLDOWN_MULTIPLIER;

    public static final EchoNativeConfigSpec.BooleanValue WORLDGEN_PROFILER_ENABLED;
    public static final EchoNativeConfigSpec.IntValue WARN_CHUNK_GEN_MS;
    public static final EchoNativeConfigSpec.IntValue WARN_FEATURE_GEN_MS;

    public static final EchoNativeConfigSpec.BooleanValue CLIENT_FPS_GUARD_ENABLED;
    public static final EchoNativeConfigSpec.IntValue WARNING_FPS;
    public static final EchoNativeConfigSpec.IntValue EMERGENCY_FPS;
    public static final EchoNativeConfigSpec.BooleanValue HOLOGRAM_LOD_ENABLED;
    public static final EchoNativeConfigSpec.BooleanValue ROBOTIC_ANIMATION_LOD_ENABLED;
    public static final EchoNativeConfigSpec.BooleanValue TERMINAL_ANIMATION_THROTTLE;

    public static final EchoNativeConfigSpec COMMON_SPEC;
    public static final EchoNativeConfigSpec CLIENT_SPEC;

    static {
        COMMON.push("general");
        ENABLED = COMMON.comment("Master switch for RuntimeGuard services.").define("enabled", true);
        RUNTIME_MODE = COMMON.comment("Default RuntimeGuard performance preset.").defineEnum("runtime_mode", RuntimeMode.BALANCED);
        AUTO_EMERGENCY_MODE = COMMON.comment("Automatically enter Emergency mode when TPS/MSPT pressure persists.").define("auto_emergency_mode", true);
        DEBUG_LOGGING = COMMON.comment("Emit detailed RuntimeGuard diagnostic logs.").define("debug_logging", false);
        REPORT_INTERVAL_SECONDS = COMMON.comment("Minimum interval for periodic report-oriented summaries.").defineInRange("report_interval_seconds", 60, 5, 3600);
        COMMON.pop();

        COMMON.push("tps_guard");
        TPS_GUARD_ENABLED = COMMON.define("tps_guard_enabled", true);
        WARNING_TPS = COMMON.defineInRange("warning_tps", 18.0D, 1.0D, 20.0D);
        EMERGENCY_TPS = COMMON.defineInRange("emergency_tps", 15.0D, 1.0D, 20.0D);
        CRITICAL_TPS = COMMON.defineInRange("critical_tps", 10.0D, 1.0D, 20.0D);
        EMERGENCY_RECOVERY_TPS = COMMON.defineInRange("emergency_recovery_tps", 18.5D, 1.0D, 20.0D);
        EMERGENCY_TRIGGER_SECONDS = COMMON.defineInRange("emergency_trigger_seconds", 10, 1, 300);
        COMMON.pop();

        COMMON.push("smart_ticking");
        SMART_TICK_ENABLED = COMMON.define("smart_tick_enabled", true);
        NEARBY_ACTIVE_TICK_RATE = COMMON.defineInRange("nearby_active_tick_rate", 1, 1, 1200);
        NEARBY_IDLE_TICK_RATE = COMMON.defineInRange("nearby_idle_tick_rate", 20, 1, 1200);
        FAR_TICK_RATE = COMMON.defineInRange("far_tick_rate", 60, 1, 2400);
        VERY_FAR_TICK_RATE = COMMON.defineInRange("very_far_tick_rate", 200, 1, 6000);
        FAR_DISTANCE_BLOCKS = COMMON.defineInRange("far_distance_blocks", 64, 8, 512);
        VERY_FAR_DISTANCE_BLOCKS = COMMON.defineInRange("very_far_distance_blocks", 128, 16, 1024);
        SLEEP_IDLE_BLOCK_ENTITIES = COMMON.define("sleep_idle_block_entities", true);
        COMMON.pop();

        COMMON.push("block_entity_guard");
        BLOCK_ENTITY_GUARD_ENABLED = COMMON.define("block_entity_guard_enabled", true);
        MAX_ACTIVE_BLOCK_ENTITIES_PER_CHUNK = COMMON.defineInRange("max_active_block_entities_per_chunk", 64, 1, 4096);
        MAX_ACTIVE_BLOCK_ENTITIES_PER_PLAYER_AREA = COMMON.defineInRange("max_active_block_entities_per_player_area", 512, 1, 65536);
        THROTTLE_DISTANT_BLOCK_ENTITIES = COMMON.define("throttle_distant_block_entities", true);
        SLEEP_WHEN_NO_PLAYER_NEARBY = COMMON.define("sleep_when_no_player_nearby", true);
        COMMON.pop();

        COMMON.push("multiblock_guard");
        MULTIBLOCK_SCHEDULER_ENABLED = COMMON.define("multiblock_scheduler_enabled", true);
        MAX_VALIDATIONS_PER_TICK = COMMON.defineInRange("max_validations_per_tick", 2, 1, 256);
        MAX_BLOCKS_SCANNED_PER_TICK = COMMON.defineInRange("max_blocks_scanned_per_tick", 512, 1, 65536);
        IDLE_RECHECK_SECONDS = COMMON.defineInRange("idle_recheck_seconds", 10, 1, 3600);
        VALIDATE_ONLY_WHEN_DIRTY = COMMON.define("validate_only_when_dirty", true);
        COMMON.pop();

        COMMON.push("particle_guard");
        PARTICLE_BUDGET_ENABLED = COMMON.define("particle_budget_enabled", true);
        PARTICLE_MODE = COMMON.defineEnum("particle_mode", ParticleMode.AUTO);
        POTATO_PARTICLE_BUDGET = COMMON.defineInRange("potato_particle_budget", 300, 0, 100000);
        BALANCED_PARTICLE_BUDGET = COMMON.defineInRange("balanced_particle_budget", 1200, 0, 100000);
        CINEMATIC_PARTICLE_BUDGET = COMMON.defineInRange("cinematic_particle_budget", 3500, 0, 100000);
        EMERGENCY_PARTICLE_BUDGET = COMMON.defineInRange("emergency_particle_budget", 150, 0, 100000);
        REDUCE_FAR_PARTICLES = COMMON.define("reduce_far_particles", true);
        REDUCE_DECORATIVE_PARTICLES_FIRST = COMMON.define("reduce_decorative_particles_first", true);
        COMMON.pop();

        COMMON.push("lens_guard");
        LENS_GUARD_ENABLED = COMMON.define("lens_guard_enabled", true);
        LENS_SCAN_COOLDOWN_MS = COMMON.defineInRange("lens_scan_cooldown_ms", 250, 0, 60000);
        LENS_MAX_BLOCKS_PER_SCAN = COMMON.defineInRange("lens_max_blocks_per_scan", 128, 1, 65536);
        LENS_MAX_ENTITIES_PER_SCAN = COMMON.defineInRange("lens_max_entities_per_scan", 32, 1, 4096);
        LENS_DEEP_SCAN_BUDGET_PER_TICK = COMMON.defineInRange("lens_deep_scan_budget_per_tick", 64, 1, 65536);
        DISABLE_PASSIVE_SCANS_WHEN_INACTIVE = COMMON.define("disable_passive_scans_when_inactive", true);
        COMMON.pop();

        COMMON.push("holomap_guard");
        HOLOMAP_GUARD_ENABLED = COMMON.define("holomap_guard_enabled", true);
        HOLOMAP_MARKER_REFRESH_SECONDS = COMMON.defineInRange("holomap_marker_refresh_seconds", 5, 1, 3600);
        HOLOMAP_DYNAMIC_ROUTE_REFRESH_SECONDS = COMMON.defineInRange("holomap_dynamic_route_refresh_seconds", 10, 1, 3600);
        HOLOMAP_MAX_ANIMATED_MARKERS = COMMON.defineInRange("holomap_max_animated_markers", 40, 0, 4096);
        HOLOMAP_SYNC_DIRTY_ONLY = COMMON.define("holomap_sync_dirty_only", true);
        SLEEP_HOLOMAP_WHEN_CLOSED = COMMON.define("sleep_holomap_when_closed", true);
        COMMON.pop();

        COMMON.push("network_guard");
        NETWORK_GUARD_ENABLED = COMMON.define("network_guard_enabled", true);
        WARN_PACKETS_PER_SECOND = COMMON.defineInRange("warn_packets_per_second", 300, 1, 100000);
        WARN_BYTES_PER_SECOND = COMMON.defineInRange("warn_bytes_per_second", 250000, 1, Integer.MAX_VALUE);
        BATCH_NONCRITICAL_PACKETS = COMMON.define("batch_noncritical_packets", true);
        RATE_LIMIT_DUPLICATE_PAYLOADS = COMMON.define("rate_limit_duplicate_payloads", true);
        SYNC_UI_ONLY_WHEN_OPEN = COMMON.define("sync_ui_only_when_open", true);
        COMMON.pop();

        COMMON.push("entity_guard");
        ENTITY_GUARD_ENABLED = COMMON.define("entity_guard_enabled", true);
        THROTTLE_FAR_ENTITY_AI = COMMON.define("throttle_far_entity_ai", true);
        FAR_ENTITY_AI_TICK_RATE = COMMON.defineInRange("far_entity_ai_tick_rate", 40, 1, 2400);
        VERY_FAR_ENTITY_AI_TICK_RATE = COMMON.defineInRange("very_far_entity_ai_tick_rate", 100, 1, 6000);
        MAX_ACTIVE_HOSTILE_AI_PER_PLAYER = COMMON.defineInRange("max_active_hostile_ai_per_player", 60, 1, 4096);
        PATHFINDING_COOLDOWN_MULTIPLIER = COMMON.defineInRange("pathfinding_cooldown_multiplier", 2.0D, 1.0D, 100.0D);
        COMMON.pop();

        COMMON.push("worldgen_guard");
        WORLDGEN_PROFILER_ENABLED = COMMON.define("worldgen_profiler_enabled", true);
        WARN_CHUNK_GEN_MS = COMMON.defineInRange("warn_chunk_gen_ms", 250, 1, 60000);
        WARN_FEATURE_GEN_MS = COMMON.defineInRange("warn_feature_gen_ms", 50, 1, 60000);
        COMMON.pop();

        CLIENT.push("rendering_guard_client");
        CLIENT_FPS_GUARD_ENABLED = CLIENT.define("client_fps_guard_enabled", true);
        WARNING_FPS = CLIENT.defineInRange("warning_fps", 50, 1, 1000);
        EMERGENCY_FPS = CLIENT.defineInRange("emergency_fps", 30, 1, 1000);
        HOLOGRAM_LOD_ENABLED = CLIENT.define("hologram_lod_enabled", true);
        ROBOTIC_ANIMATION_LOD_ENABLED = CLIENT.define("robotic_animation_lod_enabled", true);
        TERMINAL_ANIMATION_THROTTLE = CLIENT.define("terminal_animation_throttle", true);
        CLIENT.pop();

        COMMON_SPEC = COMMON.build();
        CLIENT_SPEC = CLIENT.build();
    }

    private RuntimeGuardConfig() {
    }

    public static void registerEchoConfig() {
        EchoConfigRegistry.register(EchoConfigProvider.of(EchoRuntimeGuard.MODID, () -> new EchoConfigModule(
                EchoRuntimeGuard.MODID,
                "RuntimeGuard",
                List.of(
                        new EchoConfigCategory("general", "General", List.of(
                                EchoConfigEntry.booleanSpec("enabled", "Enabled", "Master RuntimeGuard switch.",
                                        EchoConfigSide.COMMON, ENABLED, true, false, false),
                                EchoConfigEntry.enumSpec("runtime_mode", "Runtime Mode", "Default performance preset.",
                                        EchoConfigSide.COMMON, RUNTIME_MODE, RuntimeMode.class, true, false, false),
                                EchoConfigEntry.booleanSpec("auto_emergency_mode", "Auto Emergency Mode",
                                        "Automatically enter Emergency mode under sustained pressure.",
                                        EchoConfigSide.COMMON, AUTO_EMERGENCY_MODE, true, false, false),
                                EchoConfigEntry.booleanSpec("debug_logging", "Debug Logging", "",
                                        EchoConfigSide.COMMON, DEBUG_LOGGING, true, false, false),
                                EchoConfigEntry.intSpec("report_interval_seconds", "Report Interval Seconds", "",
                                        EchoConfigSide.COMMON, REPORT_INTERVAL_SECONDS, 5, 3600, true, false, false))),
                        new EchoConfigCategory("tps_guard", "TPS Guard", List.of(
                                EchoConfigEntry.booleanSpec("tps_guard_enabled", "TPS Guard Enabled", "",
                                        EchoConfigSide.COMMON, TPS_GUARD_ENABLED, true, false, false),
                                EchoConfigEntry.doubleSpec("warning_tps", "Warning TPS", "",
                                        EchoConfigSide.COMMON, WARNING_TPS, 1.0D, 20.0D, true, false, false),
                                EchoConfigEntry.doubleSpec("emergency_tps", "Emergency TPS", "",
                                        EchoConfigSide.COMMON, EMERGENCY_TPS, 1.0D, 20.0D, true, false, false),
                                EchoConfigEntry.doubleSpec("critical_tps", "Critical TPS", "",
                                        EchoConfigSide.COMMON, CRITICAL_TPS, 1.0D, 20.0D, true, false, false),
                                EchoConfigEntry.doubleSpec("emergency_recovery_tps", "Emergency Recovery TPS", "",
                                        EchoConfigSide.COMMON, EMERGENCY_RECOVERY_TPS, 1.0D, 20.0D, true, false, false),
                                EchoConfigEntry.intSpec("emergency_trigger_seconds", "Emergency Trigger Seconds", "",
                                        EchoConfigSide.COMMON, EMERGENCY_TRIGGER_SECONDS, 1, 300, true, false, false))),
                        new EchoConfigCategory("smart_ticking", "Smart Ticking", List.of(
                                EchoConfigEntry.booleanSpec("smart_tick_enabled", "Smart Tick Enabled", "",
                                        EchoConfigSide.COMMON, SMART_TICK_ENABLED, true, false, false),
                                EchoConfigEntry.intSpec("nearby_active_tick_rate", "Nearby Active Tick Rate", "",
                                        EchoConfigSide.COMMON, NEARBY_ACTIVE_TICK_RATE, 1, 1200, true, false, false),
                                EchoConfigEntry.intSpec("nearby_idle_tick_rate", "Nearby Idle Tick Rate", "",
                                        EchoConfigSide.COMMON, NEARBY_IDLE_TICK_RATE, 1, 1200, true, false, false),
                                EchoConfigEntry.intSpec("far_tick_rate", "Far Tick Rate", "",
                                        EchoConfigSide.COMMON, FAR_TICK_RATE, 1, 2400, true, false, false),
                                EchoConfigEntry.intSpec("very_far_tick_rate", "Very Far Tick Rate", "",
                                        EchoConfigSide.COMMON, VERY_FAR_TICK_RATE, 1, 6000, true, false, false),
                                EchoConfigEntry.intSpec("far_distance_blocks", "Far Distance Blocks", "",
                                        EchoConfigSide.COMMON, FAR_DISTANCE_BLOCKS, 8, 512, true, false, false),
                                EchoConfigEntry.intSpec("very_far_distance_blocks", "Very Far Distance Blocks", "",
                                        EchoConfigSide.COMMON, VERY_FAR_DISTANCE_BLOCKS, 16, 1024, true, false, false),
                                EchoConfigEntry.booleanSpec("sleep_idle_block_entities", "Sleep Idle Block Entities", "",
                                        EchoConfigSide.COMMON, SLEEP_IDLE_BLOCK_ENTITIES, true, false, false))),
                        new EchoConfigCategory("block_entity_guard", "Block Entity Guard", List.of(
                                EchoConfigEntry.booleanSpec("block_entity_guard_enabled", "Block Entity Guard Enabled", "",
                                        EchoConfigSide.COMMON, BLOCK_ENTITY_GUARD_ENABLED, true, false, false),
                                EchoConfigEntry.intSpec("max_active_block_entities_per_chunk", "Max Active Block Entities Per Chunk", "",
                                        EchoConfigSide.COMMON, MAX_ACTIVE_BLOCK_ENTITIES_PER_CHUNK, 1, 4096, true, false, false),
                                EchoConfigEntry.intSpec("max_active_block_entities_per_player_area", "Max Active Block Entities Per Player Area", "",
                                        EchoConfigSide.COMMON, MAX_ACTIVE_BLOCK_ENTITIES_PER_PLAYER_AREA, 1, 65536, true, false, false),
                                EchoConfigEntry.booleanSpec("throttle_distant_block_entities", "Throttle Distant Block Entities", "",
                                        EchoConfigSide.COMMON, THROTTLE_DISTANT_BLOCK_ENTITIES, true, false, false),
                                EchoConfigEntry.booleanSpec("sleep_when_no_player_nearby", "Sleep When No Player Nearby", "",
                                        EchoConfigSide.COMMON, SLEEP_WHEN_NO_PLAYER_NEARBY, true, false, false))),
                        new EchoConfigCategory("multiblock_guard", "Multiblock Guard", List.of(
                                EchoConfigEntry.booleanSpec("multiblock_scheduler_enabled", "Multiblock Scheduler Enabled", "",
                                        EchoConfigSide.COMMON, MULTIBLOCK_SCHEDULER_ENABLED, true, false, false),
                                EchoConfigEntry.intSpec("max_validations_per_tick", "Max Validations Per Tick", "",
                                        EchoConfigSide.COMMON, MAX_VALIDATIONS_PER_TICK, 1, 256, true, false, false),
                                EchoConfigEntry.intSpec("max_blocks_scanned_per_tick", "Max Blocks Scanned Per Tick", "",
                                        EchoConfigSide.COMMON, MAX_BLOCKS_SCANNED_PER_TICK, 1, 65536, true, false, false),
                                EchoConfigEntry.intSpec("idle_recheck_seconds", "Idle Recheck Seconds", "",
                                        EchoConfigSide.COMMON, IDLE_RECHECK_SECONDS, 1, 3600, true, false, false),
                                EchoConfigEntry.booleanSpec("validate_only_when_dirty", "Validate Only When Dirty", "",
                                        EchoConfigSide.COMMON, VALIDATE_ONLY_WHEN_DIRTY, true, false, false))),
                        new EchoConfigCategory("particle_guard", "Particle Guard", List.of(
                                EchoConfigEntry.booleanSpec("particle_budget_enabled", "Particle Budget Enabled", "",
                                        EchoConfigSide.COMMON, PARTICLE_BUDGET_ENABLED, true, false, false),
                                EchoConfigEntry.enumSpec("particle_mode", "Particle Mode", "",
                                        EchoConfigSide.COMMON, PARTICLE_MODE, ParticleMode.class, true, false, false),
                                EchoConfigEntry.intSpec("potato_particle_budget", "Potato Particle Budget", "",
                                        EchoConfigSide.COMMON, POTATO_PARTICLE_BUDGET, 0, 100000, true, false, false),
                                EchoConfigEntry.intSpec("balanced_particle_budget", "Balanced Particle Budget", "",
                                        EchoConfigSide.COMMON, BALANCED_PARTICLE_BUDGET, 0, 100000, true, false, false),
                                EchoConfigEntry.intSpec("cinematic_particle_budget", "Cinematic Particle Budget", "",
                                        EchoConfigSide.COMMON, CINEMATIC_PARTICLE_BUDGET, 0, 100000, true, false, false),
                                EchoConfigEntry.intSpec("emergency_particle_budget", "Emergency Particle Budget", "",
                                        EchoConfigSide.COMMON, EMERGENCY_PARTICLE_BUDGET, 0, 100000, true, false, false),
                                EchoConfigEntry.booleanSpec("reduce_far_particles", "Reduce Far Particles", "",
                                        EchoConfigSide.COMMON, REDUCE_FAR_PARTICLES, true, false, false),
                                EchoConfigEntry.booleanSpec("reduce_decorative_particles_first", "Reduce Decorative Particles First", "",
                                        EchoConfigSide.COMMON, REDUCE_DECORATIVE_PARTICLES_FIRST, true, false, false))),
                        new EchoConfigCategory("lens_guard", "Lens Guard", List.of(
                                EchoConfigEntry.booleanSpec("lens_guard_enabled", "Lens Guard Enabled", "",
                                        EchoConfigSide.COMMON, LENS_GUARD_ENABLED, true, false, false),
                                EchoConfigEntry.intSpec("lens_scan_cooldown_ms", "Lens Scan Cooldown Ms", "",
                                        EchoConfigSide.COMMON, LENS_SCAN_COOLDOWN_MS, 0, 60000, true, false, false),
                                EchoConfigEntry.intSpec("lens_max_blocks_per_scan", "Lens Max Blocks Per Scan", "",
                                        EchoConfigSide.COMMON, LENS_MAX_BLOCKS_PER_SCAN, 1, 65536, true, false, false),
                                EchoConfigEntry.intSpec("lens_max_entities_per_scan", "Lens Max Entities Per Scan", "",
                                        EchoConfigSide.COMMON, LENS_MAX_ENTITIES_PER_SCAN, 1, 4096, true, false, false),
                                EchoConfigEntry.intSpec("lens_deep_scan_budget_per_tick", "Lens Deep Scan Budget Per Tick", "",
                                        EchoConfigSide.COMMON, LENS_DEEP_SCAN_BUDGET_PER_TICK, 1, 65536, true, false, false),
                                EchoConfigEntry.booleanSpec("disable_passive_scans_when_inactive", "Disable Passive Scans When Inactive", "",
                                        EchoConfigSide.COMMON, DISABLE_PASSIVE_SCANS_WHEN_INACTIVE, true, false, false))),
                        new EchoConfigCategory("holomap_guard", "HoloMap Guard", List.of(
                                EchoConfigEntry.booleanSpec("holomap_guard_enabled", "HoloMap Guard Enabled", "",
                                        EchoConfigSide.COMMON, HOLOMAP_GUARD_ENABLED, true, false, false),
                                EchoConfigEntry.intSpec("holomap_marker_refresh_seconds", "HoloMap Marker Refresh Seconds", "",
                                        EchoConfigSide.COMMON, HOLOMAP_MARKER_REFRESH_SECONDS, 1, 3600, true, false, false),
                                EchoConfigEntry.intSpec("holomap_dynamic_route_refresh_seconds", "HoloMap Dynamic Route Refresh Seconds", "",
                                        EchoConfigSide.COMMON, HOLOMAP_DYNAMIC_ROUTE_REFRESH_SECONDS, 1, 3600, true, false, false),
                                EchoConfigEntry.intSpec("holomap_max_animated_markers", "HoloMap Max Animated Markers", "",
                                        EchoConfigSide.COMMON, HOLOMAP_MAX_ANIMATED_MARKERS, 0, 4096, true, false, false),
                                EchoConfigEntry.booleanSpec("holomap_sync_dirty_only", "HoloMap Sync Dirty Only", "",
                                        EchoConfigSide.COMMON, HOLOMAP_SYNC_DIRTY_ONLY, true, false, false),
                                EchoConfigEntry.booleanSpec("sleep_holomap_when_closed", "Sleep HoloMap When Closed", "",
                                        EchoConfigSide.COMMON, SLEEP_HOLOMAP_WHEN_CLOSED, true, false, false))),
                        new EchoConfigCategory("network_guard", "Network Guard", List.of(
                                EchoConfigEntry.booleanSpec("network_guard_enabled", "Network Guard Enabled", "",
                                        EchoConfigSide.COMMON, NETWORK_GUARD_ENABLED, true, false, false),
                                EchoConfigEntry.intSpec("warn_packets_per_second", "Warn Packets Per Second", "",
                                        EchoConfigSide.COMMON, WARN_PACKETS_PER_SECOND, 1, 100000, true, false, false),
                                EchoConfigEntry.intSpec("warn_bytes_per_second", "Warn Bytes Per Second", "",
                                        EchoConfigSide.COMMON, WARN_BYTES_PER_SECOND, 1, Integer.MAX_VALUE, true, false, false),
                                EchoConfigEntry.booleanSpec("batch_noncritical_packets", "Batch Noncritical Packets", "",
                                        EchoConfigSide.COMMON, BATCH_NONCRITICAL_PACKETS, true, false, false),
                                EchoConfigEntry.booleanSpec("rate_limit_duplicate_payloads", "Rate Limit Duplicate Payloads", "",
                                        EchoConfigSide.COMMON, RATE_LIMIT_DUPLICATE_PAYLOADS, true, false, false),
                                EchoConfigEntry.booleanSpec("sync_ui_only_when_open", "Sync UI Only When Open", "",
                                        EchoConfigSide.COMMON, SYNC_UI_ONLY_WHEN_OPEN, true, false, false))),
                        new EchoConfigCategory("entity_guard", "Entity Guard", List.of(
                                EchoConfigEntry.booleanSpec("entity_guard_enabled", "Entity Guard Enabled", "",
                                        EchoConfigSide.COMMON, ENTITY_GUARD_ENABLED, true, false, false),
                                EchoConfigEntry.booleanSpec("throttle_far_entity_ai", "Throttle Far Entity AI", "",
                                        EchoConfigSide.COMMON, THROTTLE_FAR_ENTITY_AI, true, false, false),
                                EchoConfigEntry.intSpec("far_entity_ai_tick_rate", "Far Entity AI Tick Rate", "",
                                        EchoConfigSide.COMMON, FAR_ENTITY_AI_TICK_RATE, 1, 2400, true, false, false),
                                EchoConfigEntry.intSpec("very_far_entity_ai_tick_rate", "Very Far Entity AI Tick Rate", "",
                                        EchoConfigSide.COMMON, VERY_FAR_ENTITY_AI_TICK_RATE, 1, 6000, true, false, false),
                                EchoConfigEntry.intSpec("max_active_hostile_ai_per_player", "Max Active Hostile AI Per Player", "",
                                        EchoConfigSide.COMMON, MAX_ACTIVE_HOSTILE_AI_PER_PLAYER, 1, 4096, true, false, false),
                                EchoConfigEntry.doubleSpec("pathfinding_cooldown_multiplier", "Pathfinding Cooldown Multiplier", "",
                                        EchoConfigSide.COMMON, PATHFINDING_COOLDOWN_MULTIPLIER, 1.0D, 100.0D, true, false, false))),
                        new EchoConfigCategory("worldgen_guard", "Worldgen Guard", List.of(
                                EchoConfigEntry.booleanSpec("worldgen_profiler_enabled", "Worldgen Profiler Enabled", "",
                                        EchoConfigSide.COMMON, WORLDGEN_PROFILER_ENABLED, true, false, false),
                                EchoConfigEntry.intSpec("warn_chunk_gen_ms", "Warn Chunk Gen Ms", "",
                                        EchoConfigSide.COMMON, WARN_CHUNK_GEN_MS, 1, 60000, true, false, false),
                                EchoConfigEntry.intSpec("warn_feature_gen_ms", "Warn Feature Gen Ms", "",
                                        EchoConfigSide.COMMON, WARN_FEATURE_GEN_MS, 1, 60000, true, false, false))),
                        new EchoConfigCategory("client", "Client", List.of(
                                EchoConfigEntry.booleanSpec("client_fps_guard_enabled", "FPS Guard Enabled", "",
                                        EchoConfigSide.CLIENT, CLIENT_FPS_GUARD_ENABLED, true, false, false),
                                EchoConfigEntry.intSpec("warning_fps", "Warning FPS", "",
                                        EchoConfigSide.CLIENT, WARNING_FPS, 1, 1000, true, false, false),
                                EchoConfigEntry.intSpec("emergency_fps", "Emergency FPS", "",
                                        EchoConfigSide.CLIENT, EMERGENCY_FPS, 1, 1000, true, false, false),
                                EchoConfigEntry.booleanSpec("hologram_lod_enabled", "Hologram LOD Enabled", "",
                                        EchoConfigSide.CLIENT, HOLOGRAM_LOD_ENABLED, true, false, false),
                                EchoConfigEntry.booleanSpec("robotic_animation_lod_enabled", "Robotic Animation LOD Enabled", "",
                                        EchoConfigSide.CLIENT, ROBOTIC_ANIMATION_LOD_ENABLED, true, false, false),
                                EchoConfigEntry.booleanSpec("terminal_animation_throttle", "Terminal Animation Throttle", "",
                                        EchoConfigSide.CLIENT, TERMINAL_ANIMATION_THROTTLE, true, false, false)))))));
    }

    public static boolean enabled() {
        return safeBool(ENABLED, true);
    }

    public static RuntimeMode configuredMode() {
        try {
            return RUNTIME_MODE.get();
        } catch (RuntimeException exception) {
            return RuntimeMode.BALANCED;
        }
    }

    public static boolean safeBool(EchoNativeConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static int safeInt(EchoNativeConfigSpec.IntValue value, int fallback) {
        try {
            return value.get();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static double safeDouble(EchoNativeConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
