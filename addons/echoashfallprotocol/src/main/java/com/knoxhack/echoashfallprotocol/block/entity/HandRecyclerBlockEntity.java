package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.HandRecyclerBlock;
import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.gameplay.MachineGameplayHelper;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echoashfallprotocol.block.menu.HandRecyclerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Hand Recycler Block Entity — converts scrap into useful materials.
 */
public class HandRecyclerBlockEntity extends BlockEntity implements MenuProvider, HopperHandler, IEnergyStorage {
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int UPGRADE_SLOT = 2;
    public static final int BATTERY_SLOT = 3;
    private static final int DEFAULT_PROCESS_TIME = 100;
    private static final int ENERGY_CAPACITY = 1_000;
    private static final int ENERGY_TRANSFER = 64;

    private final MachineInventory inventory = new MachineInventory(4, this::setChanged);
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);

    private int progress = 0;
    private int maxProgress = DEFAULT_PROCESS_TIME;
    private int upgradeLevel = 0;
    private boolean hasPower = false;
    private boolean isJammed = false;

    public final ContainerData data = new ContainerData() {
        private int wearPercent = 0;
        
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> hasPower ? 1 : 0;
                case 3 -> isJammed ? 1 : 0;
                case 4 -> hasSpeedUpgrade() ? 1 : 0;
                case 5 -> wearPercent;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> hasPower = value != 0;
                case 3 -> isJammed = value != 0;
                case 5 -> wearPercent = value;
            }
        }
        
        public void setWearPercent(int wear) {
            this.wearPercent = wear;
        }

        @Override
        public int getCount() { return 6; }
    };

    private static final Map<Item, Item> RECIPES = Map.of(
            ModItems.SCRAP_METAL.get(), ModItems.MACHINE_CASING.get(),
            ModItems.SCRAP_WIRE.get(), ModItems.CIRCUIT_BOARD.get(),
            ModItems.SCRAP_CIRCUIT.get(), ModItems.ENERGY_CELL.get(),
            ModItems.SCRAP_PLASTIC.get(), ModItems.FILTRATION_MEMBRANE.get()
    );

    public static Map<Item, Item> getRecipes() {
        return RECIPES;
    }

    public HandRecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HAND_RECYCLER.get(), pos, state);
        recalculateMaxProgress();
    }

    public MachineInventory getInventory() { return inventory; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.EchoAshfallProtocol.hand_recycler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new HandRecyclerMenu(containerId, playerInventory, this, data);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HandRecyclerBlockEntity entity) {
        boolean wasActive = state.getValue(HandRecyclerBlock.ACTIVE);
        boolean isProcessing = false;
        EnergyAccess.dischargeBatteryToStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);
        entity.recalculateMaxProgress();

        // Check machine wear and jam status
        MachineWearData wearData = new MachineWearData(level);
        entity.isJammed = wearData.isJammed(pos);
        
        // Sync wear percentage to client for UI display
        int wearPercent = (int)(wearData.getWearPercent(pos) * 100);
        entity.data.set(5, wearPercent);

        // Check for power (requires 1 energy per tick to operate)
        entity.hasPower = EnergyAccess.tryConsumeLocalOrNetworkPower(entity, level, pos,
                entity.getAdjustedPowerCost(level, pos));
        boolean hasPower = entity.hasPower;
        boolean isJammed = entity.isJammed;

        if (entity.hasRecipe() && !isJammed && hasPower) {
            isProcessing = true;
            entity.progress++;
            entity.setChanged();

            // Accumulate wear during processing (Standard Duty: 1 wear per 20 ticks)
            if (entity.progress % 20 == 0) { // Every 20 ticks
                wearData.addWear(pos, 1, level.getRandom());

                // Check for jam at high wear
                if (wearData.checkJamChance(pos, level.getRandom())) {
                    // Machine jammed - notify nearby players
                    level.players().forEach(player -> {
                        if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 100) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§c[ECHO-7]§r Hand Recycler has jammed! Repair needed."));
                        }
                    });
                }
            }

            if (entity.progress >= entity.maxProgress) {
                entity.craftItem();
                AshfallAdapterCoreMachineRuntimeHost.outputCreated(
                        level, pos, entity, entity.inventory.getStackInSlot(OUTPUT_SLOT).copy());
                entity.progress = 0;
                // Try to chain output to adjacent machines
                entity.tryPushOutputToNeighbors(level, pos);
            }
        } else {
            entity.progress = 0;
        }

        if (wasActive != isProcessing) {
            level.setBlockAndUpdate(pos, state.setValue(HandRecyclerBlock.ACTIVE, isProcessing));
        }
    }

    public boolean hasRecipe() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) return false;

        Item outputItem = RECIPES.get(input.getItem());
        if (outputItem == null) return false;

        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        return output.isEmpty() || (output.is(outputItem) && output.getCount() < output.getMaxStackSize());
    }

    private void craftItem() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        Item outputItem = RECIPES.get(input.getItem());
        if (outputItem == null) return;

        input.shrink(1);
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, new ItemStack(outputItem, 1));
        } else {
            output.grow(1);
        }

        // Play completion sound and particles
        if (level != null) {
            level.playSound(null, worldPosition,
                    net.minecraft.sounds.SoundEvents.BLASTFURNACE_FIRE_CRACKLE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, 1.2f);
            // Spawn smoke particles
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.SMOKE,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5,
                        3, 0.1, 0.1, 0.1, 0.01);
            }
        }
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
        ValueOutput invOut = output.child("inventory");
        inventory.serialize(invOut);
        output.putInt("progress", progress);
        output.putInt("upgradeLevel", upgradeLevel);
        output.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(inv -> inventory.deserialize(inv));
        progress = input.getIntOr("progress", 0);
        upgradeLevel = input.getIntOr("upgradeLevel", 0);
        energyStorage.setEnergyStored(input.getIntOr("energy", 0));
        recalculateMaxProgress();
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

    private void recalculateMaxProgress() {
        int base = Config.RECYCLER_PROCESS_TIME.get();
        if (base <= 0) {
            base = DEFAULT_PROCESS_TIME;
        }
        // Apply speed/overclock modules. Efficiency changes FE cost instead.
        if (hasSpeedUpgrade()) {
            base = (int)(base * 0.6);
        }
        if (hasOverclockUpgrade()) {
            base = (int)(base * 0.4);
        }
        base = Math.max(1, base / (1 + upgradeLevel));
        if (level != null) {
            maxProgress = MachineGameplayHelper.getAdjustedProcessTime(level, worldPosition, base);
        } else {
            maxProgress = base;
        }
    }

    public boolean hasSpeedUpgrade() {
        ItemStack upgrade = inventory.getStackInSlot(UPGRADE_SLOT);
        return upgrade.is(ModItems.MACHINE_UPGRADE_SPEED.get());
    }

    public boolean hasEfficiencyUpgrade() {
        ItemStack upgrade = inventory.getStackInSlot(UPGRADE_SLOT);
        return upgrade.is(ModItems.MACHINE_UPGRADE_EFFICIENCY.get());
    }

    public boolean hasOverclockUpgrade() {
        ItemStack upgrade = inventory.getStackInSlot(UPGRADE_SLOT);
        return upgrade.is(ModItems.MACHINE_UPGRADE_OVERCLOCK.get());
    }

    private int getAdjustedPowerCost(Level level, BlockPos pos) {
        int baseCost = hasOverclockUpgrade() ? 3 : 1;
        if (hasEfficiencyUpgrade()) {
            baseCost = Math.max(1, baseCost / 2);
        }
        return MachineGameplayHelper.getAdjustedPowerCost(level, pos, baseCost);
    }

    public net.minecraft.world.item.Item getUpgradeSlotItem() {
        return ModItems.MACHINE_UPGRADE_SPEED.get();
    }

    public boolean isUpgradeItem(ItemStack stack) {
        return stack.is(ModItems.MACHINE_UPGRADE_SPEED.get())
                || stack.is(ModItems.MACHINE_UPGRADE_EFFICIENCY.get())
                || stack.is(ModItems.MACHINE_UPGRADE_OVERCLOCK.get());
    }

    // === HopperHandler Implementation ===
    @Override
    public int[] getInputSlots(Direction side) {
        // Accept input from any side
        return new int[]{INPUT_SLOT};
    }

    @Override
    public int[] getOutputSlots(Direction side) {
        // Output from bottom only
        return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack) {
        if (slot == INPUT_SLOT) {
            return RECIPES.containsKey(stack.getItem());
        }
        if (slot == UPGRADE_SLOT) {
            return isUpgradeItem(stack);
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slot) {
        return slot == OUTPUT_SLOT;
    }
}
