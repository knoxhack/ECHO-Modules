package com.knoxhack.echoorbitalremnants.integration;

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
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.block.OrbitalMachineBlock.MachineKind;
import com.knoxhack.echoorbitalremnants.block.entity.OrbitalMachineBlockEntity;
import com.knoxhack.echoorbitalremnants.block.entity.OrbitalMachineBlockEntity.MachineStatus;
import com.knoxhack.echoorbitalremnants.recipe.OrbitalProcessingRecipe;
import com.knoxhack.echoorbitalremnants.registry.ModRecipes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class OrbitalMachineCoreAdapter {
    private static final EchoModuleId OWNER = EchoModuleId.of(EchoOrbitalRemnants.MODID);

    private OrbitalMachineCoreAdapter() {
    }

    public static boolean supports(BlockEntity blockEntity) {
        return blockEntity instanceof OrbitalMachineBlockEntity;
    }

    public static EchoMachineRuntimeSnapshot runtimeSnapshot(OrbitalMachineBlockEntity machine) {
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

    public static EchoMachineProfile profile(OrbitalMachineBlockEntity machine) {
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
                        20 * 60 * 15,
                        List.of("orbital_remnants", family(machine), "field_service"),
                        true,
                        Map.of(
                                "source", "OrbitalMachineCoreAdapter",
                                "family", family(machine),
                                "charge", Integer.toString(charge(machine)))),
                failureStates(machine),
                automationHooks(machine),
                integrationRefs(machine),
                List.of(),
                Map.of(
                        "source", "OrbitalMachineCoreAdapter",
                        "family", family(machine),
                        "displayName", machine.kind().displayName())
        );
    }

    private static Identifier blockId(OrbitalMachineBlockEntity machine) {
        Identifier id = machine == null ? null : BuiltInRegistries.BLOCK.getKey(machine.getBlockState().getBlock());
        return id == null ? EchoOrbitalRemnants.id("oxygen_compressor") : id;
    }

    private static EchoMachineKind machineKind(OrbitalMachineBlockEntity machine) {
        return switch (machine.kind()) {
            case ROCKET_ASSEMBLY_FRAME -> EchoMachineKind.ASSEMBLER;
            case FUEL_REFINERY -> EchoMachineKind.REFINERY;
            case HEAT_SHIELD_FABRICATOR, ORBITAL_FABRICATOR -> EchoMachineKind.FABRICATOR;
            case VACUUM_SMELTER, SOLAR_RECLAIMER, SIGNAL_ANALYZER -> EchoMachineKind.REFINERY;
            case SUIT_CHARGING_STATION, STATION_LIFE_SUPPORT_CORE -> EchoMachineKind.POWERED_STATION;
            case NAVIGATION_CONSOLE -> EchoMachineKind.AUTOMATION_NODE;
            case OXYGEN_COMPRESSOR -> EchoMachineKind.SINGLE_BLOCK;
        };
    }

    private static EchoMachineState machineState(OrbitalMachineBlockEntity machine) {
        return switch (machine.status()) {
            case PROCESSING, COMPLETE, DIAGNOSTIC -> EchoMachineState.ACTIVE;
            case CHARGING -> EchoMachineState.POWER_STARVED;
            case OUTPUT_BLOCKED -> EchoMachineState.JAMMED;
            case BAD_INPUT -> EchoMachineState.JAMMED;
            case IDLE -> EchoMachineState.IDLE;
        };
    }

    private static List<EchoMachineFailureState> failureStates(OrbitalMachineBlockEntity machine) {
        MachineStatus status = machine.status();
        if (status == MachineStatus.CHARGING) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.POWER_LOSS, 0.35D,
                    "Orbital machine is waiting for charge", "orbital_charge_low", List.of(),
                    Map.of("charge", Integer.toString(charge(machine)))));
        }
        if (status == MachineStatus.OUTPUT_BLOCKED) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.OUTPUT_BLOCKED, 0.6D,
                    "Orbital machine output slot is blocked", "orbital_output_blocked", List.of(), Map.of()));
        }
        if (status == MachineStatus.BAD_INPUT) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.INPUT_MISSING, 0.4D,
                    "Orbital machine cannot process this input", "orbital_bad_input", List.of(),
                    Map.of("kind", machine.kind().getSerializedName())));
        }
        return List.of(new EchoMachineFailureState(EchoMachineFailureKind.NONE, 0.0D,
                "Orbital machine nominal", "nominal", List.of(), Map.of()));
    }

    private static EchoMachineRuntimeSnapshot.InventoryContract inventory(OrbitalMachineBlockEntity machine) {
        List<EchoMachineRuntimeSnapshot.SlotSnapshot> slots = List.of(
                slot(machine, OrbitalMachineBlockEntity.INPUT_SLOT, "input"),
                slot(machine, OrbitalMachineBlockEntity.OUTPUT_SLOT, "output"));
        int occupied = (int) slots.stream().filter(EchoMachineRuntimeSnapshot.SlotSnapshot::occupied).count();
        return new EchoMachineRuntimeSnapshot.InventoryContract(machine.getContainerSize(), occupied, slots,
                Map.of("contract", "orbital_machine_inventory", "family", family(machine)));
    }

    private static EchoMachineRuntimeSnapshot.SlotSnapshot slot(OrbitalMachineBlockEntity machine, int index, String role) {
        ItemStack stack = machine.getItem(index);
        String itemId = stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String itemName = stack.isEmpty() ? "Empty" : stack.getHoverName().getString();
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("slotRole", role);
        attributes.put("machineKind", machine.kind().getSerializedName());
        return new EchoMachineRuntimeSnapshot.SlotSnapshot(index, role, itemId, itemName,
                stack.getCount(), !stack.isEmpty(), Map.copyOf(attributes));
    }

    private static EchoMachineRuntimeSnapshot.EnergyContract energy(OrbitalMachineBlockEntity machine) {
        return new EchoMachineRuntimeSnapshot.EnergyContract(
                EchoOrbitalRemnants.id("orbital_machine_charge").toString(),
                "charge",
                charge(machine),
                maxCharge(machine),
                true,
                machine.kind() == MachineKind.SUIT_CHARGING_STATION,
                Map.of(
                        "contract", "orbital_machine_internal_charge",
                        "family", family(machine),
                        "status", machine.status().name())
        );
    }

    private static EchoMachineRuntimeSnapshot.ProcessContract process(OrbitalMachineBlockEntity machine) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("contract", "orbital_machine_process");
        attributes.put("family", family(machine));
        attributes.put("machineKind", machine.kind().getSerializedName());
        attributes.put("status", machine.status().name());
        currentRecipe(machine).ifPresent(holder -> {
            OrbitalProcessingRecipe recipe = holder.value();
            attributes.put("recipeId", holder.id().identifier().toString());
            attributes.put("recipeDuration", Integer.toString(recipe.duration()));
            attributes.put("recipeChargeCost", Integer.toString(recipe.chargeCost()));
            attributes.put("recipeOutput", BuiltInRegistries.ITEM.getKey(recipe.resultItem()).toString());
        });
        return new EchoMachineRuntimeSnapshot.ProcessContract(
                machine.status().label(),
                machine.status() == MachineStatus.PROCESSING || machine.status() == MachineStatus.CHARGING
                        || machine.status() == MachineStatus.COMPLETE || machine.status() == MachineStatus.DIAGNOSTIC,
                progress(machine),
                maxProgress(machine),
                percent(progress(machine), maxProgress(machine)),
                recipeContract(machine),
                Map.copyOf(attributes)
        );
    }

    private static EchoMachineRuntimeSnapshot.SideConfigurationContract side(OrbitalMachineBlockEntity machine) {
        return new EchoMachineRuntimeSnapshot.SideConfigurationContract(
                "Orbital input/output lanes",
                List.of("input"),
                List.of("output"),
                List.of("input", "output"),
                Map.of("contract", "orbital_machine_sides", "family", family(machine))
        );
    }

    private static EchoMachineRuntimeSnapshot.SavedStateContract savedState(OrbitalMachineBlockEntity machine) {
        return new EchoMachineRuntimeSnapshot.SavedStateContract(
                "orbital_machine_v1",
                List.of("Items", "progress", "max_progress", "charge", "status"),
                machine.getContainerSize(),
                charge(machine),
                percent(progress(machine), maxProgress(machine)),
                machineState(machine).serializedName(),
                side(machine).label(),
                false,
                Map.of(
                        "family", family(machine),
                        "source", "OrbitalMachineCoreAdapter",
                        "machineKind", machine.kind().getSerializedName())
        );
    }

    private static EchoMachineRecipeBinding recipeBinding(OrbitalMachineBlockEntity machine) {
        return new EchoMachineRecipeBinding(
                EchoRecipeId.of(recipeContract(machine)),
                null,
                family(machine),
                0,
                List.of(),
                Map.of("family", family(machine), "source", "OrbitalMachineCoreAdapter")
        );
    }

    private static List<EchoMachineAutomationHook> automationHooks(OrbitalMachineBlockEntity machine) {
        return List.of(
                hook("item_input", EchoMachineAutomationHookKind.ITEM_INPUT, "input"),
                hook("item_output", EchoMachineAutomationHookKind.ITEM_OUTPUT, "output"),
                hook("power_input", EchoMachineAutomationHookKind.POWER_INPUT, "charge"),
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
                        EchoFeatureId.of("echoorbitalremnants.route_progression")),
                true,
                Map.of("detail", detail));
    }

    private static EchoMachineIntegrationRefs integrationRefs(OrbitalMachineBlockEntity machine) {
        return new EchoMachineIntegrationRefs(null, null, null,
                List.of(
                        EchoFeatureId.of("echolens.deep_scan"),
                        EchoFeatureId.of("echoterminal.recipe_provider"),
                        EchoFeatureId.of("echoindex.machine_entries"),
                        EchoFeatureId.of("echoholomap.machine_markers"),
                        EchoFeatureId.of("echoorbitalremnants.terminal_routes"),
                        EchoFeatureId.of("echoorbitalremnants.route_progression")),
                List.of(),
                Map.of("family", family(machine), "recipe", recipePath(machine))
        );
    }

    private static Map<String, String> runtimeAttributes(OrbitalMachineBlockEntity machine) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("source", "OrbitalMachineCoreAdapter");
        attributes.put("family", family(machine));
        attributes.put("machineKind", machine.kind().getSerializedName());
        attributes.put("status", machine.status().name());
        attributes.put("position", machine.getBlockPos().getX() + "," + machine.getBlockPos().getY() + "," + machine.getBlockPos().getZ());
        if (machine.getLevel() != null) {
            attributes.put("dimension", machine.getLevel().dimension().identifier().toString());
        }
        attributes.put("charge", Integer.toString(charge(machine)));
        attributes.put("maxCharge", Integer.toString(maxCharge(machine)));
        return Map.copyOf(attributes);
    }

    private static String recipeContract(OrbitalMachineBlockEntity machine) {
        return currentRecipe(machine)
                .map(holder -> holder.id().identifier().toString())
                .orElse(EchoOrbitalRemnants.id(recipePath(machine)).toString());
    }

    private static String recipePath(OrbitalMachineBlockEntity machine) {
        return "machine/" + machine.kind().getSerializedName();
    }

    @SuppressWarnings("unchecked")
    private static java.util.Optional<RecipeHolder<OrbitalProcessingRecipe>> currentRecipe(OrbitalMachineBlockEntity machine) {
        if (!(machine.getLevel() instanceof ServerLevel level)) {
            return java.util.Optional.empty();
        }
        ItemStack input = machine.getItem(OrbitalMachineBlockEntity.INPUT_SLOT);
        if (input.isEmpty() || !machine.kind().processingRecipeDriven()) {
            return java.util.Optional.empty();
        }
        return level.getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value().getType() == ModRecipes.ORBITAL_PROCESSING_TYPE.get())
                .map(holder -> (RecipeHolder<OrbitalProcessingRecipe>) holder)
                .filter(holder -> holder.value().matches(machine.kind(), input, level))
                .findFirst();
    }

    private static int progress(OrbitalMachineBlockEntity machine) {
        return machine.data().get(OrbitalMachineBlockEntity.DATA_PROGRESS);
    }

    private static int maxProgress(OrbitalMachineBlockEntity machine) {
        return machine.data().get(OrbitalMachineBlockEntity.DATA_MAX_PROGRESS);
    }

    private static int charge(OrbitalMachineBlockEntity machine) {
        return machine.data().get(OrbitalMachineBlockEntity.DATA_CHARGE);
    }

    private static int maxCharge(OrbitalMachineBlockEntity machine) {
        return machine.data().get(OrbitalMachineBlockEntity.DATA_MAX_CHARGE);
    }

    private static String family(OrbitalMachineBlockEntity machine) {
        return machine.kind().getSerializedName();
    }

    private static int percent(int value, int max) {
        return max <= 0 ? 0 : Math.max(0, Math.min(100, (int) Math.round(value * 100.0D / max)));
    }

    private static double maintenanceWear(OrbitalMachineBlockEntity machine) {
        return machine.kind() == MachineKind.ROCKET_ASSEMBLY_FRAME ? 0.03D : 0.015D;
    }
}
