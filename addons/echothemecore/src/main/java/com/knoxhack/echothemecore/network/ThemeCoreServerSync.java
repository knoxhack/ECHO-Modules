package com.knoxhack.echothemecore.network;

import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echothemecore.EchoThemeCore;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import com.knoxhack.echothemecore.content.ThemeRegistry;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ThemeCoreServerSync {
    private static MinecraftServer currentServer;

    private ThemeCoreServerSync() {
    }

    public static void onServerStarted(MinecraftServer server) {
        currentServer = server;
    }

    public static void onServerStopping(MinecraftServer server) {
        if (currentServer == server) {
            currentServer = null;
        }
    }

    public static void onPlayerLogin(ServerPlayer player) {
        if (!ThemeCoreConfig.syncServerTheme()) {
            return;
        }
        Identifier themeId = ThemeRegistry.getThemeFor(player).id();
        sendToPlayer(player, themeId);
        EchoThemeCore.LOGGER.debug("Sent theme sync {} to player {}", themeId, player.getScoreboardName());
    }

    public static void broadcastGlobalTheme(Identifier themeId) {
        if (!ThemeCoreConfig.syncServerTheme()) {
            return;
        }
        MinecraftServer server = currentServer;
        if (server == null) {
            return;
        }
        int sent = EchoNetSend.toAllPlayers(server, new ThemeSyncPacket(themeId), EchoPacketKind.CLIENTBOUND_SYNC);
        EchoThemeCore.LOGGER.debug("Broadcast global theme sync {} to {} players", themeId, sent);
    }

    public static void sendPlayerTheme(ServerPlayer player, Identifier themeId) {
        if (!ThemeCoreConfig.syncServerTheme()) {
            return;
        }
        sendToPlayer(player, themeId);
    }

    public static void sendPlayerTheme(UUID playerId, Identifier themeId) {
        if (playerId == null) {
            return;
        }
        MinecraftServer server = currentServer;
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) {
            sendPlayerTheme(player, themeId);
        }
    }

    private static void sendToPlayer(ServerPlayer player, Identifier themeId) {
        EchoNetSend.toPlayer(player, new PlayerThemeSyncPacket(themeId), EchoPacketKind.CLIENTBOUND_SYNC);
    }
}
