package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.block.entity.HopperHandler;
import com.knoxhack.echoashfallprotocol.block.entity.ItemPipeBlockEntity;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Item Pipe - transports items between machines.
 * Directional placement determines extraction direction.
 */
public class ItemPipeBlock extends BaseEntityBlock {
    public static final MapCodec<ItemPipeBlock> CODEC = simpleCodec(ItemPipeBlock::new);
    
    // Facing = extraction direction (where pipe pulls FROM)
    public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);
    
    // Connection states for each side
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    
    public ItemPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.DOWN)
            .setValue(NORTH, false)
            .setValue(SOUTH, false)
            .setValue(EAST, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false));
    }
    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }
    
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace().getOpposite();
        return this.defaultBlockState().setValue(FACING, facing);
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide()) {
            updateConnections(level, pos, state);
        }
    }
    
    /**
     * Update connection states based on adjacent blocks.
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
     * Check if this pipe can connect to the block at the given position.
     */
    private boolean canConnect(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ItemPipeBlockEntity) {
            return true; // Connect to other pipes
        }
        if (be instanceof HopperHandler) {
            return true; // Connect to machines
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
        return new ItemPipeBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.ITEM_PIPE.get(), ItemPipeBlockEntity::serverTick);
    }
}
