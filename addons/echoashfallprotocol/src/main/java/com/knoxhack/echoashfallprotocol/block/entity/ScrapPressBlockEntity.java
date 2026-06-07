package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.recipe.ScrapPressRecipe;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.block.menu.ScrapPressMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Scrap Press - compresses 9 scrap metal into 1 scrap block for storage.
 * Requires power to operate. Has 2-slot inventory (input + output).
 */
public class ScrapPressBlockEntity extends BlockEntity implements MenuProvider, HopperHandler, IEnergyStorage {
    
    private static final int POWER_COST_PER_TICK = 1;
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    public static final int BATTERY_SLOT = 2;
    private static final int ENERGY_CAPACITY = 1_500;
    private static final int ENERGY_TRANSFER = 128;
    
    private final MachineInventory inventory = new MachineInventory(3, this::setChanged);
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);
    
    private int processingProgress = 0;
    private int currentRecipeTime = 40; // Default processing time
    private boolean isProcessing = false;
    private MachineWearData wearData;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> processingProgress;
                case 1 -> currentRecipeTime;
                case 2 -> level != null && EnergyAccess.hasLocalOrNetworkPower(ScrapPressBlockEntity.this, level, worldPosition, POWER_COST_PER_TICK) ? 1 : 0;
                case 3 -> isProcessing ? 1 : 0;
                case 4 -> wearData != null && wearData.isJammed(worldPosition) ? 1 : 0;
                case 5 -> wearData != null ? (int) (wearData.getWearPercent(worldPosition) * 100) : 0;
                case 6 -> energyStorage.getEnergyStored();
                case 7 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                processingProgress = value;
            } else if (index == 1) {
                currentRecipeTime = value;
            } else if (index == 3) {
                isProcessing = value != 0;
            } else if (index == 6) {
                energyStorage.setEnergyStored(value);
            }
        }

        @Override
        public int getCount() {
            return 8;
        }
    };
    
    public ScrapPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCRAP_PRESS.get(), pos, state);
    }
    
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null) {
            this.wearData = new MachineWearData(level);
        }
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, ScrapPressBlockEntity entity) {
        if (level.isClientSide()) return;
        EnergyAccess.dischargeBatteryToStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);
        if (entity.wearData != null && entity.wearData.isJammed(pos)) {
            entity.isProcessing = false;
            entity.processingProgress = 0;
            return;
        }

        // Check if we have power
        if (!EnergyAccess.hasLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_TICK)) {
            entity.isProcessing = false;
            entity.processingProgress = 0;
            return;
        }

        // Find matching recipe
        ItemStack input = entity.inventory.getStackInSlot(INPUT_SLOT);
        ScrapPressRecipe recipe = ScrapPressRecipe.findRecipe(input);

        if (recipe == null) {
            entity.isProcessing = false;
            entity.processingProgress = 0;
            return;
        }

        // Check output space
        ItemStack output = entity.inventory.getStackInSlot(OUTPUT_SLOT);
        ItemStack result = recipe.createOutputStack();
        if (!output.isEmpty() && !ItemStack.isSameItem(output, result)) {
            entity.isProcessing = false;
            entity.processingProgress = 0;
            return;
        }
        if (!output.isEmpty() && output.getCount() + result.getCount() > output.getMaxStackSize()) {
            entity.isProcessing = false;
            entity.processingProgress = 0;
            return;
        }

        // Try to consume power
        if (!EnergyAccess.tryConsumeLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_TICK)) {
            entity.isProcessing = false;
            return;
        }

        // Process
        entity.isProcessing = true;
        entity.currentRecipeTime = recipe.processingTime();
        entity.processingProgress++;

        if (entity.processingProgress >= entity.currentRecipeTime) {
            // Complete processing
            input.shrink(recipe.inputCount());

            if (output.isEmpty()) {
                entity.inventory.setStackInSlot(OUTPUT_SLOT, result.copy());
            } else {
                output.grow(result.getCount());
            }
            AshfallAdapterCoreMachineRuntimeHost.outputCreated(level, pos, entity, result.copy());

            entity.processingProgress = 0;
            
            // Try to chain output to adjacent machines
            entity.tryPushOutputToNeighbors(level, pos);

            // Add wear
            if (entity.wearData != null) {
                entity.wearData.addWear(pos, 2, level.getRandom());
                if (entity.wearData.isJammed(pos)) {
                    entity.isProcessing = false;
                    entity.processingProgress = 0;
                }
                
                // Visual feedback based on wear level
                double wearPercent = entity.wearData.getWearPercent(pos);
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    if (wearPercent > 0.50 && level.getRandom().nextFloat() < 0.08f) {
                        // Spark particles when wear > 50%
                        double px = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8;
                        double py = pos.getY() + 0.5 + level.getRandom().nextDouble() * 0.5;
                        double pz = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8;
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, 0, 0, 0, 0.1);
                    }
                    if (wearPercent > 0.75 && level.getRandom().nextFloat() < 0.12f) {
                        // Smoke particles when wear > 75%
                        double px = pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.6;
                        double py = pos.getY() + 0.8;
                        double pz = pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.6;
                        serverLevel.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0, 0.05, 0, 0.02);
                    }
                    if (wearPercent > 0.90 && level.getRandom().nextFloat() < 0.03f) {
                        // Machine groan sound when approaching jam threshold
                        serverLevel.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3f, 0.5f);
                    }
                }
            }
        }

        entity.setChanged();
    }
    
    public MachineInventory getInventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Scrap Press");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ScrapPressMenu(containerId, playerInventory, this, data);
    }
    
    public boolean isProcessing() {
        return isProcessing;
    }
    
    public int getProcessingProgress() {
        return processingProgress;
    }
    
    public int getProcessingTime() {
        return currentRecipeTime;
    }
    
    public MachineWearData getWearData() {
        return wearData;
    }

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
        output.putInt("processingProgress", processingProgress);
        output.putInt("currentRecipeTime", currentRecipeTime);
        output.putBoolean("isProcessing", isProcessing);
        output.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(inventory::deserialize);
        processingProgress = input.getIntOr("processingProgress", 0);
        currentRecipeTime = input.getIntOr("currentRecipeTime", 40);
        isProcessing = input.getBooleanOr("isProcessing", false);
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
        // Accept scrap metal from any side
        return new int[]{INPUT_SLOT};
    }

    @Override
    public int[] getOutputSlots(Direction side) {
        // Extract scrap blocks from bottom only
        return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack) {
        // Only accept scrap metal in input slot
        if (slot == INPUT_SLOT) {
            return stack.is(com.knoxhack.echoashfallprotocol.registry.ModItems.SCRAP_METAL.get());
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slot) {
        // Only extract from output slot
        return slot == OUTPUT_SLOT;
    }
}
