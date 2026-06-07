package com.knoxhack.echoblockworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class DirectionalDetailBlock extends Block {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape NORTH_WALL = Block.box(2.0D, 2.0D, 0.0D, 14.0D, 14.0D, 3.0D);
   private static final VoxelShape SOUTH_WALL = Block.box(2.0D, 2.0D, 13.0D, 14.0D, 14.0D, 16.0D);
   private static final VoxelShape WEST_WALL = Block.box(0.0D, 2.0D, 2.0D, 3.0D, 14.0D, 14.0D);
   private static final VoxelShape EAST_WALL = Block.box(13.0D, 2.0D, 2.0D, 16.0D, 14.0D, 14.0D);
   private final boolean thinWallShape;

   public DirectionalDetailBlock(boolean thinWallShape, Properties properties) {
      super(properties);
      this.thinWallShape = thinWallShape;
      registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
      return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   @Override
   protected BlockState rotate(BlockState state, Rotation rotation) {
      return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation(state.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (!thinWallShape) {
         return super.getShape(state, level, pos, context);
      }
      return switch (state.getValue(FACING)) {
         case SOUTH -> SOUTH_WALL;
         case WEST -> WEST_WALL;
         case EAST -> EAST_WALL;
         default -> NORTH_WALL;
      };
   }
}
