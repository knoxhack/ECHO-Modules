package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echo.machinecore.EchoMachineAutomationHook;
import com.knoxhack.echo.machinecore.EchoMachineAutomationHookKind;
import com.knoxhack.echo.machinecore.EchoMachineFailureKind;
import com.knoxhack.echo.machinecore.EchoMachineFailureState;
import com.knoxhack.echo.machinecore.EchoMachineId;
import com.knoxhack.echo.machinecore.EchoMachineIntegrationRefs;
import com.knoxhack.echo.machinecore.EchoMachineKind;
import com.knoxhack.echo.machinecore.EchoMachineMaintenanceProfile;
import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRecipeBinding;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineState;
import com.knoxhack.echo.machinecore.EchoMachineUpgradeSlot;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.recipecore.EchoRecipeId;
import com.knoxhack.echoindustrialnexus.api.IndustrialMachineTelemetryView;
import com.knoxhack.echoindustrialnexus.block.IndustrialMachineBlock;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapts live Industrial machines into MachineCore's neutral contracts.
 */
public final class IndustrialMachineCoreAdapter {
   private static final EchoModuleId OWNER = EchoModuleId.of("echoindustrialnexus");

   private IndustrialMachineCoreAdapter() {
   }

   public static EchoMachineRuntimeSnapshot runtimeSnapshot(IndustrialMachineBlockEntity machine) {
      IndustrialMachineTelemetryView telemetry = IndustrialMachineTelemetryView.from(machine);
      EchoMachineId id = machineId(telemetry.kind());
      return new EchoMachineRuntimeSnapshot(
         id,
         OWNER,
         id.value(),
         machineKind(telemetry.kind()),
         machineState(machine == null ? IndustrialMachineBlockEntity.MachineStatus.IDLE : machine.machineStatus()),
         telemetry.displayName(),
         failureStates(machine == null ? IndustrialMachineBlockEntity.MachineStatus.IDLE : machine.machineStatus()),
         inventory(telemetry.inventory()),
         energy(telemetry.energy()),
         fluids(telemetry.fluids()),
         process(telemetry.process()),
         side(telemetry.side()),
         upgrades(telemetry.upgrades()),
         savedState(telemetry.savedSnapshot()),
         integrationRefs(telemetry.kind()),
         runtimeAttributes(machine, telemetry)
      );
   }

   public static EchoMachineProfile profile(IndustrialMachineBlockEntity machine) {
      IndustrialMachineTelemetryView telemetry = IndustrialMachineTelemetryView.from(machine);
      IndustrialMachineBlock.MachineKind kind = telemetry.kind();
      EchoMachineId id = machineId(kind);
      return new EchoMachineProfile(
         id,
         machineKind(kind),
         machineState(machine == null ? IndustrialMachineBlockEntity.MachineStatus.IDLE : machine.machineStatus()),
         OWNER,
         null,
         List.of(recipeBinding(telemetry.process().recipeContract(), kind)),
         upgradeSlots(telemetry.upgrades()),
         new EchoMachineMaintenanceProfile(0.0D, 0, List.of("industrial_service"), true,
            Map.of("heat", Integer.toString(telemetry.heat()))),
         failureStates(machine == null ? IndustrialMachineBlockEntity.MachineStatus.IDLE : machine.machineStatus()),
         automationHooks(telemetry),
         integrationRefs(kind),
         List.of(),
         Map.of(
            "source", "IndustrialMachineCoreAdapter",
            "industrialKind", kind.getSerializedName(),
            "displayName", telemetry.displayName()
         )
      );
   }

   private static EchoMachineId machineId(IndustrialMachineBlock.MachineKind kind) {
      IndustrialMachineBlock.MachineKind safeKind = kind == null ? IndustrialMachineBlock.MachineKind.ORE_GRINDER : kind;
      return EchoMachineId.of("echoindustrialnexus:" + safeKind.getSerializedName());
   }

   private static Map<String, String> runtimeAttributes(IndustrialMachineBlockEntity machine, IndustrialMachineTelemetryView telemetry) {
      Map<String, String> attributes = new LinkedHashMap<>();
      attributes.put("source", "IndustrialMachineTelemetryView");
      attributes.put("industrialKind", telemetry.kind().getSerializedName());
      attributes.put("statusLabel", telemetry.status());
      if (machine != null) {
         attributes.put("position", machine.getBlockPos().getX() + "," + machine.getBlockPos().getY() + "," + machine.getBlockPos().getZ());
         if (machine.getLevel() != null) {
            attributes.put("dimension", machine.getLevel().dimension().identifier().toString());
         }
      }
      return Map.copyOf(attributes);
   }

