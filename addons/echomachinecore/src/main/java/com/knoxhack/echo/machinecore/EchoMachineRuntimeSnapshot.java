package com.knoxhack.echo.machinecore;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoMachineRuntimeSnapshot(
        EchoMachineId id,
        EchoModuleId ownerModule,
        String machineBlockId,
        EchoMachineKind kind,
        EchoMachineState state,
        String displayName,
        List<EchoMachineFailureState> failureStates,
        InventoryContract inventory,
        EnergyContract energy,
        FluidContract fluids,
        ProcessContract process,
        SideConfigurationContract side,
        UpgradeContract upgrades,
        SavedStateContract savedState,
        EchoMachineIntegrationRefs integrationRefs,
        Map<String, String> attributes
) {
    public EchoMachineRuntimeSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerModule, "ownerModule");
        machineBlockId = MachineContractGuards.normalizedId(machineBlockId, "machine block id");
        kind = kind == null ? EchoMachineKind.UNKNOWN : kind;
        state = state == null ? EchoMachineState.UNKNOWN : state;
        displayName = fallback(displayName, id.value());
        failureStates = MachineContractGuards.immutableList(failureStates);
        inventory = inventory == null ? InventoryContract.empty() : inventory;
        energy = energy == null ? EnergyContract.empty() : energy;
        fluids = fluids == null ? FluidContract.empty() : fluids;
        process = process == null ? ProcessContract.empty() : process;
        side = side == null ? SideConfigurationContract.empty() : side;
        upgrades = upgrades == null ? UpgradeContract.empty() : upgrades;
        savedState = savedState == null ? SavedStateContract.empty() : savedState;
        attributes = MachineContractGuards.immutableMap(attributes);
    }

    public boolean degraded() {
        return state.degraded() || failureStates.stream().anyMatch(EchoMachineFailureState::blocking);
    }

    private static String fallback(String value, String fallback) {
        String cleaned = MachineContractGuards.optionalText(value);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    public record SlotSnapshot(
            int index,
            String role,
            String itemId,
            String itemName,
            int count,
            boolean occupied,
            Map<String, String> attributes
    ) {
        public SlotSnapshot {
            index = MachineContractGuards.nonNegative(index, "slot index");
            role = fallback(role, "internal");
            itemId = fallback(itemId, "minecraft:air");
            itemName = fallback(itemName, "Empty");
            count = MachineContractGuards.nonNegative(count, "slot count");
            attributes = MachineContractGuards.immutableMap(attributes);
        }
    }

    public record InventoryContract(
            int totalSlots,
            int occupiedSlots,
            List<SlotSnapshot> slots,
            Map<String, String> attributes
    ) {
        public InventoryContract {
            totalSlots = MachineContractGuards.nonNegative(totalSlots, "inventory total slots");
            slots = MachineContractGuards.immutableList(slots);
            occupiedSlots = Math.max(0, Math.min(occupiedSlots, slots.size()));
            attributes = MachineContractGuards.immutableMap(attributes);
        }

        public static InventoryContract empty() {
            return new InventoryContract(0, 0, List.of(), Map.of());
        }
    }

    public record EnergyContract(
            String resourceId,
            String unit,
            int stored,
            int capacity,
            boolean canReceive,
            boolean canExtract,
            Map<String, String> attributes
    ) {
        public EnergyContract {
            resourceId = fallback(resourceId, "echo:energy");
            unit = fallback(unit, "EU");
            stored = MachineContractGuards.nonNegative(stored, "energy stored");
            capacity = MachineContractGuards.nonNegative(capacity, "energy capacity");
            attributes = MachineContractGuards.immutableMap(attributes);
        }

        public static EnergyContract empty() {
            return new EnergyContract("echo:energy", "EU", 0, 0, false, false, Map.of());
        }
    }

    public record FluidTankSnapshot(
            String role,
            int fluidId,
            String fluidName,
            int amount,
            int capacity,
            Map<String, String> attributes
    ) {
        public FluidTankSnapshot {
            role = fallback(role, "tank");
            fluidId = MachineContractGuards.nonNegative(fluidId, "fluid id");
            fluidName = fallback(fluidName, "Empty");
            amount = MachineContractGuards.nonNegative(amount, "fluid amount");
            capacity = MachineContractGuards.nonNegative(capacity, "fluid capacity");
            attributes = MachineContractGuards.immutableMap(attributes);
        }
    }

    public record FluidContract(
            boolean supported,
            int tankCapacity,
            FluidTankSnapshot input,
            FluidTankSnapshot output,
            Map<String, String> attributes
    ) {
        public FluidContract {
            tankCapacity = MachineContractGuards.nonNegative(tankCapacity, "fluid tank capacity");
            input = input == null ? new FluidTankSnapshot("input", 0, "Empty", 0, tankCapacity, Map.of()) : input;
            output = output == null ? new FluidTankSnapshot("output", 0, "Empty", 0, tankCapacity, Map.of()) : output;
            attributes = MachineContractGuards.immutableMap(attributes);
        }

        public static FluidContract empty() {
            return new FluidContract(false, 0, null, null, Map.of());
        }
    }

    public record ProcessContract(
            String status,
            boolean active,
            int progressTicks,
            int maxProgressTicks,
            int progressPercent,
            String recipeContract,
            Map<String, String> attributes
    ) {
        public ProcessContract {
            status = fallback(status, "Idle");
            progressTicks = MachineContractGuards.nonNegative(progressTicks, "process progress ticks");
            maxProgressTicks = MachineContractGuards.nonNegative(maxProgressTicks, "process max progress ticks");
            progressPercent = Math.max(0, Math.min(100, progressPercent));
            recipeContract = fallback(recipeContract, "machine_runtime");
            attributes = MachineContractGuards.immutableMap(attributes);
        }

        public static ProcessContract empty() {
            return new ProcessContract("Idle", false, 0, 0, 0, "machine_runtime", Map.of());
        }
    }

    public record SideConfigurationContract(
            String label,
            List<String> upSlots,
            List<String> downSlots,
            List<String> sideSlots,
            Map<String, String> attributes
    ) {
        public SideConfigurationContract {
            label = fallback(label, "Standard");
            upSlots = MachineContractGuards.immutableList(upSlots);
            downSlots = MachineContractGuards.immutableList(downSlots);
            sideSlots = MachineContractGuards.immutableList(sideSlots);
            attributes = MachineContractGuards.immutableMap(attributes);
        }

        public static SideConfigurationContract empty() {
            return new SideConfigurationContract("Standard", List.of(), List.of(), List.of(), Map.of());
        }
    }

    public record UpgradeContract(
            int capacity,
            int installedCount,
            List<SlotSnapshot> slots,
            Map<String, String> attributes
    ) {
        public UpgradeContract {
            capacity = MachineContractGuards.nonNegative(capacity, "upgrade capacity");
            slots = MachineContractGuards.immutableList(slots);
            installedCount = Math.max(0, Math.min(installedCount, slots.size()));
            attributes = MachineContractGuards.immutableMap(attributes);
        }

        public static UpgradeContract empty() {
            return new UpgradeContract(0, 0, List.of(), Map.of());
        }
    }

    public record SavedStateContract(
            String format,
            List<String> persistedKeys,
            int slotCount,
            int energyStored,
            int heat,
            String status,
            String sideConfig,
            boolean remoteShutdown,
            Map<String, String> attributes
    ) {
        public SavedStateContract {
            format = fallback(format, "unknown");
            persistedKeys = MachineContractGuards.immutableList(persistedKeys);
            slotCount = MachineContractGuards.nonNegative(slotCount, "saved slot count");
            energyStored = MachineContractGuards.nonNegative(energyStored, "saved energy stored");
            heat = Math.max(0, Math.min(100, heat));
            status = fallback(status, "Idle");
            sideConfig = fallback(sideConfig, "Standard");
            attributes = MachineContractGuards.immutableMap(attributes);
        }

        public static SavedStateContract empty() {
            return new SavedStateContract("unknown", List.of(), 0, 0, 0, "Idle", "Standard", false, Map.of());
        }
    }
}
