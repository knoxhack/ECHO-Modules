package com.knoxhack.echoblockworks.block;

import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;

public class BlockworksStairBlock extends StairBlock implements BlockworksConvertible {
   private final BlockworksBlockInfo info;

   public BlockworksStairBlock(BlockworksBlockInfo info, Properties properties) {
      super(Blocks.IRON_BLOCK.defaultBlockState(), properties);
      this.info = info;
   }

   @Override
   public BlockworksBlockInfo blockworksInfo() {
      return info;
   }
}
