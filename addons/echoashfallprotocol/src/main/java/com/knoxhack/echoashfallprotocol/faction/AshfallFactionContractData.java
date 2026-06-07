package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

/**
 * Per-player Ashfall-owned progress for Echo Core faction contracts and services.
 */
public class AshfallFactionContractData implements EchoValueIOSerializable {
    public static final StreamCodec<RegistryFriendlyByteBuf, AshfallFactionContractData> STREAM_CODEC = StreamCodec.of(
            AshfallFactionContractData::writeSync,
            AshfallFactionContractData::readSync
    );

    private final Map<String, Integer> objectiveProgress = new HashMap<>();
    private final Map<String, Long> serviceCooldowns = new HashMap<>();

    public int progress(Identifier contractId, int objectiveIndex) {
        return objectiveProgress.getOrDefault(progressKey(contractId, objectiveIndex), 0);
    }

    public int setProgress(Identifier contractId, int objectiveIndex, int value, int max) {
        int clamped = Math.max(0, Math.min(max, value));
        objectiveProgress.put(progressKey(contractId, objectiveIndex), clamped);
        return clamped;
    }

    public int addProgress(Identifier contractId, int objectiveIndex, int amount, int max) {
        return setProgress(contractId, objectiveIndex, progress(contractId, objectiveIndex) + amount, max);
    }

    public void ensureContract(Object spec) {
        if (spec == null) {
            return;
        }
        try {
            Object contractId = spec.getClass().getMethod("contractId").invoke(spec);
            Object objectives = spec.getClass().getMethod("objectives").invoke(spec);
            if (!(contractId instanceof Identifier id) || !(objectives instanceof java.util.List<?> objectiveList)) {
                return;
            }
            for (int i = 0; i < objectiveList.size(); i++) {
                objectiveProgress.putIfAbsent(progressKey(id, i), 0);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional contract bootstrap data should never block player save loading.
        }
    }

    public long serviceCooldownUntil(Identifier factionId, String serviceKey) {
        return serviceCooldowns.getOrDefault(cooldownKey(factionId, serviceKey), 0L);
    }

    public void setServiceCooldown(Identifier factionId, String serviceKey, long gameTime) {
        serviceCooldowns.put(cooldownKey(factionId, serviceKey), Math.max(0L, gameTime));
    }

    public static AshfallFactionContractData get(Player player) {
        return player.getData(ModAttachments.ASHFALL_FACTION_CONTRACT_DATA.get());
    }

    public static void saveAndSync(ServerPlayer player, AshfallFactionContractData data) {
        player.setData(ModAttachments.ASHFALL_FACTION_CONTRACT_DATA.get(), data);
        player.syncData(ModAttachments.ASHFALL_FACTION_CONTRACT_DATA.get());
    }

    private static String progressKey(Identifier contractId, int objectiveIndex) {
        return contractId + "#" + objectiveIndex;
    }

    private static String cooldownKey(Identifier factionId, String serviceKey) {
        return factionId + "#" + (serviceKey == null ? "" : serviceKey);
    }

    private static void writeSync(RegistryFriendlyByteBuf buf, AshfallFactionContractData data) {
        writeStringIntMap(buf, data.objectiveProgress);
        writeStringLongMap(buf, data.serviceCooldowns);
    }

    private static AshfallFactionContractData readSync(RegistryFriendlyByteBuf buf) {
        AshfallFactionContractData data = new AshfallFactionContractData();
        readStringIntMap(buf, data.objectiveProgress);
        readStringLongMap(buf, data.serviceCooldowns);
        return data;
    }

    private static void writeStringIntMap(RegistryFriendlyByteBuf buf, Map<String, Integer> values) {
        buf.writeVarInt(values.size());
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    private static void readStringIntMap(RegistryFriendlyByteBuf buf, Map<String, Integer> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String key = buf.readUtf();
            int value = buf.readVarInt();
            if (!key.isBlank()) {
                values.put(key, value);
            }
        }
    }

    private static void writeStringLongMap(RegistryFriendlyByteBuf buf, Map<String, Long> values) {
        buf.writeVarInt(values.size());
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeLong(entry.getValue());
        }
    }

    private static void readStringLongMap(RegistryFriendlyByteBuf buf, Map<String, Long> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String key = buf.readUtf();
            long value = buf.readLong();
            if (!key.isBlank()) {
                values.put(key, value);
            }
        }
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("progressCount", objectiveProgress.size());
        int index = 0;
        for (Map.Entry<String, Integer> entry : objectiveProgress.entrySet()) {
            output.putString("progress_" + index + "_key", entry.getKey());
            output.putInt("progress_" + index + "_value", entry.getValue());
            index++;
        }
        output.putInt("cooldownCount", serviceCooldowns.size());
        index = 0;
        for (Map.Entry<String, Long> entry : serviceCooldowns.entrySet()) {
            output.putString("cooldown_" + index + "_key", entry.getKey());
            output.putLong("cooldown_" + index + "_value", entry.getValue());
            index++;
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        objectiveProgress.clear();
        int progressCount = input.getIntOr("progressCount", 0);
        for (int i = 0; i < progressCount; i++) {
            String key = input.getStringOr("progress_" + i + "_key", "");
            int value = input.getIntOr("progress_" + i + "_value", 0);
            if (!key.isBlank()) {
                objectiveProgress.put(key, value);
            }
        }
        serviceCooldowns.clear();
        int cooldownCount = input.getIntOr("cooldownCount", 0);
        for (int i = 0; i < cooldownCount; i++) {
            String key = input.getStringOr("cooldown_" + i + "_key", "");
            long value = input.getLongOr("cooldown_" + i + "_value", 0L);
            if (!key.isBlank()) {
                serviceCooldowns.put(key, value);
            }
        }
    }
}
