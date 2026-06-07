package com.knoxhack.echorendercore.animation;

public final class AnimationPlayer {
   private AnimationClip clip = AnimationClip.empty("empty");
   private AnimationClip previousClip = AnimationClip.empty("empty");
   private float startedAt;
   private float previousStartedAt;
   private float transitionStartedAt;
   private float transitionSeconds;
   private boolean playing;
   private boolean transitioning;
   private final ModelPose previousPose = new ModelPose();
   private final ModelPose currentPose = new ModelPose();

   public void play(AnimationClip next, float ageInTicks) {
      play(next, ageInTicks, next == null ? 0.0F : next.transitionSeconds());
   }

   public void play(AnimationClip next, float ageInTicks, float transitionSeconds) {
      if (next == null) {
         stop();
         return;
      }
      if (!next.id().equals(clip.id())) {
         this.previousClip = this.clip;
         this.previousStartedAt = this.startedAt;
         this.transitionStartedAt = ageInTicks;
         this.transitionSeconds = Math.max(0.0F, transitionSeconds);
         this.transitioning = playing && !previousClip.tracks().isEmpty() && this.transitionSeconds > 0.0F;
         this.clip = next;
         this.startedAt = ageInTicks;
      }
      this.playing = true;
   }

   public void stop() {
      this.playing = false;
      this.clip = AnimationClip.empty("empty");
      this.previousClip = AnimationClip.empty("empty");
      this.startedAt = 0.0F;
      this.previousStartedAt = 0.0F;
      this.transitionStartedAt = 0.0F;
      this.transitionSeconds = 0.0F;
      this.transitioning = false;
   }

   public boolean playing() {
      return playing;
   }

   public void sample(float ageInTicks, ModelPose target) {
      target.clear();
      if (!playing || clip.tracks().isEmpty()) {
         return;
      }
      if (!sampleClip(clip, startedAt, ageInTicks, currentPose)) {
         playing = false;
         transitioning = false;
         return;
      }
      if (transitioning) {
         float transitionTicks = Math.max(0.0F, (ageInTicks - transitionStartedAt) / 20.0F);
         float weight = transitionSeconds <= 0.0F ? 1.0F : transitionTicks / transitionSeconds;
         if (weight >= 1.0F || !sampleClip(previousClip, previousStartedAt, ageInTicks, previousPose)) {
            transitioning = false;
            target.copyFrom(currentPose);
            return;
         }
         target.blendFrom(previousPose, currentPose, weight);
      } else {
         target.copyFrom(currentPose);
      }
   }

   public static void sample(AnimationClip clip, float ageInTicks, ModelPose target) {
      target.clear();
      if (clip == null || clip.tracks().isEmpty()) {
         return;
      }
      sampleClip(clip, 0.0F, ageInTicks, target);
   }

   private static boolean sampleClip(AnimationClip clip, float startedAt, float ageInTicks, ModelPose target) {
      target.clear();
      float elapsedTicks = Math.max(0.0F, ageInTicks - startedAt);
      float elapsedSeconds = elapsedTicks / 20.0F;
      if (!clip.loop() && elapsedSeconds > clip.length()) {
         return false;
      }
      float clipTime = clip.loop() ? elapsedSeconds % clip.length() : Math.min(elapsedSeconds, clip.length());
      for (AnimationTrack track : clip.tracks()) {
         target.apply(track.part(), track.channel(), track.valueAt(clipTime, clip.length()), track.blendMode());
      }
      return true;
   }
}
