package com.knoxhack.echoashfallprotocol.client.hud;

import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventStatus;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * Client-side HUD state singleton.
 * Controls display mode cycling: NORMAL -> COMPACT -> EXTENDED -> NORMAL.
 */
public final class HudState {

    // HUD display mode
    public enum DisplayMode {
        COMPACT,   // Minimal: 3 thin bars only
        NORMAL,    // Full panel: bars + mutations + mission tracker
        EXTENDED   // Full immersive: all panels + radar + situation report
    }

    private static DisplayMode mode = DisplayMode.NORMAL;

    // Nexus state, synced from server
    private static NexusWorldData.WorldState nexusState = NexusWorldData.WorldState.NORMAL;
    private static String nexusPlayer = "";
    private static int nexusX = 0, nexusY = 0, nexusZ = 0;
    private static boolean nexusCampaignAwakened = false;
    private static int nexusInstability = 0;
    private static int nexusRelaysScanned = 0;
    private static int nexusRelaysResolved = 0;
    private static int nexusReadinessRestore = 0;
    private static int nexusReadinessDestroy = 0;
    private static int nexusReadinessControl = 0;
    private static boolean nexusSiegeComplete = false;
    private static boolean nexusWarfrontComplete = false;
    private static boolean nexusWardenDefeated = false;
    private static boolean nexusFinaleComplete = false;
    private static String nexusRelaySummaryPayload = "";
    private static BossTarget bossTarget = null;
    private static long bossTargetUpdatedAtTick = 0L;
    private static final long BOSS_TARGET_TIMEOUT_TICKS = 80L;

    private HudState() {}

    public static DisplayMode getMode() { return mode; }

    public static void cycleMode() {
        mode = switch (mode) {
            case NORMAL   -> DisplayMode.COMPACT;
            case COMPACT  -> DisplayMode.EXTENDED;
            case EXTENDED -> DisplayMode.NORMAL;
        };
    }

    public static void setMode(DisplayMode m) { mode = m; }

