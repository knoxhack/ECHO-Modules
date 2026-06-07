package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.knoxhack.echorendercore.animation.AnimationChannel;
import com.knoxhack.echorendercore.animation.AnimationClip;
import com.knoxhack.echorendercore.animation.AnimationBlendMode;
import com.knoxhack.echorendercore.animation.AnimationKeyframe;
import com.knoxhack.echorendercore.animation.AnimationTrack;
import com.knoxhack.echorendercore.animation.AnimationTimeline;
import com.knoxhack.echorendercore.animation.Easing;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public final class RenderCoreJsonParsers {
   private RenderCoreJsonParsers() {
   }

   public static int visualSchemaVersion(JsonObject json) {
      return integerEither(json, "schema_version", "schemaVersion", 1);
   }

   public static boolean visualProfileRequiresMigration(JsonObject json) {
      return visualSchemaVersion(json) != VisualProfile.CURRENT_SCHEMA_VERSION;
   }

   public static VisualProfile parseRuntimeVisualProfile(Identifier id, JsonObject json) {
      int schemaVersion = visualSchemaVersion(json);
      if (schemaVersion == 11) {
         return parseVisualProfile(id, RenderCoreProfileMigration.migrateVisualProfile(id, json).migratedJson());
      }
      if (schemaVersion < 11 || schemaVersion > VisualProfile.CURRENT_SCHEMA_VERSION) {
         throw new JsonParseException("migration_required: profile uses schema_version " + schemaVersion
            + " and RenderCore runtime requires schema_version " + VisualProfile.CURRENT_SCHEMA_VERSION
            + " or auto-migratable schema_version 11.");
      }
      return parseVisualProfile(id, json);
   }

   public static VisualProfile parseVisualProfile(Identifier id, JsonObject json) {
      Identifier baseTexture = identifier(json, "base_texture", null);
      if (baseTexture == null) {
         baseTexture = identifier(json, "baseTexture", null);
      }
      VisualState defaultState = state(string(json, "default_state", string(json, "defaultState", "IDLE")), VisualState.IDLE);
      return new VisualProfile(
         id,
         baseTexture,
         identifierEither(json, "glow_texture", "glowTexture"),
         identifierEither(json, "damaged_overlay_texture", "damagedOverlayTexture"),
         identifierEither(json, "corrupted_overlay_texture", "corruptedOverlayTexture"),
         identifierEither(json, "active_overlay_texture", "activeOverlayTexture"),
         identifierEither(json, "animation_profile", "animationProfile"),
         identifierEither(json, "particle_profile", "particleProfile"),
         defaultState,
         stateStringMap(objectEither(json, "state_animations", "stateAnimations")),
         stateOverlayMap(objectEither(json, "state_overlays", "stateOverlays")),
         stateIdentifierMap(objectEither(json, "state_texture_variants", "stateTextureVariants")),
         variantTextures(objectEither(json, "variants", "variant_textures")),
         anchors(object(json, "anchors")),
         visualSchemaVersion(json),
         decimalEither(json, "transition_seconds", "transitionSeconds", 0.15F),
         visualLayers(json),
         materials(object(json, "materials")),
         blockParts(objectEither(json, "block_parts", "blockParts")),
         includes(array(json, "includes")),
         effect(objectEither(json, "effect", "effects"), VisualEffectProfile.NONE),
         surface(id, object(json, "surface")),
         fallback(object(json, "fallback")),
         budget(object(json, "budget")),
         screenChrome(objectEither(json, "screen_chrome", "screenChrome")),
         qa(object(json, "qa"))
      );
   }

   public static AnimationProfile parseAnimationProfile(Identifier id, JsonObject json) {
      JsonObject animationsJson = object(json, "animations");
      Map<String, AnimationClip> animations = new LinkedHashMap<>();
      if (animationsJson != null) {
         for (Map.Entry<String, JsonElement> entry : animationsJson.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
               throw new JsonParseException("Animation '" + entry.getKey() + "' must be an object.");
            }
            animations.put(entry.getKey(), parseClip(entry.getKey(), entry.getValue().getAsJsonObject()));
         }
      }
      return new AnimationProfile(id, animations);
   }

   public static ParticleProfile parseParticleProfile(Identifier id, JsonObject json) {
      JsonObject emittersJson = object(json, "emitters");
      Map<String, ParticleEmitter> emitters = new LinkedHashMap<>();
      if (emittersJson != null) {
         for (Map.Entry<String, JsonElement> entry : emittersJson.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
               throw new JsonParseException("Emitter '" + entry.getKey() + "' must be an object.");
            }
            emitters.put(entry.getKey(), parseEmitter(entry.getKey(), entry.getValue().getAsJsonObject()));
         }
      }
      return new ParticleProfile(id, emitters);
   }

   private static AnimationClip parseClip(String id, JsonObject json) {
      JsonArray tracksJson = array(json, "tracks");
      List<AnimationTrack> tracks = new ArrayList<>();
      if (tracksJson != null) {
         for (int i = 0; i < tracksJson.size(); i++) {
            JsonElement element = tracksJson.get(i);
            if (!element.isJsonObject()) {
               throw new JsonParseException("Animation '" + id + "' track " + i + " must be an object.");
            }
            tracks.add(parseTrack(id, i, element.getAsJsonObject()));
         }
      }
      return new AnimationClip(
         id,
         bool(json, "loop", false),
         decimal(json, "length", 1.0F),
         tracks,
         decimalEither(json, "transition_seconds", "transitionSeconds", AnimationClip.DEFAULT_TRANSITION_SECONDS),
         AnimationBlendMode.byName(stringEither(json, "blend_mode", "blendMode", "replace"))
      );
   }

   private static AnimationTrack parseTrack(String clipId, int index, JsonObject json) {
      String part = string(json, "part", "");
      if (part.isBlank()) {
         throw new JsonParseException("Animation '" + clipId + "' track " + index + " is missing part.");
      }
      return new AnimationTrack(
         part,
         AnimationChannel.byName(string(json, "channel", "position_y")),
         decimal(json, "from", 0.0F),
         decimal(json, "to", 0.0F),
         decimalEither(json, "start_time", "startTime", 0.0F),
         decimalEither(json, "end_time", "endTime", decimal(json, "length", 0.0F)),
         Easing.byName(string(json, "easing", "linear")),
         timeline(json, clipId, index),
         AnimationBlendMode.byName(stringEither(json, "blend_mode", "blendMode", "replace"))
      );
   }

   private static ParticleEmitter parseEmitter(String id, JsonObject json) {
      Identifier particle = identifier(json, "particle", null);
      if (particle == null) {
         throw new JsonParseException("Particle emitter '" + id + "' is missing particle.");
      }
      String stateName = string(json, "state", "");
      ParticleOptionsSpec options = particleOptions(json);
      return new ParticleEmitter(
         id,
         string(json, "anchor", ""),
         particle,
         stateName.isBlank() ? null : state(stateName, null),
         states(array(json, "states")),
         decimal(json, "rate", 0.0F),
         integerEither(json, "burst_count", "burstCount", 0),
         vector(json, "offset", RenderCoreVector.ZERO),
         vector(json, "velocity", RenderCoreVector.ZERO),
         vector(json, "spread", RenderCoreVector.ZERO),
         integer(json, "lifetime", 0),
         color(json, "color", 0xFFFFFFFF),
         boolEither(json, "requires_moving", "requiresMoving", bool(json, "moving", false)),
         boolEither(json, "requires_damaged", "requiresDamaged", bool(json, "damaged", false)),
         decimalEither(json, "min_progress", "minProgress", 0.0F),
         decimalEither(json, "max_progress", "maxProgress", 1.0F),
         options
      );
   }

   private static AnimationTimeline timeline(JsonObject json, String clipId, int trackIndex) {
      JsonArray keyframes = array(json, "keyframes");
      if (keyframes == null) {
         return AnimationTimeline.EMPTY;
      }
      List<AnimationKeyframe> frames = new ArrayList<>();
      Easing defaultEasing = Easing.byName(string(json, "easing", "linear"));
      for (int i = 0; i < keyframes.size(); i++) {
         JsonElement element = keyframes.get(i);
         if (!element.isJsonObject()) {
            throw new JsonParseException("Animation '" + clipId + "' track " + trackIndex + " keyframe " + i + " must be an object.");
         }
         JsonObject frame = element.getAsJsonObject();
         frames.add(new AnimationKeyframe(
            decimal(frame, "time", 0.0F),
            decimal(frame, "value", 0.0F),
            Easing.byName(string(frame, "easing", defaultEasing.name().toLowerCase(java.util.Locale.ROOT)))
         ));
      }
      return new AnimationTimeline(frames);
   }

   private static List<VisualLayerProfile> visualLayers(JsonObject json) {
      List<VisualLayerProfile> layers = new ArrayList<>();
      JsonArray layerArray = array(json, "layers");
      if (layerArray != null) {
         for (int i = 0; i < layerArray.size(); i++) {
            JsonElement element = layerArray.get(i);
            if (!element.isJsonObject()) {
               throw new JsonParseException("Visual layer " + i + " must be an object.");
            }
            layers.add(parseLayer("layer_" + i, element.getAsJsonObject()));
         }
      }
      addLegacyLayer(layers, "glow", VisualLayerKind.GLOW, identifierEither(json, "glow_texture", "glowTexture"),
         Set.of(VisualState.ONLINE, VisualState.ACTIVE, VisualState.WORKING, VisualState.SCANNING, VisualState.CHARGING, VisualState.COMPLETE));
      addLegacyLayer(layers, "damaged", VisualLayerKind.OVERLAY, identifierEither(json, "damaged_overlay_texture", "damagedOverlayTexture"),
         Set.of(VisualState.DAMAGED));
      addLegacyLayer(layers, "corrupted", VisualLayerKind.OVERLAY, identifierEither(json, "corrupted_overlay_texture", "corruptedOverlayTexture"),
         Set.of(VisualState.CORRUPTED));
      addLegacyLayer(layers, "active", VisualLayerKind.OVERLAY, identifierEither(json, "active_overlay_texture", "activeOverlayTexture"),
         Set.of(VisualState.ACTIVE, VisualState.WORKING, VisualState.SCANNING));
      JsonObject overlays = objectEither(json, "state_overlays", "stateOverlays");
      if (overlays != null) {
         for (Map.Entry<String, JsonElement> entry : overlays.entrySet()) {
            VisualState state = state(entry.getKey(), VisualState.IDLE);
            if (entry.getValue().isJsonArray()) {
               int index = 0;
               for (JsonElement overlay : entry.getValue().getAsJsonArray()) {
                  addLegacyLayer(layers, "overlay_" + state.name().toLowerCase(java.util.Locale.ROOT) + "_" + index++,
                     VisualLayerKind.OVERLAY, Identifier.parse(stringElement(overlay, "state overlay " + entry.getKey())), Set.of(state));
               }
            } else {
               addLegacyLayer(layers, "overlay_" + state.name().toLowerCase(java.util.Locale.ROOT), VisualLayerKind.OVERLAY,
                  Identifier.parse(stringElement(entry.getValue(), "state overlay " + entry.getKey())), Set.of(state));
            }
         }
      }
      return layers;
   }

   private static VisualLayerProfile parseLayer(String fallbackId, JsonObject json) {
      return new VisualLayerProfile(
         string(json, "id", fallbackId),
         VisualLayerKind.byName(string(json, "kind", string(json, "type", "overlay"))),
         identifier(json, "texture", null),
         string(json, "material", "default"),
         states(array(json, "states"), string(json, "state", "")),
         variants(array(json, "variants"), string(json, "variant", "")),
         stringList(arrayAny(json, "parts", "part_filter", "partFilter")),
         color(json, "color", 0xFFFFFFFF),
         decimal(json, "alpha", 1.0F),
         bool(json, "emissive", false),
         VisualLightMode.byName(stringEither(json, "light_mode", "lightMode", "profile")),
         VisualRenderPass.byName(stringEither(json, "render_pass", "renderPass", "auto")),
         optionalBoolEither(json, "cull", "cull"),
         optionalBoolEither(json, "depth_write", "depthWrite"),
         integerEither(json, "sort_order", "sortOrder", 0),
         optionalIntegerEither(json, "light_override", "lightOverride"),
         optionalIntegerEither(json, "overlay_override", "overlayOverride"),
         optionalColorEither(json, "outline_color", "outlineColor"),
         integerEither(json, "render_priority", "renderPriority", 0),
         effect(objectEither(json, "effect", "effects"), VisualEffectProfile.NONE)
      );
   }

   private static void addLegacyLayer(List<VisualLayerProfile> layers, String id, VisualLayerKind kind, Identifier texture, Set<VisualState> states) {
      if (texture != null) {
         layers.add(new VisualLayerProfile(id, kind, texture, "default", states, Set.of(), List.of(), 0xFFFFFFFF, 1.0F, kind == VisualLayerKind.GLOW));
      }
   }

   private static Map<String, VisualMaterial> materials(JsonObject json) {
      if (json == null) {
         return Map.of();
      }
      Map<String, VisualMaterial> materials = new LinkedHashMap<>();
      for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
         if (!entry.getValue().isJsonObject()) {
            throw new JsonParseException("Material '" + entry.getKey() + "' must be an object.");
         }
         JsonObject material = entry.getValue().getAsJsonObject();
         materials.put(entry.getKey(), new VisualMaterial(
            entry.getKey(),
            color(material, "color", 0xFFFFFFFF),
            decimal(material, "alpha", 1.0F),
            bool(material, "emissive", false),
            AnimationBlendMode.byName(stringEither(material, "blend_mode", "blendMode", "replace")),
            VisualLightMode.byName(stringEither(material, "light_mode", "lightMode", "profile")),
            VisualRenderPass.byName(stringEither(material, "render_pass", "renderPass", "auto")),
            optionalBoolEither(material, "cull", "cull"),
            optionalBoolEither(material, "depth_write", "depthWrite"),
            integerEither(material, "sort_order", "sortOrder", 0),
            optionalIntegerEither(material, "light_override", "lightOverride"),
            optionalIntegerEither(material, "overlay_override", "overlayOverride"),
            optionalColorEither(material, "outline_color", "outlineColor"),
            integerEither(material, "render_priority", "renderPriority", 0),
            effect(objectEither(material, "effect", "effects"), VisualEffectProfile.NONE)
         ));
      }
      return materials;
   }

   private static VisualEffectProfile effect(JsonObject json, VisualEffectProfile fallback) {
      if (json == null) {
         return fallback == null ? VisualEffectProfile.NONE : fallback;
      }
      return new VisualEffectProfile(
         VisualEffectKind.byName(stringEither(json, "preset", "kind", string(json, "type", "none"))),
         decimalEither(json, "glow_intensity", "glowIntensity", 0.0F),
         decimalEither(json, "bloom_intensity", "bloomIntensity", 0.0F),
         decimalEither(json, "pulse_speed", "pulseSpeed", 0.0F),
         decimalEither(json, "pulse_min_alpha", "pulseMinAlpha", 1.0F),
         decimalEither(json, "pulse_max_alpha", "pulseMaxAlpha", 1.0F),
         decimalEither(json, "flicker_intensity", "flickerIntensity", 0.0F),
         decimalEither(json, "scanline_strength", "scanlineStrength", 0.0F),
         decimalEither(json, "hue_shift_speed", "hueShiftSpeed", 0.0F),
         decimalEither(json, "depth_bias", "depthBias", 0.0F),
         boolEither(json, "advanced_enabled", "advancedEnabled", false),
         decimalEither(json, "bloom_radius", "bloomRadius", 0.0F),
         decimalEither(json, "bloom_threshold", "bloomThreshold", 1.0F),
         integerEither(json, "bloom_passes", "bloomPasses", 0),
         decimalEither(json, "screen_blend", "screenBlend", 0.0F),
         VisualEffectTargetScope.byName(stringEither(json, "target_scope", "targetScope", "profile")),
         VisualEffectBloomMaskMode.byName(stringEither(json, "bloom_mask_mode", "bloomMaskMode", "auto")),
         optionalColorEither(json, "bloom_tint", "bloomTint"),
         optionalDecimalEither(json, "bloom_mask_alpha", "bloomMaskAlpha"),
         stringEither(json, "bloom_channel", "bloomChannel", "default"),
         optionalIntegerEither(json, "bloom_downscale", "bloomDownscale"),
         integerEither(json, "advanced_priority", "advancedPriority", 0)
      );
   }

   private static VisualSurfaceProfile surface(Identifier id, JsonObject json) {
      VisualSurfaceProfile defaults = VisualSurfaceProfile.defaults(id);
      if (json == null) {
         return defaults;
      }
      return new VisualSurfaceProfile(
         VisualSurfaceType.byName(string(json, "type", defaults.type().id())),
         stringEither(json, "owner_addon", "ownerAddon", defaults.ownerAddon()),
         stringEither(json, "display_name", "displayName", defaults.displayName()),
         stringList(array(json, "tags")),
         stringEither(json, "integration_mode", "integrationMode", defaults.integrationMode())
      );
   }

   private static VisualFallbackProfile fallback(JsonObject json) {
      if (json == null) {
         return VisualFallbackProfile.DEFAULT;
      }
      return new VisualFallbackProfile(
         VisualFallbackMode.byName(string(json, "mode", VisualFallbackProfile.DEFAULT.mode().id())),
         string(json, "expectation", VisualFallbackProfile.DEFAULT.expectation()),
         boolEither(json, "allow_advanced_fx_fallback", "allowAdvancedFxFallback", true),
         boolEither(json, "allow_missing_assets", "allowMissingAssets", false)
      );
   }

   private static VisualBudgetProfile budget(JsonObject json) {
      if (json == null) {
         return VisualBudgetProfile.DEFAULT;
      }
      return new VisualBudgetProfile(
         VisualBudgetTier.byName(string(json, "tier", VisualBudgetProfile.DEFAULT.tier().id())),
         optionalIntegerEither(json, "max_layers", "maxLayers"),
         optionalIntegerEither(json, "max_emitters", "maxEmitters"),
         optionalIntegerEither(json, "max_effect_cost", "maxEffectCost"),
         optionalIntegerEither(json, "max_bloom_cost", "maxBloomCost"),
         optionalIntegerEither(json, "max_mask_submissions", "maxMaskSubmissions"),
         VisualAdvancedFxPolicy.byName(stringEither(json, "advanced_fx", "advancedFx", VisualAdvancedFxPolicy.AUTO.id()))
      );
   }

   private static VisualScreenChromeProfile screenChrome(JsonObject json) {
      if (json == null) {
         return VisualScreenChromeProfile.DEFAULT;
      }
      VisualScreenChromeProfile defaults = VisualScreenChromeProfile.DEFAULT;
      return new VisualScreenChromeProfile(
         string(json, "style", defaults.style()),
         string(json, "label", defaults.label()),
         bool(json, "backdrop", defaults.backdrop()),
         boolEither(json, "edge_glow", "edgeGlow", defaults.edgeGlow()),
         boolEither(json, "corner_brackets", "cornerBrackets", defaults.cornerBrackets()),
         boolEither(json, "accent_rails", "accentRails", defaults.accentRails()),
         bool(json, "scanlines", defaults.scanlines()),
         boolEither(json, "glass_glints", "glassGlints", defaults.glassGlints()),
         boolEither(json, "chromatic_edge", "chromaticEdge", defaults.chromaticEdge()),
         boolEither(json, "quiet_fallback", "quietFallback", defaults.quietFallback())
      );
   }

   private static VisualQaProfile qa(JsonObject json) {
      if (json == null) {
         return VisualQaProfile.DEFAULT;
      }
      return new VisualQaProfile(
         bool(json, "required", false),
         Set.copyOf(stringList(array(json, "evidence"))),
         boolEither(json, "reduced_motion", "reducedMotion", false)
      );
   }

   private static List<VisualProfileReference> includes(JsonArray array) {
      if (array == null) {
         return List.of();
      }
      List<VisualProfileReference> references = new ArrayList<>();
      for (int i = 0; i < array.size(); i++) {
         JsonElement element = array.get(i);
         if (element.isJsonPrimitive()) {
            references.add(new VisualProfileReference(Identifier.parse(stringElement(element, "includes." + i)), Set.of(), Set.of()));
         } else if (element.isJsonObject()) {
            JsonObject json = element.getAsJsonObject();
            Identifier profile = identifier(json, "profile", identifier(json, "id", null));
            if (profile == null) {
               throw new JsonParseException("Include " + i + " is missing profile.");
            }
            references.add(new VisualProfileReference(
               profile,
               states(array(json, "states"), string(json, "state", "")),
               variants(array(json, "variants"), string(json, "variant", ""))
            ));
         } else {
            throw new JsonParseException("Include " + i + " must be a string or object.");
         }
      }
      return references;
   }

   private static Map<String, BlockPartSelectorProfile> blockParts(JsonObject json) {
      if (json == null) {
         return Map.of();
      }
      Map<String, BlockPartSelectorProfile> values = new LinkedHashMap<>();
      for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
         JsonElement value = entry.getValue();
         if (value.isJsonArray()) {
            values.put(entry.getKey(), new BlockPartSelectorProfile(entry.getKey(), intList(value.getAsJsonArray()), Set.of(), 0, null, List.of()));
         } else if (value.isJsonObject()) {
            values.put(entry.getKey(), blockPart(entry.getKey(), value.getAsJsonObject()));
         } else {
            throw new JsonParseException("Block part selector '" + entry.getKey() + "' must be an object or index array.");
         }
      }
      return values;
   }

   private static BlockPartSelectorProfile blockPart(String id, JsonObject json) {
      return new BlockPartSelectorProfile(
         id,
         intList(arrayEither(json, "indices", "index")),
         directions(arrayEither(json, "directions", "direction")),
         integerEither(json, "material_flags", "materialFlags", 0),
         optionalBoolEither(json, "ambient_occlusion", "ambientOcclusion"),
         intList(arrayEither(json, "tint_indices", "tintIndices")),
         blockStateRules(objectEither(json, "block_state", "blockState"))
      );
   }

   private static Map<String, Set<String>> blockStateRules(JsonObject json) {
      if (json == null) {
         return Map.of();
      }
      Map<String, Set<String>> rules = new LinkedHashMap<>();
      for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
         JsonElement value = entry.getValue();
         if (value == null || value.isJsonNull()) {
            rules.put(entry.getKey(), Set.of());
         } else if (value.isJsonArray()) {
            rules.put(entry.getKey(), Set.copyOf(stringList(value.getAsJsonArray())));
         } else {
            rules.put(entry.getKey(), Set.of(stringElement(value, "block_state." + entry.getKey())));
         }
      }
      return rules;
   }

   private static ParticleOptionsSpec particleOptions(JsonObject json) {
      JsonObject optionsJson = object(json, "options");
      String type = optionsJson == null ? string(json, "option_type", "") : string(optionsJson, "type", string(json, "option_type", ""));
      int fallbackColor = color(json, "color", 0xFFFFFFFF);
      int color = optionsJson == null ? fallbackColor : color(optionsJson, "color", fallbackColor);
      float scale = optionsJson == null ? decimal(json, "scale", 1.0F) : decimal(optionsJson, "scale", decimal(json, "scale", 1.0F));
      int lifetime = optionsJson == null ? integer(json, "lifetime", 0) : integer(optionsJson, "lifetime", integer(json, "lifetime", 0));
      Map<String, String> custom = new LinkedHashMap<>();
      if (optionsJson != null) {
         for (Map.Entry<String, JsonElement> entry : optionsJson.entrySet()) {
            if (!entry.getKey().equals("type") && !entry.getKey().equals("color") && !entry.getKey().equals("scale") && !entry.getKey().equals("lifetime")) {
               custom.put(entry.getKey(), entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : entry.getValue().toString());
            }
         }
      }
      return new ParticleOptionsSpec(type, color, scale, lifetime, custom);
   }

   private static Map<VisualState, String> stateStringMap(JsonObject json) {
      if (json == null) {
         return Map.of();
      }
      Map<VisualState, String> values = new EnumMap<>(VisualState.class);
      for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
         values.put(state(entry.getKey(), VisualState.IDLE), stringElement(entry.getValue(), "state animation " + entry.getKey()));
      }
      return values;
   }

   private static Map<VisualState, Identifier> stateIdentifierMap(JsonObject json) {
      if (json == null) {
         return Map.of();
      }
      Map<VisualState, Identifier> values = new EnumMap<>(VisualState.class);
      for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
         values.put(state(entry.getKey(), VisualState.IDLE), Identifier.parse(stringElement(entry.getValue(), "state texture " + entry.getKey())));
      }
      return values;
   }

   private static Map<VisualState, List<Identifier>> stateOverlayMap(JsonObject json) {
      if (json == null) {
         return Map.of();
      }
      Map<VisualState, List<Identifier>> values = new EnumMap<>(VisualState.class);
      for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
         List<Identifier> overlays = new ArrayList<>();
         JsonElement value = entry.getValue();
         if (value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) {
               overlays.add(Identifier.parse(stringElement(element, "state overlay " + entry.getKey())));
            }
         } else {
            overlays.add(Identifier.parse(stringElement(value, "state overlay " + entry.getKey())));
         }
         values.put(state(entry.getKey(), VisualState.IDLE), overlays);
      }
      return values;
   }

   private static Map<VisualVariant, Identifier> variantTextures(JsonObject json) {
      if (json == null) {
         return Map.of();
      }
      Map<VisualVariant, Identifier> values = new LinkedHashMap<>();
      for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
         values.put(VisualVariant.of(entry.getKey()), Identifier.parse(stringElement(entry.getValue(), "variant texture " + entry.getKey())));
      }
      return values;
   }

   private static Map<String, RenderCoreAnchor> anchors(JsonObject json) {
      if (json == null) {
         return Map.of();
      }
      Map<String, RenderCoreAnchor> values = new LinkedHashMap<>();
      for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
         JsonElement element = entry.getValue();
         if (element.isJsonArray()) {
            values.put(entry.getKey(), new RenderCoreAnchor(entry.getKey(), vector(element.getAsJsonArray(), "anchor " + entry.getKey())));
         } else if (element.isJsonObject()) {
            JsonObject anchorJson = element.getAsJsonObject();
            values.put(entry.getKey(), new RenderCoreAnchor(string(anchorJson, "part", entry.getKey()), vector(anchorJson, "offset", RenderCoreVector.ZERO)));
         } else {
            throw new JsonParseException("Anchor '" + entry.getKey() + "' must be an object or vector.");
         }
      }
      return values;
   }

   private static RenderCoreVector vector(JsonObject json, String key, RenderCoreVector fallback) {
      JsonElement element = json.get(key);
      if (element == null || element.isJsonNull()) {
         return fallback;
      }
      if (!element.isJsonArray()) {
         throw new JsonParseException("Field '" + key + "' must be a three-number array.");
      }
      return vector(element.getAsJsonArray(), key);
   }

   private static RenderCoreVector vector(JsonArray array, String label) {
      if (array.size() != 3) {
         throw new JsonParseException("Field '" + label + "' must contain exactly three numbers.");
      }
      return new RenderCoreVector(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
   }

   private static Identifier identifierEither(JsonObject json, String snake, String camel) {
      Identifier value = identifier(json, snake, null);
      return value == null ? identifier(json, camel, null) : value;
   }

   private static Identifier identifier(JsonObject json, String key, Identifier fallback) {
      String value = string(json, key, "");
      return value.isBlank() ? fallback : Identifier.parse(value);
   }

   private static JsonObject objectEither(JsonObject json, String first, String second) {
      JsonObject value = object(json, first);
      return value == null ? object(json, second) : value;
   }

   private static JsonObject object(JsonObject json, String key) {
      JsonElement element = json.get(key);
      if (element == null || element.isJsonNull()) {
         return null;
      }
      if (!element.isJsonObject()) {
         throw new JsonParseException("Field '" + key + "' must be an object.");
      }
      return element.getAsJsonObject();
   }

   private static JsonArray array(JsonObject json, String key) {
      JsonElement element = json.get(key);
      if (element == null || element.isJsonNull()) {
         return null;
      }
      if (!element.isJsonArray()) {
         throw new JsonParseException("Field '" + key + "' must be an array.");
      }
      return element.getAsJsonArray();
   }

   private static JsonArray arrayEither(JsonObject json, String first, String second) {
      JsonArray value = arrayOrSingle(json, first);
      return value == null ? arrayOrSingle(json, second) : value;
   }

   private static JsonArray arrayAny(JsonObject json, String... keys) {
      for (String key : keys) {
         JsonArray value = array(json, key);
         if (value != null) {
            return value;
         }
      }
      return null;
   }

   private static Set<VisualState> states(JsonArray array) {
      return states(array, "");
   }

   private static Set<VisualState> states(JsonArray array, String single) {
      Set<VisualState> states = new HashSet<>();
      if (single != null && !single.isBlank()) {
         states.add(state(single, VisualState.IDLE));
      }
      if (array != null) {
         for (JsonElement element : array) {
            states.add(state(stringElement(element, "state"), VisualState.IDLE));
         }
      }
      return states;
   }

   private static Set<VisualVariant> variants(JsonArray array, String single) {
      Set<VisualVariant> variants = new HashSet<>();
      if (single != null && !single.isBlank()) {
         variants.add(VisualVariant.of(single));
      }
      if (array != null) {
         for (JsonElement element : array) {
            variants.add(VisualVariant.of(stringElement(element, "variant")));
         }
      }
      return variants;
   }

   private static List<String> stringList(JsonArray array) {
      if (array == null) {
         return List.of();
      }
      List<String> values = new ArrayList<>();
      for (JsonElement element : array) {
         values.add(stringElement(element, "string list value"));
      }
      return values;
   }

   private static List<Integer> intList(JsonArray array) {
      if (array == null) {
         return List.of();
      }
      List<Integer> values = new ArrayList<>();
      for (JsonElement element : array) {
         values.add(element.getAsInt());
      }
      return values;
   }

   private static Set<Direction> directions(JsonArray array) {
      if (array == null) {
         return Set.of();
      }
      Set<Direction> values = new HashSet<>();
      for (JsonElement element : array) {
         String value = stringElement(element, "direction").toLowerCase(Locale.ROOT);
         if (!value.equals("all") && !value.equals("*")) {
            values.add(Direction.valueOf(value.toUpperCase(Locale.ROOT)));
         }
      }
      return values;
   }

   private static String string(JsonObject json, String key, String fallback) {
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : stringElement(element, key);
   }

   private static String stringEither(JsonObject json, String first, String second, String fallback) {
      JsonElement element = json.get(first);
      if (element == null || element.isJsonNull()) {
         element = json.get(second);
      }
      return element == null || element.isJsonNull() ? fallback : stringElement(element, first);
   }

   private static String stringElement(JsonElement element, String label) {
      if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
         throw new JsonParseException("Field '" + label + "' must be a string.");
      }
      return element.getAsString();
   }

   private static float decimalEither(JsonObject json, String first, String second, float fallback) {
      JsonElement element = json.get(first);
      if (element == null || element.isJsonNull()) {
         element = json.get(second);
      }
      return element == null || element.isJsonNull() ? fallback : element.getAsFloat();
   }

   private static float decimal(JsonObject json, String key, float fallback) {
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsFloat();
   }

   private static Float optionalDecimalEither(JsonObject json, String first, String second) {
      JsonElement element = json.get(first);
      if (element == null || element.isJsonNull()) {
         element = json.get(second);
      }
      return element == null || element.isJsonNull() ? null : element.getAsFloat();
   }

   private static int integerEither(JsonObject json, String first, String second, int fallback) {
      JsonElement element = json.get(first);
      if (element == null || element.isJsonNull()) {
         element = json.get(second);
      }
      return element == null || element.isJsonNull() ? fallback : element.getAsInt();
   }

   private static int integer(JsonObject json, String key, int fallback) {
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsInt();
   }

   private static boolean bool(JsonObject json, String key, boolean fallback) {
      JsonElement element = json.get(key);
      return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
   }

   private static boolean boolEither(JsonObject json, String first, String second, boolean fallback) {
      JsonElement element = json.get(first);
      if (element == null || element.isJsonNull()) {
         element = json.get(second);
      }
      return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
   }

   private static Boolean optionalBoolEither(JsonObject json, String first, String second) {
      JsonElement element = json.get(first);
      if (element == null || element.isJsonNull()) {
         element = json.get(second);
      }
      return element == null || element.isJsonNull() ? null : element.getAsBoolean();
   }

   private static Integer optionalIntegerEither(JsonObject json, String first, String second) {
      JsonElement element = json.get(first);
      if (element == null || element.isJsonNull()) {
         element = json.get(second);
      }
      return element == null || element.isJsonNull() ? null : element.getAsInt();
   }

   private static Integer optionalColorEither(JsonObject json, String first, String second) {
      if (json.has(first) && !json.get(first).isJsonNull()) {
         return color(json, first, 0);
      }
      return json.has(second) && !json.get(second).isJsonNull() ? color(json, second, 0) : null;
   }

   private static JsonArray arrayOrSingle(JsonObject json, String key) {
      JsonElement element = json.get(key);
      if (element == null || element.isJsonNull()) {
         return null;
      }
      if (element.isJsonArray()) {
         return element.getAsJsonArray();
      }
      JsonArray array = new JsonArray();
      array.add(element);
      return array;
   }

   private static int color(JsonObject json, String key, int fallback) {
      JsonElement element = json.get(key);
      if (element != null && element.isJsonArray()) {
         JsonArray array = element.getAsJsonArray();
         if (array.size() < 3) {
            throw new JsonParseException("Color field '" + key + "' must contain at least three numbers.");
         }
         int r = Math.round(Math.max(0.0F, Math.min(1.0F, array.get(0).getAsFloat())) * 255.0F);
         int g = Math.round(Math.max(0.0F, Math.min(1.0F, array.get(1).getAsFloat())) * 255.0F);
         int b = Math.round(Math.max(0.0F, Math.min(1.0F, array.get(2).getAsFloat())) * 255.0F);
         int a = array.size() > 3 ? Math.round(Math.max(0.0F, Math.min(1.0F, array.get(3).getAsFloat())) * 255.0F) : 255;
         return (a << 24) | (r << 16) | (g << 8) | b;
      }
      String value = string(json, key, "");
      if (value.isBlank()) {
         return fallback;
      }
      String normalized = value.startsWith("#") ? value.substring(1) : value;
      if (normalized.length() == 6) {
         normalized = "FF" + normalized;
      }
      return (int)Long.parseLong(normalized, 16);
   }

   private static VisualState state(String value, VisualState fallback) {
      return VisualState.byName(value, fallback);
   }
}
