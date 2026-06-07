package com.knoxhack.echosoundcore.api;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.SoundCoreAudioPriority;
import com.knoxhack.echosoundcore.SoundCoreCombatIntensity;
import com.knoxhack.echosoundcore.api.context.SoundCoreContext;
import com.knoxhack.echosoundcore.api.context.SoundCoreContextStack;
import com.knoxhack.echosoundcore.client.config.SoundCoreConfig;
import com.knoxhack.echosoundcore.registry.SoundCoreSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public final class SoundCoreApi {
    private SoundCoreApi() {}

    // --- UI Sounds ---

    public static void playUiSound(Player player, SoundEvent sound, float volume, float pitch) {
        if (player == null || sound == null) {
            return;
        }
        Identifier id = sound.location();
        if (!EchoCoreServices.soundService().playEvent(player, id, volume, pitch) && player instanceof ServerPlayer sp) {
            EchoSoundCore.LOGGER.debug("SoundCore packet playback for {} was not delivered to {}.", id, sp.getScoreboardName());
        }
    }

    public static void playUiSound(Player player, SoundEvent sound) {
        playUiSound(player, sound, 1.0f, 1.0f);
    }

    // --- Stingers ---

    public static void playStinger(Player player, SoundEvent sound) {
        if (!SoundCoreConfig.ENABLE_MISSION_STINGERS.get()) {
            return;
        }
        playUiSound(player, sound, 1.0f, 1.0f);
    }

    public static void playMissionAccepted(Player player) {
        playUiSound(player, SoundCoreSounds.STINGER_MISSION_ACCEPT.get());
    }

    public static void playMissionUpdated(Player player) {
        playUiSound(player, SoundCoreSounds.STINGER_MISSION_UPDATE.get());
    }

    public static void playObjectiveComplete(Player player) {
        playUiSound(player, SoundCoreSounds.STINGER_OBJECTIVE_COMPLETE.get());
    }

    public static void playMissionComplete(Player player) {
        playUiSound(player, SoundCoreSounds.STINGER_MISSION_COMPLETE.get());
    }

    public static void playSignalDetected(Player player) {
        playUiSound(player, SoundCoreSounds.STINGER_SIGNAL_DETECTED.get());
    }

    public static void playGuardianLocated(Player player) {
        playUiSound(player, SoundCoreSounds.STINGER_GUARDIAN_LOCATED.get());
    }

    public static void playNexusStateChanged(Player player) {
        playUiSound(player, SoundCoreSounds.STINGER_NEXUS_STATE_CHANGED.get());
    }

    // --- Context ---

    public static void pushAudioContext(SoundCoreContext context) {
        SoundCoreContextStack.push(context);
    }

    public static void clearAudioContext() {
        SoundCoreContextStack.clear();
    }

    // --- Combat ---

    public static void setCombatIntensity(SoundCoreCombatIntensity intensity) {
        SoundCoreContext ctx = SoundCoreContextStack.current().copy();
        ctx.combatIntensity(intensity);
        SoundCoreContextStack.setBase(ctx);
    }

    public static void clearCombatIntensity() {
        setCombatIntensity(SoundCoreCombatIntensity.NONE);
    }

    // --- Boss ---

    public static void setBossMusic(Identifier bossId) {
        SoundCoreContext ctx = SoundCoreContextStack.current().copy();
        ctx.bossId(bossId);
        SoundCoreContextStack.setBase(ctx);
    }

    public static void clearBossMusic() {
        SoundCoreContext ctx = SoundCoreContextStack.current().copy();
        ctx.bossId(null);
        SoundCoreContextStack.setBase(ctx);
    }

    // --- Nexus ---

    public static void setNexusCorruptionLevel(float level) {
        SoundCoreContext ctx = SoundCoreContextStack.current().copy();
        ctx.nexusCorruptionLevel(Math.max(0.0f, Math.min(1.0f, level)));
        SoundCoreContextStack.setBase(ctx);
    }

    // --- Terminal ---

    public static void setTerminalOpen(boolean open) {
        SoundCoreContext ctx = SoundCoreContextStack.current().copy();
        ctx.terminalOpen(open);
        SoundCoreContextStack.setBase(ctx);
    }

    // --- Profile Registration ---
    // Data-driven profiles are loaded via JSON; this hook is for programmatic registration if needed.

    public static void registerMusicProfile(SoundCoreMusicProfile profile) {
        // Profiles are primarily data-driven; programmatic registration can be added here.
        EchoSoundCore.LOGGER.debug("Programmatic music profile registration requested for {}", profile.id());
    }

    public static void registerAmbienceProfile(SoundCoreAmbienceProfile profile) {
        EchoSoundCore.LOGGER.debug("Programmatic ambience profile registration requested for {}", profile.id());
    }

}
