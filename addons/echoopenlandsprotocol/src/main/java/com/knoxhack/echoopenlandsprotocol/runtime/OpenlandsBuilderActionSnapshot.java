package com.knoxhack.echoopenlandsprotocol.runtime;

public record OpenlandsBuilderActionSnapshot(
        String commandId,
        boolean playerCanEdit,
        boolean targetSupported,
        boolean variantExists,
        boolean hasRequiredItemOrCreative,
        boolean containerPermission,
        boolean serverAuthoritative,
        boolean chunkLoaded
) {
    public OpenlandsBuilderActionSnapshot {
        if (commandId == null || commandId.isBlank()) {
            throw new IllegalArgumentException("commandId must not be blank");
        }
    }
}
