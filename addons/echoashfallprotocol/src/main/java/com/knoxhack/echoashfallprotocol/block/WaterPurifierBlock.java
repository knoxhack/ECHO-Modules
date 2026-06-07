package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.block.entity.WaterPurifierBlockEntity;
import com.knoxhack.echoashfallprotocol.machine.MachineState;
import com.knoxhack.echoashfallprotocol.machine.MachineStateProvider;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Water Purifier — converts dirty water + filter to clean water.
 * Requires local or wired network power.
 */
public class WaterPurifierBlock extends BaseEntityBlock implements MachineStateProvider {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final MapCodec<WaterPurifierBlock> CODEC = simpleCodec(WaterPurifierBlock::new);

    public WaterPurifierBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterPurifierBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.WATER_PURIFIER.get(), WaterPurifierBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                AshfallAdapterCoreMachineRuntimeHost.dispatchUseBlock(
                        serverPlayer, level, pos, "echoashfallprotocol:water_purifier");
            }
            MachineWearData wearData = new MachineWearData(level);
            if ((wearData.isJammed(pos) || wearData.getWear(pos) > 0) && player.getMainHandItem().is(ModItems.SCRAP_METAL.get())) {
                wearData.repair(pos, 200);
                player.getMainHandItem().shrink(1);
                player.sendSystemMessage(Component.literal("§a[ECHO-7]§r Water Purifier repaired. Wear reduced."));
                return InteractionResult.SUCCESS;
            }

            if (player instanceof ServerPlayer serverPlayer) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof WaterPurifierBlockEntity purifier) {
                    serverPlayer.openMenu(purifier, pos);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public MachineState getMachineState(Level level, BlockPos pos, BlockState state) {
        // Check for jam first
        MachineWearData wearData = new MachineWearData(level);
        if (wearData.isJammed(pos)) {
            return MachineState.JAMMED;
        }

        if (state.getValue(ACTIVE)) {
            return MachineState.PROCESSING;
        }
        MachineState powerState = MachineState.blockingPowerState(level, pos);
        if (powerState != null) return powerState;
        return MachineState.IDLE;
    }
}
