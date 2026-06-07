package com.knoxhack.echo.adaptercore;

/**
 * Mutable energy storage implemented by live runtime machine instances.
 */
public interface EchoMutableEnergyStorage {
    int getEnergyStored();

    int getMaxEnergyStored();

    int receiveEnergy(int amount, boolean simulate);

    int extractEnergy(int amount, boolean simulate);

    boolean canReceive();

    boolean canExtract();

    void setEnergyStored(int energy);
}
