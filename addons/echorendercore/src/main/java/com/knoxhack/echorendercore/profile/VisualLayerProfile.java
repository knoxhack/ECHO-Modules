package com.knoxhack.echorendercore.profile;

import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record VisualLayerProfile(
   String id,
   VisualLayerKind kind,
   Identifier texture,
   String material,
   Set<VisualState> states,
   Set<VisualVariant> variants,
   List<String> partFilter,
   int color,
   float alpha,
   boolean emissive,
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
   public VisualLayerProfile(String id, VisualLayerKind kind, Identifier texture, String material,
         Set<VisualState> states, Set<VisualVariant> variants, List<String> partFilter,
         int color, float alpha, boolean emissive) {
      this(id, kind, texture, material, states, variants, partFilter, color, alpha, emissive,
         VisualLightMode.PROFILE, VisualRenderPass.AUTO, null, null, 0, null, null, null, 0, VisualEffectProfile.NONE);
   }

   public VisualLayerProfile(String id, VisualLayerKind kind, Identifier texture, String material,
         Set<VisualState> states, Set<VisualVariant> variants, List<String> partFilter,
         int color, float alpha, boolean emissive, VisualLightMode lightMode, VisualRenderPass renderPass,
         Boolean cull, Boolean depthWrite, int sortOrder) {
      this(id, kind, texture, material, states, variants, partFilter, color, alpha, emissive,
         lightMode, renderPass, cull, depthWrite, sortOrder, null, null, null, 0, VisualEffectProfile.NONE);
   }

   public VisualLayerProfile(String id, VisualLayerKind kind, Identifier texture, String material,
         Set<VisualState> states, Set<VisualVariant> variants, List<String> partFilter,
         int color, float alpha, boolean emissive, VisualLightMode lightMode, VisualRenderPass renderPass,
         Boolean cull, Boolean depthWrite, int sortOrder, Integer lightOverride, Integer overlayOverride,
         Integer outlineColor, int renderPriority) {
      this(id, kind, texture, material, states, variants, partFilter, color, alpha, emissive,
         lightMode, renderPass, cull, depthWrite, sortOrder, lightOverride, overlayOverride, outlineColor,
         renderPriority, VisualEffectProfile.NONE);
   }

   public VisualLayerProfile {
      id = id == null || id.isBlank() ? "layer" : id.trim();
      kind = kind == null ? VisualLayerKind.OVERLAY : kind;
      material = material == null || material.isBlank() ? "default" : material.trim();
      states = states == null ? Set.of() : Set.copyOf(states);
      variants = variants == null ? Set.of() : Set.copyOf(variants);
      partFilter = partFilter == null ? List.of() : partFilter.stream()
         .filter(value -> value != null && !value.isBlank())
         .map(String::trim)
         .toList();
      alpha = Math.max(0.0F, Math.min(1.0F, alpha));
      lightMode = lightMode == null ? VisualLightMode.PROFILE : lightMode;
      renderPass = renderPass == null ? VisualRenderPass.AUTO : renderPass;
      effect = effect == null ? VisualEffectProfile.NONE : effect;
   }

   public boolean matches(VisualState state, VisualVariant variant) {
      boolean stateMatches = states.isEmpty() || states.contains(state);
      boolean variantMatches = variants.isEmpty() || variants.contains(variant == null ? VisualVariant.DEFAULT : variant);
      return stateMatches && variantMatches;
   }

   public int colorWithAlpha() {
      int base = color == 0 ? 0xFFFFFFFF : color;
      int scaledAlpha = Math.round(((base >>> 24) & 0xFF) * alpha);
      return (scaledAlpha << 24) | (base & 0x00FFFFFF);
   }

   public boolean fullbright(VisualMaterial materialProfile) {
      VisualMaterial resolved = materialProfile == null ? VisualMaterial.DEFAULT : materialProfile;
      return emissive || lightMode.fullbright() || renderPass == VisualRenderPass.EMISSIVE || resolved.fullbright();
   }

   public int effectiveSortOrder(VisualMaterial materialProfile) {
      VisualMaterial resolved = materialProfile == null ? VisualMaterial.DEFAULT : materialProfile;
      return sortOrder + renderPriority + resolved.priority();
   }

   public int effectiveLight(VisualMaterial materialProfile, int fallbackLight) {
      VisualMaterial resolved = materialProfile == null ? VisualMaterial.DEFAULT : materialProfile;
      if (lightOverride != null) {
         return lightOverride;
      }
      if (resolved.lightOverride() != null) {
         return resolved.lightOverride();
      }
      return fullbright(resolved) ? 0xF000F0 : fallbackLight;
   }

   public int effectiveOverlay(VisualMaterial materialProfile, int fallbackOverlay) {
      VisualMaterial resolved = materialProfile == null ? VisualMaterial.DEFAULT : materialProfile;
      if (overlayOverride != null) {
         return overlayOverride;
      }
      return resolved.overlayOverride() == null ? fallbackOverlay : resolved.overlayOverride();
   }

   public int effectiveOutlineColor(VisualMaterial materialProfile, int fallbackOutlineColor) {
      VisualMaterial resolved = materialProfile == null ? VisualMaterial.DEFAULT : materialProfile;
      if (outlineColor != null) {
         return outlineColor;
      }
      return resolved.outlineColor() == null ? fallbackOutlineColor : resolved.outlineColor();
   }
}
