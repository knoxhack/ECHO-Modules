package com.knoxhack.echo.npcore.data;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface EchoNpcDataStorage {
    void recordConversion(ServerLevel level, NpcConversionRecord record);

    Optional<NpcConversionRecord> conversion(ServerLevel level, UUID oldEntityUuid);

    String dialogueNode(UUID playerId, UUID npcId);

    void setDialogueNode(UUID playerId, UUID npcId, String nodeId);

    int stock(ServerLevel level, UUID npcId, String offerId, int initialStock);

    void setStock(ServerLevel level, UUID npcId, String offerId, int stock);

    long tradeRestockAt(ServerLevel level, UUID npcId, String offerId);

    void setTradeRestockAt(ServerLevel level, UUID npcId, String offerId, long gameTime);

    long serviceCooldownUntil(ServerPlayer player, UUID npcId, String serviceId);

    void setServiceCooldownUntil(ServerPlayer player, UUID npcId, String serviceId, long gameTime);

    String mode();

    default boolean persistentBackendAvailable() {
        return false;
    }

    default int registeredDataKeyCount() {
        return 0;
    }
}
