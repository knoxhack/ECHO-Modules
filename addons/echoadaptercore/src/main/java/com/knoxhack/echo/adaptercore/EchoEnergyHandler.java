package com.knoxhack.echo.adaptercore;

/**
 * Native-facing energy view for addon gameplay code.
 */
public interface EchoEnergyHandler {
    long amount();

    long capacity();

    int insertEnergy(int amount);

    int extractEnergy(int amount);

    int simulateInsertEnergy(int amount);

    int simulateExtractEnergy(int amount);

    default int amountAsInt() {
        return Math.toIntExact(Math.clamp(amount(), 0L, Integer.MAX_VALUE));
    }

    default int capacityAsInt() {
        return Math.toIntExact(Math.clamp(capacity(), 0L, Integer.MAX_VALUE));
    }
}
