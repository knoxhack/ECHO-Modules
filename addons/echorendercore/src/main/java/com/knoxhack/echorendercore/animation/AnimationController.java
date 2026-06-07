package com.knoxhack.echorendercore.animation;

import com.knoxhack.echorendercore.api.VisualState;
import java.util.EnumMap;
import java.util.Map;

public final class AnimationController {
   private final Map<VisualState, AnimationClip> stateClips = new EnumMap<>(VisualState.class);
   private final AnimationPlayer player = new AnimationPlayer();
   private VisualState currentState = VisualState.IDLE;
   private float transitionSeconds = AnimationClip.DEFAULT_TRANSITION_SECONDS;

   public void bind(VisualState state, AnimationClip clip) {
      if (state != null && clip != null) {
         stateClips.put(state, clip);
      }
   }

   public void transitionSeconds(float transitionSeconds) {
      this.transitionSeconds = Math.max(0.0F, transitionSeconds);
   }

   public void update(VisualState state, float ageInTicks) {
      VisualState next = state == null ? VisualState.IDLE : state;
      AnimationClip clip = stateClips.get(next);
      if (clip == null) {
         player.stop();
         currentState = next;
         return;
      }
      if (next != currentState || !player.playing()) {
         player.play(clip, ageInTicks, clip.transitionSeconds() > 0.0F ? clip.transitionSeconds() : transitionSeconds);
         currentState = next;
      }
   }

   public void sample(float ageInTicks, ModelPose target) {
      player.sample(ageInTicks, target);
   }
}
