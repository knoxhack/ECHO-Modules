package com.knoxhack.echoindex.event;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.IndexIds;
import com.knoxhack.echoindex.network.IndexSync;
import com.knoxhack.echoindex.service.IndexDiscoveryStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class IndexEvents {
    private static final String ROOT = "echoindex_state";

    private IndexEvents() {
    }

    public static void register() {
        EchoBackendLifecycleBridge.registerGameEventHandler(IndexEvents::onPlayerLogin);
        EchoBackendLifecycleBridge.registerGameEventHandler(IndexEvents::onPlayerClone);
        EchoBackendLifecycleBridge.registerGameEventHandler(IndexEvents::onItemCrafted);
        EchoBackendLifecycleBridge.registerGameEventHandler(IndexEvents::onItemPickup);
    }

    public static void onPlayerLogin(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.loggedInServerPlayer(event);
        if (player != null) {
            IndexDiscoveryStore.INSTANCE.discover(player, IndexIds.ENTRY_OVERVIEW);
            IndexSync.send(player);
        }
    }

    public static void onPlayerClone(Object event) {
        var originalPlayer = EchoBackendWorldEventBridge.cloneOriginalPlayer(event);
        var newPlayer = EchoBackendWorldEventBridge.cloneNewPlayer(event);
        if (originalPlayer == null || newPlayer == null) {
            return;
        }
        CompoundTag original = originalPlayer.getPersistentData().getCompoundOrEmpty(ROOT);
        if (!original.isEmpty()) {
            newPlayer.getPersistentData().put(ROOT, original.copy());
        }
    }

    public static void onItemCrafted(Object event) {
        if (EchoBackendWorldEventBridge.itemCraftedPlayer(event) instanceof ServerPlayer player) {
            IndexDiscoveryStore.INSTANCE.discover(player, IndexIds.ENTRY_OVERVIEW);
        }
    }

    public static void onItemPickup(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.itemPickupServerPlayer(event);
        if (player != null && !EchoBackendWorldEventBridge.itemPickupOriginalStack(event).isEmpty()) {
            IndexDiscoveryStore.INSTANCE.discover(player, IndexIds.ENTRY_OVERVIEW);
        }
    }
}
