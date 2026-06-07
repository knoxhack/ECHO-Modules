package com.knoxhack.echosoundcore;

import com.knoxhack.echosoundcore.registry.SoundCoreSounds;
import net.minecraft.sounds.SoundEvent;

public final class SoundCoreStingers {
    private SoundCoreStingers() {}

    public static SoundEvent missionAccept() { return SoundCoreSounds.STINGER_MISSION_ACCEPT.get(); }
    public static SoundEvent missionUpdate() { return SoundCoreSounds.STINGER_MISSION_UPDATE.get(); }
    public static SoundEvent objectiveComplete() { return SoundCoreSounds.STINGER_OBJECTIVE_COMPLETE.get(); }
    public static SoundEvent missionComplete() { return SoundCoreSounds.STINGER_MISSION_COMPLETE.get(); }
    public static SoundEvent signalDetected() { return SoundCoreSounds.STINGER_SIGNAL_DETECTED.get(); }
    public static SoundEvent guardianLocated() { return SoundCoreSounds.STINGER_GUARDIAN_LOCATED.get(); }
    public static SoundEvent nexusStateChanged() { return SoundCoreSounds.STINGER_NEXUS_STATE_CHANGED.get(); }
    public static SoundEvent chapterUnlocked() { return SoundCoreSounds.STINGER_CHAPTER_UNLOCKED.get(); }
    public static SoundEvent rewardAvailable() { return SoundCoreSounds.STINGER_REWARD_AVAILABLE.get(); }
    public static SoundEvent factionRadwardenRepUp() { return SoundCoreSounds.STINGER_FACTION_RADWARDEN_REP_UP.get(); }
    public static SoundEvent factionCrashbreakRepUp() { return SoundCoreSounds.STINGER_FACTION_CRASHBREAK_REP_UP.get(); }
    public static SoundEvent factionSporeboundRepUp() { return SoundCoreSounds.STINGER_FACTION_SPOREBOUND_REP_UP.get(); }
}
