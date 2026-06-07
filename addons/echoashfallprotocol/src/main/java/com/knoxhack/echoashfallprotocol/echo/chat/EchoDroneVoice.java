package com.knoxhack.echoashfallprotocol.echo.chat;

import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.echo.Mission;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.registry.ModSounds;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Facade that routes ECHO-7 messages through the player's deployed Echo Companion Drone
 * when it is nearby, turning the drone into ECHO-7's physical voice & body.
 *
 * Falls back to player-centered delivery when no drone is available.
 */
public final class EchoDroneVoice {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Max distance (blocks) for the drone to be the speaker. */
    public static final double DRONE_SPEAK_RANGE = 32.0;
    public static final double DRONE_SPEAK_RANGE_SQ = DRONE_SPEAK_RANGE * DRONE_SPEAK_RANGE;
    private static final long DRONE_LOOKUP_HIT_CACHE_TICKS = 100L;
    private static final long DRONE_LOOKUP_MISS_CACHE_TICKS = 20L;
    private static final Map<UUID, CachedDroneLookup> DRONE_LOOKUP_CACHE = new HashMap<>();

    public enum EventType {
        MISSION_START(1.2f, EchoCompanionDrone.MOOD_PROFESSIONAL, 40),
        MISSION_READY(1.4f, EchoCompanionDrone.MOOD_CHEERFUL, 40),
        MISSION_COMPLETE(1.5f, EchoCompanionDrone.MOOD_CHEERFUL, 60),
        PHASE_UNLOCK(0.9f, EchoCompanionDrone.MOOD_REFLECTIVE, 80),
        CRITICAL_ALERT(0.7f, EchoCompanionDrone.MOOD_URGENT, 60),
        INFO(1.0f, EchoCompanionDrone.MOOD_PROFESSIONAL, 30),
        IDLE(1.0f, EchoCompanionDrone.MOOD_PROFESSIONAL, 0);

        public final float pitch;
        public final int defaultMood;
        public final int alertTicks;

        EventType(float pitch, int defaultMood, int alertTicks) {
            this.pitch = pitch;
            this.defaultMood = defaultMood;
            this.alertTicks = alertTicks;
        }
    }

    private EchoDroneVoice() {}

    /**
     * Find the player's bonded drone if it is in the same level and within speak range.
     * Returns null otherwise.
     */
    public static EchoCompanionDrone findDrone(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return null;
        UUID playerId = player.getUUID();
        long now = level.getGameTime();
        String dimension = level.dimension().toString();
        CachedDroneLookup cached = DRONE_LOOKUP_CACHE.get(playerId);
        if (cached != null && dimension.equals(cached.dimension())) {
            EchoCompanionDrone cachedDrone = cached.drone();
            long maxAge = cachedDrone == null ? DRONE_LOOKUP_MISS_CACHE_TICKS : DRONE_LOOKUP_HIT_CACHE_TICKS;
            if (now - cached.gameTime() <= maxAge) {
                if (cachedDrone == null) {
                    return null;
                }
                if (cachedDrone.isAlive() && cachedDrone.level() == level
                        && cachedDrone.distanceToSqr(player) <= DRONE_SPEAK_RANGE_SQ) {
                    return cachedDrone;
                }
            }
        }

        AABB box = player.getBoundingBox().inflate(DRONE_SPEAK_RANGE);
        List<EchoCompanionDrone> drones = level.getEntitiesOfClass(EchoCompanionDrone.class, box,
                d -> playerId.equals(d.getOwnerUUID()) && d.isAlive());
        if (drones.isEmpty()) {
            DRONE_LOOKUP_CACHE.put(playerId, new CachedDroneLookup(dimension, now, null));
            return null;
        }
        EchoCompanionDrone nearest = null;
        double bestSq = DRONE_SPEAK_RANGE_SQ;
        for (EchoCompanionDrone d : drones) {
            double sq = d.distanceToSqr(player);
            if (sq <= bestSq) {
                bestSq = sq;
                nearest = d;
            }
        }
        DRONE_LOOKUP_CACHE.put(playerId, new CachedDroneLookup(dimension, now, nearest));
        return nearest;
    }

    /**
     * Resolve ECHO-7's current mood for the player, preferring chat session state,
     * falling back to a survival-data-derived mood.
     */
    public static int resolveMood(ServerPlayer player) {
        EchoChatSystem.ChatSession session = EchoChatSystem.getSession(player.getUUID());
        if (session != null) {
            return session.personality.getCurrentMood().ordinal();
        }
        SurvivalData sv = player.getData(ModAttachments.SURVIVAL_DATA.get());
        float radPct = sv.getRadiationLevel() / SurvivalData.MAX_RADIATION;
        int healthPct = (int)((player.getHealth() / player.getMaxHealth()) * 100);
        boolean maskOk = sv.hasMask() && sv.getFilterPercent() > 0;
        if (healthPct < 30 || (!maskOk && radPct > 0.5f)) return EchoCompanionDrone.MOOD_URGENT;
        if (healthPct < 60 || !maskOk || radPct > 0.3f) return EchoCompanionDrone.MOOD_CONCERNED;
        return EchoCompanionDrone.MOOD_PROFESSIONAL;
    }

    /**
     * Strip Minecraft color codes (§x) for hologram display.
     */
    public static String stripColors(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replaceAll("\u00a7.", "");
    }

