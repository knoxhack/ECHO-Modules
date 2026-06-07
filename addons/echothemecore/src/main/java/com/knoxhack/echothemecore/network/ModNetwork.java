package com.knoxhack.echothemecore.network;

import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads() {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        registrar.playToClient(ThemeSyncPacket.TYPE, ThemeSyncPacket.CODEC,
            (packet, context) -> context.enqueueWork(() -> ThemeCoreClientPacketHooks.applyTheme(packet.themeId())));
        registrar.playToClient(PlayerThemeSyncPacket.TYPE, PlayerThemeSyncPacket.CODEC,
            (packet, context) -> context.enqueueWork(() -> ThemeCoreClientPacketHooks.applyTheme(packet.themeId())));
    }
}
