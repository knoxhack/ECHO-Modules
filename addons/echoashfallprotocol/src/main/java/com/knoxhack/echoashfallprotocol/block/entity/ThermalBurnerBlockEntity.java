package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.ThermalBurnerBlock;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.gameplay.MachineGameplayHelper;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.nativebridge.AshfallAdapterCoreMachineRuntimeHost;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echoashfallprotocol.block.menu.ThermalBurnerMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Thermal Burner — burns any item for energy + ash byproduct.
 */
public class ThermalBurnerBlockEntity extends BlockEntity implements MenuProvider, HopperHandler, IEnergyStorage {
    private static final int INPUT_SLOT = 0;
    private static final int ASH_OUTPUT_SLOT = 1;
    public static final int BATTERY_SLOT = 2;
    private static final int BURN_TIME = 40;

    private final MachineInventory inventory = new MachineInventory(3, this::setChanged);

    private int burnProgress = 0;
    private int maxBurnProgress = BURN_TIME;
    private int energy = 0;
    private int maxEnergy = 1000;
    private int itemsBurned = 0;
    private int wearPercent = 0;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnProgress;
                case 1 -> maxBurnProgress;
                case 2 -> energy;
                case 3 -> maxEnergy;
                case 4 -> wearPercent;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> burnProgress = value;
                case 1 -> maxBurnProgress = value;
                case 2 -> energy = value;
                case 3 -> maxEnergy = value;
                case 4 -> wearPercent = value;
            }
        }

        @Override
        public int getCount() { return 5; }
    };

    public ThermalBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMAL_BURNER.get(), pos, state);
    }

    public MachineInventory getInventory() { return inventory; }
    public int getEnergy() { return energy; }
    public int getMaxEnergy() { return maxEnergy; }

    public int extractEnergy(int amount) {
        int extracted = Math.min(energy, amount);
        energy -= extracted;
        setChanged();
        return extracted;
    }

    @Override
    public int getEnergyStored() { return energy; }

    @Override
    public int getMaxEnergyStored() { return maxEnergy; }

    @Override
    public int receiveEnergy(int amount, boolean simulate) { return 0; }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        int extracted = Math.min(energy, amount);
        if (!simulate) {
            energy -= extracted;
            setChanged();
        }
        return extracted;
    }

    @Override
    public boolean canReceive() { return false; }

    @Override
    public boolean canExtract() { return energy > 0; }

    @Override
    public void setEnergyStored(int energy) {
        this.energy = Math.max(0, Math.min(maxEnergy, energy));
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.EchoAshfallProtocol.thermal_burner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ThermalBurnerMenu(containerId, playerInventory, this, data);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ThermalBurnerBlockEntity entity) {
        boolean wasActive = state.getValue(ThermalBurnerBlock.ACTIVE);
        boolean isBurning = false;
        EnergyAccess.chargeBatteryFromStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);
        entity.maxBurnProgress = MachineGameplayHelper.getAdjustedProcessTime(level, pos, BURN_TIME);

        // Check machine wear and jam status
        MachineWearData wearData = new MachineWearData(level);
        boolean isJammed = wearData.isJammed(pos);
        entity.wearPercent = (int)(wearData.getWearPercent(pos) * 100);

        if (!isJammed && !entity.inventory.getStackInSlot(INPUT_SLOT).isEmpty() && entity.energy < entity.maxEnergy) {
            isBurning = true;
            entity.burnProgress++;
            entity.setChanged();

            // Accumulate wear during burning (2 wear per cycle - higher heat = more wear)
            if (entity.burnProgress % 20 == 0) {
                wearData.addWear(pos, 2, level.getRandom());

                // Check for jam at high wear
                if (wearData.checkJamChance(pos, level.getRandom())) {
                    level.players().forEach(player -> {
                        if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 100) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§c[ECHO-7]§r Thermal Burner has jammed! Repair needed."));
                        }
                    });
                }
            }

            if (entity.burnProgress >= entity.maxBurnProgress) {
                entity.inventory.getStackInSlot(INPUT_SLOT).shrink(1);
                entity.energy = Math.min(entity.maxEnergy, entity.energy + 50);
                entity.itemsBurned++;
                entity.burnProgress = 0;

                if (entity.itemsBurned >= 4) {
                    entity.itemsBurned = 0;
                    boolean ashProduced = false;
                    ItemStack ashSlot = entity.inventory.getStackInSlot(ASH_OUTPUT_SLOT);
                    if (ashSlot.isEmpty()) {
                        entity.inventory.setStackInSlot(ASH_OUTPUT_SLOT, new ItemStack(ModItems.ASH.get(), 1));
                        ashProduced = true;
                    } else if (ashSlot.is(ModItems.ASH.get()) && ashSlot.getCount() < ashSlot.getMaxStackSize()) {
                        ashSlot.grow(1);
                        ashProduced = true;
                    }
                    
                    // Try to chain ash output to adjacent machines
                    entity.tryPushAshToNeighbors(level, pos);
                    if (ashProduced) {
                        AshfallAdapterCoreMachineRuntimeHost.outputCreated(
                                level,
                                pos,
                                entity,
                                new ItemStack(ModItems.ASH.get(), 1));
                    }
                }
            }
        } else {
            entity.burnProgress = 0;
        }

        if (wasActive != isBurning) {
            level.setBlockAndUpdate(pos, state.setValue(ThermalBurnerBlock.ACTIVE, isBurning));
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput invOut = output.child("inventory");
        inventory.serialize(invOut);
        output.putInt("burnProgress", burnProgress);
        output.putInt("energy", energy);
        output.putInt("itemsBurned", itemsBurned);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(inv -> inventory.deserialize(inv));
        burnProgress = input.getIntOr("burnProgress", 0);
        energy = input.getIntOr("energy", 0);
        itemsBurned = input.getIntOr("itemsBurned", 0);
    }

    // === HopperHandler Implementation ===
    @Override
    public int[] getInputSlots(Direction side) {
        // Accept fuel from any side
        return new int[]{INPUT_SLOT};
    }

    @Override
    public int[] getOutputSlots(Direction side) {
        // Extract ash from bottom only
        return side == Direction.DOWN ? new int[]{ASH_OUTPUT_SLOT} : new int[]{};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack) {
        // Accept any item in input slot (any item can be burned)
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean canExtractItem(int slot) {
        // Only extract ash from output slot
        return slot == ASH_OUTPUT_SLOT;
    }

    /**
     * Try to push ash to adjacent machines (machine chaining).
     */
    private void tryPushAshToNeighbors(Level level, BlockPos pos) {
        MachineChainingHelper.tryPushOutput(level, pos, inventory, ASH_OUTPUT_SLOT);
    }
}
