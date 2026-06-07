package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echoindustrialnexus.block.IndustrialMachineBlock;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;

public final class IndustrialRenderCoreVisuals {
   private IndustrialRenderCoreVisuals() {
   }

   public static String visualStateName(IndustrialMachineBlockEntity.MachineStatus status, int heat, int fluxStored) {
      IndustrialMachineBlockEntity.MachineStatus safeStatus =
         status == null ? IndustrialMachineBlockEntity.MachineStatus.IDLE : status;
      return switch (safeStatus) {
         case REMOTE_SHUTDOWN, EMERGENCY_SHUTDOWN -> "OFFLINE";
         case NEXUS_CONTAMINATION -> "CORRUPTED";
         case MELTDOWN -> "FAILED";
         case CRITICAL_HEAT -> "OVERHEATED";
         case PROCESSING, HOT_PROCESSING -> heat >= 85 ? "OVERHEATED" : "WORKING";
         case GENERATING, SCRUBBING, CONTROLLING -> heat >= 85 ? "OVERHEATED" : "ACTIVE";
         case CHARGING -> "CHARGING";
         case COMPLETE -> "COMPLETE";
         case OUTPUT_BLOCKED, BAD_INPUT, CATALYST_REQUIRED, FLUID_REQUIRED, FLUID_OUTPUT_BLOCKED -> "FAILED";
         case STORED -> "ONLINE";
         case IDLE -> fluxStored > 0 ? "ONLINE" : "IDLE";
      };
   }

   public static String variantId(IndustrialMachineBlock.MachineKind kind) {
      return kind == null ? "ore_grinder" : kind.getSerializedName();
   }

   public static float progress(int progressTicks, int maxProgressTicks, int fluxStored, int maxFluxStored) {
      if (maxProgressTicks > 0) {
         return clamp(progressTicks / (float)maxProgressTicks);
      }
      return maxFluxStored > 0 ? clamp(fluxStored / (float)maxFluxStored) : 0.0F;
   }

   private static float clamp(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }
}
