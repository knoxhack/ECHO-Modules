package com.knoxhack.echoblockworks.content;

public record BlockworksDetailSpec(
   String id,
   String displayName,
   BlockworksDetailKind kind,
   BlockworksTheme theme,
   BlockworksUnlockTier unlockTier,
   int light,
   boolean animated
) {
}
