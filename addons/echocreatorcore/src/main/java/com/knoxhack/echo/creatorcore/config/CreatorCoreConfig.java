package com.knoxhack.echo.creatorcore.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class CreatorCoreConfig {
    public static final EchoNativeConfigSpec COMMON_SPEC;

    public static final EchoNativeConfigSpec.BooleanValue ENABLED;
    public static final EchoNativeConfigSpec.BooleanValue CREATOR_MODE_ENABLED;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_CLIENT_DASHBOARD;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_IN_GAME_EDITING;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_DRAFT_WRITES;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_EXPORTS;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_CODEX_BRIDGE;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_CODEX_REPO_EDITS;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_CODEX_VISUAL_CONTEXT;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_CODEX_PILOT;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_CODEX_PILOT_AUTOPILOT;
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_CODEX_PILOT_WORLD_ACTIONS;
    public static final EchoNativeConfigSpec.BooleanValue REQUIRE_OPERATOR;
    public static final EchoNativeConfigSpec.IntValue OPERATOR_PERMISSION_LEVEL;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_DEBUG_PANELS;
    public static final EchoNativeConfigSpec.BooleanValue PREFER_SCREENCORE_UI;
    public static final EchoNativeConfigSpec.BooleanValue PREFER_TERMINAL_ENTRY;
    public static final EchoNativeConfigSpec.BooleanValue GENERATE_EXAMPLE_DRAFTS;
    public static final EchoNativeConfigSpec.IntValue MAX_DRAFTS;
    public static final EchoNativeConfigSpec.IntValue MAX_DRAFT_FILE_SIZE_KB;
    public static final EchoNativeConfigSpec.ConfigValue<String> DRAFT_ROOT;
    public static final EchoNativeConfigSpec.ConfigValue<String> EXPORT_ROOT;
    public static final EchoNativeConfigSpec.ConfigValue<String> CODEX_BRIDGE_URL;
    public static final EchoNativeConfigSpec.ConfigValue<String> CODEX_BRIDGE_TOKEN;
    public static final EchoNativeConfigSpec.ConfigValue<String> CODEX_WORKSPACE_ROOT;
    public static final EchoNativeConfigSpec.ConfigValue<String> CODEX_MODEL;
    public static final EchoNativeConfigSpec.ConfigValue<String> CODEX_CAPTURE_ROOT;
    public static final EchoNativeConfigSpec.IntValue CODEX_CAPTURE_KEEP;
    public static final EchoNativeConfigSpec.IntValue CODEX_PILOT_MAX_RADIUS;
    public static final EchoNativeConfigSpec.IntValue CODEX_PILOT_MAX_STEPS;
    public static final EchoNativeConfigSpec.ConfigValue<String> CODEX_PILOT_LOG_ROOT;
    public static final EchoNativeConfigSpec.BooleanValue LOG_ADAPTER_STATUS;
    public static final EchoNativeConfigSpec.BooleanValue LOG_UI_OPEN;

    static {
        EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder.define("enabled", true);
        CREATOR_MODE_ENABLED = builder.define("creator_mode_enabled", true);
        ALLOW_CLIENT_DASHBOARD = builder.define("allow_client_dashboard", true);
        REQUIRE_OPERATOR = builder.define("require_operator", true);
        OPERATOR_PERMISSION_LEVEL = builder.defineInRange("operator_permission_level", 2, 0, 4);
        SHOW_DEBUG_PANELS = builder.define("show_debug_panels", false);
        builder.pop();

        builder.push("safety");
        ALLOW_IN_GAME_EDITING = builder.comment("Unlocks editor flows only; draft file writes still require allow_draft_writes.")
                .define("allow_in_game_editing", false);
        ALLOW_DRAFT_WRITES = builder.comment("Allows CreatorCore commands/services to write draft JSON files.")
                .define("allow_draft_writes", false);
        ALLOW_EXPORTS = builder.comment("Allows CreatorCore to export drafts to the configured script/export root.")
                .define("allow_exports", false);
        ALLOW_CODEX_BRIDGE = builder.comment("Allows CreatorCore to contact the local Echo Codex Bridge on localhost.")
                .define("allow_codex_bridge", false);
        ALLOW_CODEX_REPO_EDITS = builder.comment("Allows CreatorCore Codex Studio to start Codex jobs that may edit the configured workspace.")
                .define("allow_codex_repo_edits", false);
        ALLOW_CODEX_VISUAL_CONTEXT = builder.comment("Allows CreatorCore to save and register local screenshots for Codex visual debugging context.")
                .define("allow_codex_visual_context", false);
        ALLOW_CODEX_PILOT = builder.comment("Allows CreatorCore to run the local Codex Pilot dev bot.")
                .define("allow_codex_pilot", false);
        ALLOW_CODEX_PILOT_AUTOPILOT = builder.comment("Allows Codex Pilot to accept bridge/CLI task prompts and execute step-limited autopilot actions.")
                .define("allow_codex_pilot_autopilot", false);
        ALLOW_CODEX_PILOT_WORLD_ACTIONS = builder.comment("Allows Codex Pilot to perform guarded world-changing actions such as placing or breaking blocks.")
                .define("allow_codex_pilot_world_actions", false);
        builder.pop();

        builder.push("integration");
        PREFER_SCREENCORE_UI = builder.define("prefer_screencore_ui", true);
        PREFER_TERMINAL_ENTRY = builder.define("prefer_terminal_entry", true);
        CODEX_BRIDGE_URL = builder.comment("Local Echo Codex Bridge URL. Keep this on localhost.")
                .define("codex_bridge_url", "http://127.0.0.1:47321");
        CODEX_BRIDGE_TOKEN = builder.comment("Optional bearer token for the local Echo Codex Bridge. Leave empty unless the bridge was started with --auth-token.")
                .define("codex_bridge_token", "");
        CODEX_WORKSPACE_ROOT = builder.comment("Workspace root passed to the bridge in job requests.")
                .define("codex_workspace_root", "C:/Github/Echo");
        CODEX_MODEL = builder.comment("Optional model preference sent to local Codex CLI. Empty uses the Codex CLI default.")
                .define("codex_model", "");
        CODEX_CAPTURE_ROOT = builder.comment("Local capture root for Codex Vision screenshots. Relative paths resolve under codex_workspace_root.")
                .define("codex_capture_root", "run/creatorcore/codex_vision/captures");
        CODEX_CAPTURE_KEEP = builder.comment("Maximum local Codex Vision captures to keep.")
                .defineInRange("codex_capture_keep", 25, 1, 1000);
        CODEX_PILOT_MAX_RADIUS = builder.comment("Maximum distance Codex Pilot may move or affect blocks from its anchor.")
                .defineInRange("codex_pilot_max_radius", 32, 1, 256);
        CODEX_PILOT_MAX_STEPS = builder.comment("Maximum bridge/autopilot actions Codex Pilot may claim per polling pass.")
                .defineInRange("codex_pilot_max_steps", 20, 1, 200);
        CODEX_PILOT_LOG_ROOT = builder.comment("Local log root for Codex Pilot JSONL action/event logs. Relative paths resolve under codex_workspace_root.")
                .define("codex_pilot_log_root", "run/creatorcore/codex_pilot");
        LOG_ADAPTER_STATUS = builder.define("log_adapter_status", true);
        LOG_UI_OPEN = builder.define("log_ui_open", true);
        builder.pop();

        builder.push("drafts");
        GENERATE_EXAMPLE_DRAFTS = builder.define("generate_example_drafts", true);
        MAX_DRAFTS = builder.defineInRange("max_drafts", 500, 1, 10000);
        MAX_DRAFT_FILE_SIZE_KB = builder.defineInRange("max_draft_file_size_kb", 512, 1, 16384);
        DRAFT_ROOT = builder.define("draft_root", "config/echo/creatorcore/drafts");
        EXPORT_ROOT = builder.define("export_root", "config/echo/scripts");
        builder.pop();

        COMMON_SPEC = builder.build();
    }

    private CreatorCoreConfig() {
    }

    public static boolean bool(EchoNativeConfigSpec.ConfigValue<Boolean> value, boolean fallback) {
        try {
            Boolean current = value.get();
            return current == null ? fallback : current;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static int integer(EchoNativeConfigSpec.ConfigValue<Integer> value, int fallback) {
        try {
            Integer current = value.get();
            return current == null ? fallback : current;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static String string(EchoNativeConfigSpec.ConfigValue<String> value, String fallback) {
        try {
            String current = value.get();
            return current == null || current.isBlank() ? fallback : current;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
