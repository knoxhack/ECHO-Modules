package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

/**
 * Tracks identifier-keyed faction influence over biomes, chunks, and contact sites.
 */
public class FactionTerritory implements EchoValueIOSerializable {
    public static final StreamCodec<RegistryFriendlyByteBuf, FactionTerritory> STREAM_CODEC = StreamCodec.of(
            FactionTerritory::writeSync,
            FactionTerritory::readSync
    );

    public static final int CONTESTED_THRESHOLD = 30;
    public static final int DOMINANT_THRESHOLD = 60;
    public static final int CONTROLLED_THRESHOLD = 80;

    private final Map<Identifier, Map<String, Integer>> biomeInfluence = new HashMap<>();
    private final Map<String, Identifier> chunkOwnership = new HashMap<>();
    private final List<VillageControl> factionVillages = new ArrayList<>();

    public FactionTerritory() {
        for (Identifier factionId : AshfallFactionMap.all()) {
            biomeInfluence.put(factionId, new HashMap<>());
        }
    }

    public enum TerritoryStatus {
        UNCLAIMED("Unclaimed", 0xFF555555),
        CONTESTED("Contested", 0xFFFFA94D),
        DOMINANT("Dominant", 0xFF8A9BB0),
        CONTROLLED("Controlled", 0xFF42D67E);

        private final String displayName;
        private final int mapColor;

        TerritoryStatus(String displayName, int mapColor) {
            this.displayName = displayName;
            this.mapColor = mapColor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getMapColor() {
            return mapColor;
        }
    }

    public int getBiomeInfluence(Identifier faction, String biomeKey) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(faction);
        return biomeInfluence.getOrDefault(canonical, Collections.emptyMap())
                .getOrDefault(normalizeBiome(biomeKey), 0);
    }

