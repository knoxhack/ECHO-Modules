package com.knoxhack.echorendercore.profile;

public record VisualEffectProfile(
   VisualEffectKind kind,
   float glowIntensity,
   float bloomIntensity,
   float pulseSpeed,
   float pulseMinAlpha,
   float pulseMaxAlpha,
   float flickerIntensity,
   float scanlineStrength,
   float hueShiftSpeed,
   float depthBias,
   boolean advancedEnabled,
   float bloomRadius,
   float bloomThreshold,
   int bloomPasses,
   float screenBlend,
   VisualEffectTargetScope targetScope,
   VisualEffectBloomMaskMode bloomMaskMode,
   Integer bloomTint,
   Float bloomMaskAlpha,
   String bloomChannel,
   Integer bloomDownscale,
   int advancedPriority
) {
   public static final VisualEffectProfile NONE = new VisualEffectProfile(
      VisualEffectKind.NONE,
      0.0F,
      0.0F,
      0.0F,
      1.0F,
      1.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      false,
      0.0F,
      1.0F,
      0,
      0.0F,
      VisualEffectTargetScope.PROFILE,
      VisualEffectBloomMaskMode.AUTO,
      null,
      null,
      "default",
      null,
      0
   );

   public VisualEffectProfile(
      VisualEffectKind kind,
      float glowIntensity,
      float bloomIntensity,
      float pulseSpeed,
      float pulseMinAlpha,
      float pulseMaxAlpha,
      float flickerIntensity,
      float scanlineStrength,
      float hueShiftSpeed,
      float depthBias
   ) {
      this(
         kind,
         glowIntensity,
         bloomIntensity,
         pulseSpeed,
         pulseMinAlpha,
         pulseMaxAlpha,
         flickerIntensity,
         scanlineStrength,
         hueShiftSpeed,
         depthBias,
         false,
         0.0F,
         1.0F,
         0,
         0.0F,
         VisualEffectTargetScope.PROFILE
      );
   }

   public VisualEffectProfile(
      VisualEffectKind kind,
      float glowIntensity,
      float bloomIntensity,
      float pulseSpeed,
      float pulseMinAlpha,
      float pulseMaxAlpha,
      float flickerIntensity,
      float scanlineStrength,
      float hueShiftSpeed,
      float depthBias,
      boolean advancedEnabled,
      float bloomRadius,
      float bloomThreshold,
      int bloomPasses,
      float screenBlend,
      VisualEffectTargetScope targetScope
   ) {
      this(
         kind,
         glowIntensity,
         bloomIntensity,
         pulseSpeed,
         pulseMinAlpha,
         pulseMaxAlpha,
         flickerIntensity,
         scanlineStrength,
         hueShiftSpeed,
         depthBias,
         advancedEnabled,
         bloomRadius,
         bloomThreshold,
         bloomPasses,
         screenBlend,
         targetScope,
         VisualEffectBloomMaskMode.AUTO,
         null,
         null,
         "default",
         null,
         0
      );
   }

   public VisualEffectProfile {
      kind = kind == null ? VisualEffectKind.NONE : kind;
      glowIntensity = Math.max(0.0F, glowIntensity);
      bloomIntensity = Math.max(0.0F, bloomIntensity);
      pulseSpeed = Math.max(0.0F, pulseSpeed);
      pulseMinAlpha = clamp01(pulseMinAlpha);
      pulseMaxAlpha = clamp01(pulseMaxAlpha);
      if (pulseMinAlpha > pulseMaxAlpha) {
         float previousMin = pulseMinAlpha;
         pulseMinAlpha = pulseMaxAlpha;
         pulseMaxAlpha = previousMin;
      }
      flickerIntensity = clamp01(flickerIntensity);
      scanlineStrength = clamp01(scanlineStrength);
      depthBias = Math.max(0.0F, depthBias);
      bloomRadius = Math.max(0.0F, bloomRadius);
      bloomThreshold = clamp01(bloomThreshold);
      bloomPasses = Math.max(0, bloomPasses);
      screenBlend = clamp01(screenBlend);
      targetScope = targetScope == null ? VisualEffectTargetScope.PROFILE : targetScope;
      bloomMaskMode = bloomMaskMode == null ? VisualEffectBloomMaskMode.AUTO : bloomMaskMode;
      bloomChannel = bloomChannel == null ? "default" : bloomChannel.trim();
   }

   public boolean active() {
      return kind != VisualEffectKind.NONE
         || glowIntensity > 0.0F
         || bloomIntensity > 0.0F
         || pulseSpeed > 0.0F
         || flickerIntensity > 0.0F
         || scanlineStrength > 0.0F
         || hueShiftSpeed != 0.0F
         || depthBias != 0.0F
         || advancedActive();
   }

   public boolean advancedActive() {
      return advancedEnabled
         || bloomRadius > 0.0F
         || bloomPasses > 0
         || screenBlend > 0.0F
         || targetScope != VisualEffectTargetScope.PROFILE
         || bloomMaskMode != VisualEffectBloomMaskMode.AUTO
         || bloomTint != null
         || bloomMaskAlpha != null
         || bloomDownscale != null
         || advancedPriority != 0
         || hasCustomBloomChannel();
   }

   public boolean bloomCapable() {
      return bloomIntensity > 0.0F
         || bloomRadius > 0.0F
         || bloomPasses > 0
         || screenBlend > 0.0F
         || (advancedEnabled && kind != VisualEffectKind.NONE && bloomMaskMode != VisualEffectBloomMaskMode.NONE);
   }

   public int advancedPassCount() {
      return bloomCapable() ? Math.max(1, bloomPasses) : 0;
   }

   public int bloomCost() {
      if (!bloomCapable()) {
         return 0;
      }
      int passCost = advancedPassCount() * 6;
      int radiusCost = Math.round(bloomRadius * 2.0F);
      int intensityCost = Math.round((bloomIntensity + screenBlend) * 8.0F);
      int maskCost = bloomMaskMode == VisualEffectBloomMaskMode.NONE ? 0 : (bloomTint != null || bloomMaskAlpha != null ? 2 : 1);
      int downscaleCost = bloomDownscale == null ? 0 : Math.max(0, 5 - bloomDownscale);
      return passCost + radiusCost + intensityCost + maskCost + downscaleCost;
   }

   public int effectiveBloomTint(int fallbackColor) {
      return bloomTint == null ? fallbackColor : bloomTint;
   }

   public float effectiveBloomMaskAlpha(float computedLayerAlpha) {
      float alpha = bloomMaskAlpha == null
         ? computedLayerAlpha * Math.max(0.0F, Math.min(1.0F, bloomIntensity))
         : bloomMaskAlpha;
      if (bloomMaskMode == VisualEffectBloomMaskMode.SOLID) {
         alpha = 1.0F;
      } else if (bloomMaskMode == VisualEffectBloomMaskMode.LAYER_ALPHA) {
         alpha = computedLayerAlpha;
      }
      return clamp01(alpha);
   }

   public String effectiveBloomChannel() {
      return bloomChannel == null || bloomChannel.isBlank() ? "default" : bloomChannel;
   }

   public int effectiveBloomDownscale(int fallback) {
      int value = bloomDownscale == null ? fallback : bloomDownscale;
      return value == 1 || value == 2 || value == 4 ? value : fallback;
   }

   public int effectiveAdvancedPriority() {
      return Math.max(-100, Math.min(100, advancedPriority));
   }

   public VisualEffectProfile mergeOver(VisualEffectProfile fallback) {
      VisualEffectProfile base = fallback == null ? NONE : fallback;
      if (this == NONE || !active()) {
         return base;
      }
      return this;
   }

   public int cost() {
      if (!active()) {
         return 0;
      }
      int cost = 1;
      if (glowIntensity > 1.0F) {
         cost++;
      }
      if (bloomIntensity > 0.0F) {
         cost += 2;
      }
      if (pulseSpeed > 0.0F) {
         cost++;
      }
      if (flickerIntensity > 0.0F) {
         cost++;
      }
      if (scanlineStrength > 0.0F) {
         cost++;
      }
      if (hueShiftSpeed != 0.0F) {
         cost++;
      }
      cost += bloomCost();
      return cost;
   }

   private boolean hasCustomBloomChannel() {
      return bloomChannel != null && !bloomChannel.isBlank() && !"default".equals(bloomChannel);
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }
}
