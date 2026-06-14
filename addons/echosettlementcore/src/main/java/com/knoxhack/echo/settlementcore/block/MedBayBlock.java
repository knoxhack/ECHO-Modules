package com.knoxhack.echo.settlementcore.block;

import com.knoxhack.echo.settlementcore.block.entity.MedBayBlockEntity;
import com.knoxhack.echo.settlementcore.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MedBayBlock extends HabitatBlock implements EntityBlock {
    public MedBayBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MedBayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.MED_BAY.get()
            ? (tickLevel, pos, blockState, blockEntity) -> MedBayBlockEntity.tick(tickLevel, pos, blockState, (MedBayBlockEntity) blockEntity)
            : null;
    }
}
