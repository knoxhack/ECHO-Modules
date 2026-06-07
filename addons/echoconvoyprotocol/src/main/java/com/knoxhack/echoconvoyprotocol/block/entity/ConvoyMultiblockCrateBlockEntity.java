package com.knoxhack.echoconvoyprotocol.block.entity;

import com.knoxhack.echoconvoyprotocol.registry.ModBlockEntities;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ConvoyMultiblockCrateBlockEntity extends MultiblockCrateBlockEntity {
   public ConvoyMultiblockCrateBlockEntity(BlockPos pos, BlockState blockState) {
      super(ModBlockEntities.CONVOY_MULTIBLOCK_CRATE.get(), pos, blockState);
   }
}
