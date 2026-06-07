package com.knoxhack.echoblockworks.content;

import java.util.Set;

public record BlockworksVariant(
   String id,
   String displayName,
   boolean supportsSlab,
   boolean supportsStairs,
   boolean supportsWall,
   int light,
   boolean animated,
   Set<BlockworksTheme> tags
) {
   public BlockworksVariant {
      tags = tags == null ? Set.of() : Set.copyOf(tags);
   }

   public boolean supports(BlockworksShapeKind shape) {
      return switch (shape) {
         case FULL -> true;
         case SLAB -> supportsSlab;
         case STAIRS -> supportsStairs;
         case WALL -> supportsWall;
      };
   }

   public boolean glassLike() {
      String normalized = id == null ? "" : id;
      return normalized.contains("glass") || normalized.contains("crystal") || normalized.contains("dome");
   }
}
