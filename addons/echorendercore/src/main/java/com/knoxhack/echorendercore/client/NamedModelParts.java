package com.knoxhack.echorendercore.client;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;

public final class NamedModelParts {
   private final Map<String, ModelPart> parts;

   private NamedModelParts(Map<String, ModelPart> parts) {
      this.parts = Map.copyOf(parts);
   }

   public ModelPart get(String name) {
      return parts.get(name);
   }

   public Map<String, ModelPart> asMap() {
      return parts;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static final class Builder {
      private final Map<String, ModelPart> parts = new LinkedHashMap<>();

      public Builder put(String alias, ModelPart part) {
         if (alias != null && !alias.isBlank() && part != null) {
            parts.put(alias.trim(), part);
         }
         return this;
      }

      public NamedModelParts build() {
         return new NamedModelParts(parts);
      }
   }
}
