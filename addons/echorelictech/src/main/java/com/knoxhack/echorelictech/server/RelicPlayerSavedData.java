package com.knoxhack.echorelictech.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class RelicPlayerSavedData extends SavedData {
    public static final String DATA_NAME = "echorelictech_player_data";

    public static final Codec<RelicPlayerSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerEchoData.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(RelicPlayerSavedData::entries)
    ).apply(instance, RelicPlayerSavedData::new));

    public static final SavedDataType<RelicPlayerSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("echorelictech", "player_data"),
            RelicPlayerSavedData::new,
            CODEC);

    private final Map<UUID, PlayerEchoData> data = new HashMap<>();

    public RelicPlayerSavedData() {}

    private RelicPlayerSavedData(List<PlayerEchoData> entries) {
        for (PlayerEchoData e : entries) {
            if (e.playerId != null) data.put(e.playerId, e);
        }
    }

    private List<PlayerEchoData> entries() {
        return List.copyOf(data.values());
    }

    public PlayerEchoData get(UUID playerId) {
        return data.computeIfAbsent(playerId, k -> new PlayerEchoData(playerId));
    }

    public void set(UUID playerId, PlayerEchoData inst) {
        data.put(playerId, inst);
        setDirty();
    }

    public static RelicPlayerSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static class PlayerEchoData {
        public static final Codec<PlayerEchoData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("player_id").forGetter(p -> p.playerId.toString()),
                Codec.INT.optionalFieldOf("analyzed_count", 0).forGetter(p -> p.analyzedCount),
                Codec.INT.optionalFieldOf("total_uses", 0).forGetter(p -> p.totalUses),
                Codec.BOOL.optionalFieldOf("first_vault_discovered", false).forGetter(p -> p.firstVaultDiscovered),
                Codec.STRING.listOf().optionalFieldOf("discovered_relics", List.of()).forGetter(p -> new ArrayList<>(p.discoveredRelics)),
                Codec.STRING.listOf().optionalFieldOf("discovered_vault_ids", List.of()).forGetter(p -> new ArrayList<>(p.discoveredVaultIds)),
                BlockPos.CODEC.listOf().optionalFieldOf("discovered_vaults", List.of()).forGetter(p -> new ArrayList<>(p.discoveredVaults))
        ).apply(instance, PlayerEchoData::new));

        public UUID playerId;
        public int analyzedCount;
        public int totalUses;
        public boolean firstVaultDiscovered;
        public Set<String> discoveredRelics = new HashSet<>();
        public List<String> discoveredVaultIds = new ArrayList<>();
        public List<BlockPos> discoveredVaults = new ArrayList<>();

        public PlayerEchoData(UUID playerId) {
            this.playerId = playerId;
        }

        public PlayerEchoData(String playerIdStr, int analyzedCount, int totalUses, boolean firstVaultDiscovered, List<String> discoveredRelics, List<String> discoveredVaultIds, List<BlockPos> discoveredVaults) {
            this.playerId = UUID.fromString(playerIdStr);
            this.analyzedCount = analyzedCount;
            this.totalUses = totalUses;
            this.firstVaultDiscovered = firstVaultDiscovered;
            this.discoveredRelics.addAll(discoveredRelics);
            this.discoveredVaultIds.addAll(discoveredVaultIds);
            this.discoveredVaults.addAll(discoveredVaults);
            while (this.discoveredVaultIds.size() < this.discoveredVaults.size()) {
                this.discoveredVaultIds.add("echorelictech:pre_gridfall_research_vault");
            }
        }
    }
}
