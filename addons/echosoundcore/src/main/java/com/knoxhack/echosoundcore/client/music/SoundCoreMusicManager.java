package com.knoxhack.echosoundcore.client.music;

import com.mojang.blaze3d.audio.Channel;
import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.SoundCoreAudioPriority;
import com.knoxhack.echosoundcore.SoundCoreChapter;
import com.knoxhack.echosoundcore.SoundCoreCombatIntensity;
import com.knoxhack.echosoundcore.api.SoundCoreMusicProfile;
import com.knoxhack.echosoundcore.api.context.SoundCoreContext;
import com.knoxhack.echosoundcore.api.context.SoundCoreContextStack;
import com.knoxhack.echosoundcore.client.config.SoundCoreConfig;
import com.knoxhack.echosoundcore.data.SoundCoreDataReloadListener;
import com.knoxhack.echosoundcore.registry.NativeRegistryHolder;
import com.knoxhack.echosoundcore.registry.SoundCoreSounds;
import com.knoxhack.echosoundcore.util.SoundCoreAudioIds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.client.resources.sounds.SoundInstance;

public final class SoundCoreMusicManager {
    private static final Minecraft MC = Minecraft.getInstance();
    private static Identifier currentTrackId = null;
    private static SoundCoreAudioPriority currentPriority = SoundCoreAudioPriority.IDLE;
    private static long lastChangeTick = -9999;
    private static long trackStartTick = -9999;
    private static SoundInstance currentInstance = null;
    private static Identifier currentSoundId = null;
    private static long musicGeneration = 0L;
    private static final List<SoundInstance> OWNED_MUSIC = new ArrayList<>();
    private static final CopyOnWriteArrayList<Channel> OWNED_CHANNELS = new CopyOnWriteArrayList<>();
    private static String currentSelectionReason = "";
    private static String lastFailure = "";

    private SoundCoreMusicManager() {}

    public static void tick() {
        pruneOwnedMusic();
        pruneOwnedChannels();
        enforceSingleMusicInstance();
        if (!SoundCoreConfig.ENABLE_ADAPTIVE_MUSIC.get()) {
            stopControlled();
            return;
        }
        if (MC.player == null || MC.level == null) {
            return;
        }

        long now = MC.level.getGameTime();
        SoundCoreContext ctx = SoundCoreContextStack.current();
        DesiredTrack desired = selectDesiredTrack(ctx);

        if (desired == null || desired.sound() == null) {
            if (currentTrackId != null) {
                stopControlled();
            }
            return;
        }

        if (currentTrackId != null && currentTrackId.equals(desired.id())) {
            if (currentInstance != null && !MC.getSoundManager().isActive(currentInstance)) {
                currentTrackId = null;
                currentSoundId = null;
                currentInstance = null;
            }
            enforceSingleMusicInstance();
            return;
        }

        long cooldown = SoundCoreConfig.MUSIC_CHANGE_COOLDOWN_TICKS.get();
        long minPlay = SoundCoreConfig.MINIMUM_TRACK_PLAY_TICKS.get();
        if (now - lastChangeTick < cooldown) {
            return;
        }
        if (currentTrackId != null && now - trackStartTick < minPlay) {
            return;
        }

        playTrack(desired, now);
    }

    public static void stopControlled() {
        hardStopAllMusic(true);
    }

    public static Identifier currentTrackId() {
        return currentTrackId;
    }

    public static Identifier currentSoundId() {
        return currentSoundId;
    }

    public static SoundCoreAudioPriority currentPriority() {
        return currentPriority;
    }

    public static long currentTrackElapsedTicks() {
        if (currentTrackId == null || MC.level == null) {
            return 0L;
        }
        return Math.max(0L, MC.level.getGameTime() - trackStartTick);
    }

    public static String currentSelectionReason() {
        return currentSelectionReason;
    }

    public static String lastFailure() {
        return lastFailure;
    }

    public static boolean playProfile(Identifier profileId) {
        SoundCoreMusicProfile profile = SoundCoreDataReloadListener.getMusicProfile(profileId);
        if (profile == null) {
            lastFailure = "Unknown music profile " + profileId;
            return false;
        }
        return playSoundEvent(profile.sound(), profile.priority(), "profile:" + profile.id(), profile.id());
    }

    public static boolean playSoundEvent(Identifier eventId, String reason) {
        return playSoundEvent(eventId, SoundCoreAudioPriority.SCRIPTED, reason, eventId);
    }

