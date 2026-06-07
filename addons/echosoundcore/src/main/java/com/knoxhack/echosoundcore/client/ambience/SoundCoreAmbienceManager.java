package com.knoxhack.echosoundcore.client.ambience;

import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.api.SoundCoreAmbienceProfile;
import com.knoxhack.echosoundcore.api.context.SoundCoreContext;
import com.knoxhack.echosoundcore.api.context.SoundCoreContextStack;
import com.knoxhack.echosoundcore.client.config.SoundCoreConfig;
import com.knoxhack.echosoundcore.data.SoundCoreDataReloadListener;
import com.knoxhack.echosoundcore.registry.SoundCoreSounds;
import com.knoxhack.echosoundcore.util.SoundCoreAudioIds;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.client.resources.sounds.SoundInstance;

public final class SoundCoreAmbienceManager {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Map<Identifier, SimpleSoundInstance> ACTIVE_LOOPS = new HashMap<>();
    private static String lastFailure = "";

    private SoundCoreAmbienceManager() {}

    public static void tick() {
        if (!SoundCoreConfig.ENABLE_AMBIENCE_LAYERS.get()) {
            stopAll();
            return;
        }
        if (MC.player == null || MC.level == null) {
            return;
        }

        SoundCoreContext ctx = SoundCoreContextStack.current();
        Map<Identifier, Layer> desired = new HashMap<>();

        if (ctx.nexusCorruptionLevel() > 0.0f && SoundCoreConfig.ENABLE_NEXUS_AMBIENCE.get()) {
            desired.put(id("nexus.ambience.low_hum"), new Layer(SoundCoreSounds.NEXUS_AMBIENCE_LOW_HUM.get(), 1.0f, 1.0f));
            if (ctx.nexusCorruptionLevel() > 0.5f) {
                desired.put(id("nexus.ambience.glitch_pulse"), new Layer(SoundCoreSounds.NEXUS_AMBIENCE_GLITCH_PULSE.get(), 1.0f, 1.0f));
            }
        }

        if (ctx.inStationOrbit() && SoundCoreConfig.ENABLE_ORBITAL_AMBIENCE.get()) {
            desired.put(id("nexus.ambience.memory_whisper"), new Layer(SoundCoreSounds.NEXUS_AMBIENCE_MEMORY_WHISPER.get(), 1.0f, 1.0f));
        }

        if (ctx.hazardLevel() > 0) {
            // Toxic
            if (ctx.hazardLevel() >= 2 && SoundCoreConfig.ENABLE_AMBIENCE_LAYERS.get()) {
                desired.put(id("nexus.ambience.reality_tear"), new Layer(SoundCoreSounds.NEXUS_AMBIENCE_REALITY_TEAR.get(), 1.0f, 1.0f));
            }
        }

        if (ctx.structure() != null) {
            String path = ctx.structure().getPath();
            if (path.contains("blackbox") && SoundCoreConfig.ENABLE_BLACKBOX_AMBIENCE.get()) {
                desired.put(id("nexus.ambience.memory_whisper"), new Layer(SoundCoreSounds.NEXUS_AMBIENCE_MEMORY_WHISPER.get(), 1.0f, 1.0f));
            }
            if (path.contains("station") && SoundCoreConfig.ENABLE_STATIONFALL_AMBIENCE.get()) {
                desired.put(id("nexus.core.heartbeat"), new Layer(SoundCoreSounds.NEXUS_CORE_HEARTBEAT.get(), 1.0f, 1.0f));
            }
        }

        // Data-driven ambience profiles
        for (SoundCoreAmbienceProfile profile : SoundCoreDataReloadListener.getAmbienceProfiles()) {
            if (matches(profile, ctx)) {
                if (isMusicEvent(profile.sound())) {
                    lastFailure = "Skipped ambience profile " + profile.id() + " because it references music event " + profile.sound();
                    EchoSoundCore.LOGGER.warn("{}", lastFailure);
                    continue;
                }
                SoundEvent se = findRegisteredSound(profile.sound());
                if (se != null) {
                    desired.put(profile.id(), new Layer(se, profile.volume(), profile.pitch()));
                }
            }
        }

        // Stop layers no longer desired
        ACTIVE_LOOPS.entrySet().removeIf(entry -> {
            if (!desired.containsKey(entry.getKey())) {
                MC.getSoundManager().stop(entry.getValue());
                return true;
            }
            return false;
        });

        // Start new layers
        for (Map.Entry<Identifier, Layer> e : desired.entrySet()) {
            if (e.getValue().sound() == null) continue;
            if (!ACTIVE_LOOPS.containsKey(e.getKey())) {
                SimpleSoundInstance instance = createLoopInstance(e.getValue());
                MC.getSoundManager().play(instance);
                ACTIVE_LOOPS.put(e.getKey(), instance);
            } else {
                // Verify still active; if not, restart
                SimpleSoundInstance inst = ACTIVE_LOOPS.get(e.getKey());
                if (!MC.getSoundManager().isActive(inst)) {
                    SimpleSoundInstance instance = createLoopInstance(e.getValue());
                    MC.getSoundManager().play(instance);
                    ACTIVE_LOOPS.put(e.getKey(), instance);
                }
            }
        }
    }

    public static void stopAll() {
        for (SimpleSoundInstance inst : ACTIVE_LOOPS.values()) {
            MC.getSoundManager().stop(inst);
        }
        ACTIVE_LOOPS.clear();
    }

    public static Map<Identifier, SimpleSoundInstance> activeLoops() {
        return Map.copyOf(ACTIVE_LOOPS);
    }

    public static String lastFailure() {
        return lastFailure;
    }

    public static boolean stopLoop(Identifier id) {
        SimpleSoundInstance instance = ACTIVE_LOOPS.remove(id);
        if (instance == null) {
            return false;
        }
        MC.getSoundManager().stop(instance);
        return true;
    }

    private static boolean matches(SoundCoreAmbienceProfile profile, SoundCoreContext ctx) {
        if (profile.chapter() != com.knoxhack.echosoundcore.SoundCoreChapter.UNKNOWN && profile.chapter() != ctx.chapter()) return false;
        if (profile.biome() != null && !profile.biome().equals(ctx.biome())) return false;
        if (ctx.region() != null && profile.id().getPath().contains("region/") && !profile.id().equals(ctx.region())) return false;
        if (profile.structure() != null && !profile.structure().equals(ctx.structure())) return false;
        if (profile.faction() != null && !profile.faction().equals(ctx.faction())) return false;
        if (profile.hazard() != null && !profile.hazard().isEmpty()) {
            // Simplified hazard check
            if (ctx.hazardLevel() == 0) return false;
        }
        return true;
    }

    private static SimpleSoundInstance createLoopInstance(Layer layer) {
        return new SimpleSoundInstance(
            layer.sound().location(),
            SoundSource.AMBIENT,
            (float) (SoundCoreConfig.AMBIENCE_VOLUME_MULTIPLIER.get() * layer.volume()),
            layer.pitch(),
            RandomSource.create(0L),
            true,
            0,
            SoundInstance.Attenuation.NONE,
            0.0,
            0.0,
            0.0,
            true
        );
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

    public static boolean isMusicEvent(Identifier id) {
        return SoundCoreAudioIds.isSoundCoreMusic(id);
    }

    private record Layer(SoundEvent sound, float volume, float pitch) {}
}
