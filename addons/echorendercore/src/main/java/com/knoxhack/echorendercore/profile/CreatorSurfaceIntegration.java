package com.knoxhack.echorendercore.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import net.minecraft.resources.Identifier;

public record CreatorSurfaceIntegration(
   int worldSurfaceCount,
   int screenSurfaceCount,
   int particleOnlyStaticSurfaceCount,
   int convertedBlockEntityCount,
   int fallbackRendererCount,
   List<Identifier> profileIds
) {
   public static final CreatorSurfaceIntegration EMPTY = new CreatorSurfaceIntegration(0, 0, 0, 0, 0, List.of());

   public CreatorSurfaceIntegration {
      worldSurfaceCount = Math.max(0, worldSurfaceCount);
      screenSurfaceCount = Math.max(0, screenSurfaceCount);
      particleOnlyStaticSurfaceCount = Math.max(0, particleOnlyStaticSurfaceCount);
      convertedBlockEntityCount = Math.max(0, convertedBlockEntityCount);
      fallbackRendererCount = Math.max(0, fallbackRendererCount);
      profileIds = profileIds == null ? List.of() : profileIds.stream()
         .filter(id -> id != null)
         .sorted(java.util.Comparator.comparing(Identifier::toString))
         .toList();
   }

   public JsonObject toJson() {
      JsonObject root = new JsonObject();
      root.addProperty("world_surface_count", worldSurfaceCount);
      root.addProperty("screen_surface_count", screenSurfaceCount);
      root.addProperty("particle_only_static_surface_count", particleOnlyStaticSurfaceCount);
      root.addProperty("converted_block_entity_count", convertedBlockEntityCount);
      root.addProperty("fallback_renderer_count", fallbackRendererCount);
      JsonArray profiles = new JsonArray();
      profileIds.forEach(id -> profiles.add(id.toString()));
      root.add("profile_ids", profiles);
      return root;
   }
}
