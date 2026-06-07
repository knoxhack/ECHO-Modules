package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.block.entity.HandRecyclerBlockEntity;
import com.knoxhack.echoashfallprotocol.machine.MachineState;
import com.knoxhack.echoashfallprotocol.machine.MachineStateProvider;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Hand Recycler — core starter machine.
 * Converts scrap → usable materials. Slow but essential. Upgradeable.
 */
public class HandRecyclerBlock extends BaseEntityBlock implements MachineStateProvider {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final MapCodec<HandRecyclerBlock> CODEC = simpleCodec(HandRecyclerBlock::new);

    public HandRecyclerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HandRecyclerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.HAND_RECYCLER.get(), HandRecyclerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                AshfallAdapterCoreMachineRuntimeHost.dispatchUseBlock(
                        serverPlayer, level, pos, "echoashfallprotocol:hand_recycler");
            }
            // Check for machine repair with scrap metal
            MachineWearData wearData = new MachineWearData(level);
            if (wearData.isJammed(pos) || wearData.getWear(pos) > 0) {
                if (player.getMainHandItem().is(ModItems.SCRAP_METAL.get())) {
                    // Repair the machine
                    wearData.repair(pos, 200); // Repair 200 wear points
                    player.getMainHandItem().shrink(1); // Consume 1 scrap metal
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§a[ECHO-7]§r Machine repaired. Wear reduced."));
                    return InteractionResult.SUCCESS;
                }
            }

            if (player instanceof ServerPlayer serverPlayer) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof HandRecyclerBlockEntity recycler) {
                    serverPlayer.openMenu(recycler, pos);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public MachineState getMachineState(Level level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HandRecyclerBlockEntity recycler) {
            // Check for jam first
            MachineWearData wearData = new MachineWearData(level);
            if (wearData.isJammed(pos)) {
                return MachineState.JAMMED;
            }

            if (state.getValue(ACTIVE)) {
                return MachineState.PROCESSING;
            } else if (!recycler.hasRecipe()) {
                return MachineState.IDLE;
            } else {
                MachineState powerState = MachineState.blockingPowerState(level, pos);
                if (powerState != null) return powerState;
                return MachineState.BLOCKED; // Has recipe and power but not processing (output full)
            }
        }
        return MachineState.OFFLINE;
    }

    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HandRecyclerBlockEntity recycler) {
            // Calculate fill percentage for comparator output (0-15)
            float filled = 0;
            float total = 0;
            for (int i = 0; i < recycler.getInventory().getContainerSize(); i++) {
                ItemStack stack = recycler.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    filled += (float) stack.getCount() / Math.max(stack.getMaxStackSize(), 1);
                    total += 1;
                }
            }
            return total > 0 ? (int) ((filled / total) * 15) : 0;
        }
        return 0;
    }
}
