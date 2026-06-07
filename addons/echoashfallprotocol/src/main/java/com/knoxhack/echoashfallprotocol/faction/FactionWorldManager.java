package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echoashfallprotocol.echo.EchoIntel;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * World-level pressure for the three Ashfall factions.
 */
public class FactionWorldManager {
    private static final Map<UUID, FactionRaidEvent> ACTIVE_RAIDS = new HashMap<>();
    private static final List<PendingRaid> PENDING_RAIDS = new ArrayList<>();
    private static final int RAID_WARNING_TICKS = 6000;
    private static final int DIPLOMACY_TICK_INTERVAL = 24000;
    private static final int RAID_CHECK_INTERVAL = 12000;
    private static final int INTEL_DECAY_INTERVAL = 72000;
    private static final double DUPLICATE_RAID_DISTANCE_SQR = 10000.0D;
    private static final Random RANDOM = new Random();
    private static int diplomacyTickCounter = 0;
    private static int raidCheckCounter = 0;
    private static int intelRefreshCounter = 0;

    public static void onLevelTick(Object event) {
        if (!(eventValue(event, "getLevel") instanceof ServerLevel level)) {
            return;
        }
        if (++diplomacyTickCounter >= DIPLOMACY_TICK_INTERVAL) {
            diplomacyTickCounter = 0;
            updateDiplomaticRelations(level);
        }
        if (++raidCheckCounter >= RAID_CHECK_INTERVAL) {
            raidCheckCounter = 0;
            checkForRaidTriggers(level);
        }
        if (++intelRefreshCounter >= INTEL_DECAY_INTERVAL) {
            intelRefreshCounter = 0;
            refreshPlayerIntel(level);
        }
        tickPendingRaids();
        tickActiveRaids();
    }

    public static void startRaid(ServerLevel level, Identifier attacker, Identifier defender, BlockPos target, int waves) {
        if (attacker == null || defender == null || target == null || hasRaidNear(level, target)) {
            return;
        }
        FactionRaidEvent raid = new FactionRaidEvent(attacker, defender, target, level, waves);
        ACTIVE_RAIDS.put(raid.getRaidId(), raid);
        notifyNearbyPlayers(level, target, "\u00A74[FACTION WAR] " + AshfallFactionMap.displayName(attacker)
                + " pressure reported against " + AshfallFactionMap.displayName(defender) + ".");
    }

    public static void onPlayerQuestComplete(ServerPlayer player, Identifier faction, int reputationChange) {
        FactionDiplomacy diplomacy = player.getData(ModAttachments.FACTION_DIPLOMACY.get());
        for (Identifier other : AshfallFactionMap.all()) {
            if (other.equals(faction)) {
                continue;
            }
            FactionDiplomacy.FactionPair pair = FactionDiplomacy.FactionPair.fromFactions(faction, other);
            if (pair != null && diplomacy.getRelation(pair) < 0) {
                diplomacy.modifyRelation(pair, -Math.max(1, reputationChange / 5));
            }
        }
        FactionDiplomacy.saveAndSync(player, diplomacy);
    }

    public static boolean hasActiveRaids() {
        return !ACTIVE_RAIDS.isEmpty();
    }

    public static int getActiveRaidCount() {
        return ACTIVE_RAIDS.size();
    }

