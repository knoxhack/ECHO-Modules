package com.knoxhack.echoindustrialnexus.api;

import com.knoxhack.echoindustrialnexus.block.IndustrialMachineBlock;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Read-only Industrial machine telemetry for scanners, terminals, maps, and diagnostics.
 */
public interface IndustrialMachineTelemetryView {
   BlockPos pos();

   IndustrialMachineBlock.MachineKind kind();

   String displayName();

   String status();

   int fluxStored();

   int maxFluxStored();

   int heat();

   int progressTicks();

   int maxProgressTicks();

   int inputFluidId();

   int inputFluidAmount();

   int outputFluidId();

   int outputFluidAmount();

   String inputFluidName();

   String outputFluidName();

   String scrubberMode();

   String sideConfig();

   boolean remoteShutdown();

   int alertCount();

   InventoryContract inventory();

   EnergyContract energy();

   FluidContract fluids();

   ProcessContract process();

   SideConfigurationContract side();

   UpgradeContract upgrades();

   SavedMachineSnapshot savedSnapshot();

   static IndustrialMachineTelemetryView from(IndustrialMachineBlockEntity machine) {
      return Snapshot.from(machine);
   }

   private static String clean(String value, String fallback) {
      String cleaned = value == null ? "" : value.strip();
      return cleaned.isBlank() ? fallback : cleaned;
   }

   record SlotSnapshot(
      int index,
      String role,
      String itemId,
      String itemName,
      int count,
      boolean occupied
   ) {
      public SlotSnapshot {
         index = Math.max(0, index);
         role = clean(role, "internal");
         itemId = clean(itemId, "minecraft:air");
         itemName = clean(itemName, "Empty");
         count = Math.max(0, count);
      }
   }

   record InventoryContract(
      int totalSlots,
      int occupiedSlots,
      List<SlotSnapshot> slots
   ) {
      public InventoryContract {
         totalSlots = Math.max(0, totalSlots);
         slots = List.copyOf(slots == null ? List.of() : slots);
         occupiedSlots = Math.max(0, Math.min(occupiedSlots, slots.size()));
      }

      static InventoryContract empty() {
         return new InventoryContract(IndustrialMachineBlockEntity.SLOT_COUNT, 0, List.of());
      }
   }

   record EnergyContract(
      String resourceId,
      String unit,
      int stored,
      int capacity,
      boolean canReceive,
      boolean canExtract
   ) {
      public EnergyContract {
         resourceId = clean(resourceId, "echoindustrialnexus:thermal_flux");
         unit = clean(unit, "TF");
         stored = Math.max(0, stored);
         capacity = Math.max(0, capacity);
      }
   }

   record FluidTankSnapshot(
      String role,
      int fluidId,
      String fluidName,
      int amount,
      int capacity
   ) {
      public FluidTankSnapshot {
         role = clean(role, "tank");
         fluidId = Math.max(0, fluidId);
         fluidName = clean(fluidName, "Empty");
         amount = Math.max(0, amount);
         capacity = Math.max(0, capacity);
      }
   }

   record FluidContract(
      boolean supported,
      int tankCapacity,
      FluidTankSnapshot input,
      FluidTankSnapshot output
   ) {
      public FluidContract {
         tankCapacity = Math.max(0, tankCapacity);
         input = input == null ? new FluidTankSnapshot("input", 0, "Empty", 0, tankCapacity) : input;
         output = output == null ? new FluidTankSnapshot("output", 0, "Empty", 0, tankCapacity) : output;
      }
   }

   record ProcessContract(
      String status,
      boolean active,
      int progressTicks,
      int maxProgressTicks,
      int progressPercent,
      String recipeContract
   ) {
      public ProcessContract {
         status = clean(status, "Idle");
         progressTicks = Math.max(0, progressTicks);
         maxProgressTicks = Math.max(0, maxProgressTicks);
         progressPercent = Math.max(0, Math.min(100, progressPercent));
         recipeContract = clean(recipeContract, "machine_runtime");
      }
   }

   record SideConfigurationContract(
      String label,
      List<String> upSlots,
      List<String> downSlots,
      List<String> sideSlots
   ) {
      public SideConfigurationContract {
         label = clean(label, "Standard");
         upSlots = List.copyOf(upSlots == null ? List.of() : upSlots);
         downSlots = List.copyOf(downSlots == null ? List.of() : downSlots);
         sideSlots = List.copyOf(sideSlots == null ? List.of() : sideSlots);
      }
   }

