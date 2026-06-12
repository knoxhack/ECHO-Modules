package com.knoxhack.echoashfallprotocol.faction;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.faction.FactionNpcEntity;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;

/**
 * Spawns low-density three-faction contact patrols near discovered affinity sites.
 */
public class FactionPatrolSpawner {
    private static final Map<UUID, FactionPatrol> ACTIVE_PATROLS = new HashMap<>();
    private static final int SPAWN_CHECK_INTERVAL = 6000;
    private static final int MAX_PATROLS_PER_FACTION = 2;
    private static final int PATROL_RADIUS = 80;
    private static int spawnCheckCounter = 0;

    public static void onLevelTick(Object event) {
        if (!(eventValue(event, "getLevel") instanceof Level level)) {
            return;
        }
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        cleanupDeadPatrols();
        spawnCheckCounter++;
        if (spawnCheckCounter >= SPAWN_CHECK_INTERVAL) {
            spawnCheckCounter = 0;
            checkAndSpawnPatrols(serverLevel);
        }
        tickPatrols(serverLevel);
    }

    private static void checkAndSpawnPatrols(ServerLevel level) {
        Random random = new Random();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) {
                continue;
            }
            FactionTerritory territory = player.getData(ModAttachments.FACTION_TERRITORY.get());
            FactionDiplomacy diplomacy = player.getData(ModAttachments.FACTION_DIPLOMACY.get());
            for (Identifier factionId : AshfallFactionMap.all()) {
                if (countFactionPatrols(factionId, player.blockPosition()) >= MAX_PATROLS_PER_FACTION) {
                    continue;
                }
                double spawnChance = calculateSpawnChance(factionId, diplomacy, territory, player);
                if (random.nextDouble() >= spawnChance) {
                    continue;
                }
                FactionTerritory.VillageControl village = territory.getNearestVillage(player.blockPosition(), factionId);
                if (village == null || village.center.distSqr(player.blockPosition()) >= 40000) {
                    continue;
                }
                BlockPos spawnPos = findValidSpawnPos(level, village.center, PATROL_RADIUS);
                if (spawnPos != null) {
                    spawnPatrol(level, factionId, spawnPos, player, village);
                }
            }
        }
    }

    private static double calculateSpawnChance(Identifier factionId, FactionDiplomacy diplomacy,
            FactionTerritory territory, ServerPlayer player) {
        int reputation = EchoCoreServices.factionProfile(player, factionId).map(profile -> profile.reputation()).orElse(0);
        double chance = 0.04;
        if (reputation >= 35) {
            chance += 0.08;
        } else if (reputation < 0) {
            chance -= 0.03;
        }
        FactionTerritory.VillageControl local = territory.getNearestVillage(player.blockPosition(), factionId);
        if (local != null && local.center.distSqr(player.blockPosition()) < 40000) {
            chance += 0.05;
        }
        String biomeKey = player.level().getBiome(player.blockPosition()).toString();
        chance += territory.getBiomeInfluence(factionId, biomeKey) / 800.0D;
        for (Identifier other : AshfallFactionMap.all()) {
            if (!other.equals(factionId) && diplomacy.getState(factionId, other).isConflict()) {
                chance += 0.04;
            }
        }
        return Math.min(0.24D, Math.max(0.0D, chance));
    }

    private static void spawnPatrol(ServerLevel level, Identifier factionId, BlockPos pos, ServerPlayer player,
            FactionTerritory.VillageControl homeVillage) {
        Random random = new Random();
        int patrolSize = 1 + random.nextInt(2);
        List<FactionNpcEntity> members = new ArrayList<>();
        for (int i = 0; i < patrolSize; i++) {
            FactionNpcEntity member = ModEntities.FACTION_NPC.get().create(level, EntitySpawnReason.EVENT);
            if (member == null) {
                continue;
            }
            double offsetX = (random.nextDouble() - 0.5D) * 10.0D;
            double offsetZ = (random.nextDouble() - 0.5D) * 10.0D;
            member.setPos(pos.getX() + offsetX, pos.getY(), pos.getZ() + offsetZ);
            member.configure(factionId, "patrol_contact");
            level.addFreshEntity(member);
            members.add(member);
        }
        if (!members.isEmpty()) {
            FactionPatrol patrol = new FactionPatrol(factionId, members, homeVillage);
            ACTIVE_PATROLS.put(patrol.id, patrol);
            if (EchoCoreServices.factionProfile(player, factionId).map(profile -> profile.reputation()).orElse(0) >= 25) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "\u00A7a[ECHO-7]\u00A7r " + AshfallFactionMap.displayName(factionId)
                                + " patrol contact spotted nearby."));
            }
        }
    }

    private static BlockPos findValidSpawnPos(ServerLevel level, BlockPos center, int radius) {
        Random random = new Random();
        for (int attempts = 0; attempts < 10; attempts++) {
            int offsetX = random.nextInt(radius * 2) - radius;
            int offsetZ = random.nextInt(radius * 2) - radius;
            BlockPos pos = center.offset(offsetX, 0, offsetZ);
            for (int y = 10; y > -10; y--) {
                BlockPos check = pos.offset(0, y, 0);
                if (!level.getBlockState(check).isAir() && level.getBlockState(check.above()).isAir()) {
                    return check.above();
                }
            }
        }
        return null;
    }

    private static void tickPatrols(ServerLevel level) {
        for (FactionPatrol patrol : ACTIVE_PATROLS.values()) {
            patrol.tick();
        }
    }

    private static void cleanupDeadPatrols() {
        Iterator<Map.Entry<UUID, FactionPatrol>> iterator = ACTIVE_PATROLS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isInvalid()) {
                iterator.remove();
            }
        }
    }

    private static int countFactionPatrols(Identifier factionId, BlockPos pos) {
        int count = 0;
        for (FactionPatrol patrol : ACTIVE_PATROLS.values()) {
            if (patrol.factionId.equals(factionId) && patrol.isNear(pos)) {
                count++;
            }
        }
        return count;
    }

    private static class FactionPatrol {
        private final UUID id = UUID.randomUUID();
        private final Identifier factionId;
        private final List<FactionNpcEntity> members;
        private final FactionTerritory.VillageControl homeVillage;
        private int patrolTicks = 0;
        private BlockPos currentTargetPos;

        FactionPatrol(Identifier factionId, List<FactionNpcEntity> members, FactionTerritory.VillageControl homeVillage) {
            this.factionId = factionId;
            this.members = members;
            this.homeVillage = homeVillage;
            this.currentTargetPos = homeVillage.center;
        }

        void tick() {
            patrolTicks++;
            if (patrolTicks % 600 == 0) {
                Random random = new Random();
                currentTargetPos = homeVillage.center.offset(
                        random.nextInt(PATROL_RADIUS * 2) - PATROL_RADIUS,
                        0,
                        random.nextInt(PATROL_RADIUS * 2) - PATROL_RADIUS);
            }
            members.removeIf(member -> !member.isAlive());
            for (FactionNpcEntity member : members) {
                if (member.isAlive() && currentTargetPos != null
                        && member.distanceToSqr(currentTargetPos.getX(), currentTargetPos.getY(), currentTargetPos.getZ()) > 100) {
                    member.getNavigation().moveTo(currentTargetPos.getX(), currentTargetPos.getY(), currentTargetPos.getZ(), 0.8D);
                }
            }
        }

        boolean isInvalid() {
            return members.isEmpty();
        }

        boolean isNear(BlockPos pos) {
            return homeVillage.center.distSqr(pos) < 250000;
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
