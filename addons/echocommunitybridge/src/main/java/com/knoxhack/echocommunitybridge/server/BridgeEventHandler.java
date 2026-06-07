package com.knoxhack.echocommunitybridge.server;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echocommunitybridge.config.CommunityBridgeConfig;
import com.knoxhack.echocommunitybridge.discord.DiscordGatewayClient;
import com.knoxhack.echocommunitybridge.discord.DiscordMessageQueue;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class BridgeEventHandler {
    private BridgeEventHandler() {
    }

    public static void onServerStarted(Object event) {
        if (!CommunityBridgeConfig.enabled()) {
            return;
        }
        MinecraftServer server = server(event);
        if (server == null) {
            return;
        }
        ServerStatusService.INSTANCE.setServer(server, true);
        ServerStatusService.INSTANCE.addEvent("server_start", "", "Server online");
        StatusHttpServer.INSTANCE.start();
        DiscordGatewayClient.INSTANCE.start();
        OfficialChatService.INSTANCE.recordSystem("server_start", "Server online");
        if (CommunityBridgeConfig.RELAY_SERVER_LIFECYCLE.get()) {
            DiscordMessageQueue.INSTANCE.enqueueStatus(":green_circle: "
                    + CommunityBridgeConfig.string(CommunityBridgeConfig.SERVER_NAME) + " is online.");
        }
    }

    public static void onServerStopping(Object event) {
        if (!CommunityBridgeConfig.enabled()) {
            return;
        }
        ServerStatusService.INSTANCE.addEvent("server_stop", "", "Server stopping");
        OfficialChatService.INSTANCE.recordSystem("server_stop", "Server stopping");
        ServerStatusService.INSTANCE.markOffline();
        if (CommunityBridgeConfig.RELAY_SERVER_LIFECYCLE.get()) {
            DiscordMessageQueue.INSTANCE.enqueueStatus(":red_circle: "
                    + CommunityBridgeConfig.string(CommunityBridgeConfig.SERVER_NAME) + " is stopping.");
        }
        StatusHttpServer.INSTANCE.stop();
        DiscordGatewayClient.INSTANCE.shutdown();
        DiscordMessageQueue.INSTANCE.shutdown();
    }

    public static void onPlayerLogin(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.playerEventServerPlayer(event);
        if (!CommunityBridgeConfig.enabled() || player == null) {
            return;
        }
        String playerName = player.getScoreboardName();
        ServerStatusService.INSTANCE.playerLoggedIn(player);
        ServerStatusService.INSTANCE.addEvent("join", playerName, "joined");
        OfficialChatService.INSTANCE.recordSystem("join", playerName + " joined the server.");
        if (CommunityBridgeConfig.RELAY_JOIN_LEAVE.get()) {
            DiscordMessageQueue.INSTANCE.enqueueStatus(":arrow_right: **"
                    + ServerStatusService.sanitizeDiscordText(playerName, 32) + "** joined the server.");
        }
    }

    public static void onPlayerLogout(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.playerEventServerPlayer(event);
        if (!CommunityBridgeConfig.enabled() || player == null) {
            return;
        }
        String playerName = player.getScoreboardName();
        ServerStatusService.INSTANCE.playerLoggedOut(player);
        ServerStatusService.INSTANCE.addEvent("leave", playerName, "left");
        OfficialChatService.INSTANCE.recordSystem("leave", playerName + " left the server.");
        if (CommunityBridgeConfig.RELAY_JOIN_LEAVE.get()) {
            DiscordMessageQueue.INSTANCE.enqueueStatus(":arrow_left: **"
                    + ServerStatusService.sanitizeDiscordText(playerName, 32) + "** left the server.");
        }
    }

    public static void onServerChat(Object event) {
        if (!CommunityBridgeConfig.enabled()) {
            return;
        }
        ServerPlayer player = player(event);
        if (player == null) {
            return;
        }
        String playerName = player.getScoreboardName();
        String rawText = rawText(event);
        ServerStatusService.INSTANCE.addEvent("chat", playerName, rawText);
        OfficialChatService.INSTANCE.recordMinecraftChat(playerName, rawText);
        DiscordMessageQueue.INSTANCE.enqueueChat(playerName, rawText);
    }

    public static void onAdvancementEarned(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.advancementServerPlayer(event);
        if (!CommunityBridgeConfig.enabled() || !CommunityBridgeConfig.RELAY_ADVANCEMENTS.get()
                || player == null) {
            return;
        }
        AdvancementHolder advancement = advancement(event);
        Identifier id = advancement == null ? EchoBackendWorldEventBridge.advancementId(event) : advancement.id();
        DisplayInfo display = advancement == null ? null : advancement.value().display().orElse(null);
        if (display != null && display.isHidden() && CommunityBridgeConfig.IGNORE_HIDDEN_ADVANCEMENTS.get()) {
            return;
        }
        if (CommunityBridgeConfig.IGNORE_ROOT_ADVANCEMENTS.get() && isRootAdvancement(id)) {
            return;
        }
        String playerName = player.getScoreboardName();
        String title = advancementTitle(id, display);
        ServerStatusService.INSTANCE.addEvent("advancement", playerName, "earned advancement: " + title);
        OfficialChatService.INSTANCE.recordSystem("advancement", playerName + " earned advancement: " + title);
        DiscordMessageQueue.INSTANCE.enqueueStatus(":medal: **"
                + ServerStatusService.sanitizeDiscordText(playerName, 32)
                + "** earned advancement **"
                + ServerStatusService.sanitizeDiscordText(title, 96)
                + "**.");
    }

    private static boolean isRootAdvancement(Identifier id) {
        return id != null && ("root".equals(id.getPath()) || id.getPath().endsWith("/root"));
    }

    private static String advancementTitle(Identifier id, DisplayInfo display) {
        if (display != null) {
            Component title = display.getTitle();
            if (title != null) {
                String text = title.getString();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return id == null ? "unknown" : id.toString();
    }

    private static MinecraftServer server(Object event) {
        Object value = invokeNoArg(event, "getServer");
        return value instanceof MinecraftServer server ? server : null;
    }

    private static ServerPlayer player(Object event) {
        Object value = invokeNoArg(event, "getPlayer");
        return value instanceof ServerPlayer player ? player : null;
    }

    private static String rawText(Object event) {
        Object value = invokeNoArg(event, "getRawText");
        return value instanceof String text ? text : "";
    }

    private static AdvancementHolder advancement(Object event) {
        Object value = invokeNoArg(event, "getAdvancement");
        return value instanceof AdvancementHolder advancement ? advancement : null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
