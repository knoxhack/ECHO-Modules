package com.knoxhack.echorendercore.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AnimationTimeline(List<AnimationKeyframe> keyframes) {
   public static final AnimationTimeline EMPTY = new AnimationTimeline(List.of());

   public AnimationTimeline {
      if (keyframes == null || keyframes.isEmpty()) {
         keyframes = List.of();
      } else {
         ArrayList<AnimationKeyframe> sorted = new ArrayList<>(keyframes);
         Collections.sort(sorted);
         keyframes = List.copyOf(sorted);
      }
   }

   public boolean empty() {
      return keyframes.isEmpty();
   }

   public float valueAt(float clipTime, float fallback) {
      if (keyframes.isEmpty()) {
         return fallback;
      }
      if (keyframes.size() == 1 || clipTime <= keyframes.getFirst().time()) {
         return keyframes.getFirst().value();
      }
      AnimationKeyframe previous = keyframes.getFirst();
      for (int i = 1; i < keyframes.size(); i++) {
         AnimationKeyframe next = keyframes.get(i);
         if (clipTime <= next.time()) {
            float span = Math.max(0.001F, next.time() - previous.time());
            float eased = next.easing().apply((clipTime - previous.time()) / span);
            return previous.value() + (next.value() - previous.value()) * eased;
         }
         previous = next;
      }
      return keyframes.getLast().value();
   }
}
