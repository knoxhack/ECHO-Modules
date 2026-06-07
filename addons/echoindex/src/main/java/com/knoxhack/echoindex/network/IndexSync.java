package com.knoxhack.echoindex.network;

import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echoindex.service.IndexDiscoveryStore;
import com.knoxhack.echoindex.service.IndexRecipeSnapshotCodec;
import com.knoxhack.echoindex.service.IndexService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class IndexSync {
    private IndexSync() {
    }

    public static boolean send(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        CompoundTag tag = IndexDiscoveryStore.INSTANCE.syncTag(player);
        tag.put("recipe_health", IndexRecipeSnapshotCodec.encodeHealth(IndexService.INSTANCE.recipeSnapshot(player)));
        return EchoNetSend.toPlayer(player,
                new IndexStateSyncPacket(tag),
                EchoPacketKind.CLIENTBOUND_SYNC);
    }
}
