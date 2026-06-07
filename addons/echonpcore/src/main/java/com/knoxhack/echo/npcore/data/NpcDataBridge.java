package com.knoxhack.echo.npcore.data;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class NpcDataBridge {
    private static final EchoNpcDataStorage STORAGE = new HybridNpcDataStorage();

    private NpcDataBridge() {
    }

    public static EchoNpcDataStorage storage() {
        return STORAGE;
    }

    public static void recordConversion(ServerLevel level, NpcConversionRecord record) {
        STORAGE.recordConversion(level, record);
    }

    public static Optional<NpcConversionRecord> conversion(ServerLevel level, UUID oldEntityUuid) {
        return STORAGE.conversion(level, oldEntityUuid);
    }

    public static String dialogueNode(UUID playerId, UUID npcId) {
        return STORAGE.dialogueNode(playerId, npcId);
    }

    public static void setDialogueNode(UUID playerId, UUID npcId, String nodeId) {
        STORAGE.setDialogueNode(playerId, npcId, nodeId);
    }

    public static int stock(ServerLevel level, UUID npcId, String offerId, int initialStock) {
        return STORAGE.stock(level, npcId, offerId, initialStock);
    }

    public static void setStock(ServerLevel level, UUID npcId, String offerId, int stock) {
        STORAGE.setStock(level, npcId, offerId, stock);
    }

    public static long tradeRestockAt(ServerLevel level, UUID npcId, String offerId) {
        return STORAGE.tradeRestockAt(level, npcId, offerId);
    }

    public static void setTradeRestockAt(ServerLevel level, UUID npcId, String offerId, long gameTime) {
        STORAGE.setTradeRestockAt(level, npcId, offerId, gameTime);
    }

    public static long serviceCooldownUntil(ServerPlayer player, UUID npcId, String serviceId) {
        return STORAGE.serviceCooldownUntil(player, npcId, serviceId);
    }

    public static void setServiceCooldownUntil(ServerPlayer player, UUID npcId, String serviceId, long gameTime) {
        STORAGE.setServiceCooldownUntil(player, npcId, serviceId, gameTime);
    }

    public static String storageMode() {
        return STORAGE.mode();
    }

    public static boolean persistentBackendAvailable() {
        return STORAGE.persistentBackendAvailable();
    }

    public static int registeredDataKeyCount() {
        return STORAGE.registeredDataKeyCount();
    }
}
