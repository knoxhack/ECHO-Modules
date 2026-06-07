package com.knoxhack.echorendercore.profile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class RenderCoreProfileValidator {
   private RenderCoreProfileValidator() {
   }

   public static ProfileDiagnosticsReport diagnostics(
         Map<Identifier, VisualProfile> visuals,
         Map<Identifier, AnimationProfile> animations,
         Map<Identifier, ParticleProfile> particles,
         int discoveredJsonCount,
         int loadedJsonCount,
         int failedJsonCount) {
      return diagnostics(visuals, animations, particles, discoveredJsonCount, loadedJsonCount, failedJsonCount, ProfileValidationReport.EMPTY);
   }

   public static ProfileDiagnosticsReport diagnostics(
         Map<Identifier, VisualProfile> visuals,
         Map<Identifier, AnimationProfile> animations,
         Map<Identifier, ParticleProfile> particles,
         int discoveredJsonCount,
         int loadedJsonCount,
         int failedJsonCount,
         ProfileValidationReport additionalValidation) {
      ProfilePerformanceReport performanceReport = analyzePerformance(visuals, animations, particles);
      ProfileValidationReport validationReport = validate(visuals, animations, particles)
         .merge(additionalValidation)
         .merge(performanceReport.asValidationReport());
      ProfileCacheMetrics metrics = ProfileCacheMetrics.from(
         visuals,
         animations,
         particles,
         validationReport,
         performanceReport,
         discoveredJsonCount,
         loadedJsonCount,
         failedJsonCount
      );
      return new ProfileDiagnosticsReport(validationReport, performanceReport, metrics);
   }

   public static ProfileValidationReport validate(
         Map<Identifier, VisualProfile> visuals,
         Map<Identifier, AnimationProfile> animations,
         Map<Identifier, ParticleProfile> particles) {
      ArrayList<ProfileValidationIssue> issues = new ArrayList<>();
      for (VisualProfile visual : visuals.values()) {
         validateVisual(visual, animations, particles, issues);
      }
      return new ProfileValidationReport(issues);
   }

   public static ProfilePerformanceReport analyzePerformance(
         Map<Identifier, VisualProfile> visuals,
         Map<Identifier, AnimationProfile> animations,
         Map<Identifier, ParticleProfile> particles) {
      Map<String, ProfilePerformanceSummary> summaries = new LinkedHashMap<>();
      ArrayList<ProfilePerformanceIssue> issues = new ArrayList<>();
      for (VisualProfile visual : visuals.values()) {
         AnimationProfile animationProfile = visual.animationProfile() == null ? null : animations.get(visual.animationProfile());
         ParticleProfile particleProfile = visual.particleProfile() == null ? null : particles.get(visual.particleProfile());
         int animationClipCount = animationProfile == null ? 0 : animationProfile.animations().size();
         int trackCount = animationProfile == null ? 0 : animationProfile.animations().values().stream()
            .mapToInt(clip -> clip.tracks().size())
            .sum();
         int emitterCount = particleProfile == null ? 0 : particleProfile.emitters().size();
         int maxBurst = particleProfile == null ? 0 : particleProfile.emitters().values().stream()
            .mapToInt(ParticleEmitter::burstCount)
            .max()
            .orElse(0);
         float emitterRate = particleProfile == null ? 0.0F : (float)particleProfile.emitters().values().stream()
            .mapToDouble(emitter -> emitter.rate() * Math.max(1, emitter.burstCount()))
            .sum();
         int maskedLayerCount = (int)visual.layers().stream()
            .filter(layer -> !layer.partFilter().isEmpty())
            .count();
         int activeEffectCount = activeEffectCount(visual);
         int effectCost = effectCost(visual);
         int bloomCost = bloomCost(visual);
         int advancedPassCount = advancedPassCount(visual);
         int maskSubmissions = estimatedMaskSubmissions(visual);
         int bloomChannelCount = estimatedBloomChannelCount(visual);
         int bloomDownscale = estimatedBloomDownscale(visual);
         int prioritySkips = Math.max(0, maskSubmissions - 96);
         summaries.put(visual.id().toString(), new ProfilePerformanceSummary(
            visual.id(),
            visual.layers().size(),
            maskedLayerCount,
            animationClipCount,
            trackCount,
            emitterCount,
            maxBurst,
            emitterRate,
            activeEffectCount,
            effectCost,
            bloomCost,
            advancedPassCount,
            primaryEffectTargetScope(visual).id(),
            maskSubmissions,
            bloomChannelCount,
            bloomDownscale,
            prioritySkips,
            visual.budget().advancedFx().id(),
            false,
            true
         ));
         addPerformanceWarnings(issues, visual.id(), visual.layers().size(), emitterRate, trackCount, effectCost, bloomCost,
            advancedPassCount, maskSubmissions, bloomChannelCount, prioritySkips, visual.budget(),
            particleProfile == null ? 0 : particleProfile.emitters().size());
      }
      return new ProfilePerformanceReport(summaries, issues);
   }

   private static void validateVisual(VisualProfile visual, Map<Identifier, AnimationProfile> animations,
         Map<Identifier, ParticleProfile> particles, ArrayList<ProfileValidationIssue> issues) {
      validateV12Contract(visual, issues);
      if (visual.baseTexture() == null) {
         warn(issues, visual.id(), "missing_base_texture", "base_texture", "Visual profile has no base texture; callers must provide a fallback texture.",
            "Add base_texture or make sure every caller supplies a fallback texture.");
      }
      validateMaterialControls(visual, issues);
      validateEffectControls(visual, issues);
      if (visual.animationProfile() != null && !animations.containsKey(visual.animationProfile())) {
         warn(issues, visual.id(), "missing_profile_reference", "animation_profile", "Referenced animation profile " + visual.animationProfile() + " was not loaded.",
            "Create the animation profile JSON or update animation_profile.");
      }
      if (visual.particleProfile() != null && !particles.containsKey(visual.particleProfile())) {
         warn(issues, visual.id(), "missing_profile_reference", "particle_profile", "Referenced particle profile " + visual.particleProfile() + " was not loaded.",
            "Create the particle profile JSON or update particle_profile.");
      }
      AnimationProfile animationProfile = visual.animationProfile() == null ? null : animations.get(visual.animationProfile());
      if (animationProfile != null) {
         for (Map.Entry<com.knoxhack.echorendercore.api.VisualState, String> entry : visual.stateAnimations().entrySet()) {
            if (animationProfile.clip(entry.getValue()) == null) {
               warn(issues, visual.id(), "missing_animation_clip", "state_animations." + entry.getKey().name(), "Missing animation clip '" + entry.getValue() + "'.",
                  "Add the clip to the referenced animation profile or rename this state animation.");
            }
         }
      }
      ParticleProfile particleProfile = visual.particleProfile() == null ? null : particles.get(visual.particleProfile());
      if (particleProfile != null) {
         for (ParticleEmitter emitter : particleProfile.emitters().values()) {
            if (!emitter.anchor().isBlank() && !visual.anchors().containsKey(emitter.anchor())) {
               warn(issues, visual.id(), "missing_anchor", "particles." + emitter.id(), "Emitter anchor '" + emitter.anchor() + "' is not declared by the visual profile.",
                  "Declare the anchor in the visual profile anchors object.");
            }
            if (emitter.options().lifetime() > 0 && !softParticleOptions(emitter.options())) {
               warn(issues, visual.id(), "unsupported_particle_option", "particles." + emitter.id() + ".options.lifetime",
                  "Particle lifetime is parsed for validation but vanilla particle spawning does not apply it directly.",
                  "Use a particle type that encodes lifetime itself or register a custom option resolver.");
            }
            Set<String> supportedOptions = supportedParticleOptions(emitter.options());
            for (String option : emitter.options().custom().keySet()) {
               if (supportedOptions.contains(option)) {
                  continue;
               }
               warn(issues, visual.id(), "unsupported_particle_option", "particles." + emitter.id() + ".options." + option,
                  "Particle option '" + option + "' is parsed for forward compatibility but has no built-in resolver.",
                  "Register a client particle option resolver for options.type or remove the unsupported option.");
            }
         }
      }
      validateBlockPartSelectors(visual, -1).issues().forEach(issues::add);
   }

   private static void validateV12Contract(VisualProfile visual, ArrayList<ProfileValidationIssue> issues) {
      if (!visual.surface().type().supported() || visual.surface().type() == VisualSurfaceType.UNKNOWN) {
         warn(issues, visual.id(), "surface_type_missing", "surface.type",
            "V12 profiles should declare a concrete RenderCore surface type.",
            "Set surface.type to entity, block_entity, static_block, screen, hud_overlay, particle_only, weather, or mob_family.");
      }
      if (!visual.fallback().mode().supported()) {
         warn(issues, visual.id(), "fallback_policy_invalid", "fallback.mode",
            "V12 fallback mode is not supported.",
            "Use stable, vanilla_renderer, particles_only, minimal_screen, or disabled.");
      }
      if (!visual.budget().tier().supported() || !visual.budget().advancedFx().supported()) {
         warn(issues, visual.id(), "budget_exceeded", "budget",
            "V12 budget policy contains an unsupported tier or advanced_fx value, so RenderCore cannot evaluate the profile budget reliably.",
            "Use budget.tier low, normal, high, or showcase with budget.advanced_fx deny, auto, or allow.");
      }
      if (visual.surface().screenLike() && !visual.screenChrome().supportedStyle()) {
         warn(issues, visual.id(), "screen_chrome_policy_mismatch", "screen_chrome.style",
            "Screen chrome style is not supported by shared RenderCore chrome.",
            "Use minimal, cyberglass, terminal, hologram, or neon.");
      }
      if (visual.surface().screenLike() && visual.screenChrome().scanlines()) {
         warn(issues, visual.id(), "screen_chrome_policy_mismatch", "screen_chrome.scanlines",
            "V21 clean cyberglass policy keeps scanlines opt-in and rejects unintended screen banding.",
            "Set screen_chrome.scanlines to false unless this is a deliberate legacy display.");
      }
      if (visual.qa().required() && visual.qa().evidence().isEmpty()) {
         warn(issues, visual.id(), "qa_evidence_missing", "qa.evidence",
            "V12 QA is required but no evidence kinds are declared.",
            "Add screenshot, advanced_fx, screen_chrome, or world_surface evidence kinds.");
      }
      if (!visual.qa().supportedEvidenceOnly()) {
         warn(issues, visual.id(), "qa_evidence_missing", "qa.evidence",
            "V12 QA evidence contains unsupported values.",
            "Use screenshot, advanced_fx, screen_chrome, or world_surface.");
      }
      if (visual.id() != null && visual.id().getPath().toLowerCase(java.util.Locale.ROOT).contains("terminal")
         && visual.surface().screenLike() && !visual.qa().reducedMotion()) {
         warn(issues, visual.id(), "qa_reduced_motion_missing", "qa.reduced_motion",
            "Terminal screen profiles require reduced-motion QA coverage.",
            "Set qa.reduced_motion to true and capture the reduced-motion evidence path.");
      }
   }

   private static void validateEffectControls(VisualProfile visual, ArrayList<ProfileValidationIssue> issues) {
      validateEffect(issues, visual.id(), "effects", visual.effect());
      for (Map.Entry<String, VisualMaterial> entry : visual.materials().entrySet()) {
         validateEffect(issues, visual.id(), "materials." + entry.getKey() + ".effects", entry.getValue().effect());
      }
      for (VisualLayerProfile layer : visual.layers()) {
         validateEffect(issues, visual.id(), "layers." + layer.id() + ".effects", layer.effect());
      }
   }

   private static void validateEffect(ArrayList<ProfileValidationIssue> issues, Identifier id, String path, VisualEffectProfile effect) {
      VisualEffectProfile resolved = effect == null ? VisualEffectProfile.NONE : effect;
      if (!resolved.kind().supported()) {
         warn(issues, id, "unsupported_effect_option", path + ".preset",
         "Effect preset is not supported by RenderCore V12.",
            "Use none, neon, hologram, energy_field, terminal_hud, or atmosphere.");
      }
      if (!resolved.targetScope().supported()) {
         warn(issues, id, "unsupported_effect_option", path + ".target_scope",
            "Effect target_scope is not supported by RenderCore V12.",
            "Use profile, entity, block, or global.");
      }
      if (!resolved.bloomMaskMode().supported()) {
         warn(issues, id, "unsupported_effect_option", path + ".bloom_mask_mode",
            "Effect bloom_mask_mode is not supported by RenderCore V12.",
            "Use auto, emissive, layer_alpha, solid, or none.");
      }
      if (resolved.glowIntensity() > 8.0F || resolved.bloomIntensity() > 8.0F) {
         warn(issues, id, "invalid_effect_option", path,
            "Effect glow and bloom intensity values above 8 are outside RenderCore's supported range.",
            "Use lower intensity values and add extra layers only where needed.");
      }
      if (resolved.bloomRadius() > 64.0F || resolved.bloomPasses() > 8) {
         warn(issues, id, "invalid_effect_option", path,
            "Effect bloom_radius above 64 or bloom_passes above 8 can overwhelm the advanced FX chain.",
            "Use a smaller radius/pass count and layer stable emissive materials for extra punch.");
      }
      if (resolved.bloomMaskAlpha() != null
         && (!Float.isFinite(resolved.bloomMaskAlpha()) || resolved.bloomMaskAlpha() < 0.0F || resolved.bloomMaskAlpha() > 1.0F)) {
         warn(issues, id, "invalid_effect_option", path + ".bloom_mask_alpha",
            "Effect bloom_mask_alpha must be between 0 and 1.",
            "Clamp the bloom mask alpha or omit it to use computed layer alpha.");
      }
      if (resolved.bloomChannel() != null && resolved.bloomChannel().isBlank()) {
         warn(issues, id, "invalid_effect_option", path + ".bloom_channel",
            "Effect bloom_channel must be a nonblank string.",
            "Use default or a short channel id such as neon, terminal, or atmosphere.");
      }
      if (resolved.bloomDownscale() != null && resolved.bloomDownscale() != 1
         && resolved.bloomDownscale() != 2 && resolved.bloomDownscale() != 4) {
         warn(issues, id, "invalid_effect_option", path + ".bloom_downscale",
            "Effect bloom_downscale must be 1, 2, or 4.",
            "Use 2 for the default balanced isolated bloom mask.");
      }
      if (resolved.advancedPriority() < -100 || resolved.advancedPriority() > 100) {
         warn(issues, id, "invalid_effect_option", path + ".advanced_priority",
            "Effect advanced_priority must be between -100 and 100.",
            "Keep priority near 0 and reserve high priority for hero visuals.");
      }
      if (resolved.bloomCapable()) {
         warn(issues, id, "effect_pipeline_unavailable", path + ".bloom_intensity",
            "Bloom-capable effects require the optional client advanced FX pipeline and fall back to stable emissive rendering otherwise.",
            "Enable advanced FX on clients that support it, and keep stable emissive values for fallback visuals.");
      }
      if (resolved.advancedEnabled() && resolved.bloomCapable()) {
         warn(issues, id, "advanced_effect_config_disabled", path + ".advanced_enabled",
            "Advanced FX is disabled by default and only runs when the client config or debug override enables it.",
            "Keep stable emissive fallback values, or enable advanced FX per client/session.");
      }
      if (resolved.advancedEnabled() && resolved.bloomCapable() && resolved.bloomMaskMode() != VisualEffectBloomMaskMode.NONE) {
         warn(issues, id, "advanced_effect_mask_unavailable", path + ".bloom_mask_mode",
            "V12 isolated bloom masks require client-only mask render targets and fall back when unavailable.",
            "Use bloom_mask_mode none for stable-only visuals, or keep fallback_to_stable enabled.");
      }
      if (resolved.bloomCapable() && !resolved.advancedEnabled()) {
         warn(issues, id, "advanced_effect_disabled", path + ".advanced_enabled",
            "Bloom-capable effect fields are present but advanced_enabled is false.",
            "Set advanced_enabled to true where postprocessing is intended, or keep this as fallback-only metadata.");
      }
      if (resolved.pulseSpeed() > 20.0F || Math.abs(resolved.hueShiftSpeed()) > 10.0F) {
         warn(issues, id, "invalid_effect_option", path,
            "Effect animation speeds are high enough to flicker or become visually noisy.",
            "Keep pulse_speed at or below 20 and hue_shift_speed between -10 and 10.");
      }
   }

   private static void validateMaterialControls(VisualProfile visual, ArrayList<ProfileValidationIssue> issues) {
      for (Map.Entry<String, VisualMaterial> entry : visual.materials().entrySet()) {
         VisualMaterial material = entry.getValue();
         if (!material.lightMode().supported()) {
            warn(issues, visual.id(), "unsupported_material_option", "materials." + entry.getKey() + ".light_mode",
               "Material '" + entry.getKey() + "' requests an unsupported light_mode.",
               "Use profile, packed, fullbright, or emissive.");
         }
         if (!material.renderPass().supported()) {
            warn(issues, visual.id(), "unsupported_material_option", "materials." + entry.getKey() + ".render_pass",
               "Material '" + entry.getKey() + "' requests an unsupported render_pass.",
               "Use auto, base, cutout, translucent, or emissive.");
         }
         validateLightOverride(issues, visual.id(), "materials." + entry.getKey() + ".light_override", material.lightOverride());
         validateOverlayOverride(issues, visual.id(), "materials." + entry.getKey() + ".overlay_override", material.overlayOverride());
         validateRenderPriority(issues, visual.id(), "materials." + entry.getKey() + ".render_priority", material.renderPriority());
      }
      for (VisualLayerProfile layer : visual.layers()) {
         if (!layer.lightMode().supported()) {
            warn(issues, visual.id(), "unsupported_material_option", "layers." + layer.id() + ".light_mode",
               "Layer '" + layer.id() + "' requests an unsupported light_mode.",
               "Use profile, packed, fullbright, or emissive.");
         }
         if (!layer.renderPass().supported()) {
            warn(issues, visual.id(), "unsupported_material_option", "layers." + layer.id() + ".render_pass",
               "Layer '" + layer.id() + "' requests an unsupported render_pass.",
               "Use auto, base, cutout, translucent, or emissive.");
         }
         validateLightOverride(issues, visual.id(), "layers." + layer.id() + ".light_override", layer.lightOverride());
         validateOverlayOverride(issues, visual.id(), "layers." + layer.id() + ".overlay_override", layer.overlayOverride());
         validateRenderPriority(issues, visual.id(), "layers." + layer.id() + ".render_priority", layer.renderPriority());
      }
   }

   private static void validateLightOverride(ArrayList<ProfileValidationIssue> issues, Identifier id, String path, Integer value) {
      if (value != null && (value < 0 || value > 0xF000F0)) {
         warn(issues, id, "invalid_material_option", path,
            "light_override must be between 0 and 15728880.",
            "Use a packed light value exposed by the renderer, or omit light_override.");
      }
   }

   private static void validateOverlayOverride(ArrayList<ProfileValidationIssue> issues, Identifier id, String path, Integer value) {
      if (value != null && value < 0) {
         warn(issues, id, "invalid_material_option", path,
            "overlay_override must be a non-negative packed overlay value.",
            "Use OverlayTexture.NO_OVERLAY-equivalent 0 or omit overlay_override.");
      }
   }

   private static void validateRenderPriority(ArrayList<ProfileValidationIssue> issues, Identifier id, String path, int value) {
      if (value < -1000 || value > 1000) {
         warn(issues, id, "invalid_material_option", path,
            "render_priority is outside RenderCore's supported -1000..1000 range.",
            "Keep render_priority near zero and use sort_order for coarse layer ordering.");
      }
   }

   public static ProfileValidationReport validateLayerParts(VisualProfile visual, Set<String> knownParts) {
      ArrayList<ProfileValidationIssue> issues = new ArrayList<>();
      if (visual == null || knownParts == null) {
         return new ProfileValidationReport(issues);
      }
      for (VisualLayerProfile layer : visual.layers()) {
         for (String part : layer.partFilter()) {
            if (!knownParts.contains(part)) {
               warn(issues, visual.id(), "masked_part_missing", "layers." + layer.id() + ".parts",
                  "Layer '" + layer.id() + "' references missing named part '" + part + "'.",
                  "Add the model part alias or remove the part from the layer mask.");
            }
         }
      }
      return new ProfileValidationReport(issues);
   }

   public static ProfileValidationReport validateBlockPartSelectors(VisualProfile visual, int collectedPartCount) {
      return validateBlockPartSelectors(visual, collectedPartCount, null, null);
   }

   public static ProfileValidationReport validateBlockPartSelectors(
         VisualProfile visual,
         int collectedPartCount,
         BlockState blockState,
         Set<Integer> availableTintIndices) {
      ArrayList<ProfileValidationIssue> issues = new ArrayList<>();
      if (visual == null) {
         return new ProfileValidationReport(issues);
      }
      for (Map.Entry<String, BlockPartSelectorProfile> entry : visual.blockParts().entrySet()) {
         BlockPartSelectorProfile selector = entry.getValue();
         if (selector.isEmptySelector()) {
            warn(issues, visual.id(), "block_part_selector_empty", "block_parts." + entry.getKey(),
               "Block part alias '" + entry.getKey() + "' has no selector rules and will not match baked model parts.",
               "Add indices, directions, material_flags, ambient_occlusion, tint_indices, or block_state.");
         }
         if (collectedPartCount >= 0) {
            for (int index : selector.indices()) {
               if (index < 0 || index >= collectedPartCount) {
                  warn(issues, visual.id(), "block_part_index_out_of_range", "block_parts." + entry.getKey() + ".indices",
                     "Block part alias '" + entry.getKey() + "' references collected part index " + index + " but only " + collectedPartCount + " part(s) exist.",
                  "Update the selector index or switch to direction/material selectors.");
               }
            }
         }
         if (availableTintIndices != null) {
            for (int tintIndex : selector.tintIndices()) {
               if (!availableTintIndices.contains(tintIndex)) {
                  warn(issues, visual.id(), "block_part_tint_index_missing", "block_parts." + entry.getKey() + ".tint_indices",
                     "Block part alias '" + entry.getKey() + "' requires tint index " + tintIndex + " but collected parts expose "
                        + availableTintIndices + ".",
                     "Update tint_indices or remove the tint rule from this selector.");
               }
            }
         }
         if (blockState != null && !selector.blockState().isEmpty()) {
            validateBlockStateRules(visual, entry.getKey(), selector, blockState, issues);
         }
      }
      return new ProfileValidationReport(issues);
   }

   private static void validateBlockStateRules(VisualProfile visual, String alias, BlockPartSelectorProfile selector,
         BlockState blockState, ArrayList<ProfileValidationIssue> issues) {
      for (Map.Entry<String, Set<String>> rule : selector.blockState().entrySet()) {
         Property<?> property = findProperty(blockState, rule.getKey());
         if (property == null) {
            warn(issues, visual.id(), "block_state_property_missing", "block_parts." + alias + ".block_state." + rule.getKey(),
               "Block part alias '" + alias + "' requires block state property '" + rule.getKey() + "', but " + blockState + " does not expose it.",
               "Use a property that exists on the target block state or remove this block_state selector.");
            continue;
         }
         String value = serializedPropertyValue(blockState, property);
         if (!rule.getValue().isEmpty() && !rule.getValue().contains(value)) {
            warn(issues, visual.id(), "block_state_property_value_missing", "block_parts." + alias + ".block_state." + rule.getKey(),
               "Block part alias '" + alias + "' requires block state property '" + rule.getKey() + "' value " + rule.getValue()
                  + ", but the resolved value is '" + value + "'.",
               "Add the current serialized value to the selector list or use a less specific alias.");
         }
      }
   }

   private static Property<?> findProperty(BlockState blockState, String name) {
      for (Property<?> property : blockState.getProperties()) {
         if (property.getName().equals(name)) {
            return property;
         }
      }
      return null;
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   private static String serializedPropertyValue(BlockState blockState, Property property) {
      return property.getName((Comparable)blockState.getValue(property));
   }

   private static Set<String> supportedParticleOptions(ParticleOptionsSpec options) {
      String type = options.type().toLowerCase(java.util.Locale.ROOT);
      return switch (type) {
         case "dust", "minecraft:dust" -> Set.of();
         case "dust_transition", "minecraft:dust_color_transition" -> Set.of("from_color", "fromColor", "to_color", "toColor");
         case "color", "entity_effect", "minecraft:entity_effect" -> Set.of();
         case "item", "minecraft:item" -> Set.of("item");
         case "block", "minecraft:block" -> Set.of("block", "block_state", "blockState");
         case "trail", "minecraft:trail" -> Set.of("target", "duration");
         case "soft_mote", "rendercore:soft_mote", "echorendercore:soft_mote", "soft_wisp", "rendercore:soft_wisp", "echorendercore:soft_wisp" ->
            Set.of("alpha", "drag", "fade", "style", "fullbright", "gravity", "spin", "stretch", "flicker");
         default -> Set.of();
      };
   }

   private static boolean softParticleOptions(ParticleOptionsSpec options) {
      String type = options.type().toLowerCase(java.util.Locale.ROOT);
      return type.equals("soft_mote") || type.equals("rendercore:soft_mote") || type.equals("echorendercore:soft_mote")
         || type.equals("soft_wisp") || type.equals("rendercore:soft_wisp") || type.equals("echorendercore:soft_wisp");
   }

   private static int activeEffectCount(VisualProfile visual) {
      int count = visual.effect().active() ? 1 : 0;
      count += (int)visual.materials().values().stream().filter(material -> material.effect().active()).count();
      count += (int)visual.layers().stream().filter(layer -> visual.effectFor(layer).active()).count();
      return count;
   }

   private static int effectCost(VisualProfile visual) {
      int cost = visual.effect().cost();
      cost += visual.materials().values().stream().mapToInt(material -> material.effect().cost()).sum();
      cost += visual.layers().stream().mapToInt(layer -> visual.effectFor(layer).cost()).sum();
      return cost;
   }

   private static int bloomCost(VisualProfile visual) {
      int cost = visual.effect().bloomCost();
      cost += visual.materials().values().stream().mapToInt(material -> material.effect().bloomCost()).sum();
      cost += visual.layers().stream().mapToInt(layer -> visual.effectFor(layer).bloomCost()).sum();
      return cost;
   }

   private static int advancedPassCount(VisualProfile visual) {
      int count = visual.effect().advancedPassCount();
      count += visual.materials().values().stream().mapToInt(material -> material.effect().advancedPassCount()).sum();
      count += visual.layers().stream().mapToInt(layer -> visual.effectFor(layer).advancedPassCount()).sum();
      return count;
   }

   private static VisualEffectTargetScope primaryEffectTargetScope(VisualProfile visual) {
      if (visual.effect().active()) {
         return visual.effect().targetScope();
      }
      for (VisualLayerProfile layer : visual.layers()) {
         VisualEffectProfile effect = visual.effectFor(layer);
         if (effect.active()) {
            return effect.targetScope();
         }
      }
      return VisualEffectTargetScope.PROFILE;
   }

   private static int estimatedMaskSubmissions(VisualProfile visual) {
      int count = visual.effect().advancedEnabled() && visual.effect().bloomCapable()
         && visual.effect().bloomMaskMode() != VisualEffectBloomMaskMode.NONE ? 1 : 0;
      for (VisualLayerProfile layer : visual.layers()) {
         VisualEffectProfile effect = visual.effectFor(layer);
         if (effect.advancedEnabled() && effect.bloomCapable() && effect.bloomMaskMode() != VisualEffectBloomMaskMode.NONE) {
            count++;
         }
      }
      return count;
   }

   private static int estimatedBloomChannelCount(VisualProfile visual) {
      HashSet<String> channels = new HashSet<>();
      if (visual.effect().advancedEnabled() && visual.effect().bloomCapable()) {
         channels.add(visual.effect().effectiveBloomChannel());
      }
      for (VisualLayerProfile layer : visual.layers()) {
         VisualEffectProfile effect = visual.effectFor(layer);
         if (effect.advancedEnabled() && effect.bloomCapable() && effect.bloomMaskMode() != VisualEffectBloomMaskMode.NONE) {
            channels.add(effect.effectiveBloomChannel());
         }
      }
      return channels.size();
   }

   private static int estimatedBloomDownscale(VisualProfile visual) {
      if (visual.effect().bloomDownscale() != null) {
         return visual.effect().effectiveBloomDownscale(2);
      }
      for (VisualLayerProfile layer : visual.layers()) {
         VisualEffectProfile effect = visual.effectFor(layer);
         if (effect.bloomDownscale() != null) {
            return effect.effectiveBloomDownscale(2);
         }
      }
      return 2;
   }

   private static void addPerformanceWarnings(ArrayList<ProfilePerformanceIssue> issues, Identifier id, int layerCount, float emitterRate,
         int trackCount, int effectCost, int bloomCost, int advancedPassCount, int maskSubmissions, int bloomChannelCount, int prioritySkips,
         VisualBudgetProfile budget, int emitterCount) {
      VisualBudgetProfile resolvedBudget = budget == null ? VisualBudgetProfile.DEFAULT : budget;
      if (layerCount > 12) {
         issues.add(new ProfilePerformanceIssue(id, "profile_perf_high_layer_count", ProfileValidationSeverity.WARNING,
            "Visual profile has " + layerCount + " layer(s), which can become expensive when many instances render.", layerCount, 12));
      }
      if (emitterRate > 8.0F) {
         issues.add(new ProfilePerformanceIssue(id, "profile_perf_high_emitter_rate", ProfileValidationSeverity.WARNING,
            "Particle profile estimates " + emitterRate + " particle(s) per tick before runtime gates.", Math.round(emitterRate), 8));
      }
      if (trackCount > 96) {
         issues.add(new ProfilePerformanceIssue(id, "profile_perf_high_animation_track_count", ProfileValidationSeverity.WARNING,
            "Animation profile contains " + trackCount + " track(s).", trackCount, 96));
      }
      if (effectCost > 18) {
         issues.add(new ProfilePerformanceIssue(id, "profile_perf_high_effect_cost", ProfileValidationSeverity.WARNING,
            "Visual profile has estimated effect cost " + effectCost + " from active V12 effects.", effectCost, 18));
      }
      if (bloomCost > 24) {
         issues.add(new ProfilePerformanceIssue(id, "profile_perf_high_bloom_cost", ProfileValidationSeverity.WARNING,
            "Visual profile has estimated bloom cost " + bloomCost + " from advanced FX settings.", bloomCost, 24));
      }
      if (maskSubmissions > 96 || advancedPassCount > 4) {
         issues.add(new ProfilePerformanceIssue(id, "advanced_effect_budget_exceeded", ProfileValidationSeverity.WARNING,
            "Visual profile estimates " + maskSubmissions + " isolated bloom mask submission(s) and " + advancedPassCount + " pass(es).",
            Math.max(maskSubmissions, advancedPassCount), maskSubmissions > 96 ? 96 : 4));
      }
      if (bloomChannelCount > 4) {
         issues.add(new ProfilePerformanceIssue(id, "advanced_effect_channel_limit", ProfileValidationSeverity.WARNING,
            "Visual profile estimates " + bloomChannelCount + " bloom channel(s), above the default V12 channel budget.",
            bloomChannelCount, 4));
      }
      if (prioritySkips > 0) {
         issues.add(new ProfilePerformanceIssue(id, "advanced_effect_budget_exceeded", ProfileValidationSeverity.WARNING,
            "Visual profile may skip " + prioritySkips + " low-priority isolated bloom submission(s) at the default budget.",
            prioritySkips, 0));
      }
      addBudgetWarning(issues, id, "layers", layerCount, resolvedBudget.effectiveMaxLayers());
      addBudgetWarning(issues, id, "emitters", emitterCount, resolvedBudget.effectiveMaxEmitters());
      addBudgetWarning(issues, id, "effect cost", effectCost, resolvedBudget.effectiveMaxEffectCost());
      addBudgetWarning(issues, id, "bloom cost", bloomCost, resolvedBudget.effectiveMaxBloomCost());
      addBudgetWarning(issues, id, "mask submissions", maskSubmissions, resolvedBudget.effectiveMaxMaskSubmissions());
      if (resolvedBudget.advancedFxDenied() && (advancedPassCount > 0 || maskSubmissions > 0 || bloomCost > 0)) {
         issues.add(new ProfilePerformanceIssue(id, "budget_exceeded", ProfileValidationSeverity.WARNING,
            "Visual profile opts out of advanced FX but declares bloom-capable advanced settings.", advancedPassCount + maskSubmissions + bloomCost, 0));
      }
   }

   private static void addBudgetWarning(ArrayList<ProfilePerformanceIssue> issues, Identifier id, String label, int value, int threshold) {
      if (value > threshold) {
         issues.add(new ProfilePerformanceIssue(id, "budget_exceeded", ProfileValidationSeverity.WARNING,
            "Visual profile exceeds its V12 " + label + " budget (" + value + " > " + threshold + ").", value, threshold));
      }
   }

   private static void warn(ArrayList<ProfileValidationIssue> issues, Identifier id, String code, String path, String message, String suggestion) {
      issues.add(new ProfileValidationIssue(ProfileValidationSeverity.WARNING, id, code, path, message, suggestion));
   }
}
