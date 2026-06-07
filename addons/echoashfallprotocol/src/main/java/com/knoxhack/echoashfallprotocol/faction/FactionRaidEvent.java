package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echoashfallprotocol.echo.EchoIntel;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.faction.FactionNpcEntity;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Identifier-keyed raid between two Ashfall factions.
 */
public class FactionRaidEvent {
    private static final int PREPARE_TICKS = 600;
    private static final int WAVE_INTERVAL = 400;
    private static final int RAID_RADIUS = 50;

    private final UUID raidId;
    private final Identifier attackingFaction;
    private final Identifier defendingFaction;
    private final BlockPos targetLocation;
    private final Level level;
    private final Random random = new Random();
    private final List<Mob> activeAttackers = new ArrayList<>();
    private final List<Mob> activeDefenders = new ArrayList<>();

    private RaidStatus status = RaidStatus.PREPARING;
    private int currentWave = 0;
    private int maxWaves;
    private int ticksUntilNextWave;
    private int totalTicks = 0;

    public FactionRaidEvent(Identifier attacker, Identifier defender, BlockPos target, Level level, int waves) {
        this.raidId = UUID.randomUUID();
        this.attackingFaction = attacker;
        this.defendingFaction = defender;
        this.targetLocation = target;
        this.level = level;
        this.maxWaves = Math.max(1, waves);
        this.ticksUntilNextWave = PREPARE_TICKS;
    }

    public enum RaidStatus {
        PREPARING("Preparing", 0xFFFFA94D),
        ACTIVE("Active", 0xFFFF3333),
        DEFENDER_VICTORY("Defenders Win", 0xFF42D67E),
        ATTACKER_VICTORY("Attackers Win", 0xFFFF3333),
        EXPIRED("Expired", 0xFF8A9BB0);

        private final String displayName;
        private final int color;

        RaidStatus(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColor() {
            return color;
        }
    }

    public void tick() {
        if (status != RaidStatus.ACTIVE && status != RaidStatus.PREPARING) {
            return;
        }
        totalTicks++;
        activeAttackers.removeIf(entity -> !entity.isAlive());
        activeDefenders.removeIf(entity -> !entity.isAlive());
        if (status == RaidStatus.PREPARING) {
            ticksUntilNextWave--;
            if (ticksUntilNextWave <= 0) {
                startRaid();
            }
            return;
        }
        ticksUntilNextWave--;
        if (activeAttackers.isEmpty() && currentWave > 0) {
            if (currentWave >= maxWaves) {
                endRaid(RaidStatus.DEFENDER_VICTORY);
                return;
            }
            spawnNextWave();
        }
        if (ticksUntilNextWave <= 0 && currentWave < maxWaves) {
            spawnNextWave();
        }
        if (activeDefenders.isEmpty() && currentWave >= Math.max(1, maxWaves / 2)) {
            endRaid(RaidStatus.ATTACKER_VICTORY);
        }
        if (totalTicks > 36000) {
            endRaid(RaidStatus.EXPIRED);
        }
    }

    private void startRaid() {
        status = RaidStatus.ACTIVE;
        notifyPlayers("\u00A74[FACTION WAR] " + AshfallFactionMap.displayName(attackingFaction)
                + " are pressuring a " + AshfallFactionMap.displayName(defendingFaction) + " position.");
        spawnNextWave();
    }

    private void spawnNextWave() {
        currentWave++;
        ticksUntilNextWave = WAVE_INTERVAL;
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int attackersPerWave = 2 + currentWave;
        for (int i = 0; i < attackersPerWave; i++) {
            double angle = (2 * Math.PI * i) / attackersPerWave;
            BlockPos spawnPos = findValidSpawnPos(serverLevel,
                    targetLocation.getX() + (int) (Math.cos(angle) * RAID_RADIUS),
                    targetLocation.getY(),
                    targetLocation.getZ() + (int) (Math.sin(angle) * RAID_RADIUS));
            if (spawnPos != null) {
                spawnUnit(serverLevel, spawnPos, attackingFaction, activeAttackers, "raider");
            }
        }
        if (currentWave == 1) {
            int defenders = 2 + maxWaves;
            for (int i = 0; i < defenders; i++) {
                BlockPos defenderPos = targetLocation.offset(random.nextInt(10) - 5, 0, random.nextInt(10) - 5);
                spawnUnit(serverLevel, defenderPos, defendingFaction, activeDefenders, "defender");
            }
        }
        notifyPlayers("\u00A7c[RAID ALERT] Wave " + currentWave + "/" + maxWaves + " incoming.");
    }

