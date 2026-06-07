package com.knoxhack.echorendercore.profile;

import java.util.Locale;

public enum VisualEffectKind {
   NONE,
   NEON,
   HOLOGRAM,
   ENERGY_FIELD,
   TERMINAL_HUD,
   ATMOSPHERE,
   UNSUPPORTED;

   public static VisualEffectKind byName(String name) {
      if (name == null || name.isBlank()) {
         return NONE;
      }
      return switch (name.trim().toLowerCase(Locale.ROOT)) {
         case "none", "off" -> NONE;
         case "neon" -> NEON;
         case "hologram", "holo" -> HOLOGRAM;
         case "energy_field", "energyfield", "energy-field", "field" -> ENERGY_FIELD;
         case "terminal_hud", "terminalhud", "terminal-hud", "hud" -> TERMINAL_HUD;
         case "atmosphere", "atmospheric" -> ATMOSPHERE;
         default -> UNSUPPORTED;
      };
   }

   public boolean supported() {
      return this != UNSUPPORTED;
   }
}
