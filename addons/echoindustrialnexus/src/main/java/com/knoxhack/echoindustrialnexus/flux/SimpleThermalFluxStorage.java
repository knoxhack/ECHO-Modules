package com.knoxhack.echoindustrialnexus.flux;

public class SimpleThermalFluxStorage implements ThermalFluxStorage {
   private final int capacity;
   private int flux;

   public SimpleThermalFluxStorage(int capacity) {
      this.capacity = Math.max(1, capacity);
   }

   @Override
   public int getFluxStored() {
      return this.flux;
   }

   @Override
   public int getMaxFluxStored() {
      return this.capacity;
   }

   @Override
   public int receiveFlux(int amount, boolean simulate) {
      if (amount <= 0) {
         return 0;
      } else {
         int received = Math.min(amount, this.capacity - this.flux);
         if (!simulate) {
            this.flux += received;
         }

         return received;
      }
   }

   @Override
   public int extractFlux(int amount, boolean simulate) {
      if (amount <= 0) {
         return 0;
      } else {
         int extracted = Math.min(amount, this.flux);
         if (!simulate) {
            this.flux -= extracted;
         }

         return extracted;
      }
   }

   public void setFlux(int flux) {
      this.flux = Math.clamp((long)flux, 0, this.capacity);
   }
}
