package com.knoxhack.echoplayercore.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;
import java.util.List;

public final class PlayerCoreConfig {
    private static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();

    public static final EchoNativeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master enable for ECHO PlayerCore.")
            .define("general.enabled", true);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_ALIAS_COMMANDS = BUILDER
            .comment("Enable direct alias commands like /home, /rtp, etc.")
            .define("general.enable_alias_commands", true);

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_ECHO_NAMESPACE_COMMANDS = BUILDER
            .comment("Enable /echo namespace aliases.")
            .define("general.enable_echo_namespace_commands", true);

    public static final EchoNativeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Log debug info to console.")
            .define("general.debug_logging", false);

    public static final EchoNativeConfigSpec.BooleanValue HOMES_ENABLED = BUILDER
            .comment("Enable home commands.")
            .define("homes.enabled", true);

    public static final EchoNativeConfigSpec.IntValue MAX_HOMES_DEFAULT = BUILDER
            .comment("Default max homes for non-op players.")
            .defineInRange("homes.max_homes_default", 3, 0, 512);

    public static final EchoNativeConfigSpec.IntValue MAX_HOMES_OP = BUILDER
            .comment("Max homes for op players.")
            .defineInRange("homes.max_homes_op", 20, 0, 512);

    public static final EchoNativeConfigSpec.StringValue DEFAULT_HOME_NAME = BUILDER
            .comment("Default home name when none is provided.")
            .define("homes.default_home_name", "home", s -> s instanceof String str && !str.isBlank());

    public static final EchoNativeConfigSpec.BooleanValue ALLOW_CROSS_DIMENSION_HOME = BUILDER
            .comment("Allow teleporting across dimensions with /home.")
            .define("homes.allow_cross_dimension_home", false);

    public static final EchoNativeConfigSpec.BooleanValue ALLOW_NAMED_HOMES = BUILDER
            .comment("Allow named homes beyond the default.")
            .define("homes.allow_named_homes", true);

    public static final EchoNativeConfigSpec.BooleanValue CASE_SENSITIVE_HOME_NAMES = BUILDER
            .comment("If false, home names are normalized to lowercase.")
            .define("homes.case_sensitive_home_names", false);

    public static final EchoNativeConfigSpec.BooleanValue OVERWRITE_HOME_REQUIRES_CONFIRM = BUILDER
            .comment("If true, overwriting a home requires a confirmation step (not yet implemented).")
            .define("homes.overwrite_home_requires_confirm", false);

    public static final EchoNativeConfigSpec.BooleanValue RTP_ENABLED = BUILDER
            .comment("Enable random teleport.")
            .define("random_teleport.enabled", true);

    public static final EchoNativeConfigSpec.IntValue RTP_MIN_RADIUS = BUILDER
            .comment("Minimum RTP distance from origin.")
            .defineInRange("random_teleport.min_radius", 500, 64, 20000);

    public static final EchoNativeConfigSpec.IntValue RTP_MAX_RADIUS = BUILDER
            .comment("Maximum RTP distance from origin.")
            .defineInRange("random_teleport.max_radius", 5000, 64, 20000);

    public static final EchoNativeConfigSpec.IntValue RTP_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown between RTP attempts (seconds).")
            .defineInRange("random_teleport.cooldown_seconds", 300, 0, 3600);

    public static final EchoNativeConfigSpec.IntValue RTP_MAX_ATTEMPTS = BUILDER
            .comment("Max safe-location attempts per RTP request.")
            .defineInRange("random_teleport.max_attempts", 32, 1, 256);

    public static final EchoNativeConfigSpec.IntValue RTP_SEARCH_HEIGHT_MIN = BUILDER
            .comment("Minimum Y to search for safe surface.")
            .defineInRange("random_teleport.search_height_min", 40, -64, 320);

    public static final EchoNativeConfigSpec.IntValue RTP_SEARCH_HEIGHT_MAX = BUILDER
            .comment("Maximum Y to search for safe surface.")
            .defineInRange("random_teleport.search_height_max", 320, -64, 512);

    public static final EchoNativeConfigSpec.BooleanValue RTP_AVOID_LAVA = BUILDER
            .comment("Avoid lava for RTP.")
            .define("random_teleport.avoid_lava", true);

    public static final EchoNativeConfigSpec.BooleanValue RTP_AVOID_WATER = BUILDER
            .comment("Avoid water for RTP.")
            .define("random_teleport.avoid_water", true);

    public static final EchoNativeConfigSpec.BooleanValue RTP_AVOID_POWDER_SNOW = BUILDER
            .comment("Avoid powder snow for RTP.")
            .define("random_teleport.avoid_powder_snow", true);

    public static final EchoNativeConfigSpec.BooleanValue RTP_AVOID_CACTUS = BUILDER
            .comment("Avoid cactus for RTP.")
            .define("random_teleport.avoid_cactus", true);

