package com.knoxhack.echoashfallprotocol.entity.drone;

import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionMap;
import com.knoxhack.echoashfallprotocol.faction.FactionDiplomacy;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Combat AI for ECHO drone with faction-aware targeting and support abilities.
 * Extends drone capabilities in COMBAT mode with SUPPRESS and MARK abilities.
 */
public class DroneCombatAI {
    
    // Repair thresholds for abilities
    private static final int REPAIR_SUPPRESS = 50;
    private static final int REPAIR_MARK = 75;
    
    // Cooldowns (in ticks)
    private static final int SUPPRESS_COOLDOWN = 100;
    private static final int MARK_COOLDOWN = 200;
    
    private final EchoCompanionDrone drone;
    private int suppressCooldown = 0;
    private int markCooldown = 0;
    private LivingEntity markedTarget = null;
    private int markDuration = 0;
    
    public DroneCombatAI(EchoCompanionDrone drone) {
        this.drone = drone;
    }
    
    /**
     * Tick combat logic - called from drone's tick when in COMBAT mode
     */
    public void tickCombat(ServerLevel level) {
        if (suppressCooldown > 0) suppressCooldown--;
        if (markCooldown > 0) markCooldown--;
        
        // Update mark duration
        if (markedTarget != null) {
            if (!markedTarget.isAlive() || markDuration <= 0) {
                clearMark();
            } else {
                markDuration--;
                // Visual effect on marked target
                if (level.getGameTime() % 10 == 0) {
                    level.sendParticles(ParticleTypes.GLOW, 
                        markedTarget.getX(), markedTarget.getY() + markedTarget.getBbHeight() / 2, markedTarget.getZ(),
                        3, 0.3, 0.3, 0.3, 0);
                }
            }
        }
        
        // Find and engage targets
        findAndEngageTargets(level);
    }
    
    /**
     * Find targets based on faction hostility and engage
     */
    private void findAndEngageTargets(ServerLevel level) {
        Player owner = getOwner(level);
        if (owner == null) return;
        
        // Get faction diplomacy for target prioritization
        FactionDiplomacy diplomacy = owner.getData(ModAttachments.FACTION_DIPLOMACY.get());
        
        // Scan for potential targets
        AABB scanArea = new AABB(drone.blockPosition()).inflate(32);
        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, scanArea, this::isValidTarget);
        
        // Score targets by priority
        LivingEntity bestTarget = null;
        int bestScore = -1;
        
        for (Mob mob : nearbyMobs) {
            int score = calculateTargetPriority(mob, owner, diplomacy);
            if (score > bestScore) {
                bestScore = score;
                bestTarget = mob;
            }
        }
        
