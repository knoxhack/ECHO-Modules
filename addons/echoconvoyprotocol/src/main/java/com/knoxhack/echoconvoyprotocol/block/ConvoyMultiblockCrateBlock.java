package com.knoxhack.echoconvoyprotocol.block;

import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyMultiblockCrateBlockEntity;
import com.knoxhack.echomultiblockcore.block.MultiblockCrateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ConvoyMultiblockCrateBlock extends MultiblockCrateBlock {
   public ConvoyMultiblockCrateBlock(CrateKind kind, Properties properties) {
      super(kind, properties);
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ConvoyMultiblockCrateBlockEntity(pos, state);
   }
}
