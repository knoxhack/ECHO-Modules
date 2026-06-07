package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Atmospheric Scrubber - reduces radiation accumulation for nearby players.
 * Consumes power to scrub radiation from players in range.
 */
public class AtmosphericScrubberBlockEntity extends BlockEntity implements IEnergyStorage {
    
    private static final int POWER_COST_PER_TICK = 2;
    private static final int ENERGY_CAPACITY = 2_000;
    private static final int ENERGY_TRANSFER = 128;
    private static final float RADIATION_DECAY_RATE = 0.5f; // Radiation reduced per tick
    private static final int WEAR_ACCUMULATION_INTERVAL = 200; // Every 10 seconds
    
    private int wearCounter = 0;
    private long nextFeedbackTick = 0L;
    private boolean isActive = false;
    private MachineWearData wearData;
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);
    
    public AtmosphericScrubberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERIC_SCRUBBER.get(), pos, state);
    }
    
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null) {
            this.wearData = new MachineWearData(level);
        }
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, AtmosphericScrubberBlockEntity entity) {
        long gameTime = level.getGameTime();
        if (entity.wearData != null && entity.wearData.isJammed(pos)) {
            entity.isActive = false;
            entity.notifyNearby(level, pos, "\u00A7c[ECHO-7]\u00A7r Atmospheric Scrubber jammed. Repair before sheltering here.", gameTime);
            return;
        }

        // Check power availability
        if (!EnergyAccess.hasLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_TICK)) {
            entity.isActive = false;
            entity.notifyNearby(level, pos, "\u00A7e[ECHO-7]\u00A7r Scrubber field offline: missing power.", gameTime);
            return;
        }
        
        // Try to consume power
        if (!EnergyAccess.tryConsumeLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_TICK)) {
            entity.isActive = false;
            return;
        }
        
        entity.isActive = true;
        entity.notifyNearby(level, pos, "\u00A7b[ECHO-7]\u00A7r Scrubber field active: toxic air suppressed, radiation decay improved.", gameTime);
        
        // Register with safe zone manager
        if (level instanceof ServerLevel serverLevel) {
            ScrubberSafeZoneManager.registerScrubber(serverLevel, pos);
        }
        
        // Find nearby players and reduce their radiation
        AABB area = new AABB(pos).inflate(Config.SCRUBBER_SAFE_ZONE_RADIUS.get());
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, area);
        
        for (ServerPlayer player : players) {
            SurvivalData data = player.getData(ModAttachments.SURVIVAL_DATA.get());
            if (data.getRadiationLevel() > 0) {
                float beforeRadiation = data.getRadiationLevel();
                data.decayRadiation(RADIATION_DECAY_RATE);
                player.setData(ModAttachments.SURVIVAL_DATA.get(), data);
                player.syncData(ModAttachments.SURVIVAL_DATA.get());
                AshfallAdapterCoreHazardRuntime.atmosphericScrubberUsed(
                        player,
                        pos,
                        beforeRadiation,
                        data.getRadiationLevel(),
                        Config.SCRUBBER_SAFE_ZONE_RADIUS.get());
            }
        }
        
        // Accumulate wear periodically
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
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public MachineWearData getWearData() {
        return wearData;
    }
    
    public float getWearPercent() {
        return wearData != null ? wearData.getWearPercent(worldPosition) : 0f;
    }
    
    public boolean isJammed() {
        return wearData != null && wearData.isJammed(worldPosition);
    }

    private void notifyNearby(Level level, BlockPos pos, String message, long gameTime) {
        if (gameTime < nextFeedbackTick) {
            return;
        }
        nextFeedbackTick = gameTime + 200L;
        AABB area = new AABB(pos).inflate(Config.SCRUBBER_SAFE_ZONE_RADIUS.get());
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            player.sendSystemMessage(Component.literal(message), true);
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