    /**
     * Emit an ECHO-7 message through the drone if present.
     * Returns true if routed through the drone, false if the caller should fall back
     * to the player-centered delivery.
     */
    public static boolean relayMessage(ServerPlayer player, String message, EventType type) {
        EchoCompanionDrone drone = findDrone(player);
        if (drone == null) return false;

        int mood = resolveMood(player);
        String holo = stripColors(message);
        // Strip [ECHO-7] / [ECHO-7 // DRONE] prefix from hologram (already implied by source).
        holo = holo.replaceFirst("^\\[ECHO-7[^\\]]*\\]\\s*", "");
        int holdTicks = Math.min(300, 40 + holo.length() * 2);
        drone.speak(holo, mood, holdTicks, type.alertTicks);

        // Play ECHO sound at drone's position for spatial effect
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, drone.getX(), drone.getY(), drone.getZ(),
                    ModSounds.ECHO_MESSAGE.get(), SoundSource.PLAYERS, 0.9f, type.pitch);
        }
        return true;
    }

    /**
     * Force a specific mood onto the drone (if present) without speaking.
     */
    public static void syncMood(ServerPlayer player) {
        EchoCompanionDrone drone = findDrone(player);
        if (drone != null) drone.setMoodId(resolveMood(player));
    }

    /**
     * Trigger a visual/audio event on the drone without a text line.
     */
    public static void triggerEvent(ServerPlayer player, EventType type) {
        EchoCompanionDrone drone = findDrone(player);
        if (drone == null) return;
        drone.triggerAlert(type.alertTicks);
        drone.setMoodId(resolveMood(player));
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, drone.getX(), drone.getY(), drone.getZ(),
                    ModSounds.ECHO_MESSAGE.get(), SoundSource.PLAYERS, 0.8f, type.pitch);
        }
    }

    // ---------- Idle chatter ----------

    /** Per-player cooldown between idle chatter lines (ticks). */
    private static final int MIN_CHATTER_GAP = 900;  // 45s
    private static final int MAX_CHATTER_GAP = 1800; // 90s
    /** Don't chatter if another ECHO message fired within this many ticks. */
    private static final int RECENT_MESSAGE_WINDOW = 400;

    /**
     * Called each server tick from EchoGuideManager. Emits occasional
     * contextual hints through the drone when nothing else is happening.
     */
    public static boolean tickIdleChatter(ServerPlayer player, QuestData quest) {
        long now = player.level().getGameTime();

        // Skip if chat recently fired (terminal / mission event)
        if (now - quest.getLastMessageTick() < RECENT_MESSAGE_WINDOW) return false;

        long nextDue = quest.getNextDroneChatterTick();
        if (nextDue == 0L) {
            // First run: schedule initial delay
            quest.setNextDroneChatterTick(now + MIN_CHATTER_GAP + player.level().getRandom().nextInt(MAX_CHATTER_GAP - MIN_CHATTER_GAP));
            return true;
        }
        if (now < nextDue) return false;

        EchoCompanionDrone drone = findDrone(player);
        if (drone == null || drone.isSpeaking()) {
            // Reschedule and wait
            quest.setNextDroneChatterTick(now + MIN_CHATTER_GAP);
            return true;
        }
        if (drone.getRepairLevel() < EchoCompanionDrone.REPAIR_FOLLOW) {
            quest.setNextDroneChatterTick(now + MIN_CHATTER_GAP);
            return true;
        }

        String line = pickIdleLine(player, quest);
        if (line != null && !line.isEmpty()) {
            int mood = resolveMood(player);
            drone.speak(line, mood, 120, 0); // hologram only, no chat, no alert
            // Soft chirp from drone position
            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, drone.getX(), drone.getY(), drone.getZ(),
                        ModSounds.ECHO_MESSAGE.get(), SoundSource.PLAYERS, 0.4f, 1.3f);
            }
        }

        int gap = MIN_CHATTER_GAP + player.level().getRandom().nextInt(MAX_CHATTER_GAP - MIN_CHATTER_GAP);
        quest.setNextDroneChatterTick(now + gap);
        return true;
    }

    private record CachedDroneLookup(String dimension, long gameTime, EchoCompanionDrone drone) {}

    private static String pickIdleLine(ServerPlayer player, QuestData quest) {
        SurvivalData sv = player.getData(ModAttachments.SURVIVAL_DATA.get());
        float radPct = sv.getRadiationLevel() / SurvivalData.MAX_RADIATION;
        Mission mission = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());

        // Priority hazard hints
        if (sv.isToxicAirActive() && !sv.hasMask()) return "Toxic pocket detected. Mask up or leave the hazard.";
        if (sv.isToxicAirActive() && sv.getFilterPercent() <= 0 && sv.hasMask()) return "Filter depleted inside hazard air. Retreat or swap it.";
        if (radPct > 0.75f) return "Radiation saturation critical. Move to shelter.";

        // Mission-contextual hints
        if (mission != null) {
            if (mission.isTurnInMission()) {
                if (mission.hasRequiredItems(player)) {
                    return "Requirements satisfied. ECHO turn-in ready.";
                }
                // Pick first missing item and name it
                try {
                    var progress = mission.getItemProgress(player);
                    for (var ip : progress) {
                        if (!ip.satisfied()) {
                            int remaining = ip.need() - ip.have();
                            return "Still tracking " + remaining + "x " + ip.item().getHoverName().getString() + ".";
                        }
                    }
                } catch (Throwable t) {
                    LOGGER.warn("Failed to compute mission item progress for ECHO-7 voice line", t);
                }
            }
            return "Active objective: " + mission.objectiveText();
        }

        // Phase-based fallbacks
        int phase = quest.getCurrentPhase();
        return switch (phase) {
            case 0 -> "Phase 0: establish shelter, craft a tool, secure water.";
            case 1 -> "Phase 1: scavenge safely and build the starter outpost.";
            case 2 -> "Phase 2: stabilize water, power, and toxic-route gear.";
            case 3 -> "Phase 3: scan routes, pack supplies, and keep exposure brief.";
            default -> "All systems nominal. Awaiting orders.";
        };
    }
}