   record UpgradeContract(
      int capacity,
      int installedCount,
      List<SlotSnapshot> slots
   ) {
      public UpgradeContract {
         capacity = Math.max(0, capacity);
         slots = List.copyOf(slots == null ? List.of() : slots);
         installedCount = Math.max(0, Math.min(installedCount, slots.size()));
      }
   }

   record SavedMachineSnapshot(
      String format,
      List<String> persistedKeys,
      int slotCount,
      int fluxStored,
      int heat,
      String status,
      String sideConfig,
      int inputFluidAmount,
      int outputFluidAmount,
      boolean remoteShutdown
   ) {
      public SavedMachineSnapshot {
         format = clean(format, "block_entity_value_output");
         persistedKeys = List.copyOf(persistedKeys == null ? List.of() : persistedKeys);
         slotCount = Math.max(0, slotCount);
         fluxStored = Math.max(0, fluxStored);
         heat = Math.max(0, Math.min(100, heat));
         status = clean(status, "Idle");
         sideConfig = clean(sideConfig, "Standard");
         inputFluidAmount = Math.max(0, inputFluidAmount);
         outputFluidAmount = Math.max(0, outputFluidAmount);
      }
   }

   record Snapshot(
      BlockPos pos,
      IndustrialMachineBlock.MachineKind kind,
      String displayName,
      String status,
      int fluxStored,
      int maxFluxStored,
      int heat,
      int progressTicks,
      int maxProgressTicks,
      int inputFluidId,
      int inputFluidAmount,
      int outputFluidId,
      int outputFluidAmount,
      String inputFluidName,
      String outputFluidName,
      String scrubberMode,
      String sideConfig,
      boolean remoteShutdown,
      int alertCount,
      InventoryContract inventory,
      EnergyContract energy,
      FluidContract fluids,
      ProcessContract process,
      SideConfigurationContract side,
      UpgradeContract upgrades,
      SavedMachineSnapshot savedSnapshot
   ) implements IndustrialMachineTelemetryView {
      public Snapshot {
         pos = pos == null ? BlockPos.ZERO : pos.immutable();
         kind = kind == null ? IndustrialMachineBlock.MachineKind.ORE_GRINDER : kind;
         displayName = clean(displayName, kind.displayName());
         status = clean(status, "Idle");
         fluxStored = Math.max(0, fluxStored);
         maxFluxStored = Math.max(0, maxFluxStored);
         heat = Math.max(0, Math.min(100, heat));
         progressTicks = Math.max(0, progressTicks);
         maxProgressTicks = Math.max(0, maxProgressTicks);
         inputFluidId = Math.max(0, inputFluidId);
         inputFluidAmount = Math.max(0, inputFluidAmount);
         outputFluidId = Math.max(0, outputFluidId);
         outputFluidAmount = Math.max(0, outputFluidAmount);
         inputFluidName = clean(inputFluidName, "Empty");
         outputFluidName = clean(outputFluidName, "Empty");
         scrubberMode = clean(scrubberMode, "Air Mode");
         sideConfig = clean(sideConfig, "Standard");
         alertCount = Math.max(0, alertCount);
         inventory = inventory == null ? InventoryContract.empty() : inventory;
         energy = energy == null ? new EnergyContract("echoindustrialnexus:thermal_flux", "TF", fluxStored, maxFluxStored, false, false) : energy;
         fluids = fluids == null ? new FluidContract(false, IndustrialMachineBlockEntity.FLUID_TANK_CAPACITY, null, null) : fluids;
         process = process == null ? new ProcessContract(status, false, progressTicks, maxProgressTicks, 0, "machine_runtime") : process;
         side = side == null ? new SideConfigurationContract(sideConfig, List.of(), List.of(), List.of()) : side;
         upgrades = upgrades == null ? new UpgradeContract(0, 0, List.of()) : upgrades;
         savedSnapshot = savedSnapshot == null ? savedSnapshot(null) : savedSnapshot;
      }

      private static Snapshot from(IndustrialMachineBlockEntity machine) {
         if (machine == null) {
            return new Snapshot(BlockPos.ZERO, IndustrialMachineBlock.MachineKind.ORE_GRINDER, "Industrial Machine",
               "Offline", 0, 0, 0, 0, 0, 0, 0, 0, 0, "Empty", "Empty", "Air Mode", "Standard", false, 0,
               InventoryContract.empty(),
               new EnergyContract("echoindustrialnexus:thermal_flux", "TF", 0, 0, false, false),
               new FluidContract(false, IndustrialMachineBlockEntity.FLUID_TANK_CAPACITY, null, null),
               new ProcessContract("Offline", false, 0, 0, 0, "machine_runtime"),
               new SideConfigurationContract("Standard", List.of(), List.of(), List.of()),
               new UpgradeContract(5, 0, List.of()),
               savedSnapshot(null));
         }
         return new Snapshot(
            machine.getBlockPos(),
            machine.kind(),
            machine.kind().displayName(),
            machine.statusLabel(),
            machine.getFluxStored(),
            machine.getMaxFluxStored(),
            machine.heatLevel(),
            machine.progressTicks(),
            machine.maxProgressTicks(),
            machine.inputFluidId(),
            machine.inputFluidAmount(),
            machine.outputFluidId(),
            machine.outputFluidAmount(),
            IndustrialMachineBlockEntity.fluidLabel(machine.inputFluidId()),
            IndustrialMachineBlockEntity.fluidLabel(machine.outputFluidId()),
            machine.scrubberModeName(),
            IndustrialMachineBlockEntity.sideConfigLabel(machine.sideConfigId()),
            machine.remoteShutdown(),
            machine.alertCountForTelemetry(),
            inventory(machine),
            energy(machine),
            fluids(machine),
            process(machine),
            side(machine),
            upgrades(machine),
            savedSnapshot(machine)
         );
      }

      private static String clean(String value, String fallback) {
         String cleaned = value == null ? "" : value.strip();
         return cleaned.isBlank() ? fallback : cleaned;
      }

      private static InventoryContract inventory(IndustrialMachineBlockEntity machine) {
         List<SlotSnapshot> slots = new ArrayList<>();
         int occupied = 0;
         for (int index = 0; index < IndustrialMachineBlockEntity.SLOT_COUNT; index++) {
            SlotSnapshot slot = slot(machine, index);
            if (slot.occupied()) {
               occupied++;
            }
            slots.add(slot);
         }
         return new InventoryContract(IndustrialMachineBlockEntity.SLOT_COUNT, occupied, slots);
      }

      private static EnergyContract energy(IndustrialMachineBlockEntity machine) {
         if (machine == null) {
            return new EnergyContract("echoindustrialnexus:thermal_flux", "TF", 0, 0, false, false);
         }
         return new EnergyContract("echoindustrialnexus:thermal_flux", "TF", machine.getFluxStored(),
            machine.getMaxFluxStored(), machine.canReceive(), machine.canExtract());
      }

      private static FluidContract fluids(IndustrialMachineBlockEntity machine) {
         if (machine == null) {
            return new FluidContract(false, IndustrialMachineBlockEntity.FLUID_TANK_CAPACITY, null, null);
         }
         return new FluidContract(
            machine.kind().usesFluidHandling(),
            IndustrialMachineBlockEntity.FLUID_TANK_CAPACITY,
            new FluidTankSnapshot("input", machine.inputFluidId(),
               IndustrialMachineBlockEntity.fluidLabel(machine.inputFluidId()), machine.inputFluidAmount(),
               IndustrialMachineBlockEntity.FLUID_TANK_CAPACITY),
            new FluidTankSnapshot("output", machine.outputFluidId(),
               IndustrialMachineBlockEntity.fluidLabel(machine.outputFluidId()), machine.outputFluidAmount(),
               IndustrialMachineBlockEntity.FLUID_TANK_CAPACITY)
         );
      }

      private static ProcessContract process(IndustrialMachineBlockEntity machine) {
         if (machine == null) {
            return new ProcessContract("Offline", false, 0, 0, 0, "machine_runtime");
         }
         int progress = machine.progressTicks();
         int maxProgress = machine.maxProgressTicks();
         int percent = maxProgress <= 0 ? 0 : Math.min(100, progress * 100 / maxProgress);
         return new ProcessContract(machine.statusLabel(), active(machine.machineStatus()), progress, maxProgress,
            percent, recipeContract(machine.kind()));
      }

      private static SideConfigurationContract side(IndustrialMachineBlockEntity machine) {
         if (machine == null) {
            return new SideConfigurationContract("Standard", List.of(), List.of(), List.of());
         }
         return new SideConfigurationContract(
            IndustrialMachineBlockEntity.sideConfigLabel(machine.sideConfigId()),
            slotsForFace(machine, Direction.UP),
            slotsForFace(machine, Direction.DOWN),
            slotsForFace(machine, Direction.NORTH)
         );
      }

      private static UpgradeContract upgrades(IndustrialMachineBlockEntity machine) {
         if (machine == null) {
            return new UpgradeContract(5, 0, List.of());
         }
         List<SlotSnapshot> slots = new ArrayList<>();
         int installed = 0;
         for (int index = IndustrialMachineBlockEntity.UPGRADE_SLOT_START; index <= IndustrialMachineBlockEntity.UPGRADE_SLOT_END; index++) {
            SlotSnapshot slot = slot(machine, index);
            if (slot.occupied()) {
               installed++;
            }
            slots.add(slot);
         }
         return new UpgradeContract(slots.size(), installed, slots);
      }

      private static SavedMachineSnapshot savedSnapshot(IndustrialMachineBlockEntity machine) {
         List<String> keys = List.of(
            "container_items",
            "thermal_flux",
            "progress",
            "max_progress",
            "heat",
            "burn_time",
            "scrubber_mode",
            "side_config",
            "remote_shutdown",
            "meltdown_cooldown",
            "input_fluid_id",
            "input_fluid_amount",
            "output_fluid_id",
            "output_fluid_amount",
            "linked_count",
            "controller_alerts",
            "status"
         );
         if (machine == null) {
            return new SavedMachineSnapshot("block_entity_value_output", keys, IndustrialMachineBlockEntity.SLOT_COUNT,
               0, 0, "Offline", "Standard", 0, 0, false);
         }
         return new SavedMachineSnapshot("block_entity_value_output", keys, machine.getContainerSize(),
            machine.getFluxStored(), machine.heatLevel(), machine.statusLabel(),
            IndustrialMachineBlockEntity.sideConfigLabel(machine.sideConfigId()), machine.inputFluidAmount(),
            machine.outputFluidAmount(), machine.remoteShutdown());
      }

      private static SlotSnapshot slot(IndustrialMachineBlockEntity machine, int index) {
         ItemStack stack = machine == null ? ItemStack.EMPTY : machine.getItem(index);
         Identifier itemId = stack.isEmpty() ? Identifier.withDefaultNamespace("air") : BuiltInRegistries.ITEM.getKey(stack.getItem());
         return new SlotSnapshot(index, role(index), itemId == null ? "minecraft:air" : itemId.toString(),
            stack.isEmpty() ? "Empty" : stack.getHoverName().getString(), stack.getCount(), !stack.isEmpty());
      }

      private static List<String> slotsForFace(IndustrialMachineBlockEntity machine, Direction direction) {
         List<String> slots = new ArrayList<>();
         for (int slot : machine.getSlotsForFace(direction)) {
            slots.add(slot + ":" + role(slot));
         }
         return List.copyOf(slots);
      }

      private static String role(int index) {
         return switch (index) {
            case IndustrialMachineBlockEntity.INPUT_SLOT -> "input";
            case IndustrialMachineBlockEntity.OUTPUT_SLOT -> "output";
            case IndustrialMachineBlockEntity.BYPRODUCT_SLOT -> "byproduct";
            case IndustrialMachineBlockEntity.AUX_SLOT -> "auxiliary";
            case IndustrialMachineBlockEntity.UPGRADE_SLOT_START, 5, 6, 7, 8 -> "upgrade";
            default -> "internal";
         };
      }

      private static boolean active(IndustrialMachineBlockEntity.MachineStatus status) {
         return switch (status == null ? IndustrialMachineBlockEntity.MachineStatus.IDLE : status) {
            case GENERATING, PROCESSING, HOT_PROCESSING, SCRUBBING, CONTROLLING -> true;
            default -> false;
         };
      }

      private static String recipeContract(IndustrialMachineBlock.MachineKind kind) {
         IndustrialMachineBlock.MachineKind safeKind = kind == null ? IndustrialMachineBlock.MachineKind.ORE_GRINDER : kind;
         if (safeKind.recipeDriven()) {
            return "echoindustrialnexus:industrial_processing";
         }
         if (safeKind.generator()) {
            return "echoindustrialnexus:thermal_flux_generation";
         }
         if (safeKind.factoryController()) {
            return "echoindustrialnexus:factory_controller";
         }
         if (safeKind.storesFlux()) {
            return "echoindustrialnexus:thermal_flux_storage";
         }
         return "echoindustrialnexus:machine_runtime";
      }
   }
}
