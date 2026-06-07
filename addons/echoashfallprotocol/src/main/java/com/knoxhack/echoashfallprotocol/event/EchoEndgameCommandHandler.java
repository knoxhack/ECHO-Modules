package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.dimension.ModDimensions;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.endgame.PrefallArchivesArenaService;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
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

/**
 * Permission-level 2 QA hooks for validating the ECHO finale.
 */
public final class EchoEndgameCommandHandler {
    private EchoEndgameCommandHandler() {
    }

    public static void onRegisterCommands(Object event) {
        Object dispatcher = eventValue(event, "getDispatcher");
        if (dispatcher instanceof CommandDispatcher<?> commandDispatcher) {
            registerUnchecked(commandDispatcher);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("echoendgame")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("status")
                    .executes(ctx -> status(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("validate")
                    .executes(ctx -> validate(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("set_path")
                    .then(Commands.argument("path", StringArgumentType.word())
                        .executes(ctx -> setPath(
                            ctx.getSource().getPlayerOrException(),
                            StringArgumentType.getString(ctx, "path")))))
                .then(Commands.literal("spawn_warden")
                    .executes(ctx -> spawnWarden(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("reset_archives")
                    .executes(ctx -> resetArchives(ctx.getSource().getPlayerOrException())))
        );
    }

    private static int status(ServerPlayer player) {
        PostNexusData data = PostNexusData.get(player);
        NexusWorldData worldData = NexusWorldData.get(player.level().getServer().overworld());
        ServerLevel archives = player.level().getServer().getLevel(ModDimensions.PREFALL_ARCHIVES);

        tell(player, "Player path=" + data.getSelectedPath()
                + " archives=" + data.hasEnteredArchives()
                + " warden=" + data.isWardenDefeated()
                + " wardenReward=" + data.isWardenRewardClaimed()
                + " epilogue=" + data.isEpilogueComplete()
                + " finalReward=" + data.isFinalRewardClaimed(), ChatFormatting.AQUA);
        tell(player, "World state=" + worldData.getState()
                + " choiceMade=" + worldData.hasChoiceBeenMade()
                + " selectedPath=" + selectedPathLabel(data, worldData),
                ChatFormatting.GRAY);
        tell(player, "Return point=" + returnPointLabel(data), ChatFormatting.GRAY);
        tell(player, "Arena " + arenaSummary(archives), ChatFormatting.GRAY);
        return Command.SINGLE_SUCCESS;
    }

    private static int validate(ServerPlayer player) {
        PostNexusData data = PostNexusData.get(player);
        NexusWorldData worldData = NexusWorldData.get(player.level().getServer().overworld());
        ServerLevel archives = player.level().getServer().getLevel(ModDimensions.PREFALL_ARCHIVES);
        PrefallArchivesArenaService.ArenaReport report = archives == null ? null : PrefallArchivesArenaService.inspectArena(archives);
        boolean pathPresent = data.hasMadeChoice() || worldData.hasChoiceBeenMade();
        boolean ok = archives != null && pathPresent && (report == null || report.duplicateWardenCount() == 0);
        tell(player, ok
                ? "Endgame validation passed. path=" + selectedPathLabel(data, worldData) + " " + arenaSummary(archives)
                : "Endgame validation failed. pathPresent=" + pathPresent + " archivesLoaded=" + (archives != null)
                        + " " + arenaSummary(archives),
                ok ? ChatFormatting.GREEN : ChatFormatting.RED);
        return ok ? Command.SINGLE_SUCCESS : 0;
    }

    private static int setPath(ServerPlayer player, String pathName) {
        PostNexusData.NexusPath path = parsePath(pathName);
        boolean reset = "reset".equalsIgnoreCase(pathName) || "none".equalsIgnoreCase(pathName);
        if (path == PostNexusData.NexusPath.NONE && !reset) {
            tell(player, "Use restore, destroy, control, or reset.", ChatFormatting.RED);
            return 0;
        }

        PostNexusData data = PostNexusData.get(player);
        data.setSelectedPath(path);
        data.setArchivesEntered(false);
        data.setWardenDefeated(false);
        data.setWardenRewardClaimed(false);
        data.setFinalRewardClaimed(false);
        data.setEpilogueComplete(false);
        PostNexusData.saveAndSync(player, data);

        NexusWorldData.get(player.level().getServer().overworld()).setChoice(toWorldState(path), player.blockPosition(), player.getName().getString());
        tell(player, reset ? "Reset ECHO endgame path state." : "Set ECHO endgame path to " + path.name() + ".", ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int spawnWarden(ServerPlayer player) {
        ServerLevel archives = player.level().getServer().getLevel(ModDimensions.PREFALL_ARCHIVES);
        if (archives == null) {
            tell(player, "Pre-Fall Archives dimension is not loaded.", ChatFormatting.RED);
            return 0;
        }

        PostNexusData data = PostNexusData.get(player);
        PrefallArchivesArenaService.repairArenaShell(archives, data.getSelectedPath());
        int removed = PrefallArchivesArenaService.cleanupDuplicateWardens(archives);
        boolean spawned = PrefallArchivesArenaService.spawnWardenIfMissing(archives);
        tell(player, (spawned ? "Spawned The Warden." : "The Warden is already present.")
                + " duplicateCleanup=" + removed + " " + arenaSummary(archives), ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetArchives(ServerPlayer player) {
        ServerLevel archives = player.level().getServer().getLevel(ModDimensions.PREFALL_ARCHIVES);
        if (archives == null) {
            tell(player, "Pre-Fall Archives dimension is not loaded.", ChatFormatting.RED);
            return 0;
        }

        PostNexusData data = PostNexusData.get(player);
        int removed = PrefallArchivesArenaService.resetArena(archives, data.getSelectedPath(), true);
        data.setArchivesEntered(false);
        data.setWardenDefeated(false);
        data.setWardenRewardClaimed(false);
        data.setFinalRewardClaimed(false);
        data.setEpilogueComplete(false);
        data.clearArchivesReturnPoint();
        PostNexusData.saveAndSync(player, data);
        tell(player, "Archives reset. removedWardens=" + removed + " " + arenaSummary(archives), ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static String arenaSummary(ServerLevel archives) {
        if (archives == null) {
            return "arenaLoaded=false";
        }
        PrefallArchivesArenaService.ArenaReport report = PrefallArchivesArenaService.inspectArena(archives);
        return "arenaLoaded=true"
                + " wardens=" + report.wardenCount()
                + " activePlayers=" + report.activePlayerCount()
                + " duplicateWardens=" + report.duplicateWardenCount()
                + " activeFight=" + report.activeFight()
                + " shellReady=" + report.shellReady()
                + " arenaReady=" + report.ready();
    }

    private static String returnPointLabel(PostNexusData data) {
        if (!data.hasArchivesReturnPoint()) {
            return "none";
        }
        return data.getArchivesReturnDimension()
                + " @ " + String.format(java.util.Locale.ROOT, "%.1f %.1f %.1f",
                        data.getArchivesReturnX(),
                        data.getArchivesReturnY(),
                        data.getArchivesReturnZ());
    }

    private static String selectedPathLabel(PostNexusData data, NexusWorldData worldData) {
        if (data.hasMadeChoice()) {
            return data.getSelectedPath().name();
        }
        return switch (worldData.getState()) {
            case RESTORED -> "RESTORE(world)";
            case DESTROYED -> "DESTROY(world)";
            case CONTROLLED -> "CONTROL(world)";
            case NORMAL -> "NONE";
        };
    }

    private static PostNexusData.NexusPath parsePath(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "restore", "restored" -> PostNexusData.NexusPath.RESTORE;
            case "destroy", "destroyed" -> PostNexusData.NexusPath.DESTROY;
            case "control", "controlled" -> PostNexusData.NexusPath.CONTROL;
            case "reset", "none" -> PostNexusData.NexusPath.NONE;
            default -> PostNexusData.NexusPath.NONE;
        };
    }

    private static NexusWorldData.WorldState toWorldState(PostNexusData.NexusPath path) {
        return switch (path) {
            case RESTORE -> NexusWorldData.WorldState.RESTORED;
            case DESTROY -> NexusWorldData.WorldState.DESTROYED;
            case CONTROL -> NexusWorldData.WorldState.CONTROLLED;
            case NONE -> NexusWorldData.WorldState.NORMAL;
        };
    }

    private static void tell(ServerPlayer player, String message, ChatFormatting color) {
        player.sendSystemMessage(Component.literal("[ECHO ENDGAME] " + message).withStyle(color));
    }

    @SuppressWarnings("unchecked")
    private static void registerUnchecked(CommandDispatcher<?> dispatcher) {
        register((CommandDispatcher<CommandSourceStack>) dispatcher);
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
