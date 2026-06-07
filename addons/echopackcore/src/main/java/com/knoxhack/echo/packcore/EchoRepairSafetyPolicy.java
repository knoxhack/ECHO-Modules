package com.knoxhack.echo.packcore;

public record EchoRepairSafetyPolicy(
        boolean executesRepairs,
        boolean deletesJars,
        boolean downloadsModules,
        boolean modifiesSaves,
        boolean resetsConfigs,
        String summary
) {
    public EchoRepairSafetyPolicy {
        summary = PackContractGuards.optionalText(summary);
    }

    public static EchoRepairSafetyPolicy planningOnly() {
        return new EchoRepairSafetyPolicy(
                false,
                false,
                false,
                false,
                false,
                "Repair plans are advisory only and do not execute filesystem changes."
        );
    }
}
