package com.knoxhack.echoblockworks.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class BlockworksStateUtil {
   private BlockworksStateUtil() {
   }

   public static BlockState copySharedProperties(BlockState from, BlockState to) {
      BlockState result = to;
      for (Property<?> property : from.getProperties()) {
         if (result.hasProperty(property)) {
            result = copyValue(from, result, property);
         }
      }
      return result;
   }

   private static <T extends Comparable<T>> BlockState copyValue(BlockState from, BlockState to, Property<T> property) {
      return to.setValue(property, from.getValue(property));
   }
}
