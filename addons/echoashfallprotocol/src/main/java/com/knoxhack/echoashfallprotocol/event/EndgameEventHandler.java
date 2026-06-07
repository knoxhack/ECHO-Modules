package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Random;

/**
 * Event handler for post-Nexus world events.
 * Handles Grid Defense (RESTORE), Raider Swarm (DESTROY), and System Overload (CONTROL).
 */
public class EndgameEventHandler {

    private static final int TICKS_PER_DAY = 24000;
    private static final Random RANDOM = new Random();

    // Event chance per day (as percentage)
    private static final float RESTORE_EVENT_CHANCE = 0.05f;  // 5%
    private static final float DESTROY_EVENT_CHANCE = 0.08f;  // 8%
    private static final float CONTROL_EVENT_CHANCE = 0.06f;  // 6%

    public static void onLevelTick(Object event) {
        Object eventLevel = eventValue(event, "getLevel");
        if (!(eventLevel instanceof Level rawLevel) || rawLevel.isClientSide()) return;
        if (!(rawLevel instanceof ServerLevel level)) return;
        if (!level.dimension().toString().contains("overworld")) return;

        // Only run once per ~20 minutes
        long gameTime = level.getGameTime();
        if (gameTime % TICKS_PER_DAY < 6000 || gameTime % TICKS_PER_DAY > 6100) return;

        // Check world state
        NexusWorldData worldData = NexusWorldData.get(level);
        if (!worldData.hasChoiceBeenMade()) return;
        if (gameTime - worldData.getLastEndgameEventTick() < TICKS_PER_DAY) return;

        // Check if event should trigger based on path
        float chance = RANDOM.nextFloat();
        boolean triggered = false;
        
        switch (worldData.getState()) {
            case RESTORED -> {
                if (chance < RESTORE_EVENT_CHANCE) {
                    triggerGridDefenseEvent(level);
                    triggered = true;
                }
            }
            case DESTROYED -> {
                if (chance < DESTROY_EVENT_CHANCE) {
                    triggerRaiderSwarmEvent(level);
                    triggered = true;
                }
            }
            case CONTROLLED -> {
                if (chance < CONTROL_EVENT_CHANCE) {
                    triggerSystemOverloadEvent(level);
                    triggered = true;
                }
            }
            default -> {}
        }

        if (triggered) {
            worldData.setLastEndgameEventTick(gameTime);
        }
    }

