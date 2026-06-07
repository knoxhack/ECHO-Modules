package com.knoxhack.echoagriculturereclamation.content;

import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationCrossAddonIntegration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum SoilState {
   DEAD("Dead Soil", 8, 0, false),
   CONTAMINATED("Contaminated Soil", 14, 0, false),
   IRRADIATED("Irradiated Soil", 10, 0, false),
   TOXIC_MUD("Toxic Mud", 12, 0, false),
   PURIFIED("Purified Soil", 28, 1, true),
   STABILIZED("Stabilized Soil", 42, 2, true),
   RESTORED("Restored Soil", 55, 4, true);

   private final String displayName;
   private final int growthChance;
   private final int restorationGain;
   private final boolean safe;

   SoilState(String displayName, int growthChance, int restorationGain, boolean safe) {
      this.displayName = displayName;
      this.growthChance = growthChance;
      this.restorationGain = restorationGain;
      this.safe = safe;
   }

   public String displayName() {
      return displayName;
   }

   public int growthChance() {
      return ReclamationContent.soil(this).growthChance();
   }

   public int restorationGain() {
      return ReclamationContent.soil(this).restorationGain();
   }

   public boolean safe() {
      return ReclamationContent.soil(this).safe();
   }

   public boolean canSupport(CropSpec spec, int stability) {
      return ReclamationContent.soil(this).canSupport(spec.category(), stability);
   }

   public SoilState purifiedStep() {
      return switch (this) {
         case DEAD, CONTAMINATED, IRRADIATED, TOXIC_MUD -> PURIFIED;
         case PURIFIED -> STABILIZED;
         case STABILIZED, RESTORED -> RESTORED;
      };
   }

   public static SoilState fromBlock(BlockState state) {
      Block block = state.getBlock();
      if (block == ModBlocks.DEAD_SOIL.get()) {
         return DEAD;
      }
      if (block == ModBlocks.CONTAMINATED_SOIL.get()) {
         return CONTAMINATED;
      }
      if (block == ModBlocks.IRRADIATED_SOIL.get()) {
         return IRRADIATED;
      }
      if (block == ModBlocks.TOXIC_MUD.get()) {
         return TOXIC_MUD;
      }
      if (block == ModBlocks.PURIFIED_SOIL.get()) {
         return PURIFIED;
      }
      if (block == ModBlocks.STABILIZED_SOIL.get()) {
         return STABILIZED;
      }
      if (block == ModBlocks.RESTORED_SOIL.get()) {
         return RESTORED;
      }
      if (state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) {
         return PURIFIED;
      }
      SoilState external = ReclamationCrossAddonIntegration.externalSoilState(state);
      if (external != null) {
         return external;
      }
      return DEAD;
   }
}
