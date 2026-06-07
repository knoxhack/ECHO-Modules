package com.knoxhack.echo.npcore.network;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echo.npcore.service.EchoNpcInteractionService;
import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoRateLimitPolicy;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;

public final class ModNetwork {
    private static final String VERSION = "1";

    private ModNetwork() {
    }

    public static void register(Object event) {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional(VERSION);
        EchoNetPayloads.clientboundSync(registrar, OpenNpcScreenPacket.TYPE, OpenNpcScreenPacket.CODEC,
                (packet, player, ctx) -> handleOpen(packet));
        EchoNetPayloads.clientboundSync(registrar, SyncNpcScreenStatePacket.TYPE, SyncNpcScreenStatePacket.CODEC,
                (packet, player, ctx) -> handleSync(packet));
        EchoNetPayloads.serverboundAction(registrar, SelectDialogueOptionPacket.TYPE, SelectDialogueOptionPacket.CODEC,
                EchoRateLimitPolicy.of(20, "npc_dialogue"), (packet, player, ctx) -> EchoNpcInteractionService.selectDialogueOption(player, packet));
        EchoNetPayloads.serverboundAction(registrar, RequestNpcTradePacket.TYPE, RequestNpcTradePacket.CODEC,
                EchoRateLimitPolicy.of(8, "npc_trade"), (packet, player, ctx) -> EchoNpcInteractionService.requestTrade(player, packet));
        EchoNetPayloads.serverboundAction(registrar, RequestNpcServicePacket.TYPE, RequestNpcServicePacket.CODEC,
                EchoRateLimitPolicy.of(8, "npc_service"), (packet, player, ctx) -> EchoNpcInteractionService.requestService(player, packet));
        EchoNetPayloads.serverboundAction(registrar, RequestNpcScreenRefreshPacket.TYPE, RequestNpcScreenRefreshPacket.CODEC,
                EchoRateLimitPolicy.of(10, "npc_refresh"), (packet, player, ctx) -> EchoNpcInteractionService.refresh(player, packet));
        EchoNetPayloads.serverboundAction(registrar, CloseNpcInteractionPacket.TYPE, CloseNpcInteractionPacket.CODEC,
                EchoRateLimitPolicy.of(20, "npc_close"), (packet, player, ctx) -> EchoNpcInteractionService.close(player, packet));
    }

    private static void handleOpen(OpenNpcScreenPacket packet) {
        dispatchToClientHandler("handleOpenNpcScreen", OpenNpcScreenPacket.class, packet);
    }

    private static void handleSync(SyncNpcScreenStatePacket packet) {
        dispatchToClientHandler("handleSyncNpcScreenState", SyncNpcScreenStatePacket.class, packet);
    }

    private static <T> void dispatchToClientHandler(String methodName, Class<T> packetType, T packet) {
        try {
            Class<?> handlerClass = Class.forName("com.knoxhack.echo.npcore.client.ClientNetworkHandlers");
            handlerClass.getMethod(methodName, packetType).invoke(null, packet);
        } catch (ClassNotFoundException | LinkageError ignored) {
            // Dedicated/native server runtimes do not load client handlers.
        } catch (ReflectiveOperationException exception) {
            EchoNpcCore.LOGGER.error("Failed to dispatch client network packet {}", packetType.getSimpleName(), exception);
        }
    }
}
