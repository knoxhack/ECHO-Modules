package com.knoxhack.echoplayercore.data;

import com.knoxhack.echoplayercore.EchoPlayerCore;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class PlayerCoreSavedData extends SavedData {
    public static final Codec<PlayerCoreSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlayerDataEntry.CODEC.listOf().optionalFieldOf("players", List.of())
                    .forGetter(PlayerCoreSavedData::storedEntries)
    ).apply(instance, PlayerCoreSavedData::fromCodec));

    public static final SavedDataType<PlayerCoreSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EchoPlayerCore.MODID, "player_travel_data"),
            PlayerCoreSavedData::new,
            CODEC
    );

    private final Map<UUID, PlayerTravelData> data = new LinkedHashMap<>();

    public PlayerCoreSavedData() {
    }

    private PlayerCoreSavedData(List<PlayerDataEntry> entries) {
        for (PlayerDataEntry entry : entries) {
            if (entry == null || entry.playerId == null) {
                continue;
            }
            PlayerTravelData travel = new PlayerTravelData(entry.playerId);
            if (entry.homes != null) {
                for (HomeLocation home : entry.homes) {
                    if (home != null) {
                        travel.setHome(home);
                    }
                }
            }
            entry.lastBack.ifPresent(travel::setLastBackLocation);
            entry.lastDeath.ifPresent(travel::setLastDeathLocation);
            entry.lastRtp.ifPresent(travel::setLastRtpLocation);
            if (entry.cooldowns != null) {
                for (Map.Entry<String, Long> c : entry.cooldowns.entrySet()) {
                    if (c.getKey() != null && c.getValue() != null) {
                        travel.setCooldown(c.getKey(), c.getValue());
                    }
                }
            }
            data.put(entry.playerId, travel);
        }
    }

    public static PlayerCoreSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public PlayerTravelData getOrCreate(UUID playerId) {
        return data.computeIfAbsent(playerId, PlayerTravelData::new);
    }

    public Optional<PlayerTravelData> get(UUID playerId) {
        return Optional.ofNullable(data.get(playerId));
    }

    public void markDirty() {
        setDirty();
    }

    public int playerCount() {
        return data.size();
    }

    private List<PlayerDataEntry> storedEntries() {
        return data.values().stream().map(PlayerDataEntry::from).toList();
    }

    private static PlayerCoreSavedData fromCodec(List<PlayerDataEntry> entries) {
        return new PlayerCoreSavedData(entries);
    }

    private record PlayerDataEntry(
            UUID playerId,
            List<HomeLocation> homes,
            Optional<TeleportLocation> lastBack,
            Optional<TeleportLocation> lastDeath,
            Optional<TeleportLocation> lastRtp,
            Map<String, Long> cooldowns) {

        public static final Codec<PlayerDataEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("playerId").forGetter(e -> e.playerId.toString()),
                HomeLocation.CODEC.listOf().optionalFieldOf("homes", List.of()).forGetter(PlayerDataEntry::homes),
                TeleportLocation.CODEC.optionalFieldOf("lastBack").forGetter(PlayerDataEntry::lastBack),
                TeleportLocation.CODEC.optionalFieldOf("lastDeath").forGetter(PlayerDataEntry::lastDeath),
                TeleportLocation.CODEC.optionalFieldOf("lastRtp").forGetter(PlayerDataEntry::lastRtp),
                Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("cooldowns", Map.of())
                        .forGetter(PlayerDataEntry::cooldowns)
        ).apply(instance, PlayerDataEntry::fromCodec));

        static PlayerDataEntry from(PlayerTravelData travel) {
            return new PlayerDataEntry(
                    travel.playerId(),
                    List.copyOf(travel.homes().values()),
                    travel.lastBackLocation(),
                    travel.lastDeathLocation(),
                    travel.lastRtpLocation(),
                    new LinkedHashMap<>(travel.cooldowns())
            );
        }

        static PlayerDataEntry fromCodec(String playerIdStr, List<HomeLocation> homes,
                                         Optional<TeleportLocation> lastBack, Optional<TeleportLocation> lastDeath,
                                         Optional<TeleportLocation> lastRtp, Map<String, Long> cooldowns) {
            UUID id;
            try {
                id = UUID.fromString(playerIdStr);
            } catch (IllegalArgumentException e) {
                id = new UUID(0L, 0L);
            }
            return new PlayerDataEntry(id, homes, lastBack, lastDeath, lastRtp, cooldowns);
        }
    }
}
