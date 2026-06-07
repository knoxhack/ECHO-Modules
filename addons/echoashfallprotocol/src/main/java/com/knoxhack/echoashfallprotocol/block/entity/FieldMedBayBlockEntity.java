package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime;
import com.knoxhack.echoashfallprotocol.capability.EnergyStorage;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import com.knoxhack.echoashfallprotocol.survival.MutationData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Field Med Bay - heals player and removes negative effects.
 * Consumes power to provide healing and mutation suppression.
 */
public class FieldMedBayBlockEntity extends BlockEntity implements IEnergyStorage {
    
    private static final int RADIUS = 8;
    private static final int POWER_COST_PER_HEAL = 20;
    private static final int ENERGY_CAPACITY = 2_000;
    private static final int ENERGY_TRANSFER = 128;
    private static final int HEAL_INTERVAL = 40; // Every 2 seconds
    private static final int WEAR_ACCUMULATION_INTERVAL = 300; // Every 15 seconds
    private static final float HEAL_AMOUNT = 2.0f;
    
    private int tickCounter = 0;
    private int wearCounter = 0;
    private long nextFeedbackTick = 0L;
    private boolean isActive = false;
    private MachineWearData wearData;
    private final EnergyStorage energyStorage = new EnergyStorage(ENERGY_CAPACITY, ENERGY_TRANSFER, ENERGY_TRANSFER);
    
    public FieldMedBayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIELD_MED_BAY.get(), pos, state);
    }
    
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level != null) {
            this.wearData = new MachineWearData(level);
        }
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, FieldMedBayBlockEntity entity) {
        entity.tickCounter++;
        long gameTime = level.getGameTime();

        if (entity.wearData != null && entity.wearData.isJammed(pos)) {
            entity.isActive = false;
            entity.notifyNearby(level, pos, "\u00A7c[ECHO-7]\u00A7r Field Med Bay jammed. Clear wear before treatment.", gameTime);
            return;
        }
        
        // Check power availability
        if (!EnergyAccess.hasLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_HEAL)) {
            entity.isActive = false;
            entity.notifyNearby(level, pos, "\u00A7e[ECHO-7]\u00A7r Field Med Bay idle: missing power.", gameTime);
            return;
        }
        
        // Only heal periodically
        if (entity.tickCounter < HEAL_INTERVAL) {
            return;
        }
        entity.tickCounter = 0;
        
        // Try to consume power for this heal cycle
        if (!EnergyAccess.tryConsumeLocalOrNetworkPower(entity, level, pos, POWER_COST_PER_HEAL)) {
            entity.isActive = false;
            return;
        }
        
        entity.isActive = true;
        
        // Find nearby players and heal them
        AABB area = new AABB(pos).inflate(RADIUS);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, area);
        
        for (ServerPlayer player : players) {
            // Heal health
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(HEAL_AMOUNT);
            }
            
            // Remove negative potion effects
            entity.removeNegativeEffects(player);
            
            // Suppress mutation side effects temporarily
            entity.suppressMutationEffects(player);
            MutationData mutationData = player.getData(ModAttachments.MUTATION_DATA.get());
            AshfallAdapterCoreHazardRuntime.medBayUsed(
                    player,
                    pos,
                    entity.getEnergyStored(),
                    mutationData.getMutationCount());
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
    
    private void removeNegativeEffects(ServerPlayer player) {
        // Remove poison, weakness, blindness, hunger, mining fatigue
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.MINING_FATIGUE);
        player.removeEffect(MobEffects.WITHER);
        
        // Clear fire if burning
        if (player.isOnFire()) {
            player.clearFire();
        }
    }
    
    private void suppressMutationEffects(ServerPlayer player) {
        // Give temporary resistance to counteract mutation side effects
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, false, false));
        
        // Access mutation data and temporarily suppress side effects
        MutationData mutationData = player.getData(ModAttachments.MUTATION_DATA);
        if (mutationData.getMutationCount() > 0) {
            // Grant temporary regeneration to offset mutation instability
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
        }
    }

    private void notifyNearby(Level level, BlockPos pos, String message, long gameTime) {
        if (gameTime < nextFeedbackTick) {
            return;
        }
        nextFeedbackTick = gameTime + 100L;
        AABB area = new AABB(pos).inflate(RADIUS);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            player.sendSystemMessage(Component.literal(message), true);
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