    /**
     * RESTORE Path: Grid Defense Event
     * Spawn waves of mobs near Power Nodes
     */
    private static void triggerGridDefenseEvent(ServerLevel level) {
        // Find online players
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers()
            .stream().filter(p -> p.level() == level).toList();
        
        if (players.isEmpty()) return;

        for (ServerPlayer player : players) {
            // Find nearby Power Nodes
            BlockPos playerPos = player.blockPosition();
            BlockPos nearestNode = findNearestTrackedPowerNode(level, playerPos, 100);
            if (nearestNode == null) {
                nearestNode = findNearestBlock(level, playerPos, ModBlocks.POWER_NODE.get(), 100);
            }
            
            if (nearestNode != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§a[ECHO-7] Restore lattice stabilized the local node and released repair support."
                ), true);
                // Spawn wave announcement
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c[WARNING]§r Hostiles detected near Power Node at " + nearestNode.getX() + ", " + nearestNode.getZ()
                ));

                // Spawn mobs
                spawnMobWave(level, nearestNode, 4, (net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>) ModEntities.SCAVENGER_BANDIT.get());
                spawnMobWave(level, nearestNode, 3, (net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>) ModEntities.GLOWING_GHOUL.get());
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    nearestNode.getX() + 0.5D, nearestNode.getY() + 1.2D, nearestNode.getZ() + 0.5D,
                    24, 1.5D, 0.8D, 1.5D, 0.05D);

                // Drop reward at node location
                ItemStack reward = new ItemStack(ModItems.NEXUS_CRYSTAL.get(), 1 + RANDOM.nextInt(2));
                ItemEntity itemEntity = new ItemEntity(level, 
                    nearestNode.getX() + 0.5, nearestNode.getY() + 1, nearestNode.getZ() + 0.5, reward);
                level.addFreshEntity(itemEntity);
                level.addFreshEntity(new ItemEntity(level,
                    nearestNode.getX() + 0.5D, nearestNode.getY() + 1.2D, nearestNode.getZ() + 0.5D,
                    new ItemStack(ModItems.SCRAP_WIRE.get(), 4 + RANDOM.nextInt(5))));
            }
        }
    }

    /**
     * DESTROY Path: Raider Swarm Event
     * Spawn large group of bandits near player
     */
    private static void triggerRaiderSwarmEvent(ServerLevel level) {
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers()
            .stream().filter(p -> p.level() == level).toList();

        for (ServerPlayer player : players) {
            BlockPos pos = player.blockPosition();
            
            // Announce
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§c[WARNING]§r Raider swarm detected in your vicinity!"
            ));
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§c[ECHO-7] Destroy path instability spiking. High salvage detected; local pressure rising."
            ), true);
            var survival = player.getData(com.knoxhack.echoashfallprotocol.registry.ModAttachments.SURVIVAL_DATA.get());
            survival.addRadiation(com.knoxhack.echoashfallprotocol.gameplay.RadiationHelper.scaleIncomingRadiation(player, 3));
            player.setData(com.knoxhack.echoashfallprotocol.registry.ModAttachments.SURVIVAL_DATA.get(), survival);
            level.sendParticles(ParticleTypes.SMOKE, pos.getX(), pos.getY() + 1.0D, pos.getZ(),
                40, 4.0D, 1.0D, 4.0D, 0.02D);

            // Spawn 8-12 bandits
            int count = 8 + RANDOM.nextInt(5);
            for (int i = 0; i < count; i++) {
                net.minecraft.world.entity.Mob bandit = ((net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>) ModEntities.SCAVENGER_BANDIT.get()).create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
                if (bandit != null) {
                    // Random offset around player
                    double angle = RANDOM.nextDouble() * Math.PI * 2;
                    double distance = 15 + RANDOM.nextDouble() * 10;
                    double x = pos.getX() + Math.cos(angle) * distance;
                    double z = pos.getZ() + Math.sin(angle) * distance;
                    
                    bandit.setPos(x, pos.getY(), z);
                    bandit.setTarget(player);
                    level.addFreshEntity(bandit);
                }
            }

            // Drop leader reward
            ItemStack reward = new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 2 + RANDOM.nextInt(3));
            ItemEntity itemEntity = new ItemEntity(level, pos.getX(), pos.getY() + 1, pos.getZ(), reward);
            level.addFreshEntity(itemEntity);
            level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY() + 1.2D, pos.getZ(),
                new ItemStack(ModItems.SCRAP_METAL.get(), 8 + RANDOM.nextInt(9))));
        }
    }

    /**
     * CONTROL Path: System Overload Event
     * Random machine gets boosted but risky
     */
    private static void triggerSystemOverloadEvent(ServerLevel level) {
        List<ServerPlayer> players = level.getServer().getPlayerList().getPlayers()
            .stream().filter(p -> p.level() == level).toList();

        for (ServerPlayer player : players) {
            // Find a random machine within 50 blocks
            BlockPos playerPos = player.blockPosition();
            BlockPos machinePos = findRandomBlock(level, playerPos, 50, 
                ModBlocks.HAND_RECYCLER.get(),
                ModBlocks.ORE_GRINDER.get(),
                ModBlocks.ISOTOPE_REFINER.get(),
                ModBlocks.THERMAL_BURNER.get()
            );

            if (machinePos != null) {
                com.knoxhack.echoashfallprotocol.gameplay.MachineGameplayHelper.addNexusSurge(level, machinePos, 6000L);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§5[SYSTEM]§r Machine overload detected at " + machinePos.getX() + ", " + machinePos.getZ() + 
                    " - Control field active; verify machine status before rare inputs."
                ));

                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§5Nexus surge routed: +35% machine speed for 5 minutes. Monitor wear before rare inputs."), true);
                level.sendParticles(ParticleTypes.PORTAL,
                    machinePos.getX() + 0.5D, machinePos.getY() + 1.0D, machinePos.getZ() + 0.5D,
                    36, 1.0D, 0.6D, 1.0D, 0.08D);
            }
        }
    }

    /**
     * Find nearest block of a specific type
     */
    private static BlockPos findNearestBlock(ServerLevel level, BlockPos center, net.minecraft.world.level.block.Block block, int radius) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x += 5) {
            for (int y = -10; y <= 10; y += 5) {
                for (int z = -radius; z <= radius; z += 5) {
                    BlockPos checkPos = center.offset(x, y, z);
                    if (level.getBlockState(checkPos).is(block)) {
                        double dist = center.distSqr(checkPos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = checkPos;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private static BlockPos findNearestTrackedPowerNode(ServerLevel level, BlockPos center, int radius) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        int radiusSqr = radius * radius;
        NexusWorldData worldData = NexusWorldData.get(level);

        for (BlockPos nodePos : worldData.getActiveNodePositions()) {
            double dist = center.distSqr(nodePos);
            if (dist > radiusSqr || dist >= nearestDist) {
                continue;
            }
            if (!worldData.isTrackedActiveNode(level, nodePos)) {
                continue;
            }
            nearest = nodePos;
            nearestDist = dist;
        }
        return nearest;
    }

    /**
     * Find random block of any of the given types
     */
    @SafeVarargs
    private static BlockPos findRandomBlock(ServerLevel level, BlockPos center, int radius, net.minecraft.world.level.block.Block... blocks) {
        for (int attempt = 0; attempt < 20; attempt++) {
            int x = center.getX() + (RANDOM.nextInt(radius * 2) - radius);
            int y = center.getY() + (RANDOM.nextInt(20) - 10);
            int z = center.getZ() + (RANDOM.nextInt(radius * 2) - radius);
            BlockPos checkPos = new BlockPos(x, y, z);
            
            for (net.minecraft.world.level.block.Block block : blocks) {
                if (level.getBlockState(checkPos).is(block)) {
                    return checkPos;
                }
            }
        }
        return null;
    }

    /**
     * Spawn a wave of mobs at a location
     */
    private static void spawnMobWave(ServerLevel level, BlockPos pos, int count, net.minecraft.world.entity.EntityType<? extends Mob> entityType) {
        for (int i = 0; i < count; i++) {
            Mob mob = entityType.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (mob != null) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double distance = 5 + RANDOM.nextDouble() * 10;
                double x = pos.getX() + Math.cos(angle) * distance;
                double z = pos.getZ() + Math.sin(angle) * distance;
                
                mob.setPos(x, pos.getY(), z);
                level.addFreshEntity(mob);
            }
        }
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
