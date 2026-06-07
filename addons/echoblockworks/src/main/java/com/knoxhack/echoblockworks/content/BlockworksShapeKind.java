package com.knoxhack.echoblockworks.content;

public enum BlockworksShapeKind {
   FULL("", "Full Block"),
   SLAB("_slab", "Slab"),
   STAIRS("_stairs", "Stairs"),
   WALL("_wall", "Wall");

   private final String suffix;
   private final String displayName;

   BlockworksShapeKind(String suffix, String displayName) {
      this.suffix = suffix;
      this.displayName = displayName;
   }

   public String suffix() {
      return suffix;
   }

   public String displayName() {
      return displayName;
   }
}