    public static boolean isMusicEvent(Identifier eventId) {
        return SoundCoreAudioIds.isSoundCoreMusic(eventId);
    }

    public static boolean shouldStopForEvent(Identifier eventId) {
        return SoundCoreAudioIds.matchesControlledMusicStop(eventId, currentTrackId, currentSoundId);
    }

    public static int ownedMusicInstanceCount() {
        pruneOwnedMusic();
        pruneOwnedChannels();
        return Math.max(OWNED_MUSIC.size(), OWNED_CHANNELS.size());
    }

    public static boolean shouldSuppressVanillaMusic() {
        if (!SoundCoreConfig.ENABLE_ADAPTIVE_MUSIC.get() || !SoundCoreConfig.REPLACE_VANILLA_MUSIC_WITH_SOUNDCORE.get()) {
            return false;
        }
        if (MC.player == null || MC.level == null) {
            return false;
        }
        if (hasActiveOwnedMusic()) {
            return true;
        }

        DesiredTrack desired = selectDesiredTrack(SoundCoreContextStack.current());
        return desired != null && desired.sound() != null;
    }

    private static void playTrack(DesiredTrack desired, long now) {
        SoundEvent sound = desired.sound();
        if (sound == null) return;

        hardStopAllMusic(false);
        long generation = ++musicGeneration;
        float volume = getVolumeMultiplier(desired.priority());
        currentInstance = createMusicInstance(sound, volume, desired.soundId(), generation);
        OWNED_MUSIC.add(currentInstance);
        currentTrackId = desired.id();
        currentSoundId = desired.soundId();
        currentPriority = desired.priority();
        currentSelectionReason = desired.reason();
        lastChangeTick = now;
        trackStartTick = now;
        MC.getSoundManager().play(currentInstance);
    }

    public static void onSoundChannelStarted(SoundInstance sound, Channel channel) {
        if (sound == null || channel == null) {
            return;
        }
        Identifier soundId = sound.getIdentifier();
        boolean soundCoreMusic = isMusicEvent(soundId);
        boolean minecraftMusic = sound.getSource() == SoundSource.MUSIC;
        if (!soundCoreMusic && (!minecraftMusic || currentSoundId == null)) {
            return;
        }

        if (soundCoreMusic) {
            claimStartedMusic(soundId, sound, channel);
            return;
        }

        // SoundCore owns music; do not let vanilla or another MUSIC-category source layer underneath it.
        channel.stop();
    }

    private static boolean playSoundEvent(Identifier eventId, SoundCoreAudioPriority priority, String reason, Identifier trackId) {
        SoundEvent sound = findRegisteredSound(eventId);
        if (sound == null) {
            lastFailure = "Unknown sound event " + eventId;
            return false;
        }
        long now = MC.level == null ? 0L : MC.level.getGameTime();
        playTrack(new DesiredTrack(trackId, eventId, sound, priority, reason), now);
        return true;
    }

    private static void pruneOwnedMusic() {
        OWNED_MUSIC.removeIf(instance -> instance == null || !MC.getSoundManager().isActive(instance));
        if (currentInstance != null && !MC.getSoundManager().isActive(currentInstance) && !hasActiveOwnedChannel()) {
            currentInstance = null;
            currentTrackId = null;
            currentSoundId = null;
            currentPriority = SoundCoreAudioPriority.IDLE;
            currentSelectionReason = "";
        }
    }

    private static boolean hasActiveOwnedMusic() {
        pruneOwnedMusic();
        pruneOwnedChannels();
        return !OWNED_MUSIC.isEmpty() || !OWNED_CHANNELS.isEmpty();
    }

    private static void hardStopAllMusic(boolean clearState) {
        stopTrackedMusicChannelsExcept(null);
        for (SoundInstance instance : List.copyOf(OWNED_MUSIC)) {
            MC.getSoundManager().stop(instance);
        }
        OWNED_MUSIC.clear();
        musicGeneration++;
        MC.getMusicManager().stopPlaying();
        MC.getSoundManager().stop((Identifier) null, SoundSource.MUSIC);
        for (var entry : SoundCoreSounds.getEntries()) {
            Identifier id = entry.getId();
            if (isMusicEvent(id)) {
                MC.getSoundManager().stop(id, SoundSource.MUSIC);
            }
        }
        if (clearState) {
            currentInstance = null;
            currentTrackId = null;
            currentSoundId = null;
            currentPriority = SoundCoreAudioPriority.IDLE;
            currentSelectionReason = "";
        }
    }

