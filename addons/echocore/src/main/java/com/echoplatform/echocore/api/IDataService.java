package com.echoplatform.echocore.api;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IDataService {
    default <T> IDataKey<T> registerKey(IDataKey<T> key) {
        return key;
    }

    default void register(IDataKey<?> key) {
        registerKey(key);
    }

    default Optional<IDataKey<?>> key(Identifier id) {
        return Optional.empty();
    }

    default Optional<DataKeyMetadata> keyMetadata(Identifier id) {
        return Optional.ofNullable(allKeyMetadata().get(id));
    }

    default DataServiceDiagnostics diagnostics() {
        return DataServiceDiagnostics.unavailable();
    }

    default Map<Identifier, DataKeyMetadata> allKeyMetadata() {
        return Map.of();
    }

    default java.util.List<IDataKey<?>> registeredKeys() {
        return java.util.List.of();
    }

    default IPlayerDataView player(Player player) {
        return DataView.EMPTY;
    }

    default IWorldDataView world(Level level) {
        return DataView.EMPTY;
    }

    default ITeamDataView team(Level level, Identifier teamId) {
        return DataView.EMPTY;
    }

    default IDataSyncBridge syncBridge() {
        return new IDataSyncBridge() {
            @Override
            public void requestFullSync(ServerPlayer player) {
            }

            @Override
            public void markDirty(DataScope scope, String ownerId, Identifier keyId) {
            }

            @Override
            public long revision() {
                return 0L;
            }
        };
    }

    interface DataView extends IPlayerDataView, IWorldDataView, ITeamDataView {
        DataView EMPTY = new DataView() {
        };
    }
}