    public static final EchoNativeConfigSpec.BooleanValue RTP_AVOID_DEEP_DARK = BUILDER
            .comment("Avoid deep dark biomes for RTP.")
            .define("random_teleport.avoid_deep_dark", true);

    public static final EchoNativeConfigSpec.BooleanValue RTP_AVOID_HIGH_RADIATION = BUILDER
            .comment("Avoid high-radiation zones if WorldCore is present.")
            .define("random_teleport.avoid_high_radiation", true);

    public static final EchoNativeConfigSpec.BooleanValue RTP_REQUIRE_SAFE_SURFACE = BUILDER
            .comment("Require solid ground under the destination.")
            .define("random_teleport.require_safe_surface", true);

    public static final EchoNativeConfigSpec.ListValue RTP_ALLOWED_DIMENSIONS = BUILDER
            .comment("Dimensions where RTP is allowed.")
            .defineList("random_teleport.allowed_dimensions", List.of("minecraft:overworld"), s -> s instanceof String str && !str.isBlank());

    public static final EchoNativeConfigSpec.ListValue RTP_BLOCKED_DIMENSIONS = BUILDER
            .comment("Dimensions where RTP is blocked.")
            .defineList("random_teleport.blocked_dimensions", List.of("minecraft:the_nether", "minecraft:the_end"), s -> s instanceof String str && !str.isBlank());

    public static final EchoNativeConfigSpec.BooleanValue BACK_ENABLED = BUILDER
            .comment("Enable /back command.")
            .define("back.enabled", true);

    public static final EchoNativeConfigSpec.BooleanValue ALLOW_BACK_AFTER_DEATH = BUILDER
            .comment("Allow /back to return to death location.")
            .define("back.allow_back_after_death", true);

    public static final EchoNativeConfigSpec.BooleanValue ALLOW_BACK_AFTER_TELEPORT = BUILDER
            .comment("Allow /back after any PlayerCore teleport.")
            .define("back.allow_back_after_teleport", true);

    public static final EchoNativeConfigSpec.IntValue BACK_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown for /back.")
            .defineInRange("back.cooldown_seconds", 60, 0, 3600);

    public static final EchoNativeConfigSpec.BooleanValue STORE_BACK_ON_HOME = BUILDER
            .comment("Store back location when using /home.")
            .define("back.store_back_on_home", true);

    public static final EchoNativeConfigSpec.BooleanValue STORE_BACK_ON_SPAWN = BUILDER
            .comment("Store back location when using /spawn.")
            .define("back.store_back_on_spawn", true);

    public static final EchoNativeConfigSpec.BooleanValue STORE_BACK_ON_RTP = BUILDER
            .comment("Store back location when using /rtp.")
            .define("back.store_back_on_rtp", true);

    public static final EchoNativeConfigSpec.BooleanValue SPAWN_ENABLED = BUILDER
            .comment("Enable spawn teleport.")
            .define("spawn.enabled", true);

    public static final EchoNativeConfigSpec.BooleanValue SPAWN_USE_WORLD_SPAWN = BUILDER
            .comment("Teleport to world spawn.")
            .define("spawn.use_world_spawn", true);

    public static final EchoNativeConfigSpec.BooleanValue ALLOW_SPAWN_COMMAND = BUILDER
            .comment("Allow /spawn command.")
            .define("spawn.allow_spawn_command", true);

    public static final EchoNativeConfigSpec.IntValue SPAWN_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown for /spawn.")
            .defineInRange("spawn.cooldown_seconds", 30, 0, 3600);

    public static final EchoNativeConfigSpec.BooleanValue ALLOW_CROSS_DIMENSION_SPAWN = BUILDER
            .comment("Allow /spawn across dimensions.")
            .define("spawn.allow_cross_dimension_spawn", true);

    public static final EchoNativeConfigSpec.BooleanValue TPA_ENABLED = BUILDER
            .comment("Enable TPA commands.")
            .define("tpa.enabled", true);

    public static final EchoNativeConfigSpec.IntValue TPA_TIMEOUT_SECONDS = BUILDER
            .comment("Timeout for TPA requests (seconds).")
            .defineInRange("tpa.timeout_seconds", 60, 5, 300);

    public static final EchoNativeConfigSpec.IntValue TPA_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown between TPA requests (seconds).")
            .defineInRange("tpa.cooldown_seconds", 30, 0, 3600);

    public static final EchoNativeConfigSpec.BooleanValue WARPS_ENABLED = BUILDER
            .comment("Enable warp commands.")
            .define("warps.enabled", true);

    public static final EchoNativeConfigSpec.BooleanValue OPS_BYPASS_COOLDOWNS = BUILDER
            .comment("Ops bypass cooldowns.")
            .define("permissions.ops_bypass_cooldowns", true);

