package com.knoxhack.echoarcanacore.api;

import net.minecraft.resources.Identifier;

public record AetherModifier(
        Identifier id,
        AetherSignalType type,
        double maxMultiplier,
        double regenerationMultiplier,
        double costMultiplier,
        int ticksRemaining) {
    public AetherModifier {
        type = type == null ? AetherSignalType.RAW_AETHER : type;
        maxMultiplier = maxMultiplier <= 0.0D ? 1.0D : maxMultiplier;
        regenerationMultiplier = regenerationMultiplier <= 0.0D ? 1.0D : regenerationMultiplier;
        costMultiplier = costMultiplier <= 0.0D ? 1.0D : costMultiplier;
        ticksRemaining = Math.max(0, ticksRemaining);
    }
}
