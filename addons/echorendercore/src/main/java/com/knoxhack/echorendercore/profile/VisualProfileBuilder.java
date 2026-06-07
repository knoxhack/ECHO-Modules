package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import java.util.Collection;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class VisualProfileBuilder extends RenderCoreProfileBuilder<VisualProfileBuilder> {
   private VisualProfileBuilder(Identifier id) {
      super(id);
      schemaVersion(VisualProfile.CURRENT_SCHEMA_VERSION);
   }

   public static VisualProfileBuilder create(Identifier id) {
      return new VisualProfileBuilder(id);
   }

   public VisualProfileBuilder schemaVersion(int schemaVersion) {
      return add("schema_version", schemaVersion);
   }

   public VisualProfileBuilder baseTexture(Identifier texture) {
      return add("base_texture", texture);
   }

   public VisualProfileBuilder animationProfile(Identifier profile) {
      return add("animation_profile", profile);
   }

   public VisualProfileBuilder particleProfile(Identifier profile) {
      return add("particle_profile", profile);
   }

   public VisualProfileBuilder defaultState(VisualState state) {
      return add("default_state", state == null ? VisualState.IDLE.name() : state.name());
   }

   public VisualProfileBuilder transitionSeconds(float seconds) {
      return add("transition_seconds", seconds);
   }

   public VisualProfileBuilder effect(VisualEffectProfile effect) {
      if (effect != null && effect.active()) {
         json.add("effect", effectJson(effect));
      }
      return this;
   }

   public VisualProfileBuilder surface(VisualSurfaceProfile surface) {
      VisualSurfaceProfile resolved = surface == null ? VisualSurfaceProfile.defaults(id) : surface;
      JsonObject value = new JsonObject();
      value.addProperty("type", resolved.type().id());
      value.addProperty("owner_addon", resolved.ownerAddon());
      value.addProperty("display_name", resolved.displayName());
      value.add("tags", strings(resolved.tags()));
      value.addProperty("integration_mode", resolved.integrationMode());
      json.add("surface", value);
      return this;
   }

   public VisualProfileBuilder surface(VisualSurfaceType type, String displayName) {
      VisualSurfaceProfile defaults = VisualSurfaceProfile.defaults(id);
      return surface(new VisualSurfaceProfile(type, defaults.ownerAddon(), displayName, defaults.tags(), defaults.integrationMode()));
   }

   public VisualProfileBuilder fallback(VisualFallbackProfile fallback) {
      VisualFallbackProfile resolved = fallback == null ? VisualFallbackProfile.DEFAULT : fallback;
      JsonObject value = new JsonObject();
      value.addProperty("mode", resolved.mode().id());
      value.addProperty("expectation", resolved.expectation());
      value.addProperty("allow_advanced_fx_fallback", resolved.allowAdvancedFxFallback());
      value.addProperty("allow_missing_assets", resolved.allowMissingAssets());
      json.add("fallback", value);
      return this;
   }

   public VisualProfileBuilder budget(VisualBudgetProfile budget) {
      VisualBudgetProfile resolved = budget == null ? VisualBudgetProfile.DEFAULT : budget;
      JsonObject value = new JsonObject();
      value.addProperty("tier", resolved.tier().id());
      addOptional(value, "max_layers", resolved.maxLayers());
      addOptional(value, "max_emitters", resolved.maxEmitters());
      addOptional(value, "max_effect_cost", resolved.maxEffectCost());
      addOptional(value, "max_bloom_cost", resolved.maxBloomCost());
      addOptional(value, "max_mask_submissions", resolved.maxMaskSubmissions());
      value.addProperty("advanced_fx", resolved.advancedFx().id());
      json.add("budget", value);
      return this;
   }

   public VisualProfileBuilder screenChrome(VisualScreenChromeProfile chrome) {
      VisualScreenChromeProfile resolved = chrome == null ? VisualScreenChromeProfile.DEFAULT : chrome;
      JsonObject value = new JsonObject();
      value.addProperty("style", resolved.style());
      value.addProperty("label", resolved.label());
      value.addProperty("backdrop", resolved.backdrop());
      value.addProperty("edge_glow", resolved.edgeGlow());
      value.addProperty("corner_brackets", resolved.cornerBrackets());
      value.addProperty("accent_rails", resolved.accentRails());
      value.addProperty("scanlines", resolved.scanlines());
      value.addProperty("glass_glints", resolved.glassGlints());
      value.addProperty("chromatic_edge", resolved.chromaticEdge());
      value.addProperty("quiet_fallback", resolved.quietFallback());
      json.add("screen_chrome", value);
      return this;
   }

   public VisualProfileBuilder qa(VisualQaProfile qa) {
      VisualQaProfile resolved = qa == null ? VisualQaProfile.DEFAULT : qa;
      JsonObject value = new JsonObject();
      value.addProperty("required", resolved.required());
      value.add("evidence", strings(resolved.evidence()));
      value.addProperty("reduced_motion", resolved.reducedMotion());
      json.add("qa", value);
      return this;
   }

   public VisualProfileBuilder stateAnimation(VisualState state, String clip) {
      JsonObject animations = object("state_animations");
      animations.addProperty(state.name(), clip);
      return this;
   }

   public VisualProfileBuilder stateVariantTexture(VisualState state, Identifier texture) {
      JsonObject textures = object("state_texture_variants");
      textures.addProperty(state.name(), texture.toString());
      return this;
   }

   public VisualProfileBuilder variantTexture(VisualVariant variant, Identifier texture) {
      JsonObject variants = object("variants");
      variants.addProperty(variant.id(), texture.toString());
      return this;
   }

   public VisualProfileBuilder material(VisualMaterial material) {
      JsonObject materials = object("materials");
      JsonObject value = new JsonObject();
      value.addProperty("color", color(material.color()));
      value.addProperty("alpha", material.alpha());
      value.addProperty("emissive", material.emissive());
      value.addProperty("blend_mode", material.blendMode().name().toLowerCase(java.util.Locale.ROOT));
      value.addProperty("light_mode", material.lightMode().name().toLowerCase(java.util.Locale.ROOT));
      value.addProperty("render_pass", material.renderPass().name().toLowerCase(java.util.Locale.ROOT));
      if (material.cull() != null) {
         value.addProperty("cull", material.cull());
      }
      if (material.depthWrite() != null) {
         value.addProperty("depth_write", material.depthWrite());
      }
      if (material.sortOrder() != 0) {
         value.addProperty("sort_order", material.sortOrder());
      }
      if (material.lightOverride() != null) {
         value.addProperty("light_override", material.lightOverride());
      }
      if (material.overlayOverride() != null) {
         value.addProperty("overlay_override", material.overlayOverride());
      }
      if (material.outlineColor() != null) {
         value.addProperty("outline_color", color(material.outlineColor()));
      }
      if (material.renderPriority() != 0) {
         value.addProperty("render_priority", material.renderPriority());
      }
      if (material.effect().active()) {
         value.add("effect", effectJson(material.effect()));
      }
      materials.add(material.id(), value);
      return this;
   }

   public VisualProfileBuilder layer(VisualLayerProfile layer) {
      JsonObject value = new JsonObject();
      value.addProperty("id", layer.id());
      value.addProperty("kind", layer.kind().name().toLowerCase(java.util.Locale.ROOT));
      if (layer.texture() != null) {
         value.addProperty("texture", layer.texture().toString());
      }
      value.addProperty("material", layer.material());
      value.add("states", names(layer.states()));
      value.add("variants", variants(layer.variants()));
      value.add("parts", strings(layer.partFilter()));
      value.addProperty("color", color(layer.color()));
      value.addProperty("alpha", layer.alpha());
      value.addProperty("emissive", layer.emissive());
      value.addProperty("light_mode", layer.lightMode().name().toLowerCase(java.util.Locale.ROOT));
      value.addProperty("render_pass", layer.renderPass().name().toLowerCase(java.util.Locale.ROOT));
      if (layer.cull() != null) {
         value.addProperty("cull", layer.cull());
      }
      if (layer.depthWrite() != null) {
         value.addProperty("depth_write", layer.depthWrite());
      }
      if (layer.sortOrder() != 0) {
         value.addProperty("sort_order", layer.sortOrder());
      }
      if (layer.lightOverride() != null) {
         value.addProperty("light_override", layer.lightOverride());
      }
      if (layer.overlayOverride() != null) {
         value.addProperty("overlay_override", layer.overlayOverride());
      }
      if (layer.outlineColor() != null) {
         value.addProperty("outline_color", color(layer.outlineColor()));
      }
      if (layer.renderPriority() != 0) {
         value.addProperty("render_priority", layer.renderPriority());
      }
      if (layer.effect().active()) {
         value.add("effect", effectJson(layer.effect()));
      }
      layers().add(value);
      return this;
   }

   public VisualProfileBuilder include(Identifier profile) {
      if (profile != null) {
         includes().add(profile.toString());
      }
      return this;
   }

   public VisualProfileBuilder include(VisualProfileReference reference) {
      if (reference == null || reference.profileId() == null) {
         return this;
      }
      JsonObject value = new JsonObject();
      value.addProperty("profile", reference.profileId().toString());
      value.add("states", names(reference.states()));
      value.add("variants", variants(reference.variants()));
      includes().add(value);
      return this;
   }

   public VisualProfileBuilder anchor(String name, RenderCoreVector offset) {
      object("anchors").add(name, vector(offset));
      return this;
   }

   public VisualProfileBuilder blockPart(String alias, BlockPartSelectorProfile selector) {
      JsonObject blockParts = object("block_parts");
      JsonObject value = new JsonObject();
      if (!selector.indices().isEmpty()) {
         value.add("indices", ints(selector.indices()));
      }
      if (!selector.directions().isEmpty()) {
         JsonArray directions = new JsonArray();
         selector.directions().forEach(direction -> directions.add(direction.getName()));
         value.add("directions", directions);
      }
      if (selector.materialFlags() != 0) {
         value.addProperty("material_flags", selector.materialFlags());
      }
      if (selector.ambientOcclusion() != null) {
         value.addProperty("ambient_occlusion", selector.ambientOcclusion());
      }
      if (!selector.tintIndices().isEmpty()) {
         value.add("tint_indices", ints(selector.tintIndices()));
      }
      if (!selector.blockState().isEmpty()) {
         JsonObject blockState = new JsonObject();
         selector.blockState().forEach((property, values) -> blockState.add(property, strings(values)));
         value.add("block_state", blockState);
      }
      blockParts.add(alias, value);
      return this;
   }

   public VisualProfileBuilder blockPart(String alias, int... indices) {
      java.util.ArrayList<Integer> values = new java.util.ArrayList<>();
      for (int index : indices) {
         values.add(index);
      }
      return blockPart(alias, new BlockPartSelectorProfile(alias, values, java.util.Set.of(), 0, null, java.util.List.of()));
   }

   private JsonObject object(String key) {
      if (!json.has(key) || !json.get(key).isJsonObject()) {
         json.add(key, new JsonObject());
      }
      return json.getAsJsonObject(key);
   }

   private JsonArray layers() {
      if (!json.has("layers") || !json.get("layers").isJsonArray()) {
         json.add("layers", new JsonArray());
      }
      return json.getAsJsonArray("layers");
   }

   private JsonArray includes() {
      if (!json.has("includes") || !json.get("includes").isJsonArray()) {
         json.add("includes", new JsonArray());
      }
      return json.getAsJsonArray("includes");
   }

   private static JsonArray names(java.util.Set<VisualState> states) {
      JsonArray array = new JsonArray();
      states.forEach(state -> array.add(state.name()));
      return array;
   }

   private static JsonArray variants(java.util.Set<VisualVariant> variants) {
      JsonArray array = new JsonArray();
      variants.forEach(variant -> array.add(variant.id()));
      return array;
   }

   private static JsonArray strings(Collection<String> values) {
      JsonArray array = new JsonArray();
      values.forEach(array::add);
      return array;
   }

   private static JsonArray strings(Set<String> values) {
      JsonArray array = new JsonArray();
      values.stream().sorted().forEach(array::add);
      return array;
   }

   private static JsonArray ints(java.util.List<Integer> values) {
      JsonArray array = new JsonArray();
      values.forEach(array::add);
      return array;
   }

   private static JsonObject effectJson(VisualEffectProfile effect) {
      JsonObject value = new JsonObject();
      value.addProperty("preset", effect.kind().name().toLowerCase(java.util.Locale.ROOT));
      value.addProperty("glow_intensity", effect.glowIntensity());
      value.addProperty("bloom_intensity", effect.bloomIntensity());
      value.addProperty("pulse_speed", effect.pulseSpeed());
      value.addProperty("pulse_min_alpha", effect.pulseMinAlpha());
      value.addProperty("pulse_max_alpha", effect.pulseMaxAlpha());
      value.addProperty("flicker_intensity", effect.flickerIntensity());
      value.addProperty("scanline_strength", effect.scanlineStrength());
      value.addProperty("hue_shift_speed", effect.hueShiftSpeed());
      value.addProperty("depth_bias", effect.depthBias());
      value.addProperty("advanced_enabled", effect.advancedEnabled());
      value.addProperty("bloom_radius", effect.bloomRadius());
      value.addProperty("bloom_threshold", effect.bloomThreshold());
      value.addProperty("bloom_passes", effect.bloomPasses());
      value.addProperty("screen_blend", effect.screenBlend());
      value.addProperty("target_scope", effect.targetScope().id());
      value.addProperty("bloom_mask_mode", effect.bloomMaskMode().id());
      if (effect.bloomTint() != null) {
         value.addProperty("bloom_tint", color(effect.bloomTint()));
      }
      if (effect.bloomMaskAlpha() != null) {
         value.addProperty("bloom_mask_alpha", effect.bloomMaskAlpha());
      }
      value.addProperty("bloom_channel", effect.effectiveBloomChannel());
      if (effect.bloomDownscale() != null) {
         value.addProperty("bloom_downscale", effect.bloomDownscale());
      }
      value.addProperty("advanced_priority", effect.advancedPriority());
      return value;
   }

   private static void addOptional(JsonObject json, String key, Integer value) {
      if (value != null) {
         json.addProperty(key, value);
      }
   }
}
