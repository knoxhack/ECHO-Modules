package com.knoxhack.echoorbitalremnants.network;

import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import com.knoxhack.echoorbitalremnants.faction.OrbitalFactionDialogueService;
import com.knoxhack.echoorbitalremnants.item.EchoTerminalItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void registerPayloads(Object event) {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.clientboundSync(registrar, OpenEchoTerminalPayload.TYPE, OpenEchoTerminalPayload.STREAM_CODEC);
        EchoNetPayloads.clientboundSync(registrar, OrbitalFactionDialogueOpenPayload.TYPE, OrbitalFactionDialogueOpenPayload.STREAM_CODEC);
        registrar.playToClient(OrbitalEventVisualPayload.TYPE, OrbitalEventVisualPayload.STREAM_CODEC);
        EchoNetPayloads.serverboundAction(registrar, EchoTerminalActionPayload.TYPE, EchoTerminalActionPayload.STREAM_CODEC,
                EchoNetPayloads.terminalActionPolicy("orbital_terminal_action"), ModNetworking::handleTerminalAction);
        EchoNetPayloads.serverboundAction(registrar, OrbitalFactionNpcActionPayload.TYPE, OrbitalFactionNpcActionPayload.STREAM_CODEC,
                EchoNetPayloads.defaultActionPolicy("orbital_faction_npc_action"),
                (payload, player, context) -> OrbitalFactionDialogueService.handleAction(payload, player));
    }

    private static void handleTerminalAction(EchoTerminalActionPayload payload, ServerPlayer player,
            EchoPayloadContext context) {
        if (!EchoTerminalItem.hasTerminal(player)) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Terminal link lost."));
            return;
        }
        if (payload.action() == EchoTerminalActionPayload.Action.SCAN) {
            EchoTerminalItem.performScan(player);
        }
        EchoTerminalItem.openTerminal(player);
    }
}
