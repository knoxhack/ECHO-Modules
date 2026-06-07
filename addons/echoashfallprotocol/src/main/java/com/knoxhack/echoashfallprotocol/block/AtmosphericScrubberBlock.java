package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.block.entity.AtmosphericScrubberBlockEntity;
import com.knoxhack.echoashfallprotocol.block.menu.MachineStatusMenu;
import com.knoxhack.echoashfallprotocol.machine.MachineState;
import com.knoxhack.echoashfallprotocol.machine.MachineStateProvider;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class AtmosphericScrubberBlock extends BaseEntityBlock implements MachineStateProvider {
    public static final MapCodec<AtmosphericScrubberBlock> CODEC = simpleCodec(AtmosphericScrubberBlock::new);

    public AtmosphericScrubberBlock(Properties properties) {
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
        return new AtmosphericScrubberBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.ATMOSPHERIC_SCRUBBER.get(), AtmosphericScrubberBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            MachineWearData wearData = new MachineWearData(level);
            if ((wearData.isJammed(pos) || wearData.getWear(pos) > 0) && player.getMainHandItem().is(ModItems.SCRAP_METAL.get())) {
                wearData.repair(pos, 200);
                player.getMainHandItem().shrink(1);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[ECHO-7] Atmospheric Scrubber repaired. Wear reduced."));
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof AtmosphericScrubberBlockEntity scrubber) {
                serverPlayer.openMenu(new MachineStatusMenu.Provider(scrubber), pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public MachineState getMachineState(Level level, BlockPos pos, BlockState state) {
        MachineWearData wearData = new MachineWearData(level);
        if (wearData.isJammed(pos)) return MachineState.JAMMED;
        MachineState powerState = MachineState.blockingPowerState(level, pos);
        if (powerState != null) return powerState;
        if (level.getBlockEntity(pos) instanceof AtmosphericScrubberBlockEntity scrubber && scrubber.isActive()) {
            return MachineState.PROCESSING;
        }
        return MachineState.IDLE;
    }
}
