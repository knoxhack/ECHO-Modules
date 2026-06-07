package com.knoxhack.echoashfallprotocol.integration;

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
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.entity.HopperHandler;
import com.knoxhack.echoashfallprotocol.block.entity.MachineInventory;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.machine.MachineState;
import com.knoxhack.echoashfallprotocol.machine.MachineStateProvider;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Adapts live Ashfall machines into MachineCore's neutral machine contracts.
 */
public final class AshfallMachineCoreAdapter {
    private static final EchoModuleId OWNER = EchoModuleId.of(EchoAshfallProtocol.MODID);

    private AshfallMachineCoreAdapter() {
    }

    public static EchoMachineRuntimeSnapshot runtimeSnapshot(Level level, BlockPos pos) {
        BlockEntity machine = Objects.requireNonNull(level, "level").getBlockEntity(Objects.requireNonNull(pos, "pos"));
        if (machine == null) {
            throw new IllegalArgumentException("No Ashfall machine block entity at " + pos);
        }
        return runtimeSnapshot(machine);
    }

    public static EchoMachineRuntimeSnapshot runtimeSnapshot(BlockEntity machine) {
        Objects.requireNonNull(machine, "machine");
        Level level = machine.getLevel();
        BlockPos pos = machine.getBlockPos();
        BlockState blockState = machine.getBlockState();
        String machineBlockId = blockId(blockState);
        String machinePath = path(machineBlockId);
        MachineState ashfallState = ashfallState(level, pos, blockState);
        MachineInventory inventory = inventory(machine);
        ContainerData data = data(machine);
        int wearPercent = wearPercent(level, pos);
        EchoMachineState state = machineState(ashfallState);

        return new EchoMachineRuntimeSnapshot(
                EchoMachineId.of(machineBlockId),
                OWNER,
                machineBlockId,
                machineKind(machinePath),
                state,
                displayName(machine, machinePath),
                failureStates(ashfallState, wearPercent),
                inventory(machine, inventory, machinePath),
                energy(machine),
                fluids(machine, inventory, machinePath),
                process(data, ashfallState, state, machinePath, wearPercent),
                side(machine, machinePath),
                upgrades(inventory, machinePath),
                savedState(inventory, machine, data, ashfallState, wearPercent),
                integrationRefs(machinePath),
                Map.of(
                        "source", "AshfallMachineCoreAdapter",
                        "ashfallState", ashfallState.name(),
                        "blockEntity", machine.getClass().getSimpleName(),
                        "position", pos.getX() + "," + pos.getY() + "," + pos.getZ(),
                        "dimension", level == null ? "minecraft:overworld" : level.dimension().identifier().toString()
                )
        );
    }

    public static EchoMachineProfile profile(BlockEntity machine) {
        EchoMachineRuntimeSnapshot snapshot = runtimeSnapshot(machine);
        return new EchoMachineProfile(
                snapshot.id(),
                snapshot.kind(),
                snapshot.state(),
                snapshot.ownerModule(),
                null,
                List.of(recipeBinding(snapshot)),
                List.of(),
                new EchoMachineMaintenanceProfile(
                        0.01D,
                        MachineWearData.MAX_WEAR * 20,
                        List.of("ashfall_repair", "scrap_metal_service"),
                        true,
                        Map.of("wearPercent", Integer.toString(snapshot.savedState().heat()))
                ),
                snapshot.failureStates(),
                automationHooks(snapshot),
                snapshot.integrationRefs(),
                List.of(),
                Map.of(
                        "source", "AshfallMachineCoreAdapter",
                        "machineBlockId", snapshot.machineBlockId(),
                        "displayName", snapshot.displayName()
                )
        );
    }

    private static String blockId(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id == null ? EchoAshfallProtocol.MODID + ":unknown_machine" : id.toString();
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }

    private static MachineState ashfallState(Level level, BlockPos pos, BlockState state) {
        if (level != null && state.getBlock() instanceof MachineStateProvider provider) {
            return provider.getMachineState(level, pos, state);
        }
        return MachineState.IDLE;
    }

