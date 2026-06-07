package com.knoxhack.echosoundcore.network;

import com.knoxhack.echosoundcore.EchoSoundCore;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SoundCoreAudioPacket(
        SoundCoreAudioAction action,
        Identifier eventId,
        Identifier profileId,
        Map<String, String> context,
        float volume,
        float pitch) implements CustomPacketPayload {
    private static final int MAX_ID = 192;
    private static final int MAX_CONTEXT_KEY = 64;
    private static final int MAX_CONTEXT_VALUE = 192;
    private static final int MAX_CONTEXT_ENTRIES = 32;

    public static final Identifier ID = EchoSoundCore.id("audio_action");
    public static final Type<SoundCoreAudioPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SoundCoreAudioPacket> CODEC =
            StreamCodec.of(SoundCoreAudioPacket::write, SoundCoreAudioPacket::read);

    public SoundCoreAudioPacket {
        action = action == null ? SoundCoreAudioAction.PLAY_ONESHOT : action;
        context = Map.copyOf(context == null ? Map.of() : context);
        if (volume < 0.0f) {
            volume = 0.0f;
        }
        if (pitch <= 0.0f) {
            pitch = 1.0f;
        }
    }

    public static SoundCoreAudioPacket playOneShot(Identifier eventId, float volume, float pitch) {
        return new SoundCoreAudioPacket(SoundCoreAudioAction.PLAY_ONESHOT, eventId, null, Map.of(), volume, pitch);
    }

    public static SoundCoreAudioPacket stopEvent(Identifier eventId) {
        return new SoundCoreAudioPacket(SoundCoreAudioAction.STOP_EVENT, eventId, null, Map.of(), 1.0f, 1.0f);
    }

    public static SoundCoreAudioPacket setContext(Map<String, String> context) {
        return new SoundCoreAudioPacket(SoundCoreAudioAction.SET_CONTEXT, null, null, context, 1.0f, 1.0f);
    }

    public static SoundCoreAudioPacket patchContext(Map<String, String> patch) {
        return new SoundCoreAudioPacket(SoundCoreAudioAction.PATCH_CONTEXT, null, null, patch, 1.0f, 1.0f);
    }

    public static SoundCoreAudioPacket clearContext() {
        return new SoundCoreAudioPacket(SoundCoreAudioAction.CLEAR_CONTEXT, null, null, Map.of(), 1.0f, 1.0f);
    }

    public static SoundCoreAudioPacket playProfile(Identifier profileId) {
        return new SoundCoreAudioPacket(SoundCoreAudioAction.PLAY_PROFILE, null, profileId, Map.of(), 1.0f, 1.0f);
    }

    public static SoundCoreAudioPacket stopControlledAudio() {
        return new SoundCoreAudioPacket(SoundCoreAudioAction.STOP_CONTROLLED_AUDIO, null, null, Map.of(), 1.0f, 1.0f);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, SoundCoreAudioPacket packet) {
        buffer.writeEnum(packet.action());
        writeOptionalId(buffer, packet.eventId());
        writeOptionalId(buffer, packet.profileId());
        Map<String, String> context = packet.context();
        buffer.writeVarInt(Math.min(context.size(), MAX_CONTEXT_ENTRIES));
        int written = 0;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            if (written++ >= MAX_CONTEXT_ENTRIES) {
                break;
            }
            buffer.writeUtf(entry.getKey(), MAX_CONTEXT_KEY);
            buffer.writeUtf(entry.getValue(), MAX_CONTEXT_VALUE);
        }
        buffer.writeFloat(packet.volume());
        buffer.writeFloat(packet.pitch());
    }

    private static SoundCoreAudioPacket read(RegistryFriendlyByteBuf buffer) {
        SoundCoreAudioAction action = buffer.readEnum(SoundCoreAudioAction.class);
        Identifier eventId = readOptionalId(buffer);
        Identifier profileId = readOptionalId(buffer);
        int count = Math.min(buffer.readVarInt(), MAX_CONTEXT_ENTRIES);
        Map<String, String> context = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            context.put(buffer.readUtf(MAX_CONTEXT_KEY), buffer.readUtf(MAX_CONTEXT_VALUE));
        }
        return new SoundCoreAudioPacket(action, eventId, profileId, context, buffer.readFloat(), buffer.readFloat());
    }

    private static void writeOptionalId(RegistryFriendlyByteBuf buffer, Identifier id) {
        buffer.writeBoolean(id != null);
        if (id != null) {
            buffer.writeUtf(id.toString(), MAX_ID);
        }
    }

    private static Identifier readOptionalId(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Identifier.parse(buffer.readUtf(MAX_ID)) : null;
    }
}
