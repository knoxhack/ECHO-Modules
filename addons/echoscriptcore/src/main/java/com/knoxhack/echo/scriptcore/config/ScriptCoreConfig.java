package com.knoxhack.echo.scriptcore.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class ScriptCoreConfig {
    public static final EchoNativeConfigSpec COMMON_SPEC;
    public static final EchoNativeConfigSpec.BooleanValue ENABLED;
    public static final EchoNativeConfigSpec.BooleanValue GENERATE_EXAMPLES;
    public static final EchoNativeConfigSpec.BooleanValue GENERATE_PUBLIC_EXAMPLES;
    public static final EchoNativeConfigSpec.BooleanValue GENERATE_ASHFALL_EXAMPLES;
    public static final EchoNativeConfigSpec.BooleanValue DEV_MODE;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_RUNTIME_RELOAD;
    public static final EchoNativeConfigSpec.BooleanValue LOG_LOADED_DEFINITIONS;
    public static final EchoNativeConfigSpec.BooleanValue FAIL_PACK_ON_ERROR;
    public static final EchoNativeConfigSpec.IntValue MAX_FILES_PER_RELOAD;
    public static final EchoNativeConfigSpec.IntValue MAX_FILE_SIZE_KB;
    public static final EchoNativeConfigSpec.BooleanValue READ_ONLY_MODE;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_DRAFT_WRITES;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_RUNTIME_MIGRATIONS;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_SCREENCORE_UI_ACTIONS;

    static {
        EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();
        builder.push("scriptcore");
        ENABLED = builder.comment("Enable ScriptCore script loading.").define("enabled", true);
        GENERATE_EXAMPLES = builder.comment("Generate example scripts under config/echo/scripts/examples.").define("generate_examples", true);
        GENERATE_PUBLIC_EXAMPLES = builder.comment("Generate generic public examples.").define("generate_public_examples", true);
        GENERATE_ASHFALL_EXAMPLES = builder.comment("Generate Ashfall example content separately from generic examples.").define("generate_ashfall_examples", true);
        DEV_MODE = builder.comment("Enable development-mode authoring surfaces.").define("dev_mode", false);
        ALLOW_RUNTIME_RELOAD = builder.comment("Allow /echo scriptcore reload commands.").define("allow_runtime_reload", true);
        LOG_LOADED_DEFINITIONS = builder.comment("Log loaded definition ids after reload.").define("log_loaded_definitions", true);
        FAIL_PACK_ON_ERROR = builder.comment("Keep previous registry if a reload has validation errors.").define("fail_pack_on_error", false);
        MAX_FILES_PER_RELOAD = builder.defineInRange("max_files_per_reload", 5000, 1, 100000);
        MAX_FILE_SIZE_KB = builder.defineInRange("max_file_size_kb", 512, 1, 8192);
        READ_ONLY_MODE = builder.comment("Disallow ScriptCore write surfaces.").define("read_only_mode", false);
        ALLOW_DRAFT_WRITES = builder.comment("Allow draft file writes when dev_mode is also true.").define("allow_draft_writes", false);
        ALLOW_RUNTIME_MIGRATIONS = builder.comment("Allow explicit runtime storage migration apply/export commands.").define("allow_runtime_migrations", false);
        ALLOW_SCREENCORE_UI_ACTIONS = builder.comment("Allow trusted ScreenCore UI actions to execute preloaded ScriptCore JSON actions on the server when ScreenCore and NetCore are installed.").define("allow_screencore_ui_actions", false);
        builder.pop();
        COMMON_SPEC = builder.build();
    }

    private ScriptCoreConfig() {
    }

    public static void registerEchoConfig() {
        // Native config host consumes COMMON_SPEC through AdapterCore when available.
    }

    public static boolean draftWritesAllowed() {
        return bool(DEV_MODE, false) && bool(ALLOW_DRAFT_WRITES, false) && !bool(READ_ONLY_MODE, false);
    }

    public static boolean runtimeMigrationsAllowed() {
        return !bool(READ_ONLY_MODE, false) && (bool(DEV_MODE, false) || bool(ALLOW_RUNTIME_MIGRATIONS, false));
    }

    public static boolean screenCoreUiActionsAllowed() {
        return bool(ENABLED, true) && !bool(READ_ONLY_MODE, false) && bool(ALLOW_SCREENCORE_UI_ACTIONS, false);
    }

    public static boolean bool(EchoNativeConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public static int integer(EchoNativeConfigSpec.IntValue value, int fallback) {
        try {
            return value.get();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
