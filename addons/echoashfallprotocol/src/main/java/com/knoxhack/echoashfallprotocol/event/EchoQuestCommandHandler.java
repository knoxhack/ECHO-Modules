package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.echo.Mission;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;

/**
 * Minimal OP-only recovery command for forcing Echo mission progression.
 */
public final class EchoQuestCommandHandler {

    private EchoQuestCommandHandler() {
    }

    public static void onRegisterCommands(Object event) {
        Object dispatcher = eventValue(event, "getDispatcher");
        if (dispatcher instanceof CommandDispatcher<?> commandDispatcher) {
            registerUnchecked(commandDispatcher);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("echoquest")
                .requires(source -> source.getEntity() instanceof ServerPlayer player
                    && source.getServer().getProfilePermissions(new net.minecraft.server.players.NameAndId(player.getGameProfile()))
                        .level().isEqualOrHigherThan(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS))
                .then(Commands.literal("complete")
                    .executes(ctx -> completeCurrent(ctx.getSource().getPlayerOrException())))
        );
    }

    private static int completeCurrent(ServerPlayer player) {
        QuestData quest = QuestData.get(player);
        if (quest.repairMissionState(player)) {
            QuestData.saveAndSync(player, quest);
        }

        Mission current = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
        if (current == null) {
            tell(player, "No current mission found to complete.");
            return 0;
        }

        if (quest.isMissionCompleted(current.id())) {
            tell(player, "No incomplete current mission found. Echo progression is already at the end.");
            QuestData.syncToClient(player);
            return 0;
        }

        if ("reach_decision".equals(current.id()) && !PostNexusData.get(player).hasMadeChoice()) {
            tell(player, "Choose a Nexus path first with /nexus restore, /nexus destroy, or /nexus control.");
            return 0;
        }

        quest.completeMission(player, current.id(), Collections.emptyList());
        quest.clearTurnInReminder(current.id());
        if (current.id().endsWith("_epilogue")) {
            PostNexusData post = PostNexusData.get(player);
            post.setEpilogueComplete(true);
            PostNexusData.saveAndSync(player, post);
        }
        quest.repairMissionState(player);
        QuestData.saveAndSync(player, quest);

        Mission next = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
        if (next != null && !quest.isMissionCompleted(next.id())) {
            tell(player, "Force-completed " + current.objectiveText() + ". Current: " + next.objectiveText() + ".");
        } else {
            tell(player, "Force-completed " + current.objectiveText() + ". All Echo missions are complete.");
        }

        return Command.SINGLE_SUCCESS;
    }

    private static void tell(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("[ECHO-7] " + message));
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
