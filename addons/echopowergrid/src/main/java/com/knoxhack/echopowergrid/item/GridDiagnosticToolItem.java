package com.knoxhack.echopowergrid.item;

import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.api.EchoPowerNetwork;
import com.knoxhack.echopowergrid.api.EchoEnergyStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class GridDiagnosticToolItem extends Item {
    public GridDiagnosticToolItem(Properties properties) {
        super(properties.stacksTo(1).durability(256));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (player == null) return InteractionResult.SUCCESS;

        boolean sneaking = player.isShiftKeyDown();
        EchoPowerNetwork network = EchoPowerGridApi.getNetwork(level, pos).orElse(null);

        if (network == null) {
            var storage = EchoPowerGridApi.getEnergyStorage(level, pos).orElse(null);
            if (storage != null) {
                player.sendSystemMessage(Component.literal("ECHO GRID DIAG // Local Storage"));
                player.sendSystemMessage(Component.literal("  Stored: " + storage.getEnergyStored() + "/" + storage.getMaxEnergyStored()));
                player.sendSystemMessage(Component.literal("  I/O: " + storage.getMaxInput() + "/" + storage.getMaxOutput()));
            } else {
                player.sendSystemMessage(Component.literal("ECHO GRID DIAG // No power node at target."));
            }
            return InteractionResult.SUCCESS;
        }

        if (sneaking) {
            var snap = network.toSnapshot();
            player.sendSystemMessage(Component.literal("ECHO GRID DIAG // Network " + network.networkId.toString().substring(0, 8)));
            player.sendSystemMessage(Component.literal("  Generation: " + snap.totalGeneration() + " EP/t"));
            player.sendSystemMessage(Component.literal("  Demand: " + snap.totalDemand() + " EP/t"));
            player.sendSystemMessage(Component.literal("  Stored: " + snap.totalStored() + "/" + snap.totalCapacity()));
            player.sendSystemMessage(Component.literal("  State: " + snap.state()));
            player.sendSystemMessage(Component.literal("  Quality: " + snap.quality()));
            player.sendSystemMessage(Component.literal("  Nodes: " + snap.nodeCount()));
        } else {
            var storage = EchoPowerGridApi.getEnergyStorage(level, pos).orElse(null);
            player.sendSystemMessage(Component.literal("ECHO GRID DIAG // Local Node"));
            if (storage != null) {
                player.sendSystemMessage(Component.literal("  Energy: " + storage.getEnergyStored() + "/" + storage.getMaxEnergyStored()));
            }
            player.sendSystemMessage(Component.literal("  Network State: " + network.state));
            player.sendSystemMessage(Component.literal("  Network ID: " + network.networkId.toString().substring(0, 8)));
        }

        return InteractionResult.SUCCESS;
    }
}
