package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.research.Perk;
import com.knoxhack.echoashfallprotocol.research.PerkRegistry;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Admin/debug controls for testing the research loop in-game.
 */
public final class ResearchCommandHandler {

    private ResearchCommandHandler() {
    }

    public static void onRegisterCommands(Object event) {
        Object dispatcher = eventValue(event, "getDispatcher");
        if (dispatcher instanceof CommandDispatcher<?> commandDispatcher) {
            registerUnchecked(commandDispatcher);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("echoresearch")
                .requires(source -> source.getEntity() instanceof ServerPlayer player
                    && source.getServer().getProfilePermissions(new net.minecraft.server.players.NameAndId(player.getGameProfile()))
                        .level().isEqualOrHigherThan(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS))
                .then(Commands.literal("points")
                    .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                            .executes(ctx -> addPoints(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "amount")))))
                    .then(Commands.literal("set")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, ResearchData.MAX_POINTS))
                            .executes(ctx -> setPoints(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(Commands.literal("perk")
                    .then(Commands.literal("unlock")
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                            .executes(ctx -> unlockPerk(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id")))))
                    .then(Commands.literal("reset")
                        .executes(ctx -> resetPerks(ctx.getSource().getPlayerOrException()))))
                .then(Commands.literal("schematic")
                    .then(Commands.literal("unlock")
                        .then(Commands.argument("category", StringArgumentType.word())
                            .executes(ctx -> unlockSchematic(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "category")))))
                    .then(Commands.literal("reset")
                        .executes(ctx -> resetSchematics(ctx.getSource().getPlayerOrException()))))
                .then(Commands.literal("reset")
                    .executes(ctx -> resetAll(ctx.getSource().getPlayerOrException())))
        );
    }

    private static int addPoints(ServerPlayer player, int amount) {
        ResearchData research = ResearchData.get(player);
        int added = research.addPoints(amount);
        ResearchData.saveAndSync(player, research);
        tell(player, "Added " + added + " RP. Current: " + research.getPoints() + " RP.");
        return Command.SINGLE_SUCCESS;
    }

    private static int setPoints(ServerPlayer player, int amount) {
        ResearchData research = ResearchData.get(player);
        research.setPoints(amount);
        ResearchData.saveAndSync(player, research);
        tell(player, "Set research points to " + research.getPoints() + " RP.");
        return Command.SINGLE_SUCCESS;
    }

    private static int unlockPerk(ServerPlayer player, String id) {
        Perk perk = PerkRegistry.get(id);
        if (perk == null) {
            tell(player, "Unknown perk id: " + id);
            return 0;
        }

        ResearchData research = ResearchData.get(player);
        boolean unlocked = research.unlockPerk(id);
        ResearchData.saveAndSync(player, research);
        tell(player, (unlocked ? "Unlocked " : "Already unlocked ") + perk.getName() + ".");
        return Command.SINGLE_SUCCESS;
    }

    private static int resetPerks(ServerPlayer player) {
        ResearchData research = ResearchData.get(player);
        research.clearPerks();
        ResearchData.saveAndSync(player, research);
        tell(player, "Cleared all research perks.");
        return Command.SINGLE_SUCCESS;
    }

    private static int unlockSchematic(ServerPlayer player, String category) {
        ResearchData research = ResearchData.get(player);
        String key = category.toLowerCase(Locale.ROOT);
        boolean unlocked = research.unlockSchematic(key);
        ResearchData.saveAndSync(player, research);
        tell(player, (unlocked ? "Unlocked schematic: " : "Already had schematic: ") + key + ".");
        return Command.SINGLE_SUCCESS;
    }

    private static int resetSchematics(ServerPlayer player) {
        ResearchData research = ResearchData.get(player);
        research.clearSchematics();
        ResearchData.saveAndSync(player, research);
        tell(player, "Cleared all schematic unlocks.");
        return Command.SINGLE_SUCCESS;
    }

    private static int resetAll(ServerPlayer player) {
        ResearchData research = ResearchData.get(player);
        research.resetAll();
        ResearchData.saveAndSync(player, research);
        tell(player, "Reset all research state.");
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