    private static void updateDiplomaticRelations(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) {
                continue;
            }
            FactionDiplomacy diplomacy = player.getData(ModAttachments.FACTION_DIPLOMACY.get());
            diplomacy.tickRelations(level.getGameTime());
            FactionDiplomacy.saveAndSync(player, diplomacy);
        }
    }

    private static void checkForRaidTriggers(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) {
                continue;
            }
            FactionTerritory territory = player.getData(ModAttachments.FACTION_TERRITORY.get());
            FactionDiplomacy diplomacy = player.getData(ModAttachments.FACTION_DIPLOMACY.get());
            for (FactionDiplomacy.FactionPair pair : FactionDiplomacy.FactionPair.values()) {
                FactionDiplomacy.DiplomaticState state = diplomacy.getState(pair);
                if (state != FactionDiplomacy.DiplomaticState.OPEN_WAR
                        && state != FactionDiplomacy.DiplomaticState.SKIRMISH) {
                    continue;
                }
                float chance = state == FactionDiplomacy.DiplomaticState.OPEN_WAR ? 0.12F : 0.08F;
                if (RANDOM.nextFloat() >= chance) {
                    continue;
                }
                FactionTerritory.VillageControl targetVillage =
                        territory.getNearestVillage(player.blockPosition(), pair.getFactionB());
                if (targetVillage == null || hasRaidNear(level, targetVillage.center)) {
                    continue;
                }
                int waves = state == FactionDiplomacy.DiplomaticState.OPEN_WAR ? 4 : 2;
                PENDING_RAIDS.add(new PendingRaid(pair.getFactionA(), pair.getFactionB(), targetVillage.center,
                        level, waves, RAID_WARNING_TICKS));
                broadcastRaidWarning(level, PENDING_RAIDS.get(PENDING_RAIDS.size() - 1), false);
            }
        }
    }

    private static void tickPendingRaids() {
        Iterator<PendingRaid> iterator = PENDING_RAIDS.iterator();
        while (iterator.hasNext()) {
            PendingRaid pending = iterator.next();
            pending.ticksRemaining -= 20;
            if (pending.ticksRemaining == 1200) {
                broadcastRaidWarning(pending.level, pending, true);
            }
            if (pending.ticksRemaining <= 0) {
                startRaid(pending.level, pending.attacker, pending.defender, pending.target, pending.waves);
                iterator.remove();
            }
        }
    }

    private static void tickActiveRaids() {
        Iterator<Map.Entry<UUID, FactionRaidEvent>> iterator = ACTIVE_RAIDS.entrySet().iterator();
        while (iterator.hasNext()) {
            FactionRaidEvent raid = iterator.next().getValue();
            raid.tick();
            if (!raid.isActive()) {
                iterator.remove();
            }
        }
    }

    private static void refreshPlayerIntel(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) {
                continue;
            }
            EchoIntel intel = player.getData(ModAttachments.ECHO_INTEL.get());
            if (!ACTIVE_RAIDS.isEmpty()) {
                intel.addTacticalIntel("Faction Pressure Map",
                        ACTIVE_RAIDS.size() + " faction pressure event(s) currently active.",
                        null,
                        EchoIntel.IntelPriority.MEDIUM);
                EchoIntel.saveAndSync(player, intel);
            }
        }
    }

    private static void broadcastRaidWarning(ServerLevel level, PendingRaid pending, boolean imminent) {
        String timing = imminent ? "imminent" : "forming";
        notifyNearbyPlayers(level, pending.target, "\u00A76[ECHO-7]\u00A7r Faction pressure " + timing + ": "
                + AshfallFactionMap.displayName(pending.attacker) + " -> "
                + AshfallFactionMap.displayName(pending.defender) + ".");
    }

    private static boolean hasRaidNear(ServerLevel level, BlockPos target) {
        for (FactionRaidEvent raid : ACTIVE_RAIDS.values()) {
            if (raid.getTargetLevel() == level && raid.getTargetLocation().distSqr(target) < DUPLICATE_RAID_DISTANCE_SQR) {
                return true;
            }
        }
        for (PendingRaid pending : PENDING_RAIDS) {
            if (pending.level == level && pending.target.distSqr(target) < DUPLICATE_RAID_DISTANCE_SQR) {
                return true;
            }
        }
        return false;
    }

    private static void notifyNearbyPlayers(ServerLevel level, BlockPos pos, String message) {
        for (ServerPlayer player : level.getPlayers(player -> player.blockPosition().distSqr(pos) < 40000.0D)) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private static class PendingRaid {
        private final Identifier attacker;
        private final Identifier defender;
        private final BlockPos target;
        private final ServerLevel level;
        private final int waves;
        private int ticksRemaining;

        PendingRaid(Identifier attacker, Identifier defender, BlockPos target, ServerLevel level, int waves,
                int ticksRemaining) {
            this.attacker = attacker;
            this.defender = defender;
            this.target = target;
            this.level = level;
            this.waves = waves;
            this.ticksRemaining = ticksRemaining;
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
