package com.knoxhack.echo.packcore;

public record EchoRepairActionRequirement(
        String id,
        String summary,
        boolean satisfied,
        boolean blocking
) {
    public EchoRepairActionRequirement {
        id = PackContractGuards.requireText(id, "repair action requirement id");
        summary = PackContractGuards.optionalText(summary);
    }
}
