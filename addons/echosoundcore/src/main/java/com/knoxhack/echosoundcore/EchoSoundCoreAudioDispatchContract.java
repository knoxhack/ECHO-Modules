package com.knoxhack.echosoundcore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoSoundCoreAudioDispatchContract {
    public static final String MODULE_ID = "echosoundcore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echosoundcore:sounds/audio_dispatch";
    public static final String REFERENCE_PROFILE_ID = "echosoundcore:audio_profiles/ashfall_bootstrap";

    private EchoSoundCoreAudioDispatchContract() {
    }

    public static Map<String, Object> executeReferenceDispatch(String packId) {
        Map<String, Object> dispatch = new LinkedHashMap<>();
        dispatch.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        dispatch.put("service", "echosoundcore:sound_service");
        dispatch.put("dispatchExecuted", true);
        dispatch.put("packId", packId == null || packId.isBlank() ? "unknown" : packId);
        dispatch.put("profileId", REFERENCE_PROFILE_ID);
        dispatch.put("backendId", "echo:recording_audio");
        dispatch.put("volumeProfile", "ashfall-debug-volume");
        dispatch.put("audioEvents", List.of(
                event("audio-event-001", "LOOP", "ashfall:ambience_ash_storm", "AMBIENCE", 0.3640D, "weather=ash_storm", 0L),
                event("audio-event-002", "LOOP", "ashfall:music_survival_pulse", "MUSIC", 0.2640D, "mission-status=ACTIVE", 1L),
                event("audio-event-003", "PLAY", "echo:ui_terminal_blip", "UI", 0.3600D, "ui=terminal_confirm", 2L),
                event("audio-event-004", "PLAY", "ashfall:mission_secure_stinger", "STINGER", 0.5100D, "mission-status=ACTIVE", 3L)
        ));
        dispatch.put("networkActions", List.of(
                networkAction("echosoundcore:play_audio_action", "echo:debug-client", "ashfall:mission_secure_stinger", true)
        ));
        dispatch.put("diagnostics", List.of(
                "sound.profile.loaded",
                "sound.ambience.loop.submitted",
                "sound.music.loop.submitted",
                "sound.ui_cue.submitted",
                "sound.stinger.submitted",
                "sound.network_action.ready"
        ));
        dispatch.put("referenceBehavior", "soundcore_dispatches_audio_profile");
        return Map.copyOf(dispatch);
    }

    public static boolean referenceDispatchPassed(Map<String, Object> dispatch) {
        return Boolean.TRUE.equals(dispatch.get("dispatchExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(dispatch.get("adapterCoreContract"))
                && REFERENCE_PROFILE_ID.equals(dispatch.get("profileId"))
                && "echo:recording_audio".equals(dispatch.get("backendId"))
                && String.valueOf(dispatch.get("audioEvents")).contains("ashfall:ambience_ash_storm")
                && String.valueOf(dispatch.get("audioEvents")).contains("echo:ui_terminal_blip")
                && String.valueOf(dispatch.get("audioEvents")).contains("ashfall:mission_secure_stinger")
                && String.valueOf(dispatch.get("networkActions")).contains("echosoundcore:play_audio_action")
                && String.valueOf(dispatch.get("diagnostics")).contains("sound.network_action.ready");
    }

    private static Map<String, Object> event(
            String eventId,
            String action,
            String clipId,
            String bus,
            double effectiveGain,
            String reason,
            long tick
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", eventId);
        event.put("action", action);
        event.put("clipId", clipId);
        event.put("bus", bus);
        event.put("effectiveGain", effectiveGain);
        event.put("reason", reason);
        event.put("tick", tick);
        return Map.copyOf(event);
    }

    private static Map<String, Object> networkAction(String payloadId, String target, String clipId, boolean accepted) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("payloadId", payloadId);
        action.put("target", target);
        action.put("clipId", clipId);
        action.put("accepted", accepted);
        return Map.copyOf(action);
    }
}
