package com.knoxhack.echorendercore.animation;

public record AnimationTrack(
   String part,
   AnimationChannel channel,
   float from,
   float to,
   float startTime,
   float endTime,
   Easing easing,
   AnimationTimeline timeline,
   AnimationBlendMode blendMode
) {
   public AnimationTrack(String part, AnimationChannel channel, float from, float to, float startTime, float endTime, Easing easing) {
      this(part, channel, from, to, startTime, endTime, easing, AnimationTimeline.EMPTY, AnimationBlendMode.REPLACE);
   }

   public AnimationTrack {
      part = part == null ? "" : part.trim();
      channel = channel == null ? AnimationChannel.POSITION_Y : channel;
      if (endTime <= startTime) {
         endTime = startTime + 1.0F;
      }
      easing = easing == null ? Easing.LINEAR : easing;
      timeline = timeline == null ? AnimationTimeline.EMPTY : timeline;
      blendMode = blendMode == null ? AnimationBlendMode.REPLACE : blendMode;
   }

   public float valueAt(float clipTime, float clipLength) {
      if (!timeline.empty()) {
         return timeline.valueAt(clipTime, from);
      }
      float end = endTime <= 0.0F ? Math.max(clipLength, 1.0F) : endTime;
      float duration = Math.max(0.001F, end - startTime);
      float t = (clipTime - startTime) / duration;
      float eased = easing.apply(t);
      return from + (to - from) * eased;
   }
}
