package com.knoxhack.echomultiblockcore.network;

import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads(Object event) {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        registrar.playToClient(RobotAnimationPacket.TYPE, RobotAnimationPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> handleClient("handleRobotAnimation", packet)));
        registrar.playToClient(MultiblockDefinitionMetadataPacket.TYPE, MultiblockDefinitionMetadataPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> handleClient("handleDefinitionMetadata", packet)));
        registrar.playToClient(AutomationRecipeMetadataPacket.TYPE, AutomationRecipeMetadataPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> handleClient("handleAutomationRecipeMetadata", packet)));
        registrar.playToClient(MultiblockBuildAssistPacket.TYPE, MultiblockBuildAssistPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> handleClient("handleBuildAssistMetadata", packet)));
    }

    private static void handleClient(String method, Object packet) {
        try {
            Class.forName("com.knoxhack.echomultiblockcore.client.MultiblockClientPackets")
                    .getMethod(method, packet.getClass())
                    .invoke(null, packet);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
