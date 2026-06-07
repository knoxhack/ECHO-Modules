package com.knoxhack.echorendercore.animation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ModelPose {
   private final Map<String, PartTransform> transforms = new LinkedHashMap<>();

   public void clear() {
      transforms.clear();
   }

   public void apply(String part, AnimationChannel channel, float value) {
      apply(part, channel, value, AnimationBlendMode.REPLACE);
   }

   public void apply(String part, AnimationChannel channel, float value, AnimationBlendMode blendMode) {
      if (part == null || part.isBlank()) {
         return;
      }
      transforms.compute(part, (key, existing) -> {
         PartTransform current = existing == null ? PartTransform.IDENTITY : existing;
         PartTransform next = PartTransform.IDENTITY.with(channel, value);
         return blendMode == AnimationBlendMode.ADDITIVE ? current.add(next) : current.with(channel, value);
      });
   }

   public void copyFrom(ModelPose other) {
      clear();
      if (other != null) {
         transforms.putAll(other.transforms);
      }
   }

   public void blendFrom(ModelPose previous, ModelPose next, float weight) {
      clear();
      if (previous == null && next == null) {
         return;
      }
      if (previous != null) {
         for (String part : previous.parts()) {
            transforms.put(part, previous.transform(part).blend(next == null ? PartTransform.IDENTITY : next.transform(part), weight));
         }
      }
      if (next != null) {
         for (String part : next.parts()) {
            transforms.computeIfAbsent(part, key -> PartTransform.IDENTITY.blend(next.transform(key), weight));
         }
      }
   }

   public PartTransform transform(String part) {
      return transforms.getOrDefault(part, PartTransform.IDENTITY);
   }

   public Set<String> parts() {
      return Collections.unmodifiableSet(transforms.keySet());
   }

   public boolean isEmpty() {
      return transforms.isEmpty();
   }
}
