package com.knoxhack.echosoundcore.network;

import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import com.knoxhack.echosoundcore.EchoSoundCore;

public final class SoundCoreNetwork {
    private SoundCoreNetwork() {
    }

    public static void registerPayloads() {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.clientboundSync(registrar, SoundCoreAudioPacket.TYPE, SoundCoreAudioPacket.CODEC,
                (packet, player, context) -> handleClientbound(packet));
    }

    private static void handleClientbound(SoundCoreAudioPacket packet) {
        if (!isClientRuntime()) {
            return;
        }
        try {
            Class<?> actions = Class.forName("com.knoxhack.echosoundcore.client.SoundCoreClientActions");
            actions.getMethod("handle", SoundCoreAudioPacket.class).invoke(null, packet);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoSoundCore.LOGGER.warn("Could not dispatch SoundCore audio packet.", exception);
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
}
