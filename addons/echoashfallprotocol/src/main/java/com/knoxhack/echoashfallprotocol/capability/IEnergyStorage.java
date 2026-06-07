package com.knoxhack.echoashfallprotocol.capability;

import com.knoxhack.echo.adaptercore.EchoMutableEnergyStorage;

/**
 * Energy storage interface for power network system.
 * Machines implement this to consume power from the grid.
 */
public interface IEnergyStorage extends EchoMutableEnergyStorage {
    
    /**
     * Get current stored energy.
     */
    int getEnergyStored();
    
    /**
     * Get maximum energy capacity.
     */
    int getMaxEnergyStored();
    
    /**
     * Add energy to storage.
     * @param amount Amount to add
     * @param simulate If true, don't actually add
     * @return Amount actually added
     */
    int receiveEnergy(int amount, boolean simulate);
    
    /**
     * Remove energy from storage.
     * @param amount Amount to remove
     * @param simulate If true, don't actually remove
     * @return Amount actually removed
     */
    int extractEnergy(int amount, boolean simulate);
    
    /**
     * Check if storage can receive energy.
     */
    boolean canReceive();
    
    /**
     * Check if storage can provide energy.
     */
    boolean canExtract();
    
    /**
     * Set stored energy directly (for power failure events).
     */
    void setEnergyStored(int energy);
}
