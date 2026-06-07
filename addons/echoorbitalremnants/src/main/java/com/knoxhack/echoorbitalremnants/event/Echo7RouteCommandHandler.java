package com.knoxhack.echoorbitalremnants.event;

import com.knoxhack.echoorbitalremnants.entity.EchoZeroEntity;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Permission-level 2 QA hooks for the ECHO-7 route and final protocol chain.
 */
public final class Echo7RouteCommandHandler {
    private Echo7RouteCommandHandler() {
    }

    public static void onRegisterCommands(Object event) {
        CommandDispatcher<CommandSourceStack> dispatcher = commandDispatcher(event);
        if (dispatcher == null) {
            return;
        }
        dispatcher.register(
            Commands.literal("echo7route")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("status")
                    .executes(ctx -> status(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("validate")
                    .executes(ctx -> validate(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("unlock")
                    .then(Commands.argument("route", StringArgumentType.word())
                        .executes(ctx -> unlock(
                            ctx.getSource().getPlayerOrException(),
                            StringArgumentType.getString(ctx, "route")))))
                .then(Commands.literal("complete")
                    .executes(ctx -> complete(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("spawn_echo0")
                    .executes(ctx -> spawnEchoZero(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("reset_final")
                    .executes(ctx -> resetFinal(ctx.getSource().getPlayerOrException())))
        );
    }

    @SuppressWarnings("unchecked")
    private static CommandDispatcher<CommandSourceStack> commandDispatcher(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object dispatcher = event.getClass().getMethod("getDispatcher").invoke(event);
            return dispatcher instanceof CommandDispatcher<?> value ? (CommandDispatcher<CommandSourceStack>) value : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static int status(ServerPlayer player) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        tell(player, "orbit=" + progress.lowOrbitReached()
                + " moon=" + progress.lunarSignalInvestigated()
                + " mars=" + progress.marsAshBasinVisited()
                + " europa=" + progress.europaCryoOceanVisited()
                + " saturn=" + progress.saturnRingGraveyardVisited()
                + " titan=" + progress.titanMethaneShelfVisited()
                + " nexus=" + progress.anomalyBeltEntered()
                + " echo0=" + progress.echoZeroEncountered()
                + " reward=" + progress.echoZeroRewardClaimed()
                + " anchors=" + progress.nexusStabilized()
                + " final=" + progress.finalNetworkSealed(), ChatFormatting.AQUA);
        tell(player, "surveys=" + progress.surveyStatus() + " | outpost_charters=" + progress.completedOutpostCharterCount(),
                ChatFormatting.GRAY);
        return Command.SINGLE_SUCCESS;
    }

    private static int validate(ServerPlayer player) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        boolean ok = progress.finalNetworkSealed() || !progress.canSealFinalNetwork();
        tell(player, ok
                ? "Route validation passed for current state."
                : "Route validation warning: all final requirements are present but the final network is not sealed. Press SCAN or run complete.",
                ok ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        return ok ? Command.SINGLE_SUCCESS : 0;
    }

    private static int unlock(ServerPlayer player, String route) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        switch (route.toLowerCase(java.util.Locale.ROOT)) {
            case "orbit" -> {
                progress.markLaunchPrepared(player);
                progress.markLowOrbitReached(player);
            }
            case "station" -> {
                progress.markLowOrbitReached(player);
                progress.restoreStationLifeSupport(player);
            }
            case "moon", "lunar" -> {
                progress.restoreStationLifeSupport(player);
                progress.markLunarSignalInvestigated(player);
            }
            case "mars" -> {
                progress.markLunarSignalInvestigated(player);
                progress.unlockMarsRoute(player);
                progress.markMarsAshBasinVisited(player);
            }
            case "europa" -> {
                progress.unlockMarsRoute(player);
                progress.markMarsAshBasinVisited(player);
                progress.unlockEuropaRoute(player);
                progress.markEuropaCryoOceanVisited(player);
            }
            case "saturn" -> {
                progress.unlockEuropaRoute(player);
                progress.markEuropaCryoOceanVisited(player);
                progress.unlockSaturnRoute(player);
                progress.markSaturnRingGraveyardVisited(player);
            }
            case "titan" -> {
                progress.unlockSaturnRoute(player);
                progress.markSaturnRingGraveyardVisited(player);
                progress.unlockTitanRoute(player);
                progress.markTitanMethaneShelfVisited(player);
            }
            case "nexus" -> {
                progress.unlockTitanRoute(player);
                progress.markTitanMethaneShelfVisited(player);
                progress.unlockDeepSpaceProtocol(player);
                progress.markAnomalyBeltEntered(player);
            }
            case "echo0", "echo-0" -> progress.markEchoZeroEncountered(player);
            default -> {
                tell(player, "Use orbit, station, moon, mars, europa, saturn, titan, nexus, or echo0.", ChatFormatting.RED);
                return 0;
            }
        }
        tell(player, "Unlocked route state: " + route + ".", ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int complete(ServerPlayer player) {
        EchoTerminalProgress.get(player).completeFullArcForQa(player);
        tell(player, "Completed ECHO-7 route arc and sealed the final network for QA.", ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int spawnEchoZero(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        EchoZeroEntity echoZero = ModEntities.ECHO_ZERO.get().create(level, EntitySpawnReason.EVENT);
        if (echoZero == null) {
            tell(player, "Unable to create ECHO-0.", ChatFormatting.RED);
            return 0;
        }
        echoZero.setPos(player.getX() + 3.0D, player.getY(), player.getZ() + 3.0D);
        level.addFreshEntity(echoZero);
        tell(player, "Spawned ECHO-0 nearby.", ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetFinal(ServerPlayer player) {
        EchoTerminalProgress.get(player).resetFinalStateForQa(player);
        tell(player, "Reset ECHO-0, Nexus stabilization, and final-network flags.", ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static void tell(ServerPlayer player, String message, ChatFormatting color) {
        player.sendSystemMessage(Component.literal("[ECHO7 ROUTE] " + message).withStyle(color));
    }
}
