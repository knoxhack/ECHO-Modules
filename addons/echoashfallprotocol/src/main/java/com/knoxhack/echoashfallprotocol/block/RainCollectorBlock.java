package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.block.entity.RainCollectorBlockEntity;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventHandler;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RainCollectorBlock extends BaseEntityBlock {
    public static final MapCodec<RainCollectorBlock> CODEC = simpleCodec(RainCollectorBlock::new);

    public RainCollectorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RainCollectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.RAIN_COLLECTOR.get(), RainCollectorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RainCollectorBlockEntity collector) {
                player.sendSystemMessage(statusMessage(level, pos, collector));
            } else {
                player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.rain_collector.offline"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static Component statusMessage(Level level, BlockPos pos, RainCollectorBlockEntity collector) {
        if (collector.getStoredBottles() > 0) {
            return Component.translatable(
                    "message.EchoAshfallProtocol.rain_collector.ready",
                    collector.getStoredBottles(),
                    RainCollectorBlockEntity.CAPACITY_BOTTLES);
        }

        if (RainCollectorBlockEntity.canCollect(level, pos)) {
            return Component.translatable(
                    "message.EchoAshfallProtocol.rain_collector.collecting",
                    collector.getCollectionPercent());
        }

        if (level.isRaining() || EnvironmentalEventHandler.isStormRainAt(level, pos.above())) {
            return Component.translatable("message.EchoAshfallProtocol.rain_collector.blocked");
        }

        return Component.translatable("message.EchoAshfallProtocol.rain_collector.dry");
    }
}
