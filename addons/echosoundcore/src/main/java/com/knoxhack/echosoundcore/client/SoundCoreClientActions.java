package com.knoxhack.echosoundcore.client;

import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.SoundCoreChapter;
import com.knoxhack.echosoundcore.SoundCoreCombatIntensity;
import com.knoxhack.echosoundcore.api.SoundCoreClientApi;
import com.knoxhack.echosoundcore.api.context.SoundCoreContext;
import com.knoxhack.echosoundcore.api.context.SoundCoreContextStack;
import com.knoxhack.echosoundcore.client.ambience.SoundCoreAmbienceManager;
import com.knoxhack.echosoundcore.client.music.SoundCoreMusicManager;
import com.knoxhack.echosoundcore.network.SoundCoreAudioAction;
import com.knoxhack.echosoundcore.network.SoundCoreAudioPacket;
import com.knoxhack.echosoundcore.registry.SoundCoreSounds;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class SoundCoreClientActions {
    private static String lastFailure = "";

    private SoundCoreClientActions() {
    }

    public static void handle(SoundCoreAudioPacket packet) {
        if (packet == null || Minecraft.getInstance().player == null) {
            return;
        }
        SoundCoreAudioAction action = packet.action();
        switch (action) {
            case PLAY_ONESHOT -> playOneShot(packet.eventId(), packet.volume(), packet.pitch());
            case STOP_EVENT -> stopEvent(packet.eventId());
            case SET_CONTEXT -> SoundCoreContextStack.setBase(contextFrom(packet.context(), new SoundCoreContext()));
            case PATCH_CONTEXT -> SoundCoreContextStack.setBase(contextFrom(packet.context(), SoundCoreContextStack.current().copy()));
            case CLEAR_CONTEXT -> SoundCoreContextStack.resetBase();
            case PLAY_PROFILE -> SoundCoreMusicManager.playProfile(packet.profileId());
            case STOP_CONTROLLED_AUDIO -> {
                SoundCoreMusicManager.stopControlled();
                SoundCoreAmbienceManager.stopAll();
            }
        }
    }

    public static boolean playOneShot(Identifier eventId, float volume, float pitch) {
        if (SoundCoreMusicManager.isMusicEvent(eventId)) {
            return SoundCoreMusicManager.playSoundEvent(eventId, "event:" + eventId);
        }
        SoundEvent sound = resolveSound(eventId);
        if (sound == null) {
            lastFailure = "Unknown sound event " + eventId;
            EchoSoundCore.LOGGER.warn("SoundCore could not resolve sound event {}.", eventId);
            return false;
        }
        SoundCoreClientApi.playLocalUi(sound, volume, pitch);
        return true;
    }

    public static boolean stopEvent(Identifier eventId) {
        if (eventId == null) {
            return false;
        }
        if (SoundCoreMusicManager.shouldStopForEvent(eventId)) {
            SoundCoreMusicManager.stopControlled();
            return true;
        }
        return SoundCoreAmbienceManager.stopLoop(eventId);
    }

    public static SoundEvent resolveSound(Identifier eventId) {
        if (eventId == null) {
            return null;
        }
        Identifier mapped = mapLegacyEvent(eventId);
        return BuiltInRegistries.SOUND_EVENT.getOptional(mapped).orElse(null);
    }

    public static String lastFailure() {
        return lastFailure;
    }

    private static Identifier mapLegacyEvent(Identifier eventId) {
        if (EchoSoundCore.MODID.equals(eventId.getNamespace())) {
            String path = eventId.getPath();
            if (path.startsWith("ui.signaloos.")) {
                return EchoSoundCore.id(path.replace("ui.signaloos.", "ui.signalos."));
            }
            return eventId;
        }
        String path = eventId.getPath();
        if ("echolens".equals(eventId.getNamespace())) {
            return switch (path) {
                case "sound/scan_start" -> SoundCoreSounds.UI_LENS_DEEP_SCAN_START.getId();
                case "sound/scan_verified" -> SoundCoreSounds.UI_LENS_DEEP_SCAN_COMPLETE.getId();
                case "sound/scan_redacted", "sound/scan_unavailable" -> SoundCoreSounds.UI_LENS_SCAN_INVALID.getId();
                case "sound/action_shortcut" -> SoundCoreSounds.UI_LENS_SCAN_COMPACT.getId();
                default -> eventId;
            };
        }
        return eventId;
    }

    private static SoundCoreContext contextFrom(Map<String, String> values, SoundCoreContext context) {
        if (values == null || values.isEmpty()) {
            return context;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            apply(context, entry.getKey(), entry.getValue());
        }
        return context;
    }

    private static void apply(SoundCoreContext context, String rawKey, String value) {
        if (rawKey == null) {
            return;
        }
        String key = rawKey.toLowerCase(Locale.ROOT);
        try {
            switch (key) {
                case "chapter" -> context.chapter(enumValue(value, SoundCoreChapter.UNKNOWN, SoundCoreChapter.class));
                case "biome" -> context.biome(parseId(value));
                case "region" -> context.region(parseId(value));
                case "structure" -> context.structure(parseId(value));
                case "faction" -> context.faction(parseId(value));
                case "missionid", "mission_id" -> context.missionId(parseId(value));
                case "hazardlevel", "hazard_level" -> context.hazardLevel(parseInt(value, 0));
                case "combatintensity", "combat_intensity" -> context.combatIntensity(enumValue(value, SoundCoreCombatIntensity.NONE, SoundCoreCombatIntensity.class));
                case "bossid", "boss_id" -> context.bossId(parseId(value));
                case "nexuscorruptionlevel", "nexus_corruption_level" -> context.nexusCorruptionLevel(clamp(parseFloat(value, 0.0f), 0.0f, 1.0f));
                case "terminalopen", "terminal_open" -> context.terminalOpen(Boolean.parseBoolean(value));
                case "mapopen", "map_open" -> context.mapOpen(Boolean.parseBoolean(value));
                case "lensscanactive", "lens_scan_active" -> context.lensScanActive(Boolean.parseBoolean(value));
                case "underground" -> context.underground(Boolean.parseBoolean(value));
                case "invehicle", "in_vehicle" -> context.inVehicle(Boolean.parseBoolean(value));
                case "instationorbit", "in_station_orbit" -> context.inStationOrbit(Boolean.parseBoolean(value));
                case "paniclevel", "panic_level" -> context.panicLevel(clamp(parseFloat(value, 0.0f), 0.0f, 1.0f));
                case "powergridalertlevel", "power_grid_alert_level" -> context.powerGridAlertLevel(parseInt(value, 0));
                default -> {
                }
            }
        } catch (RuntimeException exception) {
            lastFailure = "Invalid context value " + rawKey + "=" + value;
        }
    }

    private static Identifier parseId(String value) {
        return value == null || value.isBlank() ? null : Identifier.tryParse(value);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static <E extends Enum<E>> E enumValue(String value, E fallback, Class<E> type) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
