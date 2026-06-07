package com.knoxhack.echorendercore.profile;

import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public record VisualProfile(
   Identifier id,
   Identifier baseTexture,
   Identifier glowTexture,
   Identifier damagedOverlayTexture,
   Identifier corruptedOverlayTexture,
   Identifier activeOverlayTexture,
   Identifier animationProfile,
   Identifier particleProfile,
   VisualState defaultState,
   Map<VisualState, String> stateAnimations,
   Map<VisualState, List<Identifier>> stateOverlays,
   Map<VisualState, Identifier> stateTextureVariants,
   Map<VisualVariant, Identifier> variantTextures,
   Map<String, RenderCoreAnchor> anchors,
   int schemaVersion,
   float transitionSeconds,
   List<VisualLayerProfile> layers,
   Map<String, VisualMaterial> materials,
   Map<String, BlockPartSelectorProfile> blockParts,
   List<VisualProfileReference> includes,
   VisualEffectProfile effect,
   VisualSurfaceProfile surface,
   VisualFallbackProfile fallback,
   VisualBudgetProfile budget,
   VisualScreenChromeProfile screenChrome,
   VisualQaProfile qa
) {
   public static final int CURRENT_SCHEMA_VERSION = 12;

   public VisualProfile(Identifier id, Identifier baseTexture, Identifier glowTexture, Identifier damagedOverlayTexture,
         Identifier corruptedOverlayTexture, Identifier activeOverlayTexture, Identifier animationProfile,
         Identifier particleProfile, VisualState defaultState, Map<VisualState, String> stateAnimations,
         Map<VisualState, List<Identifier>> stateOverlays, Map<VisualState, Identifier> stateTextureVariants,
         Map<VisualVariant, Identifier> variantTextures, Map<String, RenderCoreAnchor> anchors) {
      this(id, baseTexture, glowTexture, damagedOverlayTexture, corruptedOverlayTexture, activeOverlayTexture,
         animationProfile, particleProfile, defaultState, stateAnimations, stateOverlays, stateTextureVariants,
         variantTextures, anchors, 1, 0.15F, List.of(), Map.of(), Map.of(), List.of(), VisualEffectProfile.NONE);
   }

   public VisualProfile(Identifier id, Identifier baseTexture, Identifier glowTexture, Identifier damagedOverlayTexture,
         Identifier corruptedOverlayTexture, Identifier activeOverlayTexture, Identifier animationProfile,
         Identifier particleProfile, VisualState defaultState, Map<VisualState, String> stateAnimations,
         Map<VisualState, List<Identifier>> stateOverlays, Map<VisualState, Identifier> stateTextureVariants,
         Map<VisualVariant, Identifier> variantTextures, Map<String, RenderCoreAnchor> anchors,
         int schemaVersion, float transitionSeconds, List<VisualLayerProfile> layers,
         Map<String, VisualMaterial> materials) {
      this(id, baseTexture, glowTexture, damagedOverlayTexture, corruptedOverlayTexture, activeOverlayTexture,
         animationProfile, particleProfile, defaultState, stateAnimations, stateOverlays, stateTextureVariants,
         variantTextures, anchors, schemaVersion, transitionSeconds, layers, materials, Map.of(), List.of(), VisualEffectProfile.NONE);
   }

   public VisualProfile(Identifier id, Identifier baseTexture, Identifier glowTexture, Identifier damagedOverlayTexture,
         Identifier corruptedOverlayTexture, Identifier activeOverlayTexture, Identifier animationProfile,
         Identifier particleProfile, VisualState defaultState, Map<VisualState, String> stateAnimations,
         Map<VisualState, List<Identifier>> stateOverlays, Map<VisualState, Identifier> stateTextureVariants,
         Map<VisualVariant, Identifier> variantTextures, Map<String, RenderCoreAnchor> anchors,
         int schemaVersion, float transitionSeconds, List<VisualLayerProfile> layers,
         Map<String, VisualMaterial> materials, Map<String, BlockPartSelectorProfile> blockParts) {
      this(id, baseTexture, glowTexture, damagedOverlayTexture, corruptedOverlayTexture, activeOverlayTexture,
         animationProfile, particleProfile, defaultState, stateAnimations, stateOverlays, stateTextureVariants,
         variantTextures, anchors, schemaVersion, transitionSeconds, layers, materials, blockParts, List.of(), VisualEffectProfile.NONE);
   }

   public VisualProfile(Identifier id, Identifier baseTexture, Identifier glowTexture, Identifier damagedOverlayTexture,
         Identifier corruptedOverlayTexture, Identifier activeOverlayTexture, Identifier animationProfile,
         Identifier particleProfile, VisualState defaultState, Map<VisualState, String> stateAnimations,
         Map<VisualState, List<Identifier>> stateOverlays, Map<VisualState, Identifier> stateTextureVariants,
         Map<VisualVariant, Identifier> variantTextures, Map<String, RenderCoreAnchor> anchors,
         int schemaVersion, float transitionSeconds, List<VisualLayerProfile> layers,
         Map<String, VisualMaterial> materials, Map<String, BlockPartSelectorProfile> blockParts,
         List<VisualProfileReference> includes) {
      this(id, baseTexture, glowTexture, damagedOverlayTexture, corruptedOverlayTexture, activeOverlayTexture,
         animationProfile, particleProfile, defaultState, stateAnimations, stateOverlays, stateTextureVariants,
         variantTextures, anchors, schemaVersion, transitionSeconds, layers, materials, blockParts, includes, VisualEffectProfile.NONE);
   }

   public VisualProfile(Identifier id, Identifier baseTexture, Identifier glowTexture, Identifier damagedOverlayTexture,
         Identifier corruptedOverlayTexture, Identifier activeOverlayTexture, Identifier animationProfile,
         Identifier particleProfile, VisualState defaultState, Map<VisualState, String> stateAnimations,
         Map<VisualState, List<Identifier>> stateOverlays, Map<VisualState, Identifier> stateTextureVariants,
         Map<VisualVariant, Identifier> variantTextures, Map<String, RenderCoreAnchor> anchors,
         int schemaVersion, float transitionSeconds, List<VisualLayerProfile> layers,
         Map<String, VisualMaterial> materials, Map<String, BlockPartSelectorProfile> blockParts,
         List<VisualProfileReference> includes, VisualEffectProfile effect) {
      this(id, baseTexture, glowTexture, damagedOverlayTexture, corruptedOverlayTexture, activeOverlayTexture,
         animationProfile, particleProfile, defaultState, stateAnimations, stateOverlays, stateTextureVariants,
         variantTextures, anchors, schemaVersion, transitionSeconds, layers, materials, blockParts, includes, effect,
         VisualSurfaceProfile.defaults(id), VisualFallbackProfile.DEFAULT, VisualBudgetProfile.DEFAULT,
         VisualScreenChromeProfile.DEFAULT, VisualQaProfile.DEFAULT);
   }

   public VisualProfile {
      defaultState = defaultState == null ? VisualState.IDLE : defaultState;
      stateAnimations = stateAnimations == null ? Map.of() : Map.copyOf(stateAnimations);
      stateOverlays = stateOverlays == null ? Map.of() : copyOverlayMap(stateOverlays);
      stateTextureVariants = stateTextureVariants == null ? Map.of() : Map.copyOf(stateTextureVariants);
      variantTextures = variantTextures == null ? Map.of() : Map.copyOf(variantTextures);
      anchors = anchors == null ? Map.of() : Map.copyOf(anchors);
      schemaVersion = Math.max(1, schemaVersion);
      transitionSeconds = Math.max(0.0F, transitionSeconds);
      layers = layers == null ? List.of() : List.copyOf(layers);
      materials = materials == null ? Map.of() : Map.copyOf(materials);
      blockParts = blockParts == null ? Map.of() : Map.copyOf(blockParts);
      includes = includes == null ? List.of() : List.copyOf(includes);
      effect = effect == null ? VisualEffectProfile.NONE : effect;
      surface = surface == null ? VisualSurfaceProfile.defaults(id) : surface;
      fallback = fallback == null ? VisualFallbackProfile.DEFAULT : fallback;
      budget = budget == null ? VisualBudgetProfile.DEFAULT : budget;
      screenChrome = screenChrome == null ? VisualScreenChromeProfile.DEFAULT : screenChrome;
      qa = qa == null ? VisualQaProfile.DEFAULT : qa;
   }

   public Identifier textureFor(VisualState state, VisualVariant variant) {
      if (variant != null && !variant.isDefault() && variantTextures.containsKey(variant)) {
         return variantTextures.get(variant);
      }
      return stateTextureVariants.getOrDefault(state, baseTexture);
   }

   public List<Identifier> overlaysFor(VisualState state) {
      return stateOverlays.getOrDefault(state, List.of());
   }

   public String animationFor(VisualState state) {
      return stateAnimations.get(state);
   }

   public RenderCoreAnchor anchor(String name) {
      return anchors.get(name);
   }

   public List<VisualLayerProfile> layersFor(VisualState state, VisualVariant variant) {
      return layers.stream().filter(layer -> layer.matches(state, variant)).toList();
   }

   public VisualMaterial material(String id) {
      return materials.getOrDefault(id == null || id.isBlank() ? "default" : id, VisualMaterial.DEFAULT);
   }

   public VisualEffectProfile effectFor(VisualLayerProfile layer) {
      if (layer == null) {
         return effect;
      }
      VisualMaterial material = material(layer.material());
      return layer.effect().mergeOver(material.effect().mergeOver(effect));
   }

   public BlockPartSelectorProfile blockPart(String id) {
      return id == null ? null : blockParts.get(id);
   }

   private static Map<VisualState, List<Identifier>> copyOverlayMap(Map<VisualState, List<Identifier>> source) {
      return source.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
         Map.Entry::getKey,
         entry -> entry.getValue() == null ? List.of() : List.copyOf(entry.getValue())
      ));
   }
}
