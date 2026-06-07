package com.knoxhack.echoblockworks.block;

import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import net.minecraft.world.level.block.Block;

public class BlockworksDecorativeBlock extends Block implements BlockworksConvertible {
   private final BlockworksBlockInfo info;

   public BlockworksDecorativeBlock(BlockworksBlockInfo info, Properties properties) {
      super(properties);
      this.info = info;
   }

   @Override
   public BlockworksBlockInfo blockworksInfo() {
      return info;
   }
}
