package com.knoxhack.echoindustrialnexus.block.entity;

import com.knoxhack.echomultiblockcore.block.entity.RoboticArmBlockEntity;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public class IndustrialRoboticArmMountBlockEntity extends RoboticArmBlockEntity {
   public IndustrialRoboticArmMountBlockEntity(BlockPos pos, BlockState blockState) {
      super(ModBlockEntities.INDUSTRIAL_ROBOTIC_ARM.get(), pos, blockState);
   }

   @Override
   public Identifier getRobotId() {
      return EchoIndustrialNexus.id("robotic_arm_mount/" + Long.toUnsignedString(getBlockPos().asLong()));
   }
}
