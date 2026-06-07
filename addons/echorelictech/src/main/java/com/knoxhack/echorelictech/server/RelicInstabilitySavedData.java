package com.knoxhack.echorelictech.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RelicInstabilitySavedData extends SavedData {
    public static final String DATA_NAME = "echorelictech_instability";

    public static final Codec<RelicInstabilitySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerInstability.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(RelicInstabilitySavedData::entries)
    ).apply(instance, RelicInstabilitySavedData::new));

    public static final SavedDataType<RelicInstabilitySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("echorelictech", "instability"),
            RelicInstabilitySavedData::new,
            CODEC);

    private final Map<UUID, PlayerInstability> data = new java.util.HashMap<>();

    public RelicInstabilitySavedData() {}

    private RelicInstabilitySavedData(List<PlayerInstability> entries) {
        for (PlayerInstability e : entries) {
            data.put(e.playerId, e);
        }
    }

    private List<PlayerInstability> entries() {
        return List.copyOf(data.values());
    }

    public PlayerInstability get(UUID playerId) {
        return data.computeIfAbsent(playerId, k -> new PlayerInstability(playerId));
    }

    public void set(UUID playerId, PlayerInstability inst) {
        data.put(playerId, inst);
        setDirty();
    }

    public java.util.Collection<PlayerInstability> allEntries() {
        return data.values();
    }

    public static RelicInstabilitySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static class PlayerInstability {
        public static final Codec<PlayerInstability> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("player_id").forGetter(p -> p.playerId.toString()),
                Codec.INT.optionalFieldOf("value", 0).forGetter(p -> p.value),
                Codec.INT.optionalFieldOf("level", 0).forGetter(p -> p.level),
                Codec.LONG.optionalFieldOf("last_change", 0L).forGetter(p -> p.lastChange),
                Codec.INT.optionalFieldOf("total_uses", 0).forGetter(p -> p.totalUses),
                Codec.INT.optionalFieldOf("recent_uses", 0).forGetter(p -> p.recentUses),
                Codec.INT.optionalFieldOf("recent_failures", 0).forGetter(p -> p.recentFailures),
                Codec.INT.optionalFieldOf("highest_level", 0).forGetter(p -> p.highestLevel)
        ).apply(instance, PlayerInstability::new));

        public final UUID playerId;
        public int value;
        public int level;
        public long lastChange;
        public int totalUses;
        public int recentUses;
        public int recentFailures;
        public int highestLevel;

        public PlayerInstability(UUID playerId) {
            this(playerId, 0, 0, 0, 0, 0, 0, 0);
        }

        public PlayerInstability(String playerIdStr, int value, int level, long lastChange, int totalUses, int recentUses, int recentFailures, int highestLevel) {
            this(UUID.fromString(playerIdStr), value, level, lastChange, totalUses, recentUses, recentFailures, highestLevel);
        }

        public PlayerInstability(UUID playerId, int value, int level, long lastChange, int totalUses, int recentUses, int recentFailures, int highestLevel) {
            this.playerId = playerId;
            this.value = value;
            this.level = level;
            this.lastChange = lastChange;
            this.totalUses = totalUses;
            this.recentUses = recentUses;
            this.recentFailures = recentFailures;
            this.highestLevel = highestLevel;
        }
    }
}