    public static final EchoNativeConfigSpec.BooleanValue OPS_BYPASS_HOME_LIMIT = BUILDER
            .comment("Ops bypass home limits.")
            .define("permissions.ops_bypass_home_limit", true);

    public static final EchoNativeConfigSpec.IntValue PERMISSION_LEVEL_SETSPAWN = BUILDER
            .comment("Permission level for /setspawn.")
            .defineInRange("permissions.permission_level_setspawn", 2, 0, 4);

    public static final EchoNativeConfigSpec.IntValue PERMISSION_LEVEL_WARPS = BUILDER
            .comment("Permission level for warp admin commands.")
            .defineInRange("permissions.permission_level_warps", 2, 0, 4);

    public static final EchoNativeConfigSpec.IntValue PERMISSION_LEVEL_ADMIN_RELOAD = BUILDER
            .comment("Permission level for admin reload commands.")
            .defineInRange("permissions.permission_level_admin_reload", 2, 0, 4);

    public static final EchoNativeConfigSpec.IntValue RTP_SEARCHES_PER_TICK = BUILDER
            .comment("Max RTP searches per tick (queue budget).")
            .defineInRange("performance.rtp_searches_per_tick", 1, 1, 64);

    public static final EchoNativeConfigSpec.IntValue RTP_MAX_CHUNK_GENERATIONS = BUILDER
            .comment("Max chunk generations triggered per RTP request.")
            .defineInRange("performance.rtp_max_chunk_generations_per_request", 8, 1, 64);

    public static final EchoNativeConfigSpec.BooleanValue RTP_ASYNC_QUEUE_ENABLED = BUILDER
            .comment("Enable async RTP queue (prepared; currently synchronous with limits).")
            .define("performance.rtp_async_queue_enabled", true);

    public static final EchoNativeConfigSpec.BooleanValue USE_RUNTIMEGUARD_IF_AVAILABLE = BUILDER
            .comment("Use RuntimeGuard for scan budgeting if present.")
            .define("performance.use_runtimeguard_if_available", true);

    public static final EchoNativeConfigSpec.StringValue MESSAGE_PREFIX = BUILDER
            .comment("Chat prefix for PlayerCore messages.")
            .define("messages.prefix", "ECHO PlayerCore", s -> s instanceof String str && !str.isBlank());

    public static final EchoNativeConfigSpec.BooleanValue USE_ECHO_STYLE_MESSAGES = BUILDER
            .comment("Use ECHO-styled prefix in messages.")
            .define("messages.use_echo_style_messages", true);

    public static final EchoNativeConfigSpec SPEC = BUILDER.build();

    private PlayerCoreConfig() {
    }

    public static boolean enabled() {
        return ENABLED.get();
    }

    public static boolean aliasCommandsEnabled() {
        return ENABLE_ALIAS_COMMANDS.get();
    }

    public static boolean echoNamespaceCommandsEnabled() {
        return ENABLE_ECHO_NAMESPACE_COMMANDS.get();
    }

    public static boolean debugLogging() {
        return DEBUG_LOGGING.get();
    }

    public static boolean homesEnabled() {
        return HOMES_ENABLED.get();
    }

    public static int maxHomesDefault() {
        return Math.max(0, MAX_HOMES_DEFAULT.get());
    }

    public static int maxHomesOp() {
        return Math.max(0, MAX_HOMES_OP.get());
    }

    public static String defaultHomeName() {
        String v = DEFAULT_HOME_NAME.get();
        return v == null || v.isBlank() ? "home" : v.strip();
    }

    public static boolean allowCrossDimensionHome() {
        return ALLOW_CROSS_DIMENSION_HOME.get();
    }

    public static boolean allowNamedHomes() {
        return ALLOW_NAMED_HOMES.get();
    }

    public static boolean caseSensitiveHomeNames() {
        return CASE_SENSITIVE_HOME_NAMES.get();
    }

    public static boolean overwriteHomeRequiresConfirm() {
        return OVERWRITE_HOME_REQUIRES_CONFIRM.get();
    }

    public static boolean rtpEnabled() {
        return RTP_ENABLED.get();
    }

    public static int rtpMinRadius() {
        int min = Math.max(64, RTP_MIN_RADIUS.get());
        int max = rtpMaxRadius();
        return Math.min(min, max);
    }

    public static int rtpMaxRadius() {
        return Math.max(64, RTP_MAX_RADIUS.get());
    }

    public static int rtpCooldownSeconds() {
        return Math.max(0, RTP_COOLDOWN_SECONDS.get());
    }

    public static int rtpMaxAttempts() {
        return Math.max(1, Math.min(256, RTP_MAX_ATTEMPTS.get()));
    }

