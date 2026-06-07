package com.knoxhack.echopresencelink.config;

import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;
import com.knoxhack.echopresencelink.api.PresenceSanitizer;

public final class PresenceLinkConfig {
    private static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();

    public static final EchoNativeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master enable for ECHO Presence Link on this client.")
            .define("general.enabled", true);
    public static final EchoNativeConfigSpec.ConfigValue<String> DISCORD_APPLICATION_ID = BUILDER
            .comment("Discord Developer application id for Rich Presence. Blank disables Discord IPC quietly.")
            .define("discord.applicationId", "");
    public static final EchoNativeConfigSpec.IntValue UPDATE_INTERVAL_SECONDS = BUILDER
            .comment("Minimum seconds between Rich Presence updates.")
            .defineInRange("discord.updateIntervalSeconds", 15, 10, 300);
    public static final EchoNativeConfigSpec.BooleanValue SHOW_BUTTONS = BUILDER
            .comment("Show public Rich Presence buttons when metadata is configured.")
            .define("discord.showButtons", true);
    public static final EchoNativeConfigSpec.BooleanValue PRIVACY_MODE = BUILDER
            .comment("Hide player-identifying and location-identifying context by default.")
            .define("privacy.privacyMode", true);
    public static final EchoNativeConfigSpec.BooleanValue INCLUDE_WORLD_NAME = BUILDER
            .comment("Allow world names in presence text when privacy mode is enabled.")
            .define("privacy.includeWorldName", false);
    public static final EchoNativeConfigSpec.BooleanValue INCLUDE_SERVER_NAME = BUILDER
            .comment("Allow server names in presence text when privacy mode is enabled.")
            .define("privacy.includeServerName", false);
    public static final EchoNativeConfigSpec.BooleanValue INCLUDE_COORDINATES = BUILDER
            .comment("Allow coordinate text in presence text when privacy mode is enabled. No join secrets are sent in v1.")
            .define("privacy.includeCoordinates", false);
    public static final EchoNativeConfigSpec.ConfigValue<String> PRIMARY_BUTTON_LABEL = BUILDER
            .comment("Optional first Rich Presence button label.")
            .define("buttons.primaryLabel", "");
    public static final EchoNativeConfigSpec.ConfigValue<String> PRIMARY_BUTTON_URL = BUILDER
            .comment("Optional first Rich Presence button URL.")
            .define("buttons.primaryUrl", "");

    public static final EchoNativeConfigSpec SPEC = BUILDER.build();

    private PresenceLinkConfig() {
    }

    public static boolean enabled() {
        return ENABLED.get();
    }

    public static String applicationId() {
        return PresenceSanitizer.text(DISCORD_APPLICATION_ID.get(), 64, "");
    }

    public static int updateIntervalSeconds() {
        return UPDATE_INTERVAL_SECONDS.get();
    }

    public static boolean showButtons() {
        return SHOW_BUTTONS.get();
    }

    public static boolean privacyMode() {
        return PRIVACY_MODE.get();
    }

    public static boolean includeWorldName() {
        return INCLUDE_WORLD_NAME.get();
    }

    public static boolean includeServerName() {
        return INCLUDE_SERVER_NAME.get();
    }

    public static boolean includeCoordinates() {
        return INCLUDE_COORDINATES.get();
    }

    public static String primaryButtonLabel() {
        return PresenceSanitizer.text(PRIMARY_BUTTON_LABEL.get(), 32, "");
    }

    public static String primaryButtonUrl() {
        return PresenceSanitizer.url(PRIMARY_BUTTON_URL.get());
    }
}
