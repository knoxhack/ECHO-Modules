package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.block.menu.FilterWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Filter Workbench - crafts and upgrades filter cartridges.
 * Provides 4-slot crafting inventory with power requirement for upgrades.
 */
public class FilterWorkbenchBlockEntity extends BlockEntity implements MenuProvider, HopperHandler, IEnergyStorage {
    
    private static final int CRAFTING_SLOTS = 4;
    public static final int BATTERY_SLOT = 4;
    private static final int POWER_COST_PER_CRAFT = 30;
    private static final int WEAR_ACCUMULATION_INTERVAL = 300; // Every 15 seconds
    private static final int ENERGY_CAPACITY = 1_500;
    private static final int ENERGY_TRANSFER = 128;
    
    private final MachineInventory inventory = new MachineInventory(CRAFTING_SLOTS + 1, this::setChanged);
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);
    
    private int wearCounter = 0;
    private MachineWearData wearData;
    private int craftingProgress = 0;
    private static final int CRAFTING_TIME = 20; // 1 second

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> craftingProgress;
                case 1 -> CRAFTING_TIME;
                case 2 -> level != null && EnergyAccess.hasLocalOrNetworkPower(FilterWorkbenchBlockEntity.this, level, worldPosition, POWER_COST_PER_CRAFT / CRAFTING_TIME) ? 1 : 0;
                case 3 -> wearData != null && wearData.isJammed(worldPosition) ? 1 : 0;
                case 4 -> wearData != null ? (int) (wearData.getWearPercent(worldPosition) * 100) : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                craftingProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };
    
    public FilterWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILTER_WORKBENCH.get(), pos, state);
    }
    
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null) {
            this.wearData = new MachineWearData(level);
        }
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, FilterWorkbenchBlockEntity entity) {
        if (level.isClientSide()) return;
        EnergyAccess.dischargeBatteryToStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);
        if (entity.wearData != null && entity.wearData.isJammed(pos)) {
            entity.craftingProgress = 0;
            return;
        }

        // Check if powered
        if (!EnergyAccess.hasLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_CRAFT / CRAFTING_TIME)) {
            entity.craftingProgress = 0;
            return;
        }

        // Check for valid crafting recipe and attempt crafting
        RecipeResult result = checkRecipe(entity);

        if (result != null && EnergyAccess.tryConsumeLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_CRAFT / CRAFTING_TIME)) {
            entity.craftingProgress++;

            if (entity.craftingProgress >= CRAFTING_TIME) {
                // Complete craft
                completeCraft(entity, result);
                AshfallAdapterCoreMachineRuntimeHost.outputCreated(level, pos, entity, result.output().copy());
                entity.craftingProgress = 0;
                
                // Try to chain output to adjacent machines
                entity.tryPushOutputToNeighbors(level, pos);

                // Accumulate wear
                entity.wearCounter++;
                if (entity.wearCounter >= WEAR_ACCUMULATION_INTERVAL) {
                    entity.wearCounter = 0;
                    if (entity.wearData != null) {
                        entity.wearData.addWear(pos, 1, level.getRandom());
                        if (entity.wearData.isJammed(pos)) {
                            entity.craftingProgress = 0;
                        }
                    }
                }
            }
        } else {
            entity.craftingProgress = 0;
        }
    }

    /**
     * Represents a crafting recipe result with inputs consumed and output produced.
     */
    private record RecipeResult(ItemStack output, int[] inputSlots, int[] consumeAmounts) {}

    /**
     * Checks all filter cartridge recipes and returns the first matching one.
     * Recipe Slots: 0=primary input, 1=secondary, 2=tertiary, 3=output
     */
    private static RecipeResult checkRecipe(FilterWorkbenchBlockEntity entity) {
        ItemStack slot0 = entity.inventory.getStackInSlot(0);
        ItemStack slot1 = entity.inventory.getStackInSlot(1);
        ItemStack slot2 = entity.inventory.getStackInSlot(2);
        ItemStack outputSlot = entity.inventory.getStackInSlot(3);

        // ELITE Filter: Advanced Filter + Machine Casing + Energy Cell → Elite Filter
        if (slot0.is(ModItems.FILTER_CARTRIDGE_ADVANCED.get()) &&
            slot1.is(ModItems.MACHINE_CASING.get()) &&
            slot2.is(ModItems.ENERGY_CELL.get()) &&
            canOutput(outputSlot, ModItems.FILTER_CARTRIDGE_ELITE.get())) {
            return new RecipeResult(
                new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get()),
                new int[]{0, 1, 2},
                new int[]{1, 1, 1}
            );
        }

        // ADVANCED Filter: Basic Filter + Circuit Board + Energy Cell → Advanced Filter
        if (slot0.is(ModItems.FILTER_CARTRIDGE_BASIC.get()) &&
            slot1.is(ModItems.CIRCUIT_BOARD.get()) &&
            slot2.is(ModItems.ENERGY_CELL.get()) &&
            canOutput(outputSlot, ModItems.FILTER_CARTRIDGE_ADVANCED.get())) {
            return new RecipeResult(
                new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get()),
                new int[]{0, 1, 2},
                new int[]{1, 1, 1}
            );
        }

        // BASIC Filter: Scrap Plastic + Filtration Membrane → Basic Filter
        if (slot0.is(ModItems.SCRAP_PLASTIC.get()) &&
            slot1.is(ModItems.FILTRATION_MEMBRANE.get()) &&
            slot2.isEmpty() &&
            canOutput(outputSlot, ModItems.FILTER_CARTRIDGE_BASIC.get())) {
            return new RecipeResult(
                new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get()),
                new int[]{0, 1},
                new int[]{2, 1}  // 2 plastic + 1 membrane
            );
        }

        return null;
    }

    /**
     * Checks if the output slot can accept the given item.
     */
    private static boolean canOutput(ItemStack outputSlot, net.minecraft.world.item.Item item) {
        if (outputSlot.isEmpty()) return true;
        if (!outputSlot.is(item)) return false;
        return outputSlot.getCount() < outputSlot.getMaxStackSize();
    }

    /**
     * Completes a craft by consuming inputs and producing output.
     */
    private static void completeCraft(FilterWorkbenchBlockEntity entity, RecipeResult result) {
        // Consume inputs
        for (int i = 0; i < result.inputSlots().length; i++) {
            int slot = result.inputSlots()[i];
            int amount = result.consumeAmounts()[i];
            entity.inventory.getStackInSlot(slot).shrink(amount);
        }

        // Produce output
        ItemStack outputSlot = entity.inventory.getStackInSlot(3);
        if (outputSlot.isEmpty()) {
            entity.inventory.setStackInSlot(3, result.output().copy());
        } else if (outputSlot.is(result.output().getItem())) {
            outputSlot.grow(1);
        }

        entity.setChanged();
    }
    
    public MachineInventory getInventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Filter Workbench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FilterWorkbenchMenu(containerId, playerInventory, this, data);
    }
    
    public int getCraftingProgress() {
        return craftingProgress;
    }
    
    public int getCraftingTime() {
        return CRAFTING_TIME;
    }
    
    public MachineWearData getWearData() {
        return wearData;
    }

    private static final int OUTPUT_SLOT = 3;

    /**
     * Try to push output items to adjacent machines (machine chaining).
     */
    private void tryPushOutputToNeighbors(Level level, BlockPos pos) {
        MachineChainingHelper.tryPushOutput(level, pos, inventory, OUTPUT_SLOT);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
        output.putInt("craftingProgress", craftingProgress);
        output.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(inventory::deserialize);
        craftingProgress = input.getIntOr("craftingProgress", 0);
        energyStorage.setEnergyStored(input.getIntOr("energy", 0));
    }

    @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
    @Override public int receiveEnergy(int amount, boolean simulate) {
        int received = energyStorage.receiveEnergy(amount, simulate);
        if (received > 0 && !simulate) setChanged();
        return received;
    }
    @Override public int extractEnergy(int amount, boolean simulate) {
        int extracted = energyStorage.extractEnergy(amount, simulate);
        if (extracted > 0 && !simulate) setChanged();
        return extracted;
    }
    @Override public boolean canReceive() { return true; }
    @Override public boolean canExtract() { return energyStorage.getEnergyStored() > 0; }
    @Override public void setEnergyStored(int energy) {
        energyStorage.setEnergyStored(energy);
        setChanged();
    }

    // === HopperHandler Implementation ===
    @Override
    public int[] getInputSlots(Direction side) {
        // Accept materials from any side (slots 0-2)
        return new int[]{0, 1, 2};
    }

    @Override
    public int[] getOutputSlots(Direction side) {
        // Extract filters from bottom only (slot 3)
        return side == Direction.DOWN ? new int[]{3} : new int[]{};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack) {
        // Slot 0: Primary material (plastic, basic filter, advanced filter)
        // Slot 1: Secondary material (membrane, circuit board, casing)
        // Slot 2: Tertiary material (empty, energy cell, energy cell)
        if (slot >= 0 && slot <= 2) {
            return true; // Accept any item, recipe validation happens in tick
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slot) {
        // Only extract from output slot (slot 3)
        return slot == 3;
    }
}
