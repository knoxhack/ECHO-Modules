package com.knoxhack.echopowergrid.block;

import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.api.PowerGridSnapshot;
import com.knoxhack.echopowergrid.block.entity.SubstationBlockEntity;
import com.knoxhack.echopowergrid.menu.PowerNodeMenu;
import com.knoxhack.echopowergrid.registry.ModBlockEntities;
import com.knoxhack.echopowergrid.grid.PowerNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class SubstationBlock extends Block implements EntityBlock {
    public SubstationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SubstationBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == ModBlockEntities.SUBSTATION.get() ? (l, p, s, be) -> SubstationBlockEntity.tick(l, p, s, (SubstationBlockEntity) be) : null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof SubstationBlockEntity substation) {
                substation.cyclePolicy(player);
            }
            PowerGridSnapshot snap = EchoPowerGridApi.getSnapshot(level, pos);
            player.sendSystemMessage(Component.literal("ECHO GRID // Substation Report"));
            player.sendSystemMessage(Component.literal("  Network: " + snap.networkId().toString().substring(0, 8)));
            player.sendSystemMessage(Component.literal("  Generation: " + snap.totalGeneration() + " EP/t"));
            player.sendSystemMessage(Component.literal("  Demand: " + snap.totalDemand() + " EP/t"));
            player.sendSystemMessage(Component.literal("  Stored: " + snap.totalStored() + "/" + snap.totalCapacity() + " EP"));
            player.sendSystemMessage(Component.literal("  State: " + snap.state()));
            player.sendSystemMessage(Component.literal("  Quality: " + snap.quality()));
            if (snap.state().name().contains("BROWNOUT") || snap.state().name().contains("OVERLOAD")) {
                player.sendSystemMessage(Component.literal("  WARNING: Grid unstable."));
            }
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof SubstationBlockEntity substation) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> substation.createMenu(containerId, inventory),
                    Component.literal("Outpost Substation")), pos);
        } else {
            player.openMenu(PowerNodeMenu.provider(level, pos), pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof SubstationBlockEntity substation) {
                substation.cyclePolicy(player);
            }
            PowerGridSnapshot snap = EchoPowerGridApi.getSnapshot(level, pos);
            player.sendSystemMessage(Component.literal("ECHO GRID // Substation Report"));
            player.sendSystemMessage(Component.literal("  Network: " + snap.networkId().toString().substring(0, 8)));
            player.sendSystemMessage(Component.literal("  Generation: " + snap.totalGeneration() + " EP/t"));
            player.sendSystemMessage(Component.literal("  Demand: " + snap.totalDemand() + " EP/t"));
            player.sendSystemMessage(Component.literal("  Stored: " + snap.totalStored() + "/" + snap.totalCapacity() + " EP"));
            player.sendSystemMessage(Component.literal("  State: " + snap.state()));
            player.sendSystemMessage(Component.literal("  Quality: " + snap.quality()));
            if (snap.state().name().contains("BROWNOUT") || snap.state().name().contains("OVERLOAD")) {
                player.sendSystemMessage(Component.literal("  WARNING: Grid unstable."));
            }
        } else {
            if (level.getBlockEntity(pos) instanceof SubstationBlockEntity substation) {
                player.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, p) -> substation.createMenu(containerId, inventory),
                        Component.literal("Outpost Substation")), pos);
            } else {
                player.openMenu(PowerNodeMenu.provider(level, pos), pos);
            }
        }
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
