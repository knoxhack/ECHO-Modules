package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Contaminant Condenser - converts toxic puddles to sand.
 * Requires power and processes adjacent toxic blocks.
 */
public class ContaminantCondenserBlockEntity extends BlockEntity implements IEnergyStorage {
    
    private static final int POWER_COST_PER_OPERATION = 50;
    private static final int ENERGY_CAPACITY = 2_000;
    private static final int ENERGY_TRANSFER = 128;
    private static final int PROCESS_RADIUS = 3;
    private static final int PROCESS_INTERVAL = 100; // Every 5 seconds
    private static final int WEAR_ACCUMULATION_INTERVAL = 200; // Every 10 seconds
    
    private int tickCounter = 0;
    private int wearCounter = 0;
    private boolean isActive = false;
    private int blocksProcessed = 0;
    private MachineWearData wearData;
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);
    
    public ContaminantCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTAMINANT_CONDENSER.get(), pos, state);
    }
    
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null) {
            this.wearData = new MachineWearData(level);
        }
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, ContaminantCondenserBlockEntity entity) {
        entity.tickCounter++;

        if (entity.wearData != null && entity.wearData.isJammed(pos)) {
            entity.isActive = false;
            return;
        }
        
        // Check power
        if (!EnergyAccess.hasLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_OPERATION)) {
            entity.isActive = false;
            return;
        }
        
        // Only process periodically
        if (entity.tickCounter % PROCESS_INTERVAL != 0) {
            return;
        }
        
        // Try to consume power
        if (!EnergyAccess.tryConsumeLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_OPERATION)) {
            entity.isActive = false;
            return;
        }
        
        // Scan for toxic blocks and convert them
        boolean processedAny = false;
        for (int x = -PROCESS_RADIUS; x <= PROCESS_RADIUS && !processedAny; x++) {
            for (int y = -1; y <= 2 && !processedAny; y++) {
                for (int z = -PROCESS_RADIUS; z <= PROCESS_RADIUS && !processedAny; z++) {
                    BlockPos targetPos = pos.offset(x, y, z);
                    if (targetPos.equals(pos)) continue; // Skip self
                    
                    BlockState targetState = level.getBlockState(targetPos);
                    // Check if it's a toxic block (toxic_puddle from the mod)
                    if (targetState.is(ModBlocks.TOXIC_PUDDLE.get())) {
                        // Convert toxic puddle to sand (repurposing the toxic material)
                        level.setBlock(targetPos, Blocks.SAND.defaultBlockState(), 3);
                        processedAny = true;
                        entity.blocksProcessed++;
                        break;
                    }
                }
            }
        }
        
        if (processedAny) {
            entity.isActive = true;
            
            // Accumulate wear
            entity.wearCounter++;
            if (entity.wearCounter >= WEAR_ACCUMULATION_INTERVAL) {
                entity.wearCounter = 0;
                if (entity.wearData != null) {
                    entity.wearData.addWear(pos, 2, level.getRandom());
                    if (entity.wearData.isJammed(pos)) {
                        entity.isActive = false;
                    }
                }
            }
        } else {
            entity.isActive = false;
        }
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public int getBlocksProcessed() {
        return blocksProcessed;
    }
    
    public MachineWearData getWearData() {
        return wearData;
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
