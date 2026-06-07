package com.knoxhack.echotutorialcore.network;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialGuideMode;
import com.knoxhack.echotutorialcore.api.hint.TutorialHint;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import com.knoxhack.echotutorialcore.server.TutorialProgressManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class TutorialNetworking {
    private TutorialNetworking() {}

    public static void register(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, TutorialNetworking::registerPayloads);
    }

    private static void registerPayloads(Object event) {
        var registrar = EchoNetPayloads.optional();
        registrar.playToClient(ShowTutorialHintPacket.TYPE, ShowTutorialHintPacket.CODEC, TutorialNetworking::handleShowHint);
        registrar.playToClient(ShowTutorialCardPacket.TYPE, ShowTutorialCardPacket.CODEC, TutorialNetworking::handleShowCard);
        registrar.playToClient(UnlockTutorialCardPacket.TYPE, UnlockTutorialCardPacket.CODEC, TutorialNetworking::handleUnlockCard);
        registrar.playToClient(SyncTutorialProgressPacket.TYPE, SyncTutorialProgressPacket.CODEC, TutorialNetworking::handleSyncProgress);
        registrar.playToClient(SyncTutorialContentPacket.TYPE, SyncTutorialContentPacket.CODEC, TutorialNetworking::handleSyncContent);
        EchoNetPayloads.serverboundAction(registrar, SetGuideModePacket.TYPE, SetGuideModePacket.CODEC,
                EchoNetPayloads.defaultActionPolicy("echotutorialcore_guide_mode"), TutorialNetworking::handleSetGuideModeRequest);
    }

    public static void sendShowHint(ServerPlayer player, TutorialHint hint) {
        if (hint == null) return;
        sendToPlayer(player, new ShowTutorialHintPacket(
                hint.id(), hint.type().name(), hint.title(), hint.message(), hint.details()));
    }

    public static void sendShowCard(ServerPlayer player, Identifier cardId) {
        if (cardId == null) return;
        sendToPlayer(player, new ShowTutorialCardPacket(cardId));
    }

    public static void sendSetGuideMode(ServerPlayer player, TutorialGuideMode mode) {
        if (mode == null) return;
        sendSyncProgress(player);
    }

    public static void sendUnlockCard(ServerPlayer player, Identifier cardId) {
        if (cardId == null) return;
        sendToPlayer(player, new UnlockTutorialCardPacket(cardId));
    }

    public static void sendSyncProgress(ServerPlayer player) {
        TutorialPlayerData data = TutorialPlayerData.get(player);
        sendToPlayer(player, new SyncTutorialProgressPacket(
                TutorialProgressManager.getGuideMode(player).name(),
                new java.util.ArrayList<>(data.progressFlags()),
                new java.util.ArrayList<>(data.unlockedCardIds()),
                new java.util.ArrayList<>(data.unreadCardIds()),
                new java.util.ArrayList<>(data.completedFlowIds()),
                data.lastRecommendationReason()));
    }

    public static void sendSyncContent(ServerPlayer player) {
        sendToPlayer(player, SyncTutorialContentPacket.fromRegistries());
    }

    private static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player == null || payload == null) return;
        EchoNetSend.toPlayer(player, payload, EchoPacketKind.CLIENTBOUND_SYNC);
    }

    // Client-side handlers use reflection so this class stays safe on dedicated server.
    private static void handleShowHint(ShowTutorialHintPacket packet, EchoPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Class<?> display = Class.forName("com.knoxhack.echotutorialcore.client.TutorialClientDisplay");
                display.getMethod("showHint", ShowTutorialHintPacket.class).invoke(null, packet);
            } catch (ReflectiveOperationException ignored) {
                EchoTutorialCore.LOGGER.debug("TutorialClientDisplay not available for hint.");
            }
        });
    }

    private static void handleShowCard(ShowTutorialCardPacket packet, EchoPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Class<?> display = Class.forName("com.knoxhack.echotutorialcore.client.TutorialClientDisplay");
                display.getMethod("showCardToast", Identifier.class).invoke(null, packet.cardId());
            } catch (ReflectiveOperationException ignored) {
                EchoTutorialCore.LOGGER.debug("TutorialClientDisplay not available for card toast.");
            }
        });
    }

    private static void handleSetGuideModeRequest(SetGuideModePacket packet, ServerPlayer player, EchoPayloadContext context) {
        context.enqueueWork(() -> {
            TutorialGuideMode mode = TutorialGuideMode.byName(packet.modeName());
            TutorialProgressManager.setGuideMode(player, mode);
        });
    }

    private static void handleUnlockCard(UnlockTutorialCardPacket packet, EchoPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Class<?> display = Class.forName("com.knoxhack.echotutorialcore.client.TutorialClientDisplay");
                display.getMethod("showUnlockCard", Identifier.class).invoke(null, packet.cardId());
            } catch (ReflectiveOperationException ignored) {
                EchoTutorialCore.LOGGER.debug("TutorialClientDisplay not available for unlock card.");
            }
        });
    }

    private static void handleSyncProgress(SyncTutorialProgressPacket packet, EchoPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Class<?> state = Class.forName("com.knoxhack.echotutorialcore.client.TutorialClientData");
                state.getMethod("applyProgress", SyncTutorialProgressPacket.class).invoke(null, packet);
            } catch (ReflectiveOperationException ignored) {
                EchoTutorialCore.LOGGER.debug("TutorialClientData not available for progress sync.");
            }
        });
    }

    private static void handleSyncContent(SyncTutorialContentPacket packet, EchoPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Class<?> state = Class.forName("com.knoxhack.echotutorialcore.client.TutorialClientData");
                state.getMethod("replaceContent", SyncTutorialContentPacket.class).invoke(null, packet);
            } catch (ReflectiveOperationException ignored) {
                EchoTutorialCore.LOGGER.debug("TutorialClientData not available for content sync.");
            }
        });
    }
}