    private static void enforceSingleMusicInstance() {
        pruneOwnedMusic();
        pruneOwnedChannels();
        if (OWNED_MUSIC.size() <= 1 && OWNED_CHANNELS.size() <= 1) {
            return;
        }
        if (OWNED_CHANNELS.size() > 1) {
            Channel keepChannel = OWNED_CHANNELS.get(OWNED_CHANNELS.size() - 1);
            stopTrackedMusicChannelsExcept(keepChannel);
            if (!OWNED_CHANNELS.contains(keepChannel) && !keepChannel.stopped()) {
                OWNED_CHANNELS.add(keepChannel);
            }
        }
        if (OWNED_MUSIC.isEmpty()) {
            return;
        }
        SoundInstance keep = currentInstance;
        if (keep == null || !OWNED_MUSIC.contains(keep)) {
            keep = OWNED_MUSIC.get(OWNED_MUSIC.size() - 1);
        }
        for (SoundInstance instance : List.copyOf(OWNED_MUSIC)) {
            if (instance != keep) {
                MC.getSoundManager().stop(instance);
            }
        }
        OWNED_MUSIC.clear();
        if (keep != null && MC.getSoundManager().isActive(keep)) {
            OWNED_MUSIC.add(keep);
            currentInstance = keep;
        } else if (currentSoundId != null) {
            Identifier trackId = currentTrackId == null ? currentSoundId : currentTrackId;
            SoundCoreAudioPriority priority = currentPriority;
            String reason = currentSelectionReason;
            SoundEvent sound = findRegisteredSound(currentSoundId);
            if (sound != null) {
                playTrack(new DesiredTrack(trackId, currentSoundId, sound, priority, reason),
                        MC.level == null ? 0L : MC.level.getGameTime());
            }
        }
    }

    private static void claimStartedMusic(Identifier soundId, SoundInstance sound, Channel channel) {
        stopTrackedMusicChannelsExcept(channel);
        if (!channel.stopped() && !OWNED_CHANNELS.contains(channel)) {
            OWNED_CHANNELS.add(channel);
        }
        MC.getMusicManager().stopPlaying();
        if (currentSoundId == null || !soundId.equals(currentSoundId)) {
            currentTrackId = soundId;
            currentSoundId = soundId;
            currentPriority = SoundCoreAudioPriority.SCRIPTED;
            currentSelectionReason = "external:" + soundId;
            lastChangeTick = MC.level == null ? 0L : MC.level.getGameTime();
            trackStartTick = lastChangeTick;
            currentInstance = sound;
            OWNED_MUSIC.add(sound);
        }
    }

    private static void stopTrackedMusicChannelsExcept(Channel keep) {
        for (Channel channel : OWNED_CHANNELS) {
            if (channel != keep) {
                channel.stop();
                OWNED_CHANNELS.remove(channel);
            }
        }
    }

    private static void pruneOwnedChannels() {
        OWNED_CHANNELS.removeIf(channel -> channel == null || channel.stopped());
    }

    private static boolean hasActiveOwnedChannel() {
        pruneOwnedChannels();
        return !OWNED_CHANNELS.isEmpty();
    }

    private static SoundInstance createMusicInstance(SoundEvent sound, float volume, Identifier soundId, long generation) {
        return new ControlledMusicInstance(sound, volume, soundId, generation);
    }

    private static float getVolumeMultiplier(SoundCoreAudioPriority priority) {
        double base = SoundCoreConfig.MUSIC_VOLUME_MULTIPLIER.get();
        if (priority == SoundCoreAudioPriority.COMBAT || priority == SoundCoreAudioPriority.BOSS || priority == SoundCoreAudioPriority.SIEGE) {
            base *= SoundCoreConfig.COMBAT_MUSIC_VOLUME_MULTIPLIER.get();
        }
        return (float) base;
    }

