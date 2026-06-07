package com.knoxhack.echoindustrialnexus.flux;

import com.knoxhack.echoindustrialnexus.Config;
import com.knoxhack.echoindustrialnexus.block.IndustrialFluxDuctBlock;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class ThermalFluxNetwork {
   private static final int MAX_DUCTS_SCANNED = 96;

   private ThermalFluxNetwork() {
   }

   public static int drawFlux(Level level, BlockPos machinePos, int requested) {
      if (requested <= 0) {
         return 0;
      } else {
         int transferLimit = Math.max((Integer)Config.DUCT_TRANSFER_RATE.get(), networkTransferLimit(level, machinePos));
         int remaining = Math.min(requested, transferLimit);
         int extracted = 0;
         Set<BlockPos> visitedDucts = new HashSet<>();
         Queue<BlockPos> queue = new ArrayDeque<>();

         for (Direction direction : Direction.values()) {
            BlockPos neighbor = machinePos.relative(direction);
            extracted += tryExtract(level, neighbor, remaining - extracted);
            if (extracted >= remaining) {
               return extracted;
            }

            if (level.getBlockState(neighbor).getBlock() instanceof IndustrialFluxDuctBlock && visitedDucts.add(neighbor)) {
               queue.add(neighbor);
            }
         }

         while (!queue.isEmpty() && visitedDucts.size() <= 96 && extracted < remaining) {
            BlockPos duct = queue.remove();

            for (Direction direction : Direction.values()) {
               BlockPos neighborx = duct.relative(direction);
               if (!neighborx.equals(machinePos)) {
                  extracted += tryExtract(level, neighborx, remaining - extracted);
                  if (extracted >= remaining) {
                     return extracted;
                  }

                  if (level.getBlockState(neighborx).getBlock() instanceof IndustrialFluxDuctBlock && visitedDucts.add(neighborx)) {
                     queue.add(neighborx);
                  }
               }
            }
         }

         return extracted;
      }
   }

   private static int networkTransferLimit(Level level, BlockPos machinePos) {
      int limit = 0;
      Set<BlockPos> visitedDucts = new HashSet<>();
      Queue<BlockPos> queue = new ArrayDeque<>();

      for (Direction direction : Direction.values()) {
         BlockPos neighbor = machinePos.relative(direction);
         if (level.getBlockState(neighbor).getBlock() instanceof IndustrialFluxDuctBlock duct && visitedDucts.add(neighbor)) {
            limit = Math.max(limit, duct.transferLimit());
            queue.add(neighbor);
         }
      }

      while (!queue.isEmpty() && visitedDucts.size() <= 96) {
         BlockPos ductPos = queue.remove();

         for (Direction directionx : Direction.values()) {
            BlockPos neighbor = ductPos.relative(directionx);
            if (level.getBlockState(neighbor).getBlock() instanceof IndustrialFluxDuctBlock duct && visitedDucts.add(neighbor)) {
               limit = Math.max(limit, duct.transferLimit());
               queue.add(neighbor);
            }
         }
      }

      return limit;
   }

   public static ThermalFluxNetwork.ScanReport scan(Level level, BlockPos origin) {
      Set<BlockPos> visitedDucts = new HashSet<>();
      Set<BlockPos> visitedStorages = new HashSet<>();
      Set<BlockPos> linkedMachines = new LinkedHashSet<>();
      Queue<BlockPos> queue = new ArrayDeque<>();
      ThermalFluxNetwork.MutableScan scan = new ThermalFluxNetwork.MutableScan();
      addStorage(level, origin, origin, visitedStorages, linkedMachines, scan);
      if (level.getBlockState(origin).getBlock() instanceof IndustrialFluxDuctBlock && visitedDucts.add(origin)) {
         queue.add(origin);
      }

      for (Direction direction : Direction.values()) {
         BlockPos neighbor = origin.relative(direction);
         addStorage(level, neighbor, origin, visitedStorages, linkedMachines, scan);
         if (level.getBlockState(neighbor).getBlock() instanceof IndustrialFluxDuctBlock && visitedDucts.add(neighbor)) {
            queue.add(neighbor);
         }
      }

      while (!queue.isEmpty() && visitedDucts.size() <= 96) {
         BlockPos duct = queue.remove();

         for (Direction directionx : Direction.values()) {
            BlockPos neighbor = duct.relative(directionx);
            addStorage(level, neighbor, origin, visitedStorages, linkedMachines, scan);
            if (level.getBlockState(neighbor).getBlock() instanceof IndustrialFluxDuctBlock && visitedDucts.add(neighbor)) {
               queue.add(neighbor);
            }
         }
      }

      return new ThermalFluxNetwork.ScanReport(visitedDucts.size(), scan.suppliers, scan.receivers, scan.storedFlux, scan.capacity, List.copyOf(linkedMachines));
   }


   public static int balanceFlux(Level level, BlockPos ductPos, int transferLimit) {
      if (level.isClientSide() || transferLimit <= 0) {
         return 0;
      }
      int remaining = Math.max(1, transferLimit);
      int moved = 0;
      for (Direction sourceDirection : Direction.values()) {
         if (remaining <= 0) {
            break;
         }
         BlockPos sourcePos = ductPos.relative(sourceDirection);
         if (!(level.getBlockEntity(sourcePos) instanceof ThermalFluxStorage source) || !source.canExtract()) {
            continue;
         }
         int available = source.extractFlux(remaining, true);
         if (available <= 0) {
            continue;
         }
         for (Direction receiverDirection : Direction.values()) {
            if (remaining <= 0) {
               break;
            }
            BlockPos receiverPos = ductPos.relative(receiverDirection);
            if (receiverPos.equals(sourcePos)) {
               continue;
            }
            if (level.getBlockEntity(receiverPos) instanceof ThermalFluxStorage receiver && receiver.canReceive()) {
               int accepted = receiver.receiveFlux(Math.min(available, remaining), true);
               if (accepted <= 0) {
                  continue;
               }
               int extracted = source.extractFlux(accepted, false);
               if (extracted <= 0) {
                  continue;
               }
               int delivered = Math.max(0, extracted - lossFor(level, ductPos, extracted));
               int received = receiver.receiveFlux(delivered, false);
               moved += received;
               remaining -= received;
               available -= extracted;
               if (available <= 0) {
                  break;
               }
            }
         }
      }
      return moved;
   }

   private static int tryExtract(Level level, BlockPos pos, int amount) {
      if (amount <= 0) {
         return 0;
      } else {
         return level.getBlockEntity(pos) instanceof ThermalFluxStorage storage && storage.canExtract() ? storage.extractFlux(amount, false) : 0;
      }
   }

   private static int lossFor(Level level, BlockPos ductPos, int amount) {
      if (!(level.getBlockState(ductPos).getBlock() instanceof IndustrialFluxDuctBlock duct) || amount <= 0) {
         return 0;
      }
      int divisor = duct.transferLimit() >= 3000 ? 64 : duct.transferLimit() >= 1500 ? 48 : duct.transferLimit() >= 700 ? 32 : 24;
      return Math.min(amount - 1, Math.max(0, amount / divisor));
   }

   private static void addStorage(Level level, BlockPos pos, BlockPos origin, Set<BlockPos> visitedStorages, Set<BlockPos> linkedMachines, ThermalFluxNetwork.MutableScan scan) {
      if (visitedStorages.add(pos)) {
         if (level.getBlockEntity(pos) instanceof ThermalFluxStorage storage) {
            if (!pos.equals(origin)) {
               linkedMachines.add(pos.immutable());
            }
            if (storage.canExtract()) {
               scan.suppliers++;
            }

            if (storage.canReceive()) {
               scan.receivers++;
            }

            scan.storedFlux = scan.storedFlux + storage.getFluxStored();
            scan.capacity = scan.capacity + storage.getMaxFluxStored();
         }
      }
   }

   private static final class MutableScan {
      private int suppliers;
      private int receivers;
      private int storedFlux;
      private int capacity;
   }

   public record ScanReport(int ducts, int suppliers, int receivers, int storedFlux, int capacity, List<BlockPos> linkedMachines) {
      public boolean detected() {
         return this.ducts > 0 || this.suppliers > 0 || this.receivers > 0 || this.capacity > 0;
      }
   }
}
