package com.knoxhack.echoblockworks.block;

import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import net.minecraft.world.level.block.SlabBlock;

public class BlockworksSlabBlock extends SlabBlock implements BlockworksConvertible {
   private final BlockworksBlockInfo info;

   public BlockworksSlabBlock(BlockworksBlockInfo info, Properties properties) {
      super(properties);
      this.info = info;
   }

   @Override
   public BlockworksBlockInfo blockworksInfo() {
      return info;
   }
}
