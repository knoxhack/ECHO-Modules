package com.knoxhack.echoblockworks.content;

import java.util.List;
import java.util.Optional;

public record BlockworksPaletteKit(
   String id,
   String displayName,
   String description,
   String recommendedUsage,
   BlockworksTheme theme,
   List<String> familyIds,
   List<String> featuredBlockIds,
   List<String> accentBlockIds,
   Optional<String> worldgenSiteId
) {
   public BlockworksPaletteKit {
      familyIds = List.copyOf(familyIds);
      featuredBlockIds = List.copyOf(featuredBlockIds);
      accentBlockIds = List.copyOf(accentBlockIds);
      worldgenSiteId = worldgenSiteId == null ? Optional.empty() : worldgenSiteId;
   }

   public boolean includesFamily(String familyId) {
      return familyIds.contains(familyId);
   }

   public boolean includesBlock(String blockId) {
      return featuredBlockIds.contains(blockId) || accentBlockIds.contains(blockId);
   }
}
