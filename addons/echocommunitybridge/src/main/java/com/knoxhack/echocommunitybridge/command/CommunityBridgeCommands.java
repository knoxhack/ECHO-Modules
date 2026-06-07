package com.knoxhack.echocommunitybridge.command;

import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echocommunitybridge.config.CommunityBridgeConfig;
import com.knoxhack.echocommunitybridge.discord.DiscordGatewayClient;
import com.knoxhack.echocommunitybridge.discord.DiscordMessageQueue;
import com.knoxhack.echocommunitybridge.server.OfficialChatService;
import com.knoxhack.echocommunitybridge.server.ServerStatusService;
import com.knoxhack.echocommunitybridge.server.StatusHttpServer;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public final class CommunityBridgeCommands {
    private CommunityBridgeCommands() {
    }

    public static void onRegisterCommands(Object event) {
        var dispatcher = EchoBackendCommandEventBridge.dispatcher(event);
        if (dispatcher == null) {
            return;
        }
        dispatcher.register(Commands.literal("echobridge")
                .executes(CommunityBridgeCommands::status)
                .then(Commands.literal("status")
                        .executes(CommunityBridgeCommands::status))
                .then(Commands.literal("testdiscord")
                        .requires(CommunityBridgeCommands::hasBridgePermission)
                        .executes(CommunityBridgeCommands::testDiscord))
                .then(Commands.literal("reload")
                        .requires(CommunityBridgeCommands::hasBridgePermission)
                        .executes(CommunityBridgeCommands::reload)));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        ServerStatusService.INSTANCE.refresh();
        ctx.getSource().sendSuccess(() -> Component.literal("ECHO Bridge // "
                + ServerStatusService.INSTANCE.summaryLine()
                + " HTTP=" + (StatusHttpServer.INSTANCE.running() ? "running" : "stopped")
                + " DiscordQueue=" + DiscordMessageQueue.INSTANCE.pendingCount()
                + " DiscordGateway=" + DiscordGatewayClient.INSTANCE.statusLabel()
                + " ChatClients=" + OfficialChatService.INSTANCE.socketCount()
                + " ChatHistory=" + OfficialChatService.INSTANCE.historyCount()), false);
        return 1;
    }

    private static int testDiscord(CommandContext<CommandSourceStack> ctx) {
        boolean queued = DiscordMessageQueue.INSTANCE.enqueueStatus(":satellite: ECHO Community Bridge test message.");
        ctx.getSource().sendSuccess(() -> Component.literal(queued
                ? "ECHO Bridge // Discord test message queued."
                : "ECHO Bridge // Discord is disabled, missing a token/channel, or queue is full."), false);
        return queued ? 1 : 0;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        StatusHttpServer.INSTANCE.restart();
        DiscordGatewayClient.INSTANCE.restart();
        ServerStatusService.INSTANCE.refresh();
        ctx.getSource().sendSuccess(() -> Component.literal("ECHO Bridge // Reloaded public status HTTP service."), false);
        return 1;
    }

    private static boolean hasBridgePermission(CommandSourceStack source) {
        int level = CommunityBridgeConfig.COMMAND_PERMISSION_LEVEL.get();
        if (level <= 0) {
            return true;
        }
        if (level == 1) {
            return source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
        }
        if (level == 2) {
            return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        }
        if (level == 3) {
            return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
        }
        return source.permissions().hasPermission(Permissions.COMMANDS_OWNER);
    }
}
