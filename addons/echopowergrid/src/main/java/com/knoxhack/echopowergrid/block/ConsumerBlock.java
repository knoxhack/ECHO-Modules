package com.knoxhack.echopowergrid.block;

import com.knoxhack.echopowergrid.block.entity.PowerConsumerBlockEntity;
import com.knoxhack.echopowergrid.grid.PowerNetworkManager;
import com.knoxhack.echopowergrid.menu.PowerNodeMenu;
import com.knoxhack.echopowergrid.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

public class ConsumerBlock extends Block implements EntityBlock {
    private final long demand;

    public ConsumerBlock(long demand, Properties properties) {
        super(properties);
        this.demand = demand;
    }

    public long getDemand() { return demand; }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerConsumerBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == ModBlockEntities.CONSUMER.get() ? (l, p, s, be) -> PowerConsumerBlockEntity.tick(l, p, s, (PowerConsumerBlockEntity) be) : null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof PowerConsumerBlockEntity con) {
            if (player.isShiftKeyDown()) {
                con.onUse(player);
            } else {
                player.openMenu(PowerNodeMenu.provider(level, pos), pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof PowerConsumerBlockEntity con && player.isShiftKeyDown()) {
            con.onUse(player);
        } else {
            player.openMenu(PowerNodeMenu.provider(level, pos), pos);
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
