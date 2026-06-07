package com.knoxhack.echoashfallprotocol.boss;

import com.knoxhack.echoashfallprotocol.echo.Mission;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity;
import com.knoxhack.echoashfallprotocol.entity.boss.WardenBossEntity;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import com.knoxhack.echoashfallprotocol.network.BossNavigationPacket;
import com.knoxhack.echoashfallprotocol.world.BiomeGuardianSiteData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Optional;

public final class BossHudTargetResolver {
    private static final double LIVE_BOSS_RADIUS = 160.0D;

    private BossHudTargetResolver() {
    }

    public static BossNavigationPacket resolve(Player player) {
        return findLiveBoss(player)
                .or(() -> findActiveGuardianSite(player))
                .orElseGet(BossNavigationPacket::inactive);
    }

    public static Optional<BossNavigationPacket> findLiveBoss(Player player) {
        AABB box = player.getBoundingBox().inflate(LIVE_BOSS_RADIUS);
        ServerLevel level = (ServerLevel) player.level();
        return level.getEntitiesOfClass(LivingEntity.class, box, BossHudTargetResolver::isSupportedLiveBoss)
                .stream()
                .min(Comparator
                        .comparing((LivingEntity boss) -> !player.hasLineOfSight(boss))
                        .thenComparingDouble(player::distanceToSqr))
                .flatMap(BossHudTargetResolver::packetForLiveBoss);
    }

    public static Optional<BossNavigationPacket> findActiveGuardianSite(Player player) {
        QuestData quest = QuestData.get(player);
        Mission mission = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
        if (mission == null) {
            return Optional.empty();
        }

        BiomeGuardianProfile guardian = BiomeGuardianProfiles.byMissionId(mission.id()).orElse(null);
        if (guardian == null) {
            return Optional.empty();
        }

        return BiomeGuardianSiteData.get((ServerLevel) player.level())
                .nearestActiveForMission(player.blockPosition(), mission.id())
                .flatMap(site -> BossHudProfiles.byEntityId(guardian.entityId())
                        .map(profile -> BossNavigationPacket.active(
                                profile,
                                dimensionId(player),
                                site.entrance(),
                                0,
                                1.0F,
                                guardian.title() + " Entrance",
                                "ENTRANCE"
                        )));
    }

    public static Optional<BossNavigationPacket> packetForLiveBoss(LivingEntity entity) {
        return BossHudProfiles.byEntityId(entityId(entity))
                .map(profile -> packetForLiveBoss(entity, profile, phaseOf(entity, profile)));
    }

    public static BossNavigationPacket packetForLiveBoss(LivingEntity entity, BossHudProfile profile, int phase) {
        float health = Math.max(0.0F, entity.getHealth() / Math.max(1.0F, entity.getMaxHealth()));
        return BossNavigationPacket.active(profile, dimensionId(entity), entity.blockPosition(), phase, health,
                profile.title(), targetKindFor(profile));
    }

    private static String targetKindFor(BossHudProfile profile) {
        return switch (profile.category()) {
            case WARDEN -> "ARCHIVE";
            case ORBITAL -> "ORBITAL";
            default -> "LIVE";
        };
    }

    private static boolean isSupportedLiveBoss(LivingEntity entity) {
        return entity.isAlive() && BossHudProfiles.isSupportedEntityId(entityId(entity));
    }

    private static int phaseOf(LivingEntity entity, BossHudProfile profile) {
        if (entity instanceof BiomeBossEntity guardian) {
            return guardian.getGuardianPhase();
        }
        if (entity instanceof WardenBossEntity warden) {
            return warden.getPhase();
        }
        try {
            Method method = entity.getClass().getMethod("getEncounterPhase");
            Object value = method.invoke(entity);
            if (value instanceof Number number) {
                return Math.max(1, Math.min(3, number.intValue()));
            }
        } catch (ReflectiveOperationException ignored) {
            // Non-Ashfall addon bosses can still fall back to health thresholds.
        }
        float health = Math.max(0.0F, entity.getHealth() / Math.max(1.0F, entity.getMaxHealth()));
        return profile.phaseForHealth(health);
    }

    private static String entityId(LivingEntity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static String dimensionId(Player player) {
        return player.level().dimension().identifier().toString();
    }

    private static String dimensionId(LivingEntity entity) {
        return entity.level().dimension().identifier().toString();
    }
}
