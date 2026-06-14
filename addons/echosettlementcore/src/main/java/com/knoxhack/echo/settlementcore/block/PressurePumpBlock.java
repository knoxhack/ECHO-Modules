package com.knoxhack.echo.settlementcore.block;

import com.knoxhack.echo.settlementcore.block.entity.PressurePumpBlockEntity;
import com.knoxhack.echo.settlementcore.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PressurePumpBlock extends HabitatBlock implements EntityBlock {
    public PressurePumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PressurePumpBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.PRESSURE_PUMP.get()
            ? (tickLevel, pos, blockState, blockEntity) -> PressurePumpBlockEntity.tick(tickLevel, pos, blockState, (PressurePumpBlockEntity) blockEntity)
            : null;
    }
}
