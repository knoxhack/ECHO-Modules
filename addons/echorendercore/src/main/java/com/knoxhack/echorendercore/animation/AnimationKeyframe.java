package com.knoxhack.echorendercore.animation;

public record AnimationKeyframe(float time, float value, Easing easing) implements Comparable<AnimationKeyframe> {
   public AnimationKeyframe {
      time = Math.max(0.0F, time);
      easing = easing == null ? Easing.LINEAR : easing;
   }

   @Override
   public int compareTo(AnimationKeyframe other) {
      return Float.compare(this.time, other == null ? 0.0F : other.time);
   }
}
