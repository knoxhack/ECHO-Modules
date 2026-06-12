package com.knoxhack.echoopenlandsprotocol.runtime;

public record OpenlandsBuilderActionResult(
        String commandId,
        boolean accepted,
        String reason,
        boolean serverAuthoritativeRequired,
        boolean inventoryMutation
) {
    public OpenlandsBuilderActionResult {
        if (commandId == null || commandId.isBlank()) {
            throw new IllegalArgumentException("commandId must not be blank");
        }
    }

    public static OpenlandsBuilderActionResult accepted(String commandId, boolean inventoryMutation) {
        return new OpenlandsBuilderActionResult(commandId, true, "accepted", true, inventoryMutation);
    }

    public static OpenlandsBuilderActionResult rejected(String commandId, String reason, boolean inventoryMutation) {
        return new OpenlandsBuilderActionResult(commandId, false, reason, true, inventoryMutation);
    }
}