    public static int rtpSearchHeightMin() {
        return Math.max(-64, Math.min(512, RTP_SEARCH_HEIGHT_MIN.get()));
    }

    public static int rtpSearchHeightMax() {
        return Math.max(-64, Math.min(512, RTP_SEARCH_HEIGHT_MAX.get()));
    }

    public static boolean rtpAvoidLava() {
        return RTP_AVOID_LAVA.get();
    }

    public static boolean rtpAvoidWater() {
        return RTP_AVOID_WATER.get();
    }

    public static boolean rtpAvoidPowderSnow() {
        return RTP_AVOID_POWDER_SNOW.get();
    }

    public static boolean rtpAvoidCactus() {
        return RTP_AVOID_CACTUS.get();
    }

    public static boolean rtpAvoidDeepDark() {
        return RTP_AVOID_DEEP_DARK.get();
    }

    public static boolean rtpAvoidHighRadiation() {
        return RTP_AVOID_HIGH_RADIATION.get();
    }

    public static boolean rtpRequireSafeSurface() {
        return RTP_REQUIRE_SAFE_SURFACE.get();
    }

    public static List<String> rtpAllowedDimensions() {
        return List.copyOf(RTP_ALLOWED_DIMENSIONS.get());
    }

    public static List<String> rtpBlockedDimensions() {
        return List.copyOf(RTP_BLOCKED_DIMENSIONS.get());
    }

    public static boolean backEnabled() {
        return BACK_ENABLED.get();
    }

    public static boolean allowBackAfterDeath() {
        return ALLOW_BACK_AFTER_DEATH.get();
    }

    public static boolean allowBackAfterTeleport() {
        return ALLOW_BACK_AFTER_TELEPORT.get();
    }

    public static int backCooldownSeconds() {
        return Math.max(0, BACK_COOLDOWN_SECONDS.get());
    }

    public static boolean storeBackOnHome() {
        return STORE_BACK_ON_HOME.get();
    }

    public static boolean storeBackOnSpawn() {
        return STORE_BACK_ON_SPAWN.get();
    }

    public static boolean storeBackOnRtp() {
        return STORE_BACK_ON_RTP.get();
    }

    public static boolean spawnEnabled() {
        return SPAWN_ENABLED.get();
    }

    public static boolean spawnUseWorldSpawn() {
        return SPAWN_USE_WORLD_SPAWN.get();
    }

    public static boolean allowSpawnCommand() {
        return ALLOW_SPAWN_COMMAND.get();
    }

    public static int spawnCooldownSeconds() {
        return Math.max(0, SPAWN_COOLDOWN_SECONDS.get());
    }

    public static boolean allowCrossDimensionSpawn() {
        return ALLOW_CROSS_DIMENSION_SPAWN.get();
    }

    public static boolean tpaEnabled() {
        return TPA_ENABLED.get();
    }

    public static int tpaTimeoutSeconds() {
        return Math.max(5, Math.min(300, TPA_TIMEOUT_SECONDS.get()));
    }

    public static int tpaCooldownSeconds() {
        return Math.max(0, Math.min(3600, TPA_COOLDOWN_SECONDS.get()));
    }

    public static boolean warpsEnabled() {
        return WARPS_ENABLED.get();
    }

    public static boolean opsBypassCooldowns() {
        return OPS_BYPASS_COOLDOWNS.get();
    }

    public static boolean opsBypassHomeLimit() {
        return OPS_BYPASS_HOME_LIMIT.get();
    }

    public static int permissionLevelSetspawn() {
        return Math.max(0, Math.min(4, PERMISSION_LEVEL_SETSPAWN.get()));
    }

    public static int permissionLevelWarps() {
        return Math.max(0, Math.min(4, PERMISSION_LEVEL_WARPS.get()));
    }

    public static int permissionLevelAdminReload() {
        return Math.max(0, Math.min(4, PERMISSION_LEVEL_ADMIN_RELOAD.get()));
    }

    public static int rtpSearchesPerTick() {
        return Math.max(1, Math.min(64, RTP_SEARCHES_PER_TICK.get()));
    }

    public static int rtpMaxChunkGenerations() {
        return Math.max(1, Math.min(64, RTP_MAX_CHUNK_GENERATIONS.get()));
    }

    public static boolean rtpAsyncQueueEnabled() {
        return RTP_ASYNC_QUEUE_ENABLED.get();
    }

    public static boolean useRuntimeGuardIfAvailable() {
        return USE_RUNTIMEGUARD_IF_AVAILABLE.get();
    }

    public static String messagePrefix() {
        String v = MESSAGE_PREFIX.get();
        return v == null || v.isBlank() ? "ECHO PlayerCore" : v.strip();
    }

    public static boolean useEchoStyleMessages() {
        return USE_ECHO_STYLE_MESSAGES.get();
    }
}
