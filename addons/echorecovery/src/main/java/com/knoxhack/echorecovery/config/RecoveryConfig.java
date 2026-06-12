package com.knoxhack.echorecovery.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class RecoveryConfig {
    private RecoveryConfig() {}

    public static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();
    public static final EchoNativeConfigSpec SPEC;

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_GRAVES;
    public static final EchoNativeConfigSpec.BooleanValue STORE_ITEMS;
    public static final EchoNativeConfigSpec.BooleanValue STORE_ARMOR;
    public static final EchoNativeConfigSpec.BooleanValue STORE_OFFHAND;
    public static final EchoNativeConfigSpec.BooleanValue STORE_XP;
    public static final EchoNativeConfigSpec.BooleanValue STORE_CURIOS;
    public static final EchoNativeConfigSpec.BooleanValue KEEP_HOTBAR_ORDER;
    public static final EchoNativeConfigSpec.IntValue MAX_GRAVES_PER_PLAYER;
    public static final EchoNativeConfigSpec.IntValue GRAVE_EXPIRATION_MINUTES;
    public static final EchoNativeConfigSpec.BooleanValue DROP_OVERFLOW_ITEMS;
    public static final EchoNativeConfigSpec.BooleanValue DELETE_EMPTY_GRAVES;
    public static final EchoNativeConfigSpec.BooleanValue CREATE_GRAVE_ON_PVP;
    public static final EchoNativeConfigSpec.BooleanValue CREATE_GRAVE_ON_VOID_DEATH;
    public static final EchoNativeConfigSpec.BooleanValue CREATE_GRAVE_ON_LAVA_DEATH;

    public static final EchoNativeConfigSpec.BooleanValue SAFE_PLACEMENT;
    public static final EchoNativeConfigSpec.IntValue SAFE_PLACEMENT_RADIUS;
    public static final EchoNativeConfigSpec.EnumValue<VoidDeathMode> VOID_DEATH_MODE;
    public static final EchoNativeConfigSpec.BooleanValue LAVA_DEATH_SAFE_PLACEMENT;
    public static final EchoNativeConfigSpec.BooleanValue CREATE_TEMPORARY_PLATFORM;
    public static final EchoNativeConfigSpec.BooleanValue FALLBACK_TO_SPAWN;
    public static final EchoNativeConfigSpec.BooleanValue FALLBACK_TO_BED;

    public static final EchoNativeConfigSpec.BooleanValue OWNER_ONLY;
    public static final EchoNativeConfigSpec.BooleanValue TEAM_ACCESS;
    public static final EchoNativeConfigSpec.IntValue PUBLIC_ACCESS_AFTER_MINUTES;
    public static final EchoNativeConfigSpec.BooleanValue ADMIN_BYPASS;
    public static final EchoNativeConfigSpec.BooleanValue GRAVE_THEFT;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_GRAVE_BREAKING;
    public static final EchoNativeConfigSpec.BooleanValue EXPLOSION_PROOF;
    public static final EchoNativeConfigSpec.BooleanValue FIREPROOF;
    public static final EchoNativeConfigSpec.BooleanValue WITHER_PROOF;
    public static final EchoNativeConfigSpec.BooleanValue MOB_GRIEF_PROOF;

    public static final EchoNativeConfigSpec.BooleanValue GRAVE_KEY_ENABLED;
    public static final EchoNativeConfigSpec.BooleanValue GRAVE_KEY_REQUIRED;
    public static final EchoNativeConfigSpec.BooleanValue GRAVE_KEY_CONSUMED;
    public static final EchoNativeConfigSpec.BooleanValue GRAVE_KEY_CRAFTABLE;
    public static final EchoNativeConfigSpec.BooleanValue RECOVERY_COMPASS_ENABLED;
    public static final EchoNativeConfigSpec.BooleanValue RECOVERY_COMPASS_CRAFTABLE;
    public static final EchoNativeConfigSpec.BooleanValue RECOVERY_COMPASS_TRACKS_SELECTED_GRAVE;
    public static final EchoNativeConfigSpec.BooleanValue RECOVERY_COMPASS_WORKS_CROSS_DIMENSION;

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_DEATH_HISTORY;
    public static final EchoNativeConfigSpec.IntValue MAX_DEATH_HISTORY;

    public static final EchoNativeConfigSpec.BooleanValue REMOTE_RECOVERY_ENABLED;
    public static final EchoNativeConfigSpec.EnumValue<DifficultyPreset> DIFFICULTY_PRESET;

    static {
        BUILDER.push("grave");
        ENABLE_GRAVES = BUILDER.define("enable_graves", true);
        STORE_ITEMS = BUILDER.define("store_items", true);
        STORE_ARMOR = BUILDER.define("store_armor", true);
        STORE_OFFHAND = BUILDER.define("store_offhand", true);
        STORE_XP = BUILDER.define("store_xp", true);
        STORE_CURIOS = BUILDER.define("store_curios", true);
        KEEP_HOTBAR_ORDER = BUILDER.define("keep_hotbar_order", true);
        MAX_GRAVES_PER_PLAYER = BUILDER.defineInRange("max_graves_per_player", 10, 1, 100);
        GRAVE_EXPIRATION_MINUTES = BUILDER.defineInRange("grave_expiration_minutes", -1, -1, 10080);
        DROP_OVERFLOW_ITEMS = BUILDER.define("drop_overflow_items", true);
        DELETE_EMPTY_GRAVES = BUILDER.define("delete_empty_graves", true);
        CREATE_GRAVE_ON_PVP = BUILDER.define("create_grave_on_pvp", true);
        CREATE_GRAVE_ON_VOID_DEATH = BUILDER.define("create_grave_on_void_death", true);
        CREATE_GRAVE_ON_LAVA_DEATH = BUILDER.define("create_grave_on_lava_death", true);
        BUILDER.pop();

        BUILDER.push("placement");
        SAFE_PLACEMENT = BUILDER.define("safe_placement", true);
        SAFE_PLACEMENT_RADIUS = BUILDER.defineInRange("safe_placement_radius", 8, 1, 64);
        VOID_DEATH_MODE = BUILDER.defineEnum("void_death_mode", VoidDeathMode.LAST_SAFE_POSITION);
        LAVA_DEATH_SAFE_PLACEMENT = BUILDER.define("lava_death_safe_placement", true);
        CREATE_TEMPORARY_PLATFORM = BUILDER.define("create_temporary_platform", false);
        FALLBACK_TO_SPAWN = BUILDER.define("fallback_to_spawn", true);
        FALLBACK_TO_BED = BUILDER.define("fallback_to_bed", true);
        BUILDER.pop();

        BUILDER.push("protection");
        OWNER_ONLY = BUILDER.define("owner_only", true);
        TEAM_ACCESS = BUILDER.define("team_access", false);
        PUBLIC_ACCESS_AFTER_MINUTES = BUILDER.defineInRange("public_access_after_minutes", -1, -1, 10080);
        ADMIN_BYPASS = BUILDER.define("admin_bypass", true);
        GRAVE_THEFT = BUILDER.define("grave_theft", false);
        ALLOW_GRAVE_BREAKING = BUILDER.define("allow_grave_breaking", false);
        EXPLOSION_PROOF = BUILDER.define("explosion_proof", true);
        FIREPROOF = BUILDER.define("fireproof", true);
        WITHER_PROOF = BUILDER.define("wither_proof", true);
        MOB_GRIEF_PROOF = BUILDER.define("mob_grief_proof", true);
        BUILDER.pop();

        BUILDER.push("items");
        GRAVE_KEY_ENABLED = BUILDER.define("grave_key_enabled", true);
        GRAVE_KEY_REQUIRED = BUILDER.define("grave_key_required", false);
        GRAVE_KEY_CONSUMED = BUILDER.define("grave_key_consumed", false);
        GRAVE_KEY_CRAFTABLE = BUILDER.define("grave_key_craftable", true);
        RECOVERY_COMPASS_ENABLED = BUILDER.define("recovery_compass_enabled", true);
        RECOVERY_COMPASS_CRAFTABLE = BUILDER.define("recovery_compass_craftable", true);
        RECOVERY_COMPASS_TRACKS_SELECTED_GRAVE = BUILDER.define("recovery_compass_tracks_selected_grave", true);
        RECOVERY_COMPASS_WORKS_CROSS_DIMENSION = BUILDER.define("recovery_compass_works_cross_dimension", false);
        BUILDER.pop();

        BUILDER.push("history");
        ENABLE_DEATH_HISTORY = BUILDER.define("enable_death_history", true);
        MAX_DEATH_HISTORY = BUILDER.defineInRange("max_death_history", 25, 1, 100);
        BUILDER.pop();

        BUILDER.push("misc");
        REMOTE_RECOVERY_ENABLED = BUILDER.define("remote_recovery_enabled", false);
        DIFFICULTY_PRESET = BUILDER.defineEnum("difficulty_preset", DifficultyPreset.FORGIVING);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public enum VoidDeathMode {
        LAST_SAFE_POSITION, BED, WORLD_SPAWN, TEAM_SPAWN, DISABLED
    }

    public enum DifficultyPreset {
        CUSTOM, FORGIVING, VANILLA_PLUS, ADVENTURE, RPG, HARDCORE, SKYBLOCK, HORROR, ASHFALL
    }
}
