package com.knoxhack.echorendercore.animation;

import java.util.List;

public record AnimationClip(
   String id,
   boolean loop,
   float length,
   List<AnimationTrack> tracks,
   float transitionSeconds,
   AnimationBlendMode blendMode
) {
   public static final float DEFAULT_TRANSITION_SECONDS = 0.15F;

   public AnimationClip(String id, boolean loop, float length, List<AnimationTrack> tracks) {
      this(id, loop, length, tracks, DEFAULT_TRANSITION_SECONDS, AnimationBlendMode.REPLACE);
   }

   public AnimationClip {
      id = id == null || id.isBlank() ? "unnamed" : id.trim();
      length = Math.max(0.001F, length);
      tracks = tracks == null ? List.of() : List.copyOf(tracks);
      transitionSeconds = Math.max(0.0F, transitionSeconds);
      blendMode = blendMode == null ? AnimationBlendMode.REPLACE : blendMode;
   }

   public static AnimationClip empty(String id) {
      return new AnimationClip(id, false, 1.0F, List.of(), 0.0F, AnimationBlendMode.REPLACE);
   }
}
