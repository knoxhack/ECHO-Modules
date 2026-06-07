package com.knoxhack.echoindex.network;

import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echoindex.integration.IndexLensMissionBridge;
import com.knoxhack.echoindex.integration.IndexMissionHooks;
import com.knoxhack.echoindex.service.IndexDiscoveryStore;
import com.knoxhack.echoindex.service.IndexRecipeQueryClientState;
import com.knoxhack.echoindex.service.IndexRecipeSnapshot;
import com.knoxhack.echoindex.service.IndexRecipeSnapshotCodec;
import com.knoxhack.echoindex.service.IndexRecipeSourceKind;
import com.knoxhack.echoindex.service.IndexRecipeTransferService;
import com.knoxhack.echoindex.service.IndexService;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads(Object event) {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.clientboundSync(registrar, IndexStateSyncPacket.TYPE, IndexStateSyncPacket.CODEC,
                (packet, player, context) -> IndexDiscoveryStore.INSTANCE.applyClientSync(packet.state()));
        EchoNetPayloads.clientboundSync(registrar, IndexRecipeQueryResultPacket.TYPE, IndexRecipeQueryResultPacket.CODEC,
                (packet, player, context) -> IndexRecipeQueryClientState.apply(packet.itemId(), packet.result()));
        EchoNetPayloads.serverboundAction(registrar, IndexActionPacket.TYPE, IndexActionPacket.CODEC,
                EchoNetPayloads.defaultActionPolicy("echoindex_action"), ModNetwork::handleAction);
        EchoNetPayloads.serverboundAction(registrar, IndexRecipeQueryPacket.TYPE, IndexRecipeQueryPacket.CODEC,
                EchoNetPayloads.defaultActionPolicy("echoindex_recipe_query"), ModNetwork::handleRecipeQuery);
    }

    private static void handleAction(IndexActionPacket packet, ServerPlayer player,
            EchoPayloadContext context) {
        switch (packet.action()) {
            case REQUEST_SYNC -> {
                IndexMissionHooks.recordOpenSearch(player, packet.targetId());
                IndexSync.send(player);
            }
            case MARK_READ -> {
                if (IndexService.INSTANCE.entry(player, packet.targetId()).isPresent()) {
                    IndexDiscoveryStore.INSTANCE.markRead(player, packet.targetId());
                    IndexMissionHooks.recordOpenSearch(player, packet.targetId());
                    IndexMissionHooks.recordSourceNote(player, packet.targetId());
                    IndexMissionHooks.recordReadEntry(player, packet.targetId());
                }
                IndexSync.send(player);
            }
            case BOOKMARK -> {
                IndexDiscoveryStore.INSTANCE.setBookmarked(player, packet.targetId(), true);
                IndexMissionHooks.recordBookmark(player, packet.targetId());
                IndexSync.send(player);
            }
            case UNBOOKMARK -> {
                IndexDiscoveryStore.INSTANCE.setBookmarked(player, packet.targetId(), false);
                IndexSync.send(player);
            }
            case PIN_RECIPE -> {
                if (packet.targetId() != null) {
                    IndexDiscoveryStore.INSTANCE.setRecipePinned(player, packet.targetId(), true);
                    IndexMissionHooks.recordRecipeInspect(player, packet.targetId());
                    IndexMissionHooks.recordRecipePin(player, packet.targetId());
                    IndexLensMissionBridge.recordIndexShortcut(player, "PIN_RECIPE");
                }
                IndexSync.send(player);
            }
            case UNPIN_RECIPE -> {
                IndexDiscoveryStore.INSTANCE.setRecipePinned(player, packet.targetId(), false);
                IndexSync.send(player);
            }
            case TRANSFER_RECIPE -> {
                boolean transferred = IndexRecipeTransferService.transfer(player, packet.targetId());
                if (transferred) {
                    IndexMissionHooks.recordRecipeInspect(player, packet.targetId());
                    IndexMissionHooks.recordSourceNote(player, packet.targetId());
                    IndexMissionHooks.recordRecipeTransfer(player, packet.targetId());
                    IndexLensMissionBridge.recordIndexShortcut(player, "TRANSFER_RECIPE");
                }
            }
        }
    }

    public static void handleActionForTests(IndexActionPacket packet, ServerPlayer player) {
        handleAction(packet, player, null);
    }

    private static void handleRecipeQuery(IndexRecipeQueryPacket packet, ServerPlayer player,
            EchoPayloadContext context) {
        Identifier itemId = packet.itemId();
        String warning = "";
        List<IndexRecipeView> recipes = List.of();
        List<IndexRecipeView> uses = List.of();
        List<IndexRecipeView> sources = List.of();
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshot(player);
        if (itemId == null) {
            warning = "Missing item id.";
        } else {
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            if (item == null) {
                warning = "Unknown item id: " + itemId;
            } else {
                List<IndexRecipeView> outputViews = snapshot.recipesFor(item);
                if (packet.recipes()) {
                    recipes = outputViews.stream()
                            .filter(recipe -> !IndexRecipeSourceKind.isSourceCard(recipe))
                            .toList();
                }
                if (packet.uses()) {
                    uses = snapshot.usesFor(item);
                }
                if (packet.sources()) {
                    sources = outputViews.stream()
                            .filter(IndexRecipeSourceKind::isSourceCard)
                            .toList();
                }
                IndexMissionHooks.recordRecipeInspect(player, itemId);
            }
        }
        CompoundTag tag = IndexRecipeSnapshotCodec.encodeQueryResult(itemId, snapshot, recipes, uses, sources, warning);
        EchoNetSend.toPlayer(player, new IndexRecipeQueryResultPacket(itemId, tag));
    }
}
