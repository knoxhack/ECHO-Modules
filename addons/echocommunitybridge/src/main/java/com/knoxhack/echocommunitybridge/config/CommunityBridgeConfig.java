package com.knoxhack.echocommunitybridge.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class CommunityBridgeConfig {
    private static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();

    public static final EchoNativeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master enable for the official community bridge.")
            .define("general.enabled", true);

    public static final EchoNativeConfigSpec.ConfigValue<String> SERVER_ID = BUILDER
            .comment("Stable public id for the official server.")
            .define("server.serverId", "official-ashfall", CommunityBridgeConfig::validString);

    public static final EchoNativeConfigSpec.ConfigValue<String> SERVER_NAME = BUILDER
            .comment("Public server display name.")
            .define("server.serverName", "ECHO Ashfall Official", CommunityBridgeConfig::validString);

    public static final EchoNativeConfigSpec.ConfigValue<String> SERVER_MOTD = BUILDER
            .comment("Public server tagline shown in the launcher.")
            .define("server.serverMotd", "Survive. Adapt. Endure.");

    public static final EchoNativeConfigSpec.BooleanValue PUBLIC_STATUS_ENABLED = BUILDER
            .comment("Serve public JSON status from the Minecraft server process.")
            .define("public_status.enabled", true);

    public static final EchoNativeConfigSpec.ConfigValue<String> PUBLIC_STATUS_HOST = BUILDER
            .comment("Host/interface for the public status HTTP server. Use 0.0.0.0 for public binding.")
            .define("public_status.host", "127.0.0.1", CommunityBridgeConfig::validString);

    public static final EchoNativeConfigSpec.IntValue PUBLIC_STATUS_PORT = BUILDER
            .comment("Port for the public status HTTP server.")
            .defineInRange("public_status.port", 47870, 1, 65535);

    public static final EchoNativeConfigSpec.ConfigValue<String> PUBLIC_STATUS_PATH = BUILDER
            .comment("HTTP path that serves the launcher status JSON.")
            .define("public_status.path", "/status.json", CommunityBridgeConfig::validHttpPath);

    public static final EchoNativeConfigSpec.ConfigValue<String> PUBLIC_CORS_ORIGIN = BUILDER
            .comment("CORS origin allowed to fetch public status. Use * for launcher/browser preview.")
            .define("public_status.corsOrigin", "*");

    public static final EchoNativeConfigSpec.BooleanValue SHOW_PLAYER_NAMES = BUILDER
            .comment("Expose current player names in public status JSON.")
            .define("privacy.showPlayerNames", true);

    public static final EchoNativeConfigSpec.IntValue RECENT_EVENT_LIMIT = BUILDER
            .comment("Maximum public recent events retained in status JSON.")
            .defineInRange("privacy.recentEventLimit", 12, 0, 100);

    public static final EchoNativeConfigSpec.BooleanValue DISCORD_ENABLED = BUILDER
            .comment("Enable Discord REST posting.")
            .define("discord.enabled", false);

    public static final EchoNativeConfigSpec.ConfigValue<String> DISCORD_BOT_TOKEN = BUILDER
            .comment("Discord bot token. Prefer ECHO_DISCORD_BOT_TOKEN env var; this value is never logged or exposed.")
            .define("discord.botToken", "");

    public static final EchoNativeConfigSpec.ConfigValue<String> DISCORD_STATUS_CHANNEL_ID = BUILDER
            .comment("Discord channel id for join/leave/server lifecycle events.")
            .define("discord.statusChannelId", "");

    public static final EchoNativeConfigSpec.ConfigValue<String> DISCORD_CHAT_CHANNEL_ID = BUILDER
            .comment("Discord channel id for Minecraft chat relay.")
            .define("discord.chatChannelId", "");

    public static final EchoNativeConfigSpec.ConfigValue<String> DISCORD_INVITE_URL = BUILDER
            .comment("Public Discord invite URL exposed in status JSON.")
            .define("discord.inviteUrl", "");

    public static final EchoNativeConfigSpec.BooleanValue RELAY_MINECRAFT_CHAT = BUILDER
            .comment("Post Minecraft chat to Discord.")
            .define("relay.minecraftChat", true);

    public static final EchoNativeConfigSpec.BooleanValue RELAY_DISCORD_CHAT = BUILDER
            .comment("Listen for Discord chat and relay it into Minecraft and ECHO Chat. Requires Discord Message Content Intent.")
            .define("relay.discordChat", false);

    public static final EchoNativeConfigSpec.BooleanValue RELAY_JOIN_LEAVE = BUILDER
            .comment("Post player join/leave events to Discord.")
            .define("relay.joinLeave", true);

    public static final EchoNativeConfigSpec.BooleanValue RELAY_SERVER_LIFECYCLE = BUILDER
            .comment("Post server start/stop events to Discord.")
            .define("relay.serverLifecycle", true);

    public static final EchoNativeConfigSpec.BooleanValue RELAY_ADVANCEMENTS = BUILDER
            .comment("Post advancement announcements to Discord and public status.")
            .define("relay.advancements", true);

    public static final EchoNativeConfigSpec.BooleanValue IGNORE_HIDDEN_ADVANCEMENTS = BUILDER
            .comment("Skip hidden advancements when relaying advancement announcements.")
            .define("relay.ignoreHiddenAdvancements", true);

    public static final EchoNativeConfigSpec.BooleanValue IGNORE_ROOT_ADVANCEMENTS = BUILDER
            .comment("Skip root/category advancements when relaying advancement announcements.")
            .define("relay.ignoreRootAdvancements", true);

    public static final EchoNativeConfigSpec.IntValue DISCORD_POST_COOLDOWN_MS = BUILDER
            .comment("Minimum delay between Discord REST posts.")
            .defineInRange("discord.postCooldownMs", 1000, 0, 60000);

    public static final EchoNativeConfigSpec.IntValue DISCORD_MAX_QUEUE_SIZE = BUILDER
            .comment("Maximum Discord messages queued before new events are dropped.")
            .defineInRange("discord.maxQueueSize", 256, 1, 5000);

    public static final EchoNativeConfigSpec.BooleanValue LAUNCHER_CHAT_ENABLED = BUILDER
            .comment("Enable the official launcher/Android chat API on the public status HTTP server.")
            .define("launcher_chat.enabled", true);

    public static final EchoNativeConfigSpec.ConfigValue<String> LAUNCHER_CHAT_CHANNEL_ID = BUILDER
            .comment("Launcher community channel id that maps to this Minecraft server.")
            .define("launcher_chat.channelId", "server-ashfall", CommunityBridgeConfig::validString);

    public static final EchoNativeConfigSpec.IntValue LAUNCHER_CHAT_HISTORY_LIMIT = BUILDER
            .comment("Maximum official chat messages retained in memory for launcher and Android history.")
            .defineInRange("launcher_chat.historyLimit", 200, 1, 500);

    public static final EchoNativeConfigSpec.BooleanValue LAUNCHER_CHAT_ALLOW_LAUNCHER = BUILDER
            .comment("Allow launcher clients to post to official chat.")
            .define("launcher_chat.allowLauncher", true);

    public static final EchoNativeConfigSpec.BooleanValue LAUNCHER_CHAT_ALLOW_ANDROID = BUILDER
            .comment("Allow Android clients to post to official chat.")
            .define("launcher_chat.allowAndroid", true);

    public static final EchoNativeConfigSpec.IntValue COMMAND_PERMISSION_LEVEL = BUILDER
            .comment("Operator permission level required for /echobridge admin actions.")
            .defineInRange("commands.permissionLevel", 2, 0, 4);

    public static final EchoNativeConfigSpec SPEC = BUILDER.build();

    private CommunityBridgeConfig() {
    }

    public static boolean enabled() {
        return ENABLED.get();
    }

    public static String discordBotToken() {
        String envToken = System.getenv("ECHO_DISCORD_BOT_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            return envToken.strip();
        }
        return string(DISCORD_BOT_TOKEN);
    }

    public static boolean discordReady() {
        return enabled() && DISCORD_ENABLED.get() && !discordBotToken().isBlank();
    }

    public static boolean discordGatewayReady() {
        return discordReady()
                && RELAY_DISCORD_CHAT.get()
                && !string(DISCORD_CHAT_CHANNEL_ID).isBlank();
    }

    public static boolean launcherChatReady() {
        return enabled()
                && PUBLIC_STATUS_ENABLED.get()
                && LAUNCHER_CHAT_ENABLED.get();
    }

    public static String statusPath() {
        String path = string(PUBLIC_STATUS_PATH);
        return path.startsWith("/") ? path : "/" + path;
    }

    public static String string(EchoNativeConfigSpec.ConfigValue<String> value) {
        String raw = value.get();
        return raw == null ? "" : raw.strip();
    }

    private static boolean validString(Object value) {
        return value instanceof String text && !text.isBlank();
    }

    private static boolean validHttpPath(Object value) {
        return value instanceof String text && text.startsWith("/") && !text.contains(" ");
    }
}
