package com.knoxhack.echoashfallprotocol.event;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * OP-only QA hooks for environmental events.
 */
public final class EnvironmentalEventCommandHandler {

    private EnvironmentalEventCommandHandler() {
    }

    public static void onRegisterCommands(Object event) {
        Object dispatcher = eventValue(event, "getDispatcher");
        if (dispatcher instanceof CommandDispatcher<?> commandDispatcher) {
            registerUnchecked(commandDispatcher);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("echoevent")
                .requires(source -> source.getEntity() instanceof ServerPlayer player
                    && source.getServer().getProfilePermissions(new net.minecraft.server.players.NameAndId(player.getGameProfile()))
                        .level().isEqualOrHigherThan(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS))
                .then(Commands.literal("start")
                    .then(Commands.argument("event", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            EnvironmentalEventProfiles.commandAliases().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> startByAlias(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "event")))))
                .then(Commands.literal("clear")
                    .executes(ctx -> clear(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("status")
                    .executes(ctx -> status(ctx.getSource().getPlayerOrException())))
        );
    }

    private static int startByAlias(ServerPlayer player, String alias) {
        return EnvironmentalEventProfiles.byAlias(alias)
                .map(type -> start(player, type))
                .orElseGet(() -> {
                    tell(player, "Unknown environmental event '" + alias + "'. Try: "
                            + String.join(", ", EnvironmentalEventProfiles.commandAliases()) + ".");
                    return 0;
                });
    }

    private static int start(ServerPlayer player, EnvironmentalEventType type) {
        if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            tell(player, "Environmental events can only be forced from the overworld.");
            return 0;
        }

        EnvironmentalEventHandler.forceStartEvent(level, type);
        EnvironmentalEventStatus status = EnvironmentalEventStatus.fromData(EnvironmentalEventData.get(level), level.getGameTime());
        tell(player, "Started " + displayName(type) + ". " + status.counterGuidance());
        return Command.SINGLE_SUCCESS;
    }

    private static int status(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            tell(player, "Environmental event status is only available from the overworld.");
            return 0;
        }
        EnvironmentalEventData data = EnvironmentalEventData.get(level);
        EnvironmentalEventStatus status = EnvironmentalEventStatus.fromData(data, level.getGameTime());
        tell(player, status.qaSummary(data.canTriggerEvent()));
        if (status.active()) {
            tell(player, "Counter: " + status.counterGuidance());
            tell(player, "Impact: " + status.survivalImpact());
        } else {
            tell(player, "Known events: " + String.join(", ", EnvironmentalEventProfiles.commandAliases()) + ".");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int clear(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            tell(player, "Environmental events can only be cleared from the overworld.");
            return 0;
        }

        if (EnvironmentalEventHandler.clearActiveEvent(level)) {
            tell(player, "Cleared active environmental event.");
            return Command.SINGLE_SUCCESS;
        }

        tell(player, "No active environmental event to clear.");
        return 0;
    }

    private static String displayName(EnvironmentalEventType type) {
        return EnvironmentalEventProfiles.hudLabel(type).toLowerCase(java.util.Locale.ROOT);
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