    private static DesiredTrack selectDesiredTrack(SoundCoreContext ctx) {
        // Scripted / boss / combat overrides
        if (ctx.bossId() != null) {
            Identifier bossTrack = resolveBossTrack(ctx.bossId());
            if (bossTrack != null) {
                SoundEvent se = findRegisteredSound(bossTrack);
                if (se != null && SoundCoreConfig.ENABLE_BOSS_MUSIC.get()) return new DesiredTrack(bossTrack, bossTrack, se, SoundCoreAudioPriority.BOSS, "boss:" + ctx.bossId());
            }
        }

        if (ctx.combatIntensity() == SoundCoreCombatIntensity.SIEGE) {
            SoundEvent se = SoundCoreSounds.MUSIC_COMBAT_SIEGE.get();
            if (se != null && SoundCoreConfig.ENABLE_COMBAT_MUSIC.get()) return new DesiredTrack(id("music.combat.siege"), id("music.combat.siege"), se, SoundCoreAudioPriority.SIEGE, "combat:siege");
        } else if (ctx.combatIntensity() == SoundCoreCombatIntensity.BOSS) {
            SoundEvent se = SoundCoreSounds.MUSIC_COMBAT_HEAVY.get();
            if (se != null && SoundCoreConfig.ENABLE_COMBAT_MUSIC.get()) return new DesiredTrack(id("music.combat.heavy"), id("music.combat.heavy"), se, SoundCoreAudioPriority.BOSS, "combat:boss");
        } else if (ctx.combatIntensity() == SoundCoreCombatIntensity.HEAVY || ctx.combatIntensity() == SoundCoreCombatIntensity.ELITE) {
            SoundEvent se = SoundCoreSounds.MUSIC_COMBAT_HEAVY.get();
            if (se != null && SoundCoreConfig.ENABLE_COMBAT_MUSIC.get()) return new DesiredTrack(id("music.combat.heavy"), id("music.combat.heavy"), se, SoundCoreAudioPriority.COMBAT, "combat:" + ctx.combatIntensity());
        } else if (ctx.combatIntensity() == SoundCoreCombatIntensity.LIGHT) {
            SoundEvent se = SoundCoreSounds.MUSIC_COMBAT_LIGHT.get();
            if (se != null && SoundCoreConfig.ENABLE_COMBAT_MUSIC.get()) return new DesiredTrack(id("music.combat.light"), id("music.combat.light"), se, SoundCoreAudioPriority.COMBAT, "combat:light");
        }

        DesiredTrack dataProfile = tryDataProfile(ctx);
        if (dataProfile != null) {
            return dataProfile;
        }

        if (ctx.terminalOpen() && SoundCoreConfig.TERMINAL_MUSIC_BED_ENABLED.get()) {
            if (ctx.nexusCorruptionLevel() > 0.5f) {
                SoundEvent se = SoundCoreSounds.MUSIC_TERMINAL_NEXUS_CORRUPTED.get();
                if (se != null) return new DesiredTrack(id("music.terminal.nexus_corrupted"), id("music.terminal.nexus_corrupted"), se, SoundCoreAudioPriority.STRUCTURE, "terminal:nexus_corrupted");
            }
            SoundEvent se = SoundCoreSounds.MUSIC_TERMINAL_COMMAND_BED.get();
            if (se != null) return new DesiredTrack(id("music.terminal.command_bed"), id("music.terminal.command_bed"), se, SoundCoreAudioPriority.STRUCTURE, "terminal:command_bed");
        }

        if (ctx.missionId() != null) {
            SoundEvent se = tryFindChapterTrack(ctx.chapter());
            Identifier soundId = id("music.chapter." + ctx.chapter().name().toLowerCase());
            if (se != null) return new DesiredTrack(soundId, soundId, se, SoundCoreAudioPriority.MISSION, "mission:" + ctx.missionId());
        }

        if (ctx.structure() != null) {
            SoundEvent se = tryFindStructureTrack(ctx.structure());
            if (se != null) return new DesiredTrack(ctx.structure(), se.location(), se, SoundCoreAudioPriority.STRUCTURE, "structure:" + ctx.structure());
        }

        if (ctx.biome() != null) {
            SoundEvent se = tryFindBiomeTrack(ctx.biome());
            if (se != null && SoundCoreConfig.ENABLE_BIOME_MUSIC.get()) return new DesiredTrack(ctx.biome(), se.location(), se, SoundCoreAudioPriority.BIOME, "biome:" + ctx.biome());
        }

        if (ctx.underground()) {
            SoundEvent se = SoundCoreSounds.MUSIC_GAMEPLAY_UNDERGROUND.get();
            if (se != null) return new DesiredTrack(id("music.gameplay.underground"), id("music.gameplay.underground"), se, SoundCoreAudioPriority.BASE, "fallback:underground");
        }

        // Safe base / exploration fallback
        SoundEvent se = SoundCoreSounds.MUSIC_GAMEPLAY_EXPLORATION.get();
        if (se != null) return new DesiredTrack(id("music.gameplay.exploration"), id("music.gameplay.exploration"), se, SoundCoreAudioPriority.IDLE, "fallback:exploration");
        return null;
    }

