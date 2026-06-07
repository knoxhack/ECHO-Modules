package com.knoxhack.echoblockworks.content;

import java.util.List;

public record BlockworksFamily(
   String id,
   String displayName,
   BlockworksTheme theme,
   String style,
   BlockworksUnlockTier unlockTier,
   List<BlockworksVariant> variants
) {
   public BlockworksFamily {
      variants = List.copyOf(variants);
   }
}
