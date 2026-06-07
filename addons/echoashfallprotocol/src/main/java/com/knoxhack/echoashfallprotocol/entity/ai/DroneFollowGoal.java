package com.knoxhack.echoashfallprotocol.entity.ai;

import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import java.util.EnumSet;

/**
 * AI Goal for the companion drone to follow its owner player.
 */
public class DroneFollowGoal extends Goal {
    
    private final EchoCompanionDrone drone;
    private final double speedModifier;
    private final float stopDistance;
    private final float startDistance;
    
    private Player owner;
    private int timeToRecalcPath;
    
    public DroneFollowGoal(EchoCompanionDrone drone, double speedModifier, float stopDistance, float startDistance) {
        this.drone = drone;
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
        this.startDistance = startDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        // Only follow in FOLLOW mode
        if (drone.getCurrentMode() != EchoCompanionDrone.DroneMode.FOLLOW) {
            return false;
        }
        
        // Check if owner exists and is valid
        Player owner = drone.getOwner();
        if (owner == null) {
            return false;
        }
        
        // Only follow if too far away
        if (drone.distanceTo(owner) < startDistance) {
            return false;
        }
        
        this.owner = owner;
        return true;
    }
    
    @Override
    public boolean canContinueToUse() {
        if (drone.getCurrentMode() != EchoCompanionDrone.DroneMode.FOLLOW) {
            return false;
        }
        
        if (owner == null || !owner.isAlive()) {
            return false;
        }
        
        // Stop following if close enough
        if (drone.distanceTo(owner) <= stopDistance) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }
    
    @Override
    public void stop() {
        this.owner = null;
        drone.getNavigation().stop();
    }
    
    @Override
    public void tick() {
        if (owner == null) return;
        
        // Look at owner
        drone.getLookControl().setLookAt(owner, 10.0F, drone.getMaxHeadXRot());
        
        // Recalculate path frequently so follow mode feels immediate.
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 3;
            
            // Simple direct movement toward owner (flying entity)
            double dx = owner.getX() - drone.getX();
            double dy = owner.getY() + 2.0 - drone.getY(); // Hover 2 blocks above player
            double dz = owner.getZ() - drone.getZ();
            
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            
            if (distance > stopDistance) {
                // Normalize and apply speed
                double speed = speedModifier * 0.18;
                drone.setDeltaMovement(
                    (dx / distance) * speed,
                    (dy / distance) * speed,
                    (dz / distance) * speed
                );
            }
        }
    }
}
