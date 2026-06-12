package com.knoxhack.echomissioncore.storage;

import com.echoplatform.echocore.api.mission.MissionStatus;
import com.knoxhack.echomissioncore.EchoMissionCore;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class MissionPlayerData {
    public static final StreamCodec<RegistryFriendlyByteBuf, MissionPlayerData> STREAM_CODEC =
            StreamCodec.of(MissionPlayerData::writeSync, MissionPlayerData::readSync);
    private static final Map<UUID, MissionPlayerData> PLAYER_DATA = new ConcurrentHashMap<>();

    private final Map<String, MissionState> missions = new LinkedHashMap<>();
    private final Set<String> migratedSources = new HashSet<>();
    private final Set<String> unlockedChapters = new HashSet<>();
    private String trackedMissionId = "";
    private int schemaVersion = 1;

    public MissionState state(Identifier missionId) {
        String key = normalize(missionId);
        return missions.computeIfAbsent(key, ignored -> new MissionState());
    }

    public MissionState stateIfPresent(Identifier missionId) {
        return missions.get(normalize(missionId));
    }

    public Map<String, MissionState> missionStates() {
        return Collections.unmodifiableMap(missions);
    }

    public boolean hasMigrated(String source) {
        return source != null && migratedSources.contains(source);
    }

    public void markMigrated(String source) {
        if (source != null && !source.isBlank()) {
            migratedSources.add(source);
        }
    }

    public boolean hasUnlockedChapter(Identifier chapterId) {
        return unlockedChapters.contains(normalize(chapterId));
    }

    public boolean markUnlockedChapter(Identifier chapterId) {
        String key = normalize(chapterId);
        return !key.isBlank() && unlockedChapters.add(key);
    }

    public String trackedMissionId() {
        return trackedMissionId;
    }

    public void trackMission(Identifier missionId) {
        trackedMissionId = missionId == null ? "" : missionId.toString();
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public static MissionPlayerData get(Player player) {
        return PLAYER_DATA.computeIfAbsent(player.getUUID(), ignored -> new MissionPlayerData());
    }

    public static void saveAndSync(ServerPlayer player, MissionPlayerData data) {
        PLAYER_DATA.put(player.getUUID(), data == null ? new MissionPlayerData() : data);
        EchoMissionCore.LOGGER.trace("MissionCore player data saved for {} through native attachment store.", player.getUUID());
    }

    private static String normalize(Identifier id) {
        return id == null ? "" : id.toString();
    }

    private static void writeSync(RegistryFriendlyByteBuf buf, MissionPlayerData data) {
        buf.writeVarInt(data.schemaVersion);
        buf.writeUtf(data.trackedMissionId);
        writeStringSet(buf, data.migratedSources);
        writeStringSet(buf, data.unlockedChapters);
        buf.writeVarInt(data.missions.size());
        for (Map.Entry<String, MissionState> entry : data.missions.entrySet()) {
            buf.writeUtf(entry.getKey());
            entry.getValue().write(buf);
        }
    }

    private static MissionPlayerData readSync(RegistryFriendlyByteBuf buf) {
        MissionPlayerData data = new MissionPlayerData();
        data.schemaVersion = buf.readVarInt();
        data.trackedMissionId = buf.readUtf();
        readStringSet(buf, data.migratedSources);
        readStringSet(buf, data.unlockedChapters);
        data.missions.clear();
        int missionCount = buf.readVarInt();
        for (int i = 0; i < missionCount; i++) {
            data.missions.put(buf.readUtf(), MissionState.read(buf));
        }
        return data;
    }

    private static void writeStringSet(RegistryFriendlyByteBuf buf, Set<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value);
        }
    }

    private static void readStringSet(RegistryFriendlyByteBuf buf, Set<String> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String value = buf.readUtf();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
    }

    public void serialize(ValueOutput output) {
        output.putInt("schemaVersion", schemaVersion);
        output.putString("trackedMissionId", trackedMissionId);
        output.putInt("migratedSourceCount", migratedSources.size());
        int index = 0;
        for (String source : migratedSources) {
            output.putString("migratedSource_" + index++, source);
        }
        output.putInt("unlockedChapterCount", unlockedChapters.size());
        index = 0;
        for (String chapter : unlockedChapters) {
            output.putString("unlockedChapter_" + index++, chapter);
        }
        output.putInt("missionCount", missions.size());
        index = 0;
        for (Map.Entry<String, MissionState> entry : missions.entrySet()) {
            output.putString("mission_" + index + "_id", entry.getKey());
            entry.getValue().serialize(output, "mission_" + index + "_");
            index++;
        }
    }

    public void deserialize(ValueInput input) {
        schemaVersion = input.getIntOr("schemaVersion", 1);
        trackedMissionId = input.getStringOr("trackedMissionId", "");
        migratedSources.clear();
        int migratedCount = input.getIntOr("migratedSourceCount", 0);
        for (int i = 0; i < migratedCount; i++) {
            String source = input.getStringOr("migratedSource_" + i, "");
            if (!source.isBlank()) {
                migratedSources.add(source);
            }
        }
        unlockedChapters.clear();
        int unlockedChapterCount = input.getIntOr("unlockedChapterCount", 0);
        for (int i = 0; i < unlockedChapterCount; i++) {
            String chapter = input.getStringOr("unlockedChapter_" + i, "");
            if (!chapter.isBlank()) {
                unlockedChapters.add(chapter);
            }
        }
        missions.clear();
        int missionCount = input.getIntOr("missionCount", 0);
        for (int i = 0; i < missionCount; i++) {
            String id = input.getStringOr("mission_" + i + "_id", "");
            if (!id.isBlank()) {
                MissionState state = new MissionState();
                state.deserialize(input, "mission_" + i + "_");
                missions.put(id, state);
            }
        }
    }

    public static final class MissionState {
        private MissionStatus status = MissionStatus.UNLOCKED;
        private final Map<String, Integer> objectiveProgress = new HashMap<>();
        private final Set<String> claimedRewards = new HashSet<>();
        private final Set<String> revealedObjectives = new HashSet<>();
        private int repeatCompletions;
        private long lastCompletedGameTime;

        public MissionStatus status() {
            return status;
        }

        public void status(MissionStatus status) {
            this.status = status == null ? MissionStatus.UNLOCKED : status;
        }

        public int objectiveProgress(Identifier objectiveId) {
            return objectiveProgress.getOrDefault(normalize(objectiveId), 0);
        }

        public int addObjectiveProgress(Identifier objectiveId, int amount, int required) {
            String key = normalize(objectiveId);
            int next = Math.min(Math.max(1, required), objectiveProgress.getOrDefault(key, 0) + Math.max(0, amount));
            objectiveProgress.put(key, next);
            return next;
        }

        public void setObjectiveProgress(Identifier objectiveId, int value) {
            objectiveProgress.put(normalize(objectiveId), Math.max(0, value));
        }

        public Map<String, Integer> objectiveProgress() {
            return Collections.unmodifiableMap(objectiveProgress);
        }

        public boolean isRewardClaimed(Identifier rewardId) {
            return claimedRewards.contains(normalize(rewardId));
        }

        public void claimReward(Identifier rewardId) {
            claimedRewards.add(normalize(rewardId));
        }

        public void clearProgressAndRewards() {
            objectiveProgress.clear();
            claimedRewards.clear();
            revealedObjectives.clear();
        }

        public boolean isObjectiveRevealed(Identifier objectiveId) {
            return revealedObjectives.contains(normalize(objectiveId));
        }

        public void revealObjective(Identifier objectiveId) {
            revealedObjectives.add(normalize(objectiveId));
        }

        public int repeatCompletions() {
            return repeatCompletions;
        }

        public void incrementRepeatCompletions() {
            repeatCompletions++;
        }

        public long lastCompletedGameTime() {
            return lastCompletedGameTime;
        }

        public void lastCompletedGameTime(long tick) {
            lastCompletedGameTime = Math.max(0L, tick);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(status.name());
            buf.writeVarInt(repeatCompletions);
            buf.writeLong(lastCompletedGameTime);
            writeStringIntMap(buf, objectiveProgress);
            writeStringSet(buf, claimedRewards);
            writeStringSet(buf, revealedObjectives);
        }

        private static MissionState read(RegistryFriendlyByteBuf buf) {
            MissionState state = new MissionState();
            try {
                state.status = MissionStatus.valueOf(buf.readUtf());
            } catch (IllegalArgumentException exception) {
                state.status = MissionStatus.UNLOCKED;
            }
            state.repeatCompletions = buf.readVarInt();
            state.lastCompletedGameTime = buf.readLong();
            readStringIntMap(buf, state.objectiveProgress);
            readStringSet(buf, state.claimedRewards);
            readStringSet(buf, state.revealedObjectives);
            return state;
        }

        private void serialize(ValueOutput output, String prefix) {
            output.putString(prefix + "status", status.name());
            output.putInt(prefix + "repeatCompletions", repeatCompletions);
            output.putLong(prefix + "lastCompletedGameTime", lastCompletedGameTime);
            output.putInt(prefix + "objectiveCount", objectiveProgress.size());
            int index = 0;
            for (Map.Entry<String, Integer> entry : objectiveProgress.entrySet()) {
                output.putString(prefix + "objective_" + index + "_id", entry.getKey());
                output.putInt(prefix + "objective_" + index + "_progress", entry.getValue());
                index++;
            }
            writePersistedSet(output, prefix + "claimedReward", claimedRewards);
            writePersistedSet(output, prefix + "revealedObjective", revealedObjectives);
        }

        private void deserialize(ValueInput input, String prefix) {
            try {
                status = MissionStatus.valueOf(input.getStringOr(prefix + "status", MissionStatus.UNLOCKED.name()));
            } catch (IllegalArgumentException exception) {
                status = MissionStatus.UNLOCKED;
            }
            repeatCompletions = input.getIntOr(prefix + "repeatCompletions", 0);
            lastCompletedGameTime = input.getLongOr(prefix + "lastCompletedGameTime", 0L);
            objectiveProgress.clear();
            int objectiveCount = input.getIntOr(prefix + "objectiveCount", 0);
            for (int i = 0; i < objectiveCount; i++) {
                String id = input.getStringOr(prefix + "objective_" + i + "_id", "");
                int progress = input.getIntOr(prefix + "objective_" + i + "_progress", 0);
                if (!id.isBlank() && progress > 0) {
                    objectiveProgress.put(id, progress);
                }
            }
            claimedRewards.clear();
            readPersistedSet(input, prefix + "claimedReward", claimedRewards);
            revealedObjectives.clear();
            readPersistedSet(input, prefix + "revealedObjective", revealedObjectives);
        }
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
            if (!key.isBlank() && value > 0) {
                values.put(key, value);
            }
        }
    }

    private static void writePersistedSet(ValueOutput output, String prefix, Set<String> values) {
        output.putInt(prefix + "Count", values.size());
        int index = 0;
        for (String value : values) {
            output.putString(prefix + "_" + index++, value);
        }
    }

    private static void readPersistedSet(ValueInput input, String prefix, Set<String> values) {
        int count = input.getIntOr(prefix + "Count", 0);
        for (int i = 0; i < count; i++) {
            String value = input.getStringOr(prefix + "_" + i, "");
            if (!value.isBlank()) {
                values.add(value);
            }
        }
    }
}