    // Nexus state getters/setters
    public static NexusWorldData.WorldState getNexusState() { return nexusState; }
    public static void setNexusState(NexusWorldData.WorldState state) { nexusState = state; }
    public static String getNexusPlayer() { return nexusPlayer; }
    public static void setNexusPlayer(String player) { nexusPlayer = player; }
    public static int getNexusX() { return nexusX; }
    public static int getNexusY() { return nexusY; }
    public static int getNexusZ() { return nexusZ; }
    public static void setNexusPos(int x, int y, int z) { nexusX = x; nexusY = y; nexusZ = z; }
    public static boolean isNexusCampaignAwakened() { return nexusCampaignAwakened; }
    public static int getNexusInstability() { return nexusInstability; }
    public static int getNexusRelaysScanned() { return nexusRelaysScanned; }
    public static int getNexusRelaysResolved() { return nexusRelaysResolved; }
    public static int getNexusReadinessRestore() { return nexusReadinessRestore; }
    public static int getNexusReadinessDestroy() { return nexusReadinessDestroy; }
    public static int getNexusReadinessControl() { return nexusReadinessControl; }
    public static boolean isNexusSiegeComplete() { return nexusSiegeComplete; }
    public static boolean isNexusWarfrontComplete() { return nexusWarfrontComplete; }
    public static boolean isNexusWardenDefeated() { return nexusWardenDefeated; }
    public static boolean isNexusFinaleComplete() { return nexusFinaleComplete; }
    public static String getNexusRelaySummaryPayload() { return nexusRelaySummaryPayload; }
    public static List<String> getNexusRelaySummaryLines() {
        if (nexusRelaySummaryPayload == null || nexusRelaySummaryPayload.isBlank()) {
            return List.of();
        }
        return Arrays.stream(nexusRelaySummaryPayload.split("\\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }
    public static void setNexusRelaySummaryPayload(String payload) {
        nexusRelaySummaryPayload = payload == null ? "" : payload;
    }
    public static void setNexusCampaign(boolean awakened, int instability, int relaysScanned, int relaysResolved,
                                        int readinessRestore, int readinessDestroy, int readinessControl,
                                        boolean siegeComplete, boolean warfrontComplete,
                                        boolean wardenDefeated, boolean finaleComplete) {
        nexusCampaignAwakened = awakened;
        nexusInstability = Math.max(0, Math.min(100, instability));
        nexusRelaysScanned = Math.max(0, relaysScanned);
        nexusRelaysResolved = Math.max(0, relaysResolved);
        nexusReadinessRestore = Math.max(0, readinessRestore);
        nexusReadinessDestroy = Math.max(0, readinessDestroy);
        nexusReadinessControl = Math.max(0, readinessControl);
        nexusSiegeComplete = siegeComplete;
        nexusWarfrontComplete = warfrontComplete;
        nexusWardenDefeated = wardenDefeated;
        nexusFinaleComplete = finaleComplete;
    }

    public static void setBossTarget(BossTarget target) {
        bossTarget = target != null && target.active() ? target : null;
        bossTargetUpdatedAtTick = clientGameTime();
    }

    public static void clearBossTarget(String bossId) {
        if (bossTarget == null || bossId == null || bossId.isBlank() || bossTarget.bossId().equals(bossId)) {
            bossTarget = null;
        }
        bossTargetUpdatedAtTick = clientGameTime();
    }

    public static BossTarget getBossTarget() {
        if (bossTarget == null) {
            return null;
        }
        long now = clientGameTime();
        if (now > 0L && bossTargetUpdatedAtTick > 0L
                && now - bossTargetUpdatedAtTick > BOSS_TARGET_TIMEOUT_TICKS) {
            bossTarget = null;
            return null;
        }
        return bossTarget;
    }

    private static long clientGameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    public record BossTarget(
            boolean active,
            String bossId,
            String title,
            String subtitle,
            String dimension,
            int x,
            int y,
            int z,
            int phase,
            float healthPercent,
            int accentColor,
            String compassLabel,
            String category,
            String targetKind
    ) {
        public boolean isLiveBoss() {
            return "LIVE".equals(targetKind) || "ARCHIVE".equals(targetKind) || "ORBITAL".equals(targetKind);
        }
    }

    // Environmental event state, synced from server
    private static String envEventType = "NONE";
    private static long envEventStart = 0;
    private static int envEventDuration = 0;
    private static long lastEnvSyncTime = 0;
    private static float envEventIntensity = 0.0F;
    private static float envEventPhase = 0.0F;
    private static long envEventSeed = 0L;
    private static int radStormsSurvived = 0;
    private static int toxicStormsSurvived = 0;
    private static int blackoutsSurvived = 0;
    private static int ashStormsSurvived = 0;
    private static int cryoFrontsSurvived = 0;
    private static int nexusSurgesSurvived = 0;

    // Grace countdown state, derived from the synced survival attachment.
    private static long graceTicksRemaining = 0L;
    private static boolean graceActive = false;
    private static boolean graceExpiryShown = false;

    public static String getEnvEventType() { return envEventType; }
    public static void setEnvEvent(String type, long startTime, int duration, long gameTime,
                                   float intensity, float phase, long seed) {
        setEnvEvent(type, startTime, duration, gameTime, intensity, phase, seed,
                radStormsSurvived, toxicStormsSurvived, blackoutsSurvived,
                ashStormsSurvived, cryoFrontsSurvived, nexusSurgesSurvived);
    }

    public static void setEnvEvent(String type, long startTime, int duration, long gameTime,
                                   float intensity, float phase, long seed,
                                   int radiationCount, int toxicCount, int blackoutCount,
                                   int ashCount, int cryoCount, int nexusCount) {
        envEventType = type;
        envEventStart = startTime;
        envEventDuration = duration;
        lastEnvSyncTime = gameTime;
        envEventIntensity = intensity;
        envEventPhase = phase;
        envEventSeed = seed;
        radStormsSurvived = Math.max(0, radiationCount);
        toxicStormsSurvived = Math.max(0, toxicCount);
        blackoutsSurvived = Math.max(0, blackoutCount);
        ashStormsSurvived = Math.max(0, ashCount);
        cryoFrontsSurvived = Math.max(0, cryoCount);
        nexusSurgesSurvived = Math.max(0, nexusCount);
    }
    public static long getEnvEventStart() { return envEventStart; }
    public static int getEnvEventDuration() { return envEventDuration; }
    public static long getLastEnvSyncTime() { return lastEnvSyncTime; }
    public static float getEnvEventIntensity() { return envEventIntensity; }
    public static float getEnvEventPhase() { return envEventPhase; }
    public static long getEnvEventSeed() { return envEventSeed; }
    public static int getEnvEventSurvivalCount(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM -> radStormsSurvived;
            case TOXIC_STORM -> toxicStormsSurvived;
            case BLACKOUT -> blackoutsSurvived;
            case ASH_STORM -> ashStormsSurvived;
            case CRYO_FRONT -> cryoFrontsSurvived;
            case NEXUS_SURGE -> nexusSurgesSurvived;
            default -> 0;
        };
    }

    public static int getEnvEventRemainingTicks(long gameTime) {
        if ("NONE".equals(envEventType) || envEventDuration <= 0) {
            return 0;
        }
        if (gameTime > 0L && envEventStart > 0L) {
            return Math.max(0, envEventDuration - (int) Math.max(0L, gameTime - envEventStart));
        }
        return Math.max(0, envEventDuration - Math.round(envEventDuration * envEventPhase));
    }

    public static float getEnvEventComputedPhase(long gameTime) {
        if ("NONE".equals(envEventType) || envEventDuration <= 0) {
            return 0.0F;
        }
        if (gameTime > 0L && envEventStart > 0L) {
            float elapsed = Math.max(0L, gameTime - envEventStart);
            return Math.max(0.0F, Math.min(1.0F, elapsed / (float) envEventDuration));
        }
        return envEventPhase;
    }

    public static EnvironmentalEventStatus getEnvironmentalEventStatus(long gameTime) {
        EnvironmentalEventType type = EnvironmentalEventStatus.parseType(envEventType);
        return EnvironmentalEventStatus.fromSynced(envEventType,
                getEnvEventRemainingTicks(gameTime),
                envEventDuration,
                envEventIntensity,
                getEnvEventComputedPhase(gameTime),
                getEnvEventSurvivalCount(type));
    }

    public static void setGraceTicksRemaining(long ticksRemaining) {
        setGraceCountdown(ticksRemaining, ticksRemaining > 0L);
    }

    public static void setGraceCountdown(long ticksRemaining, boolean active) {
        long clamped = Math.max(0L, ticksRemaining);
        if (clamped > 0L && graceTicksRemaining <= 0L) {
            graceExpiryShown = false;
        }
        graceTicksRemaining = clamped;
        graceActive = active && clamped > 0L;
    }

    public static long getGraceTicksRemaining() { return graceTicksRemaining; }
    public static boolean isGraceActive() { return graceActive; }
    public static boolean isGraceEndingSoon() { return graceTicksRemaining > 0L && graceTicksRemaining <= 20L * 60L; }

    public static String getGraceCountdownText() {
        long secondsRemaining = Math.max(1L, (graceTicksRemaining + 19L) / 20L);
        return String.format("%d:%02d", secondsRemaining / 60L, secondsRemaining % 60L);
    }

    public static boolean consumeGraceExpiryNotice() {
        if (graceTicksRemaining > 0L || graceExpiryShown) {
            return false;
        }
        graceExpiryShown = true;
        return true;
    }
}
