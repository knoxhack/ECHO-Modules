package com.knoxhack.echoaetherworks.block;

import com.knoxhack.echoaetherworks.api.AetherWorksApi;
import com.knoxhack.echoaetherworks.block.entity.AetherCellBlockEntity;
import com.knoxhack.echoaetherworks.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AetherCellBlock extends AetherMachineBlock {
    public static final MapCodec<AetherCellBlock> CODEC = simpleCodec(AetherCellBlock::new);

    public AetherCellBlock(Properties props) {
        super(props, AetherWorksApi.AETHER_CELL);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AetherCellBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.AETHER_CELL.get(), AetherCellBlockEntity::serverTick);
    }
}
