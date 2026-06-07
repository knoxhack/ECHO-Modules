package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.block.entity.PowerCableBlockEntity;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.jetbrains.annotations.Nullable;

/**
 * Power Cable - wired power distribution network.
 * Basic tier: 1000 FE capacity, 50 FE/t transfer.
 */
public class PowerCableBlock extends BaseEntityBlock {
    public static final MapCodec<PowerCableBlock> CODEC = simpleCodec(PowerCableBlock::new);
    public static final int BASIC_CAPACITY = 1000;
    public static final int BASIC_TRANSFER = 50;
    
    // Connection states for each side
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private final int capacity;
    private final int transferRate;
    
    public PowerCableBlock(Properties properties) {
        this(properties, BASIC_CAPACITY, BASIC_TRANSFER);
    }

    public PowerCableBlock(Properties properties, int capacity, int transferRate) {
        super(properties);
        this.capacity = capacity;
        this.transferRate = transferRate;
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, false)
            .setValue(SOUTH, false)
            .setValue(EAST, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false)
            .setValue(ACTIVE, false));
    }

    public int getCapacity() {
        return capacity;
    }

    public int getTransferRate() {
        return transferRate;
    }
    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, ACTIVE);
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide()) {
            updateConnections(level, pos, state);
        }
    }
    
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide()) {
            updateConnections(level, pos, state);
        }
    }
    
    /**
     * Update connection states based on adjacent energy-handling blocks.
     */
    private void updateConnections(Level level, BlockPos pos, BlockState state) {
        BlockState newState = state
            .setValue(NORTH, canConnect(level, pos.relative(Direction.NORTH)))
            .setValue(SOUTH, canConnect(level, pos.relative(Direction.SOUTH)))
            .setValue(EAST, canConnect(level, pos.relative(Direction.EAST)))
            .setValue(WEST, canConnect(level, pos.relative(Direction.WEST)))
            .setValue(UP, canConnect(level, pos.relative(Direction.UP)))
            .setValue(DOWN, canConnect(level, pos.relative(Direction.DOWN)));
        
        level.setBlock(pos, newState, 3);
    }
    
    /**
     * Check if cable can connect to the block at the given position.
     */
    private boolean canConnect(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PowerCableBlockEntity || be instanceof com.knoxhack.echoashfallprotocol.block.entity.PowerNodeBlockEntity
                || be instanceof com.knoxhack.echoashfallprotocol.block.entity.BatteryBankBlockEntity
                || be instanceof com.knoxhack.echoashfallprotocol.block.entity.NexusCapacitorBlockEntity
                || be instanceof com.knoxhack.echoashfallprotocol.block.entity.LoadDistributorBlockEntity) {
            return true; // Connect to other cables
        }
        if (be instanceof IEnergyStorage) {
            return true; // Connect to energy storage/generators/consumers
        }
        return false;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerCableBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.POWER_CABLE.get(), PowerCableBlockEntity::serverTick);
    }
}