    public void setBiomeInfluence(Identifier faction, String biomeKey, int value) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(faction);
        if (canonical == null) {
            return;
        }
        biomeInfluence.computeIfAbsent(canonical, key -> new HashMap<>())
                .put(normalizeBiome(biomeKey), clamp(value));
    }

    public void modifyBiomeInfluence(Identifier faction, String biomeKey, int amount) {
        setBiomeInfluence(faction, biomeKey, getBiomeInfluence(faction, biomeKey) + amount);
    }

    public Identifier getDominantFaction(String biomeKey) {
        Identifier dominant = null;
        int maxInfluence = 0;
        for (Identifier factionId : AshfallFactionMap.all()) {
            int influence = getBiomeInfluence(factionId, biomeKey);
            if (influence > maxInfluence) {
                maxInfluence = influence;
                dominant = factionId;
            }
        }
        return dominant;
    }

    public TerritoryStatus getBiomeStatus(String biomeKey) {
        Identifier dominant = getDominantFaction(biomeKey);
        if (dominant == null) {
            return TerritoryStatus.UNCLAIMED;
        }
        int influence = getBiomeInfluence(dominant, biomeKey);
        if (influence >= CONTROLLED_THRESHOLD) {
            return TerritoryStatus.CONTROLLED;
        }
        if (influence >= DOMINANT_THRESHOLD) {
            return TerritoryStatus.DOMINANT;
        }
        if (influence >= CONTESTED_THRESHOLD) {
            return TerritoryStatus.CONTESTED;
        }
        return TerritoryStatus.UNCLAIMED;
    }

    public boolean isBiomeContested(String biomeKey) {
        int factionsWithInfluence = 0;
        for (Identifier factionId : AshfallFactionMap.all()) {
            if (getBiomeInfluence(factionId, biomeKey) >= CONTESTED_THRESHOLD) {
                factionsWithInfluence++;
            }
        }
        return factionsWithInfluence >= 2;
    }

    public List<String> getFactionBiomes(Identifier faction, TerritoryStatus minStatus) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(faction);
        List<String> biomes = new ArrayList<>();
        int threshold = switch (minStatus) {
            case UNCLAIMED -> 0;
            case CONTESTED -> CONTESTED_THRESHOLD;
            case DOMINANT -> DOMINANT_THRESHOLD;
            case CONTROLLED -> CONTROLLED_THRESHOLD;
        };
        for (Map.Entry<String, Integer> entry : biomeInfluence.getOrDefault(canonical, Collections.emptyMap()).entrySet()) {
            if (entry.getValue() >= threshold) {
                biomes.add(entry.getKey());
            }
        }
        return biomes;
    }

    public void setChunkOwner(ChunkPos pos, Level level, Identifier faction) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(faction);
        if (pos != null && level != null && canonical != null) {
            chunkOwnership.put(chunkKey(pos, level), canonical);
        }
    }

    public Identifier getChunkOwner(ChunkPos pos, Level level) {
        if (pos == null || level == null) {
            return null;
        }
        return chunkOwnership.get(chunkKey(pos, level));
    }

    public void addVillage(BlockPos center, Identifier controllingFaction, String villageName) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(controllingFaction);
        if (center != null && canonical != null) {
            factionVillages.add(new VillageControl(center, canonical, villageName, System.currentTimeMillis()));
        }
    }

    public List<VillageControl> getFactionVillages(Identifier faction) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(faction);
        return factionVillages.stream()
                .filter(village -> village.controllingFaction.equals(canonical))
                .toList();
    }

    public List<VillageControl> getAllVillages() {
        return List.copyOf(factionVillages);
    }

    public boolean transferVillageControl(BlockPos villageCenter, Identifier newFaction) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(newFaction);
        if (canonical == null) {
            return false;
        }
        for (VillageControl village : factionVillages) {
            if (village.center.closerThan(villageCenter, 16.0D)) {
                village.controllingFaction = canonical;
                village.captureTime = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    public VillageControl getNearestVillage(BlockPos pos, Identifier faction) {
        Identifier canonical = AshfallFactionMap.canonicalFaction(faction);
        VillageControl nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (VillageControl village : factionVillages) {
            if (canonical != null && !village.controllingFaction.equals(canonical)) {
                continue;
            }
            double distance = village.center.distSqr(pos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = village;
            }
        }
        return nearest;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt("influenceFactionCount", biomeInfluence.size());
        int factionIndex = 0;
        for (Map.Entry<Identifier, Map<String, Integer>> factionEntry : biomeInfluence.entrySet()) {
            output.putString("influence_" + factionIndex + "_faction", factionEntry.getKey().toString());
            output.putInt("influence_" + factionIndex + "_count", factionEntry.getValue().size());
            int biomeIndex = 0;
            for (Map.Entry<String, Integer> biomeEntry : factionEntry.getValue().entrySet()) {
                output.putString("influence_" + factionIndex + "_" + biomeIndex + "_biome", biomeEntry.getKey());
                output.putInt("influence_" + factionIndex + "_" + biomeIndex + "_value", biomeEntry.getValue());
                biomeIndex++;
            }
            factionIndex++;
        }
        output.putInt("chunkCount", chunkOwnership.size());
        int index = 0;
        for (Map.Entry<String, Identifier> entry : chunkOwnership.entrySet()) {
            output.putString("chunk_" + index + "_key", entry.getKey());
            output.putString("chunk_" + index + "_faction", entry.getValue().toString());
            index++;
        }
        output.putInt("villageCount", factionVillages.size());
        for (int i = 0; i < factionVillages.size(); i++) {
            VillageControl village = factionVillages.get(i);
            output.putInt("village_" + i + "_x", village.center.getX());
            output.putInt("village_" + i + "_y", village.center.getY());
            output.putInt("village_" + i + "_z", village.center.getZ());
            output.putString("village_" + i + "_faction", village.controllingFaction.toString());
            output.putString("village_" + i + "_name", village.name);
            output.putLong("village_" + i + "_time", village.captureTime);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        biomeInfluence.clear();
        for (Identifier factionId : AshfallFactionMap.all()) {
            biomeInfluence.put(factionId, new HashMap<>());
        }

        int factionCount = input.getIntOr("influenceFactionCount", -1);
        if (factionCount >= 0) {
            for (int i = 0; i < factionCount; i++) {
                Identifier faction = parseFaction(input.getStringOr("influence_" + i + "_faction", ""));
                int count = input.getIntOr("influence_" + i + "_count", 0);
                if (faction == null) {
                    continue;
                }
                Map<String, Integer> influence = biomeInfluence.computeIfAbsent(faction, key -> new HashMap<>());
                for (int j = 0; j < count; j++) {
                    String biome = normalizeBiome(input.getStringOr("influence_" + i + "_" + j + "_biome", ""));
                    if (!biome.isBlank()) {
                        influence.put(biome, clamp(input.getIntOr("influence_" + i + "_" + j + "_value", 0)));
                    }
                }
            }
        }

        chunkOwnership.clear();
        int chunkCount = input.getIntOr("chunkCount", 0);
        for (int i = 0; i < chunkCount; i++) {
            Identifier faction = parseFaction(input.getStringOr("chunk_" + i + "_faction", ""));
            String key = input.getStringOr("chunk_" + i + "_key", "");
            if (faction != null && !key.isBlank()) {
                chunkOwnership.put(key, faction);
            }
        }

        factionVillages.clear();
        int villageCount = input.getIntOr("villageCount", 0);
        for (int i = 0; i < villageCount; i++) {
            Identifier faction = parseFaction(input.getStringOr("village_" + i + "_faction", ""));
            if (faction != null) {
                factionVillages.add(new VillageControl(
                        new BlockPos(input.getIntOr("village_" + i + "_x", 0),
                                input.getIntOr("village_" + i + "_y", 64),
                                input.getIntOr("village_" + i + "_z", 0)),
                        faction,
                        input.getStringOr("village_" + i + "_name", "Faction Contact"),
                        input.getLongOr("village_" + i + "_time", System.currentTimeMillis())));
            }
        }
    }

    public static FactionTerritory get(net.minecraft.world.entity.player.Player player) {
        return player.getData(ModAttachments.FACTION_TERRITORY.get());
    }

    public static void saveAndSync(ServerPlayer player, FactionTerritory territory) {
        player.setData(ModAttachments.FACTION_TERRITORY.get(), territory);
        player.syncData(ModAttachments.FACTION_TERRITORY.get());
    }

    public static void syncToClient(ServerPlayer player) {
        player.syncData(ModAttachments.FACTION_TERRITORY.get());
    }

    public static class VillageControl {
        public final BlockPos center;
        public Identifier controllingFaction;
        public final String name;
        public long captureTime;

        public VillageControl(BlockPos center, Identifier faction, String name, long captureTime) {
            this.center = center;
            this.controllingFaction = faction;
            this.name = name == null || name.isBlank() ? AshfallFactionMap.shortName(faction) + " Contact" : name;
            this.captureTime = captureTime;
        }
    }

    private static void writeSync(RegistryFriendlyByteBuf buf, FactionTerritory data) {
        buf.writeVarInt(data.biomeInfluence.size());
        for (Map.Entry<Identifier, Map<String, Integer>> factionEntry : data.biomeInfluence.entrySet()) {
            buf.writeUtf(factionEntry.getKey().toString());
            buf.writeVarInt(factionEntry.getValue().size());
            for (Map.Entry<String, Integer> biomeEntry : factionEntry.getValue().entrySet()) {
                buf.writeUtf(biomeEntry.getKey());
                buf.writeVarInt(biomeEntry.getValue());
            }
        }
        buf.writeVarInt(data.chunkOwnership.size());
        for (Map.Entry<String, Identifier> entry : data.chunkOwnership.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue().toString());
        }
        buf.writeVarInt(data.factionVillages.size());
        for (VillageControl village : data.factionVillages) {
            buf.writeBlockPos(village.center);
            buf.writeUtf(village.controllingFaction.toString());
            buf.writeUtf(village.name);
            buf.writeLong(village.captureTime);
        }
    }

    private static FactionTerritory readSync(RegistryFriendlyByteBuf buf) {
        FactionTerritory data = new FactionTerritory();
        data.biomeInfluence.clear();
        int factionCount = buf.readVarInt();
        for (int i = 0; i < factionCount; i++) {
            Identifier faction = parseFaction(buf.readUtf());
            Map<String, Integer> influence = new HashMap<>();
            int count = buf.readVarInt();
            for (int j = 0; j < count; j++) {
                influence.put(normalizeBiome(buf.readUtf()), clamp(buf.readVarInt()));
            }
            if (faction != null) {
                data.biomeInfluence.put(faction, influence);
            }
        }
        data.chunkOwnership.clear();
        int chunkCount = buf.readVarInt();
        for (int i = 0; i < chunkCount; i++) {
            String key = buf.readUtf();
            Identifier faction = parseFaction(buf.readUtf());
            if (faction != null) {
                data.chunkOwnership.put(key, faction);
            }
        }
        data.factionVillages.clear();
        int villageCount = buf.readVarInt();
        for (int i = 0; i < villageCount; i++) {
            BlockPos center = buf.readBlockPos();
            Identifier faction = parseFaction(buf.readUtf());
            String name = buf.readUtf();
            long time = buf.readLong();
            if (faction != null) {
                data.factionVillages.add(new VillageControl(center, faction, name, time));
            }
        }
        return data;
    }

    private static String chunkKey(ChunkPos pos, Level level) {
        ResourceKey<Level> dimension = level.dimension();
        return dimension.identifier() + ":" + pos.x() + "," + pos.z();
    }

    private static Identifier parseFaction(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return AshfallFactionMap.resolveFactionId(value);
    }

    private static String normalizeBiome(String biomeKey) {
        return biomeKey == null ? "" : biomeKey.toLowerCase(java.util.Locale.ROOT).trim();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
