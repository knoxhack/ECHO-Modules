package com.knoxhack.echospellcore.network;

import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import com.knoxhack.echonetcore.api.EchoRateLimitPolicy;
import com.knoxhack.echospellcore.EchoSpellCore;
import com.knoxhack.echospellcore.api.SpellCoreApi;
import com.knoxhack.echospellcore.client.SpellPredictionClientState;
import com.knoxhack.echospellcore.menu.SpellDeckMenu;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads(Object event) {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.serverboundAction(registrar, SpellLoadoutActionPacket.TYPE, SpellLoadoutActionPacket.CODEC,
                EchoRateLimitPolicy.of(5, "spell_loadout_action"), ModNetwork::handleLoadoutAction);
        EchoNetPayloads.clientboundSync(registrar, SpellProjectileSyncPacket.TYPE, SpellProjectileSyncPacket.CODEC,
                (packet, player, context) -> SpellPredictionClientState.apply(packet));
    }

    private static void handleLoadoutAction(SpellLoadoutActionPacket packet, ServerPlayer player,
            EchoPayloadContext context) {
        SpellCoreApi.applyLoadoutAction(player, packet.action(), packet.slot(), packet.spellId(), packet.modifierId());
        if (player.containerMenu instanceof SpellDeckMenu menu) {
            menu.refreshSocketsFromDeck();
        }
    }
}
