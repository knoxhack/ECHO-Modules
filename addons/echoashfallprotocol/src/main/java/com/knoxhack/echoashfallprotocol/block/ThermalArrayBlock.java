package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.block.entity.ThermalArrayBlockEntity;
import com.knoxhack.echoashfallprotocol.machine.MachineState;
import com.knoxhack.echoashfallprotocol.machine.MachineStateProvider;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * Thermal Array — Tier 2.5 power generator.
 * Burns fuel more efficiently than Micro Generator, producing 30 FE/tick.
 * Multi-slot fuel input for longer autonomous operation.
 * Right-click to open GUI for fuel management.
 */
public class ThermalArrayBlock extends BaseEntityBlock implements MachineStateProvider {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final MapCodec<ThermalArrayBlock> CODEC = simpleCodec(ThermalArrayBlock::new);

    public ThermalArrayBlock(Properties properties) {
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
        return new ThermalArrayBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.THERMAL_ARRAY.get(), ThermalArrayBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ThermalArrayBlockEntity generator) {
                MachineWearData wearData = new MachineWearData(level);

                // Check if player is trying to repair with scrap metal
                if (wearData.getWear(pos) > 0 || wearData.isJammed(pos)) {
                    ItemStack heldItem = player.getMainHandItem();
                    if (heldItem.is(ModItems.SCRAP_METAL.get())) {
                        // Repair the generator
                        wearData.repair(pos, 150);
                        heldItem.shrink(1);
                        player.sendSystemMessage(Component.literal(
                                "\u00A7a[ECHO-7]\u00A7r Thermal Array patched. Heat curve back inside tolerances."));
                        return InteractionResult.SUCCESS;
                    }
                }

                if (generator.isFailed()) {
                    // Restart the generator
                    generator.restart();
                    player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.thermal_array.restarted")
                            .withColor(0x55FF55));
                } else if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(generator, pos);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public MachineState getMachineState(Level level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ThermalArrayBlockEntity generator) {
            // Check for jam first
            MachineWearData wearData = new MachineWearData(level);
            if (wearData.isJammed(pos)) {
                return MachineState.JAMMED;
            }

            if (generator.isFailed()) {
                return MachineState.UNSTABLE; // Failed state
            } else if (state.getValue(ACTIVE)) {
                return MachineState.GENERATING; // Active and producing power
            } else {
                return MachineState.IDLE;
            }
        }
        return MachineState.OFFLINE;
    }
}
