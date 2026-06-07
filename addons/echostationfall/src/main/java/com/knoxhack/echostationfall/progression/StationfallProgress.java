package com.knoxhack.echostationfall.progression;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoHandoffs;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echostationfall.integration.StationfallMissionHooks;
import com.knoxhack.echostationfall.integration.StationfallOrbitalCompat;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class StationfallProgress {
    public static final String ROOT = "echostationfall_progress";
    public static final String MILESTONE_COORDINATES = "stationfall.coordinates_unlocked";
    public static final String MILESTONE_BOARDED = "stationfall.boarded";
    public static final String MILESTONE_OVERRIDE = "stationfall.ai_override";
    public static final String MILESTONE_BOSS = "stationfall.station_mother_defeated";
    public static final String MILESTONE_BLACKBOX = EchoHandoffs.STATIONFALL_BLACKBOX_RECOVERED;

    private boolean coordinatesUnlocked;
    private boolean boarded;
    private boolean aiOverrideObtained;
    private boolean bossDefeated;
    private boolean blackboxRetrieved;
    private boolean hasReturnPoint;
    private double returnX;
    private double returnY = 96.0D;
    private double returnZ;
    private String returnDimension = "minecraft:overworld";
    private final EnumMap<StationSection, StationPowerState> power = new EnumMap<>(StationSection.class);
    private final EnumMap<StationSection, Boolean> doors = new EnumMap<>(StationSection.class);
    private final EnumMap<StationSection, Boolean> logs = new EnumMap<>(StationSection.class);
    private final EnumMap<StationfallObjective, Boolean> objectives = new EnumMap<>(StationfallObjective.class);
    private final EnumMap<StationfallObjective, Integer> objectiveSteps = new EnumMap<>(StationfallObjective.class);
    private final EnumMap<StationfallObjective, LinkedHashSet<String>> objectiveStepKeys =
            new EnumMap<>(StationfallObjective.class);
    private final LinkedHashSet<String> claimed = new LinkedHashSet<>();

    private StationfallProgress() {
        for (StationSection section : StationSection.values()) {
            power.put(section, section == StationSection.DOCKING_RING ? StationPowerState.EMERGENCY : StationPowerState.OFFLINE);
            doors.put(section, section == StationSection.DOCKING_RING);
            logs.put(section, false);
        }
        for (StationfallObjective objective : StationfallObjective.values()) {
            objectives.put(objective, false);
            objectiveSteps.put(objective, 0);
            objectiveStepKeys.put(objective, new LinkedHashSet<>());
        }
    }

    public static StationfallProgress get(Player player) {
        if (player == null) {
            return new StationfallProgress();
        }
        return read(player.getPersistentData().getCompoundOrEmpty(ROOT));
    }

    public static void reset(Player player) {
        player.getPersistentData().remove(ROOT);
    }

    public void save(Player player) {
        player.getPersistentData().put(ROOT, write());
    }

    public boolean coordinatesUnlocked() {
        return coordinatesUnlocked;
    }

    public boolean boarded() {
        return boarded;
    }

    public boolean aiOverrideObtained() {
        return aiOverrideObtained;
    }

    public boolean bossDefeated() {
        return bossDefeated;
    }

    public boolean blackboxRetrieved() {
        return blackboxRetrieved;
    }

    public boolean hasReturnPoint() {
        return hasReturnPoint;
    }

    public double returnX() {
        return returnX;
    }

    public double returnY() {
        return returnY;
    }

    public double returnZ() {
        return returnZ;
    }

    public String returnDimension() {
        return returnDimension;
    }

    public StationPowerState powerState(StationSection section) {
        return power.getOrDefault(section, StationPowerState.OFFLINE);
    }

    public boolean doorUnlocked(StationSection section) {
        return doors.getOrDefault(section, false) || powerState(section).opensDoors();
    }

    public boolean logDecoded(StationSection section) {
        return logs.getOrDefault(section, false);
    }

    public boolean objectiveComplete(StationfallObjective objective) {
        return objectives.getOrDefault(objective, false);
    }

    public int objectiveStepCount(StationfallObjective objective) {
        return Math.min(objective.targetSteps(), objectiveSteps.getOrDefault(objective, 0));
    }

    public int poweredSectionCount() {
        int count = 0;
        for (StationSection section : StationSection.values()) {
            if (powerState(section).stableOrBetter()) {
                count++;
            }
        }
        return count;
    }

    public int decodedLogCount() {
        int count = 0;
        for (StationSection section : StationSection.values()) {
            if (logDecoded(section)) {
                count++;
            }
        }
        return count;
    }

    public int objectiveCount() {
        int count = 0;
        for (StationfallObjective objective : StationfallObjective.values()) {
            if (objectiveComplete(objective)) {
                count++;
            }
        }
        return count;
    }

    public boolean allObjectivesComplete() {
        return objectiveCount() >= StationfallObjective.values().length;
    }

    public boolean terminalRewardClaimed(String id) {
        return claimed.contains(id);
    }

    public boolean markTerminalRewardClaimed(Player player, String id) {
        if (id == null || id.isBlank() || claimed.contains(id)) {
            return false;
        }
        claimed.add(id);
        save(player);
        return true;
    }

    public void unlockCoordinates(Player player) {
        coordinatesUnlocked = true;
        save(player);
        if (player instanceof ServerPlayer serverPlayer) {
            EchoCoreServices.recordMilestone(serverPlayer, MILESTONE_COORDINATES);
        }
    }

    public void setReturnPoint(Player player) {
        returnX = player.getX();
        returnY = player.getY();
        returnZ = player.getZ();
        returnDimension = player.level().dimension().identifier().toString();
        hasReturnPoint = true;
        save(player);
    }

    public ResourceKey<Level> returnLevelKey() {
        return StationfallOrbitalCompat.dimensionKeyFromString(returnDimension);
    }

    public void markBoarded(Player player) {
        boolean first = !boarded;
        coordinatesUnlocked = true;
        boarded = true;
        save(player);
        if (player instanceof ServerPlayer serverPlayer) {
            EchoCoreServices.recordMilestone(serverPlayer, MILESTONE_BOARDED);
        }
        if (first) {
            StationfallMissionHooks.record(player, "board_station", MissionObjectiveType.ESTABLISH_ROUTE, 1, "route", "stationfall");
        }
    }

    public void setSectionPower(Player player, StationSection section, StationPowerState state) {
        StationPowerState previous = powerState(section);
        power.put(section, state);
        if (state.opensDoors()) {
            doors.put(section, true);
            if (section.next() != null) {
                doors.put(section.next(), true);
            }
        }
        save(player);
        if (state.stableOrBetter() && player instanceof ServerPlayer serverPlayer) {
            EchoCoreServices.recordMilestone(serverPlayer, "stationfall.power." + section.key());
        }
        if (!previous.stableOrBetter() && state.stableOrBetter()) {
            StationfallMissionHooks.record(player, "restore_power", MissionObjectiveType.REPAIR_MACHINE, 1, "section", section.key());
        }
    }

    public void decodeLog(Player player, StationSection section) {
        boolean first = !logDecoded(section);
        logs.put(section, true);
        save(player);
        if (player instanceof ServerPlayer serverPlayer) {
            EchoCoreServices.unlockArchive(serverPlayer, StationLore.crewLogId(section));
            EchoCoreServices.recordMilestone(serverPlayer, "stationfall.log." + section.key());
        }
        if (first) {
            StationfallMissionHooks.record(player, "decode_logs", MissionObjectiveType.UNLOCK_RESEARCH, 1, "section", section.key());
        }
    }

    public boolean markObjectiveComplete(Player player, StationfallObjective objective) {
        if (objectiveComplete(objective)) {
            return false;
        }
        objectives.put(objective, true);
        objectiveSteps.put(objective, objective.targetSteps());
        save(player);
        if (player instanceof ServerPlayer serverPlayer) {
            EchoCoreServices.recordMilestone(serverPlayer, "stationfall.objective." + objective.key());
        }
        StationfallMissionHooks.record(player, "stabilize_sections", MissionObjectiveType.REPAIR_MACHINE, 1, "section", objective.key());
        StationfallMissionHooks.recordSectionStabilized(player, objective);
        return true;
    }

    public boolean recordObjectiveStep(Player player, StationfallObjective objective, String stepKey) {
        String key = stepKey == null || stepKey.isBlank() ? objective.key() : stepKey;
        LinkedHashSet<String> keys = objectiveStepKeys.computeIfAbsent(objective, ignored -> new LinkedHashSet<>());
        if (!keys.add(key)) {
            return false;
        }
        int next = Math.min(objective.targetSteps(), objectiveStepCount(objective) + 1);
        objectiveSteps.put(objective, next);
        if (next >= objective.targetSteps()) {
            objectives.put(objective, true);
            if (player instanceof ServerPlayer serverPlayer) {
                EchoCoreServices.recordMilestone(serverPlayer, "stationfall.objective." + objective.key());
            }
            StationfallMissionHooks.record(player, "stabilize_sections", MissionObjectiveType.REPAIR_MACHINE, 1, "section", objective.key());
            StationfallMissionHooks.recordSectionStabilized(player, objective);
        }
        save(player);
        return true;
    }

    public void markAiOverrideObtained(Player player) {
        boolean first = !aiOverrideObtained;
        aiOverrideObtained = true;
        save(player);
        if (player instanceof ServerPlayer serverPlayer) {
            EchoCoreServices.recordMilestone(serverPlayer, MILESTONE_OVERRIDE);
        }
        if (first) {
            StationfallMissionHooks.record(player, "ai_override", MissionObjectiveType.OBTAIN_ITEM, 1, "item", "ai_override_chip");
        }
    }

    public void markBossDefeated(Player player) {
        boolean first = !bossDefeated;
        bossDefeated = true;
        save(player);
        if (player instanceof ServerPlayer serverPlayer) {
            EchoCoreServices.recordMilestone(serverPlayer, MILESTONE_BOSS);
            EchoCoreServices.unlockArchive(serverPlayer, StationLore.STATION_MOTHER_RECORD);
        }
        if (first) {
            StationfallMissionHooks.record(player, "station_mother", MissionObjectiveType.KILL_ENTITY, 1, "entity", "station_mother");
        }
    }

    public void markBlackboxRetrieved(Player player) {
        boolean first = !blackboxRetrieved;
        blackboxRetrieved = true;
        bossDefeated = true;
        save(player);
        if (player instanceof ServerPlayer serverPlayer) {
            EchoCoreServices.recordMilestone(serverPlayer, MILESTONE_BLACKBOX);
            EchoCoreServices.unlockArchive(serverPlayer, StationLore.BLACKBOX_RECORD);
        }
        if (first) {
            StationfallMissionHooks.record(player, "blackbox", MissionObjectiveType.OBTAIN_ITEM, 1, "item", "stationfall_blackbox");
        }
    }

    public boolean canBoard(Player player) {
        return creativeBypass(player) || coordinatesUnlocked || orbitalGateOpen(player);
    }

    public static boolean creativeBypass(Player player) {
        return player != null && player.hasInfiniteMaterials();
    }

    public static boolean orbitalGateOpen(Player player) {
        if (creativeBypass(player)) {
            return true;
        }
        return StationfallOrbitalCompat.orbitalGateOpen(player);
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("coordinates_unlocked", coordinatesUnlocked);
        tag.putBoolean("boarded", boarded);
        tag.putBoolean("ai_override_obtained", aiOverrideObtained);
        tag.putBoolean("boss_defeated", bossDefeated);
        tag.putBoolean("blackbox_retrieved", blackboxRetrieved);
        tag.putBoolean("has_return_point", hasReturnPoint);
        tag.putDouble("return_x", returnX);
        tag.putDouble("return_y", returnY);
        tag.putDouble("return_z", returnZ);
        tag.putString("return_dimension", returnDimension);
        for (StationSection section : StationSection.values()) {
            String key = section.key();
            tag.putString("power_" + key, powerState(section).name());
            tag.putBoolean("door_" + key, doors.getOrDefault(section, false));
            tag.putBoolean("log_" + key, logDecoded(section));
        }
        for (StationfallObjective objective : StationfallObjective.values()) {
            tag.putBoolean("objective_" + objective.key(), objectiveComplete(objective));
            tag.putInt("objective_steps_" + objective.key(), objectiveStepCount(objective));
            LinkedHashSet<String> keys = objectiveStepKeys.getOrDefault(objective, new LinkedHashSet<>());
            int stepIndex = 0;
            for (String key : keys) {
                tag.putString("objective_step_key_" + objective.key() + "_" + stepIndex++, key);
            }
            tag.putInt("objective_step_key_count_" + objective.key(), stepIndex);
        }
        int index = 0;
        for (String claim : claimed) {
            tag.putString("terminal_reward_" + index++, claim);
        }
        tag.putInt("terminal_reward_count", index);
        return tag;
    }

    public static StationfallProgress read(CompoundTag tag) {
        StationfallProgress progress = new StationfallProgress();
        progress.coordinatesUnlocked = tag.getBooleanOr("coordinates_unlocked", false);
        progress.boarded = tag.getBooleanOr("boarded", false);
        progress.aiOverrideObtained = tag.getBooleanOr("ai_override_obtained", false);
        progress.bossDefeated = tag.getBooleanOr("boss_defeated", false);
        progress.blackboxRetrieved = tag.getBooleanOr("blackbox_retrieved", false);
        progress.hasReturnPoint = tag.getBooleanOr("has_return_point", false);
        progress.returnX = tag.getDoubleOr("return_x", 0.0D);
        progress.returnY = tag.getDoubleOr("return_y", 96.0D);
        progress.returnZ = tag.getDoubleOr("return_z", 0.0D);
        progress.returnDimension = tag.getStringOr("return_dimension", "minecraft:overworld");
        for (StationSection section : StationSection.values()) {
            String key = section.key();
            progress.power.put(section, StationPowerState.byName(tag.getStringOr("power_" + key, progress.powerState(section).name())));
            progress.doors.put(section, tag.getBooleanOr("door_" + key, progress.doors.getOrDefault(section, false)));
            progress.logs.put(section, tag.getBooleanOr("log_" + key, false));
        }
        for (StationfallObjective objective : StationfallObjective.values()) {
            progress.objectives.put(objective, tag.getBooleanOr("objective_" + objective.key(), false));
            int steps = tag.getIntOr(
                    "objective_steps_" + objective.key(),
                    progress.objectiveComplete(objective) ? objective.targetSteps() : 0
            );
            progress.objectiveSteps.put(objective, Math.max(0, Math.min(objective.targetSteps(), steps)));
            LinkedHashSet<String> keys = progress.objectiveStepKeys.computeIfAbsent(objective, ignored -> new LinkedHashSet<>());
            int keyCount = tag.getIntOr("objective_step_key_count_" + objective.key(), 0);
            for (int i = 0; i < keyCount; i++) {
                String value = tag.getStringOr("objective_step_key_" + objective.key() + "_" + i, "");
                if (!value.isBlank()) {
                    keys.add(value);
                }
            }
            if (progress.objectiveStepCount(objective) >= objective.targetSteps()) {
                progress.objectives.put(objective, true);
            }
        }
        int count = tag.getIntOr("terminal_reward_count", 0);
        for (int i = 0; i < count; i++) {
            String value = tag.getStringOr("terminal_reward_" + i, "");
            if (!value.isBlank()) {
                progress.claimed.add(value);
            }
        }
        return progress;
    }
}
