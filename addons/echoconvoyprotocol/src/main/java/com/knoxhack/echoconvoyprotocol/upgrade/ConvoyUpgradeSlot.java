package com.knoxhack.echoconvoyprotocol.upgrade;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

public enum ConvoyUpgradeSlot implements StringRepresentable {
   MOBILITY("mobility", "Mobility"),
   UTILITY("utility", "Utility"),
   DEFENSE("defense", "Defense");

   private static final ConvoyUpgradeSlot[] BY_ID = values();
   private final String id;
   private final String displayName;

   ConvoyUpgradeSlot(String id, String displayName) {
      this.id = id;
      this.displayName = displayName;
   }

   @Override
   public String getSerializedName() {
      return id;
   }

   public String displayName() {
      return displayName;
   }

   public static ConvoyUpgradeSlot byId(int id) {
      return id >= 0 && id < BY_ID.length ? BY_ID[id] : MOBILITY;
   }

   public static ConvoyUpgradeSlot byName(String name) {
      String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
      for (ConvoyUpgradeSlot slot : values()) {
         if (slot.id.equals(normalized)) {
            return slot;
         }
      }
      return MOBILITY;
   }
}