    private static EchoMachineKind machineKind(String path) {
        return switch (path) {
            case "water_purifier", "filter_workbench", "radiation_cleanser", "atmospheric_scrubber",
                    "contaminant_condenser", "isotope_refiner" -> EchoMachineKind.REFINERY;
            case "micro_generator", "scrap_dynamo", "battery_bank", "power_node", "load_distributor",
                    "nexus_capacitor", "thermal_array" -> EchoMachineKind.POWERED_STATION;
            case "factory_controller", "autofeed_hopper", "item_pipe" -> EchoMachineKind.AUTOMATION_NODE;
            case "hand_recycler" -> EchoMachineKind.REPAIR_BENCH;
            case "scrap_press", "ore_grinder", "thermal_burner", "crystalline_synthesizer" -> EchoMachineKind.FABRICATOR;
            default -> EchoMachineKind.SINGLE_BLOCK;
        };
    }

    private static EchoMachineState machineState(MachineState state) {
        return switch (state) {
            case PROCESSING, GENERATING -> EchoMachineState.ACTIVE;
            case JAMMED -> EchoMachineState.JAMMED;
            case UNPOWERED, BROWNOUT, BOTTLENECK, PRIORITY_PAUSED -> EchoMachineState.POWER_STARVED;
            case BLOCKED -> EchoMachineState.PAUSED;
            case CONTROLLER_DISABLED, OFFLINE -> EchoMachineState.OFFLINE;
            case UNSTABLE -> EchoMachineState.DAMAGED;
            case IDLE -> EchoMachineState.IDLE;
        };
    }

    private static String displayName(BlockEntity machine, String path) {
        if (machine instanceof MenuProvider provider) {
            String label = provider.getDisplayName().getString();
            if (!label.isBlank() && !label.startsWith("block.")) {
                return label;
            }
        }
        return title(path);
    }

