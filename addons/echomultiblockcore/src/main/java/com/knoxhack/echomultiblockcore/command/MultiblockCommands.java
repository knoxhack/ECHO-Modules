package com.knoxhack.echomultiblockcore.command;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.LensMultiblockScan;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.MultiblockIntegrationServices;
import com.knoxhack.echomultiblockcore.api.MultiblockMapMarkerSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockMaterialSummary;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockUpgradeRegistry;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.block.entity.RoboticArmBlockEntity;
import com.knoxhack.echomultiblockcore.content.MultiblockContent;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public final class MultiblockCommands {
    private MultiblockCommands() {
    }

    public static void register(Object event) {
        CommandDispatcher<CommandSourceStack> dispatcher = commandDispatcher(event);
        if (dispatcher == null) {
            return;
        }
        dispatcher.register(Commands.literal("echo_multiblock")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("validate").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return withNearestController(player, controller -> {
                        controller.validateStructure(true);
                        sendLines(player, controller.diagnosticLines());
                        return Command.SINGLE_SUCCESS;
                    });
                }))
                .then(Commands.literal("form").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return withNearestController(player, controller -> {
                        var result = controller.validateStructure(true);
                        if (!result.valid()) {
                            context.getSource().sendFailure(Component.literal("Formation failed: " + result.summaryLine()));
                            sendLines(player, controller.diagnosticLines());
                            return 0;
                        }
                        controller.onStructureFormed();
                        if (!controller.isFormedForOperations()) {
                            context.getSource().sendFailure(Component.literal("Formation failed after validation: " + result.summaryLine()));
                            return 0;
                        }
                        context.getSource().sendSuccess(() -> Component.literal("Formation complete: " + result.summaryLine()), false);
                        return 1;
                    });
                }))
                .then(Commands.literal("break").executes(context -> withNearestController(context.getSource().getPlayerOrException(), controller -> {
                    controller.onStructureBroken();
                    context.getSource().sendSuccess(() -> Component.literal("Runtime broken/cleared."), false);
                    return Command.SINGLE_SUCCESS;
                })))
                .then(Commands.literal("info").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return withNearestController(player, controller -> {
                        sendLines(player, controller.diagnosticLines());
                        return Command.SINGLE_SUCCESS;
                    });
                }))
                .then(Commands.literal("set")
                        .then(Commands.argument("definition", StringArgumentType.word()).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            Identifier id = taskId(StringArgumentType.getString(context, "definition"));
                            return withNearestController(player, controller -> {
                                controller.setDefinitionId(id, player);
                                return Command.SINGLE_SUCCESS;
                            });
                        })))
                .then(Commands.literal("task")
                        .then(Commands.literal("list").executes(context -> listRecipes(context.getSource())))
                        .then(Commands.literal("start")
                                .then(Commands.argument("task", StringArgumentType.word()).executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            return withNearestController(player, controller -> {
                                                Identifier taskId = taskId(StringArgumentType.getString(context, "task"));
                                                controller.queueRecipe(taskId, player);
                                                return Command.SINGLE_SUCCESS;
                                            });
                                        })))
                        .then(Commands.literal("clear").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    return withNearestController(player, controller -> {
                                        controller.clearQueue(player);
                                        return Command.SINGLE_SUCCESS;
                                    });
                                }))
                        .then(Commands.literal("pause").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    return withNearestController(player, controller -> {
                                        controller.pauseQueue(player);
                                        return Command.SINGLE_SUCCESS;
                                    });
                                }))
                        .then(Commands.literal("resume").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    return withNearestController(player, controller -> {
                                        controller.resumeQueue(player);
                                        return Command.SINGLE_SUCCESS;
                                    });
                                }))
                        .then(Commands.literal("retry_blocked").executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    return withNearestController(player, controller -> {
                                        controller.retryBlocked(player);
                                        return Command.SINGLE_SUCCESS;
                                    });
                                })))
                .then(Commands.literal("recipes").executes(context -> listRecipes(context.getSource())))
                .then(Commands.literal("progression")
                        .then(Commands.literal("list").executes(context -> listProgression(context.getSource())))
                        .then(Commands.literal("info")
                                .then(Commands.argument("facility", StringArgumentType.word()).executes(context -> {
                                    Identifier id = taskId(StringArgumentType.getString(context, "facility"));
                                    return progressionInfo(context.getSource(), id);
                                })))
                        .then(Commands.literal("next").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            Optional<MultiblockControllerBlockEntity> controller = nearestController(player);
                            Identifier current = controller.map(MultiblockControllerBlockEntity::getMultiblockId).orElse(null);
                            return progressionNext(context.getSource(), current);
                        })))
                .then(Commands.literal("upgrades")
                        .then(Commands.literal("list").executes(context -> listUpgrades(context.getSource())))
                        .then(Commands.literal("install")
                                .then(Commands.argument("upgrade", StringArgumentType.word()).executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    Identifier id = taskId(StringArgumentType.getString(context, "upgrade"));
                                    return withNearestController(player, controller -> {
                                        controller.installUpgrade(id, player);
                                        return Command.SINGLE_SUCCESS;
                                    });
                                })))
                        .then(Commands.literal("remove_last").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return withNearestController(player, controller -> {
                                controller.removeLastUpgrade(player);
                                return Command.SINGLE_SUCCESS;
                            });
                        })))
                .then(Commands.literal("autobuild").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return withNearestController(player, controller -> {
                        controller.runAutoBuilder(player, 16);
                        return Command.SINGLE_SUCCESS;
                    });
                }))
                .then(Commands.literal("materials")
                        .then(Commands.argument("id", StringArgumentType.word()).executes(context -> {
                            Identifier id = Identifier.parse(StringArgumentType.getString(context, "id"));
                            return printMaterials(context.getSource(), id);
                        })))
                .then(Commands.literal("robotics")
                        .then(Commands.literal("list").executes(context ->
                                withNearestController(context.getSource().getPlayerOrException(), controller -> {
                                    controller.statusSnapshot().roboticArms().forEach(line ->
                                            context.getSource().sendSuccess(() -> Component.literal(line), false));
                                    return Command.SINGLE_SUCCESS;
                                }))))
                .then(Commands.literal("integrations").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("MultiblockCore integrations // terminal="
                            + MultiblockIntegrationServices.terminalProviderCount()
                            + ", scan=" + MultiblockIntegrationServices.scanProviderCount()
                            + ", data=" + MultiblockIntegrationServices.dataCoreProviderCount()
                            + ", map=" + MultiblockIntegrationServices.mapMarkerProviderCount()), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("snapshot").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return withNearestController(player, controller -> {
                        MultiblockRuntimeSnapshot snapshot = controller.runtimeSnapshot();
                        player.sendSystemMessage(Component.literal("Snapshot: " + snapshot.displayName()
                                + " / " + snapshot.state()
                                + " / integrity " + Math.round(snapshot.integrity()) + "%"
                                + " / tasks " + snapshot.taskCount()
                                + " / warnings " + snapshot.warningCount()));
                        return Command.SINGLE_SUCCESS;
                    });
                }))
                .then(Commands.literal("scan").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    Optional<BlockPos> target = nearestIntegrationTarget(player);
                    if (target.isEmpty()) {
                        player.sendSystemMessage(Component.literal("No MultiblockCore scan target found within 8 blocks."));
                        return 0;
                    }
                    Optional<LensMultiblockScan> scan = MultiblockIntegrationServices.scan(player, player.level(), target.get());
                    if (scan.isEmpty()) {
                        player.sendSystemMessage(Component.literal("No scan data available at " + target.get().toShortString() + "."));
                        return 0;
                    }
                    LensMultiblockScan value = scan.get();
                    player.sendSystemMessage(Component.literal("Scan: " + value.structureName()
                            + " / " + value.state()
                            + " / " + Math.round(value.completion() * 100.0D) + "%"));
                    value.missingBlocks().stream().limit(5).forEach(line -> player.sendSystemMessage(Component.literal("- " + line)));
                    value.roboticStatus().stream().limit(5).forEach(line -> player.sendSystemMessage(Component.literal("- " + line)));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("markers").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    List<MultiblockMapMarkerSnapshot> markers = MultiblockIntegrationServices.mapMarkers(player);
                    player.sendSystemMessage(Component.literal("Multiblock markers: " + markers.size()));
                    markers.stream().limit(10).forEach(marker -> player.sendSystemMessage(Component.literal("- "
                            + marker.title() + " / " + marker.state() + " / " + marker.position().toShortString())));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("preview")
                        .then(Commands.argument("id", StringArgumentType.word()).executes(context -> {
                            Identifier id = Identifier.parse(StringArgumentType.getString(context, "id"));
                            context.getSource().sendSuccess(() -> Component.literal("Preview metadata: "
                                    + MultiblockContent.definition(id).map(definition -> definition.displayName()
                                            + " " + definition.width() + "x" + definition.height() + "x" + definition.depth())
                                            .orElse("unknown definition " + id)), false);
                            return Command.SINGLE_SUCCESS;
                        }))));

        dispatcher.register(Commands.literal("echomultiblockcore")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("status").executes(context -> list(context.getSource())))
                .then(Commands.literal("validate").executes(context -> {
                    context.getSource().getServer().getCommands().performPrefixedCommand(context.getSource(), "echo_multiblock validate");
                    return Command.SINGLE_SUCCESS;
                })));
    }

    @SuppressWarnings("unchecked")
    private static CommandDispatcher<CommandSourceStack> commandDispatcher(Object event) {
        if (event == null) {
            return null;
        }
        try {
            Object dispatcher = event.getClass().getMethod("getDispatcher").invoke(event);
            if (dispatcher instanceof CommandDispatcher<?> commandDispatcher) {
                return (CommandDispatcher<CommandSourceStack>) commandDispatcher;
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore command registration event was not available.", exception);
        }
        return null;
    }

    private static int list(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("ECHO MultiblockCore // Definitions: "
                + MultiblockContent.definitions().size() + ", automation recipes: "
                + AutomationRecipeRegistry.all().size() + ".").withStyle(ChatFormatting.AQUA), false);
        MultiblockContent.definitions().stream().limit(12).forEach(definition ->
                source.sendSuccess(() -> Component.literal("- " + definition.id() + " // "
                        + definition.displayName()).withStyle(ChatFormatting.GRAY), false));
        return Command.SINGLE_SUCCESS;
    }

    private static int listRecipes(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("ECHO MultiblockCore // Automation recipes: "
                + AutomationRecipeRegistry.all().size() + ".").withStyle(ChatFormatting.AQUA), false);
        AutomationRecipeRegistry.all().stream().limit(16).forEach(recipe ->
                source.sendSuccess(() -> Component.literal("- " + recipe.id() + " // "
                        + recipe.displayName() + " // " + recipe.category()).withStyle(ChatFormatting.GRAY), false));
        return Command.SINGLE_SUCCESS;
    }

    private static int listUpgrades(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("ECHO MultiblockCore // Upgrades: "
                + MultiblockUpgradeRegistry.all().size() + ".").withStyle(ChatFormatting.AQUA), false);
        MultiblockUpgradeRegistry.all().stream().limit(16).forEach(upgrade ->
                source.sendSuccess(() -> Component.literal("- " + upgrade.id() + " // "
                        + upgrade.displayName() + " // " + upgrade.category()).withStyle(ChatFormatting.GRAY), false));
        return Command.SINGLE_SUCCESS;
    }

    private static int listProgression(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("ECHO MultiblockCore // Facility progression: "
                + MultiblockProgressionRegistry.all().size() + ".").withStyle(ChatFormatting.AQUA), false);
        MultiblockProgressionRegistry.all().stream().limit(16).forEach(progression ->
                source.sendSuccess(() -> Component.literal("- T" + progression.tier() + " "
                        + progression.facilityId() + " // " + progression.title()).withStyle(ChatFormatting.GRAY), false));
        return Command.SINGLE_SUCCESS;
    }

    private static int progressionInfo(net.minecraft.commands.CommandSourceStack source, Identifier facilityId) {
        Optional<MultiblockProgressionDefinition> progression = MultiblockProgressionRegistry.byFacility(facilityId)
                .or(() -> MultiblockProgressionRegistry.byId(facilityId));
        if (progression.isEmpty()) {
            source.sendFailure(Component.literal("Unknown progression facility " + facilityId + "."));
            return 0;
        }
        MultiblockProgressionDefinition value = progression.get();
        source.sendSuccess(() -> Component.literal("T" + value.tier() + " " + value.title())
                .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("Facility: " + value.facilityId()).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Prerequisites: " + joinIds(value.prerequisites(), "none"))
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Featured recipes: " + joinIds(value.featuredRecipes(), "none"))
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Rewards: " + joinIds(value.rewardItems(), "none"))
                .withStyle(ChatFormatting.GRAY), false);
        if (!value.guideText().isBlank()) {
            source.sendSuccess(() -> Component.literal(value.guideText()).withStyle(ChatFormatting.DARK_GRAY), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int progressionNext(net.minecraft.commands.CommandSourceStack source, Identifier currentFacility) {
        List<MultiblockProgressionDefinition> all = MultiblockProgressionRegistry.all();
        if (all.isEmpty()) {
            source.sendFailure(Component.literal("No facility progression entries are loaded."));
            return 0;
        }
        MultiblockProgressionDefinition next = all.get(0);
        if (currentFacility != null) {
            Optional<MultiblockProgressionDefinition> current = MultiblockProgressionRegistry.byFacility(currentFacility);
            if (current.isPresent()) {
                next = all.stream()
                        .filter(value -> value.tier() > current.get().tier())
                        .findFirst()
                        .orElse(current.get());
            }
        }
        MultiblockProgressionDefinition selected = next;
        source.sendSuccess(() -> Component.literal("Next facility: T" + selected.tier() + " "
                + selected.title() + " // " + selected.facilityId()).withStyle(ChatFormatting.AQUA), false);
        if (!selected.featuredRecipes().isEmpty()) {
            source.sendSuccess(() -> Component.literal("Featured recipes: " + joinIds(selected.featuredRecipes(), "none"))
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static String joinIds(List<Identifier> ids, String fallback) {
        if (ids == null || ids.isEmpty()) {
            return fallback;
        }
        return ids.stream().map(Identifier::toString).reduce((left, right) -> left + ", " + right).orElse(fallback);
    }

    private static int printMaterials(net.minecraft.commands.CommandSourceStack source, Identifier id) {
        Optional<com.knoxhack.echomultiblockcore.api.MultiblockDefinition> definition = MultiblockContent.definition(id);
        if (definition.isEmpty()) {
            source.sendFailure(Component.literal("Unknown multiblock definition " + id + "."));
            return 0;
        }
        MultiblockMaterialSummary summary = MultiblockMaterialSummary.from(definition.get());
        source.sendSuccess(() -> Component.literal("Materials for " + definition.get().displayName() + ":")
                .withStyle(ChatFormatting.AQUA), false);
        summary.entries().stream().limit(32).forEach(entry -> source.sendSuccess(() ->
                Component.literal("- " + entry.line()).withStyle(entry.placeable() ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY), false));
        if (summary.entries().size() > 32) {
            source.sendSuccess(() -> Component.literal("... " + (summary.entries().size() - 32) + " more material groups.")
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int withNearestController(ServerPlayer player, ControllerAction action) {
        Optional<MultiblockControllerBlockEntity> controller = nearestController(player);
        if (controller.isEmpty()) {
            player.sendSystemMessage(Component.literal("No MultiblockCore controller found within 8 blocks."));
            return 0;
        }
        return action.run(controller.get());
    }

    private static Optional<MultiblockControllerBlockEntity> nearestController(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();
        return BlockPos.betweenClosedStream(origin.offset(-8, -4, -8), origin.offset(8, 6, 8))
                .map(BlockPos::immutable)
                .filter(pos -> level.getBlockEntity(pos) instanceof MultiblockControllerBlockEntity)
                .min(Comparator.comparingInt(pos -> pos.distManhattan(origin)))
                .map(pos -> (MultiblockControllerBlockEntity) level.getBlockEntity(pos));
    }

    private static Optional<BlockPos> nearestIntegrationTarget(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();
        return BlockPos.betweenClosedStream(origin.offset(-8, -4, -8), origin.offset(8, 6, 8))
                .map(BlockPos::immutable)
                .filter(pos -> level.getBlockEntity(pos) instanceof MultiblockControllerBlockEntity
                        || level.getBlockEntity(pos) instanceof RoboticArmBlockEntity)
                .min(Comparator.comparingInt(pos -> pos.distManhattan(origin)));
    }

    private static void sendLines(ServerPlayer player, Iterable<String> lines) {
        for (String line : lines) {
            player.sendSystemMessage(Component.literal(line));
        }
    }

    private static Identifier taskId(String raw) {
        if (raw.contains(":")) {
            return Identifier.parse(raw);
        }
        return Identifier.fromNamespaceAndPath(EchoMultiblockCore.MODID, raw);
    }

    @FunctionalInterface
    private interface ControllerAction {
        int run(MultiblockControllerBlockEntity controller);
    }
}
