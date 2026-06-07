package com.knoxhack.echorendercore.profile;

import java.util.Map;

public record ParticleOptionsSpec(
   String type,
   int color,
   float scale,
   int lifetime,
   Map<String, String> custom
) {
   public static final ParticleOptionsSpec DEFAULT = new ParticleOptionsSpec("", 0xFFFFFFFF, 1.0F, 0, Map.of());

   public ParticleOptionsSpec(int color, float scale, int lifetime, Map<String, String> custom) {
      this("", color, scale, lifetime, custom);
   }

   public ParticleOptionsSpec {
      type = type == null ? "" : type.trim();
      scale = Math.max(0.01F, scale);
      lifetime = Math.max(0, lifetime);
      custom = custom == null ? Map.of() : Map.copyOf(custom);
   }

   public String option(String key) {
      return key == null ? null : custom.get(key);
   }

   public int optionInt(String key, int fallback) {
      String value = option(key);
      if (value == null || value.isBlank()) {
         return fallback;
      }
      try {
         return Integer.parseInt(value);
      } catch (NumberFormatException ignored) {
         return fallback;
      }
   }

   public float optionFloat(String key, float fallback) {
      String value = option(key);
      if (value == null || value.isBlank()) {
         return fallback;
      }
      try {
         return Float.parseFloat(value);
      } catch (NumberFormatException ignored) {
         return fallback;
      }
   }

   public boolean optionBool(String key, boolean fallback) {
      String value = option(key);
      if (value == null || value.isBlank()) {
         return fallback;
      }
      return Boolean.parseBoolean(value);
   }
}
