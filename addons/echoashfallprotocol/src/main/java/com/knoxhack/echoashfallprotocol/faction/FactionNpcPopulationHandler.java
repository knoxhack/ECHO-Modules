package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoFactionDefinition;
import com.knoxhack.echocore.api.EchoNpcRole;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.faction.FactionNpcEntity;
import com.knoxhack.echoashfallprotocol.integration.AshfallNpcoreBridge;
import com.knoxhack.echoashfallprotocol.world.ExplorationSiteRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Controlled faction contact population around scanned faction-affinity POIs.
 */
public final class FactionNpcPopulationHandler {
    private static final int TICK_INTERVAL = 600;
    private static final int SPAWN_RADIUS_MIN = 18;
    private static final int SPAWN_RADIUS_MAX = 36;
    private static final int NEARBY_RADIUS = 56;
    private static final int MAX_NEARBY_TOTAL = 4;
    private static final int MAX_NEARBY_FACTION = 2;
    private static final Map<UUID, Long> NEXT_CONTACT_TICK = new HashMap<>();

    private FactionNpcPopulationHandler() {
    }

    public static void onPlayerTick(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime % TICK_INTERVAL != Math.floorMod(player.getUUID().getLeastSignificantBits(), TICK_INTERVAL)) {
            return;
        }
        if (NEXT_CONTACT_TICK.getOrDefault(player.getUUID(), 0L) > gameTime) {
            return;
        }
        QuestData quest = QuestData.get(player);
        if (quest.getDiscoveredPOICount() <= 0) {
            return;
        }
        trySpawnNearPlayer(player, null);
    }

    public static void onPoiDiscovered(ServerPlayer player, String poiId) {
        if (player != null) {
            trySpawnNearPlayer(player, ExplorationSiteRegistry.normalize(poiId));
        }
    }

    private static void trySpawnNearPlayer(ServerPlayer player, String triggeringPoi) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        String normalizedTrigger = ExplorationSiteRegistry.normalize(triggeringPoi);
        if (isDropPod(normalizedTrigger)) {
            return;
        }

        List<EchoFactionDefinition> candidates = EchoCoreServices.factionDefinitions().stream()
                .filter(definition -> AshfallFactionMap.all().contains(definition.id()))
                .filter(definition -> matchesPlayerPoi(player, definition, normalizedTrigger))
                .toList();
        if (candidates.isEmpty() || countNearbyContacts(level, player, null) >= MAX_NEARBY_TOTAL) {
            return;
        }

        EchoFactionDefinition definition = candidates.get(level.getRandom().nextInt(candidates.size()));
        if (countNearbyContacts(level, player, definition.id()) >= MAX_NEARBY_FACTION) {
            return;
        }
        BlockPos pos = findSpawnPos(level, player.blockPosition());
        if (pos == null) {
            return;
        }
        EchoNpcRole role = definition.roles().isEmpty()
                ? new EchoNpcRole("contact", "Contact", "General faction contact.")
                : definition.roles().get(level.getRandom().nextInt(definition.roles().size()));
        if (npcoreLoaded() && AshfallNpcoreBridge.trySpawnContact(player, definition.id(), role.id(), pos)) {
            NEXT_CONTACT_TICK.put(player.getUUID(), level.getGameTime() + 2400L);
            return;
        }

        FactionNpcEntity npc = ModEntities.FACTION_NPC.get().create(level, EntitySpawnReason.EVENT);
        if (npc == null) {
            return;
        }
        npc.configure(definition.id(), role.id());
        npc.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        npc.setYRot(level.getRandom().nextFloat() * 360.0F);
        npc.setXRot(0.0F);
        npc.setPersistenceRequired();
        if (level.addFreshEntity(npc)) {
            NEXT_CONTACT_TICK.put(player.getUUID(), level.getGameTime() + 2400L);
        }
    }

    private static boolean matchesPlayerPoi(ServerPlayer player, EchoFactionDefinition definition, String triggeringPoi) {
        QuestData quest = QuestData.get(player);
        return definition.poiAffinities().stream().anyMatch(affinity -> {
            String profile = ExplorationSiteRegistry.normalize(affinity.profileId());
            if (isDropPod(profile)) {
                return false;
            }
            if (!triggeringPoi.isBlank()) {
                return profile.equals(triggeringPoi);
            }
            return quest.isPOIDiscovered(profile) || quest.hasVisitedLocation("poi", profile);
        });
    }

    private static int countNearby(ServerLevel level, ServerPlayer player, Identifier factionId) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(factionId);
        return level.getEntitiesOfClass(FactionNpcEntity.class, player.getBoundingBox().inflate(NEARBY_RADIUS),
                npc -> npc.isAlive() && (canonical == null || npc.factionId().equals(canonical))).size();
    }

    private static int countNearbyContacts(ServerLevel level, ServerPlayer player, Identifier factionId) {
        int count = countNearby(level, player, factionId);
        return npcoreLoaded() ? count + AshfallNpcoreBridge.countNearby(level, player, factionId, NEARBY_RADIUS) : count;
    }

    private static BlockPos findSpawnPos(ServerLevel level, BlockPos center) {
        for (int attempts = 0; attempts < 18; attempts++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
            int radius = SPAWN_RADIUS_MIN + level.getRandom().nextInt(SPAWN_RADIUS_MAX - SPAWN_RADIUS_MIN + 1);
            int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (isSpawnable(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean isSpawnable(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos)
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).canOcclude();
    }

    private static boolean isDropPod(String poiId) {
        return poiId != null && (poiId.equals("drop_pod") || poiId.contains("drop_pod"));
    }

    private static boolean npcoreLoaded() {
        try {
            return EchoRuntimeModules.isLoaded("echonpcore");
        } catch (RuntimeException ignored) {
            return false;
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