        if (bestTarget != null) {
            // Set as drone's target
            drone.setTarget(bestTarget);
            
            // Use abilities if available
            if (drone.getRepairLevel() >= REPAIR_SUPPRESS && suppressCooldown <= 0) {
                useSuppress(bestTarget, level);
            }
            
            if (drone.getRepairLevel() >= REPAIR_MARK && markCooldown <= 0 && markedTarget == null) {
                useMark(bestTarget, level, owner);
            }
        }
    }
    
    /**
     * Calculate target priority based on faction relations and threat level
     */
    private int calculateTargetPriority(Mob mob, Player owner, FactionDiplomacy diplomacy) {
        int score = 0;
        
        // Base priority by distance (closer = higher priority)
        double distSqr = mob.distanceToSqr(drone);
        score += (int) ((1024 - Math.min(distSqr, 1024)) / 100);
        
        // Check faction affiliation
        Identifier mobFaction = identifyMobFaction(mob);
        if (mobFaction != null) {
            // Check if this faction is hostile to player
            for (Identifier playerFaction : AshfallFactionMap.all()) {
                FactionDiplomacy.DiplomaticState state = diplomacy.getState(playerFaction, mobFaction);
                
                if (state == FactionDiplomacy.DiplomaticState.OPEN_WAR) {
                    score += 50; // At war - highest priority
                } else if (state == FactionDiplomacy.DiplomaticState.SKIRMISH) {
                    score += 30; // Skirmishing - high priority
                } else if (state == FactionDiplomacy.DiplomaticState.TENSION) {
                    score += 15; // Tension - medium priority
                }
            }
            
            // Also consider player reputation with that faction
            int reputation = com.knoxhack.echocore.api.EchoCoreServices.factionProfile(owner, mobFaction)
                    .map(profile -> profile.reputation())
                    .orElse(0);
            if (reputation < 0) {
                score += Math.abs(reputation) / 2; // Negative rep = higher priority
            }
        }
        
        // Prioritize mobs targeting owner or drone
        if (mob.getTarget() == owner || mob.getTarget() == drone) {
            score += 40;
        }
        
        // Prioritize dangerous mobs
        if (mob.getHealth() > 20) score += 10;
        
        return score;
    }
    
    /**
     * Use SUPPRESS ability - fire at enemy to reduce their effectiveness
     */
    private void useSuppress(LivingEntity target, ServerLevel level) {
        if (!target.isAlive()) return;
        
        // Drone "fires" at target (visual only, no damage)
        level.sendParticles(ParticleTypes.SMALL_FLAME,
            target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
            5, 0.5, 0.5, 0.5, 0.1);
        
        // Apply suppression effects
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        
        suppressCooldown = SUPPRESS_COOLDOWN;
        
        // Visual/audio feedback through drone
        if (level.getGameTime() % 20 == 0) {
            drone.speak("Target suppressed.", EchoCompanionDrone.MOOD_PROFESSIONAL, 40, 0);
        }
    }
    
    /**
     * Use MARK ability - highlight target for player damage bonus
     */
    private void useMark(LivingEntity target, ServerLevel level, Player owner) {
        if (!target.isAlive()) return;
        if (markedTarget != null) return; // Already marking someone
        
        markedTarget = target;
        markDuration = 200; // 10 seconds
        markCooldown = MARK_COOLDOWN;
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, markDuration, 0, false, false, true));

        // Visual particle effect
        level.sendParticles(ParticleTypes.CRIT,
            target.getX(), target.getY() + target.getBbHeight(), target.getZ(),
            10, 0.5, 0.5, 0.5, 1.0);
        
        // Alert player
        owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "\u00A7e[ECHO-7]\u00A7r Target marked! Focus fire on highlighted enemy."
        ));
        
        drone.speak("TARGET MARKED. Engage.", EchoCompanionDrone.MOOD_URGENT, 60, 10);
    }
    
    /**
     * Clear the current mark
     */
    public void clearMark() {
        if (markedTarget != null && markedTarget.isAlive()) {
            markedTarget.removeEffect(MobEffects.GLOWING);
        }
        markedTarget = null;
        markDuration = 0;
    }
    
    /**
     * Check if an entity is currently marked by this drone
     */
    public boolean isMarked(Entity entity) {
        return markedTarget != null && markedTarget == entity && markDuration > 0;
    }
    
    /**
     * Get the currently marked target
     */
    public LivingEntity getMarkedTarget() {
        return (markedTarget != null && markedTarget.isAlive() && markDuration > 0) ? markedTarget : null;
    }
    
    /**
     * Identify which faction a mob belongs to based on entity type/name
     */
    private Identifier identifyMobFaction(Mob mob) {
        String entityId = mob.getType().getDescriptionId();
        if (entityId.contains("military") || entityId.contains("soldier")
                || entityId.contains("guard") || entityId.contains("scavenger") || entityId.contains("bandit")
                || entityId.contains("raider") || entityId.contains("mutant") || entityId.contains("feral")
                || entityId.contains("ghoul") || entityId.contains("infected")) {
            return AshfallFactionMap.forEntity(entityId);
        }
        return null;
    }
    
    /**
     * Check if a mob is a valid target for the drone
     */
    private boolean isValidTarget(Mob mob) {
        // Don't target passive animals
        if (mob.getType().getCategory().isFriendly()) return false;
        
        // Don't target owned/tamed entities
        if (mob instanceof net.minecraft.world.entity.TamableAnimal tamable) {
            if (tamable.isTame()) return false;
        }
        
        // Must be alive and visible
        return mob.isAlive() && !mob.isInvisible();
    }
    
    /**
     * Get the drone's owner
     */
    private Player getOwner(ServerLevel level) {
        java.util.UUID ownerUUID = drone.getOwnerUUID();
        if (ownerUUID == null) return null;
        return level.getServer().getPlayerList().getPlayer(ownerUUID);
    }
    
    /**
     * Get cooldown info for UI display
     */
    public float getSuppressCooldownPercent() {
        return suppressCooldown / (float) SUPPRESS_COOLDOWN;
    }
    
    public float getMarkCooldownPercent() {
        return markCooldown / (float) MARK_COOLDOWN;
    }
    
    public int getMarkDuration() {
        return markDuration;
    }
}
