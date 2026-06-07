package com.knoxhack.echoblockworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LowDetailBlock extends Block {
   private static final VoxelShape LOW_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 5.0D, 15.0D);

   public LowDetailBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return LOW_SHAPE;
   }
}