    private static String title(String path) {
        String[] words = path.replace('-', '_').split("_");
        List<String> titled = new ArrayList<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            titled.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT));
        }
        return titled.isEmpty() ? "Ashfall Machine" : String.join(" ", titled);
    }

    private static List<EchoMachineFailureState> failureStates(MachineState state, int wearPercent) {
        EchoMachineFailureKind kind = switch (state) {
            case JAMMED -> EchoMachineFailureKind.JAM;
            case UNPOWERED, BROWNOUT, BOTTLENECK, PRIORITY_PAUSED -> EchoMachineFailureKind.POWER_LOSS;
            case BLOCKED -> EchoMachineFailureKind.OUTPUT_BLOCKED;
            case CONTROLLER_DISABLED -> EchoMachineFailureKind.AUTOMATION_BLOCKED;
            case UNSTABLE -> EchoMachineFailureKind.WEAR;
            case OFFLINE -> EchoMachineFailureKind.STRUCTURE_INVALID;
            default -> EchoMachineFailureKind.NONE;
        };
        double severity = kind == EchoMachineFailureKind.NONE ? 0.0D : 1.0D;
        return List.of(new EchoMachineFailureState(
                kind,
                severity,
                state.getDisplayName(),
                state.name(),
                List.of(),
                Map.of(
                        "ashfallState", state.name(),
                        "wearPercent", Integer.toString(wearPercent)
                )
        ));
    }

    private static EchoMachineRuntimeSnapshot.InventoryContract inventory(
            BlockEntity machine,
            MachineInventory inventory,
            String machinePath
    ) {
        if (inventory == null) {
            return EchoMachineRuntimeSnapshot.InventoryContract.empty();
        }
        List<EchoMachineRuntimeSnapshot.SlotSnapshot> slots = new ArrayList<>();
        int occupied = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                occupied++;
            }
            slots.add(slot(machine, machinePath, slot, stack));
        }
        return new EchoMachineRuntimeSnapshot.InventoryContract(
                inventory.getContainerSize(),
                occupied,
                slots,
                Map.of("contract", "ashfall_machine_inventory")
        );
    }

    private static EchoMachineRuntimeSnapshot.SlotSnapshot slot(
            BlockEntity machine,
            String machinePath,
            int index,
            ItemStack stack
    ) {
        String role = slotRole(machine, machinePath, index);
        String itemId = stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String itemName = stack.isEmpty() ? "Empty" : stack.getHoverName().getString();
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("ashfallRole", detailedSlotRole(machinePath, index));
        attributes.put("canInsert", Boolean.toString(machine instanceof HopperHandler handler && inputSlot(handler, index)));
        attributes.put("canExtract", Boolean.toString(machine instanceof HopperHandler handler && outputSlot(handler, index)));
        return new EchoMachineRuntimeSnapshot.SlotSnapshot(
                index,
                role,
                itemId,
                itemName,
                stack.getCount(),
                !stack.isEmpty(),
                attributes
        );
    }

    private static EchoMachineRuntimeSnapshot.EnergyContract energy(BlockEntity machine) {
        if (!(machine instanceof IEnergyStorage energy)) {
            return EchoMachineRuntimeSnapshot.EnergyContract.empty();
        }
        return new EchoMachineRuntimeSnapshot.EnergyContract(
                EchoAshfallProtocol.MODID + ":fe_power",
                "FE",
                energy.getEnergyStored(),
                energy.getMaxEnergyStored(),
                energy.canReceive(),
                energy.canExtract(),
                Map.of("contract", "ashfall_power_network")
        );
    }

    private static EchoMachineRuntimeSnapshot.FluidContract fluids(
            BlockEntity machine,
            MachineInventory inventory,
            String machinePath
    ) {
        if ("water_purifier".equals(machinePath)) {
            int dirty = countItems(inventory, "dirty_water_bottle");
            int clean = countItems(inventory, "clean_water_bottle");
            return new EchoMachineRuntimeSnapshot.FluidContract(
                    false,
                    0,
                    null,
                    null,
                    Map.of(
                            "contract", "ashfall_item_bottle_water",
                            "dirtyBottleCount", Integer.toString(dirty),
                            "cleanBottleCount", Integer.toString(clean),
                            "source", machine.getClass().getSimpleName()
                    )
            );
        }
        return EchoMachineRuntimeSnapshot.FluidContract.empty();
    }

    private static EchoMachineRuntimeSnapshot.ProcessContract process(
            ContainerData data,
            MachineState ashfallState,
            EchoMachineState state,
            String machinePath,
            int wearPercent
    ) {
        int progress = switch (machinePath) {
            case "micro_generator", "thermal_array" -> dataValue(data, 2);
            default -> dataValue(data, 0);
        };
        int maxProgress = switch (machinePath) {
            case "micro_generator", "thermal_array" -> dataValue(data, 3);
            default -> dataValue(data, 1);
        };
        int percent = maxProgress <= 0 ? 0 : Math.min(100, (int) Math.round(progress * 100.0D / maxProgress));
        boolean active = state == EchoMachineState.ACTIVE || progress > 0;
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("contract", "ashfall_machine_process");
        attributes.put("ashfallState", ashfallState.name());
        attributes.put("wearPercent", Integer.toString(wearPercent));
        if ("micro_generator".equals(machinePath) || "thermal_array".equals(machinePath)) {
            attributes.put("processSource", "burn_time_remaining");
        } else if ("thermal_burner".equals(machinePath)) {
            attributes.put("processSource", "burn_progress");
        } else if ("battery_bank".equals(machinePath)) {
            attributes.put("processSource", "stored_charge");
        } else if ("factory_controller".equals(machinePath)) {
            attributes.put("processSource", "factory_control");
        } else if ("atmospheric_scrubber".equals(machinePath)) {
            attributes.put("processSource", "safe_zone_scrubbing");
        } else if ("contaminant_condenser".equals(machinePath)) {
            attributes.put("processSource", "toxic_block_condensing");
        } else if (data != null && data.getCount() > 2) {
            attributes.put("hasPowerFlag", Boolean.toString(data.get(2) != 0));
        }
        return new EchoMachineRuntimeSnapshot.ProcessContract(
                ashfallState.getDisplayName(),
                active,
                progress,
                maxProgress,
                percent,
                recipeContract(machinePath),
                attributes
        );
    }

    private static EchoMachineRuntimeSnapshot.SideConfigurationContract side(BlockEntity machine, String machinePath) {
        if (!(machine instanceof HopperHandler handler)) {
            return new EchoMachineRuntimeSnapshot.SideConfigurationContract(
                    "No item side routing",
                    List.of(),
                    List.of(),
                    List.of(),
                    Map.of("contract", "ashfall_no_hopper_handler")
            );
        }
        List<String> sideSlots = new ArrayList<>();
        for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            sideSlots.addAll(slotRefs(handler, direction, machinePath));
        }
        return new EchoMachineRuntimeSnapshot.SideConfigurationContract(
                "Ashfall hopper routing",
                slotRefs(handler, Direction.UP, machinePath),
                slotRefs(handler, Direction.DOWN, machinePath),
                List.copyOf(sideSlots.stream().distinct().toList()),
                Map.of("contract", "ashfall_hopper_handler")
        );
    }

    private static EchoMachineRuntimeSnapshot.UpgradeContract upgrades(MachineInventory inventory, String machinePath) {
        List<EchoMachineRuntimeSnapshot.SlotSnapshot> slots = new ArrayList<>();
        if (inventory != null) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                String itemId = stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (itemId.contains("machine_upgrade")) {
                    slots.add(new EchoMachineRuntimeSnapshot.SlotSnapshot(
                            i,
                            "upgrade",
                            itemId,
                            stack.getHoverName().getString(),
                            stack.getCount(),
                            true,
                            Map.of("sourceSlot", Integer.toString(i))
                    ));
                }
            }
        }
        return new EchoMachineRuntimeSnapshot.UpgradeContract(
                slots.size(),
                slots.size(),
                slots,
                Map.of(
                        "contract", "ashfall_optional_upgrade_items",
                        "machinePath", machinePath
                )
        );
    }

    private static EchoMachineRuntimeSnapshot.SavedStateContract savedState(
            MachineInventory inventory,
            BlockEntity machine,
            ContainerData data,
            MachineState state,
            int wearPercent
    ) {
        List<String> keys = new ArrayList<>();
        if (inventory != null) {
            keys.add("inventory");
        }
        if (data != null && data.getCount() > 0) {
            keys.add("progress");
        }
        if (machine instanceof IEnergyStorage) {
            keys.add("energy");
        }
        keys.add("machine_wear_saved_data");
        return new EchoMachineRuntimeSnapshot.SavedStateContract(
                "ashfall_block_entity_nbt_v1",
                keys,
                inventory == null ? 0 : inventory.getContainerSize(),
                machine instanceof IEnergyStorage energy ? energy.getEnergyStored() : 0,
                wearPercent,
                state.getDisplayName(),
                machine instanceof HopperHandler ? "Ashfall hopper routing" : "No item side routing",
                false,
                Map.of(
                        "blockEntity", machine.getClass().getSimpleName(),
                        "dataCount", Integer.toString(data == null ? 0 : data.getCount())
                )
        );
    }

    private static EchoMachineIntegrationRefs integrationRefs(String machinePath) {
        return new EchoMachineIntegrationRefs(
                null,
                null,
                null,
                List.of(
                        EchoFeatureId.of("echomachinecore.runtime_snapshot"),
                        EchoFeatureId.of("echoterminal.machine_status"),
                        EchoFeatureId.of("echolens.machine_scan"),
                        EchoFeatureId.of("echologisticsnetwork.item_routing"),
                        EchoFeatureId.of("echoashfallprotocol.power_network")
                ),
                List.of(),
                Map.of("machinePath", machinePath)
        );
    }

    private static EchoMachineRecipeBinding recipeBinding(EchoMachineRuntimeSnapshot snapshot) {
        return new EchoMachineRecipeBinding(
                EchoRecipeId.of(snapshot.process().recipeContract()),
                null,
                "primary",
                0,
                List.of(),
                Map.of("machineBlockId", snapshot.machineBlockId())
        );
    }

    private static List<EchoMachineAutomationHook> automationHooks(EchoMachineRuntimeSnapshot snapshot) {
        List<EchoFeatureId> features = List.of(EchoFeatureId.of("echomachinecore.runtime_snapshot"));
        List<EchoMachineAutomationHook> hooks = new ArrayList<>();
        if (snapshot.inventory().totalSlots() > 0) {
            hooks.add(hook("item_input", EchoMachineAutomationHookKind.ITEM_INPUT, features, "Ashfall hopper input"));
            hooks.add(hook("item_output", EchoMachineAutomationHookKind.ITEM_OUTPUT, features, "Ashfall hopper output"));
        }
        if (snapshot.energy().capacity() > 0) {
            hooks.add(hook("power_input", EchoMachineAutomationHookKind.POWER_INPUT, features, snapshot.energy().resourceId()));
        }
        hooks.add(hook("maintenance", EchoMachineAutomationHookKind.MAINTENANCE, features, "scrap_metal_service"));
        hooks.add(hook("remote_status", EchoMachineAutomationHookKind.REMOTE_STATUS, features, snapshot.state().serializedName()));
        return List.copyOf(hooks);
    }

    private static EchoMachineAutomationHook hook(
            String id,
            EchoMachineAutomationHookKind kind,
            List<EchoFeatureId> features,
            String detail
    ) {
        return new EchoMachineAutomationHook(id, kind, null, features, true, Map.of("detail", detail));
    }

    private static MachineInventory inventory(BlockEntity machine) {
        if (machine instanceof HopperHandler handler) {
            return handler.getInventory();
        }
        Object value = invokeNoArg(machine, "getInventory");
        return value instanceof MachineInventory inventory ? inventory : null;
    }

    private static ContainerData data(BlockEntity machine) {
        try {
            Field field = machine.getClass().getField("data");
            Object value = field.get(machine);
            return value instanceof ContainerData containerData ? containerData : null;
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            return null;
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static int wearPercent(Level level, BlockPos pos) {
        if (level == null) {
            return 0;
        }
        return Math.round(new MachineWearData(level).getWearPercent(pos) * 100.0F);
    }

    private static int dataValue(ContainerData data, int index) {
        return data != null && index >= 0 && index < data.getCount() ? Math.max(0, data.get(index)) : 0;
    }

    private static int countItems(Container inventory, String itemPath) {
        if (inventory == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals(itemPath)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static String recipeContract(String machinePath) {
        return switch (machinePath) {
            case "water_purifier" -> EchoAshfallProtocol.MODID + ":water_purification";
            case "scrap_press" -> EchoAshfallProtocol.MODID + ":scrap_pressing";
            case "ore_grinder" -> EchoAshfallProtocol.MODID + ":substrate_grinding";
            case "filter_workbench" -> EchoAshfallProtocol.MODID + ":filter_crafting";
            case "hand_recycler" -> EchoAshfallProtocol.MODID + ":hand_recycling";
            case "thermal_burner" -> EchoAshfallProtocol.MODID + ":thermal_burning";
            case "micro_generator" -> EchoAshfallProtocol.MODID + ":micro_power_generation";
            case "thermal_array" -> EchoAshfallProtocol.MODID + ":thermal_array_generation";
            case "battery_bank" -> EchoAshfallProtocol.MODID + ":battery_storage";
            case "factory_controller" -> EchoAshfallProtocol.MODID + ":factory_control";
            case "radiation_cleanser" -> EchoAshfallProtocol.MODID + ":radiation_cleansing";
            case "atmospheric_scrubber" -> EchoAshfallProtocol.MODID + ":atmospheric_scrubbing";
            case "contaminant_condenser" -> EchoAshfallProtocol.MODID + ":contaminant_condensing";
            case "isotope_refiner" -> EchoAshfallProtocol.MODID + ":isotope_refining";
            case "crystalline_synthesizer" -> EchoAshfallProtocol.MODID + ":crystalline_synthesis";
            default -> EchoAshfallProtocol.MODID + ":machine_runtime";
        };
    }

    private static String slotRole(BlockEntity machine, String machinePath, int slot) {
        return switch (detailedSlotRole(machinePath, slot)) {
            case "water_input", "fuel_input", "primary_input", "secondary_input", "tertiary_input", "input" -> "input";
            case "filter", "catalyst" -> "filter";
            case "output", "byproduct" -> "output";
            case "battery" -> "battery";
            case "upgrade" -> "upgrade";
            default -> {
                if (machine instanceof HopperHandler handler && outputSlot(handler, slot)) {
                    yield "output";
                }
                if (machine instanceof HopperHandler handler && inputSlot(handler, slot)) {
                    yield "input";
                }
                yield "internal";
            }
        };
    }

    private static String detailedSlotRole(String machinePath, int slot) {
        return switch (machinePath) {
            case "water_purifier" -> switch (slot) {
                case 0 -> "water_input";
                case 1 -> "filter";
                case 2 -> "output";
                case 3 -> "battery";
                default -> "internal";
            };
            case "filter_workbench" -> switch (slot) {
                case 0 -> "primary_input";
                case 1 -> "secondary_input";
                case 2 -> "tertiary_input";
                case 3 -> "output";
                case 4 -> "battery";
                default -> "internal";
            };
            case "hand_recycler" -> switch (slot) {
                case 0 -> "input";
                case 1 -> "output";
                case 2 -> "upgrade";
                case 3 -> "battery";
                default -> "internal";
            };
            case "thermal_burner" -> switch (slot) {
                case 0 -> "fuel_input";
                case 1 -> "byproduct";
                case 2 -> "battery";
                default -> "internal";
            };
            case "scrap_press" -> switch (slot) {
                case 0 -> "input";
                case 1 -> "output";
                case 2 -> "battery";
                default -> "internal";
            };
            case "ore_grinder" -> switch (slot) {
                case 0, 1 -> "input";
                case 2 -> "output";
                case 3 -> "byproduct";
                case 4 -> "battery";
                default -> "internal";
            };
            case "micro_generator" -> switch (slot) {
                case 0 -> "fuel_input";
                case 1 -> "battery";
                default -> "internal";
            };
            case "battery_bank" -> slot == 0 ? "battery" : "internal";
            case "radiation_cleanser" -> switch (slot) {
                case 0 -> "input";
                case 1 -> "filter";
                case 2 -> "output";
                case 3 -> "battery";
                default -> "internal";
            };
            case "isotope_refiner" -> switch (slot) {
                case 0 -> "input";
                case 1 -> "catalyst";
                case 2, 3 -> "output";
                case 4 -> "battery";
                default -> "internal";
            };
            case "crystalline_synthesizer" -> switch (slot) {
                case 0, 1 -> "input";
                case 2 -> "catalyst";
                case 3 -> "output";
                case 4 -> "battery";
                default -> "internal";
            };
            case "thermal_array" -> switch (slot) {
                case 0, 1, 2 -> "fuel_input";
                case 3 -> "battery";
                default -> "internal";
            };
            default -> "internal";
        };
    }

    private static List<String> slotRefs(HopperHandler handler, Direction direction, String machinePath) {
        List<String> refs = new ArrayList<>();
        for (int slot : handler.getInputSlots(direction)) {
            refs.add("input:" + slot + ":" + detailedSlotRole(machinePath, slot));
        }
        for (int slot : handler.getOutputSlots(direction)) {
            refs.add("output:" + slot + ":" + detailedSlotRole(machinePath, slot));
        }
        return List.copyOf(refs.stream().distinct().toList());
    }

    private static boolean inputSlot(HopperHandler handler, int slot) {
        return Arrays.stream(Direction.values()).anyMatch(direction -> contains(handler.getInputSlots(direction), slot));
    }

    private static boolean outputSlot(HopperHandler handler, int slot) {
        return Arrays.stream(Direction.values()).anyMatch(direction -> contains(handler.getOutputSlots(direction), slot));
    }

    private static boolean contains(int[] slots, int slot) {
        for (int candidate : slots) {
            if (candidate == slot) {
                return true;
            }
        }
        return false;
    }
}
