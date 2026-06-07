package com.knoxhack.signalos.network;

import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import com.knoxhack.signalos.api.SignalOsActionResult;
import com.knoxhack.signalos.api.TerminalActionRegistry;
import com.knoxhack.signalos.client.SignalOsClientState;
import com.knoxhack.signalos.integration.SignalOsRuntimeSpineBridge;
import com.knoxhack.signalos.service.SignalOsRackActions;
import com.knoxhack.signalos.service.SignalOsTerminalServices;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads(Object event) {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.clientboundSync(registrar, SignalOsTerminalStatePacket.TYPE, SignalOsTerminalStatePacket.CODEC,
                (packet, player, context) -> SignalOsClientState.apply(packet));
        EchoNetPayloads.serverboundAction(registrar, SignalOsOpenTerminalPacket.TYPE, SignalOsOpenTerminalPacket.CODEC,
                EchoNetPayloads.defaultActionPolicy("open_terminal"), ModNetwork::handleOpenTerminal);
        EchoNetPayloads.serverboundAction(registrar, SignalOsActionPacket.TYPE, SignalOsActionPacket.CODEC,
                EchoNetPayloads.terminalActionPolicy("terminal_action"), ModNetwork::handleTerminalAction);
        EchoNetPayloads.serverboundAction(registrar, SignalOsRackActionPacket.TYPE, SignalOsRackActionPacket.CODEC,
                EchoNetPayloads.defaultActionPolicy("rack_action"), ModNetwork::handleRackAction);
    }

    private static void handleOpenTerminal(SignalOsOpenTerminalPacket packet, ServerPlayer player,
            EchoPayloadContext context) {
        SignalOsTerminalServices.openRemoteTerminal(player);
    }

    private static void handleTerminalAction(SignalOsActionPacket packet, ServerPlayer player,
            EchoPayloadContext context) {
        SignalOsActionResult result = TerminalActionRegistry.handleResult(player, packet.pageId(), packet.actionId(),
                packet.payload());
        if (!result.handled()) {
            player.sendSystemMessage(Component.literal("[SignalOS] Unknown terminal action."), true);
        } else if (!result.message().isBlank()) {
            player.sendSystemMessage(Component.literal(result.message()), true);
            SignalOsTerminalServices.recordActionStatus(player, result.message());
        }
        if (result.success()) {
            SignalOsRuntimeSpineBridge.publishAction(player, packet, result);
        }
        SignalOsTerminalSync.send(player);
    }

    private static void handleRackAction(SignalOsRackActionPacket packet, ServerPlayer player,
            EchoPayloadContext context) {
        if (!SignalOsRackActions.handle(player, packet)) {
            player.sendSystemMessage(Component.literal("[SignalOS] Unknown rack action."), true);
        }
        SignalOsTerminalSync.send(player);
    }
}