    private static DesiredTrack tryDataProfile(SoundCoreContext ctx) {
        List<SoundCoreMusicProfile> profiles = SoundCoreDataReloadListener.getMusicProfiles();
        SoundCoreMusicProfile best = null;
        int bestScore = -1;
        for (SoundCoreMusicProfile p : profiles) {
            int score = scoreProfile(p, ctx);
            if (score > bestScore) {
                bestScore = score;
                best = p;
            } else if (score == bestScore && best != null && p.id().toString().compareTo(best.id().toString()) < 0) {
                best = p;
            }
        }
        if (best != null && bestScore > 0) {
            SoundEvent se = findRegisteredSound(best.sound());
            if (se != null) return new DesiredTrack(best.id(), best.sound(), se, best.priority(), "profile:" + best.id());
            lastFailure = "Unknown profile sound " + best.sound();
        }
        return null;
    }

    private static int scoreProfile(SoundCoreMusicProfile p, SoundCoreContext ctx) {
        if (!p.conditions()) {
            return -1;
        }
        int score = p.priority().weight() * 10;
        boolean matchedFilter = false;
        if (p.chapter() != SoundCoreChapter.UNKNOWN && p.chapter() != ctx.chapter()) return -1;
        if (p.chapter() != SoundCoreChapter.UNKNOWN) { score += 100; matchedFilter = true; }
        if (p.biome() != null && !p.biome().equals(ctx.biome())) return -1;
        if (p.biome() != null) { score += 80; matchedFilter = true; }
        if (p.region() != null && !p.region().equals(ctx.region())) return -1;
        if (p.region() != null) { score += 85; matchedFilter = true; }
        if (p.structure() != null && !p.structure().equals(ctx.structure())) return -1;
        if (p.structure() != null) { score += 90; matchedFilter = true; }
        if (p.faction() != null && !p.faction().equals(ctx.faction())) return -1;
        if (p.faction() != null) { score += 70; matchedFilter = true; }
        if (p.combatIntensity() != SoundCoreCombatIntensity.NONE && p.combatIntensity() != ctx.combatIntensity()) return -1;
        if (p.combatIntensity() != SoundCoreCombatIntensity.NONE) { score += 85; matchedFilter = true; }
        if (p.boss() != null && !p.boss().equals(ctx.bossId())) return -1;
        if (p.boss() != null) { score += 120; matchedFilter = true; }
        if (!matchedFilter) return 0;
        score += Math.round(p.weight() * 10.0f);
        return score;
    }

    private static Identifier resolveBossTrack(Identifier bossId) {
        String path = bossId.getPath();
        Map<String, NativeRegistryHolder<SoundEvent>> map = java.util.Map.ofEntries(
            Map.entry("warden", SoundCoreSounds.MUSIC_BOSS_WARDEN),
            Map.entry("guardian.wasteland", SoundCoreSounds.MUSIC_BOSS_GUARDIAN_WASTELAND),
            Map.entry("guardian.toxic", SoundCoreSounds.MUSIC_BOSS_GUARDIAN_TOXIC),
            Map.entry("guardian.radiation", SoundCoreSounds.MUSIC_BOSS_GUARDIAN_RADIATION),
            Map.entry("guardian.cryo", SoundCoreSounds.MUSIC_BOSS_GUARDIAN_CRYO),
            Map.entry("guardian.industrial", SoundCoreSounds.MUSIC_BOSS_GUARDIAN_INDUSTRIAL),
            Map.entry("guardian.city", SoundCoreSounds.MUSIC_BOSS_GUARDIAN_CITY),
            Map.entry("guardian.nexus", SoundCoreSounds.MUSIC_BOSS_GUARDIAN_NEXUS),
            Map.entry("corruption_bloom", SoundCoreSounds.MUSIC_BOSS_CORRUPTION_BLOOM),
            Map.entry("severance_engine", SoundCoreSounds.MUSIC_BOSS_SEVERANCE_ENGINE),
            Map.entry("mirror_command", SoundCoreSounds.MUSIC_BOSS_MIRROR_COMMAND),
            Map.entry("station_mother", SoundCoreSounds.MUSIC_BOSS_STATION_MOTHER)
        );
        var holder = map.get(path);
        return holder == null ? null : holder.getId();
    }

