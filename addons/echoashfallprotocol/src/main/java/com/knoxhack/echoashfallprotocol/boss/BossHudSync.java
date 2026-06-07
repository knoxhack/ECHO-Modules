package com.knoxhack.echoashfallprotocol.boss;

import com.knoxhack.echoashfallprotocol.network.BossNavigationPacket;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BossHudSync {
    private static final double LIVE_SYNC_RADIUS = 160.0D;
    private static final long BEST_TARGET_SCAN_INTERVAL_TICKS = 40L;
    private static final long BEST_TARGET_KEEPALIVE_TICKS = 200L;
    private static final float HEALTH_SEND_EPSILON = 0.02F;
    private static final Map<UUID, CachedBossTarget> BEST_TARGET_CACHE = new HashMap<>();

    private BossHudSync() {
    }

    public static void syncBestTarget(ServerPlayer player) {
        UUID playerId = player.getUUID();
        long gameTime = player.level().getGameTime();
        String dimension = player.level().dimension().toString();
        CachedBossTarget cached = BEST_TARGET_CACHE.get(playerId);
        if (cached != null
                && dimension.equals(cached.dimension())
                && gameTime - cached.lastScanTick() < BEST_TARGET_SCAN_INTERVAL_TICKS) {
            return;
        }

        BossNavigationPacket packet = BossHudTargetResolver.resolve(player);
        boolean shouldSend = cached == null
                || !dimension.equals(cached.dimension())
                || meaningfullyChanged(cached.lastSent(), packet)
                || gameTime - cached.lastSendTick() >= BEST_TARGET_KEEPALIVE_TICKS;
        if (shouldSend) {
            EchoNetSend.toPlayer(player, packet, EchoPacketKind.CLIENTBOUND_SYNC);
            BEST_TARGET_CACHE.put(playerId, new CachedBossTarget(dimension, gameTime, gameTime, packet));
        } else {
            BEST_TARGET_CACHE.put(playerId, new CachedBossTarget(dimension, gameTime, cached.lastSendTick(), cached.lastSent()));
        }
    }

    public static void syncLiveBoss(LivingEntity boss, int phase) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        BossHudProfiles.byEntityId(BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString())
                .map(profile -> BossHudTargetResolver.packetForLiveBoss(boss, profile, phase))
                .ifPresent(packet -> {
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                    boss.getBoundingBox().inflate(LIVE_SYNC_RADIUS), ServerPlayer::isAlive)) {
                EchoNetSend.toPlayer(player, packet, EchoPacketKind.CLIENTBOUND_SYNC);
            }
        });
    }

    public static void clearBoss(ServerPlayer player, String bossId) {
        BEST_TARGET_CACHE.remove(player.getUUID());
        EchoNetSend.toPlayer(player, BossNavigationPacket.inactive(bossId), EchoPacketKind.CLIENTBOUND_SYNC);
    }

    private static boolean meaningfullyChanged(BossNavigationPacket previous, BossNavigationPacket next) {
        if (previous == null) return true;
        if (previous.active() != next.active()) return true;
        if (!previous.bossId().equals(next.bossId())) return true;
        if (!previous.dimension().equals(next.dimension())) return true;
        if (previous.x() != next.x() || previous.y() != next.y() || previous.z() != next.z()) return true;
        if (previous.phase() != next.phase()) return true;
        if (!previous.targetKind().equals(next.targetKind())) return true;
        if (!previous.title().equals(next.title())) return true;
        return Math.abs(previous.healthPercent() - next.healthPercent()) >= HEALTH_SEND_EPSILON;
    }

    private record CachedBossTarget(String dimension, long lastScanTick, long lastSendTick, BossNavigationPacket lastSent) {}
}
