package com.knoxhack.echodatacore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class DataCoreWorldData extends SavedData {
    public static final String MIGRATION_V1_TEAM_VALUES = "echodatacore:team_values_v1";

    public static final Codec<DataCoreWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("version", DataCoreDataService.CURRENT_VERSION).forGetter(data -> data.version),
            Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC).optionalFieldOf("worldValues", Map.of()).forGetter(data -> data.worldValues),
            Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC).optionalFieldOf("teamValues", Map.of()).forGetter(data -> Map.of()),
            Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC).optionalFieldOf("teamValueGroups", Map.of()).forGetter(data -> data.teamValueGroups),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("migrations", Map.of()).forGetter(data -> data.migrations)
    ).apply(instance, (version, worldValues, legacyTeamValues, teamValueGroups, migrations) -> {
        DataCoreWorldData data = new DataCoreWorldData();
        data.version = Math.max(0, version);
        data.worldValues.putAll(copyMap(worldValues));
        data.teamValueGroups.putAll(copyMap(teamValueGroups));
        boolean migratedTeams = data.migrateLegacyTeamValues(legacyTeamValues);
        data.migrations.putAll(migrations);
        if (migratedTeams) {
            data.migrations.put(MIGRATION_V1_TEAM_VALUES, DataCoreDataService.CURRENT_VERSION);
        }
        if (data.version < DataCoreDataService.CURRENT_VERSION) {
            data.version = DataCoreDataService.CURRENT_VERSION;
            data.migrations.put(EchoDataCore.MODID, DataCoreDataService.CURRENT_VERSION);
        }
        return data;
    }));

    public static final SavedDataType<DataCoreWorldData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EchoDataCore.MODID, "data_world"), DataCoreWorldData::new, CODEC);

    private int version = DataCoreDataService.CURRENT_VERSION;
    private final Map<String, CompoundTag> worldValues = new LinkedHashMap<>();
    private final Map<String, CompoundTag> teamValueGroups = new LinkedHashMap<>();
    private final Map<String, Integer> migrations = new LinkedHashMap<>();

    public static DataCoreWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public int version() {
        return version;
    }

    public void ensureVersion() {
        if (version < DataCoreDataService.CURRENT_VERSION) {
            version = DataCoreDataService.CURRENT_VERSION;
            migrations.put(EchoDataCore.MODID, DataCoreDataService.CURRENT_VERSION);
            setDirty();
        }
    }

    public CompoundTag worldValue(String key) {
        CompoundTag value = worldValues.get(key);
        return value == null ? null : value.copy();
    }

    public boolean putWorldValue(String key, CompoundTag value) {
        CompoundTag safe = value == null ? new CompoundTag() : value.copy();
        CompoundTag previous = worldValues.get(key);
        if (safe.equals(previous)) {
            return false;
        }
        worldValues.put(key, safe);
        setDirty();
        return true;
    }

    public boolean removeWorldValue(String key) {
        if (worldValues.remove(key) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public CompoundTag teamValue(Identifier teamId, String key) {
        CompoundTag group = teamValueGroups.get(teamIdString(teamId));
        CompoundTag value = group == null ? null : group.getCompoundOrEmpty(key);
        return value == null ? null : value.copy();
    }

    public boolean putTeamValue(Identifier teamId, String key, CompoundTag value) {
        String teamKey = teamIdString(teamId);
        CompoundTag group = teamValueGroups.getOrDefault(teamKey, new CompoundTag()).copy();
        CompoundTag safe = value == null ? new CompoundTag() : value.copy();
        CompoundTag previous = group.getCompoundOrEmpty(key);
        if (safe.equals(previous)) {
            return false;
        }
        group.put(key, safe);
        teamValueGroups.put(teamKey, group);
        setDirty();
        return true;
    }

    public boolean removeTeamValue(Identifier teamId, String key) {
        String teamKey = teamIdString(teamId);
        CompoundTag group = teamValueGroups.get(teamKey);
        if (group == null || !group.contains(key)) {
            return false;
        }
        CompoundTag replacement = group.copy();
        replacement.remove(key);
        if (replacement.isEmpty()) {
            teamValueGroups.remove(teamKey);
        } else {
            teamValueGroups.put(teamKey, replacement);
        }
        setDirty();
        return true;
    }

    public Map<String, CompoundTag> worldSnapshot() {
        return copyMap(worldValues);
    }

    public Map<String, CompoundTag> teamSnapshot(Identifier teamId) {
        Map<String, CompoundTag> snapshot = new LinkedHashMap<>();
        CompoundTag group = teamValueGroups.get(teamIdString(teamId));
        if (group == null) {
            return snapshot;
        }
        for (String key : group.keySet()) {
            CompoundTag entry = group.getCompoundOrEmpty(key);
            if (!entry.isEmpty()) {
                snapshot.put(key, entry.copy());
            }
        }
        return snapshot;
    }

    public Map<String, Map<String, CompoundTag>> teamSnapshots() {
        Map<String, Map<String, CompoundTag>> snapshots = new LinkedHashMap<>();
        for (String teamId : teamValueGroups.keySet()) {
            Identifier id = Identifier.tryParse(teamId);
            if (id != null) {
                snapshots.put(teamId, teamSnapshot(id));
            }
        }
        return snapshots;
    }

    public Map<String, Integer> migrations() {
        return Map.copyOf(migrations);
    }

    private boolean migrateLegacyTeamValues(Map<String, CompoundTag> legacyTeamValues) {
        boolean migrated = false;
        for (Map.Entry<String, CompoundTag> entry : copyMap(legacyTeamValues).entrySet()) {
            int delimiter = entry.getKey().indexOf('|');
            if (delimiter <= 0 || delimiter >= entry.getKey().length() - 1) {
                continue;
            }
            String teamId = entry.getKey().substring(0, delimiter);
            String key = entry.getKey().substring(delimiter + 1);
            if (Identifier.tryParse(teamId) == null || Identifier.tryParse(key) == null) {
                continue;
            }
            CompoundTag group = teamValueGroups.getOrDefault(teamId, new CompoundTag()).copy();
            if (!group.contains(key)) {
                group.put(key, entry.getValue().copy());
                teamValueGroups.put(teamId, group);
                migrated = true;
            }
        }
        return migrated;
    }

    private static String teamIdString(Identifier teamId) {
        return teamId == null ? "echodatacore:unknown" : teamId.toString();
    }

    private static Map<String, CompoundTag> copyMap(Map<String, CompoundTag> source) {
        Map<String, CompoundTag> copy = new LinkedHashMap<>();
        for (Map.Entry<String, CompoundTag> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue().copy());
            }
        }
        return copy;
    }
}
