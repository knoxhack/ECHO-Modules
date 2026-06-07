package com.knoxhack.echorendercore.api;

import java.util.Locale;

public enum VisualState {
   OFFLINE,
   IDLE,
   ONLINE,
   ACTIVE,
   WORKING,
   SCANNING,
   CHARGING,
   OVERHEATED,
   DAMAGED,
   COMPLETE,
   FAILED,
   CORRUPTED;

   public static VisualState byName(String value) {
      return byName(value, IDLE);
   }

   public static VisualState byName(String value, VisualState fallback) {
      if (value == null || value.isBlank()) {
         return fallback == null ? IDLE : fallback;
      }
      try {
         return VisualState.valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException exception) {
         return fallback == null ? IDLE : fallback;
      }
   }

   public String serializedName() {
      return name().toLowerCase(Locale.ROOT);
   }
}
