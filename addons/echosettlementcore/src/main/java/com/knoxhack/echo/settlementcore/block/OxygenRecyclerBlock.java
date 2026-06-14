package com.knoxhack.echo.settlementcore.block;

import com.knoxhack.echo.settlementcore.block.entity.OxygenRecyclerBlockEntity;
import com.knoxhack.echo.settlementcore.registry.ModBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class OxygenRecyclerBlock extends HabitatBlock implements EntityBlock {
    public OxygenRecyclerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OxygenRecyclerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.OXYGEN_RECYCLER.get()
            ? (tickLevel, pos, blockState, blockEntity) -> OxygenRecyclerBlockEntity.tick(tickLevel, pos, blockState, (OxygenRecyclerBlockEntity) blockEntity)
            : null;
    }
}
