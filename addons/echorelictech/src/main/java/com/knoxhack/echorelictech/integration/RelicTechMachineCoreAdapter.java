package com.knoxhack.echorelictech.integration;

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
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.api.relic.RelicInstanceData;
import com.knoxhack.echorelictech.block.entity.ContainmentLockerBlockEntity;
import com.knoxhack.echorelictech.block.entity.NullBatteryDockBlockEntity;
import com.knoxhack.echorelictech.block.entity.PrototypeWorkbenchBlockEntity;
import com.knoxhack.echorelictech.block.entity.RelicAnalyzerBlockEntity;
import com.knoxhack.echorelictech.config.RelicTechConfig;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class RelicTechMachineCoreAdapter {
    private static final EchoModuleId OWNER = EchoModuleId.of(EchoRelicTech.MODID);

    private RelicTechMachineCoreAdapter() {
    }

    public static boolean supports(BlockEntity blockEntity) {
        return blockEntity instanceof RelicAnalyzerBlockEntity
                || blockEntity instanceof PrototypeWorkbenchBlockEntity
                || blockEntity instanceof ContainmentLockerBlockEntity
                || blockEntity instanceof NullBatteryDockBlockEntity;
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
                        20 * 60 * 10,
                        List.of("relictech", family(machine), "field_service"),
                        true,
                        Map.of(
                                "source", "RelicTechMachineCoreAdapter",
                                "family", family(machine),
                                "liveContainerSlots", Integer.toString(containerSize(machine)))),
                failureStates(machine),
                automationHooks(machine),
                integrationRefs(machine),
                List.of(),
                Map.of(
                        "source", "RelicTechMachineCoreAdapter",
                        "family", family(machine),
                        "displayName", title(blockId.getPath()))
        );
    }

    private static Identifier blockId(BlockEntity machine) {
        if (machine == null) {
            return EchoRelicTech.id("relic_analyzer");
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(machine.getBlockState().getBlock());
        return id == null ? EchoRelicTech.id("relic_analyzer") : id;
    }

    private static EchoMachineKind machineKind(BlockEntity machine) {
        if (machine instanceof RelicAnalyzerBlockEntity) {
            return EchoMachineKind.REFINERY;
        }
        if (machine instanceof PrototypeWorkbenchBlockEntity) {
            return EchoMachineKind.REPAIR_BENCH;
        }
        if (machine instanceof NullBatteryDockBlockEntity) {
            return EchoMachineKind.POWERED_STATION;
        }
        if (machine instanceof ContainmentLockerBlockEntity) {
            return EchoMachineKind.AUTOMATION_NODE;
        }
        return EchoMachineKind.SINGLE_BLOCK;
    }

    private static EchoMachineState machineState(BlockEntity machine) {
        if (machine instanceof RelicAnalyzerBlockEntity analyzer) {
            return analyzer.progress() > 0 ? EchoMachineState.ACTIVE : EchoMachineState.IDLE;
        }
        if (machine instanceof PrototypeWorkbenchBlockEntity workbench) {
            return !workbench.getRelicSlot().isEmpty() && !workbench.getMaterialSlot().isEmpty()
                    ? EchoMachineState.ACTIVE
                    : EchoMachineState.IDLE;
        }
        if (machine instanceof ContainmentLockerBlockEntity locker) {
            return occupiedSlots(locker) > 0 ? EchoMachineState.ACTIVE : EchoMachineState.IDLE;
        }
        if (machine instanceof NullBatteryDockBlockEntity dock) {
            if (dock.getBattery().isEmpty()) {
                return EchoMachineState.IDLE;
            }
            int charge = dock.getBattery().getOrDefault(ModDataComponents.NULL_CHARGE.get(), 0);
            return charge < maxNullCharge() && !dock.getCell().isEmpty() ? EchoMachineState.ACTIVE : EchoMachineState.IDLE;
        }
        return EchoMachineState.UNKNOWN;
    }

    private static List<EchoMachineFailureState> failureStates(BlockEntity machine) {
        if (machine instanceof NullBatteryDockBlockEntity dock && dock.getBattery().isEmpty()) {
            return List.of(new EchoMachineFailureState(EchoMachineFailureKind.INPUT_MISSING, 0.35D,
                    "Null Battery Dock is waiting for a battery", "battery_missing", List.of(), Map.of()));
        }
        if (machine instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                RelicInstanceData data = relicData(container.getItem(slot));
                if (data != null && data.corruptionFlag() && machine instanceof ContainmentLockerBlockEntity) {
                    return List.of(new EchoMachineFailureState(EchoMachineFailureKind.WEAR, 0.3D,
                            "Corrupted relic is contained", "contained_corruption",
                            List.of(), Map.of("relic", data.relicId().toString())));
                }
            }
        }
        return List.of(new EchoMachineFailureState(EchoMachineFailureKind.NONE, 0.0D,
                "RelicTech machine nominal", "nominal", List.of(), Map.of()));
    }

    private static EchoMachineRuntimeSnapshot.InventoryContract inventory(BlockEntity machine) {
        if (!(machine instanceof Container container)) {
            return EchoMachineRuntimeSnapshot.InventoryContract.empty();
        }
        List<EchoMachineRuntimeSnapshot.SlotSnapshot> slots = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            slots.add(slot(machine, container, slot));
        }
        int occupied = (int) slots.stream().filter(EchoMachineRuntimeSnapshot.SlotSnapshot::occupied).count();
        return new EchoMachineRuntimeSnapshot.InventoryContract(container.getContainerSize(), occupied, slots,
                Map.of("contract", "relictech_container_inventory", "family", family(machine)));
    }

    private static EchoMachineRuntimeSnapshot.SlotSnapshot slot(BlockEntity machine, Container container, int index) {
        ItemStack stack = container.getItem(index);
        String itemId = stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String itemName = stack.isEmpty() ? "Empty" : stack.getHoverName().getString();
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("slotRole", slotRole(machine, index));
        appendRelicAttributes(attributes, stack);
        return new EchoMachineRuntimeSnapshot.SlotSnapshot(index, slotRole(machine, index), itemId, itemName,
                stack.getCount(), !stack.isEmpty(), Map.copyOf(attributes));
    }

    private static void appendRelicAttributes(Map<String, String> attributes, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        RelicInstanceData data = relicData(stack);
        if (data != null) {
            attributes.put("relicId", data.relicId().toString());
            attributes.put("condition", data.condition().getSerializedName());
            attributes.put("contained", Boolean.toString(data.containmentFlag()));
            attributes.put("identified", Boolean.toString(data.identified()));
            attributes.put("cooldown", Integer.toString(data.cooldownRemaining()));
        }
        if (stack.has(ModDataComponents.NULL_CHARGE.get())) {
            attributes.put("nullCharge", Integer.toString(stack.getOrDefault(ModDataComponents.NULL_CHARGE.get(), 0)));
        }
    }

    private static EchoMachineRuntimeSnapshot.EnergyContract energy(BlockEntity machine) {
        if (machine instanceof NullBatteryDockBlockEntity dock) {
            int charge = dock.getBattery().getOrDefault(ModDataComponents.NULL_CHARGE.get(), 0);
            int capacity = maxNullCharge();
            return new EchoMachineRuntimeSnapshot.EnergyContract(
                    EchoRelicTech.id("null_charge").toString(),
                    "null_charge",
                    charge,
                    capacity,
                    !dock.getBattery().isEmpty(),
                    false,
                    Map.of(
                            "batteryPresent", Boolean.toString(!dock.getBattery().isEmpty()),
                            "cellPresent", Boolean.toString(!dock.getCell().isEmpty()),
                            "contract", "relictech_null_battery_dock")
            );
        }
        return EchoMachineRuntimeSnapshot.EnergyContract.empty();
    }

    private static EchoMachineRuntimeSnapshot.ProcessContract process(BlockEntity machine) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("contract", "relictech_machine_process");
        attributes.put("family", family(machine));
        if (machine instanceof RelicAnalyzerBlockEntity analyzer) {
            attributes.put("hasInput", Boolean.toString(!analyzer.getInput().isEmpty()));
            attributes.put("hasOutput", Boolean.toString(analyzer.hasOutput()));
            return new EchoMachineRuntimeSnapshot.ProcessContract(
                    analyzer.progress() > 0 ? "Analyzing relic" : "Analyzer idle",
                    analyzer.progress() > 0,
                    analyzer.progress(),
                    analyzer.progressMax(),
                    percent(analyzer.progress(), analyzer.progressMax()),
                    EchoRelicTech.id("relic_analysis").toString(),
                    Map.copyOf(attributes)
            );
        }
        if (machine instanceof PrototypeWorkbenchBlockEntity workbench) {
            attributes.put("lastAction", workbench.lastAction());
            attributes.put("hasRelic", Boolean.toString(!workbench.getRelicSlot().isEmpty()));
            attributes.put("hasMaterial", Boolean.toString(!workbench.getMaterialSlot().isEmpty()));
            return new EchoMachineRuntimeSnapshot.ProcessContract(
                    workbench.lastAction().isBlank() ? "Workbench ready" : "Last action: " + workbench.lastAction(),
                    !workbench.getRelicSlot().isEmpty() && !workbench.getMaterialSlot().isEmpty(),
                    0,
                    1,
                    !workbench.getRelicSlot().isEmpty() && !workbench.getMaterialSlot().isEmpty() ? 100 : 0,
                    EchoRelicTech.id("prototype_workbench").toString(),
                    Map.copyOf(attributes)
            );
        }
        if (machine instanceof ContainmentLockerBlockEntity locker) {
            int occupied = occupiedSlots(locker);
            attributes.put("occupiedSlots", Integer.toString(occupied));
            attributes.put("containedRelics", Integer.toString(containedRelics(locker)));
            return new EchoMachineRuntimeSnapshot.ProcessContract(
                    occupied + "/" + locker.getContainerSize() + " relic slot(s) occupied",
                    occupied > 0,
                    occupied,
                    locker.getContainerSize(),
                    percent(occupied, locker.getContainerSize()),
                    EchoRelicTech.id("containment_locker").toString(),
                    Map.copyOf(attributes)
            );
        }
        if (machine instanceof NullBatteryDockBlockEntity dock) {
            int charge = dock.getBattery().getOrDefault(ModDataComponents.NULL_CHARGE.get(), 0);
            int capacity = maxNullCharge();
            attributes.put("charge", Integer.toString(charge));
            attributes.put("capacity", Integer.toString(capacity));
            return new EchoMachineRuntimeSnapshot.ProcessContract(
                    dock.getBattery().isEmpty() ? "Waiting for null battery" : "Null charge buffer " + charge + "/" + capacity,
                    !dock.getBattery().isEmpty() && charge < capacity && !dock.getCell().isEmpty(),
                    charge,
                    capacity,
                    percent(charge, capacity),
                    EchoRelicTech.id("null_battery_charge").toString(),
                    Map.copyOf(attributes)
            );
        }
        return EchoMachineRuntimeSnapshot.ProcessContract.empty();
    }

    private static EchoMachineRuntimeSnapshot.SideConfigurationContract side(BlockEntity machine) {
        return new EchoMachineRuntimeSnapshot.SideConfigurationContract(
                "RelicTech managed slots",
                upSlots(machine),
                downSlots(machine),
                sideSlots(machine),
                Map.of("contract", "relictech_slot_roles", "family", family(machine))
        );
    }

    private static EchoMachineRuntimeSnapshot.SavedStateContract savedState(BlockEntity machine) {
        List<String> keys = new ArrayList<>();
        keys.add("Items");
        if (machine instanceof RelicAnalyzerBlockEntity) {
            keys.add("progress");
        }
        if (machine instanceof PrototypeWorkbenchBlockEntity) {
            keys.add("last_action");
        }
        if (machine instanceof NullBatteryDockBlockEntity) {
            keys.add("power_grid_charge_tick");
            keys.add("null_charge");
        }
        if (machine instanceof ContainmentLockerBlockEntity) {
            keys.add("relic_data.contained");
        }
        return new EchoMachineRuntimeSnapshot.SavedStateContract(
                "relictech_machine_v1",
                List.copyOf(keys),
                containerSize(machine),
                machine instanceof NullBatteryDockBlockEntity dock
                        ? dock.getBattery().getOrDefault(ModDataComponents.NULL_CHARGE.get(), 0)
                        : 0,
                heat(machine),
                machineState(machine).serializedName(),
                "RelicTech managed slots",
                false,
                Map.of("family", family(machine), "source", "RelicTechMachineCoreAdapter")
        );
    }

    private static EchoMachineRecipeBinding recipeBinding(BlockEntity machine) {
        return new EchoMachineRecipeBinding(
                EchoRecipeId.of(EchoRelicTech.MODID + ":" + recipePath(machine)),
                null,
                family(machine),
                0,
                List.of(),
                Map.of("family", family(machine), "source", "RelicTechMachineCoreAdapter")
        );
    }

    private static List<EchoMachineAutomationHook> automationHooks(BlockEntity machine) {
        return List.of(
                hook("item_input", EchoMachineAutomationHookKind.ITEM_INPUT, "managed_slots"),
                hook("item_output", EchoMachineAutomationHookKind.ITEM_OUTPUT, "managed_slots"),
                hook("remote_status", EchoMachineAutomationHookKind.REMOTE_STATUS, family(machine))
        );
    }

    private static EchoMachineAutomationHook hook(String id, EchoMachineAutomationHookKind kind, String detail) {
        return new EchoMachineAutomationHook(id, kind, null,
                List.of(
                        EchoFeatureId.of("echolens.deep_scan"),
                        EchoFeatureId.of("echoterminal.recipe_provider"),
                        EchoFeatureId.of("echoindex.machine_entries"),
                        EchoFeatureId.of("echoholomap.machine_markers")),
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
                        EchoFeatureId.of("echopowergrid.power_request")),
                List.of(),
                Map.of("family", family(machine), "recipe", recipePath(machine))
        );
    }

    private static Map<String, String> runtimeAttributes(BlockEntity machine) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("source", "RelicTechMachineCoreAdapter");
        attributes.put("family", family(machine));
        if (machine != null) {
            attributes.put("position", machine.getBlockPos().getX() + "," + machine.getBlockPos().getY() + "," + machine.getBlockPos().getZ());
            if (machine.getLevel() != null) {
                attributes.put("dimension", machine.getLevel().dimension().identifier().toString());
            }
        }
        attributes.put("containerSlots", Integer.toString(containerSize(machine)));
        return Map.copyOf(attributes);
    }

    private static List<String> upSlots(BlockEntity machine) {
        if (machine instanceof RelicAnalyzerBlockEntity) {
            return List.of("input");
        }
        if (machine instanceof PrototypeWorkbenchBlockEntity) {
            return List.of("relic", "material");
        }
        if (machine instanceof ContainmentLockerBlockEntity locker) {
            return relicSlots(locker.getContainerSize());
        }
        if (machine instanceof NullBatteryDockBlockEntity) {
            return List.of("battery", "cell");
        }
        return List.of();
    }

    private static List<String> downSlots(BlockEntity machine) {
        if (machine instanceof RelicAnalyzerBlockEntity) {
            return List.of("output");
        }
        if (machine instanceof NullBatteryDockBlockEntity) {
            return List.of("battery");
        }
        if (machine instanceof ContainmentLockerBlockEntity locker) {
            return relicSlots(locker.getContainerSize());
        }
        return List.of();
    }

    private static List<String> sideSlots(BlockEntity machine) {
        if (machine instanceof RelicAnalyzerBlockEntity) {
            return List.of("input", "output");
        }
        if (machine instanceof PrototypeWorkbenchBlockEntity) {
            return List.of("relic", "material");
        }
        if (machine instanceof NullBatteryDockBlockEntity) {
            return List.of("cell");
        }
        if (machine instanceof ContainmentLockerBlockEntity locker) {
            return relicSlots(locker.getContainerSize());
        }
        return List.of();
    }

    private static String slotRole(BlockEntity machine, int slot) {
        if (machine instanceof RelicAnalyzerBlockEntity) {
            return switch (slot) {
                case RelicAnalyzerBlockEntity.INPUT_SLOT -> "input";
                case RelicAnalyzerBlockEntity.OUTPUT_SLOT -> "output";
                default -> "slot_" + slot;
            };
        }
        if (machine instanceof PrototypeWorkbenchBlockEntity) {
            return switch (slot) {
                case PrototypeWorkbenchBlockEntity.RELIC_SLOT -> "relic";
                case PrototypeWorkbenchBlockEntity.MATERIAL_SLOT -> "material";
                default -> "slot_" + slot;
            };
        }
        if (machine instanceof NullBatteryDockBlockEntity) {
            return switch (slot) {
                case NullBatteryDockBlockEntity.BATTERY_SLOT -> "battery";
                case NullBatteryDockBlockEntity.CELL_SLOT -> "cell";
                default -> "slot_" + slot;
            };
        }
        if (machine instanceof ContainmentLockerBlockEntity) {
            return "relic_" + slot;
        }
        return "slot_" + slot;
    }

    private static String family(BlockEntity machine) {
        if (machine instanceof RelicAnalyzerBlockEntity) {
            return "relic_analyzer";
        }
        if (machine instanceof PrototypeWorkbenchBlockEntity) {
            return "prototype_workbench";
        }
        if (machine instanceof ContainmentLockerBlockEntity) {
            return "containment_locker";
        }
        if (machine instanceof NullBatteryDockBlockEntity) {
            return "null_battery_dock";
        }
        return "relictech_machine";
    }

    private static String recipePath(BlockEntity machine) {
        return switch (family(machine)) {
            case "relic_analyzer" -> "relic_analysis";
            case "prototype_workbench" -> "prototype_workbench_action";
            case "containment_locker" -> "containment_locker_containment";
            case "null_battery_dock" -> "null_battery_charge";
            default -> "machine_runtime";
        };
    }

    private static int containedRelics(ContainmentLockerBlockEntity locker) {
        int contained = 0;
        for (ItemStack stack : locker.getContents()) {
            if (RelicTechApi.isContained(stack)) {
                contained++;
            }
        }
        return contained;
    }

    private static int occupiedSlots(Container container) {
        int occupied = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }

    private static int containerSize(BlockEntity machine) {
        return machine instanceof Container container ? container.getContainerSize() : 0;
    }

    private static RelicInstanceData relicData(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.get(ModDataComponents.RELIC_DATA.get());
    }

    private static List<String> relicSlots(int size) {
        List<String> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            slots.add("relic_" + i);
        }
        return List.copyOf(slots);
    }

    private static int maxNullCharge() {
        try {
            return RelicTechConfig.NULL_BATTERY_MAX_CHARGE.get();
        } catch (IllegalStateException exception) {
            return 8;
        }
    }

    private static int percent(int value, int max) {
        return max <= 0 ? 0 : Math.max(0, Math.min(100, (int) Math.round(value * 100.0D / max)));
    }

    private static int heat(BlockEntity machine) {
        if (machine instanceof NullBatteryDockBlockEntity dock) {
            return percent(dock.getBattery().getOrDefault(ModDataComponents.NULL_CHARGE.get(), 0), maxNullCharge());
        }
        if (machine instanceof RelicAnalyzerBlockEntity analyzer) {
            return percent(analyzer.progress(), analyzer.progressMax());
        }
        if (machine instanceof ContainmentLockerBlockEntity locker) {
            return percent(occupiedSlots(locker), locker.getContainerSize());
        }
        return 0;
    }

    private static double maintenanceWear(BlockEntity machine) {
        if (machine instanceof ContainmentLockerBlockEntity locker) {
            return containedRelics(locker) * 0.01D;
        }
        return machine instanceof NullBatteryDockBlockEntity ? 0.02D : 0.01D;
    }

    private static String title(String path) {
        String[] parts = (path == null ? "relictech_machine" : path).split("_");
        List<String> words = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        }
        return words.isEmpty() ? "RelicTech Machine" : String.join(" ", words);
    }
}
