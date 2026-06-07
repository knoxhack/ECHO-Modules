package com.knoxhack.echonexusprotocol.integration;

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
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.block.NexusMachineBlock.MachineKind;
import com.knoxhack.echonexusprotocol.block.entity.NexusMachineBlockEntity;
import com.knoxhack.echonexusprotocol.block.entity.NexusMachineBlockEntity.MachineStatus;
import com.knoxhack.echonexusprotocol.recipe.NexusProcessingRecipe;
import com.knoxhack.echonexusprotocol.registry.ModRecipes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class NexusMachineCoreAdapter {
    private static final EchoModuleId OWNER = EchoModuleId.of(EchoNexusProtocol.MODID);

    private NexusMachineCoreAdapter() {
    }

    public static boolean supports(BlockEntity blockEntity) {
        return blockEntity instanceof NexusMachineBlockEntity;
    }

    public static EchoMachineRuntimeSnapshot runtimeSnapshot(NexusMachineBlockEntity machine) {
        Identifier blockId = blockId(machine);
        EchoMachineId machineId = EchoMachineId.of(blockId.toString());
        return new EchoMachineRuntimeSnapshot(
                machineId,
                OWNER,
                blockId.toString(),
                machineKind(machine),
                machineState(machine),
                machine.kind().displayName(),
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

    public static EchoMachineProfile profile(NexusMachineBlockEntity machine) {
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
                        20 * 60 * 20,
                        List.of("nexus_protocol", family(machine), "containment_service"),
                        true,
                        Map.of(
                                "source", "NexusMachineCoreAdapter",
                                "family", family(machine),
                                "nexusCharge", Integer.toString(charge(machine)),
                                "contamination", Integer.toString(contamination(machine)))),
                failureStates(machine),
                automationHooks(machine),
                integrationRefs(machine),
                List.of(),
                Map.of(
                        "source", "NexusMachineCoreAdapter",
                        "family", family(machine),
                        "displayName", machine.kind().displayName())
        );
    }

    private static Identifier blockId(NexusMachineBlockEntity machine) {
        Identifier id = machine == null ? null : BuiltInRegistries.BLOCK.getKey(machine.getBlockState().getBlock());
        return id == null ? EchoNexusProtocol.id("nexus_recycler") : id;
    }

    private static EchoMachineKind machineKind(NexusMachineBlockEntity machine) {
        return switch (machine.kind()) {
            case NEXUS_RECYCLER, CORRUPTION_FILTER -> EchoMachineKind.REFINERY;
            case NEXUS_CHARGE_TANK, NEXUS_FIELD_STABILIZER, CORRUPTION_REACTOR -> EchoMachineKind.POWERED_STATION;
            case NEXUS_INFUSER, REALITY_FORGE -> EchoMachineKind.FABRICATOR;
            case MEMORY_DECODER -> EchoMachineKind.AUTOMATION_NODE;
        };
    }

    private static EchoMachineState machineState(NexusMachineBlockEntity machine) {
        return switch (status(machine)) {
            case PROCESSING, COMPLETE, PURIFYING, STABILIZING, REACTING -> EchoMachineState.ACTIVE;
            case CHARGING -> EchoMachineState.POWER_STARVED;
            case OUTPUT_BLOCKED, BAD_INPUT -> EchoMachineState.JAMMED;
            case LEAKING -> EchoMachineState.OVERLOADED;
            case IDLE -> EchoMachineState.IDLE;
        };
    }

    private static List<EchoMachineFailureState> failureStates(NexusMachineBlockEntity machine) {
        MachineStatus status = status(machine);
        if (status == MachineStatus.CHARGING) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.POWER_LOSS, 0.45D,
                    "Nexus machine is waiting for Nexus Charge", "nexus_charge_low", List.of(),
                    Map.of("nexusCharge", Integer.toString(charge(machine)))));
        }
        if (status == MachineStatus.OUTPUT_BLOCKED) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.OUTPUT_BLOCKED, 0.6D,
                    "Nexus machine output slot is blocked", "nexus_output_blocked", List.of(), Map.of()));
        }
        if (status == MachineStatus.BAD_INPUT) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.INPUT_MISSING, 0.4D,
                    "Nexus machine cannot process this input", "nexus_bad_input", List.of(),
                    Map.of("kind", machine.kind().getSerializedName())));
        }
        if (status == MachineStatus.LEAKING) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.OVERHEAT, 0.75D,
                    "Nexus containment leak is raising local corruption", "nexus_containment_leak", List.of(),
                    Map.of("contamination", Integer.toString(contamination(machine)))));
        }
        return List.of(new EchoMachineFailureState(EchoMachineFailureKind.NONE, 0.0D,
                "Nexus machine nominal", "nominal", List.of(), Map.of()));
    }

    private static EchoMachineRuntimeSnapshot.InventoryContract inventory(NexusMachineBlockEntity machine) {
        List<EchoMachineRuntimeSnapshot.SlotSnapshot> slots = List.of(
                slot(machine, NexusMachineBlockEntity.INPUT_SLOT, "input"),
                slot(machine, NexusMachineBlockEntity.OUTPUT_SLOT, "output"));
        int occupied = (int) slots.stream().filter(EchoMachineRuntimeSnapshot.SlotSnapshot::occupied).count();
        return new EchoMachineRuntimeSnapshot.InventoryContract(machine.getContainerSize(), occupied, slots,
                Map.of("contract", "nexus_machine_inventory", "family", family(machine)));
    }

    private static EchoMachineRuntimeSnapshot.SlotSnapshot slot(NexusMachineBlockEntity machine, int index, String role) {
        ItemStack stack = machine.getItem(index);
        String itemId = stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String itemName = stack.isEmpty() ? "Empty" : stack.getHoverName().getString();
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("slotRole", role);
        attributes.put("machineKind", machine.kind().getSerializedName());
        return new EchoMachineRuntimeSnapshot.SlotSnapshot(index, role, itemId, itemName,
                stack.getCount(), !stack.isEmpty(), Map.copyOf(attributes));
    }

    private static EchoMachineRuntimeSnapshot.EnergyContract energy(NexusMachineBlockEntity machine) {
        return new EchoMachineRuntimeSnapshot.EnergyContract(
                EchoNexusProtocol.id("nexus_charge").toString(),
                "Nexus Charge",
                charge(machine),
                maxCharge(machine),
                true,
                true,
                Map.of(
                        "contract", "nexus_machine_charge",
                        "family", family(machine),
                        "status", status(machine).name(),
                        "contamination", Integer.toString(contamination(machine)))
        );
    }

    private static EchoMachineRuntimeSnapshot.ProcessContract process(NexusMachineBlockEntity machine) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("contract", "nexus_machine_process");
        attributes.put("family", family(machine));
        attributes.put("machineKind", machine.kind().getSerializedName());
        attributes.put("status", status(machine).name());
        attributes.put("contamination", Integer.toString(contamination(machine)));
        currentRecipe(machine).ifPresent(holder -> {
            NexusProcessingRecipe recipe = holder.value();
            attributes.put("recipeId", holder.id().identifier().toString());
            attributes.put("recipeDuration", Integer.toString(recipe.duration()));
            attributes.put("recipeChargeCost", Integer.toString(recipe.chargeCost()));
            attributes.put("recipeChargeOutput", Integer.toString(recipe.chargeOutput()));
            attributes.put("recipeCorruptionDelta", Integer.toString(recipe.corruptionDelta()));
            attributes.put("recipeFieldDelta", Integer.toString(recipe.fieldDelta()));
            if (!recipe.result().isEmpty()) {
                attributes.put("recipeOutput", BuiltInRegistries.ITEM.getKey(recipe.result().getItem()).toString());
            }
        });
        return new EchoMachineRuntimeSnapshot.ProcessContract(
                status(machine).label(),
                processActive(machine),
                progress(machine),
                maxProgress(machine),
                percent(progress(machine), maxProgress(machine)),
                recipeContract(machine),
                Map.copyOf(attributes)
        );
    }

    private static EchoMachineRuntimeSnapshot.SideConfigurationContract side(NexusMachineBlockEntity machine) {
        return new EchoMachineRuntimeSnapshot.SideConfigurationContract(
                "Nexus containment lanes",
                List.of("input", "charge"),
                List.of("output"),
                List.of("input", "output", "charge"),
                Map.of("contract", "nexus_machine_sides", "family", family(machine))
        );
    }

    private static EchoMachineRuntimeSnapshot.SavedStateContract savedState(NexusMachineBlockEntity machine) {
        return new EchoMachineRuntimeSnapshot.SavedStateContract(
                "nexus_machine_v1",
                List.of("Items", "progress", "maxProgress", "nexusCharge", "contamination", "status"),
                machine.getContainerSize(),
                charge(machine),
                contamination(machine),
                machineState(machine).serializedName(),
                side(machine).label(),
                false,
                Map.of(
                        "family", family(machine),
                        "source", "NexusMachineCoreAdapter",
                        "machineKind", machine.kind().getSerializedName())
        );
    }

    private static EchoMachineRecipeBinding recipeBinding(NexusMachineBlockEntity machine) {
        return new EchoMachineRecipeBinding(
                EchoRecipeId.of(recipeContract(machine)),
                null,
                family(machine),
                0,
                List.of(),
                Map.of("family", family(machine), "source", "NexusMachineCoreAdapter")
        );
    }

    private static List<EchoMachineAutomationHook> automationHooks(NexusMachineBlockEntity machine) {
        return List.of(
                hook("item_input", EchoMachineAutomationHookKind.ITEM_INPUT, "input"),
                hook("item_output", EchoMachineAutomationHookKind.ITEM_OUTPUT, "output"),
                hook("power_input", EchoMachineAutomationHookKind.POWER_INPUT, "nexus_charge_input"),
                hook("power_output", EchoMachineAutomationHookKind.POWER_INPUT, "nexus_charge_output"),
                hook("remote_status", EchoMachineAutomationHookKind.REMOTE_STATUS, family(machine))
        );
    }

    private static EchoMachineAutomationHook hook(String id, EchoMachineAutomationHookKind kind, String detail) {
        return new EchoMachineAutomationHook(id, kind, null,
                List.of(
                        EchoFeatureId.of("echolens.deep_scan"),
                        EchoFeatureId.of("echoterminal.recipe_provider"),
                        EchoFeatureId.of("echoindex.machine_entries"),
                        EchoFeatureId.of("echoholomap.machine_markers"),
                        EchoFeatureId.of("echonexusprotocol.field_progression")),
                true,
                Map.of("detail", detail));
    }

    private static EchoMachineIntegrationRefs integrationRefs(NexusMachineBlockEntity machine) {
        return new EchoMachineIntegrationRefs(null, null, null,
                List.of(
                        EchoFeatureId.of("echolens.deep_scan"),
                        EchoFeatureId.of("echoterminal.recipe_provider"),
                        EchoFeatureId.of("echoindex.machine_entries"),
                        EchoFeatureId.of("echoholomap.machine_markers"),
                        EchoFeatureId.of("echonexusprotocol.field_terminal"),
                        EchoFeatureId.of("echonexusprotocol.field_progression")),
                List.of(),
                Map.of("family", family(machine), "recipe", recipePath(machine))
        );
    }

    private static Map<String, String> runtimeAttributes(NexusMachineBlockEntity machine) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("source", "NexusMachineCoreAdapter");
        attributes.put("family", family(machine));
        attributes.put("machineKind", machine.kind().getSerializedName());
        attributes.put("status", status(machine).name());
        attributes.put("position", machine.getBlockPos().getX() + "," + machine.getBlockPos().getY() + "," + machine.getBlockPos().getZ());
        if (machine.getLevel() != null) {
            attributes.put("dimension", machine.getLevel().dimension().identifier().toString());
        }
        attributes.put("nexusCharge", Integer.toString(charge(machine)));
        attributes.put("maxNexusCharge", Integer.toString(maxCharge(machine)));
        attributes.put("contamination", Integer.toString(contamination(machine)));
        return Map.copyOf(attributes);
    }

    private static String recipeContract(NexusMachineBlockEntity machine) {
        return currentRecipe(machine)
                .map(holder -> holder.id().identifier().toString())
                .orElse(EchoNexusProtocol.id(recipePath(machine)).toString());
    }

    private static String recipePath(NexusMachineBlockEntity machine) {
        return "machine/" + machine.kind().getSerializedName();
    }

    @SuppressWarnings("unchecked")
    private static java.util.Optional<RecipeHolder<NexusProcessingRecipe>> currentRecipe(NexusMachineBlockEntity machine) {
        if (!(machine.getLevel() instanceof ServerLevel level)) {
            return java.util.Optional.empty();
        }
        ItemStack input = machine.getItem(NexusMachineBlockEntity.INPUT_SLOT);
        if (input.isEmpty() || (!machine.kind().recipeDriven() && machine.kind() != MachineKind.CORRUPTION_REACTOR)) {
            return java.util.Optional.empty();
        }
        return level.getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value().getType() == ModRecipes.NEXUS_PROCESSING_TYPE.get())
                .map(holder -> (RecipeHolder<NexusProcessingRecipe>) holder)
                .filter(holder -> holder.value().matches(machine.kind(), input, level))
                .findFirst();
    }

    private static boolean processActive(NexusMachineBlockEntity machine) {
        return switch (status(machine)) {
            case PROCESSING, CHARGING, COMPLETE, PURIFYING, STABILIZING, REACTING -> true;
            case IDLE, OUTPUT_BLOCKED, BAD_INPUT, LEAKING -> maxProgress(machine) > 0;
        };
    }

    private static MachineStatus status(NexusMachineBlockEntity machine) {
        return MachineStatus.byId(machine.data().get(NexusMachineBlockEntity.DATA_STATUS));
    }

    private static int progress(NexusMachineBlockEntity machine) {
        return machine.data().get(NexusMachineBlockEntity.DATA_PROGRESS);
    }

    private static int maxProgress(NexusMachineBlockEntity machine) {
        return machine.data().get(NexusMachineBlockEntity.DATA_MAX_PROGRESS);
    }

    private static int charge(NexusMachineBlockEntity machine) {
        return machine.data().get(NexusMachineBlockEntity.DATA_CHARGE);
    }

    private static int maxCharge(NexusMachineBlockEntity machine) {
        return machine.data().get(NexusMachineBlockEntity.DATA_MAX_CHARGE);
    }

    private static int contamination(NexusMachineBlockEntity machine) {
        return machine.data().get(NexusMachineBlockEntity.DATA_CORRUPTION);
    }

    private static String family(NexusMachineBlockEntity machine) {
        return machine.kind().getSerializedName();
    }

    private static int percent(int value, int max) {
        return max <= 0 ? 0 : Math.max(0, Math.min(100, (int) Math.round(value * 100.0D / max)));
    }

    private static double maintenanceWear(NexusMachineBlockEntity machine) {
        return switch (machine.kind()) {
            case CORRUPTION_REACTOR -> 0.08D;
            case CORRUPTION_FILTER, REALITY_FORGE -> 0.04D;
            default -> 0.02D;
        };
    }
}
