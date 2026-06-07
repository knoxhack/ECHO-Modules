package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks Atmospheric Scrubber locations to determine safe zones.
 * Safe zones protect players from toxic air damage.
 */
public class ScrubberSafeZoneManager {
    
    private static final Map<String, Map<BlockPos, Long>> scrubberLocations = new HashMap<>();
    
    /**
     * Register an active scrubber at a position.
     * Called by AtmosphericScrubberBlockEntity when active.
     */
    public static void registerScrubber(ServerLevel level, BlockPos pos) {
        String worldKey = level.dimension().toString();
        scrubberLocations.computeIfAbsent(worldKey, k -> new HashMap<>())
                         .put(pos, level.getGameTime());
    }
    
    /**
     * Unregister a scrubber (when broken or powered off).
     */
    public static void unregisterScrubber(ServerLevel level, BlockPos pos) {
        String worldKey = level.dimension().toString();
        Map<BlockPos, Long> worldScrubbers = scrubberLocations.get(worldKey);
        if (worldScrubbers != null) {
            worldScrubbers.remove(pos);
        }
    }
    
    /**
     * Check if a player is within a scrubber safe zone.
     */
    public static boolean isInSafeZone(ServerLevel level, BlockPos playerPos) {
        String worldKey = level.dimension().toString();
        Map<BlockPos, Long> worldScrubbers = scrubberLocations.get(worldKey);
        
        if (worldScrubbers == null || worldScrubbers.isEmpty()) {
            return false;
        }
        
        long currentTime = level.getGameTime();
        
        // Check all scrubbers
        for (Map.Entry<BlockPos, Long> entry : worldScrubbers.entrySet()) {
            BlockPos scrubberPos = entry.getKey();
            long lastSeen = entry.getValue();
            
            // Skip if scrubber hasn't been updated in 5 seconds (may be broken)
            if (currentTime - lastSeen > 100) {
                continue;
            }
            
            // Check if player is within radius
            double dist = Math.sqrt(scrubberPos.distSqr(playerPos));
            if (dist <= Config.SCRUBBER_SAFE_ZONE_RADIUS.get()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the nearest scrubber distance (for UI feedback).
     */
    public static double getNearestScrubberDistance(ServerLevel level, BlockPos playerPos) {
        String worldKey = level.dimension().toString();
        Map<BlockPos, Long> worldScrubbers = scrubberLocations.get(worldKey);
        
        if (worldScrubbers == null || worldScrubbers.isEmpty()) {
            return Double.MAX_VALUE;
        }
        
        long currentTime = level.getGameTime();
        double nearestDist = Double.MAX_VALUE;
        
        for (Map.Entry<BlockPos, Long> entry : worldScrubbers.entrySet()) {
            BlockPos scrubberPos = entry.getKey();
            long lastSeen = entry.getValue();
            
            if (currentTime - lastSeen > 100) {
                continue;
            }
            
            double dist = Math.sqrt(scrubberPos.distSqr(playerPos));
            if (dist < nearestDist) {
                nearestDist = dist;
            }
        }
        
        return nearestDist;
    }
    
    /**
     * Cleanup old entries (called periodically).
     */
    public static void cleanup(ServerLevel level) {
        String worldKey = level.dimension().toString();
        Map<BlockPos, Long> worldScrubbers = scrubberLocations.get(worldKey);
        
        if (worldScrubbers == null) return;
        
        long currentTime = level.getGameTime();
        worldScrubbers.entrySet().removeIf(entry -> {
            // Remove if not updated in 10 seconds
            return (currentTime - entry.getValue()) > 200;
        });
    }
}
