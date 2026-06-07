package com.knoxhack.echoblockworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SparkingDetailBlock extends DirectionalDetailBlock {
   public SparkingDetailBlock(Properties properties) {
      super(true, properties);
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (random.nextInt(5) == 0) {
         level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5D, pos.getY() + 0.55D, pos.getZ() + 0.5D, 0.0D, 0.02D, 0.0D);
         if (random.nextInt(4) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.15F, 1.6F, false);
         }
      }
   }
}
