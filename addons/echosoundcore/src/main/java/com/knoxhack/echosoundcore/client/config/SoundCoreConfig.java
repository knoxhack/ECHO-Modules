package com.knoxhack.echosoundcore.client.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec;

public final class SoundCoreConfig {
    public static final EchoNativeConfigSpec CLIENT_SPEC;

    public static final EchoNativeConfigSpec.BooleanValue ENABLE_ADAPTIVE_MUSIC;
    public static final EchoNativeConfigSpec.BooleanValue REPLACE_VANILLA_MUSIC_WITH_SOUNDCORE;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_MENU_MUSIC;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_BIOME_MUSIC;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_COMBAT_MUSIC;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_BOSS_MUSIC;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_MISSION_STINGERS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_TERMINAL_SOUNDS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_LENS_SOUNDS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_HOLOMAP_SOUNDS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_SIGNALOS_SOUNDS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_POWERGRID_SOUNDS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_DRONE_SOUNDS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_NEXUS_AMBIENCE;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_BLACKBOX_AMBIENCE;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_STATIONFALL_AMBIENCE;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_ORBITAL_AMBIENCE;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_AMBIENCE_LAYERS;
    public static final EchoNativeConfigSpec.BooleanValue ENABLE_DEBUG_OVERLAY;

    public static final EchoNativeConfigSpec.DoubleValue MUSIC_VOLUME_MULTIPLIER;
    public static final EchoNativeConfigSpec.DoubleValue UI_SOUND_VOLUME_MULTIPLIER;
    public static final EchoNativeConfigSpec.DoubleValue AMBIENCE_VOLUME_MULTIPLIER;
    public static final EchoNativeConfigSpec.DoubleValue STINGER_VOLUME_MULTIPLIER;
    public static final EchoNativeConfigSpec.DoubleValue COMBAT_MUSIC_VOLUME_MULTIPLIER;
    public static final EchoNativeConfigSpec.DoubleValue NEXUS_DISTORTION_INTENSITY;

    public static final EchoNativeConfigSpec.IntValue MUSIC_CHANGE_COOLDOWN_TICKS;
    public static final EchoNativeConfigSpec.IntValue MINIMUM_TRACK_PLAY_TICKS;
    public static final EchoNativeConfigSpec.BooleanValue TERMINAL_DUCKS_WORLD_MUSIC;
    public static final EchoNativeConfigSpec.BooleanValue TERMINAL_MUSIC_BED_ENABLED;

    static {
        EchoNativeConfigSpec.Builder builder = new EchoNativeConfigSpec.Builder();

        builder.push("toggles");
        ENABLE_ADAPTIVE_MUSIC = builder.comment("Enable adaptive music system.").define("enableAdaptiveMusic", true);
        REPLACE_VANILLA_MUSIC_WITH_SOUNDCORE = builder.comment("Cancel vanilla Minecraft music while SoundCore adaptive music can provide a track.").define("replaceVanillaMusicWithSoundCore", true);
        ENABLE_MENU_MUSIC = builder.comment("Enable custom menu music.").define("enableMenuMusic", true);
        ENABLE_BIOME_MUSIC = builder.comment("Enable biome music.").define("enableBiomeMusic", true);
        ENABLE_COMBAT_MUSIC = builder.comment("Enable combat music.").define("enableCombatMusic", true);
        ENABLE_BOSS_MUSIC = builder.comment("Enable boss music.").define("enableBossMusic", true);
        ENABLE_MISSION_STINGERS = builder.comment("Enable mission stingers.").define("enableMissionStingers", true);
        ENABLE_TERMINAL_SOUNDS = builder.comment("Enable Terminal UI sounds.").define("enableTerminalSounds", true);
        ENABLE_LENS_SOUNDS = builder.comment("Enable Lens UI sounds.").define("enableLensSounds", true);
        ENABLE_HOLOMAP_SOUNDS = builder.comment("Enable HoloMap UI sounds.").define("enableHoloMapSounds", true);
        ENABLE_SIGNALOS_SOUNDS = builder.comment("Enable SignalOS UI sounds.").define("enableSignalOSSounds", true);
        ENABLE_POWERGRID_SOUNDS = builder.comment("Enable PowerGrid sounds.").define("enablePowerGridSounds", true);
        ENABLE_DRONE_SOUNDS = builder.comment("Enable drone sounds.").define("enableDroneSounds", true);
        ENABLE_NEXUS_AMBIENCE = builder.comment("Enable Nexus ambience.").define("enableNexusAmbience", true);
        ENABLE_BLACKBOX_AMBIENCE = builder.comment("Enable Blackbox ambience.").define("enableBlackboxAmbience", true);
        ENABLE_STATIONFALL_AMBIENCE = builder.comment("Enable Stationfall ambience.").define("enableStationfallAmbience", true);
        ENABLE_ORBITAL_AMBIENCE = builder.comment("Enable Orbital ambience.").define("enableOrbitalAmbience", true);
        ENABLE_AMBIENCE_LAYERS = builder.comment("Enable ambience layers.").define("enableAmbienceLayers", true);
        ENABLE_DEBUG_OVERLAY = builder.comment("Enable debug audio overlay.").define("enableDebugOverlay", false);
        builder.pop();

        builder.push("volume");
        MUSIC_VOLUME_MULTIPLIER = builder.comment("Global music volume multiplier.").defineInRange("musicVolumeMultiplier", 1.0, 0.0, 2.0);
        UI_SOUND_VOLUME_MULTIPLIER = builder.comment("UI sound volume multiplier.").defineInRange("uiSoundVolumeMultiplier", 1.0, 0.0, 2.0);
        AMBIENCE_VOLUME_MULTIPLIER = builder.comment("Ambience volume multiplier.").defineInRange("ambienceVolumeMultiplier", 1.0, 0.0, 2.0);
        STINGER_VOLUME_MULTIPLIER = builder.comment("Stinger volume multiplier.").defineInRange("stingerVolumeMultiplier", 1.0, 0.0, 2.0);
        COMBAT_MUSIC_VOLUME_MULTIPLIER = builder.comment("Combat music volume multiplier.").defineInRange("combatMusicVolumeMultiplier", 1.0, 0.0, 2.0);
        NEXUS_DISTORTION_INTENSITY = builder.comment("Nexus audio distortion intensity.").defineInRange("nexusDistortionIntensity", 1.0, 0.0, 2.0);
        builder.pop();

        builder.push("timing");
        MUSIC_CHANGE_COOLDOWN_TICKS = builder.comment("Cooldown between music changes in ticks.").defineInRange("musicChangeCooldownTicks", 200, 20, 1200);
        MINIMUM_TRACK_PLAY_TICKS = builder.comment("Minimum track play time before switching.").defineInRange("minimumTrackPlayTicks", 1200, 200, 6000);
        TERMINAL_DUCKS_WORLD_MUSIC = builder.comment("Terminal UI ducks world music.").define("terminalDucksWorldMusic", true);
        TERMINAL_MUSIC_BED_ENABLED = builder.comment("Enable terminal command bed music.").define("terminalMusicBedEnabled", true);
        builder.pop();

        CLIENT_SPEC = builder.build();
    }

    private SoundCoreConfig() {}
}
