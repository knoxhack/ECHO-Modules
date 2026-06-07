package com.knoxhack.echo.npcore.data;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class InMemoryNpcDataStorage implements EchoNpcDataStorage {
    private final Map<UUID, NpcConversionRecord> conversions = new ConcurrentHashMap<>();
    private final Map<String, String> dialogueNodes = new ConcurrentHashMap<>();
    private final Map<String, Integer> stocks = new ConcurrentHashMap<>();
    private final Map<String, Long> tradeRestocks = new ConcurrentHashMap<>();
    private final Map<String, Long> serviceCooldowns = new ConcurrentHashMap<>();

    @Override
    public void recordConversion(ServerLevel level, NpcConversionRecord record) {
        if (record != null && record.oldEntityUuid() != null) {
            conversions.put(record.oldEntityUuid(), record);
        }
    }

    @Override
    public Optional<NpcConversionRecord> conversion(ServerLevel level, UUID oldEntityUuid) {
        return Optional.ofNullable(conversions.get(oldEntityUuid));
    }

    @Override
    public String dialogueNode(UUID playerId, UUID npcId) {
        return dialogueNodes.getOrDefault(key(playerId, npcId, "dialogue"), "");
    }

    @Override
    public void setDialogueNode(UUID playerId, UUID npcId, String nodeId) {
        dialogueNodes.put(key(playerId, npcId, "dialogue"), nodeId == null ? "" : nodeId);
    }

    @Override
    public int stock(ServerLevel level, UUID npcId, String offerId, int initialStock) {
        return stocks.computeIfAbsent(key(npcId, offerId), ignored -> initialStock);
    }

    @Override
    public void setStock(ServerLevel level, UUID npcId, String offerId, int stock) {
        stocks.put(key(npcId, offerId), stock);
    }

    @Override
    public long tradeRestockAt(ServerLevel level, UUID npcId, String offerId) {
        return tradeRestocks.getOrDefault(key(npcId, offerId), 0L);
    }

    @Override
    public void setTradeRestockAt(ServerLevel level, UUID npcId, String offerId, long gameTime) {
        tradeRestocks.put(key(npcId, offerId), Math.max(0L, gameTime));
    }

    @Override
    public long serviceCooldownUntil(ServerPlayer player, UUID npcId, String serviceId) {
        return serviceCooldowns.getOrDefault(key(playerId(player), npcId, serviceId), 0L);
    }

    @Override
    public void setServiceCooldownUntil(ServerPlayer player, UUID npcId, String serviceId, long gameTime) {
        serviceCooldowns.put(key(playerId(player), npcId, serviceId), gameTime);
    }

    @Override
    public String mode() {
        return "memory";
    }

    private static String key(UUID left, UUID right, String suffix) {
        return left + "|" + right + "|" + suffix;
    }

    private static String key(UUID id, String suffix) {
        return id + "|" + suffix;
    }

    private static UUID playerId(ServerPlayer player) {
        return player == null ? new UUID(0L, 0L) : player.getUUID();
    }
}
