package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.entity.RadZombie;
import com.knoxhack.echoashfallprotocol.entity.ScavengerBandit;
import com.knoxhack.echoashfallprotocol.entity.IrradiatedWolf;
import com.knoxhack.echoashfallprotocol.survival.PlayerTechTracker;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Modifies mob spawning based on Nexus world state.
 *
 * RESTORED: Fewer hostile mobs, lower health/damage
 * DESTROYED: More hostile mobs, higher health/damage, more mutations
 * CONTROLLED: Balanced spawning, occasional friendly buffs
 * NORMAL: Default spawning
 */
public class NexusMobSpawnHandler {

    // Spawn chance modifiers
    private static final float RESTORED_SPAWN_CHANCE = 0.5f;    // 50% fewer hostile spawns
    private static final float DESTROYED_SPAWN_CHANCE = 1.5f;   // 50% more hostile spawns
    private static final float CONTROLLED_SPAWN_CHANCE = 0.8f;  // 20% fewer hostile spawns

    // Health modifiers (multiplied to base health)
    private static final float RESTORED_HEALTH_MULT = 0.7f;     // 30% less health
    private static final float DESTROYED_HEALTH_MULT = 1.4f;    // 40% more health
    private static final float CONTROLLED_HEALTH_MULT = 0.9f;   // 10% less health

    public static void onFinalizeSpawn(Object event) {
        if (!(eventValue(event, "getLevel") instanceof ServerLevel level)) return;

        if (!(eventValue(event, "getEntity") instanceof LivingEntity entity)) return;

        // Only affect mod entities and hostile mobs
        if (!isAffectedMob(entity)) return;

        NexusWorldData nexusData = NexusWorldData.get(level);
        NexusWorldData.WorldState state = nexusData.getState();

        // Apply state-based modifications
        applyHealthModifiers(entity, state);
        applySpawnMessage(entity, state);

        // Apply tech level threat scaling if player nearby
        applyTechThreatScaling(entity, level);
    }

    private static void applyTechThreatScaling(LivingEntity entity, ServerLevel level) {
        // Find nearest player and apply their tech threat multiplier
        Player nearestPlayer = null;
        double nearestDist = Double.MAX_VALUE;

        for (ServerPlayer player : level.players()) {
            double dist = player.distanceTo(entity);
            if (dist < 64.0 && dist < nearestDist) {
                nearestDist = dist;
                nearestPlayer = player;
            }
        }

        if (nearestPlayer instanceof ServerPlayer serverPlayer) {
            float threatMult = PlayerTechTracker.getThreatMultiplierForPlayer(serverPlayer);
            if (threatMult > 1.5f && entity.getRandom().nextFloat() < 0.3f) {
                // High threat players get occasional "elite" spawns with extra health
                float bonusHealth = entity.getMaxHealth() * (threatMult - 1.0f) * 0.5f;
                if (bonusHealth > 0) {
                    float newHealth = entity.getMaxHealth() + bonusHealth;
                    entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(newHealth);
                    entity.setHealth(newHealth);
                }
            }
        }
    }

    private static boolean isAffectedMob(LivingEntity entity) {
        // Affects our custom mobs
        return entity instanceof RadZombie
            || entity instanceof ScavengerBandit
            || entity instanceof IrradiatedWolf;
    }

    private static void applyHealthModifiers(LivingEntity entity, NexusWorldData.WorldState state) {
        float healthMult = switch (state) {
            case RESTORED -> RESTORED_HEALTH_MULT;
            case DESTROYED -> DESTROYED_HEALTH_MULT;
            case CONTROLLED -> CONTROLLED_HEALTH_MULT;
            default -> 1.0f;
        };

        if (healthMult != 1.0f) {
            float maxHealth = entity.getMaxHealth();
            float newHealth = maxHealth * healthMult;
            entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(newHealth);
            entity.setHealth(newHealth);
        }
    }

    private static void applySpawnMessage(LivingEntity entity, NexusWorldData.WorldState state) {
        // Only show message occasionally and only for players nearby
        if (entity.getRandom().nextFloat() > 0.1f) return;

        String message = switch (state) {
            case RESTORED -> "§a[The Grid weakens the wasteland creatures...]";
            case DESTROYED -> "§c§l[The Grid's destruction empowers the horrors of the wasteland!]";
            case CONTROLLED -> "§e[The Nexus pulses, keeping the creatures at bay... for now.]";
            default -> null;
        };

        if (message != null && !entity.level().isClientSide()) {
            // Send to nearby players
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) entity.level();
            for (var player : serverLevel.players()) {
                if (player.distanceTo(entity) < 32.0) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
                }
            }
        }
    }

    /**
     * Check if a mob spawn should be cancelled based on Nexus state.
     * Called from spawn placement checks.
     */
    public static boolean shouldCancelSpawn(net.minecraft.world.entity.EntityType<?> entityType,
                                            net.minecraft.world.level.ServerLevelAccessor level,
                                            net.minecraft.core.BlockPos pos,
                                            net.minecraft.util.RandomSource random) {
        if (!(level.getLevel() instanceof ServerLevel serverLevel)) return false;

        NexusWorldData nexusData = NexusWorldData.get(serverLevel);
        float spawnChance = switch (nexusData.getState()) {
            case RESTORED -> RESTORED_SPAWN_CHANCE;
            case DESTROYED -> DESTROYED_SPAWN_CHANCE;
            case CONTROLLED -> CONTROLLED_SPAWN_CHANCE;
            default -> 1.0f;
        };

        // If spawnChance < 1.0, chance to cancel spawn
        if (spawnChance < 1.0f && random.nextFloat() > spawnChance) {
            return true; // Cancel spawn
        }

        // If spawnChance > 1.0, we can't force extra spawns here, but we allow all spawns
        return false;
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
