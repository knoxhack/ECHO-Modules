package com.knoxhack.echorendercore.profile;

import com.knoxhack.echorendercore.animation.AnimationBlendMode;

public record VisualMaterial(
   String id,
   int color,
   float alpha,
   boolean emissive,
   AnimationBlendMode blendMode,
   VisualLightMode lightMode,
   VisualRenderPass renderPass,
   Boolean cull,
   Boolean depthWrite,
   int sortOrder,
   Integer lightOverride,
   Integer overlayOverride,
   Integer outlineColor,
   int renderPriority,
   VisualEffectProfile effect
) {
   public static final VisualMaterial DEFAULT = new VisualMaterial(
      "default",
      0xFFFFFFFF,
      1.0F,
      false,
      AnimationBlendMode.REPLACE,
      VisualLightMode.PROFILE,
      VisualRenderPass.AUTO,
      null,
      null,
      0,
      null,
      null,
      null,
      0,
      VisualEffectProfile.NONE
   );

   public VisualMaterial(String id, int color, float alpha, boolean emissive, AnimationBlendMode blendMode) {
      this(id, color, alpha, emissive, blendMode, VisualLightMode.PROFILE, VisualRenderPass.AUTO, null, null, 0, null, null, null, 0, VisualEffectProfile.NONE);
   }

   public VisualMaterial(String id, int color, float alpha, boolean emissive, AnimationBlendMode blendMode,
         VisualLightMode lightMode, VisualRenderPass renderPass, Boolean cull, Boolean depthWrite, int sortOrder) {
      this(id, color, alpha, emissive, blendMode, lightMode, renderPass, cull, depthWrite, sortOrder, null, null, null, 0, VisualEffectProfile.NONE);
   }

   public VisualMaterial(String id, int color, float alpha, boolean emissive, AnimationBlendMode blendMode,
         VisualLightMode lightMode, VisualRenderPass renderPass, Boolean cull, Boolean depthWrite, int sortOrder,
         Integer lightOverride, Integer overlayOverride, Integer outlineColor, int renderPriority) {
      this(id, color, alpha, emissive, blendMode, lightMode, renderPass, cull, depthWrite, sortOrder,
         lightOverride, overlayOverride, outlineColor, renderPriority, VisualEffectProfile.NONE);
   }

   public VisualMaterial {
      id = id == null || id.isBlank() ? "default" : id.trim();
      alpha = Math.max(0.0F, Math.min(1.0F, alpha));
      blendMode = blendMode == null ? AnimationBlendMode.REPLACE : blendMode;
      lightMode = lightMode == null ? VisualLightMode.PROFILE : lightMode;
      renderPass = renderPass == null ? VisualRenderPass.AUTO : renderPass;
      effect = effect == null ? VisualEffectProfile.NONE : effect;
   }

   public boolean fullbright() {
      return emissive || lightMode.fullbright() || renderPass == VisualRenderPass.EMISSIVE;
   }

   public int priority() {
      return sortOrder + renderPriority;
   }
}
