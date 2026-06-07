package com.knoxhack.echopowergrid.api;

public interface EchoEnergyStorage {
    long getEnergyStored();
    long getMaxEnergyStored();
    long receiveEnergy(long amount, boolean simulate);
    long extractEnergy(long amount, boolean simulate);
    long getMaxInput();
    long getMaxOutput();
    boolean canReceive();
    boolean canExtract();
}
