package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NexusCapacitorBlockEntity extends BlockEntity implements IEnergyStorage {
    public static final int CAPACITY = 100_000;
    public static final int MAX_TRANSFER = 1024;

    private final EnergyStorage energyStorage = new EnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER);

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

    public NexusCapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEXUS_CAPACITOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NexusCapacitorBlockEntity entity) {
        if (level.isClientSide()) return;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            if (EnergyAccess.transferFromStorageToBlock(entity, level, neighborPos, dir.getOpposite(), MAX_TRANSFER) > 0) {
                entity.setChanged();
            }
            if (entity.energyStorage.getEnergyStored() <= 0) break;
        }
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
    @Override public void setEnergyStored(int energy) { energyStorage.setEnergyStored(energy); setChanged(); }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.setEnergyStored(input.getIntOr("energy", 0));
    }
}
