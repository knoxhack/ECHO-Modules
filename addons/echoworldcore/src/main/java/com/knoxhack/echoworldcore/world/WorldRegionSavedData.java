package com.knoxhack.echoworldcore.world;

import com.echoplatform.echocore.api.WorldDiscoverySource;
import com.echoplatform.echocore.api.WorldMarker;
import com.echoplatform.echocore.api.WorldMarkerType;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WorldRegionSavedData extends SavedData {
    private static final Codec<StoredMarker> MARKER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(StoredMarker::id),
            Codec.STRING.optionalFieldOf("region", "").forGetter(StoredMarker::region),
            Codec.STRING.optionalFieldOf("type", WorldMarkerType.STRUCTURE.name()).forGetter(StoredMarker::type),
            Codec.STRING.optionalFieldOf("display", "").forGetter(StoredMarker::display),
            Codec.STRING.optionalFieldOf("summary", "").forGetter(StoredMarker::summary),
            Codec.STRING.optionalFieldOf("dimension", Level.OVERWORLD.identifier().toString()).forGetter(StoredMarker::dimension),
            Codec.INT.optionalFieldOf("x", 0).forGetter(StoredMarker::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(StoredMarker::y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(StoredMarker::z),
            Codec.INT.optionalFieldOf("radius", 32).forGetter(StoredMarker::radius),
            Codec.BOOL.optionalFieldOf("discovered", false).forGetter(StoredMarker::discovered),
            Codec.LONG.optionalFieldOf("updated", 0L).forGetter(StoredMarker::updated)
    ).apply(instance, StoredMarker::new));

    private static final Codec<StoredDiscovery> DISCOVERY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("player").forGetter(StoredDiscovery::player),
            Codec.STRING.fieldOf("region").forGetter(StoredDiscovery::region),
            Codec.STRING.optionalFieldOf("source", WorldDiscoverySource.INTEGRATION.name()).forGetter(StoredDiscovery::source),
            Codec.INT.optionalFieldOf("x", 0).forGetter(StoredDiscovery::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(StoredDiscovery::y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(StoredDiscovery::z),
            Codec.LONG.optionalFieldOf("gameTime", 0L).forGetter(StoredDiscovery::gameTime)
    ).apply(instance, StoredDiscovery::new));

    private static final Codec<StoredHazardExposure> HAZARD_EXPOSURE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("player").forGetter(StoredHazardExposure::player),
            Codec.STRING.fieldOf("hazard").forGetter(StoredHazardExposure::hazard),
            Codec.STRING.optionalFieldOf("statusEffect", "").forGetter(StoredHazardExposure::statusEffect),
            Codec.STRING.optionalFieldOf("saveKey", "").forGetter(StoredHazardExposure::saveKey),
            Codec.INT.optionalFieldOf("durationTicks", 0).forGetter(StoredHazardExposure::durationTicks),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(StoredHazardExposure::amplifier),
            Codec.FLOAT.optionalFieldOf("damage", 0.0F).forGetter(StoredHazardExposure::damage),
            Codec.LONG.optionalFieldOf("gameTime", 0L).forGetter(StoredHazardExposure::gameTime)
    ).apply(instance, StoredHazardExposure::new));

    public static final Codec<WorldRegionSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MARKER_CODEC.listOf().optionalFieldOf("markers", List.of()).forGetter(WorldRegionSavedData::storedMarkers),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("discoveries", Map.of())
                    .forGetter(data -> data.discoveryRows),
            DISCOVERY_CODEC.listOf().optionalFieldOf("structuredDiscoveries", List.of())
                    .forGetter(WorldRegionSavedData::storedDiscoveries),
            HAZARD_EXPOSURE_CODEC.listOf().optionalFieldOf("hazardExposures", List.of())
                    .forGetter(WorldRegionSavedData::storedHazardExposures)
    ).apply(instance, WorldRegionSavedData::new));

    public static final SavedDataType<WorldRegionSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, "world_regions"),
            WorldRegionSavedData::new,
            CODEC);

    private final Map<String, WorldMarker> markers = new LinkedHashMap<>();
    private final Map<String, String> discoveryRows = new LinkedHashMap<>();
    private final Map<String, StoredDiscovery> discoveries = new LinkedHashMap<>();
    private final Map<String, StoredHazardExposure> hazardExposures = new LinkedHashMap<>();

    public WorldRegionSavedData() {
    }

    private WorldRegionSavedData(List<StoredMarker> markers, Map<String, String> discoveryRows,
            List<StoredDiscovery> discoveries, List<StoredHazardExposure> hazardExposures) {
        for (StoredMarker stored : markers) {
            WorldMarker marker = stored.toMarker();
            if (marker != null) {
                this.markers.put(marker.id().toString(), marker);
            }
        }
        this.discoveryRows.putAll(discoveryRows);
        for (StoredDiscovery discovery : discoveries) {
            if (discovery.valid()) {
                this.discoveries.put(discovery.key(), discovery);
            }
        }
        for (StoredHazardExposure exposure : hazardExposures) {
            if (exposure.valid()) {
                this.hazardExposures.put(exposure.key(), exposure);
            }
        }
        migrateLegacyDiscoveryRows();
    }

    public static WorldRegionSavedData get(ServerLevel level) {
        ServerLevel storageLevel = level.getServer().overworld();
        if (storageLevel == null) {
            storageLevel = level;
        }
        return storageLevel.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<WorldMarker> markers() {
        return List.copyOf(markers.values());
    }

    public java.util.Optional<WorldMarker> marker(Identifier id) {
        return java.util.Optional.ofNullable(id == null ? null : markers.get(id.toString()));
    }

    public boolean saveMarker(WorldMarker marker) {
        if (marker == null) {
            return false;
        }
        WorldMarker previous = markers.put(marker.id().toString(), marker);
        boolean changed = !marker.equals(previous);
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean hasMarker(WorldMarker marker) {
        return marker != null && marker.equals(markers.get(marker.id().toString()));
    }

    public void recordDiscovery(UUID playerId, Identifier regionId, WorldDiscoverySource source, BlockPos pos) {
        recordDiscovery(playerId, regionId, source, pos, 0L);
    }

    public void recordDiscovery(UUID playerId, Identifier regionId, WorldDiscoverySource source, BlockPos pos, long gameTime) {
        if (playerId == null || regionId == null) {
            return;
        }
        StoredDiscovery discovery = StoredDiscovery.from(playerId, regionId,
                source == null ? WorldDiscoverySource.INTEGRATION : source, pos, gameTime);
        StoredDiscovery previous = discoveries.put(discovery.key(), discovery);
        Set<String> ids = new LinkedHashSet<>(readDiscoveryRow(playerId.toString()));
        boolean rowChanged = ids.add(regionId.toString());
        if (rowChanged) {
            discoveryRows.put(playerId.toString(), String.join(",", ids));
        }
        if (!discovery.equals(previous) || rowChanged) {
            setDirty();
        }
    }

    public Set<Identifier> discoveries(UUID playerId) {
        if (playerId == null) {
            return Set.of();
        }
        LinkedHashSet<Identifier> ids = new LinkedHashSet<>();
        for (StoredDiscovery discovery : discoveries.values()) {
            if (playerId.toString().equals(discovery.player())) {
                Identifier id = Identifier.tryParse(discovery.region());
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return Set.copyOf(ids);
    }

    public void recordHazardExposure(UUID playerId, Identifier hazardId,
            Identifier statusEffectId, float damage, long gameTime) {
        recordHazardExposure(playerId, hazardId, statusEffectId, "", 0, 0, damage, gameTime);
    }

    public void recordHazardExposure(UUID playerId, Identifier hazardId,
            Identifier statusEffectId, String saveKey, int durationTicks, int amplifier, float damage, long gameTime) {
        if (playerId == null || hazardId == null) {
            return;
        }
        StoredHazardExposure exposure = StoredHazardExposure.from(
                playerId,
                hazardId,
                statusEffectId,
                saveKey,
                durationTicks,
                amplifier,
                damage,
                gameTime);
        StoredHazardExposure previous = hazardExposures.put(exposure.key(), exposure);
        if (!exposure.equals(previous)) {
            setDirty();
        }
    }

    public List<Identifier> hazardExposures(UUID playerId) {
        if (playerId == null) {
            return List.of();
        }
        return hazardExposures.values().stream()
                .filter(exposure -> playerId.toString().equals(exposure.player()))
                .map(StoredHazardExposure::hazard)
                .map(Identifier::tryParse)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<Identifier> hazardExposureStatusEffects(UUID playerId) {
        if (playerId == null) {
            return List.of();
        }
        return hazardExposures.values().stream()
                .filter(exposure -> playerId.toString().equals(exposure.player()))
                .map(StoredHazardExposure::statusEffect)
                .map(Identifier::tryParse)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public Map<String, Object> hazardExposureStatusState(UUID playerId, Identifier hazardId) {
        if (playerId == null || hazardId == null) {
            return Map.of();
        }
        StoredHazardExposure exposure = hazardExposures.get(playerId + "|" + hazardId);
        if (exposure == null || exposure.saveKey().isBlank()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("effectId", exposure.statusEffect());
        payload.put("durationTicks", exposure.durationTicks());
        payload.put("amplifier", exposure.amplifier());
        payload.put("hazardId", exposure.hazard());
        payload.put("damageApplied", exposure.damage());
        payload.put("gameTick", exposure.gameTime());
        LinkedHashMap<String, Object> savedStatus = new LinkedHashMap<>();
        savedStatus.put(exposure.saveKey(), Map.copyOf(payload));
        savedStatus.put("adapterCoreModule", EchoWorldCore.MODID);
        return Map.copyOf(savedStatus);
    }

    public String hazardExposureSaveKey(UUID playerId, Identifier hazardId) {
        if (playerId == null || hazardId == null) {
            return "";
        }
        StoredHazardExposure exposure = hazardExposures.get(playerId + "|" + hazardId);
        return exposure == null ? "" : exposure.saveKey();
    }

    private List<String> readDiscoveryRow(String key) {
        String raw = discoveryRows.getOrDefault(key, "");
        if (raw.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : raw.split(",")) {
            String value = part.strip();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private List<StoredMarker> storedMarkers() {
        return markers.values().stream().map(StoredMarker::from).toList();
    }

    private List<StoredDiscovery> storedDiscoveries() {
        return List.copyOf(discoveries.values());
    }

    private List<StoredHazardExposure> storedHazardExposures() {
        return List.copyOf(hazardExposures.values());
    }

    private void migrateLegacyDiscoveryRows() {
        boolean migrated = false;
        for (Map.Entry<String, String> entry : discoveryRows.entrySet()) {
            UUID playerId;
            try {
                playerId = UUID.fromString(entry.getKey());
            } catch (IllegalArgumentException exception) {
                continue;
            }
            for (String value : readDiscoveryRow(entry.getKey())) {
                Identifier regionId = Identifier.tryParse(value);
                if (regionId == null) {
                    continue;
                }
                StoredDiscovery discovery = StoredDiscovery.from(playerId, regionId,
                        WorldDiscoverySource.INTEGRATION, BlockPos.ZERO, 0L);
                if (discoveries.putIfAbsent(discovery.key(), discovery) == null) {
                    migrated = true;
                }
            }
        }
        if (migrated) {
            setDirty();
        }
    }

    private record StoredMarker(String id, String region, String type, String display, String summary,
            String dimension, int x, int y, int z, int radius, boolean discovered, long updated) {
        private static StoredMarker from(WorldMarker marker) {
            return new StoredMarker(
                    marker.id().toString(),
                    marker.regionId() == null ? "" : marker.regionId().toString(),
                    marker.type().name(),
                    marker.displayName(),
                    marker.summary(),
                    marker.dimension().identifier().toString(),
                    marker.pos().getX(),
                    marker.pos().getY(),
                    marker.pos().getZ(),
                    marker.radius(),
                    marker.discovered(),
                    marker.updatedGameTime());
        }

        private WorldMarker toMarker() {
            Identifier id = Identifier.tryParse(this.id);
            if (id == null) {
                return null;
            }
            Identifier regionId = region == null || region.isBlank() ? null : Identifier.tryParse(region);
            WorldMarkerType markerType;
            try {
                markerType = WorldMarkerType.valueOf(type);
            } catch (IllegalArgumentException | NullPointerException exception) {
                markerType = WorldMarkerType.STRUCTURE;
            }
            Identifier dimensionId = Identifier.tryParse(dimension);
            ResourceKey<Level> dimensionKey = dimensionId == null
                    ? Level.OVERWORLD
                    : ResourceKey.create(Registries.DIMENSION, dimensionId);
            return new WorldMarker(id, regionId, markerType, display, summary, dimensionKey,
                    new BlockPos(x, y, z), radius, discovered, updated);
        }
    }

    private record StoredDiscovery(String player, String region, String source,
            int x, int y, int z, long gameTime) {
        private static StoredDiscovery from(UUID playerId, Identifier regionId,
                WorldDiscoverySource source, BlockPos pos, long gameTime) {
            BlockPos safePos = pos == null ? BlockPos.ZERO : pos;
            WorldDiscoverySource safeSource = source == null ? WorldDiscoverySource.INTEGRATION : source;
            return new StoredDiscovery(playerId.toString(), regionId.toString(), safeSource.name(),
                    safePos.getX(), safePos.getY(), safePos.getZ(), Math.max(0L, gameTime));
        }

        private boolean valid() {
            try {
                UUID.fromString(player);
            } catch (IllegalArgumentException | NullPointerException exception) {
                return false;
            }
            return Identifier.tryParse(region) != null;
        }

        private String key() {
            return player + "|" + region;
        }
    }

    private record StoredHazardExposure(String player, String hazard,
            String statusEffect, String saveKey, int durationTicks, int amplifier, float damage, long gameTime) {
        private static StoredHazardExposure from(UUID playerId, Identifier hazardId,
                Identifier statusEffectId, String saveKey, int durationTicks, int amplifier, float damage, long gameTime) {
            return new StoredHazardExposure(
                    playerId.toString(),
                    hazardId.toString(),
                    statusEffectId == null ? "" : statusEffectId.toString(),
                    saveKey == null ? "" : saveKey,
                    Math.max(0, durationTicks),
                    Math.max(0, amplifier),
                    Math.max(0.0F, damage),
                    Math.max(0L, gameTime));
        }

        private boolean valid() {
            try {
                UUID.fromString(player);
            } catch (IllegalArgumentException | NullPointerException exception) {
                return false;
            }
            return Identifier.tryParse(hazard) != null;
        }

        private String key() {
            return player + "|" + hazard;
        }
    }
}
