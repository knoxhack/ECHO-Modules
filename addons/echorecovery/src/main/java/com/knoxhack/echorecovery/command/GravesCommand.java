package com.knoxhack.echorecovery.command;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import com.knoxhack.echorecovery.block.entity.GraveBlockEntity;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.data.RecoveryWorldData;
import com.knoxhack.echorecovery.grave.GraveManager;
import com.knoxhack.echorecovery.integration.RecoveryDataCoreIntegration;
import com.knoxhack.echorecovery.integration.RecoveryIntegrationDispatcher;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import java.util.Comparator;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class GravesCommand {
    private GravesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("graves")
            .then(Commands.literal("list")
                .executes(ctx -> listGraves(ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("locate")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> locateGrave(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("recover")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> recoverGrave(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> deleteGrave(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("history")
                .executes(ctx -> showHistory(ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("share")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> shareGrave(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player")))))
            .then(Commands.literal("team")
                .executes(ctx -> teamGraves(ctx.getSource().getPlayerOrException())))
            .then(Commands.literal("debug")
                .executes(ctx -> debugInfo(ctx.getSource()))
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)))
            .then(Commands.literal("reload")
                .executes(ctx -> reloadConfig(ctx.getSource()))
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)))
            .then(Commands.literal("admin")
                .requires(src -> src.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("list")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> adminList(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("restore")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ctx -> adminRestore(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "id"))))))
                .then(Commands.literal("delete")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ctx -> adminDelete(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "id")))))))
            .executes(ctx -> listGraves(ctx.getSource().getPlayerOrException())));
    }

    private static int listGraves(ServerPlayer player) {
        List<RecoveryWorldData.GraveEntry> graves = activeGraves(player);
        if (graves.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.list.empty"));
            return 0;
        }
        player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.list.header"));
        for (RecoveryWorldData.GraveEntry grave : graves) {
            String dim = grave.dimension();
            long ageMinutes = (System.currentTimeMillis() - grave.createdAt()) / 60000L;
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.list.entry",
                grave.graveId().toString().substring(0, 8),
                grave.pos().getX(), grave.pos().getY(), grave.pos().getZ(),
                dim, ageMinutes));
        }
        return graves.size();
    }

    private static int locateGrave(ServerPlayer player, String id) {
        RecoveryWorldData.GraveLookup lookup = RecoveryWorldData.findLoaded(player, player.getUUID(), id);
        if (lookup.ambiguous()) {
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.failure.ambiguous"));
            return 0;
        }
        if (lookup.entry().isPresent()) {
            RecoveryWorldData.GraveEntry grave = lookup.entry().get();
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.locate",
                grave.pos().getX(), grave.pos().getY(), grave.pos().getZ(), grave.dimension()));
            return 1;
        }
        player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.failure.not_found"));
        return 0;
    }

    private static int recoverGrave(ServerPlayer player, String id) {
        if (!RecoveryConfig.REMOTE_RECOVERY_ENABLED.get()) {
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.recover.disabled"));
            return 0;
        }
        RecoveryWorldData.GraveLookup lookup = RecoveryWorldData.findLoaded(player, player.getUUID(), id);
        if (lookup.ambiguous()) {
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.failure.ambiguous"));
            return 0;
        }
        if (lookup.entry().isPresent() && lookup.level() instanceof ServerLevel targetLevel) {
            RecoveryWorldData.GraveEntry entry = lookup.entry().get();
            if (!(targetLevel.getBlockEntity(entry.pos()) instanceof GraveBlockEntity grave)) {
                player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.failure.unloaded"));
                return 0;
            }
            if (GraveManager.accessGrave(grave, player, false) != com.knoxhack.echorecovery.grave.GraveAccessResult.ALLOWED) {
                player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.failure.access_denied"));
                return 0;
            }
            RecoveryIntegrations.remoteRecoveryRequested(player, grave.snapshot());
            RecoveryIntegrations.requestDelivery(player, grave.snapshot())
                    .ifPresent(message -> player.sendSystemMessage(Component.translatable(
                            "commands.echorecovery.graves.recover.delivery", message)));
            boolean recovered = GraveManager.recoverGrave(grave, player);
            RecoveryIntegrations.remoteRecoveryCompleted(player, grave.snapshot(), recovered);
            if (recovered) {
                RecoveryDataCoreIntegration.recordRemoteRecovered(player);
                player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.recover.success"));
                return 1;
            }
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.recover.incomplete"));
            return 0;
        }
        player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.failure.not_found"));
        return 0;
    }

    private static int deleteGrave(ServerPlayer player, String id) {
        RecoveryWorldData.GraveLookup lookup = RecoveryWorldData.findLoaded(player, player.getUUID(), id);
        if (lookup.ambiguous()) {
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.failure.ambiguous"));
            return 0;
        }
        if (lookup.entry().isPresent() && lookup.level() instanceof ServerLevel targetLevel) {
            RecoveryWorldData.GraveEntry entry = lookup.entry().get();
            RecoveryWorldData data = RecoveryWorldData.getOrCreate(targetLevel);
            if (targetLevel.getBlockEntity(entry.pos()) instanceof GraveBlockEntity grave) {
                RecoveryIntegrations.graveDeleted(player, grave.snapshot());
                grave.setRecovered(true);
            }
            targetLevel.removeBlock(entry.pos(), false);
            data.removeGrave(player.getUUID(), entry.pos());
            RecoveryIntegrationDispatcher.onGraveDeleted(player, entry.pos(), entry.graveId().toString());
            RecoveryDataCoreIntegration.recordDeleted(player);
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.delete.success"));
            return 1;
        }
        player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.failure.not_found"));
        return 0;
    }

    private static int showHistory(ServerPlayer player) {
        List<RecoveryWorldData.DeathRecord> history = deathHistory(player);
        if (history.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.history.empty"));
            return 0;
        }
        player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.history.header"));
        for (RecoveryWorldData.DeathRecord record : history) {
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.history.entry",
                record.cause(), record.pos().getX(), record.pos().getY(), record.pos().getZ(), record.dimension()));
        }
        return history.size();
    }

    private static int shareGrave(ServerPlayer player, ServerPlayer target) {
        List<RecoveryWorldData.GraveEntry> graves = activeGraves(player);
        if (graves.isEmpty()) {
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.share.empty"));
            return 0;
        }
        RecoveryWorldData.GraveEntry latest = graves.get(graves.size() - 1);
        if (levelFor(player, latest) instanceof ServerLevel targetLevel
                && targetLevel.getBlockEntity(latest.pos()) instanceof GraveBlockEntity grave) {
            grave.shareWith(target.getUUID());
            RecoveryWorldData.getOrCreate(targetLevel).shareGrave(player.getUUID(), latest.graveId(), grave.sharedPlayers());
            RecoveryDataCoreIntegration.recordShared(player);
            player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.share.success", target.getScoreboardName()));
            return 1;
        }
        player.sendSystemMessage(Component.translatable("commands.echorecovery.graves.failure.unloaded"));
        return 0;
    }

    private static int teamGraves(ServerPlayer player) {
        boolean enabled = RecoveryConfig.TEAM_ACCESS.get();
        player.sendSystemMessage(Component.translatable(enabled
            ? "commands.echorecovery.graves.team.enabled"
            : "commands.echorecovery.graves.team.disabled"));
        return enabled ? 1 : 0;
    }

    private static int debugInfo(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("commands.echorecovery.graves.debug",
            RecoveryConfig.ENABLE_GRAVES.get(), RecoveryConfig.SAFE_PLACEMENT.get(), RecoveryConfig.OWNER_ONLY.get()), false);
        return 1;
    }

    private static int reloadConfig(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("commands.echorecovery.graves.reload"), false);
        return 1;
    }

    private static int adminList(CommandSourceStack source, ServerPlayer target) {
        List<RecoveryWorldData.GraveEntry> graves = activeGraves(target);
        source.sendSuccess(() -> Component.translatable("commands.echorecovery.graves.admin.list.header",
            target.getScoreboardName(), graves.size()), false);
        for (RecoveryWorldData.GraveEntry grave : graves) {
            source.sendSuccess(() -> Component.translatable("commands.echorecovery.graves.admin.list.entry",
                    grave.graveId().toString().substring(0, 8), grave.pos().toShortString(), grave.dimension()), false);
        }
        return graves.size();
    }

    private static int adminRestore(CommandSourceStack source, ServerPlayer target, String id) {
        RecoveryWorldData.GraveLookup lookup = RecoveryWorldData.findLoaded(target, target.getUUID(), id);
        if (lookup.ambiguous()) {
            source.sendFailure(Component.translatable("commands.echorecovery.graves.failure.ambiguous"));
            return 0;
        }
        if (lookup.entry().isPresent() && lookup.level() instanceof ServerLevel targetLevel) {
            RecoveryWorldData.GraveEntry entry = lookup.entry().get();
            if (!(targetLevel.getBlockEntity(entry.pos()) instanceof GraveBlockEntity grave)) {
                source.sendFailure(Component.translatable("commands.echorecovery.graves.failure.unloaded"));
                return 0;
            }
            if (GraveManager.recoverGrave(grave, target)) {
                RecoveryDataCoreIntegration.recordAdminRestored(target);
                source.sendSuccess(() -> Component.translatable("commands.echorecovery.graves.admin.restore.success", target.getScoreboardName()), false);
                return 1;
            }
            source.sendFailure(Component.translatable("commands.echorecovery.graves.recover.incomplete"));
            return 0;
        }
        source.sendFailure(Component.translatable("commands.echorecovery.graves.failure.not_found"));
        return 0;
    }

    private static int adminDelete(CommandSourceStack source, ServerPlayer target, String id) {
        RecoveryWorldData.GraveLookup lookup = RecoveryWorldData.findLoaded(target, target.getUUID(), id);
        if (lookup.ambiguous()) {
            source.sendFailure(Component.translatable("commands.echorecovery.graves.failure.ambiguous"));
            return 0;
        }
        if (lookup.entry().isPresent() && lookup.level() instanceof ServerLevel targetLevel) {
            RecoveryWorldData.GraveEntry entry = lookup.entry().get();
            RecoveryWorldData data = RecoveryWorldData.getOrCreate(targetLevel);
            if (targetLevel.getBlockEntity(entry.pos()) instanceof GraveBlockEntity grave) {
                RecoveryIntegrations.graveDeleted(target, grave.snapshot());
                grave.setRecovered(true);
            }
            targetLevel.removeBlock(entry.pos(), false);
            data.removeGrave(target.getUUID(), entry.pos());
            RecoveryIntegrationDispatcher.onGraveDeleted(target, entry.pos(), entry.graveId().toString());
            RecoveryDataCoreIntegration.recordDeleted(target);
            source.sendSuccess(() -> Component.translatable("commands.echorecovery.graves.admin.delete.success", target.getScoreboardName()), false);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.echorecovery.graves.failure.not_found"));
        return 0;
    }

    private static List<RecoveryWorldData.GraveEntry> activeGraves(ServerPlayer player) {
        if (player.level().getServer() == null) {
            return List.of();
        }
        java.util.ArrayList<RecoveryWorldData.GraveEntry> graves = new java.util.ArrayList<>();
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            graves.addAll(RecoveryWorldData.getOrCreate(level).getActiveGraves(player.getUUID()));
        }
        graves.sort(Comparator.comparingLong(RecoveryWorldData.GraveEntry::createdAt));
        return List.copyOf(graves);
    }

    private static List<RecoveryWorldData.DeathRecord> deathHistory(ServerPlayer player) {
        if (player.level().getServer() == null) {
            return List.of();
        }
        java.util.ArrayList<RecoveryWorldData.DeathRecord> history = new java.util.ArrayList<>();
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            history.addAll(RecoveryWorldData.getOrCreate(level).getDeathHistory(player.getUUID()));
        }
        history.sort(Comparator.comparingLong(RecoveryWorldData.DeathRecord::time));
        return List.copyOf(history);
    }

    private static ServerLevel levelFor(ServerPlayer player, RecoveryWorldData.GraveEntry entry) {
        Identifier dimensionId = Identifier.tryParse(entry.dimension());
        if (dimensionId == null || player.level().getServer() == null) {
            return (ServerLevel) player.level();
        }
        ServerLevel level = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        return level == null ? (ServerLevel) player.level() : level;
    }
}
