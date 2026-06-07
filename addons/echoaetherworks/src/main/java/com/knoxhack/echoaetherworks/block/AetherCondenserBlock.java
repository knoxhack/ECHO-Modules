package com.knoxhack.echoaetherworks.block;

import com.knoxhack.echoaetherworks.api.AetherWorksApi;
import com.knoxhack.echoaetherworks.block.entity.AetherCondenserBlockEntity;
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

public class AetherCondenserBlock extends AetherMachineBlock {
    public static final MapCodec<AetherCondenserBlock> CODEC = simpleCodec(AetherCondenserBlock::new);

    public AetherCondenserBlock(Properties props) {
        super(props, AetherWorksApi.AETHER_CONDENSER);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AetherCondenserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.AETHER_CONDENSER.get(), AetherCondenserBlockEntity::serverTick);
    }
}
