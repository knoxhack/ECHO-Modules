package com.knoxhack.echomultiblockcore.block;

import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MultiblockControllerBlock extends Block implements EntityBlock {
    private final Identifier defaultDefinitionId;

    public MultiblockControllerBlock(Identifier defaultDefinitionId, Properties properties) {
        super(properties);
        this.defaultDefinitionId = defaultDefinitionId;
    }

    public Identifier defaultDefinitionId() {
        return defaultDefinitionId;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.CONTROLLER.get()
                ? (tickLevel, pos, blockState, blockEntity) -> MultiblockControllerBlockEntity.tick(tickLevel, pos, blockState, (MultiblockControllerBlockEntity) blockEntity)
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MultiblockControllerBlockEntity controller) {
            controller.handlePlayerUse(player, player.isShiftKeyDown());
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MultiblockControllerBlockEntity controller) {
            controller.markValidationDirty();
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide() && blockEntity instanceof MultiblockControllerBlockEntity controller) {
            controller.onStructureBroken();
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
}
