package com.knoxhack.echodeepreachprotocol.command;

import com.knoxhack.echodeepreachprotocol.season.DeepReachSeason;
import com.knoxhack.echodeepreachprotocol.season.DeepReachSeasonManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Deep Reach debug and info commands.
 */
public final class DeepReachCommands {
    private DeepReachCommands() {
    }

    public static void register(Object event) {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(event);
        if (dispatcher == null) {
            return;
        }

        dispatcher.register(
            Commands.literal("deepreach")
                .then(Commands.literal("season")
                    .executes(ctx -> {
                        DeepReachSeason season = DeepReachSeasonManager.INSTANCE.currentSeason();
                        int remaining = DeepReachSeasonManager.INSTANCE.ticksRemainingInSeason();
                        ctx.getSource().sendSuccess(
                                () -> Component.literal(
                                        "[Deep Reach] Current abyssal season: " + season.displayName()
                                                + " — " + season.description()
                                                + " (about " + (remaining / 20) + " seconds remaining)"),
                                false);
                        return 1;
                    })));
    }

    @SuppressWarnings("unchecked")
    private static CommandDispatcher<CommandSourceStack> dispatcher(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object dispatcher = event.getClass().getMethod("getDispatcher").invoke(event);
            return dispatcher instanceof CommandDispatcher<?> d
                    ? (CommandDispatcher<CommandSourceStack>) d
                    : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
