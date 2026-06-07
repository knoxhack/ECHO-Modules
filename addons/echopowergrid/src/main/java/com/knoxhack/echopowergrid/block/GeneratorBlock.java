package com.knoxhack.echopowergrid.block;

import com.knoxhack.echopowergrid.api.GeneratorType;
import com.knoxhack.echopowergrid.block.entity.GeneratorBlockEntity;
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

public class GeneratorBlock extends Block implements EntityBlock {
    private final long generationRate;
    private final long bufferSize;
    private final GeneratorType generatorType;

    public GeneratorBlock(long generationRate, long bufferSize, GeneratorType generatorType, Properties properties) {
        super(properties);
        this.generationRate = generationRate;
        this.bufferSize = bufferSize;
        this.generatorType = generatorType;
    }

    public long getGenerationRate() { return generationRate; }
    public long getBufferSize() { return bufferSize; }
    public GeneratorType getGeneratorType() { return generatorType; }
    public boolean usesFuel() { return generatorType == GeneratorType.FUEL_BURNER; }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == ModBlockEntities.GENERATOR.get() ? (l, p, s, be) -> GeneratorBlockEntity.tick(l, p, s, (GeneratorBlockEntity) be) : null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity gen) {
            if (player.isShiftKeyDown()) {
                gen.onUse(player, stack);
            } else {
                player.openMenu(PowerNodeMenu.provider(level, pos), pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity gen) {
            if (player.isShiftKeyDown()) {
                gen.onUse(player, ItemStack.EMPTY);
            } else if (generatorType == GeneratorType.HAND_CRANK) {
                gen.crank(player);
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
        if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            generator.dropFuelContents();
        }
        PowerNetworkManager.get(level).onBlockRemoved(pos);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide() && blockEntity instanceof GeneratorBlockEntity generator) {
            generator.dropFuelContents();
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator) {
            generator.dropFuelContents();
        }
    }
}