    private static SoundEvent tryFindChapterTrack(SoundCoreChapter chapter) {
        return switch (chapter) {
            case ASHFALL -> SoundCoreSounds.MUSIC_CHAPTER_ASHFALL_PROTOCOL.get();
            case ORBITAL -> SoundCoreSounds.MUSIC_CHAPTER_ORBITAL_REMNANTS.get();
            case AGRICULTURE -> SoundCoreSounds.MUSIC_CHAPTER_AGRICULTURE_RECLAMATION.get();
            case STATIONFALL -> SoundCoreSounds.MUSIC_CHAPTER_STATIONFALL.get();
            case NEXUS -> SoundCoreSounds.MUSIC_CHAPTER_NEXUS_PROTOCOL.get();
            case INDUSTRIAL -> SoundCoreSounds.MUSIC_CHAPTER_INDUSTRIAL_NEXUS.get();
            case LOGISTICS -> SoundCoreSounds.MUSIC_CHAPTER_LOGISTICS_NETWORK.get();
            case CONVOY -> SoundCoreSounds.MUSIC_CHAPTER_CONVOY_PROTOCOL.get();
            case ARMORY -> SoundCoreSounds.MUSIC_CHAPTER_ARMORY.get();
            case BLACKBOX -> SoundCoreSounds.MUSIC_CHAPTER_BLACKBOX_PROTOCOL.get();
            default -> null;
        };
    }

    private static SoundEvent tryFindBiomeTrack(Identifier biome) {
        String path = biome.getPath();
        if (path.contains("wasteland")) return SoundCoreSounds.MUSIC_BIOME_WASTELAND.get();
        if (path.contains("crash")) return SoundCoreSounds.MUSIC_BIOME_CRASH_ZONE.get();
        if (path.contains("toxic") || path.contains("swamp")) return SoundCoreSounds.MUSIC_BIOME_TOXIC_SWAMP.get();
        if (path.contains("radiation")) return SoundCoreSounds.MUSIC_BIOME_RADIATION_ZONE.get();
        if (path.contains("cryo")) return SoundCoreSounds.MUSIC_BIOME_CRYOGENIC_RUINS.get();
        if (path.contains("city")) return SoundCoreSounds.MUSIC_BIOME_RUINED_CITY.get();
        if (path.contains("industrial")) return SoundCoreSounds.MUSIC_BIOME_INDUSTRIAL_RUINS.get();
        if (path.contains("nexus")) return SoundCoreSounds.MUSIC_BIOME_NEXUS_SCAR.get();
        return null;
    }

    private static SoundEvent tryFindStructureTrack(Identifier structure) {
        String path = structure.getPath();
        if (path.contains("nexus")) return SoundCoreSounds.MUSIC_NEXUS_CORE_AMBIENCE.get();
        if (path.contains("blackbox")) return SoundCoreSounds.MUSIC_BLACKBOX_MEMORY_FRAGMENT.get();
        if (path.contains("station")) return SoundCoreSounds.MUSIC_CHAPTER_STATIONFALL.get();
        return null;
    }

    private static SoundEvent findRegisteredSound(Identifier id) {
        for (var entry : SoundCoreSounds.getEntries()) {
            if (entry.getId().equals(id)) {
                return entry.get();
            }
        }
        return null;
    }

    private static Identifier id(String path) {
        return EchoSoundCore.id(path);
    }

    private record DesiredTrack(Identifier id, Identifier soundId, SoundEvent sound, SoundCoreAudioPriority priority, String reason) {
        private DesiredTrack(Identifier id, SoundEvent sound, SoundCoreAudioPriority priority) {
            this(id, sound == null ? null : sound.location(), sound, priority, "");
        }
    }

    private static final class ControlledMusicInstance extends AbstractTickableSoundInstance {
        private final Identifier soundId;
        private final long generation;

        private ControlledMusicInstance(SoundEvent sound, float volume, Identifier soundId, long generation) {
            super(sound, SoundSource.MUSIC, RandomSource.create(0L));
            this.soundId = soundId;
            this.generation = generation;
            this.volume = volume;
            this.pitch = 1.0F;
            this.looping = true;
            this.delay = 0;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = true;
        }

        @Override
        public void tick() {
            if (this.generation != musicGeneration || currentSoundId == null || !currentSoundId.equals(this.soundId)) {
                this.stop();
            }
        }

        @Override
        public boolean canPlaySound() {
            return this.generation == musicGeneration && currentSoundId != null && currentSoundId.equals(this.soundId);
        }
    }
}
