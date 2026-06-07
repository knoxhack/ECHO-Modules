package com.knoxhack.echopowergrid.block.entity;

import com.knoxhack.echopowergrid.api.EchoEnergyStorage;
import com.knoxhack.echopowergrid.api.EchoPowerNode;
import com.knoxhack.echopowergrid.api.EchoPowerNodeType;
import com.knoxhack.echopowergrid.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BatteryBlockEntity extends BlockEntity implements EchoEnergyStorage, EchoPowerNode {
    private long capacity;
    private long maxInput;
    private long maxOutput;
    private long energy;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY.get(), pos, state);
        if (state.getBlock() instanceof com.knoxhack.echopowergrid.block.BatteryBlock bat) {
            this.capacity = bat.getCapacity();
            this.maxInput = bat.getMaxInput();
            this.maxOutput = bat.getMaxOutput();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatteryBlockEntity bat) {
        // Battery logic is driven by network manager; minimal ticking here
    }

    public void onUse(Player player) {
        player.sendSystemMessage(Component.literal("ECHO GRID // Battery Bank: " + energy + "/" + capacity + " EP"));
        player.sendSystemMessage(Component.literal("  Input: " + maxInput + " EP/t | Output: " + maxOutput + " EP/t"));
    }

    public void setEnergyStored(long amount) {
        energy = Math.max(0L, Math.min(capacity, amount));
        setChanged();
    }

    @Override
    public long getEnergyStored() { return energy; }

    @Override
    public long getMaxEnergyStored() { return capacity; }

    @Override
    public long receiveEnergy(long amount, boolean simulate) {
        long space = capacity - energy;
        long received = Math.min(amount, Math.min(space, maxInput));
        if (!simulate) {
            energy += received;
            setChanged();
        }
        return received;
    }

    @Override
    public long extractEnergy(long amount, boolean simulate) {
        long extracted = Math.min(amount, Math.min(energy, maxOutput));
        if (!simulate) {
            energy -= extracted;
            setChanged();
        }
        return extracted;
    }

    @Override
    public long getMaxInput() { return maxInput; }

    @Override
    public long getMaxOutput() { return maxOutput; }

    @Override
    public boolean canReceive() { return true; }

    @Override
    public boolean canExtract() { return true; }

    @Override
    public BlockPos getNodePos() { return worldPosition; }

    @Override
    public ResourceKey<Level> getDimension() { return level != null ? level.dimension() : null; }

    @Override
    public EchoPowerNodeType getNodeType() { return EchoPowerNodeType.STORAGE; }

    @Override
    public long getGenerationPerTick() { return 0; }

    @Override
    public long getDemandPerTick() { return 0; }

    @Override
    public long getStoredEnergy() { return energy; }

    @Override
    public long getCapacity() { return capacity; }

    @Override
    public long getTransferLimit() { return Math.max(maxInput, maxOutput); }

    @Override
    public boolean isOnline() { return true; }

    @Override
    public boolean isOverloaded() { return false; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", energy);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = input.getLongOr("Energy", 0);
    }
}
