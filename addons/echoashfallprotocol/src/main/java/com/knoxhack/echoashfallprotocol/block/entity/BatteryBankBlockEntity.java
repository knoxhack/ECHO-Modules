package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echo.adaptercore.EchoEnergyHandler;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Battery Bank - Stores and distributes power to adjacent machines.
 * Capacity: 10,000 energy units
 * Transfer rate: 100 per tick
 */
public class BatteryBankBlockEntity extends BlockEntity implements IEnergyStorage {
    
    public static final int CAPACITY = 10000;
    public static final int MAX_TRANSFER = 100;
    public static final int BATTERY_SLOT = 0;
    
    private final EnergyStorage energyStorage = new EnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER);
    private final MachineInventory inventory = new MachineInventory(1, this::setChanged);
    
    public final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> CAPACITY;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) energyStorage.setEnergyStored(value);
        }
        @Override public int getCount() { return 2; }
    };

    public BatteryBankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY_BANK.get(), pos, state);
    }
    
    /**
     * Server tick - distribute power to adjacent machines.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BatteryBankBlockEntity entity) {
        if (level.isClientSide()) return;

        entity.balanceInsertedBattery();
        
        // Distribute power to adjacent consumers
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (EnergyAccess.transferFromStorageToBlock(entity, level, neighborPos, dir.getOpposite(), MAX_TRANSFER) > 0) {
                entity.setChanged();
            }
        }
    }

    private void balanceInsertedBattery() {
        ItemStack stack = inventory.getStackInSlot(BATTERY_SLOT);
        EchoEnergyHandler battery = EnergyAccess.getItemEnergy(stack);
        if (battery == null || battery.capacity() <= 0L) {
            return;
        }

        float bankPercent = getMaxEnergyStored() <= 0 ? 0.0f : (float) getEnergyStored() / getMaxEnergyStored();
        float batteryPercent = (float) battery.amount() / battery.capacity();
        int moved = 0;
        if (bankPercent > batteryPercent) {
            moved = EnergyAccess.chargeBatteryFromStorage(stack, this);
        } else if (batteryPercent > bankPercent) {
            moved = EnergyAccess.dischargeBatteryToStorage(stack, this);
        }
        if (moved > 0) {
            setChanged();
        }
    }
    
    @Override
    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }
    
    @Override
    public int getMaxEnergyStored() {
        return energyStorage.getMaxEnergyStored();
    }
    
    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        int received = energyStorage.receiveEnergy(amount, simulate);
        if (received > 0 && !simulate) setChanged();
        return received;
    }
    
    @Override
    public int extractEnergy(int amount, boolean simulate) {
        int extracted = energyStorage.extractEnergy(amount, simulate);
        if (extracted > 0 && !simulate) setChanged();
        return extracted;
    }
    
    @Override
    public boolean canReceive() {
        return true;
    }
    
    @Override
    public boolean canExtract() {
        return true;
    }
    
    @Override
    public void setEnergyStored(int energy) {
        energyStorage.setEnergyStored(energy);
        setChanged();
    }
    
    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public MachineInventory getInventory() {
        return inventory;
    }
    
    public float getEnergyPercent() {
        return energyStorage.getEnergyPercent();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
        output.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(inventory::deserialize);
        energyStorage.setEnergyStored(input.getIntOr("energy", 0));
    }
}
