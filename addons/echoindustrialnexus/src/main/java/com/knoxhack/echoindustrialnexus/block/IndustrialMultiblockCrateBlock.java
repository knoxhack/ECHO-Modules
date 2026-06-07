package com.knoxhack.echoindustrialnexus.block;

import com.knoxhack.echomultiblockcore.block.MultiblockCrateBlock;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMultiblockCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class IndustrialMultiblockCrateBlock extends MultiblockCrateBlock {
   public IndustrialMultiblockCrateBlock(CrateKind kind, Properties properties) {
      super(kind, properties);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new IndustrialMultiblockCrateBlockEntity(pos, state);
   }
}
