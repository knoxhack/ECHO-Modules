package com.knoxhack.echoblockworks.content;

public record BlockworksBlockInfo(
   BlockworksFamily family,
   BlockworksVariant variant,
   BlockworksShapeKind shape,
   String blockId,
   String displayName
) {
}
