package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.PowerNodeBlock;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Power Node Block Entity — relay node for the grid.
 * Buffers 2000 FE, pulls from adjacent sources and pushes to adjacent consumers.
 * Each activated node also contributes to unlocking Nexus Core access.
 */
public class PowerNodeBlockEntity extends BlockEntity implements IEnergyStorage {

    public static final int CAPACITY = 4000;
    public static final int MAX_TRANSFER = 512;

    private final EnergyStorage energyStorage = new EnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER);

    private boolean activated = false;
    private long activationTime = 0L;

    public PowerNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_NODE.get(), pos, state);
    }

    public void activate() {
        this.activated = true;
        this.activationTime = (level != null) ? level.getGameTime() : 0L;
        setChanged();
    }

    public boolean isActivated() { return activated; }
    public long getActivationTime() { return activationTime; }
    public float getEnergyPercent() { return energyStorage.getEnergyPercent(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerNodeBlockEntity entity) {
        if (level.isClientSide()) return;

        boolean wasActive = state.getValue(PowerNodeBlock.ACTIVE);

        // Pull power from adjacent generators/batteries into our buffer
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (!(neighbor instanceof PowerNodeBlockEntity)) {
                int space = CAPACITY - entity.energyStorage.getEnergyStored();
                if (space > 0) {
                    int pulled = EnergyAccess.extractBlockEnergy(level, neighborPos, dir.getOpposite(),
                            Math.min(MAX_TRANSFER, space));
                    entity.energyStorage.receiveEnergy(pulled, false);
                    if (pulled > 0) entity.setChanged();
                }
            }
        }

        // Push power to adjacent consumers (other nodes and machines)
        if (entity.energyStorage.getEnergyStored() > 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                if (!(neighbor instanceof PowerNodeBlockEntity)) {
                    int moved = EnergyAccess.transferFromStorageToBlock(entity, level, neighborPos,
                            dir.getOpposite(), MAX_TRANSFER);
                    if (moved > 0) entity.setChanged();
                }
            }

            // Relay to adjacent power nodes (chain connectivity)
            for (Direction dir : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
                if (neighbor instanceof PowerNodeBlockEntity otherNode
                        && otherNode.energyStorage.getEnergyStored() < CAPACITY / 2) {
                    int balance = (entity.energyStorage.getEnergyStored() - otherNode.energyStorage.getEnergyStored()) / 2;
                    if (balance > 0) {
                        int moved = entity.energyStorage.extractEnergy(Math.min(MAX_TRANSFER, balance), false);
                        otherNode.energyStorage.receiveEnergy(moved, false);
                        entity.setChanged();
                        otherNode.setChanged();
                    }
                }
            }
        }

        boolean isActive = entity.energyStorage.getEnergyStored() > 0;
        if (wasActive != isActive) {
            level.setBlockAndUpdate(pos, state.setValue(PowerNodeBlock.ACTIVE, isActive));
        }
    }

    @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
    @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
    @Override public int receiveEnergy(int amount, boolean simulate) { return energyStorage.receiveEnergy(amount, simulate); }
    @Override public int extractEnergy(int amount, boolean simulate) { return energyStorage.extractEnergy(amount, simulate); }
    @Override public boolean canReceive() { return true; }
    @Override public boolean canExtract() { return energyStorage.getEnergyStored() > 0; }
    @Override public void setEnergyStored(int energy) { energyStorage.setEnergyStored(energy); }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energyStorage.getEnergyStored());
        output.putBoolean("activated", activated);
        output.putLong("activationTime", activationTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyStorage.setEnergyStored(input.getIntOr("energy", 0));
        activated = input.getBooleanOr("activated", false);
        activationTime = input.getLongOr("activationTime", 0L);
    }
}
