package com.knoxhack.echonexusprotocol.data;

import com.knoxhack.echo.adaptercore.EchoMutableEnergyStorage;

public class NexusEnergyStorage implements EchoMutableEnergyStorage {
   private final int capacity;
   private final int maxReceive;
   private final int maxExtract;
   private final Runnable onChanged;
   private int energy;

   public NexusEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
      this.capacity = Math.max(0, capacity);
      this.maxReceive = Math.max(0, maxReceive);
      this.maxExtract = Math.max(0, maxExtract);
      this.onChanged = onChanged == null ? () -> {} : onChanged;
   }

   public int receiveEnergy(int amount, boolean simulate) {
      if (amount > 0 && this.maxReceive > 0) {
         int accepted = Math.min(Math.min(amount, this.maxReceive), Math.max(0, this.capacity - this.energy));
         if (accepted > 0 && !simulate) {
            this.energy += accepted;
            this.onChanged.run();
         }

         return accepted;
      } else {
         return 0;
      }
   }

   public int extractEnergy(int amount, boolean simulate) {
      if (amount > 0 && this.maxExtract > 0) {
         int extracted = Math.min(Math.min(amount, this.maxExtract), this.energy);
         if (extracted > 0 && !simulate) {
            this.energy -= extracted;
            this.onChanged.run();
         }

         return extracted;
      } else {
         return 0;
      }
   }

   public boolean canReceive() {
      return this.maxReceive > 0;
   }

   public boolean canExtract() {
      return this.maxExtract > 0;
   }

   public int getEnergyStored() {
      return this.energy;
   }

   public int getMaxEnergyStored() {
      return this.capacity;
   }

   public int getCapacityAsInt() {
      return this.capacity;
   }

   public int getSpace() {
      return Math.max(0, this.capacity - this.energy);
   }

   public void setEnergyStored(int energy) {
      int next = Math.max(0, Math.min(this.capacity, energy));
      if (this.energy != next) {
         this.energy = next;
         this.onChanged.run();
      }
   }

   public boolean consume(int amount) {
      if (this.energy < amount) {
         return false;
      } else {
         this.setEnergyStored(this.energy - amount);
         return true;
      }
   }

   public void receiveDirect(int amount) {
      this.setEnergyStored(this.energy + Math.max(0, amount));
   }

}
