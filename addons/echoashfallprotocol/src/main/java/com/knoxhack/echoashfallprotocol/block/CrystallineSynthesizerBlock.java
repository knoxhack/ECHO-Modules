package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.block.entity.CrystallineSynthesizerBlockEntity;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Crystalline Synthesizer — Tier 3 extraction machine.
 * 4-phase reaction cycle (HEATING→REACTING→CRYSTALLIZING→COOLING).
 * Produces diamonds, emeralds, and netherite scraps.
 */
public class CrystallineSynthesizerBlock extends BaseEntityBlock {
    /** 0=idle, 1=HEATING, 2=REACTING, 3=CRYSTALLIZING, 4=COOLING */
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 4);
    public static final MapCodec<CrystallineSynthesizerBlock> CODEC = simpleCodec(CrystallineSynthesizerBlock::new);

    public CrystallineSynthesizerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PHASE, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrystallineSynthesizerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.CRYSTALLINE_SYNTHESIZER.get(), CrystallineSynthesizerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CrystallineSynthesizerBlockEntity synth) {
                serverPlayer.openMenu(synth, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
