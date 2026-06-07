package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.menu.DeepCoreMinerMenu;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Deep Core Miner Block Entity - Endgame resource generator.
 */
public class DeepCoreMinerBlockEntity extends BlockEntity implements MenuProvider, HopperHandler, IEnergyStorage {
    public static final int OUTPUT_SLOT = 0;
    public static final int BATTERY_SLOT = 1;
    public static final int TOTAL_TICKS = 800;
    public static final int POWER_PER_TICK = 40;
    public static final int MIN_Y_LEVEL = -32;
    private static final int ENERGY_CAPACITY = 12_000;
    private static final int ENERGY_TRANSFER = 512;

    private final MachineInventory inventory = new MachineInventory(2, this::setChanged);
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);
    private int progress = 0;
    private int wearLevel = 0;
    private boolean jammed = false;
    private final Random random = new Random();

    private static final Item[] POSSIBLE_OUTPUTS = {
            ModItems.DENSE_ALLOY_CHUNK.get(),
            ModItems.GEM_FRAGMENT.get(),
            ModItems.CRYSTAL_DUST.get(),
            Items.REDSTONE,
            Items.LAPIS_LAZULI
    };

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> TOTAL_TICKS;
                case 2 -> wearLevel;
                case 3 -> jammed ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 2 -> wearLevel = v;
                case 3 -> jammed = v != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public DeepCoreMinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEEP_CORE_MINER.get(), pos, state);
    }

    @Override
    public MachineInventory getInventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Deep Core Miner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DeepCoreMinerMenu(id, inv, this, data);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DeepCoreMinerBlockEntity entity) {
        MachineWearData wearData = new MachineWearData(level);
        EnergyAccess.dischargeBatteryToStorage(entity.inventory.getStackInSlot(BATTERY_SLOT), entity);
        entity.wearLevel = (int) (wearData.getWearPercent(pos) * 100);
        entity.jammed = wearData.isJammed(pos);

        if (pos.getY() > MIN_Y_LEVEL || entity.jammed) {
            return;
        }

        if (!EnergyAccess.tryConsumeLocalOrNetworkPower(entity, level, pos,
                MachineGameplayHelper.getAdjustedPowerCost(level, pos, POWER_PER_TICK))) {
            return;
        }

        ItemStack output = entity.inventory.getStackInSlot(OUTPUT_SLOT);
        if (!output.isEmpty() && output.getCount() >= output.getMaxStackSize()) {
            return;
        }

        entity.progress++;
        entity.setChanged();

        if (entity.progress % 40 == 0) {
            wearData.addWear(pos, 2, level.getRandom());
            if (wearData.checkJamChance(pos, level.getRandom())) {
                entity.jammed = true;
                level.players().forEach(player -> {
                    if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 144) {
                        player.sendSystemMessage(Component.literal(
                                "§c[ECHO-7]§r Deep Core Miner stalled under heavy load. Repairs required."));
                    }
                });
                return;
            }
        }

        if (entity.progress >= MachineGameplayHelper.getAdjustedProcessTime(level, pos, TOTAL_TICKS)) {
            ItemStack minedOutput = entity.generateOutput();
            if (!minedOutput.isEmpty()) {
                AshfallAdapterCoreMachineRuntimeHost.outputCreated(level, pos, entity, minedOutput);
            }
            entity.progress = 0;
            entity.tryPushOutputToNeighbors(level, pos);
        }
    }

    private ItemStack generateOutput() {
        Item outputItem = POSSIBLE_OUTPUTS[random.nextInt(POSSIBLE_OUTPUTS.length)];
        ItemStack current = inventory.getStackInSlot(OUTPUT_SLOT);

        if (current.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, new ItemStack(outputItem, 1));
        } else if (current.is(outputItem)) {
            current.grow(1);
        } else {
            return ItemStack.EMPTY;
        }
        setChanged();
        return new ItemStack(outputItem, 1);
    }

    private void tryPushOutputToNeighbors(Level level, BlockPos pos) {
        MachineChainingHelper.tryPushOutput(level, pos, inventory, OUTPUT_SLOT);
    }

    @Override
    public int[] getInputSlots(Direction side) {
        return new int[]{};
    }

    @Override
    public int[] getOutputSlots(Direction side) {
        return new int[]{OUTPUT_SLOT};
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canExtractItem(int slot) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        inventory.serialize(out.child("inventory"));
        out.putInt("progress", progress);
        out.putInt("wear", wearLevel);
        out.putBoolean("jammed", jammed);
        out.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        in.child("inventory").ifPresent(inv -> inventory.deserialize(inv));
        progress = in.getIntOr("progress", 0);
        wearLevel = in.getIntOr("wear", 0);
        jammed = in.getBooleanOr("jammed", false);
        energyStorage.setEnergyStored(in.getIntOr("energy", 0));
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
}
