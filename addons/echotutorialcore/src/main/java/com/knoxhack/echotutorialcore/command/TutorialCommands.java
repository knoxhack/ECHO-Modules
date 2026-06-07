package com.knoxhack.echotutorialcore.command;

import com.knoxhack.echotutorialcore.api.TutorialCoreApi;
import com.knoxhack.echotutorialcore.api.TutorialGuideMode;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import com.knoxhack.echotutorialcore.network.TutorialNetworking;
import com.knoxhack.echotutorialcore.server.TutorialHintManager;
import com.knoxhack.echotutorialcore.server.TutorialProgressManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class TutorialCommands {
    private TutorialCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("echotutorialcore")
                .then(Commands.literal("guide_mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(java.util.List.of("off", "minimal", "normal", "assisted"), builder))
                                .executes(ctx -> setGuideMode(ctx, ctx.getSource().getPlayerOrException()))))
                .then(Commands.literal("progress")
                        .executes(ctx -> progress(ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                                .executes(ctx -> progress(EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("reset")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> reset(EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("unlock_card")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("cardId", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                TutorialCoreRegistries.allCards().stream().map(c -> c.id().toString()).toList(), builder))
                                        .executes(ctx -> unlockCard(EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "cardId"))))))
                .then(Commands.literal("show_card")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("cardId", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                TutorialCoreRegistries.allCards().stream().map(c -> c.id().toString()).toList(), builder))
                                        .executes(ctx -> showCard(EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "cardId"))))))
                .then(Commands.literal("hint")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("hintId", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                TutorialCoreRegistries.allHints().stream().map(h -> h.id().toString()).toList(), builder))
                                        .executes(ctx -> hint(EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "hintId"))))))
                .then(Commands.literal("list_cards")
                        .executes(TutorialCommands::listCards))
                .then(Commands.literal("list_hints")
                        .executes(TutorialCommands::listHints))
                .then(Commands.literal("debug")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(TutorialCommands::debug))
                .then(Commands.literal("simulate_stuck")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> simulateStuck(EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("reload")
                        .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(TutorialCommands::reload))
        );

        // Simple player-facing /tutorial alias
        dispatcher.register(Commands.literal("tutorial")
                .executes(ctx -> quickProgress(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("cards")
                        .executes(TutorialCommands::listCards))
                .then(Commands.literal("hints")
                        .executes(TutorialCommands::listHints)));
    }

    private static int quickProgress(ServerPlayer player) {
        TutorialPlayerData data = TutorialPlayerData.get(player);
        player.sendSystemMessage(Component.literal("ECHO Tutorial // " + player.getScoreboardName()));
        player.sendSystemMessage(Component.literal("  Guide Mode: " + data.guideMode()));
        player.sendSystemMessage(Component.literal("  Unlocked Cards: " + data.unlockedCardIds().size()));
        player.sendSystemMessage(Component.literal("  Completed Flows: " + data.completedFlowIds().size()));
        player.sendSystemMessage(Component.literal("  Use /tutorial cards or /tutorial hints to browse."));
        return 1;
    }

    private static int setGuideMode(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        String modeName = StringArgumentType.getString(ctx, "mode");
        TutorialGuideMode mode = TutorialGuideMode.byName(modeName);
        TutorialProgressManager.setGuideMode(player, mode);
        ctx.getSource().sendSuccess(() -> Component.literal("Guide mode set to " + mode.name() + "."), false);
        return 1;
    }

    private static int progress(ServerPlayer player) {
        TutorialPlayerData data = TutorialPlayerData.get(player);
        player.sendSystemMessage(Component.literal("ECHO TutorialCore // Progress for " + player.getScoreboardName()));
        player.sendSystemMessage(Component.literal("  Guide Mode: " + data.guideMode()));
        player.sendSystemMessage(Component.literal("  Progress Flags: " + data.progressFlags().size()));
        player.sendSystemMessage(Component.literal("  Unlocked Cards: " + data.unlockedCardIds().size()));
        player.sendSystemMessage(Component.literal("  Completed Flows: " + data.completedFlowIds().size()));
        return 1;
    }

    private static int reset(ServerPlayer player) {
        TutorialProgressManager.resetPlayer(player);
        player.sendSystemMessage(Component.literal("ECHO TutorialCore // Progress reset."));
        return 1;
    }

    private static int unlockCard(ServerPlayer player, String cardIdStr) {
        Identifier cardId = Identifier.tryParse(cardIdStr);
        if (cardId == null) {
            player.sendSystemMessage(Component.literal("Invalid card ID."));
            return 0;
        }
        TutorialCoreApi.unlockCard(player, cardId);
        player.sendSystemMessage(Component.literal("Unlocked card: " + cardId));
        return 1;
    }

    private static int showCard(ServerPlayer player, String cardIdStr) {
        Identifier cardId = Identifier.tryParse(cardIdStr);
        if (cardId == null) {
            player.sendSystemMessage(Component.literal("Invalid card ID."));
            return 0;
        }
        TutorialCoreApi.showCard(player, cardId);
        player.sendSystemMessage(Component.literal("Showing card: " + cardId));
        return 1;
    }

    private static int hint(ServerPlayer player, String hintIdStr) {
        Identifier hintId = Identifier.tryParse(hintIdStr);
        if (hintId == null) {
            player.sendSystemMessage(Component.literal("Invalid hint ID."));
            return 0;
        }
        TutorialCoreApi.showHint(player, hintId);
        player.sendSystemMessage(Component.literal("Showing hint: " + hintId));
        return 1;
    }

    private static int listCards(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("ECHO TutorialCore // Cards (" + TutorialCoreRegistries.cardCount() + "):"), false);
        for (var card : TutorialCoreRegistries.allCards()) {
            ctx.getSource().sendSuccess(() -> Component.literal("  " + card.id() + " [" + card.category() + "]"), false);
        }
        return 1;
    }

    private static int listHints(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("ECHO TutorialCore // Hints (" + TutorialCoreRegistries.hintCount() + "):"), false);
        for (var hint : TutorialCoreRegistries.allHints()) {
            ctx.getSource().sendSuccess(() -> Component.literal("  " + hint.id() + " [" + hint.type() + "]"), false);
        }
        return 1;
    }

    private static int debug(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("ECHO TutorialCore // Debug"), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  Cards: " + TutorialCoreRegistries.cardCount()), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  Hints: " + TutorialCoreRegistries.hintCount()), false);
        ctx.getSource().sendSuccess(() -> Component.literal("  Flows: " + TutorialCoreRegistries.flowCount()), false);
        return 1;
    }

    private static int simulateStuck(ServerPlayer player) {
        TutorialHintManager.sendChatFallback(player, "ECHO-7", "Progress has stalled. Open Terminal > What Now for recommended next steps.");
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        // Data reload listeners are automatic on /reload
        ctx.getSource().sendSuccess(() -> Component.literal("ECHO TutorialCore // Use /reload to trigger data reload."), false);
        return 1;
    }
}
