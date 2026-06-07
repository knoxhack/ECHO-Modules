package com.knoxhack.echomultiblockcore.api;

public record ValidationOptions(
        boolean force,
        boolean requireLoadedChunks,
        boolean collectMatchedBlocks,
        long cacheTtlTicks) {
    public static final ValidationOptions DEFAULT = new ValidationOptions(false, true, true, 100L);
    public static final ValidationOptions FORCED = new ValidationOptions(true, true, true, 0L);

    public ValidationOptions {
        cacheTtlTicks = Math.max(0L, cacheTtlTicks);
    }
}
