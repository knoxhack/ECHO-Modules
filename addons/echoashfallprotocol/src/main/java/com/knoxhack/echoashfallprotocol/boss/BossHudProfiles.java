package com.knoxhack.echoashfallprotocol.boss;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.endgame.NexusFinalBossProfile;
import com.knoxhack.echoashfallprotocol.endgame.NexusFinalBossProfiles;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class BossHudProfiles {
    private static final String ORBITAL_MODID = "echoorbitalremnants";
    private static final Map<String, BossHudProfile> BY_ID = new LinkedHashMap<>();
    private static final Map<String, BossHudProfile> BY_TITLE = new LinkedHashMap<>();
    private static boolean loaded;

    private BossHudProfiles() {
    }

    public static Collection<BossHudProfile> all() {
        ensureLoaded();
        return BY_ID.values();
    }

    public static Optional<BossHudProfile> byEntityId(String entityId) {
        ensureLoaded();
        return Optional.ofNullable(BY_ID.get(normalize(entityId)));
    }

    public static Optional<BossHudProfile> byTitle(String title) {
        ensureLoaded();
        return Optional.ofNullable(BY_TITLE.get(titleKey(title)));
    }

    public static boolean isSupportedEntityId(String entityId) {
        return byEntityId(entityId).isPresent();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
            BiomeGuardianProfile.CinematicCue cue = profile.cinematicCue();
            BiomeGuardianProfile.PolishData polish = profile.polish();
            register(new BossHudProfile(
                    profile.entityId(),
                    profile.title(),
                    cue.dangerVerb() + " / " + polish.counterplayObject(),
                    profile.color(),
                    cue.objectiveLabel(),
                    cue.phaseWarningLabel(),
                    polish.counterplayObject(),
                    BossHudProfile.BossCategory.GUARDIAN,
                    0.66F,
                    0.33F
            ));
        }
        register(new BossHudProfile(
                EchoAshfallProtocol.MODID + ":warden_boss",
                "The Warden",
                "Archive judgment / defender lockdown",
                0xFFC8A4FF,
                "Archive",
                "ARCHIVE LOCKDOWN",
                "Break defenders and keep moving",
                BossHudProfile.BossCategory.WARDEN,
                0.60F,
                0.30F
        ));
        for (NexusFinalBossProfile profile : NexusFinalBossProfiles.all()) {
            register(new BossHudProfile(
                    profile.entityId(),
                    profile.title(),
                    profile.subtitle(),
                    profile.accentColor(),
                    "Finale",
                    "FINAL PROTOCOL",
                    profile.counterplay(),
                    BossHudProfile.BossCategory.WARDEN,
                    0.66F,
                    0.33F
            ));
        }
        register(new BossHudProfile(
                ORBITAL_MODID + ":corrupted_docking_ai",
                "Corrupted Docking AI",
                "Airlock pressure / reserve drones",
                0xFFFF5C5C,
                "Docking AI",
                "AIRLOCK HARD LOCK",
                "Keep pressure sealed",
                BossHudProfile.BossCategory.ORBITAL,
                0.65F,
                0.35F
        ));
        register(new BossHudProfile(
                ORBITAL_MODID + ":abandoned_captain",
                "The Abandoned Captain",
                "Oxygen drain / broken crew",
                0xFFFFC95C,
                "Captain",
                "CREW DISTRESS LOOP",
                "Protect oxygen and clear crew",
                BossHudProfile.BossCategory.ORBITAL,
                0.65F,
                0.35F
        ));
        register(new BossHudProfile(
                ORBITAL_MODID + ":europa_cryo_warden",
                "Europa Cryo Warden",
                "Thermal cover / cryo vent pulse",
                0xFF61C7FF,
                "Cryo Warden",
                "HARD FREEZE",
                "Use thermal arrays",
                BossHudProfile.BossCategory.ORBITAL,
                0.65F,
                0.35F
        ));
        register(new BossHudProfile(
                ORBITAL_MODID + ":echo_zero",
                "ECHO-0",
                "Quarantine pulse / final network",
                0xFFE0B6FF,
                "ECHO-0",
                "FINAL QUARANTINE",
                "Stabilize suit systems",
                BossHudProfile.BossCategory.ORBITAL,
                0.65F,
                0.35F
        ));
    }

    private static void register(BossHudProfile profile) {
        BY_ID.put(normalize(profile.bossId()), profile);
        BY_TITLE.put(titleKey(profile.title()), profile);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    private static String titleKey(String title) {
        return title == null ? "" : title.toLowerCase(Locale.ROOT).trim();
    }
}
