package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knoxhack.echorendercore.animation.AnimationChannel;
import com.knoxhack.echorendercore.animation.Easing;
import net.minecraft.resources.Identifier;

public final class AnimationProfileBuilder extends RenderCoreProfileBuilder<AnimationProfileBuilder> {
   private AnimationProfileBuilder(Identifier id) {
      super(id);
      json.add("animations", new JsonObject());
   }

   public static AnimationProfileBuilder create(Identifier id) {
      return new AnimationProfileBuilder(id);
   }

   public ClipBuilder clip(String id, boolean loop, float length) {
      JsonObject clip = new JsonObject();
      clip.addProperty("loop", loop);
      clip.addProperty("length", length);
      clip.add("tracks", new JsonArray());
      json.getAsJsonObject("animations").add(id, clip);
      return new ClipBuilder(this, clip);
   }

   public static final class ClipBuilder {
      private final AnimationProfileBuilder parent;
      private final JsonObject clip;

      private ClipBuilder(AnimationProfileBuilder parent, JsonObject clip) {
         this.parent = parent;
         this.clip = clip;
      }

      public ClipBuilder transitionSeconds(float seconds) {
         clip.addProperty("transition_seconds", seconds);
         return this;
      }

      public ClipBuilder blendMode(String mode) {
         clip.addProperty("blend_mode", mode);
         return this;
      }

      public ClipBuilder track(String part, AnimationChannel channel, float from, float to, Easing easing) {
         JsonObject track = baseTrack(part, channel, easing);
         track.addProperty("from", from);
         track.addProperty("to", to);
         clip.getAsJsonArray("tracks").add(track);
         return this;
      }

      public ClipBuilder keyframeTrack(String part, AnimationChannel channel, Easing easing, Keyframe... keyframes) {
         JsonObject track = baseTrack(part, channel, easing);
         JsonArray frames = new JsonArray();
         for (Keyframe keyframe : keyframes) {
            JsonObject frame = new JsonObject();
            frame.addProperty("time", keyframe.time());
            frame.addProperty("value", keyframe.value());
            frames.add(frame);
         }
         track.add("keyframes", frames);
         clip.getAsJsonArray("tracks").add(track);
         return this;
      }

      public AnimationProfileBuilder endClip() {
         return parent;
      }

      private JsonObject baseTrack(String part, AnimationChannel channel, Easing easing) {
         JsonObject track = new JsonObject();
         track.addProperty("part", part);
         track.addProperty("channel", channel.name().toLowerCase(java.util.Locale.ROOT));
         track.addProperty("easing", easing.name().toLowerCase(java.util.Locale.ROOT));
         return track;
      }
   }

   public record Keyframe(float time, float value) {
   }
}
