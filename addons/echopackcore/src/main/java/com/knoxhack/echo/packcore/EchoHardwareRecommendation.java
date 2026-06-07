package com.knoxhack.echo.packcore;

public record EchoHardwareRecommendation(
        String tier,
        int minimumMemoryMb,
        int recommendedMemoryMb,
        String cpu,
        String gpu,
        String notes
) {
    public EchoHardwareRecommendation {
        tier = PackContractGuards.requireText(tier, "hardware tier");
        cpu = PackContractGuards.optionalText(cpu);
        gpu = PackContractGuards.optionalText(gpu);
        notes = PackContractGuards.optionalText(notes);
        if (minimumMemoryMb < 0 || recommendedMemoryMb < 0) {
            throw new IllegalArgumentException("memory recommendations must not be negative");
        }
    }
}
