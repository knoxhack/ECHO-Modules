package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.knoxhack.echorendercore.api.VisualState;
import net.minecraft.resources.Identifier;

public final class ParticleProfileBuilder extends RenderCoreProfileBuilder<ParticleProfileBuilder> {
   private ParticleProfileBuilder(Identifier id) {
      super(id);
      json.add("emitters", new JsonObject());
   }

   public static ParticleProfileBuilder create(Identifier id) {
      return new ParticleProfileBuilder(id);
   }

   public EmitterBuilder emitter(String id, String anchor, Identifier particle) {
      JsonObject emitter = new JsonObject();
      emitter.addProperty("anchor", anchor);
      emitter.addProperty("particle", particle.toString());
      emitter.add("offset", vector(RenderCoreVector.ZERO));
      emitter.add("velocity", vector(RenderCoreVector.ZERO));
      json.getAsJsonObject("emitters").add(id, emitter);
      return new EmitterBuilder(this, emitter);
   }

   public static final class EmitterBuilder {
      private final ParticleProfileBuilder parent;
      private final JsonObject emitter;

      private EmitterBuilder(ParticleProfileBuilder parent, JsonObject emitter) {
         this.parent = parent;
         this.emitter = emitter;
      }

      public EmitterBuilder state(VisualState state) {
         emitter.addProperty("state", state.name());
         return this;
      }

      public EmitterBuilder states(VisualState... states) {
         JsonArray array = new JsonArray();
         for (VisualState state : states) {
            array.add(state.name());
         }
         emitter.add("states", array);
         return this;
      }

      public EmitterBuilder rate(float rate) {
         emitter.addProperty("rate", rate);
         return this;
      }

      public EmitterBuilder burstCount(int burstCount) {
         emitter.addProperty("burst_count", burstCount);
         return this;
      }

      public EmitterBuilder offset(RenderCoreVector offset) {
         emitter.add("offset", vector(offset));
         return this;
      }

      public EmitterBuilder velocity(RenderCoreVector velocity) {
         emitter.add("velocity", vector(velocity));
         return this;
      }

      public EmitterBuilder option(String key, String value) {
         JsonObject options = options();
         options.addProperty(key, value);
         return this;
      }

      public EmitterBuilder option(String key, int value) {
         JsonObject options = options();
         options.addProperty(key, value);
         return this;
      }

      public EmitterBuilder optionType(String type) {
         return option("type", type);
      }

      public ParticleProfileBuilder endEmitter() {
         return parent;
      }

      private JsonObject options() {
         if (!emitter.has("options") || !emitter.get("options").isJsonObject()) {
            emitter.add("options", new JsonObject());
         }
         return emitter.getAsJsonObject("options");
      }
   }
}
