package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public abstract class RenderCoreProfileBuilder<T extends RenderCoreProfileBuilder<T>> {
   protected final Identifier id;
   protected final JsonObject json = new JsonObject();

   protected RenderCoreProfileBuilder(Identifier id) {
      this.id = id;
   }

   public Identifier id() {
      return id;
   }

   public JsonObject toJson() {
      return json.deepCopy();
   }

   @SuppressWarnings("unchecked")
   protected T self() {
      return (T)this;
   }

   protected T add(String key, String value) {
      if (value != null && !value.isBlank()) {
         json.addProperty(key, value);
      }
      return self();
   }

   protected T add(String key, Identifier value) {
      if (value != null) {
         json.addProperty(key, value.toString());
      }
      return self();
   }

   protected T add(String key, int value) {
      json.addProperty(key, value);
      return self();
   }

   protected T add(String key, float value) {
      json.addProperty(key, value);
      return self();
   }

   protected T add(String key, boolean value) {
      json.addProperty(key, value);
      return self();
   }

   protected static JsonArray vector(RenderCoreVector vector) {
      JsonArray array = new JsonArray();
      RenderCoreVector value = vector == null ? RenderCoreVector.ZERO : vector;
      array.add(value.x());
      array.add(value.y());
      array.add(value.z());
      return array;
   }

   protected static String color(int argb) {
      return "#%08X".formatted(argb);
   }
}
