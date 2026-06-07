package com.knoxhack.echoindustrialnexus.flux;

public interface ThermalFluxStorage {
   int getFluxStored();

   int getMaxFluxStored();

   int receiveFlux(int var1, boolean var2);

   int extractFlux(int var1, boolean var2);

   default boolean canReceive() {
      return true;
   }

   default boolean canExtract() {
      return true;
   }
}
