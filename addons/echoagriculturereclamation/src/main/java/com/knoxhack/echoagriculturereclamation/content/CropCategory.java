package com.knoxhack.echoagriculturereclamation.content;

public enum CropCategory {
   EMERGENCY("Emergency Crops"),
   MUTATED("Mutated Crops"),
   MEDICINAL("Medicinal Crops"),
   INDUSTRIAL("Industrial Crops"),
   RESTORATION("Restoration Crops"),
   NEXUS_TOUCHED("Nexus-Touched Crops");

   private final String displayName;

   CropCategory(String displayName) {
      this.displayName = displayName;
   }

   public String displayName() {
      return displayName;
   }
}
