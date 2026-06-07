package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.world.ExplorationSiteRegistry;
import com.knoxhack.echoashfallprotocol.world.POIScannerService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerPlayer;

/**
 * Permission-level 2 QA tools for exploration site polish.
 */
public final class ExplorationPoiCommandHandler {

    private ExplorationPoiCommandHandler() {
    }

    public static void onRegisterCommands(Object event) {
        Object dispatcher = eventValue(event, "getDispatcher");
        if (dispatcher instanceof CommandDispatcher<?> commandDispatcher) {
            registerUnchecked(commandDispatcher);
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("echopoi")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("list")
                    .executes(ctx -> list(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("nearest")
                    .executes(ctx -> nearest(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("validate")
                    .executes(ctx -> validate(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("profile")
                    .then(Commands.argument("site", StringArgumentType.word())
                        .executes(ctx -> profile(
                            ctx.getSource().getPlayerOrException(),
                            StringArgumentType.getString(ctx, "site")))))
                .then(Commands.literal("mark")
                    .then(Commands.argument("site", StringArgumentType.word())
                        .then(Commands.argument("state", StringArgumentType.word())
                            .executes(ctx -> mark(
                                ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "site"),
                                StringArgumentType.getString(ctx, "state"))))))
        );
    }

    private static int list(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("[ECHO POI] Exploration profiles:")
                .withStyle(ChatFormatting.AQUA));
        for (ExplorationSiteRegistry.SiteProfile site : ExplorationSiteRegistry.allSorted()) {
            player.sendSystemMessage(Component.literal(" - " + site.id() + " | " + site.route()
                    + " | " + site.hazardName() + " | " + site.displayName())
                    .withStyle(ChatFormatting.GRAY));
        }
        return ExplorationSiteRegistry.all().size();
    }

    private static int nearest(ServerPlayer player) {
        POIScannerService.ScanHit hit = POIScannerService.scan(player);
        if (hit == null) {
            player.sendSystemMessage(Component.literal("[ECHO POI] No tagged POI structure in range.")
                    .withStyle(ChatFormatting.YELLOW));
            return 0;
        }
        player.sendSystemMessage(Component.literal("[ECHO POI] Structure: " + hit.structureId())
                .withStyle(ChatFormatting.DARK_GRAY));
        for (Component line : POIScannerService.createReadout(hit)) {
            player.sendSystemMessage(line);
        }
        return 1;
    }

    private static int profile(ServerPlayer player, String siteId) {
        ExplorationSiteRegistry.SiteProfile site = ExplorationSiteRegistry.getOrFallback(siteId);
        player.sendSystemMessage(Component.literal("[ECHO POI] " + site.id() + " - " + site.displayName())
                .withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("Route: " + site.route()
                + " | Kind: " + site.kind()
                + " | Risk: " + site.dangerLevel().getDisplayName()
                + " | Hazard: " + site.hazardName()).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("Prep: " + site.prepHint()).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("Objective: " + site.objective()).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal("Loot: echoashfallprotocol:chests/" + site.lootTable()
                + " | Structures: " + String.join(", ", site.structureIds())).withStyle(ChatFormatting.DARK_GRAY));
        return 1;
    }

    private static int mark(ServerPlayer player, String siteId, String stateName) {
        QuestData.POIObjectiveState state;
        try {
            state = QuestData.POIObjectiveState.valueOf(stateName.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            player.sendSystemMessage(Component.literal("[ECHO POI] Unknown state. Use SCANNED, ENTERED, CACHE_LOOTED, DATA_RECOVERED, SAMPLE_RECOVERED, BOSS_DEFEATED, CLEARED, or REWARD_CLAIMED.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        String normalized = ExplorationSiteRegistry.normalize(siteId);
        QuestData quest = QuestData.get(player);
        quest.recordPOIState(normalized, state);
        QuestData.saveAndSync(player, quest);
        player.sendSystemMessage(Component.literal("[ECHO POI] Marked " + normalized + " as " + state.name() + ".")
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int validate(ServerPlayer player) {
        var warnings = ExplorationSiteRegistry.validationWarnings();
        if (warnings.isEmpty()) {
            player.sendSystemMessage(Component.literal("[ECHO POI] Registry validation passed.")
                    .withStyle(ChatFormatting.GREEN));
            return 1;
        }
        player.sendSystemMessage(Component.literal("[ECHO POI] Registry validation warnings:")
                .withStyle(ChatFormatting.YELLOW));
        for (String warning : warnings) {
            player.sendSystemMessage(Component.literal(" - " + warning).withStyle(ChatFormatting.GRAY));
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
