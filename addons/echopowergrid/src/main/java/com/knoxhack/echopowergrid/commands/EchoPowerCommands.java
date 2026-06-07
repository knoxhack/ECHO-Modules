package com.knoxhack.echopowergrid.commands;

import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.api.PowerGridAlert;
import com.knoxhack.echopowergrid.api.PowerGridNetworkSummary;
import com.knoxhack.echopowergrid.api.PowerGridNodeSummary;
import com.knoxhack.echopowergrid.api.PowerGridRouteSummary;
import com.knoxhack.echopowergrid.api.PowerGridSnapshot;
import com.knoxhack.echopowergrid.block.entity.BatteryBlockEntity;
import com.knoxhack.echopowergrid.grid.PowerNetworkManager;
import com.knoxhack.echopowergrid.registry.ModBlocks;
import com.knoxhack.echopowergrid.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class EchoPowerCommands {
    private EchoPowerCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("echo_power")
            .then(Commands.literal("status").executes(EchoPowerCommands::status))
            .then(Commands.literal("inspect").executes(EchoPowerCommands::inspect))
            .then(Commands.literal("networks").executes(EchoPowerCommands::networks))
            .then(Commands.literal("alerts").executes(EchoPowerCommands::alerts))
            .then(Commands.literal("route")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                    .then(Commands.argument("to", BlockPosArgument.blockPos())
                        .executes(EchoPowerCommands::route))))
            .then(Commands.literal("debug_chunk").executes(EchoPowerCommands::debugChunk))
            .then(Commands.literal("give_test_kit")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(EchoPowerCommands::giveTestKit))
            .then(Commands.literal("set_energy")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                    .executes(EchoPowerCommands::setEnergy)))
            .then(Commands.literal("reset_network")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(EchoPowerCommands::resetNetwork))
        );
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be used by a player."));
            return 0;
        }
        var pos = player.blockPosition();
        var level = (ServerLevel) player.level();
        PowerGridSnapshot snap = EchoPowerGridApi.getSnapshot(level, pos);
        source.sendSuccess(() -> Component.literal("ECHO GRID // Server Status"), false);
        source.sendSuccess(() -> Component.literal("  Generation: " + snap.totalGeneration() + " EP/t"), false);
        source.sendSuccess(() -> Component.literal("  Demand: " + snap.totalDemand() + " EP/t"), false);
        source.sendSuccess(() -> Component.literal("  Stored: " + snap.totalStored() + "/" + snap.totalCapacity()), false);
        source.sendSuccess(() -> Component.literal("  State: " + snap.state()), false);
        return 1;
    }

    private static int inspect(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be used by a player."));
            return 0;
        }
        var level = (ServerLevel) player.level();
        BlockPos pos = targetedPowerNode(level, player, player.blockPosition());
        var storage = EchoPowerGridApi.getEnergyStorage(level, pos);
        if (storage.isPresent()) {
            var s = storage.get();
            source.sendSuccess(() -> Component.literal("ECHO GRID // Inspect Target"), false);
            BlockPos finalPos = pos;
            source.sendSuccess(() -> Component.literal("  Position: " + finalPos.toShortString()), false);
            source.sendSuccess(() -> Component.literal("  Energy: " + s.getEnergyStored() + "/" + s.getMaxEnergyStored()), false);
            source.sendSuccess(() -> Component.literal("  Max I/O: " + s.getMaxInput() + "/" + s.getMaxOutput()), false);
        } else {
            BlockPos finalPos = pos;
            PowerGridNodeSummary node = EchoPowerGridApi.loadedNodeSummaries(level).stream()
                    .filter(summary -> summary.pos().equals(finalPos))
                    .findFirst()
                    .orElse(null);
            if (node == null) {
                source.sendFailure(Component.literal("No power node at target."));
            } else {
                source.sendSuccess(() -> Component.literal("ECHO GRID // Inspect Target"), false);
                source.sendSuccess(() -> Component.literal("  Position: " + node.pos().toShortString()), false);
                source.sendSuccess(() -> Component.literal("  Type: " + node.type()), false);
                source.sendSuccess(() -> Component.literal("  Generation/Demand: " + node.localGeneration() + "/" + node.localDemand() + " EP/t"), false);
                source.sendSuccess(() -> Component.literal("  Transfer: " + limit(node.transferLimit())), false);
                source.sendSuccess(() -> Component.literal("  Online: " + node.online() + " | Quality: " + node.quality()), false);
            }
        }
        return 1;
    }

    private static int networks(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var level = source.getLevel();
        var summaries = EchoPowerGridApi.loadedNetworkSummaries(level);
        source.sendSuccess(() -> Component.literal("ECHO GRID // Loaded networks: " + summaries.size()), false);
        for (PowerGridNetworkSummary summary : summaries.stream().limit(12).toList()) {
            source.sendSuccess(() -> Component.literal("  "
                    + summary.networkId().toString().substring(0, 8)
                    + " @ " + summary.anchorPos().toShortString()
                    + " | " + summary.state() + "/" + summary.quality()
                    + " | gen " + summary.totalGeneration() + " EP/t"
                    + " | demand " + summary.totalDemand() + " EP/t"
                    + " | draw " + summary.availablePower() + " EP"
                    + " | nodes " + summary.nodeCount()
                    + " | limit " + limit(summary.transferLimit())), false);
        }
        return summaries.isEmpty() ? 0 : summaries.size();
    }

    private static int alerts(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var level = source.getLevel();
        var alerts = EchoPowerGridApi.alerts(level);
        source.sendSuccess(() -> Component.literal("ECHO GRID // Alerts: " + alerts.size()), false);
        for (PowerGridAlert alert : alerts.stream().limit(12).toList()) {
            source.sendSuccess(() -> Component.literal("  [" + alert.level() + "] " + alert.code()
                    + " @ " + alert.pos().toShortString() + " // " + alert.message()), false);
        }
        return alerts.isEmpty() ? 0 : alerts.size();
    }

    private static int route(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var source = ctx.getSource();
        var level = source.getLevel();
        BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, "to");
        PowerGridRouteSummary route = EchoPowerGridApi.routeSummary(level, from, to);
        source.sendSuccess(() -> Component.literal("ECHO GRID // Route " + from.toShortString() + " -> " + to.toShortString()), false);
        if (route.blocked()) {
            source.sendFailure(Component.literal("  Blocked: " + route.blockedReason()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("  Distance: " + route.cableDistance()
                + " | Transfer: " + limit(route.pathTransferLimit())
                + " | Loss: " + String.format(java.util.Locale.ROOT, "%.2f%%", route.lossPercent())), false);
        return 1;
    }

    private static int debugChunk(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be used by a player."));
            return 0;
        }
        var level = (ServerLevel) player.level();
        var chunkPos = player.blockPosition();
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(chunkPos.offset(-8, -64, -8), chunkPos.offset(8, 320, 8))) {
            if (EchoPowerGridApi.getEnergyStorage(level, pos).isPresent()) {
                count++;
            }
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.literal("ECHO GRID // Power nodes near current position: " + finalCount), false);
        return 1;
    }

    private static int giveTestKit(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be used by a player."));
            return 0;
        }
        player.getInventory().add(new ItemStack(ModBlocks.CREATIVE_POWER_SOURCE.get()));
        player.getInventory().add(new ItemStack(ModBlocks.CREATIVE_POWER_SINK.get()));
        player.getInventory().add(new ItemStack(ModBlocks.POWER_METER.get()));
        player.getInventory().add(new ItemStack(ModBlocks.LOW_VOLTAGE_CABLE.get(), 32));
        player.getInventory().add(new ItemStack(ModBlocks.SMALL_BATTERY_BANK.get()));
        player.getInventory().add(new ItemStack(ModBlocks.TEST_POWER_CONSUMER.get()));
        source.sendSuccess(() -> Component.literal("ECHO GRID // Test kit delivered."), false);
        return 1;
    }

    private static int setEnergy(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be used by a player."));
            return 0;
        }
        long amount = LongArgumentType.getLong(ctx, "amount");
        var level = (ServerLevel) player.level();
        var pos = targetedPowerNode(level, player, player.blockPosition());
        var be = level.getBlockEntity(pos);
        if (be instanceof BatteryBlockEntity bat) {
            bat.setEnergyStored(amount);
            source.sendSuccess(() -> Component.literal("ECHO GRID // Battery energy set to " + amount), false);
        } else {
            source.sendFailure(Component.literal("Target block is not a battery."));
        }
        return 1;
    }

    private static int resetNetwork(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Must be used by a player."));
            return 0;
        }
        var level = (ServerLevel) player.level();
        var pos = player.blockPosition();
        PowerNetworkManager.get(level).markDirty(pos);
        source.sendSuccess(() -> Component.literal("ECHO GRID // Network marked dirty. Rebuild queued."), false);
        return 1;
    }

    private static BlockPos targetedPowerNode(ServerLevel level, ServerPlayer player, BlockPos fallback) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (double d = 0.0D; d <= 6.0D; d += 0.25D) {
            BlockPos pos = BlockPos.containing(eye.add(look.scale(d)));
            if (ModBlocks.isPowerNode(level.getBlockState(pos))) {
                return pos.immutable();
            }
        }
        return fallback == null ? player.blockPosition() : fallback.immutable();
    }

    private static String limit(long value) {
        return value >= Long.MAX_VALUE / 8L ? "unlimited" : value + " EP/t";
    }
}
