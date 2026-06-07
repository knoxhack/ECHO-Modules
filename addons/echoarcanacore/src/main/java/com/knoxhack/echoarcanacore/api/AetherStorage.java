package com.knoxhack.echoarcanacore.api;

import java.util.Set;

public record AetherStorage(
        double storedAmount,
        double maxStoredAmount,
        Set<AetherSignalType> acceptedTypes,
        AetherSignalType outputType,
        double transferRate,
        double contaminationLevel) {
    public AetherStorage {
        storedAmount = Math.max(0.0D, storedAmount);
        maxStoredAmount = Math.max(0.0D, maxStoredAmount);
        storedAmount = Math.min(storedAmount, maxStoredAmount);
        acceptedTypes = Set.copyOf(acceptedTypes == null || acceptedTypes.isEmpty()
                ? Set.of(AetherSignalType.RAW_AETHER)
                : acceptedTypes);
        outputType = outputType == null ? acceptedTypes.iterator().next() : outputType;
        transferRate = Math.max(0.0D, transferRate);
        contaminationLevel = Math.max(0.0D, contaminationLevel);
    }

    public boolean accepts(AetherSignalType type) {
        return acceptedTypes.contains(type);
    }

    public AetherStorage withStoredAmount(double value) {
        return new AetherStorage(value, maxStoredAmount, acceptedTypes, outputType, transferRate, contaminationLevel);
    }

    public AetherStorage withContamination(double value) {
        return new AetherStorage(storedAmount, maxStoredAmount, acceptedTypes, outputType, transferRate, value);
    }
}
