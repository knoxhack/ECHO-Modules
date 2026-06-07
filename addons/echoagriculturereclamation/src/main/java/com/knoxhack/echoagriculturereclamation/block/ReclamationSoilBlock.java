package com.knoxhack.echoagriculturereclamation.block;

import com.knoxhack.echoagriculturereclamation.content.SoilState;
import net.minecraft.world.level.block.Block;

public class ReclamationSoilBlock extends Block {
   private final SoilState soilState;

   public ReclamationSoilBlock(SoilState soilState, Properties properties) {
      super(properties);
      this.soilState = soilState;
   }

   public SoilState soilState() {
      return soilState;
   }
}
