package com.knoxhack.echosoundcore.service;

import com.echoplatform.echocore.api.ISoundService;
import com.echoplatform.echocore.api.SoundServiceDiagnostics;
import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echosoundcore.EchoSoundCore;
import com.knoxhack.echosoundcore.data.SoundCoreDataReloadListener;
import com.knoxhack.echosoundcore.network.SoundCoreAudioPacket;
import com.knoxhack.echosoundcore.util.SoundCoreCatalogValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public enum SoundCoreService implements ISoundService {
    INSTANCE;

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean playEvent(Identifier eventId) {
        return invokeClient("playOneShot", new Class<?>[] { Identifier.class, float.class, float.class },
                eventId, 1.0f, 1.0f);
    }

    @Override
    public boolean playEvent(Player player, Identifier eventId) {
        return playEvent(player, eventId, 1.0f, 1.0f);
    }

    @Override
    public boolean playEvent(Player player, Identifier eventId, float volume, float pitch) {
        if (eventId == null) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return EchoNetSend.toPlayer(serverPlayer, SoundCoreAudioPacket.playOneShot(eventId, volume, pitch),
                    EchoPacketKind.OPTIONAL_ADDON);
        }
        return invokeClient("playOneShot", new Class<?>[] { Identifier.class, float.class, float.class },
                eventId, volume, pitch);
    }

    @Override
    public boolean stopEvent(Identifier eventId) {
        return invokeClient("stopEvent", new Class<?>[] { Identifier.class }, eventId);
    }

    @Override
    public boolean stopEvent(Player player, Identifier eventId) {
        if (eventId == null) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return EchoNetSend.toPlayer(serverPlayer, SoundCoreAudioPacket.stopEvent(eventId), EchoPacketKind.OPTIONAL_ADDON);
        }
        return stopEvent(eventId);
    }

    @Override
    public boolean playProfile(Player player, Identifier profileId) {
        if (profileId == null) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return EchoNetSend.toPlayer(serverPlayer, SoundCoreAudioPacket.playProfile(profileId), EchoPacketKind.OPTIONAL_ADDON);
        }
        return invokeClient("handle", new Class<?>[] { SoundCoreAudioPacket.class }, SoundCoreAudioPacket.playProfile(profileId));
    }

    @Override
    public boolean setContext(Player player, Map<String, String> context) {
        return sendContext(player, SoundCoreAudioPacket.setContext(context));
    }

    @Override
    public boolean patchContext(Player player, Map<String, String> patch) {
        return sendContext(player, SoundCoreAudioPacket.patchContext(patch));
    }

    @Override
    public boolean clearContext(Player player) {
        return sendContext(player, SoundCoreAudioPacket.clearContext());
    }

    @Override
    public boolean stopControlledAudio(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return EchoNetSend.toPlayer(serverPlayer, SoundCoreAudioPacket.stopControlledAudio(), EchoPacketKind.OPTIONAL_ADDON);
        }
        return invokeClient("handle", new Class<?>[] { SoundCoreAudioPacket.class }, SoundCoreAudioPacket.stopControlledAudio());
    }

    @Override
    public List<Identifier> activeEvents() {
        if (!isClientRuntime()) {
            return List.of();
        }
        List<Identifier> active = new ArrayList<>();
        Object currentTrack = invokeClientResult("com.knoxhack.echosoundcore.client.music.SoundCoreMusicManager", "currentTrackId");
        if (currentTrack instanceof Identifier id) {
            active.add(id);
        }
        Object currentSound = invokeClientResult("com.knoxhack.echosoundcore.client.music.SoundCoreMusicManager", "currentSoundId");
        if (currentSound instanceof Identifier id && !active.contains(id)) {
            active.add(id);
        }
        Object loops = invokeClientResult("com.knoxhack.echosoundcore.client.ambience.SoundCoreAmbienceManager", "activeLoops");
        if (loops instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                if (key instanceof Identifier id) {
                    active.add(id);
                }
            }
        }
        return List.copyOf(active);
    }

    @Override
    public SoundServiceDiagnostics diagnostics() {
        Identifier currentTrack = null;
        String priority = "";
        String reason = "";
        String lastFailure = "";
        if (isClientRuntime()) {
            Object track = invokeClientResult("com.knoxhack.echosoundcore.client.music.SoundCoreMusicManager", "currentTrackId");
            if (track instanceof Identifier id) {
                currentTrack = id;
            }
            Object currentPriority = invokeClientResult("com.knoxhack.echosoundcore.client.music.SoundCoreMusicManager", "currentPriority");
            priority = currentPriority == null ? "" : currentPriority.toString();
            Object currentReason = invokeClientResult("com.knoxhack.echosoundcore.client.music.SoundCoreMusicManager", "currentSelectionReason");
            reason = currentReason == null ? "" : currentReason.toString();
            Object musicFailure = invokeClientResult("com.knoxhack.echosoundcore.client.music.SoundCoreMusicManager", "lastFailure");
            Object actionFailure = invokeClientResult("com.knoxhack.echosoundcore.client.SoundCoreClientActions", "lastFailure");
            Object ambienceFailure = invokeClientResult("com.knoxhack.echosoundcore.client.ambience.SoundCoreAmbienceManager", "lastFailure");
            lastFailure = firstNonBlank(musicFailure, actionFailure, ambienceFailure);
        }
        return new SoundServiceDiagnostics(true, currentTrack, priority, reason,
                SoundCoreDataReloadListener.getMusicProfiles().size(),
                SoundCoreDataReloadListener.getAmbienceProfiles().size(),
                activeEvents(),
                SoundCoreCatalogValidator.missingAssetPaths(),
                lastFailure);
    }

    private static boolean sendContext(Player player, SoundCoreAudioPacket packet) {
        if (player instanceof ServerPlayer serverPlayer) {
            return EchoNetSend.toPlayer(serverPlayer, packet, EchoPacketKind.OPTIONAL_ADDON);
        }
        return invokeClient("handle", new Class<?>[] { SoundCoreAudioPacket.class }, packet);
    }

    private static boolean invokeClient(String methodName, Class<?>[] parameterTypes, Object... args) {
        if (!isClientRuntime()) {
            return false;
        }
        try {
            Object result = Class.forName("com.knoxhack.echosoundcore.client.SoundCoreClientActions")
                    .getMethod(methodName, parameterTypes)
                    .invoke(null, args);
            return !(result instanceof Boolean value) || value;
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoSoundCore.LOGGER.debug("SoundCore client action {} unavailable.", methodName, exception);
            return false;
        }
    }

    private static Object invokeClientResult(String className, String methodName) {
        try {
            return Class.forName(className).getMethod(methodName).invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static boolean isClientRuntime() {
        try {
            Class.forName("net.minecraft.client.Minecraft");
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }

    private static String firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = value == null ? "" : value.toString();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }
}