    private void spawnUnit(ServerLevel level, BlockPos pos, Identifier factionId, List<Mob> bucket, String role) {
        FactionNpcEntity npc = ModEntities.FACTION_NPC.get().create(level, EntitySpawnReason.EVENT);
        if (npc == null) {
            return;
        }
        npc.configure(factionId, role);
        npc.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        level.addFreshEntity(npc);
        bucket.add(npc);
    }

    private BlockPos findValidSpawnPos(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        for (int i = 10; i > -10; i--) {
            BlockPos check = pos.offset(0, i, 0);
            if (!level.getBlockState(check).isAir() && level.getBlockState(check.above()).isAir()) {
                return check.above();
            }
        }
        return null;
    }

    private void endRaid(RaidStatus result) {
        status = result;
        for (Mob attacker : activeAttackers) {
            if (attacker.isAlive()) {
                attacker.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        }
        String message = switch (result) {
            case DEFENDER_VICTORY -> "\u00A7aThe defenders of " + AshfallFactionMap.displayName(defendingFaction)
                    + " have repelled the attack.";
            case ATTACKER_VICTORY -> "\u00A7cThe " + AshfallFactionMap.displayName(attackingFaction)
                    + " have overrun the position.";
            case EXPIRED -> "\u00A77The raid has ended without resolution.";
            default -> "\u00A77The raid has concluded.";
        };
        notifyPlayers(message);
        applyRaidOutcome(result);
    }

    private void applyRaidOutcome(RaidStatus result) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB notifyArea = new AABB(targetLocation).inflate(RAID_RADIUS * 4);
        for (ServerPlayer player : serverLevel.getPlayers(p -> notifyArea.contains(p.getX(), p.getY(), p.getZ()))) {
            FactionTerritory territory = player.getData(ModAttachments.FACTION_TERRITORY.get());
            FactionDiplomacy diplomacy = player.getData(ModAttachments.FACTION_DIPLOMACY.get());
            String biomeKey = player.level().getBiome(targetLocation).toString();
            FactionDiplomacy.FactionPair pair = FactionDiplomacy.FactionPair.fromFactions(attackingFaction, defendingFaction);
            if (result == RaidStatus.ATTACKER_VICTORY) {
                territory.transferVillageControl(targetLocation, attackingFaction);
                territory.modifyBiomeInfluence(attackingFaction, biomeKey, 12);
                territory.modifyBiomeInfluence(defendingFaction, biomeKey, -10);
                if (pair != null) {
                    diplomacy.modifyRelation(pair, -4);
                }
            } else if (result == RaidStatus.DEFENDER_VICTORY) {
                territory.modifyBiomeInfluence(defendingFaction, biomeKey, 8);
                if (pair != null) {
                    diplomacy.modifyRelation(pair, -2);
                }
                AshfallFactionContractProgression.progressRaidDefense(player, defendingFaction);
            }
            FactionTerritory.saveAndSync(player, territory);
            FactionDiplomacy.saveAndSync(player, diplomacy);

            EchoIntel intel = player.getData(ModAttachments.ECHO_INTEL.get());
            Identifier reportedFaction = result == RaidStatus.ATTACKER_VICTORY ? attackingFaction : defendingFaction;
            intel.addTacticalIntel("Raid Resolved",
                    AshfallFactionMap.displayName(attackingFaction) + " vs "
                            + AshfallFactionMap.displayName(defendingFaction) + ": " + result.getDisplayName(),
                    reportedFaction,
                    result == RaidStatus.EXPIRED ? EchoIntel.IntelPriority.LOW : EchoIntel.IntelPriority.HIGH);
            EchoIntel.saveAndSync(player, intel);
        }
    }

    private void notifyPlayers(String message) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB notifyArea = new AABB(targetLocation).inflate(RAID_RADIUS * 2);
        for (ServerPlayer player : serverLevel.getPlayers(p -> notifyArea.contains(p.getX(), p.getY(), p.getZ()))) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    public UUID getRaidId() {
        return raidId;
    }

    public RaidStatus getStatus() {
        return status;
    }

    public Identifier getAttackingFaction() {
        return attackingFaction;
    }

    public Identifier getDefendingFaction() {
        return defendingFaction;
    }

    public BlockPos getTargetLocation() {
        return targetLocation;
    }

    public Level getTargetLevel() {
        return level;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getMaxWaves() {
        return maxWaves;
    }

    public boolean isActive() {
        return status == RaidStatus.ACTIVE || status == RaidStatus.PREPARING;
    }
}
