package com.knoxhack.echoashfallprotocol.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreLateRuntime;

/**
 * Relay Station Block - Core component of the Radio Network fast-travel system.
 * Players must repair and activate stations to enable fast travel between them.
 */
public class RelayStationBlock extends Block {
    
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty REPAIRED = BooleanProperty.create("repaired");
    
    public RelayStationBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(ACTIVE, false)
            .setValue(REPAIRED, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, REPAIRED);
    }
    
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return useRelayStation(state, level, pos, player);
    }

    public InteractionResult useRelayStation(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            AshfallAdapterCoreLateRuntime.relayStationUsed(
                    serverPlayer,
                    pos,
                    player.isShiftKeyDown(),
                    "relay_station_block");
        }
        return InteractionResult.SUCCESS;
    }
    
    /**
     * Check if station is active for fast travel
     */
    public static boolean isStationActive(BlockState state) {
        return state.getValue(ACTIVE);
    }
}