   private static EchoMachineKind machineKind(IndustrialMachineBlock.MachineKind kind) {
      IndustrialMachineBlock.MachineKind safeKind = kind == null ? IndustrialMachineBlock.MachineKind.ORE_GRINDER : kind;
      if (safeKind.factoryController()) {
         return EchoMachineKind.AUTOMATION_NODE;
      }
      if (safeKind.storesFlux()) {
         return EchoMachineKind.POWERED_STATION;
      }
      if (safeKind.generator()) {
         return EchoMachineKind.POWERED_STATION;
      }
      return switch (safeKind) {
         case ALLOY_KILN, REALITY_FURNACE -> EchoMachineKind.FABRICATOR;
         case COMPONENT_ASSEMBLER -> EchoMachineKind.ASSEMBLER;
         case FLUID_REFINER, WATER_PURIFIER, FILTER_PRESS, NEXUS_THERMAL_INJECTOR, INDUSTRIAL_SCRUBBER -> EchoMachineKind.REFINERY;
         default -> EchoMachineKind.SINGLE_BLOCK;
      };
   }

   private static EchoMachineState machineState(IndustrialMachineBlockEntity.MachineStatus status) {
      return switch (status == null ? IndustrialMachineBlockEntity.MachineStatus.IDLE : status) {
         case GENERATING, PROCESSING, HOT_PROCESSING, SCRUBBING, CONTROLLING -> EchoMachineState.ACTIVE;
         case CHARGING -> EchoMachineState.POWER_STARVED;
         case OUTPUT_BLOCKED, FLUID_OUTPUT_BLOCKED -> EchoMachineState.PAUSED;
         case BAD_INPUT, CATALYST_REQUIRED, FLUID_REQUIRED -> EchoMachineState.PAUSED;
         case CRITICAL_HEAT, MELTDOWN, NEXUS_CONTAMINATION -> EchoMachineState.OVERLOADED;
         case EMERGENCY_SHUTDOWN, REMOTE_SHUTDOWN -> EchoMachineState.OFFLINE;
         case COMPLETE, STORED, IDLE -> EchoMachineState.IDLE;
      };
   }

   private static List<EchoMachineFailureState> failureStates(IndustrialMachineBlockEntity.MachineStatus status) {
      IndustrialMachineBlockEntity.MachineStatus safeStatus = status == null ? IndustrialMachineBlockEntity.MachineStatus.IDLE : status;
      EchoMachineFailureKind kind = switch (safeStatus) {
         case CHARGING -> EchoMachineFailureKind.POWER_LOSS;
         case OUTPUT_BLOCKED, FLUID_OUTPUT_BLOCKED -> EchoMachineFailureKind.OUTPUT_BLOCKED;
         case BAD_INPUT, CATALYST_REQUIRED, FLUID_REQUIRED -> EchoMachineFailureKind.INPUT_MISSING;
         case CRITICAL_HEAT, MELTDOWN, NEXUS_CONTAMINATION -> EchoMachineFailureKind.OVERHEAT;
         case EMERGENCY_SHUTDOWN, REMOTE_SHUTDOWN -> EchoMachineFailureKind.AUTOMATION_BLOCKED;
         default -> EchoMachineFailureKind.NONE;
      };
      double severity = kind == EchoMachineFailureKind.NONE ? 0.0D : 1.0D;
      return List.of(new EchoMachineFailureState(kind, severity, safeStatus.label(), safeStatus.name(), List.of(),
         Map.of("industrialStatus", safeStatus.name())));
   }

   private static EchoMachineRuntimeSnapshot.InventoryContract inventory(IndustrialMachineTelemetryView.InventoryContract inventory) {
      List<EchoMachineRuntimeSnapshot.SlotSnapshot> slots = inventory.slots().stream()
         .map(IndustrialMachineCoreAdapter::slot)
         .toList();
      return new EchoMachineRuntimeSnapshot.InventoryContract(inventory.totalSlots(), inventory.occupiedSlots(), slots,
         Map.of("contract", "industrial_inventory"));
   }

   private static EchoMachineRuntimeSnapshot.SlotSnapshot slot(IndustrialMachineTelemetryView.SlotSnapshot slot) {
      return new EchoMachineRuntimeSnapshot.SlotSnapshot(slot.index(), slot.role(), slot.itemId(), slot.itemName(),
         slot.count(), slot.occupied(), Map.of("sourceRole", slot.role()));
   }

   private static EchoMachineRuntimeSnapshot.EnergyContract energy(IndustrialMachineTelemetryView.EnergyContract energy) {
      return new EchoMachineRuntimeSnapshot.EnergyContract(energy.resourceId(), energy.unit(), energy.stored(),
         energy.capacity(), energy.canReceive(), energy.canExtract(), Map.of("contract", "thermal_flux"));
   }

   private static EchoMachineRuntimeSnapshot.FluidContract fluids(IndustrialMachineTelemetryView.FluidContract fluids) {
      return new EchoMachineRuntimeSnapshot.FluidContract(fluids.supported(), fluids.tankCapacity(),
         fluidTank(fluids.input()), fluidTank(fluids.output()), Map.of("contract", "industrial_fluid_tanks"));
   }

   private static EchoMachineRuntimeSnapshot.FluidTankSnapshot fluidTank(IndustrialMachineTelemetryView.FluidTankSnapshot tank) {
      return new EchoMachineRuntimeSnapshot.FluidTankSnapshot(tank.role(), tank.fluidId(), tank.fluidName(),
         tank.amount(), tank.capacity(), Map.of());
   }

