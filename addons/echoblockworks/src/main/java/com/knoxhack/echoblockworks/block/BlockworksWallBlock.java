package com.knoxhack.echoblockworks.block;

import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import net.minecraft.world.level.block.WallBlock;

public class BlockworksWallBlock extends WallBlock implements BlockworksConvertible {
   private final BlockworksBlockInfo info;

   public BlockworksWallBlock(BlockworksBlockInfo info, Properties properties) {
      super(properties);
      this.info = info;
   }

   @Override
   public BlockworksBlockInfo blockworksInfo() {
      return info;
   }
}
