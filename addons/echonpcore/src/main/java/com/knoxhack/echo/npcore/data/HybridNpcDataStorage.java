package com.knoxhack.echo.npcore.data;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.echoplatform.echocore.api.DataScope;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.IDataKey;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps NPCore playable without DataCore while persisting durable state when the shared data backend is present.
 */
public final class HybridNpcDataStorage implements EchoNpcDataStorage {
    private static final IDataKey<CompoundTag> TRADE_STOCKS = recordKey("npc/trade_stocks", DataScope.WORLD);
    private static final IDataKey<CompoundTag> TRADE_RESTOCKS = recordKey("npc/trade_restocks", DataScope.WORLD);
    private static final IDataKey<CompoundTag> CONVERSIONS = recordKey("npc/conversions", DataScope.WORLD);
    private static final IDataKey<CompoundTag> SERVICE_COOLDOWNS = recordKey("npc/service_cooldowns", DataScope.PLAYER);

    private final InMemoryNpcDataStorage memory = new InMemoryNpcDataStorage();
    private final AtomicBoolean keysRegistered = new AtomicBoolean(false);

    @Override
    public void recordConversion(ServerLevel level, NpcConversionRecord record) {
        memory.recordConversion(level, record);
        if (level == null || record == null || record.oldEntityUuid() == null || !dataCoreAvailable()) {
            return;
        }
        CompoundTag root = worldRecord(level, CONVERSIONS);
        root.put(record.oldEntityUuid().toString(), encode(record));
        setWorldRecord(level, CONVERSIONS, root);
    }

    @Override
    public Optional<NpcConversionRecord> conversion(ServerLevel level, UUID oldEntityUuid) {
        if (level != null && oldEntityUuid != null && dataCoreAvailable()) {
            CompoundTag root = worldRecord(level, CONVERSIONS);
            String key = oldEntityUuid.toString();
            if (root.contains(key)) {
                return decode(root.getCompoundOrEmpty(key));
            }
        }
        return memory.conversion(level, oldEntityUuid);
    }

    @Override
    public String dialogueNode(UUID playerId, UUID npcId) {
        return memory.dialogueNode(playerId, npcId);
    }

    @Override
    public void setDialogueNode(UUID playerId, UUID npcId, String nodeId) {
        memory.setDialogueNode(playerId, npcId, nodeId);
    }

    @Override
    public int stock(ServerLevel level, UUID npcId, String offerId, int initialStock) {
        if (level != null && npcId != null && dataCoreAvailable()) {
            CompoundTag root = worldRecord(level, TRADE_STOCKS);
            String key = key(npcId, offerId);
            if (root.contains(key)) {
                return Math.max(0, root.getIntOr(key, initialStock));
            }
        }
        return memory.stock(level, npcId, offerId, initialStock);
    }

    @Override
    public void setStock(ServerLevel level, UUID npcId, String offerId, int stock) {
        int safeStock = Math.max(0, stock);
        memory.setStock(level, npcId, offerId, safeStock);
        if (level == null || npcId == null || !dataCoreAvailable()) {
            return;
        }
        CompoundTag root = worldRecord(level, TRADE_STOCKS);
        root.putInt(key(npcId, offerId), safeStock);
        setWorldRecord(level, TRADE_STOCKS, root);
    }

    @Override
    public long tradeRestockAt(ServerLevel level, UUID npcId, String offerId) {
        if (level != null && npcId != null && dataCoreAvailable()) {
            CompoundTag root = worldRecord(level, TRADE_RESTOCKS);
            String key = key(npcId, offerId);
            if (root.contains(key)) {
                return Math.max(0L, root.getLongOr(key, 0L));
            }
        }
        return memory.tradeRestockAt(level, npcId, offerId);
    }

    @Override
    public void setTradeRestockAt(ServerLevel level, UUID npcId, String offerId, long gameTime) {
        long safeTime = Math.max(0L, gameTime);
        memory.setTradeRestockAt(level, npcId, offerId, safeTime);
        if (level == null || npcId == null || !dataCoreAvailable()) {
            return;
        }
        CompoundTag root = worldRecord(level, TRADE_RESTOCKS);
        root.putLong(key(npcId, offerId), safeTime);
        setWorldRecord(level, TRADE_RESTOCKS, root);
    }

