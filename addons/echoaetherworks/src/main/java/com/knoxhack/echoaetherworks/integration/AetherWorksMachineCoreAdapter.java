package com.knoxhack.echoaetherworks.integration;

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
import com.knoxhack.echoaetherworks.EchoAetherWorks;
import com.knoxhack.echoaetherworks.api.AetherWorksApi;
import com.knoxhack.echoaetherworks.block.entity.AetherCellBlockEntity;
import com.knoxhack.echoaetherworks.block.entity.AetherCondenserBlockEntity;
import com.knoxhack.echoaetherworks.block.entity.AetherConduitBlockEntity;
import com.knoxhack.echoaetherworks.block.entity.AetherStorageBlockEntity;
import com.knoxhack.echoarcanacore.api.AetherStorage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class AetherWorksMachineCoreAdapter {
    private static final EchoModuleId OWNER = EchoModuleId.of(EchoAetherWorks.MODID);

    private AetherWorksMachineCoreAdapter() {
    }

    public static EchoMachineRuntimeSnapshot runtimeSnapshot(AetherStorageBlockEntity machine) {
        AetherWorksApi.AetherAutomationRecipe recipe = AetherWorksApi.bestAutomationRecipe(machine);
        Identifier blockId = blockId(machine);
        EchoMachineId machineId = EchoMachineId.of(blockId.toString());
        return new EchoMachineRuntimeSnapshot(
                machineId,
                OWNER,
                blockId.toString(),
                machineKind(machine),
                machineState(machine, recipe),
                title(blockId.getPath()),
                failureStates(machine),
                inventory(machine),
                energy(machine),
                EchoMachineRuntimeSnapshot.FluidContract.empty(),
                process(machine, recipe),
                side(machine),
                EchoMachineRuntimeSnapshot.UpgradeContract.empty(),
                savedState(machine),
                integrationRefs(machine, recipe),
                runtimeAttributes(machine, recipe)
        );
    }

    public static EchoMachineProfile profile(AetherStorageBlockEntity machine) {
        AetherWorksApi.AetherAutomationRecipe recipe = AetherWorksApi.bestAutomationRecipe(machine);
        Identifier blockId = blockId(machine);
        EchoMachineId machineId = EchoMachineId.of(blockId.toString());
        return new EchoMachineProfile(
                machineId,
                machineKind(machine),
                machineState(machine, recipe),
                OWNER,
                null,
                List.of(recipeBinding(machine, recipe)),
                List.of(),
                new EchoMachineMaintenanceProfile(
                        0.0D,
                        0,
                        List.of("aether_storage", "aether_network"),
                        true,
                        Map.of(
                                "overloadRisk", Integer.toString(machine.overloadRisk()),
                                "contamination", Double.toString(machine.aetherStorage().contaminationLevel()))
                ),
                failureStates(machine),
                automationHooks(machine),
                integrationRefs(machine, recipe),
                List.of(),
                Map.of(
                        "source", "AetherWorksMachineCoreAdapter",
                        "family", family(machine),
                        "displayName", title(blockId.getPath()))
        );
    }

    private static Identifier blockId(AetherStorageBlockEntity machine) {
        if (machine == null) {
            return AetherWorksApi.AETHER_CELL;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(machine.getBlockState().getBlock());
        return id == null ? AetherWorksApi.AETHER_CELL : id;
    }

    private static EchoMachineKind machineKind(AetherStorageBlockEntity machine) {
        if (machine instanceof AetherCondenserBlockEntity) {
            return EchoMachineKind.POWERED_STATION;
        }
        if (machine instanceof AetherConduitBlockEntity) {
            return EchoMachineKind.AUTOMATION_NODE;
        }
        if (machine instanceof AetherCellBlockEntity) {
            return EchoMachineKind.POWERED_STATION;
        }
        return EchoMachineKind.SINGLE_BLOCK;
    }

    private static EchoMachineState machineState(AetherStorageBlockEntity machine,
                                                 AetherWorksApi.AetherAutomationRecipe recipe) {
        if (machine == null) {
            return EchoMachineState.UNKNOWN;
        }
        if (machine.overloadLockoutTicks() > 0 || machine.overloadSeverity() >= 3) {
            return EchoMachineState.OVERLOADED;
        }
        if (!machine.automationEnabled()) {
            return EchoMachineState.PAUSED;
        }
        if (machine.redstoneControlEnabled() && !machine.redstoneAllowsAutomation()) {
            return EchoMachineState.PAUSED;
        }
        if (recipe != null && machine.automationActive()) {
            return EchoMachineState.ACTIVE;
        }
        return machine.storedAmount() <= 0.0D ? EchoMachineState.IDLE : EchoMachineState.ACTIVE;
    }

    private static List<EchoMachineFailureState> failureStates(AetherStorageBlockEntity machine) {
        if (machine == null) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.UNKNOWN, 1.0D,
                    "AetherWorks machine missing", "null_machine", List.of(), Map.of()));
        }
        if (machine.overloadLockoutTicks() > 0 || machine.overloadSeverity() > 0) {
            double severity = Math.min(1.0D, Math.max(0.25D, machine.overloadRisk() / 100.0D));
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.OVERHEAT, severity,
                    "Aether overload risk " + machine.overloadRisk() + "%",
                    machine.lastOverloadConsequence(),
                    List.of(),
                    Map.of(
                            "overloadEvents", Integer.toString(machine.overloadEvents()),
                            "lockoutTicks", Integer.toString(machine.overloadLockoutTicks()),
                            "lastSeverity", Integer.toString(machine.lastOverloadSeverity()))));
        }
        if (machine.redstoneControlEnabled() && !machine.redstoneAllowsAutomation()) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.AUTOMATION_BLOCKED, 0.5D,
                    "Redstone gate is blocking automation",
                    machine.redstoneModeName(),
                    List.of(),
                    Map.of("redstoneSide", machine.redstoneControlSideName())));
        }
        return List.of(new EchoMachineFailureState(EchoMachineFailureKind.NONE, 0.0D,
                "AetherWorks machine nominal", "nominal", List.of(), Map.of()));
    }

    private static EchoMachineRuntimeSnapshot.InventoryContract inventory(AetherStorageBlockEntity machine) {
        List<EchoMachineRuntimeSnapshot.SlotSnapshot> slots = new ArrayList<>();
        slots.add(slot(machine, AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT, "input"));
        slots.add(slot(machine, AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT, "output"));
        slots.add(slot(machine, AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT, "secondary_input"));
        int occupied = (int) slots.stream().filter(EchoMachineRuntimeSnapshot.SlotSnapshot::occupied).count();
        return new EchoMachineRuntimeSnapshot.InventoryContract(machine.getContainerSize(), occupied, slots,
                Map.of("contract", "aetherworks_automation_inventory"));
    }

    private static EchoMachineRuntimeSnapshot.SlotSnapshot slot(AetherStorageBlockEntity machine, int index, String role) {
        ItemStack stack = machine.getItem(index);
        String itemId = stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String itemName = stack.isEmpty() ? "Empty" : stack.getHoverName().getString();
        return new EchoMachineRuntimeSnapshot.SlotSnapshot(index, role, itemId, itemName, stack.getCount(),
                !stack.isEmpty(), Map.of("slotRole", role));
    }

    private static EchoMachineRuntimeSnapshot.EnergyContract energy(AetherStorageBlockEntity machine) {
        AetherStorage storage = machine.aetherStorage();
        return new EchoMachineRuntimeSnapshot.EnergyContract(
                EchoAetherWorks.id("aether").toString(),
                "aether",
                rounded(storage.storedAmount()),
                rounded(storage.maxStoredAmount()),
                machine.acceptsNetworkInput(),
                machine.canPushNetwork(),
                Map.of(
                        "outputType", storage.outputType().serializedName(),
                        "transferRate", Integer.toString(rounded(storage.transferRate())),
                        "contamination", Double.toString(storage.contaminationLevel()),
                        "acceptedTypes", Integer.toString(storage.acceptedTypes().size()))
        );
    }

    private static EchoMachineRuntimeSnapshot.ProcessContract process(AetherStorageBlockEntity machine,
                                                                      AetherWorksApi.AetherAutomationRecipe recipe) {
        AetherWorksApi.AetherTopologySnapshot topology = AetherWorksApi.describeTopology(machine.getLevel(), machine.getBlockPos());
        boolean active = recipe != null && machine.automationActive();
        String status = machine.overloadLockoutTicks() > 0
                ? "Overload Lockout"
                : active ? "Ready: " + recipe.id() : machine.automationEnabled() ? "Idle" : "Automation Paused";
        int progressPercent = Math.max(0, Math.min(100, (int) Math.round(machine.fillRatio() * 100.0D)));
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("contract", "aetherworks_network_automation");
        attributes.put("topologyNodes", Integer.toString(topology.nodeCount()));
        attributes.put("routeDepth", Integer.toString(topology.routeDepth()));
        attributes.put("readyRecipes", Integer.toString(topology.automationRecipes()));
        attributes.put("completedRecipes", Integer.toString(topology.completedRecipes()));
        if (recipe != null) {
            attributes.put("recipeId", recipe.id());
            attributes.put("recipeFamily", recipe.family());
            attributes.put("aetherCost", Integer.toString(recipe.aetherCost()));
        }
        return new EchoMachineRuntimeSnapshot.ProcessContract(
                status,
                active,
                machine.automationCycles(),
                Math.max(1, machine.automationCycles() + 1),
                progressPercent,
                recipe == null ? EchoAetherWorks.id("aether_network").toString() : EchoAetherWorks.id(recipe.id()).toString(),
                Map.copyOf(attributes)
        );
    }

    private static EchoMachineRuntimeSnapshot.SideConfigurationContract side(AetherStorageBlockEntity machine) {
        return new EchoMachineRuntimeSnapshot.SideConfigurationContract(
                machine.networkModeName() + "/" + machine.redstoneModeName(),
                slotNames(machine, Direction.UP),
                slotNames(machine, Direction.DOWN),
                slotNames(machine, Direction.NORTH),
                Map.of(
                        "contract", "aetherworks_sided_inventory",
                        "redstoneSide", machine.redstoneControlSideName(),
                        "redstonePowered", Boolean.toString(machine.redstonePowered()))
        );
    }

    private static List<String> slotNames(AetherStorageBlockEntity machine, Direction direction) {
        List<String> names = new ArrayList<>();
        for (int slot : machine.getSlotsForFace(direction)) {
            names.add(switch (slot) {
                case AetherStorageBlockEntity.AUTOMATION_INPUT_SLOT -> "input";
                case AetherStorageBlockEntity.AUTOMATION_OUTPUT_SLOT -> "output";
                case AetherStorageBlockEntity.AUTOMATION_SECONDARY_INPUT_SLOT -> "secondary_input";
                default -> "slot_" + slot;
            });
        }
        return List.copyOf(names);
    }

    private static EchoMachineRuntimeSnapshot.SavedStateContract savedState(AetherStorageBlockEntity machine) {
        return new EchoMachineRuntimeSnapshot.SavedStateContract(
                "aetherworks_storage_v1",
                List.of(
                        "Items",
                        "stored",
                        "capacity",
                        "output_type",
                        "transfer_rate",
                        "contamination",
                        "network_mode",
                        "automation_enabled",
                        "redstone_mode",
                        "redstone_control_side",
                        "automation_cycles",
                        "overload_events",
                        "overload_lockout_ticks"),
                machine.getContainerSize(),
                rounded(machine.storedAmount()),
                machine.overloadRisk(),
                machine.automationEnabled() ? "automation_enabled" : "automation_paused",
                machine.networkModeName(),
                !machine.automationEnabled(),
                Map.of(
                        "redstoneMode", machine.redstoneModeName(),
                        "redstoneSide", machine.redstoneControlSideName(),
                        "lastOverloadConsequence", machine.lastOverloadConsequence())
        );
    }

    private static EchoMachineRecipeBinding recipeBinding(AetherStorageBlockEntity machine,
                                                          AetherWorksApi.AetherAutomationRecipe recipe) {
        String id = recipe == null ? family(machine) + "_aether_automation" : recipe.id();
        return new EchoMachineRecipeBinding(
                EchoRecipeId.of(EchoAetherWorks.MODID + ":" + id),
                null,
                "aether_automation",
                0,
                List.of(),
                recipe == null
                        ? Map.of("family", family(machine))
                        : Map.of(
                                "family", recipe.family(),
                                "inputSlots", Integer.toString(recipe.inputSlots()),
                                "outputSlots", Integer.toString(recipe.outputSlots()),
                                "aetherCost", Integer.toString(recipe.aetherCost()))
        );
    }

    private static List<EchoMachineAutomationHook> automationHooks(AetherStorageBlockEntity machine) {
        return List.of(
                hook("item_input", EchoMachineAutomationHookKind.ITEM_INPUT, "top_or_side"),
                hook("item_output", EchoMachineAutomationHookKind.ITEM_OUTPUT, "bottom"),
                hook("aether_input", EchoMachineAutomationHookKind.SIGNAL_INPUT, machine.aetherStorage().outputType().serializedName()),
                hook("aether_output", EchoMachineAutomationHookKind.SIGNAL_OUTPUT, machine.networkModeName()),
                hook("redstone_gate", EchoMachineAutomationHookKind.REMOTE_STATUS, machine.redstoneModeName())
        );
    }

    private static EchoMachineAutomationHook hook(String id, EchoMachineAutomationHookKind kind, String detail) {
        return new EchoMachineAutomationHook(id, kind, null,
                List.of(
                        EchoFeatureId.of("echolens.deep_scan"),
                        EchoFeatureId.of("echologisticsnetwork.external_endpoints")),
                true,
                Map.of("detail", detail == null ? "" : detail));
    }

    private static EchoMachineIntegrationRefs integrationRefs(AetherStorageBlockEntity machine,
                                                             AetherWorksApi.AetherAutomationRecipe recipe) {
        return new EchoMachineIntegrationRefs(null, null, null,
                List.of(
                        EchoFeatureId.of("echolens.deep_scan"),
                        EchoFeatureId.of("echoterminal.recipe_provider"),
                        EchoFeatureId.of("echoindex.machine_entries"),
                        EchoFeatureId.of("echoholomap.machine_markers"),
                        EchoFeatureId.of("echologisticsnetwork.external_endpoints")),
                List.of(),
                Map.of(
                        "family", family(machine),
                        "recipe", recipe == null ? "" : recipe.id())
        );
    }

    private static Map<String, String> runtimeAttributes(AetherStorageBlockEntity machine,
                                                         AetherWorksApi.AetherAutomationRecipe recipe) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("source", "AetherWorksMachineCoreAdapter");
        attributes.put("family", family(machine));
        attributes.put("position", machine.getBlockPos().getX() + "," + machine.getBlockPos().getY() + "," + machine.getBlockPos().getZ());
        if (machine.getLevel() != null) {
            attributes.put("dimension", machine.getLevel().dimension().identifier().toString());
        }
        attributes.put("networkMode", machine.networkModeName());
        attributes.put("redstoneMode", machine.redstoneModeName());
        attributes.put("automationActive", Boolean.toString(machine.automationActive()));
        attributes.put("overloadRisk", Integer.toString(machine.overloadRisk()));
        if (recipe != null) {
            attributes.put("readyRecipe", recipe.id());
            attributes.put("recipeFamily", recipe.family());
        }
        return Map.copyOf(attributes);
    }

    private static String family(AetherStorageBlockEntity machine) {
        if (machine instanceof AetherCondenserBlockEntity) {
            return "condenser";
        }
        if (machine instanceof AetherConduitBlockEntity) {
            return "conduit";
        }
        if (machine instanceof AetherCellBlockEntity) {
            return "cell";
        }
        return "aether_storage";
    }

    private static int rounded(double value) {
        return (int) Math.round(Math.max(0.0D, value));
    }

    private static String title(String path) {
        String[] parts = (path == null ? "aether_machine" : path).split("_");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        return words.isEmpty() ? "Aether Machine" : String.join(" ", words);
    }
}
