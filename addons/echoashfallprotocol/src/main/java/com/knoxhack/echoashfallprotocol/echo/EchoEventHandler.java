package com.knoxhack.echoashfallprotocol.echo;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.boss.BossHudSync;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Event handler that drives the ECHO-7 system.
 * Ticks once per player per server tick.
 */
public class EchoEventHandler {
    private static final long SLOW_SECTION_WARN_NANOS = 10_000_000L;
    private static final long SLOW_LOG_COOLDOWN_TICKS = 200L;
    private static final Map<String, Long> SLOW_LOG_TICKS = new HashMap<>();

    public static void onPlayerTick(Object event) {
        if (eventValue(event, "getEntity") instanceof ServerPlayer serverPlayer) {
            // Only tick every 20 ticks (1 second) for performance
            long gameTime = serverPlayer.level().getGameTime();
            if (gameTime % 20 == 0) {
                long guideStart = System.nanoTime();
                EchoGuideManager.tick(serverPlayer);
                logSlow(serverPlayer, "echo.guide_tick", guideStart, gameTime);
                long bossStart = System.nanoTime();
                BossHudSync.syncBestTarget(serverPlayer);
                logSlow(serverPlayer, "echo.boss_hud_sync", bossStart, gameTime);
            }
        }
    }

    private static void logSlow(ServerPlayer player, String section, long startNanos, long gameTime) {
        long elapsed = System.nanoTime() - startNanos;
        if (elapsed < SLOW_SECTION_WARN_NANOS) {
            return;
        }
        String key = section + ":" + player.getUUID();
        long lastLogTick = SLOW_LOG_TICKS.getOrDefault(key, Long.MIN_VALUE);
        if (lastLogTick != Long.MIN_VALUE && gameTime - lastLogTick < SLOW_LOG_COOLDOWN_TICKS) {
            return;
        }
        SLOW_LOG_TICKS.put(key, gameTime);
        EchoAshfallProtocol.LOGGER.warn("{} for {} took {} ms.",
                section,
                player.getName().getString(),
                String.format(java.util.Locale.ROOT, "%.2f", elapsed / 1_000_000.0D));
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
