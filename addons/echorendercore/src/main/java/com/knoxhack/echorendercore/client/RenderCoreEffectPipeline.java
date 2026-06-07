package com.knoxhack.echorendercore.client;

import com.knoxhack.echorendercore.RenderCoreConfig;
import com.knoxhack.echorendercore.profile.VisualEffectKind;
import com.knoxhack.echorendercore.profile.VisualEffectProfile;

public final class RenderCoreEffectPipeline {
   private static volatile Boolean advancedFxOverride;

   private RenderCoreEffectPipeline() {
   }

   public static boolean advancedFxEnabled() {
      return advancedFxOverride == null ? RenderCoreConfig.advancedFxEnabled() : advancedFxOverride;
   }

   public static void setAdvancedFxEnabled(boolean enabled) {
      advancedFxOverride = enabled;
      RenderCoreAdvancedFxPipeline.resetStatus();
   }

   public static void resetAdvancedFxOverride() {
      advancedFxOverride = null;
      RenderCoreAdvancedFxPipeline.resetStatus();
   }

   public static boolean advancedFxOverrideActive() {
      return advancedFxOverride != null;
   }

   public static String advancedFxSource() {
      return advancedFxOverride == null ? "config" : "session override";
   }

   public static boolean advancedFxAvailable() {
      return RenderCoreAdvancedFxPipeline.available();
   }

   public static String statusLine() {
      return RenderCoreAdvancedFxPipeline.statusLine();
   }

   public static int color(int color, VisualEffectProfile effect) {
      VisualEffectProfile resolved = effect == null ? VisualEffectProfile.NONE : effect;
      int base = color == 0 ? 0xFFFFFFFF : color;
      if (!resolved.active()) {
         return base;
      }
      float alphaScale = alphaScale(resolved);
      int a = Math.round(((base >>> 24) & 0xFF) * alphaScale);
      int rgb = base & 0x00FFFFFF;
      if (resolved.hueShiftSpeed() != 0.0F) {
         rgb = hueShift(rgb, timeSeconds() * resolved.hueShiftSpeed());
      }
      if (resolved.scanlineStrength() > 0.0F) {
         float scanline = 1.0F - (resolved.scanlineStrength() * 0.18F * pulse(timeSeconds() * 18.0F));
         rgb = scaleRgb(rgb, scanline);
      }
      float glow = Math.min(1.75F, 1.0F + Math.max(resolved.glowIntensity(), resolved.bloomIntensity()) * 0.18F);
      if (resolved.kind() == VisualEffectKind.NEON || resolved.kind() == VisualEffectKind.ENERGY_FIELD) {
         rgb = scaleRgb(rgb, glow);
      }
      return (Math.max(0, Math.min(255, a)) << 24) | rgb;
   }

   public static int order(int baseOrder, VisualEffectProfile effect) {
      VisualEffectProfile resolved = effect == null ? VisualEffectProfile.NONE : effect;
      if (!resolved.active()) {
         return baseOrder;
      }
      return Math.max(1, baseOrder + Math.round(Math.max(0.0F, resolved.depthBias())));
   }

   private static float alphaScale(VisualEffectProfile effect) {
      float alpha = 1.0F;
      if (effect.pulseSpeed() > 0.0F) {
         float amount = pulse(timeSeconds() * effect.pulseSpeed());
         alpha *= lerp(effect.pulseMinAlpha(), effect.pulseMaxAlpha(), amount);
      }
      if (effect.flickerIntensity() > 0.0F) {
         float flicker = pulse(timeSeconds() * 37.0F + effect.kind().ordinal() * 3.17F);
         alpha *= 1.0F - effect.flickerIntensity() * 0.35F * flicker;
      }
      if (effect.kind() == VisualEffectKind.HOLOGRAM) {
         alpha *= 0.88F + 0.12F * pulse(timeSeconds() * 6.0F);
      }
      return Math.max(0.0F, Math.min(1.0F, alpha));
   }

   private static int hueShift(int rgb, float offset) {
      int r = (rgb >>> 16) & 0xFF;
      int g = (rgb >>> 8) & 0xFF;
      int b = rgb & 0xFF;
      float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
      int shifted = java.awt.Color.HSBtoRGB((hsb[0] + offset) % 1.0F, hsb[1], hsb[2]);
      return shifted & 0x00FFFFFF;
   }

   private static int scaleRgb(int rgb, float scale) {
      int r = Math.min(255, Math.round(((rgb >>> 16) & 0xFF) * scale));
      int g = Math.min(255, Math.round(((rgb >>> 8) & 0xFF) * scale));
      int b = Math.min(255, Math.round((rgb & 0xFF) * scale));
      return (r << 16) | (g << 8) | b;
   }

   private static float pulse(float phase) {
      return (float)((Math.sin(phase * Math.PI * 2.0D) + 1.0D) * 0.5D);
   }

   private static float lerp(float min, float max, float amount) {
      return min + (max - min) * amount;
   }

   private static float timeSeconds() {
      return (System.currentTimeMillis() % 3_600_000L) / 1000.0F;
   }
}
