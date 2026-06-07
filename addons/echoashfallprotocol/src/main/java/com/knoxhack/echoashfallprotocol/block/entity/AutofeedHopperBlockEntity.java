package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Autofeed Hopper - auto-feeds nearby hungry players.
 * Consumes power to feed players when their hunger drops below threshold.
 */
public class AutofeedHopperBlockEntity extends BlockEntity implements IEnergyStorage {
    
    private static final int RADIUS = 8;
    private static final int POWER_COST_PER_FEED = 10;
    private static final int ENERGY_CAPACITY = 1_000;
    private static final int ENERGY_TRANSFER = 64;
    private static final int HUNGER_THRESHOLD = 10; // Feed when hunger <= 10
    private static final int FEED_AMOUNT = 4; // Restore 4 hunger points
    private static final int FEED_INTERVAL = 60; // Every 3 seconds
    private static final int WEAR_ACCUMULATION_INTERVAL = 400; // Every 20 seconds
    
    private int tickCounter = 0;
    private int wearCounter = 0;
    private boolean isActive = false;
    private MachineWearData wearData;
    private int lastFeedTick = 0;
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);
    
    public AutofeedHopperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOFEED_HOPPER.get(), pos, state);
    }
    
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null) {
            this.wearData = new MachineWearData(level);
        }
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, AutofeedHopperBlockEntity entity) {
        entity.tickCounter++;

        if (entity.wearData != null && entity.wearData.isJammed(pos)) {
            entity.isActive = false;
            return;
        }
        
        // Check power
        if (!EnergyAccess.hasLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_FEED)) {
            entity.isActive = false;
            return;
        }
        
        // Only feed periodically
        if (entity.tickCounter - entity.lastFeedTick < FEED_INTERVAL) {
            return;
        }
        
        // Find hungry players in range
        AABB area = new AABB(pos).inflate(RADIUS);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, area);
        
        boolean fedAnyone = false;
        for (ServerPlayer player : players) {
            FoodData foodData = player.getFoodData();
            if (foodData.getFoodLevel() <= HUNGER_THRESHOLD) {
                // Try to consume power for this feed
                if (EnergyAccess.tryConsumeLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_FEED)) {
                    // Feed the player
                    int newFoodLevel = Math.min(20, foodData.getFoodLevel() + FEED_AMOUNT);
                    foodData.setFoodLevel(newFoodLevel);
                    fedAnyone = true;
                }
            }
        }
        
        if (fedAnyone) {
            entity.isActive = true;
            entity.lastFeedTick = entity.tickCounter;
            
            // Accumulate wear
            entity.wearCounter++;
            if (entity.wearCounter >= WEAR_ACCUMULATION_INTERVAL) {
                entity.wearCounter = 0;
                if (entity.wearData != null) {
                    entity.wearData.addWear(pos, 1, level.getRandom());
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
