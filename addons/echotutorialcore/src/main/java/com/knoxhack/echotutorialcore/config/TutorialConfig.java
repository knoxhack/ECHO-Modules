package com.knoxhack.echotutorialcore.config;

import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;

public final class TutorialConfig {
    public static final EchoNativeConfigSpec COMMON_SPEC;
    public static final EchoNativeConfigSpec CLIENT_SPEC;

    // Server
    public static final EchoNativeConfigSpec.BooleanValue ALLOW_ASSISTED_GUIDE_MODE;
    public static final EchoNativeConfigSpec.BooleanValue FORCE_GUIDE_MODE_ENABLED;
    public static final EchoNativeConfigSpec.EnumValue<com.knoxhack.echotutorialcore.api.TutorialGuideMode> FORCED_GUIDE_MODE;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_MISTAKE_DETECTION;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_STUCK_DETECTION;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_RECIPE_LOCK_EXPLANATIONS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_HAZARD_WARNINGS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_FIRST_HOUR_FLOW;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_TOOLTIP_HELP;
    public static final EchoNativeConfigSpec.IntValue MAX_HINTS_PER_MINUTE;
    public static final EchoNativeConfigSpec.IntValue MAX_POPUPS_PER_SESSION;
    public static final EchoNativeConfigSpec.IntValue STUCK_DETECTION_MINUTES;
    public static final EchoNativeConfigSpec.IntValue REPEATED_DEATH_THRESHOLD;
    public static final EchoNativeConfigSpec.IntValue NO_CLEAN_WATER_WARNING_DAY;

    // Client
    public static final EchoNativeConfigSpec.BooleanValue SHOW_TUTORIAL_POPUPS;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_CONTEXTUAL_HINTS;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_DANGER_WARNINGS;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_TOOLTIP_HELP;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_TERMINAL_GUIDE_CARDS;
    public static final EchoNativeConfigSpec.BooleanValue SHOW_TOAST_HINTS;
    public static final EchoNativeConfigSpec.BooleanValue PLAY_TUTORIAL_SOUNDS;
    public static final EchoNativeConfigSpec.EnumValue<com.knoxhack.echotutorialcore.api.TutorialGuideMode> GUIDE_MODE_DEFAULT;
    public static final EchoNativeConfigSpec.DoubleValue HINT_SCALE;
    public static final EchoNativeConfigSpec.IntValue HINT_DURATION_TICKS;

    static {
        EchoNativeConfigSpec.Builder common = new EchoNativeConfigSpec.Builder();
        common.push("server");
        ALLOW_ASSISTED_GUIDE_MODE = common.comment("Allow players to select ASSISTED guide mode.").define("allowAssistedGuideMode", true);
        FORCE_GUIDE_MODE_ENABLED = common.comment("When true, every player uses forcedGuideMode, including OFF.").define("forceGuideModeEnabled", false);
        FORCED_GUIDE_MODE = common.comment("Guide mode to enforce when forceGuideModeEnabled is true.")
                .defineEnum("forcedGuideMode", com.knoxhack.echotutorialcore.api.TutorialGuideMode.OFF);
        ENABLE_MISTAKE_DETECTION = common.comment("Enable mistake detection system.").define("enableMistakeDetection", true);
        ENABLE_STUCK_DETECTION = common.comment("Enable stuck detection system.").define("enableStuckDetection", true);
        ENABLE_RECIPE_LOCK_EXPLANATIONS = common.comment("Enable recipe lock explanation scaffold.").define("enableRecipeLockExplanations", true);
        ENABLE_HAZARD_WARNINGS = common.comment("Enable hazard warning hints.").define("enableHazardWarnings", true);
        ENABLE_FIRST_HOUR_FLOW = common.comment("Enable first-hour onboarding flow.").define("enableFirstHourFlow", true);
        ENABLE_TOOLTIP_HELP = common.comment("Enable expanded tooltip help.").define("enableTooltipHelp", true);
        MAX_HINTS_PER_MINUTE = common.comment("Max contextual hints per minute.").defineInRange("maxHintsPerMinute", 2, 0, 10);
        MAX_POPUPS_PER_SESSION = common.comment("Max popups per play session.").defineInRange("maxPopupsPerSession", 6, 0, 20);
        STUCK_DETECTION_MINUTES = common.comment("Minutes without progress before stuck detection triggers.").defineInRange("stuckDetectionMinutes", 45, 5, 180);
        REPEATED_DEATH_THRESHOLD = common.comment("Deaths to same cause before repeated failure hint.").defineInRange("repeatedDeathThreshold", 3, 2, 10);
        NO_CLEAN_WATER_WARNING_DAY = common.comment("In-game day to warn about lack of clean water.").defineInRange("noCleanWaterWarningDay", 2, 1, 10);
        common.pop();
        COMMON_SPEC = common.build();

        EchoNativeConfigSpec.Builder client = new EchoNativeConfigSpec.Builder();
        client.push("client");
        SHOW_TUTORIAL_POPUPS = client.comment("Show tutorial popups.").define("showTutorialPopups", true);
        SHOW_CONTEXTUAL_HINTS = client.comment("Show contextual hints.").define("showContextualHints", true);
        SHOW_DANGER_WARNINGS = client.comment("Show danger warnings.").define("showDangerWarnings", true);
        SHOW_TOOLTIP_HELP = client.comment("Show tooltip help.").define("showTooltipHelp", true);
        SHOW_TERMINAL_GUIDE_CARDS = client.comment("Show Terminal guide cards.").define("showTerminalGuideCards", true);
        SHOW_TOAST_HINTS = client.comment("Show toast hints.").define("showToastHints", true);
        PLAY_TUTORIAL_SOUNDS = client.comment("Play tutorial notification sounds.").define("playTutorialSounds", true);
        GUIDE_MODE_DEFAULT = client.comment("Default guide mode for new players.").defineEnum("guideModeDefault", com.knoxhack.echotutorialcore.api.TutorialGuideMode.NORMAL);
        HINT_SCALE = client.comment("UI scale for hints.").defineInRange("hintScale", 1.0, 0.5, 2.0);
        HINT_DURATION_TICKS = client.comment("Duration of hint toasts in ticks.").defineInRange("hintDurationTicks", 160, 40, 600);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private TutorialConfig() {}
}
