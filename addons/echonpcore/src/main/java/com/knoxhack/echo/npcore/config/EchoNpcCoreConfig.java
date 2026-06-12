package com.knoxhack.echo.npcore.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;
import java.util.Locale;

public final class EchoNpcCoreConfig {
    public static final EchoNativeConfigSpec SPEC;
    public static final EchoNativeConfigSpec.BooleanValue USE_SCREENCORE_NPC_SCREENS;
    public static final EchoNativeConfigSpec.BooleanValue FALLBACK_TO_CLASSIC_NPC_SCREENS;
    public static final EchoNativeConfigSpec.BooleanValue REPLACE_VANILLA_VILLAGERS;
    public static final EchoNativeConfigSpec.BooleanValue REPLACE_WANDERING_TRADER;
    public static final EchoNativeConfigSpec.BooleanValue REPLACE_ZOMBIE_VILLAGERS;
    public static final EchoNativeConfigSpec.BooleanValue REPLACE_ON_SPAWN;
    public static final EchoNativeConfigSpec.BooleanValue REPLACE_ON_CHUNK_LOAD;
    public static final EchoNativeConfigSpec.BooleanValue REPLACE_ON_FIRST_INTERACT;
    public static final EchoNativeConfigSpec.BooleanValue PRESERVE_CUSTOM_NAME;
    public static final EchoNativeConfigSpec.BooleanValue PRESERVE_PROFESSION;
    public static final EchoNativeConfigSpec.DoubleValue INTERACTION_RANGE;
    public static final EchoNativeConfigSpec.BooleanValue DEBUG_REPLACEMENT_LOGS;
    public static final EchoNativeConfigSpec.BooleanValue DEBUG_PROFILE_LOADING;
    public static final EchoNativeConfigSpec.StringValue CONVERSION_MODE;

    static {
        EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();
        builder.push("screens");
        USE_SCREENCORE_NPC_SCREENS = builder.comment("Prefer ScreenCore-backed NPC screens when echoscreencore is present.")
                .define("useScreenCoreNpcScreens", true);
        FALLBACK_TO_CLASSIC_NPC_SCREENS = builder.comment("Open NPCore's classic screen if ScreenCore is absent or the adapter declines.")
                .define("fallbackToClassicNpcScreens", true);
        builder.pop();

        builder.push("replacement");
        REPLACE_VANILLA_VILLAGERS = builder.define("replaceVanillaVillagers", true);
        REPLACE_WANDERING_TRADER = builder.define("replaceWanderingTrader", true);
        REPLACE_ZOMBIE_VILLAGERS = builder.define("replaceZombieVillagers", false);
        REPLACE_ON_SPAWN = builder.define("replaceOnSpawn", true);
        REPLACE_ON_CHUNK_LOAD = builder.define("replaceOnChunkLoad", true);
        REPLACE_ON_FIRST_INTERACT = builder.define("replaceOnFirstInteract", true);
        PRESERVE_CUSTOM_NAME = builder.define("preserveCustomName", true);
        PRESERVE_PROFESSION = builder.define("preserveProfession", true);
        CONVERSION_MODE = builder.comment("Supported first-pass values: off, convert_on_spawn, convert_on_first_interact.")
                .define("conversionMode", "convert_on_spawn");
        DEBUG_REPLACEMENT_LOGS = builder.define("debugReplacementLogs", true);
        builder.pop();

        builder.push("interaction");
        INTERACTION_RANGE = builder.defineInRange("interactionRange", 5.0D, 1.0D, 32.0D);
        DEBUG_PROFILE_LOADING = builder.define("debugProfileLoading", true);
        builder.pop();
        SPEC = builder.build();
    }

    private EchoNpcCoreConfig() {
    }

    public static boolean bool(EchoNativeConfigSpec.ConfigValue<Boolean> value, boolean fallback) {
        try {
            Boolean read = value.get();
            return read == null ? fallback : read;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static double interactionRange() {
        try {
            return INTERACTION_RANGE.get();
        } catch (RuntimeException exception) {
            return 5.0D;
        }
    }

    public static String conversionMode() {
        try {
            String value = CONVERSION_MODE.get();
            return value == null ? "convert_on_spawn" : value.toLowerCase(Locale.ROOT).trim();
        } catch (RuntimeException exception) {
            return "convert_on_spawn";
        }
    }
}
