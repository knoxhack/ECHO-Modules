package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.PowerCableBlock;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Power Cable Block Entity - basic tier wired power distribution.
 * Capacity: 1000 FE, Transfer Rate: 50 FE/t
 */
public class PowerCableBlockEntity extends BlockEntity implements IEnergyStorage {
    private int energy = 0;
    
    public PowerCableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_CABLE.get(), pos, state);
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerCableBlockEntity entity) {
        if (level.isClientSide()) return;
        
        boolean wasActive = state.getValue(PowerCableBlock.ACTIVE);
        boolean isActive = entity.energy > 0;
        
        // Step 1: Pull power from adjacent generators/storages (that aren't cables)
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            
            if (!(neighbor instanceof PowerCableBlockEntity) && PowerNetwork.canSupplyNetwork(neighbor) && entity.energy < entity.getCapacity()) {
                int space = entity.getCapacity() - entity.energy;
                int extracted = EnergyAccess.extractBlockEnergy(level, neighborPos, dir.getOpposite(),
                        Math.min(entity.getMaxTransfer(), space));
                entity.energy += extracted;
                if (extracted > 0) {
                    entity.setChanged();
                }
            }
        }
        
        // Step 2: Push power to adjacent consumers (that aren't cables)
        if (entity.energy > 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                
                if (!(neighbor instanceof PowerCableBlockEntity) || PowerNetwork.isRelay(neighbor)) {
                    int received = EnergyAccess.insertBlockEnergy(level, neighborPos, dir.getOpposite(),
                            Math.min(entity.getMaxTransfer(), entity.energy));
                    entity.energy -= received;
                    if (received > 0) {
                        entity.setChanged();
                    }
                    if (entity.energy <= 0) break;
                }
            }
        }
        
        // Step 3: Balance power with adjacent cables (load sharing)
        if (entity.energy > entity.getCapacity() / 2) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                
                if (neighbor instanceof PowerCableBlockEntity otherCable) {
                    // Only push if we have significantly more energy
                    int diff = entity.energy - otherCable.energy;
                    if (diff > 100) {
                        int toBalance = Math.min(Math.min(entity.getMaxTransfer(), otherCable.getMaxTransfer()) / 2, diff / 2);
                        int pushed = Math.min(toBalance, entity.energy);
                        otherCable.energy += pushed;
                        entity.energy -= pushed;
                        otherCable.setChanged();
                        entity.setChanged();
                    }
                }
            }
        }
        
        // Update visual state
        if (wasActive != isActive) {
            level.setBlockAndUpdate(pos, state.setValue(PowerCableBlock.ACTIVE, isActive));
        }
    }
    
    // === IEnergyStorage Implementation ===
    
    @Override
    public int getEnergyStored() { return energy; }
    
    @Override
    public int getMaxEnergyStored() { return getCapacity(); }
    
    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        int received = Math.min(getCapacity() - energy, Math.min(getMaxTransfer(), amount));
        if (!simulate) {
            energy += received;
            setChanged();
        }
        return received;
    }
    
    @Override
    public int extractEnergy(int amount, boolean simulate) {
        int extracted = Math.min(energy, Math.min(getMaxTransfer(), amount));
        if (!simulate) {
            energy -= extracted;
            setChanged();
        }
        return extracted;
    }
    
    @Override
    public boolean canReceive() { return energy < getCapacity(); }
    
    @Override
    public boolean canExtract() { return energy > 0; }
    
    @Override
    public void setEnergyStored(int energy) {
        this.energy = Math.max(0, Math.min(getCapacity(), energy));
        setChanged();
    }

    public int getCapacity() {
        return getBlockState().getBlock() instanceof PowerCableBlock cable
                ? cable.getCapacity()
                : PowerCableBlock.BASIC_CAPACITY;
    }

    public int getMaxTransfer() {
        return getBlockState().getBlock() instanceof PowerCableBlock cable
                ? cable.getTransferRate()
                : PowerCableBlock.BASIC_TRANSFER;
    }
    
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energy);
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = input.getIntOr("energy", 0);
    }
}
