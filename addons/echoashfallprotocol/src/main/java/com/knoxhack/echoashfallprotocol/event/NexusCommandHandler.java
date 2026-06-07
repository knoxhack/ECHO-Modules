package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.endgame.NexusCampaignActions;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles player-visible Nexus status commands.
 */
public class NexusCommandHandler {

    public static void onRegisterCommands(Object event) {
        Object dispatcher = eventValue(event, "getDispatcher");
        if (dispatcher instanceof CommandDispatcher<?> commandDispatcher) {
            registerUnchecked(commandDispatcher);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("nexus")
                        .executes(ctx -> sendStatus(ctx.getSource()))
                        .then(Commands.literal("status")
                                .executes(ctx -> sendStatus(ctx.getSource())))
        );
    }

    private static int sendStatus(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return NexusCampaignActions.sendStatus(player) ? Command.SINGLE_SUCCESS : 0;
        }
        return 0;
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