   private static EchoMachineRuntimeSnapshot.ProcessContract process(IndustrialMachineTelemetryView.ProcessContract process) {
      return new EchoMachineRuntimeSnapshot.ProcessContract(process.status(), process.active(), process.progressTicks(),
         process.maxProgressTicks(), process.progressPercent(), process.recipeContract(),
         Map.of("contract", "industrial_process"));
   }

   private static EchoMachineRuntimeSnapshot.SideConfigurationContract side(IndustrialMachineTelemetryView.SideConfigurationContract side) {
      return new EchoMachineRuntimeSnapshot.SideConfigurationContract(side.label(), side.upSlots(), side.downSlots(),
         side.sideSlots(), Map.of("contract", "industrial_side_config"));
   }

   private static EchoMachineRuntimeSnapshot.UpgradeContract upgrades(IndustrialMachineTelemetryView.UpgradeContract upgrades) {
      List<EchoMachineRuntimeSnapshot.SlotSnapshot> slots = upgrades.slots().stream()
         .map(IndustrialMachineCoreAdapter::slot)
         .toList();
      return new EchoMachineRuntimeSnapshot.UpgradeContract(upgrades.capacity(), upgrades.installedCount(), slots,
         Map.of("contract", "industrial_upgrade_slots"));
   }

   private static EchoMachineRuntimeSnapshot.SavedStateContract savedState(IndustrialMachineTelemetryView.SavedMachineSnapshot saved) {
      return new EchoMachineRuntimeSnapshot.SavedStateContract(saved.format(), saved.persistedKeys(), saved.slotCount(),
         saved.fluxStored(), saved.heat(), saved.status(), saved.sideConfig(), saved.remoteShutdown(),
         Map.of(
            "inputFluidAmount", Integer.toString(saved.inputFluidAmount()),
            "outputFluidAmount", Integer.toString(saved.outputFluidAmount())
         ));
   }

   private static EchoMachineRecipeBinding recipeBinding(String recipeContract, IndustrialMachineBlock.MachineKind kind) {
      return new EchoMachineRecipeBinding(EchoRecipeId.of(recipeContract), null, "primary", 0, List.of(),
         Map.of("industrialKind", kind.getSerializedName()));
   }

   private static List<EchoMachineUpgradeSlot> upgradeSlots(IndustrialMachineTelemetryView.UpgradeContract upgrades) {
      List<EchoMachineUpgradeSlot> slots = new ArrayList<>();
      for (IndustrialMachineTelemetryView.SlotSnapshot slot : upgrades.slots()) {
         slots.add(new EchoMachineUpgradeSlot("upgrade_" + slot.index(), "industrial", 1, true, null,
            Map.of("sourceSlot", Integer.toString(slot.index()), "occupied", Boolean.toString(slot.occupied()))));
      }
      return List.copyOf(slots);
   }

   private static List<EchoMachineAutomationHook> automationHooks(IndustrialMachineTelemetryView telemetry) {
      List<EchoMachineAutomationHook> hooks = new ArrayList<>();
      hooks.add(hook("item_input", EchoMachineAutomationHookKind.ITEM_INPUT, "top"));
      hooks.add(hook("item_output", EchoMachineAutomationHookKind.ITEM_OUTPUT, "bottom"));
      hooks.add(hook("power_input", EchoMachineAutomationHookKind.POWER_INPUT, telemetry.energy().resourceId()));
      hooks.add(hook("remote_status", EchoMachineAutomationHookKind.REMOTE_STATUS, telemetry.status()));
      if (telemetry.fluids().supported()) {
         hooks.add(hook("fluid_input", EchoMachineAutomationHookKind.FLUID_INPUT, telemetry.fluids().input().fluidName()));
         hooks.add(hook("fluid_output", EchoMachineAutomationHookKind.FLUID_OUTPUT, telemetry.fluids().output().fluidName()));
      }
      return List.copyOf(hooks);
   }

   private static EchoMachineAutomationHook hook(String id, EchoMachineAutomationHookKind kind, String detail) {
      return new EchoMachineAutomationHook(id, kind, null,
         List.of(EchoFeatureId.of("echologisticsnetwork.external_endpoints")), true,
         Map.of("detail", detail == null ? "" : detail));
   }

   private static EchoMachineIntegrationRefs integrationRefs(IndustrialMachineBlock.MachineKind kind) {
      return new EchoMachineIntegrationRefs(null, null, null,
         List.of(
            EchoFeatureId.of("echolens.deep_scan"),
            EchoFeatureId.of("echoterminal.recipe_provider"),
            EchoFeatureId.of("echologisticsnetwork.external_endpoints"),
            EchoFeatureId.of("echopowergrid.thermal_flux_bridge")
         ),
         List.of(),
         Map.of("industrialKind", (kind == null ? IndustrialMachineBlock.MachineKind.ORE_GRINDER : kind).getSerializedName())
      );
   }
}
