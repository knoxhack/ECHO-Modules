package com.knoxhack.echopowergrid.integration;

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
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.recipecore.EchoRecipeId;
import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.api.EchoEnergyStorage;
import com.knoxhack.echopowergrid.api.EchoGridState;
import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.api.GeneratorType;
import com.knoxhack.echopowergrid.api.PowerGridSnapshot;
import com.knoxhack.echopowergrid.block.entity.BatteryBlockEntity;
import com.knoxhack.echopowergrid.block.entity.GeneratorBlockEntity;
import com.knoxhack.echopowergrid.block.entity.PowerConsumerBlockEntity;
import com.knoxhack.echopowergrid.block.entity.SubstationBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PowerGridMachineCoreAdapter {
    private static final EchoModuleId OWNER = EchoModuleId.of(EchoPowerGrid.MODID);

    private PowerGridMachineCoreAdapter() {
    }

    public static boolean supports(BlockEntity blockEntity) {
        return blockEntity instanceof GeneratorBlockEntity
                || blockEntity instanceof BatteryBlockEntity
                || blockEntity instanceof SubstationBlockEntity
                || blockEntity instanceof PowerConsumerBlockEntity;
    }

    public static EchoMachineRuntimeSnapshot runtimeSnapshot(BlockEntity machine) {
        Identifier blockId = blockId(machine);
        EchoMachineId machineId = EchoMachineId.of(blockId.toString());
        return new EchoMachineRuntimeSnapshot(
                machineId,
                OWNER,
                blockId.toString(),
                machineKind(machine),
                machineState(machine),
                title(blockId.getPath()),
                failureStates(machine),
                inventory(machine),
                energy(machine),
                EchoMachineRuntimeSnapshot.FluidContract.empty(),
                process(machine),
                side(machine),
                EchoMachineRuntimeSnapshot.UpgradeContract.empty(),
                savedState(machine),
                integrationRefs(machine),
                runtimeAttributes(machine)
        );
    }

    public static EchoMachineProfile profile(BlockEntity machine) {
        Identifier blockId = blockId(machine);
        EchoMachineId machineId = EchoMachineId.of(blockId.toString());
        return new EchoMachineProfile(
                machineId,
                machineKind(machine),
                machineState(machine),
                OWNER,
                null,
                List.of(recipeBinding(machine)),
                List.of(),
                new EchoMachineMaintenanceProfile(
                        maintenanceWear(machine),
                        20 * 60 * 12,
                        List.of("powergrid", family(machine), "field_service"),
                        true,
                        Map.of(
                                "source", "PowerGridMachineCoreAdapter",
                                "family", family(machine),
                                "storedEnergy", Integer.toString(energyStored(machine)))),
                failureStates(machine),
                automationHooks(machine),
                integrationRefs(machine),
                List.of(),
                Map.of(
                        "source", "PowerGridMachineCoreAdapter",
                        "family", family(machine),
                        "displayName", title(blockId.getPath()))
        );
    }

    private static Identifier blockId(BlockEntity machine) {
        if (machine == null) {
            return EchoPowerGrid.id("unknown_power_node");
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(machine.getBlockState().getBlock());
        return id == null ? EchoPowerGrid.id("unknown_power_node") : id;
    }

    private static EchoMachineKind machineKind(BlockEntity machine) {
        if (machine instanceof GeneratorBlockEntity || machine instanceof BatteryBlockEntity) {
            return EchoMachineKind.POWERED_STATION;
        }
        if (machine instanceof SubstationBlockEntity || machine instanceof PowerConsumerBlockEntity) {
            return EchoMachineKind.AUTOMATION_NODE;
        }
        return EchoMachineKind.SINGLE_BLOCK;
    }

    private static EchoMachineState machineState(BlockEntity machine) {
        if (machine instanceof GeneratorBlockEntity generator) {
            if (generator.isOverloaded()) {
                return EchoMachineState.OVERLOADED;
            }
            if (generator.getGeneratorType() == GeneratorType.CREATIVE) {
                return EchoMachineState.ACTIVE;
            }
            if (generator.getGeneratorType() == GeneratorType.FUEL_BURNER) {
                return generator.getBurnTime() > 0 ? EchoMachineState.ACTIVE : EchoMachineState.IDLE;
            }
            if (generator.getGenerationPerTick() > 0 || generator.getEnergyStored() > 0) {
                return EchoMachineState.ACTIVE;
            }
            return EchoMachineState.IDLE;
        }
        if (machine instanceof BatteryBlockEntity battery) {
            return battery.getEnergyStored() > 0 ? EchoMachineState.ACTIVE : EchoMachineState.IDLE;
        }
        if (machine instanceof PowerConsumerBlockEntity consumer) {
            if (consumer.isOverloaded()) {
                return EchoMachineState.OVERLOADED;
            }
            return consumer.isOnline() ? EchoMachineState.ACTIVE : EchoMachineState.POWER_STARVED;
        }
        if (machine instanceof SubstationBlockEntity substation) {
            PowerGridSnapshot snapshot = networkSnapshot(substation);
            return switch (snapshot.state()) {
                case OVERLOADED, EMERGENCY -> EchoMachineState.OVERLOADED;
                case BROWNOUT -> EchoMachineState.POWER_STARVED;
                case TRIPPED, OFFLINE -> EchoMachineState.OFFLINE;
                case STABLE, CHARGING, DISCHARGING -> EchoMachineState.ACTIVE;
            };
        }
        return EchoMachineState.UNKNOWN;
    }

    private static List<EchoMachineFailureState> failureStates(BlockEntity machine) {
        if (machine instanceof GeneratorBlockEntity generator
                && generator.getGeneratorType() == GeneratorType.FUEL_BURNER
                && generator.getBurnTime() <= 0
                && generator.fuelInventory().getItem(0).isEmpty()) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.INPUT_MISSING, 0.45D,
                    "Fuel burner is waiting for fuel", "fuel_missing", List.of(), Map.of()));
        }
        if (machine instanceof PowerConsumerBlockEntity consumer && !consumer.isOnline()) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.POWER_LOSS, 0.65D,
                    "Power consumer is below demand", "consumer_power_starved", List.of(),
                    Map.of("demandEpPerTick", Long.toString(consumer.getDemandPerTick()))));
        }
        if (machine instanceof SubstationBlockEntity substation) {
            PowerGridSnapshot snapshot = networkSnapshot(substation);
            if (snapshot.state() == EchoGridState.BROWNOUT || snapshot.state() == EchoGridState.OVERLOADED
                    || snapshot.state() == EchoGridState.TRIPPED || snapshot.state() == EchoGridState.EMERGENCY) {
                return List.of(new EchoMachineFailureState(EchoMachineFailureKind.POWER_LOSS, 0.7D,
                        "Power grid is unstable", "grid_" + snapshot.state().name().toLowerCase(Locale.ROOT),
                        List.of(), Map.of("networkState", snapshot.state().name())));
            }
        }
        return List.of(new EchoMachineFailureState(EchoMachineFailureKind.NONE, 0.0D,
                "PowerGrid node nominal", "nominal", List.of(), Map.of()));
    }

    private static EchoMachineRuntimeSnapshot.InventoryContract inventory(BlockEntity machine) {
        if (!(machine instanceof GeneratorBlockEntity generator) || !generator.usesFuel()) {
            return EchoMachineRuntimeSnapshot.InventoryContract.empty();
        }
        ItemStack fuel = generator.fuelInventory().getItem(0);
        EchoMachineRuntimeSnapshot.SlotSnapshot slot = slot(0, "fuel", fuel,
                Map.of("contract", "powergrid_fuel_slot"));
        return new EchoMachineRuntimeSnapshot.InventoryContract(1, slot.occupied() ? 1 : 0,
                List.of(slot), Map.of("contract", "powergrid_generator_fuel_inventory", "family", family(machine)));
    }

    private static EchoMachineRuntimeSnapshot.SlotSnapshot slot(
            int index,
            String role,
            ItemStack stack,
            Map<String, String> attributes
    ) {
        ItemStack safeStack = stack == null ? ItemStack.EMPTY : stack;
        String itemId = safeStack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(safeStack.getItem()).toString();
        String itemName = safeStack.isEmpty() ? "Empty" : safeStack.getHoverName().getString();
        return new EchoMachineRuntimeSnapshot.SlotSnapshot(index, role, itemId, itemName,
                safeStack.getCount(), !safeStack.isEmpty(), attributes);
    }

    private static EchoMachineRuntimeSnapshot.EnergyContract energy(BlockEntity machine) {
        if (machine instanceof EchoEnergyStorage storage) {
            Map<String, String> attributes = new LinkedHashMap<>();
            attributes.put("contract", "powergrid_ep_storage");
            attributes.put("family", family(machine));
            attributes.put("maxInputEpPerTick", Long.toString(storage.getMaxInput()));
            attributes.put("maxOutputEpPerTick", Long.toString(storage.getMaxOutput()));
            if (machine instanceof GeneratorBlockEntity generator) {
                attributes.put("generationEpPerTick", Long.toString(generator.getGenerationPerTick()));
                attributes.put("generatorType", generator.getGeneratorType().name());
            }
            return new EchoMachineRuntimeSnapshot.EnergyContract(
                    EchoPowerGrid.id("ep").toString(),
                    "EP",
                    cappedInt(storage.getEnergyStored()),
                    cappedInt(storage.getMaxEnergyStored()),
                    storage.canReceive(),
                    storage.canExtract(),
                    Map.copyOf(attributes)
            );
        }
        if (machine instanceof PowerConsumerBlockEntity consumer) {
            return new EchoMachineRuntimeSnapshot.EnergyContract(
                    EchoPowerGrid.id("ep").toString(),
                    "EP",
                    cappedInt(consumer.getLastReceived()),
                    cappedInt(consumer.getDemandPerTick()),
                    true,
                    false,
                    Map.of(
                            "contract", "powergrid_ep_demand",
                            "demandEpPerTick", Long.toString(consumer.getDemandPerTick()),
                            "powered", Boolean.toString(consumer.isOnline()))
            );
        }
        return EchoMachineRuntimeSnapshot.EnergyContract.empty();
    }

    private static EchoMachineRuntimeSnapshot.ProcessContract process(BlockEntity machine) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("contract", "powergrid_machine_process");
        attributes.put("family", family(machine));
        if (machine instanceof GeneratorBlockEntity generator) {
            attributes.put("generatorType", generator.getGeneratorType().name());
            attributes.put("generationRate", Long.toString(generator.getGenerationRate()));
            attributes.put("burnTime", Integer.toString(generator.getBurnTime()));
            attributes.put("totalBurnTime", Integer.toString(generator.getTotalBurnTime()));
            attributes.put("crankCooldown", Integer.toString(generator.getCrankCooldown()));
            return new EchoMachineRuntimeSnapshot.ProcessContract(
                    generatorStatus(generator),
                    machineState(machine) == EchoMachineState.ACTIVE,
                    generator.getBurnTime(),
                    generator.getTotalBurnTime(),
                    percent(generator.getBurnTime(), generator.getTotalBurnTime()),
                    EchoPowerGrid.id(recipePath(machine)).toString(),
                    Map.copyOf(attributes)
            );
        }
        if (machine instanceof BatteryBlockEntity battery) {
            attributes.put("maxInputEpPerTick", Long.toString(battery.getMaxInput()));
            attributes.put("maxOutputEpPerTick", Long.toString(battery.getMaxOutput()));
            return new EchoMachineRuntimeSnapshot.ProcessContract(
                    "Battery buffer " + battery.getEnergyStored() + "/" + battery.getMaxEnergyStored() + " EP",
                    battery.getEnergyStored() > 0,
                    cappedInt(battery.getEnergyStored()),
                    cappedInt(battery.getMaxEnergyStored()),
                    percent(battery.getEnergyStored(), battery.getMaxEnergyStored()),
                    EchoPowerGrid.id(recipePath(machine)).toString(),
                    Map.copyOf(attributes)
            );
        }
        if (machine instanceof SubstationBlockEntity substation) {
            PowerGridSnapshot snapshot = networkSnapshot(substation);
            attributes.put("policy", substation.policy().name());
            attributes.put("networkGeneration", Long.toString(snapshot.totalGeneration()));
            attributes.put("networkDemand", Long.toString(snapshot.totalDemand()));
            attributes.put("networkState", snapshot.state().name());
            attributes.put("nodeCount", Integer.toString(snapshot.nodeCount()));
            return new EchoMachineRuntimeSnapshot.ProcessContract(
                    "Substation " + snapshot.state() + " policy " + substation.policy().name(),
                    snapshot.isPowered(),
                    cappedInt(snapshot.totalStored()),
                    cappedInt(snapshot.totalCapacity()),
                    percent(snapshot.totalStored(), snapshot.totalCapacity()),
                    EchoPowerGrid.id(recipePath(machine)).toString(),
                    Map.copyOf(attributes)
            );
        }
        if (machine instanceof PowerConsumerBlockEntity consumer) {
            attributes.put("demandEpPerTick", Long.toString(consumer.getDemandPerTick()));
            attributes.put("lastReceivedEp", Long.toString(consumer.getLastReceived()));
            return new EchoMachineRuntimeSnapshot.ProcessContract(
                    consumer.isOnline() ? "Demand satisfied" : "Waiting for power",
                    consumer.isOnline(),
                    cappedInt(consumer.getLastReceived()),
                    cappedInt(consumer.getDemandPerTick()),
                    percent(consumer.getLastReceived(), consumer.getDemandPerTick()),
                    EchoPowerGrid.id(recipePath(machine)).toString(),
                    Map.copyOf(attributes)
            );
        }
        return EchoMachineRuntimeSnapshot.ProcessContract.empty();
    }

    private static EchoMachineRuntimeSnapshot.SideConfigurationContract side(BlockEntity machine) {
        if (machine instanceof GeneratorBlockEntity generator && generator.usesFuel()) {
            return new EchoMachineRuntimeSnapshot.SideConfigurationContract(
                    "PowerGrid fuel in / EP out",
                    List.of("fuel"),
                    List.of("energy_output"),
                    List.of("fuel", "energy_output"),
                    Map.of("contract", "powergrid_generator_sides", "family", family(machine))
            );
        }
        if (machine instanceof BatteryBlockEntity) {
            return new EchoMachineRuntimeSnapshot.SideConfigurationContract(
                    "PowerGrid EP bidirectional",
                    List.of("energy_input"),
                    List.of("energy_output"),
                    List.of("energy_input", "energy_output"),
                    Map.of("contract", "powergrid_battery_sides", "family", family(machine))
            );
        }
        if (machine instanceof PowerConsumerBlockEntity) {
            return new EchoMachineRuntimeSnapshot.SideConfigurationContract(
                    "PowerGrid EP demand",
                    List.of("energy_input"),
                    List.of(),
                    List.of("energy_input"),
                    Map.of("contract", "powergrid_consumer_sides", "family", family(machine))
            );
        }
        if (machine instanceof SubstationBlockEntity) {
            return new EchoMachineRuntimeSnapshot.SideConfigurationContract(
                    "PowerGrid routing policy",
                    List.of("network_input"),
                    List.of("network_output"),
                    List.of("network_route"),
                    Map.of("contract", "powergrid_substation_sides", "family", family(machine))
            );
        }
        return EchoMachineRuntimeSnapshot.SideConfigurationContract.empty();
    }

    private static EchoMachineRuntimeSnapshot.SavedStateContract savedState(BlockEntity machine) {
        List<String> keys = new ArrayList<>();
        if (machine instanceof GeneratorBlockEntity) {
            keys.add("Energy");
            keys.add("BurnTime");
            keys.add("TotalBurnTime");
            keys.add("CrankCooldown");
            keys.add("GeneratorType");
            keys.add("FuelInventory");
        } else if (machine instanceof BatteryBlockEntity) {
            keys.add("Energy");
        } else if (machine instanceof SubstationBlockEntity) {
            keys.add("Policy");
        }
        return new EchoMachineRuntimeSnapshot.SavedStateContract(
                "powergrid_machine_v1",
                List.copyOf(keys),
                machine instanceof GeneratorBlockEntity generator && generator.usesFuel() ? 1 : 0,
                energyStored(machine),
                heat(machine),
                machineState(machine).serializedName(),
                side(machine).label(),
                false,
                Map.of("family", family(machine), "source", "PowerGridMachineCoreAdapter")
        );
    }

    private static EchoMachineRecipeBinding recipeBinding(BlockEntity machine) {
        return new EchoMachineRecipeBinding(
                EchoRecipeId.of(EchoPowerGrid.MODID + ":" + recipePath(machine)),
                null,
                family(machine),
                0,
                List.of(),
                Map.of("family", family(machine), "source", "PowerGridMachineCoreAdapter")
        );
    }

    private static List<EchoMachineAutomationHook> automationHooks(BlockEntity machine) {
        List<EchoMachineAutomationHook> hooks = new ArrayList<>();
        if (machine instanceof GeneratorBlockEntity generator && generator.usesFuel()) {
            hooks.add(hook("fuel_input", EchoMachineAutomationHookKind.ITEM_INPUT, "fuel"));
        }
        hooks.add(hook("power_input", EchoMachineAutomationHookKind.POWER_INPUT, family(machine)));
        hooks.add(hook("remote_status", EchoMachineAutomationHookKind.REMOTE_STATUS, family(machine)));
        return List.copyOf(hooks);
    }

    private static EchoMachineAutomationHook hook(String id, EchoMachineAutomationHookKind kind, String detail) {
        return new EchoMachineAutomationHook(id, kind, null,
                List.of(
                        EchoFeatureId.of("echolens.deep_scan"),
                        EchoFeatureId.of("echoterminal.recipe_provider"),
                        EchoFeatureId.of("echoindex.machine_entries"),
                        EchoFeatureId.of("echoholomap.machine_markers"),
                        EchoFeatureId.of("echopowergrid.network_summary")),
                true,
                Map.of("detail", detail));
    }

    private static EchoMachineIntegrationRefs integrationRefs(BlockEntity machine) {
        return new EchoMachineIntegrationRefs(null, null, null,
                List.of(
                        EchoFeatureId.of("echolens.deep_scan"),
                        EchoFeatureId.of("echoterminal.recipe_provider"),
                        EchoFeatureId.of("echoindex.machine_entries"),
                        EchoFeatureId.of("echoholomap.machine_markers"),
                        EchoFeatureId.of("echopowergrid.power_request"),
                        EchoFeatureId.of("echopowergrid.network_summary")),
                List.of(),
                Map.of("family", family(machine), "recipe", recipePath(machine))
        );
    }

    private static Map<String, String> runtimeAttributes(BlockEntity machine) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("source", "PowerGridMachineCoreAdapter");
        attributes.put("family", family(machine));
        if (machine != null) {
            attributes.put("position", machine.getBlockPos().getX() + "," + machine.getBlockPos().getY() + "," + machine.getBlockPos().getZ());
            if (machine.getLevel() != null) {
                attributes.put("dimension", machine.getLevel().dimension().identifier().toString());
                PowerGridSnapshot snapshot = networkSnapshot(machine);
                attributes.put("networkState", snapshot.state().name());
                attributes.put("networkNodeCount", Integer.toString(snapshot.nodeCount()));
            }
        }
        attributes.put("energyStored", Integer.toString(energyStored(machine)));
        attributes.put("blockId", blockId(machine).toString());
        return Map.copyOf(attributes);
    }

    private static PowerGridSnapshot networkSnapshot(BlockEntity machine) {
        if (machine == null || machine.getLevel() == null) {
            return EchoPowerGridApi.getSnapshot(null, null);
        }
        return EchoPowerGridApi.getSnapshot(machine.getLevel(), machine.getBlockPos());
    }

    private static int energyStored(BlockEntity machine) {
        if (machine instanceof EchoEnergyStorage storage) {
            return cappedInt(storage.getEnergyStored());
        }
        if (machine instanceof PowerConsumerBlockEntity consumer) {
            return cappedInt(consumer.getLastReceived());
        }
        if (machine instanceof SubstationBlockEntity) {
            return cappedInt(networkSnapshot(machine).totalStored());
        }
        return 0;
    }

    private static int heat(BlockEntity machine) {
        if (machine instanceof EchoEnergyStorage storage) {
            return percent(storage.getEnergyStored(), storage.getMaxEnergyStored());
        }
        if (machine instanceof PowerConsumerBlockEntity consumer) {
            return percent(consumer.getLastReceived(), consumer.getDemandPerTick());
        }
        if (machine instanceof SubstationBlockEntity) {
            PowerGridSnapshot snapshot = networkSnapshot(machine);
            return percent(snapshot.totalDemand(), Math.max(1L, snapshot.totalGeneration()));
        }
        return 0;
    }

    private static String family(BlockEntity machine) {
        if (machine instanceof GeneratorBlockEntity generator) {
            return switch (generator.getGeneratorType()) {
                case HAND_CRANK -> "hand_crank_generator";
                case FUEL_BURNER -> "fuel_generator";
                case SOLAR -> "solar_generator";
                case CREATIVE -> "creative_power_source";
            };
        }
        if (machine instanceof BatteryBlockEntity) {
            return "battery_bank";
        }
        if (machine instanceof SubstationBlockEntity) {
            return "substation";
        }
        if (machine instanceof PowerConsumerBlockEntity) {
            return "power_consumer";
        }
        return "powergrid_node";
    }

    private static String recipePath(BlockEntity machine) {
        if (machine instanceof GeneratorBlockEntity generator) {
            return switch (generator.getGeneratorType()) {
                case HAND_CRANK -> "hand_crank_generation";
                case FUEL_BURNER -> "fuel_generation";
                case SOLAR -> "solar_generation";
                case CREATIVE -> "creative_generation";
            };
        }
        if (machine instanceof BatteryBlockEntity) {
            return "battery_storage";
        }
        if (machine instanceof SubstationBlockEntity) {
            return "substation_routing";
        }
        if (machine instanceof PowerConsumerBlockEntity) {
            return "power_delivery";
        }
        return "machine_runtime";
    }

    private static String generatorStatus(GeneratorBlockEntity generator) {
        if (generator.getGeneratorType() == GeneratorType.FUEL_BURNER) {
            return generator.getBurnTime() > 0
                    ? "Generating " + generator.getGenerationPerTick() + " EP/t"
                    : "Waiting for fuel";
        }
        if (generator.getGeneratorType() == GeneratorType.HAND_CRANK) {
            return generator.getEnergyStored() > 0 ? "Crank buffer charged" : "Waiting for crank";
        }
        return generator.getGenerationPerTick() > 0
                ? "Generating " + generator.getGenerationPerTick() + " EP/t"
                : "Generation idle";
    }

    private static int cappedInt(long value) {
        if (value <= 0L) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static int percent(long value, long max) {
        return max <= 0L ? 0 : Math.max(0, Math.min(100, (int) Math.round(value * 100.0D / max)));
    }

    private static double maintenanceWear(BlockEntity machine) {
        if (machine instanceof GeneratorBlockEntity generator && generator.getGeneratorType() == GeneratorType.FUEL_BURNER) {
            return 0.02D;
        }
        if (machine instanceof BatteryBlockEntity) {
            return 0.01D;
        }
        return 0.005D;
    }

    private static String title(String path) {
        String[] parts = (path == null ? "powergrid_node" : path).split("_");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        return words.isEmpty() ? "PowerGrid Node" : String.join(" ", words);
    }
}
