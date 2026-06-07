package com.knoxhack.echopowergrid.block;

import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.api.PowerGridSnapshot;
import com.knoxhack.echopowergrid.grid.PowerNetworkManager;
import com.knoxhack.echopowergrid.menu.PowerNodeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

public class MeterBlock extends Block {
    public MeterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown()) {
            player.openMenu(PowerNodeMenu.provider(level, pos), pos);
            return InteractionResult.SUCCESS;
        }
        PowerGridSnapshot snap = EchoPowerGridApi.getSnapshot(level, pos);
        player.sendSystemMessage(Component.literal("ECHO GRID // Power Meter"));
        player.sendSystemMessage(Component.literal("  Gen: " + snap.totalGeneration() + " EP/t"));
        player.sendSystemMessage(Component.literal("  Demand: " + snap.totalDemand() + " EP/t"));
        player.sendSystemMessage(Component.literal("  Stored: " + snap.totalStored() + "/" + snap.totalCapacity()));
        player.sendSystemMessage(Component.literal("  Available: " + snap.availablePower() + " EP/t"));
        player.sendSystemMessage(Component.literal("  State: " + snap.state()));
        player.sendSystemMessage(Component.literal("  Nodes: " + snap.nodeCount()));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown()) {
            player.openMenu(PowerNodeMenu.provider(level, pos), pos);
            return InteractionResult.SUCCESS;
        }
        PowerGridSnapshot snap = EchoPowerGridApi.getSnapshot(level, pos);
        player.sendSystemMessage(Component.literal("ECHO GRID // Power Meter"));
        player.sendSystemMessage(Component.literal("  Gen: " + snap.totalGeneration() + " EP/t"));
        player.sendSystemMessage(Component.literal("  Demand: " + snap.totalDemand() + " EP/t"));
        player.sendSystemMessage(Component.literal("  Stored: " + snap.totalStored() + "/" + snap.totalCapacity()));
        player.sendSystemMessage(Component.literal("  Available: " + snap.availablePower() + " EP/t"));
        player.sendSystemMessage(Component.literal("  State: " + snap.state()));
        player.sendSystemMessage(Component.literal("  Nodes: " + snap.nodeCount()));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            PowerNetworkManager.get(level).onBlockPlaced(pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide()) {
            PowerNetworkManager.get(level).onBlockPlaced(pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        PowerNetworkManager.get(level).onBlockRemoved(pos);
    }
}
