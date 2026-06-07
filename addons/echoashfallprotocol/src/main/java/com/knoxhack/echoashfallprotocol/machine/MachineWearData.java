package com.knoxhack.echoashfallprotocol.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks machine wear data per-block in the world.
 * Machines accumulate wear over use and can jam at high wear levels.
 */
public class MachineWearData {
    
    public static final int MAX_WEAR = 1000;
    public static final int JAM_THRESHOLD = 800; // 80% wear = chance to jam
    public static final int BROKEN_THRESHOLD = 950; // 95% wear = high failure rate
    
    private final MachineWearSavedData savedData;
    private final Map<String, Integer> fallbackWear = new HashMap<>();
    private final Map<String, Boolean> fallbackJammed = new HashMap<>();
    
    public MachineWearData(Level level) {
        this.savedData = level instanceof ServerLevel serverLevel ? MachineWearSavedData.get(serverLevel) : null;
    }
    
    public int getWear(BlockPos pos) {
        if (savedData != null) {
            return savedData.getWear(pos);
        }
        return fallbackWear.getOrDefault(getKey(pos), 0);
    }
    
    public void addWear(BlockPos pos, int amount, RandomSource random) {
        int current = getWear(pos);
        int newWear = Math.min(MAX_WEAR, current + amount);
        setWear(pos, newWear);
        
        // Check for jam at threshold
        if (newWear >= JAM_THRESHOLD && !isJammed(pos)) {
            if (random.nextFloat() < 0.3f) { // 30% chance to jam when crossing threshold
                setJammed(pos, true);
            }
        }
    }
    
    public void repair(BlockPos pos, int amount) {
        int current = getWear(pos);
        int newWear = Math.max(0, current - amount);
        setWear(pos, newWear);
        setJammed(pos, false); // Repair clears jam
    }
    
    public boolean isJammed(BlockPos pos) {
        if (savedData != null) {
            return savedData.isJammed(pos);
        }
        return fallbackJammed.getOrDefault(getKey(pos), false);
    }
    
    public void setJammed(BlockPos pos, boolean jammed) {
        if (savedData != null) {
            savedData.setJammed(pos, jammed);
        } else {
            fallbackJammed.put(getKey(pos), jammed);
        }
    }

    public void setWear(BlockPos pos, int wear) {
        int clamped = Math.max(0, Math.min(MAX_WEAR, wear));
        if (savedData != null) {
            savedData.setWear(pos, clamped);
        } else {
            fallbackWear.put(getKey(pos), clamped);
        }
    }
    
    public float getWearPercent(BlockPos pos) {
        return (float) getWear(pos) / MAX_WEAR;
    }
    
    public boolean shouldShowSmoke(BlockPos pos) {
        int wear = getWear(pos);
        return wear > JAM_THRESHOLD;
    }
    
    public boolean checkJamChance(BlockPos pos, RandomSource random) {
        int wear = getWear(pos);
        if (wear < JAM_THRESHOLD) return false;
        
        // Chance to jam based on wear level
        float jamChance = (wear - JAM_THRESHOLD) / (float)(MAX_WEAR - JAM_THRESHOLD);
        jamChance *= 0.5f; // Max 50% chance per tick at 100% wear
        
        if (random.nextFloat() < jamChance) {
            setJammed(pos, true);
            return true;
        }
        return false;
    }
    
    private String getKey(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
    
    public String getWearStatus(BlockPos pos) {
        int wear = getWear(pos);
        if (wear < 300) return "Good";
        if (wear < 600) return "Worn";
        if (wear < JAM_THRESHOLD) return "Degraded";
        if (wear < BROKEN_THRESHOLD) return "Critical";
        return "Failing";
    }
}