    @Override
    public long serviceCooldownUntil(ServerPlayer player, UUID npcId, String serviceId) {
        if (player != null && npcId != null && dataCoreAvailable()) {
            CompoundTag root = playerRecord(player, SERVICE_COOLDOWNS);
            String key = key(npcId, serviceId);
            if (root.contains(key)) {
                return Math.max(0L, root.getLongOr(key, 0L));
            }
        }
        return memory.serviceCooldownUntil(player, npcId, serviceId);
    }

    @Override
    public void setServiceCooldownUntil(ServerPlayer player, UUID npcId, String serviceId, long gameTime) {
        long safeTime = Math.max(0L, gameTime);
        memory.setServiceCooldownUntil(player, npcId, serviceId, safeTime);
        if (player == null || npcId == null || !dataCoreAvailable()) {
            return;
        }
        CompoundTag root = playerRecord(player, SERVICE_COOLDOWNS);
        root.putLong(key(npcId, serviceId), safeTime);
        setPlayerRecord(player, SERVICE_COOLDOWNS, root);
    }

    @Override
    public String mode() {
        return dataCoreAvailable() ? "hybrid-datacore" : "memory";
    }

    @Override
    public boolean persistentBackendAvailable() {
        return dataCoreAvailable();
    }

    @Override
    public int registeredDataKeyCount() {
        return keysRegistered.get() ? 4 : 0;
    }

    private boolean dataCoreAvailable() {
        try {
            boolean available = EchoCoreServices.dataService().diagnostics().available();
            if (available) {
                registerKeys();
            }
            return available;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void registerKeys() {
        if (!keysRegistered.compareAndSet(false, true)) {
            return;
        }
        EchoCoreServices.registerDataKey(TRADE_STOCKS);
        EchoCoreServices.registerDataKey(TRADE_RESTOCKS);
        EchoCoreServices.registerDataKey(CONVERSIONS);
        EchoCoreServices.registerDataKey(SERVICE_COOLDOWNS);
    }

    private static CompoundTag worldRecord(ServerLevel level, IDataKey<CompoundTag> key) {
        CompoundTag value = EchoCoreServices.worldData(level).get(key);
        return value == null ? new CompoundTag() : value.copy();
    }

    private static void setWorldRecord(ServerLevel level, IDataKey<CompoundTag> key, CompoundTag value) {
        EchoCoreServices.worldData(level).set(key, value == null ? new CompoundTag() : value.copy());
    }

    private static CompoundTag playerRecord(ServerPlayer player, IDataKey<CompoundTag> key) {
        CompoundTag value = EchoCoreServices.playerData(player).get(key);
        return value == null ? new CompoundTag() : value.copy();
    }

    private static void setPlayerRecord(ServerPlayer player, IDataKey<CompoundTag> key, CompoundTag value) {
        EchoCoreServices.playerData(player).set(key, value == null ? new CompoundTag() : value.copy());
    }

    private static CompoundTag encode(NpcConversionRecord record) {
        CompoundTag tag = new CompoundTag();
        tag.putString("oldEntityUuid", record.oldEntityUuid().toString());
        tag.putString("newEntityUuid", record.newEntityUuid() == null ? "" : record.newEntityUuid().toString());
        tag.putString("sourceType", record.sourceType() == null ? "" : record.sourceType());
        tag.putString("sourceProfession", record.sourceProfession() == null ? "" : record.sourceProfession());
        tag.putString("echoNpcProfile", record.echoNpcProfile() == null ? "" : record.echoNpcProfile().toString());
        tag.putLong("convertedAtGameTime", Math.max(0L, record.convertedAtGameTime()));
        return tag;
    }

    private static Optional<NpcConversionRecord> decode(CompoundTag tag) {
        UUID oldId = parseUuid(tag.getStringOr("oldEntityUuid", ""));
        UUID newId = parseUuid(tag.getStringOr("newEntityUuid", ""));
        Identifier profile = Identifier.tryParse(tag.getStringOr("echoNpcProfile", ""));
        if (oldId == null || newId == null || profile == null) {
            return Optional.empty();
        }
        return Optional.of(new NpcConversionRecord(
                oldId,
                newId,
                tag.getStringOr("sourceType", ""),
                tag.getStringOr("sourceProfession", ""),
                profile,
                tag.getLongOr("convertedAtGameTime", 0L)));
    }

    private static UUID parseUuid(String value) {
        try {
            return value == null || value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String key(UUID id, String suffix) {
        return id + "|" + safe(suffix);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static IDataKey<CompoundTag> recordKey(String path, DataScope scope) {
        return IDataKey.record(Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, path),
                scope, CompoundTag.CODEC, new CompoundTag(), false);
    }
}
