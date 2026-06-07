package com.knoxhack.echotutorialcore.data;

import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialGuideMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TutorialPlayerData implements EchoValueIOSerializable {
    public static final StreamCodec<RegistryFriendlyByteBuf, TutorialPlayerData> STREAM_CODEC = StreamCodec.of(
            TutorialPlayerData::writeSync,
            TutorialPlayerData::readSync);

    private TutorialGuideMode guideMode = TutorialGuideMode.NORMAL;
    private final Set<String> progressFlags = new LinkedHashSet<>();
    private final Set<String> dismissedHintIds = new LinkedHashSet<>();
    private final Set<String> dismissedCardIds = new LinkedHashSet<>();
    private final Set<String> unlockedCardIds = new LinkedHashSet<>();
    private final Set<String> unreadCardIds = new LinkedHashSet<>();
    private final Set<String> completedFlowIds = new LinkedHashSet<>();
    private final Map<String, Set<String>> flowStepProgress = new LinkedHashMap<>();
    private final Map<String, Integer> mistakeCounters = new HashMap<>();
    private final Map<String, Long> lastHintTimestamps = new HashMap<>();
    private final List<Long> recentHintTimestamps = new ArrayList<>();
    private int popupCountThisSession = 0;
    private long lastDeathTime = 0;
    private String lastDeathCause = "";
    private int repeatedDeathCount = 0;
    private long lastProgressGameTime = 0;
    private long lastTerminalOpenTime = 0;
    private long lastHoloMapOpenTime = 0;
    private long lastLensScanTime = 0;
    private long lastScannerUseTime = 0;
    private String lastRecommendationReason = "";
    private String lastPowerAlert = "";
    private String lastMissionState = "";
    private String lastRegionId = "";
    private final Set<String> lastHazardIds = new LinkedHashSet<>();

    public static TutorialPlayerData get(Player player) {
        if (player == null) {
            return new TutorialPlayerData();
        }
        try {
            return player.getData(ModAttachments.TUTORIAL_PLAYER_DATA.get());
        } catch (Exception e) {
            EchoTutorialCore.LOGGER.debug("Failed to get tutorial player data, returning blank.", e);
            return new TutorialPlayerData();
        }
    }

    public static void save(Player player, TutorialPlayerData data) {
        if (player == null || data == null) {
            return;
        }
        try {
            player.setData(ModAttachments.TUTORIAL_PLAYER_DATA.get(), data);
        } catch (Exception e) {
            EchoTutorialCore.LOGGER.debug("Failed to save tutorial player data.", e);
        }
    }

    public static void saveAndSync(ServerPlayer player, TutorialPlayerData data) {
        save(player, data);
        if (player != null) {
            try {
                player.syncData(ModAttachments.TUTORIAL_PLAYER_DATA.get());
            } catch (Exception e) {
                EchoTutorialCore.LOGGER.debug("Failed to sync tutorial player data.", e);
            }
        }
    }

    public TutorialGuideMode guideMode() {
        return guideMode;
    }

    public void setGuideMode(TutorialGuideMode mode) {
        this.guideMode = mode == null ? TutorialGuideMode.NORMAL : mode;
    }

    public boolean hasProgress(Identifier id) {
        return id != null && hasProgress(id.toString());
    }

    public boolean hasProgress(String id) {
        return id != null && !id.isBlank() && progressFlags.contains(id);
    }

    public void markProgress(Identifier id) {
        if (id != null) {
            progressFlags.add(id.toString());
        }
    }

    public boolean markProgressIfNew(Identifier id) {
        return id != null && progressFlags.add(id.toString());
    }

    public boolean isHintDismissed(Identifier id) {
        return id != null && dismissedHintIds.contains(id.toString());
    }

    public void dismissHint(Identifier id) {
        if (id != null) {
            dismissedHintIds.add(id.toString());
        }
    }

    public boolean isCardDismissed(Identifier id) {
        return id != null && dismissedCardIds.contains(id.toString());
    }

    public void dismissCard(Identifier id) {
        if (id != null) {
            dismissedCardIds.add(id.toString());
            unreadCardIds.remove(id.toString());
        }
    }

    public boolean isCardUnlocked(Identifier id) {
        return id != null && unlockedCardIds.contains(id.toString());
    }

    public void unlockCard(Identifier id) {
        unlockCardIfNew(id);
    }

    public boolean unlockCardIfNew(Identifier id) {
        if (id == null) {
            return false;
        }
        String key = id.toString();
        boolean added = unlockedCardIds.add(key);
        if (added) {
            unreadCardIds.add(key);
        }
        return added;
    }

    public boolean isCardUnread(Identifier id) {
        return id != null && unreadCardIds.contains(id.toString());
    }

    public void markCardRead(Identifier id) {
        if (id != null) {
            unreadCardIds.remove(id.toString());
        }
    }

    public boolean isFlowCompleted(Identifier id) {
        return id != null && completedFlowIds.contains(id.toString());
    }

    public void completeFlow(Identifier id) {
        if (id != null) {
            completedFlowIds.add(id.toString());
        }
    }

    public boolean markFlowStep(Identifier flowId, String stepId) {
        if (flowId == null || stepId == null || stepId.isBlank()) {
            return false;
        }
        return flowStepProgress
                .computeIfAbsent(flowId.toString(), ignored -> new LinkedHashSet<>())
                .add(stepId);
    }

    public boolean hasFlowStep(Identifier flowId, String stepId) {
        if (flowId == null || stepId == null || stepId.isBlank()) {
            return false;
        }
        return flowStepProgress.getOrDefault(flowId.toString(), Set.of()).contains(stepId);
    }

    public Set<String> flowStepIds(Identifier flowId) {
        if (flowId == null) {
            return Set.of();
        }
        return Set.copyOf(flowStepProgress.getOrDefault(flowId.toString(), Set.of()));
    }

    public int getMistakeCount(String key) {
        return mistakeCounters.getOrDefault(key, 0);
    }

    public void incrementMistake(String key) {
        if (key != null && !key.isBlank()) {
            mistakeCounters.merge(key, 1, Integer::sum);
        }
    }

    public void resetMistake(String key) {
        mistakeCounters.remove(key);
    }

    public Map<String, Integer> mistakeCounters() {
        return Map.copyOf(mistakeCounters);
    }

    public long getLastHintTime(Identifier id) {
        return lastHintTimestamps.getOrDefault(id == null ? "" : id.toString(), 0L);
    }

    public void recordHintTime(Identifier id, long time) {
        if (id != null) {
            lastHintTimestamps.put(id.toString(), time);
        }
    }

    public void recordHintPopup(long gameTime) {
        recentHintTimestamps.add(gameTime);
        trimRecentHintTimestamps(gameTime);
    }

    public int recentHintCount(long now, long windowTicks) {
        trimRecentHintTimestamps(now);
        int count = 0;
        for (long timestamp : recentHintTimestamps) {
            if (now - timestamp <= windowTicks) {
                count++;
            }
        }
        return count;
    }

    private void trimRecentHintTimestamps(long now) {
        recentHintTimestamps.removeIf(time -> now - time > 1200L);
    }

    public int popupCountThisSession() {
        return popupCountThisSession;
    }

    public void incrementPopupCount() {
        popupCountThisSession++;
    }

    public void resetPopupCount() {
        popupCountThisSession = 0;
        recentHintTimestamps.clear();
    }

    public long lastDeathTime() {
        return lastDeathTime;
    }

    public String lastDeathCause() {
        return lastDeathCause;
    }

    public int repeatedDeathCount() {
        return repeatedDeathCount;
    }

    public void recordDeath(String cause, long time) {
        if (cause != null && cause.equals(lastDeathCause)) {
            repeatedDeathCount++;
        } else {
            repeatedDeathCount = 1;
        }
        lastDeathCause = cause == null ? "" : cause;
        lastDeathTime = time;
    }

    public long lastProgressGameTime() {
        return lastProgressGameTime;
    }

    public void setLastProgressGameTime(long lastProgressGameTime) {
        this.lastProgressGameTime = Math.max(0L, lastProgressGameTime);
    }

    public long lastTerminalOpenTime() {
        return lastTerminalOpenTime;
    }

    public void setLastTerminalOpenTime(long lastTerminalOpenTime) {
        this.lastTerminalOpenTime = Math.max(0L, lastTerminalOpenTime);
    }

    public long lastHoloMapOpenTime() {
        return lastHoloMapOpenTime;
    }

    public void setLastHoloMapOpenTime(long lastHoloMapOpenTime) {
        this.lastHoloMapOpenTime = Math.max(0L, lastHoloMapOpenTime);
    }

    public long lastLensScanTime() {
        return lastLensScanTime;
    }

    public void setLastLensScanTime(long lastLensScanTime) {
        this.lastLensScanTime = Math.max(0L, lastLensScanTime);
    }

    public long lastScannerUseTime() {
        return lastScannerUseTime;
    }

    public void setLastScannerUseTime(long lastScannerUseTime) {
        this.lastScannerUseTime = Math.max(0L, lastScannerUseTime);
    }

    public String lastRecommendationReason() {
        return lastRecommendationReason;
    }

    public void setLastRecommendationReason(String reason) {
        this.lastRecommendationReason = reason == null ? "" : reason;
    }

    public String lastPowerAlert() {
        return lastPowerAlert;
    }

    public void setLastPowerAlert(String alert) {
        this.lastPowerAlert = alert == null ? "" : alert;
    }

    public String lastMissionState() {
        return lastMissionState;
    }

    public void setLastMissionState(String missionState) {
        this.lastMissionState = missionState == null ? "" : missionState;
    }

    public String lastRegionId() {
        return lastRegionId;
    }

    public void setLastRegionId(String regionId) {
        this.lastRegionId = regionId == null ? "" : regionId;
    }

    public void setLastHazardIds(Set<String> hazardIds) {
        lastHazardIds.clear();
        if (hazardIds != null) {
            hazardIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .forEach(lastHazardIds::add);
        }
    }

    public Set<String> lastHazardIds() {
        return Set.copyOf(lastHazardIds);
    }

    public Set<String> progressFlags() {
        return Set.copyOf(progressFlags);
    }

    public Set<String> unlockedCardIds() {
        return Set.copyOf(unlockedCardIds);
    }

    public Set<String> unreadCardIds() {
        return Set.copyOf(unreadCardIds);
    }

    public int unreadCardCount() {
        return unreadCardIds.size();
    }

    public Set<String> completedFlowIds() {
        return Set.copyOf(completedFlowIds);
    }

    public Map<String, Set<String>> flowStepProgress() {
        Map<String, Set<String>> copy = new LinkedHashMap<>();
        flowStepProgress.forEach((flow, steps) -> copy.put(flow, Set.copyOf(steps)));
        return Map.copyOf(copy);
    }

    public void resetAll() {
        guideMode = TutorialGuideMode.NORMAL;
        progressFlags.clear();
        dismissedHintIds.clear();
        dismissedCardIds.clear();
        unlockedCardIds.clear();
        unreadCardIds.clear();
        completedFlowIds.clear();
        flowStepProgress.clear();
        mistakeCounters.clear();
        lastHintTimestamps.clear();
        recentHintTimestamps.clear();
        popupCountThisSession = 0;
        lastDeathTime = 0;
        lastDeathCause = "";
        repeatedDeathCount = 0;
        lastProgressGameTime = 0;
        lastTerminalOpenTime = 0;
        lastHoloMapOpenTime = 0;
        lastLensScanTime = 0;
        lastScannerUseTime = 0;
        lastRecommendationReason = "";
        lastPowerAlert = "";
        lastMissionState = "";
        lastRegionId = "";
        lastHazardIds.clear();
    }

    private static void writeSync(RegistryFriendlyByteBuf buf, TutorialPlayerData data) {
        buf.writeUtf(data.guideMode.name());
        writeStringSet(buf, data.progressFlags);
        writeStringSet(buf, data.dismissedHintIds);
        writeStringSet(buf, data.dismissedCardIds);
        writeStringSet(buf, data.unlockedCardIds);
        writeStringSet(buf, data.unreadCardIds);
        writeStringSet(buf, data.completedFlowIds);
        writeStringSetMap(buf, data.flowStepProgress);
        writeStringIntMap(buf, data.mistakeCounters);
        writeStringLongMap(buf, data.lastHintTimestamps);
        writeLongList(buf, data.recentHintTimestamps);
        buf.writeVarInt(data.popupCountThisSession);
        buf.writeVarLong(data.lastDeathTime);
        buf.writeUtf(data.lastDeathCause);
        buf.writeVarInt(data.repeatedDeathCount);
        buf.writeVarLong(data.lastProgressGameTime);
        buf.writeVarLong(data.lastTerminalOpenTime);
        buf.writeVarLong(data.lastHoloMapOpenTime);
        buf.writeVarLong(data.lastLensScanTime);
        buf.writeVarLong(data.lastScannerUseTime);
        buf.writeUtf(data.lastRecommendationReason);
        buf.writeUtf(data.lastPowerAlert);
        buf.writeUtf(data.lastMissionState);
        buf.writeUtf(data.lastRegionId);
        writeStringSet(buf, data.lastHazardIds);
    }

    private static TutorialPlayerData readSync(RegistryFriendlyByteBuf buf) {
        TutorialPlayerData data = new TutorialPlayerData();
        data.guideMode = TutorialGuideMode.byName(buf.readUtf());
        readStringSet(buf, data.progressFlags);
        readStringSet(buf, data.dismissedHintIds);
        readStringSet(buf, data.dismissedCardIds);
        readStringSet(buf, data.unlockedCardIds);
        readStringSet(buf, data.unreadCardIds);
        readStringSet(buf, data.completedFlowIds);
        readStringSetMap(buf, data.flowStepProgress);
        readStringIntMap(buf, data.mistakeCounters);
        readStringLongMap(buf, data.lastHintTimestamps);
        readLongList(buf, data.recentHintTimestamps);
        data.popupCountThisSession = buf.readVarInt();
        data.lastDeathTime = buf.readVarLong();
        data.lastDeathCause = buf.readUtf();
        data.repeatedDeathCount = buf.readVarInt();
        data.lastProgressGameTime = buf.readVarLong();
        data.lastTerminalOpenTime = buf.readVarLong();
        data.lastHoloMapOpenTime = buf.readVarLong();
        data.lastLensScanTime = buf.readVarLong();
        data.lastScannerUseTime = buf.readVarLong();
        data.lastRecommendationReason = buf.readUtf();
        data.lastPowerAlert = buf.readUtf();
        data.lastMissionState = buf.readUtf();
        data.lastRegionId = buf.readUtf();
        readStringSet(buf, data.lastHazardIds);
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
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
    }

    private static void writeStringSetMap(RegistryFriendlyByteBuf buf, Map<String, Set<String>> values) {
        buf.writeVarInt(values.size());
        values.forEach((key, set) -> {
            buf.writeUtf(key);
            writeStringSet(buf, set);
        });
    }

    private static void readStringSetMap(RegistryFriendlyByteBuf buf, Map<String, Set<String>> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String key = buf.readUtf();
            Set<String> set = new LinkedHashSet<>();
            readStringSet(buf, set);
            if (!key.isBlank()) {
                values.put(key, set);
            }
        }
    }

    private static void writeStringIntMap(RegistryFriendlyByteBuf buf, Map<String, Integer> values) {
        buf.writeVarInt(values.size());
        values.forEach((key, value) -> {
            buf.writeUtf(key);
            buf.writeVarInt(value);
        });
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
        values.forEach((key, value) -> {
            buf.writeUtf(key);
            buf.writeVarLong(value);
        });
    }

    private static void readStringLongMap(RegistryFriendlyByteBuf buf, Map<String, Long> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String key = buf.readUtf();
            long value = buf.readVarLong();
            if (!key.isBlank()) {
                values.put(key, value);
            }
        }
    }

    private static void writeLongList(RegistryFriendlyByteBuf buf, List<Long> values) {
        buf.writeVarInt(values.size());
        for (long value : values) {
            buf.writeVarLong(value);
        }
    }

    private static void readLongList(RegistryFriendlyByteBuf buf, List<Long> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            values.add(buf.readVarLong());
        }
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putString("guideMode", guideMode.name());
        writeStringSet(output, "progress", progressFlags);
        writeStringSet(output, "dismissedHint", dismissedHintIds);
        writeStringSet(output, "dismissedCard", dismissedCardIds);
        writeStringSet(output, "unlockedCard", unlockedCardIds);
        writeStringSet(output, "unreadCard", unreadCardIds);
        writeStringSet(output, "completedFlow", completedFlowIds);
        writeStringSetMap(output, "flowStep", flowStepProgress);
        writeStringIntMap(output, "mistake", mistakeCounters);
        writeStringLongMap(output, "lastHint", lastHintTimestamps);
        writeLongList(output, "recentHint", recentHintTimestamps);
        output.putInt("popupCount", popupCountThisSession);
        output.putLong("lastDeathTime", lastDeathTime);
        output.putString("lastDeathCause", lastDeathCause);
        output.putInt("repeatedDeathCount", repeatedDeathCount);
        output.putLong("lastProgressGameTime", lastProgressGameTime);
        output.putLong("lastTerminalOpenTime", lastTerminalOpenTime);
        output.putLong("lastHoloMapOpenTime", lastHoloMapOpenTime);
        output.putLong("lastLensScanTime", lastLensScanTime);
        output.putLong("lastScannerUseTime", lastScannerUseTime);
        output.putString("lastRecommendationReason", lastRecommendationReason);
        output.putString("lastPowerAlert", lastPowerAlert);
        output.putString("lastMissionState", lastMissionState);
        output.putString("lastRegionId", lastRegionId);
        writeStringSet(output, "lastHazard", lastHazardIds);
    }

    @Override
    public void deserialize(ValueInput input) {
        guideMode = TutorialGuideMode.byName(input.getStringOr("guideMode", "NORMAL"));
        readStringSet(input, "progress", progressFlags);
        readStringSet(input, "dismissedHint", dismissedHintIds);
        readStringSet(input, "dismissedCard", dismissedCardIds);
        readStringSet(input, "unlockedCard", unlockedCardIds);
        readStringSet(input, "unreadCard", unreadCardIds);
        readStringSet(input, "completedFlow", completedFlowIds);
        readStringSetMap(input, "flowStep", flowStepProgress);
        readStringIntMap(input, "mistake", mistakeCounters);
        readStringLongMap(input, "lastHint", lastHintTimestamps);
        readLongList(input, "recentHint", recentHintTimestamps);
        popupCountThisSession = input.getIntOr("popupCount", 0);
        lastDeathTime = input.getLongOr("lastDeathTime", 0);
        lastDeathCause = input.getStringOr("lastDeathCause", "");
        repeatedDeathCount = input.getIntOr("repeatedDeathCount", 0);
        lastProgressGameTime = input.getLongOr("lastProgressGameTime", 0);
        lastTerminalOpenTime = input.getLongOr("lastTerminalOpenTime", 0);
        lastHoloMapOpenTime = input.getLongOr("lastHoloMapOpenTime", 0);
        lastLensScanTime = input.getLongOr("lastLensScanTime", 0);
        lastScannerUseTime = input.getLongOr("lastScannerUseTime", 0);
        lastRecommendationReason = input.getStringOr("lastRecommendationReason", "");
        lastPowerAlert = input.getStringOr("lastPowerAlert", "");
        lastMissionState = input.getStringOr("lastMissionState", "");
        lastRegionId = input.getStringOr("lastRegionId", "");
        readStringSet(input, "lastHazard", lastHazardIds);
    }

    private static void writeStringSet(ValueOutput output, String prefix, Set<String> values) {
        output.putInt(prefix + "Count", values.size());
        int idx = 0;
        for (String id : values) {
            output.putString(prefix + "_" + (idx++), id);
        }
    }

    private static void readStringSet(ValueInput input, String prefix, Set<String> values) {
        values.clear();
        int count = input.getIntOr(prefix + "Count", 0);
        for (int i = 0; i < count; i++) {
            String id = input.getStringOr(prefix + "_" + i, "");
            if (!id.isBlank()) {
                values.add(id);
            }
        }
    }

    private static void writeStringSetMap(ValueOutput output, String prefix, Map<String, Set<String>> values) {
        output.putInt(prefix + "OwnerCount", values.size());
        int ownerIdx = 0;
        for (Map.Entry<String, Set<String>> entry : values.entrySet()) {
            output.putString(prefix + "Owner_" + ownerIdx, entry.getKey());
            output.putInt(prefix + "StepCount_" + ownerIdx, entry.getValue().size());
            int stepIdx = 0;
            for (String step : entry.getValue()) {
                output.putString(prefix + "_" + ownerIdx + "_" + (stepIdx++), step);
            }
            ownerIdx++;
        }
    }

    private static void readStringSetMap(ValueInput input, String prefix, Map<String, Set<String>> values) {
        values.clear();
        int ownerCount = input.getIntOr(prefix + "OwnerCount", 0);
        for (int ownerIdx = 0; ownerIdx < ownerCount; ownerIdx++) {
            String owner = input.getStringOr(prefix + "Owner_" + ownerIdx, "");
            int stepCount = input.getIntOr(prefix + "StepCount_" + ownerIdx, 0);
            Set<String> steps = new LinkedHashSet<>();
            for (int stepIdx = 0; stepIdx < stepCount; stepIdx++) {
                String step = input.getStringOr(prefix + "_" + ownerIdx + "_" + stepIdx, "");
                if (!step.isBlank()) {
                    steps.add(step);
                }
            }
            if (!owner.isBlank()) {
                values.put(owner, steps);
            }
        }
    }

    private static void writeStringIntMap(ValueOutput output, String prefix, Map<String, Integer> values) {
        output.putInt(prefix + "Count", values.size());
        int idx = 0;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            output.putString(prefix + "Key_" + idx, entry.getKey());
            output.putInt(prefix + "Value_" + idx, entry.getValue());
            idx++;
        }
    }

    private static void readStringIntMap(ValueInput input, String prefix, Map<String, Integer> values) {
        values.clear();
        int count = input.getIntOr(prefix + "Count", 0);
        for (int i = 0; i < count; i++) {
            String key = input.getStringOr(prefix + "Key_" + i, "");
            int value = input.getIntOr(prefix + "Value_" + i, 0);
            if (!key.isBlank()) {
                values.put(key, value);
            }
        }
    }

    private static void writeStringLongMap(ValueOutput output, String prefix, Map<String, Long> values) {
        output.putInt(prefix + "Count", values.size());
        int idx = 0;
        for (Map.Entry<String, Long> entry : values.entrySet()) {
            output.putString(prefix + "Key_" + idx, entry.getKey());
            output.putLong(prefix + "Value_" + idx, entry.getValue());
            idx++;
        }
    }

    private static void readStringLongMap(ValueInput input, String prefix, Map<String, Long> values) {
        values.clear();
        int count = input.getIntOr(prefix + "Count", 0);
        for (int i = 0; i < count; i++) {
            String key = input.getStringOr(prefix + "Key_" + i, "");
            long value = input.getLongOr(prefix + "Value_" + i, 0);
            if (!key.isBlank()) {
                values.put(key, value);
            }
        }
    }

    private static void writeLongList(ValueOutput output, String prefix, List<Long> values) {
        output.putInt(prefix + "Count", values.size());
        for (int i = 0; i < values.size(); i++) {
            output.putLong(prefix + "_" + i, values.get(i));
        }
    }

    private static void readLongList(ValueInput input, String prefix, List<Long> values) {
        values.clear();
        int count = input.getIntOr(prefix + "Count", 0);
        for (int i = 0; i < count; i++) {
            values.add(input.getLongOr(prefix + "_" + i, 0));
        }
    }
}
