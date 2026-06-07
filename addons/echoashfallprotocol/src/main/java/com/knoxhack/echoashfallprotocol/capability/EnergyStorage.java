package com.knoxhack.echoashfallprotocol.capability;

/**
 * Default implementation of IEnergyStorage.
 */
public class EnergyStorage implements IEnergyStorage {
    
    protected int energy;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;
    
    public EnergyStorage(int capacity) {
        this(capacity, capacity, capacity, 0);
    }
    
    public EnergyStorage(int capacity, int maxTransfer) {
        this(capacity, maxTransfer, maxTransfer, 0);
    }
    
    public EnergyStorage(int capacity, int maxReceive, int maxExtract) {
        this(capacity, maxReceive, maxExtract, 0);
    }
    
    public EnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.energy = Math.max(0, Math.min(capacity, energy));
    }
    
    @Override
    public int getEnergyStored() {
        return energy;
    }
    
    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }
    
    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        int received = Math.min(capacity - energy, Math.min(maxReceive, amount));
        if (!simulate) {
            energy += received;
        }
        return received;
    }
    
    @Override
    public int extractEnergy(int amount, boolean simulate) {
        int extracted = Math.min(energy, Math.min(maxExtract, amount));
        if (!simulate) {
            energy -= extracted;
        }
        return extracted;
    }
    
    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }
    
    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }
    
    @Override
    public void setEnergyStored(int energy) {
        this.energy = Math.max(0, Math.min(capacity, energy));
    }
    
    /**
     * Get energy percentage (0.0 to 1.0)
     */
    public float getEnergyPercent() {
        return (float) energy / capacity;
    }
    
    /**
     * Check if storage is empty
     */
    public boolean isEmpty() {
        return energy <= 0;
    }
    
    /**
     * Check if storage is full
     */
    public boolean isFull() {
        return energy >= capacity;
    }
    
    /**
     * Consume energy for machine operation
     */
    public boolean consumeEnergy(int amount) {
        if (energy >= amount) {
            energy -= amount;
            return true;
        }
        return false;
    }
}
