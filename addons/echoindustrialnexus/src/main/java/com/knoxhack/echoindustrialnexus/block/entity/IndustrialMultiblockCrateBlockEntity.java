package com.knoxhack.echoindustrialnexus.block.entity;

import com.knoxhack.echomultiblockcore.block.entity.MultiblockCrateBlockEntity;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class IndustrialMultiblockCrateBlockEntity extends MultiblockCrateBlockEntity {
   public IndustrialMultiblockCrateBlockEntity(BlockPos pos, BlockState blockState) {
      super(ModBlockEntities.INDUSTRIAL_MULTIBLOCK_CRATE.get(), pos, blockState);
   }
}
