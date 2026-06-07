package com.knoxhack.echosoundcore.integration.mission;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionRuntimeBus;
import com.knoxhack.echocore.api.mission.MissionRuntimeEvent;
import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.registry.SoundCoreSounds;
import net.minecraft.resources.Identifier;

public final class SoundCoreMissionIntegration {
    private static boolean registered;

    private SoundCoreMissionIntegration() {}

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoSoundCore.LOGGER.info("SoundCore MissionCore integration registered.");
        MissionRuntimeBus.register(SoundCoreMissionIntegration::onMissionEvent);
    }

    private static void onMissionEvent(MissionRuntimeEvent event) {
        if (event == null || event.eventType() == null) {
            return;
        }
        Identifier stinger = stingerFor(event.eventType());
        if (stinger != null) {
            EchoCoreServices.soundService().playEvent(stinger);
        }
    }

    private static Identifier stingerFor(Identifier eventType) {
        if (MissionRuntimeEvent.MISSION_STARTED.equals(eventType)) {
            return SoundCoreSounds.STINGER_MISSION_ACCEPT.getId();
        }
        if (MissionRuntimeEvent.OBJECTIVE_PROGRESSED.equals(eventType)) {
            return SoundCoreSounds.STINGER_OBJECTIVE_COMPLETE.getId();
        }
        if (MissionRuntimeEvent.MISSION_COMPLETED.equals(eventType)) {
            return SoundCoreSounds.STINGER_MISSION_COMPLETE.getId();
        }
        if (MissionRuntimeEvent.REWARD_CLAIMED.equals(eventType)) {
            return SoundCoreSounds.STINGER_REWARD_AVAILABLE.getId();
        }
        if (MissionRuntimeEvent.CHAPTER_UNLOCKED.equals(eventType)) {
            return SoundCoreSounds.STINGER_CHAPTER_UNLOCKED.getId();
        }
        return null;
    }
}
