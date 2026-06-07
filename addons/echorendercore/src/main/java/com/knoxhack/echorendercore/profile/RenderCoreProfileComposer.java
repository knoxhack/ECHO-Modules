package com.knoxhack.echorendercore.profile;

import com.knoxhack.echorendercore.api.VisualState;
import com.knoxhack.echorendercore.api.VisualVariant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class RenderCoreProfileComposer {
   private RenderCoreProfileComposer() {
   }

   public static CompositionResult composeAll(Map<Identifier, VisualProfile> profiles) {
      Map<Identifier, VisualProfile> source = profiles == null ? Map.of() : profiles;
      LinkedHashMap<Identifier, VisualProfile> composed = new LinkedHashMap<>();
      ArrayList<ProfileValidationIssue> issues = new ArrayList<>();
      for (Identifier id : source.keySet().stream().sorted(java.util.Comparator.comparing(Identifier::toString)).toList()) {
         composed.put(id, compose(id, source, new ArrayDeque<>(), issues).composed());
      }
      return new CompositionResult(composed, new ProfileValidationReport(issues));
   }

   public static ComposedVisualProfile compose(VisualProfile profile, Map<Identifier, VisualProfile> profiles) {
      if (profile == null) {
         return new ComposedVisualProfile(null, null, ProfileValidationReport.EMPTY);
      }
      ArrayList<ProfileValidationIssue> issues = new ArrayList<>();
      ComposedVisualProfile composed = compose(profile.id(), profiles == null ? Map.of() : profiles, new ArrayDeque<>(), issues);
      return new ComposedVisualProfile(profile, composed.composed(), new ProfileValidationReport(issues));
   }

   private static ComposedVisualProfile compose(
         Identifier id,
         Map<Identifier, VisualProfile> profiles,
         ArrayDeque<Identifier> stack,
         ArrayList<ProfileValidationIssue> issues) {
      VisualProfile root = profiles.get(id);
      if (root == null) {
         return new ComposedVisualProfile(null, null, ProfileValidationReport.EMPTY);
      }
      if (stack.contains(id)) {
         warn(issues, id, "profile_include_cycle", "includes",
            "Profile include cycle detected: " + cycleText(stack, id) + ".",
            "Remove one include from the cycle.");
         return new ComposedVisualProfile(root, root, new ProfileValidationReport(issues));
      }
      stack.addLast(id);
      Builder builder = new Builder(root);
      for (VisualProfileReference reference : root.includes()) {
         VisualProfile included = profiles.get(reference.profileId());
         if (included == null) {
            warn(issues, root.id(), "missing_profile_include", "includes",
               "Included visual profile " + reference.profileId() + " was not loaded.",
               "Create the included profile or remove it from includes.");
            continue;
         }
         if (stack.contains(reference.profileId())) {
            warn(issues, root.id(), "profile_include_cycle", "includes",
               "Profile include cycle detected: " + cycleText(stack, reference.profileId()) + ".",
               "Remove one include from the cycle.");
            continue;
         }
         VisualProfile includedComposed = compose(reference.profileId(), profiles, stack, issues).composed();
         if (includedComposed != null) {
            builder.addIncluded(includedComposed, reference, issues);
         }
      }
      stack.removeLast();
      builder.addRoot(root, issues);
      VisualProfile composed = builder.build();
      return new ComposedVisualProfile(root, composed, new ProfileValidationReport(issues));
   }

   private static String cycleText(ArrayDeque<Identifier> stack, Identifier repeated) {
      ArrayList<String> values = new ArrayList<>();
      boolean collecting = false;
      for (Identifier id : stack) {
         if (id.equals(repeated)) {
            collecting = true;
         }
         if (collecting) {
            values.add(id.toString());
         }
      }
      values.add(repeated.toString());
      return String.join(" -> ", values);
   }

   private static void warn(ArrayList<ProfileValidationIssue> issues, Identifier id, String code, String path, String message, String suggestion) {
      issues.add(new ProfileValidationIssue(ProfileValidationSeverity.WARNING, id, code, path, message, suggestion));
   }

   public record CompositionResult(Map<Identifier, VisualProfile> profiles, ProfileValidationReport report) {
      public CompositionResult {
         profiles = profiles == null ? Map.of() : Map.copyOf(profiles);
         report = report == null ? ProfileValidationReport.EMPTY : report;
      }
   }

   private static final class Builder {
      private final VisualProfile root;
      private Identifier baseTexture;
      private Identifier glowTexture;
      private Identifier damagedOverlayTexture;
      private Identifier corruptedOverlayTexture;
      private Identifier activeOverlayTexture;
      private Identifier animationProfile;
      private Identifier particleProfile;
      private final Map<VisualState, String> stateAnimations = new EnumMap<>(VisualState.class);
      private final Map<VisualState, List<Identifier>> stateOverlays = new EnumMap<>(VisualState.class);
      private final Map<VisualState, Identifier> stateTextureVariants = new EnumMap<>(VisualState.class);
      private final Map<VisualVariant, Identifier> variantTextures = new LinkedHashMap<>();
      private final Map<String, RenderCoreAnchor> anchors = new LinkedHashMap<>();
      private final Map<String, VisualMaterial> materials = new LinkedHashMap<>();
      private final Map<String, BlockPartSelectorProfile> blockParts = new LinkedHashMap<>();
      private final List<VisualLayerProfile> layers = new ArrayList<>();
      private final Set<String> includeLayerIds = new LinkedHashSet<>();
      private VisualEffectProfile effect = VisualEffectProfile.NONE;
      private int schemaVersion;

      private Builder(VisualProfile root) {
         this.root = root;
         this.schemaVersion = Math.max(root.schemaVersion(), 7);
      }

      private void addIncluded(VisualProfile profile, VisualProfileReference reference, ArrayList<ProfileValidationIssue> issues) {
         schemaVersion = Math.max(schemaVersion, profile.schemaVersion());
         if (baseTexture == null) {
            baseTexture = profile.baseTexture();
         }
         if (glowTexture == null) {
            glowTexture = profile.glowTexture();
         }
         if (damagedOverlayTexture == null) {
            damagedOverlayTexture = profile.damagedOverlayTexture();
         }
         if (corruptedOverlayTexture == null) {
            corruptedOverlayTexture = profile.corruptedOverlayTexture();
         }
         if (activeOverlayTexture == null) {
            activeOverlayTexture = profile.activeOverlayTexture();
         }
         if (animationProfile == null) {
            animationProfile = profile.animationProfile();
         }
         if (particleProfile == null) {
            particleProfile = profile.particleProfile();
         }
         if (!effect.active()) {
            effect = profile.effect();
         }
         stateAnimations.putAll(profile.stateAnimations());
         profile.stateOverlays().forEach((state, overlays) -> stateOverlays.put(state, List.copyOf(overlays)));
         stateTextureVariants.putAll(profile.stateTextureVariants());
         variantTextures.putAll(profile.variantTextures());
         putAllMissing(anchors, profile.anchors());
         putAllMissing(blockParts, profile.blockParts());
         putMaterials(profile, false, issues);
         for (VisualLayerProfile layer : profile.layers()) {
            VisualLayerProfile filtered = filterLayer(layer, reference);
            if (filtered == null) {
               continue;
            }
            if (!includeLayerIds.add(filtered.id())) {
               warn(issues, root.id(), "duplicate_composed_layer", "includes." + reference.profileId() + ".layers." + filtered.id(),
                  "Included layer id '" + filtered.id() + "' is duplicated in the composed profile.",
                  "Rename one layer id or remove the duplicate include.");
            }
            layers.add(filtered);
         }
      }

      private void addRoot(VisualProfile profile, ArrayList<ProfileValidationIssue> issues) {
         schemaVersion = Math.max(schemaVersion, profile.schemaVersion());
         baseTexture = profile.baseTexture() == null ? baseTexture : profile.baseTexture();
         glowTexture = profile.glowTexture() == null ? glowTexture : profile.glowTexture();
         damagedOverlayTexture = profile.damagedOverlayTexture() == null ? damagedOverlayTexture : profile.damagedOverlayTexture();
         corruptedOverlayTexture = profile.corruptedOverlayTexture() == null ? corruptedOverlayTexture : profile.corruptedOverlayTexture();
         activeOverlayTexture = profile.activeOverlayTexture() == null ? activeOverlayTexture : profile.activeOverlayTexture();
         animationProfile = profile.animationProfile() == null ? animationProfile : profile.animationProfile();
         particleProfile = profile.particleProfile() == null ? particleProfile : profile.particleProfile();
         effect = profile.effect().active() ? profile.effect() : effect;
         stateAnimations.putAll(profile.stateAnimations());
         profile.stateOverlays().forEach((state, overlays) -> stateOverlays.put(state, List.copyOf(overlays)));
         stateTextureVariants.putAll(profile.stateTextureVariants());
         variantTextures.putAll(profile.variantTextures());
         anchors.putAll(profile.anchors());
         blockParts.putAll(profile.blockParts());
         putMaterials(profile, true, issues);
         for (VisualLayerProfile layer : profile.layers()) {
            if (includeLayerIds.contains(layer.id())) {
               warn(issues, root.id(), "duplicate_composed_layer", "layers." + layer.id(),
                  "Root layer id '" + layer.id() + "' duplicates an included layer id.",
                  "Rename one layer id if preview tooling needs unique layer names.");
            }
            layers.add(layer);
         }
      }

      private VisualProfile build() {
         return new VisualProfile(
            root.id(),
            baseTexture,
            glowTexture,
            damagedOverlayTexture,
            corruptedOverlayTexture,
            activeOverlayTexture,
            animationProfile,
            particleProfile,
            root.defaultState(),
            stateAnimations,
            stateOverlays,
            stateTextureVariants,
            variantTextures,
            anchors,
            schemaVersion,
            root.transitionSeconds(),
            layers,
            materials,
            blockParts,
            root.includes(),
            effect
         );
      }

      private void putMaterials(VisualProfile profile, boolean rootProfile, ArrayList<ProfileValidationIssue> issues) {
         for (Map.Entry<String, VisualMaterial> entry : profile.materials().entrySet()) {
            if (materials.containsKey(entry.getKey())) {
               warn(issues, root.id(), "duplicate_composed_material",
                  (rootProfile ? "materials." : "includes." + profile.id() + ".materials.") + entry.getKey(),
                  "Material id '" + entry.getKey() + "' is duplicated in the composed profile.",
                  "Rename one material id if the duplicate was unintentional.");
            }
            if (rootProfile || !materials.containsKey(entry.getKey())) {
               materials.put(entry.getKey(), entry.getValue());
            }
         }
      }

      private static <T> void putAllMissing(Map<String, T> target, Map<String, T> source) {
         source.forEach(target::putIfAbsent);
      }

      private static VisualLayerProfile filterLayer(VisualLayerProfile layer, VisualProfileReference reference) {
         Set<VisualState> states = intersectStates(layer.states(), reference.states());
         Set<VisualVariant> variants = intersectVariants(layer.variants(), reference.variants());
         if ((!reference.states().isEmpty() && states.isEmpty()) || (!reference.variants().isEmpty() && variants.isEmpty())) {
            return null;
         }
         if (states == layer.states() && variants == layer.variants()) {
            return layer;
         }
         return new VisualLayerProfile(
            layer.id(),
            layer.kind(),
            layer.texture(),
            layer.material(),
            states,
            variants,
            layer.partFilter(),
            layer.color(),
            layer.alpha(),
            layer.emissive(),
            layer.lightMode(),
            layer.renderPass(),
            layer.cull(),
            layer.depthWrite(),
            layer.sortOrder(),
            layer.lightOverride(),
            layer.overlayOverride(),
            layer.outlineColor(),
            layer.renderPriority(),
            layer.effect()
         );
      }

      private static Set<VisualState> intersectStates(Set<VisualState> layer, Set<VisualState> filter) {
         if (filter.isEmpty()) {
            return layer;
         }
         if (layer.isEmpty()) {
            return filter;
         }
         LinkedHashSet<VisualState> values = new LinkedHashSet<>(layer);
         values.retainAll(filter);
         return Set.copyOf(values);
      }

      private static Set<VisualVariant> intersectVariants(Set<VisualVariant> layer, Set<VisualVariant> filter) {
         if (filter.isEmpty()) {
            return layer;
         }
         if (layer.isEmpty()) {
            return filter;
         }
         LinkedHashSet<VisualVariant> values = new LinkedHashSet<>(layer);
         values.retainAll(filter);
         return Set.copyOf(values);
      }
   }
}
