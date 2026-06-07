package com.knoxhack.echostationfall.world;

import com.knoxhack.echostationfall.EchoStationfall;
import com.knoxhack.echostationfall.progression.StationPowerState;
import com.knoxhack.echostationfall.progression.StationSection;
import com.knoxhack.echostationfall.progression.StationfallObjective;
import com.knoxhack.echostationfall.progression.StationfallProgress;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class StationfallStationState extends SavedData {
    private static final int CURRENT_LIGHTING_VERSION = 1;
    private static final Codec<Map<String, String>> MAP = Codec.unboundedMap(Codec.STRING, Codec.STRING);
    private static final Codec<StationfallStationState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("seeded", false).forGetter(state -> state.seeded),
            Codec.BOOL.optionalFieldOf("boss_active", false).forGetter(state -> state.bossActive),
            Codec.BOOL.optionalFieldOf("boss_defeated", false).forGetter(state -> state.bossDefeated),
            Codec.BOOL.optionalFieldOf("blackbox_rewarded", false).forGetter(state -> state.blackboxRewarded),
            Codec.INT.optionalFieldOf("lighting_version", 0).forGetter(state -> state.lightingVersion),
            MAP.optionalFieldOf("power", Map.of()).forGetter(StationfallStationState::powerMap),
            MAP.optionalFieldOf("doors", Map.of()).forGetter(StationfallStationState::doorMap),
            MAP.optionalFieldOf("logs", Map.of()).forGetter(StationfallStationState::logMap),
            MAP.optionalFieldOf("breaches", Map.of()).forGetter(StationfallStationState::breachMap),
            MAP.optionalFieldOf("objectives", Map.of()).forGetter(StationfallStationState::objectiveMap)
    ).apply(instance, StationfallStationState::new));

    public static final SavedDataType<StationfallStationState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EchoStationfall.MODID, "stationfall_station_state"),
            StationfallStationState::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private boolean seeded;
    private boolean bossActive;
    private boolean bossDefeated;
    private boolean blackboxRewarded;
    private int lightingVersion;
    private final EnumMap<StationSection, StationPowerState> power = new EnumMap<>(StationSection.class);
    private final EnumMap<StationSection, Boolean> doors = new EnumMap<>(StationSection.class);
    private final EnumMap<StationSection, Boolean> logs = new EnumMap<>(StationSection.class);
    private final EnumMap<StationSection, Boolean> breaches = new EnumMap<>(StationSection.class);
    private final EnumMap<StationfallObjective, Boolean> objectives = new EnumMap<>(StationfallObjective.class);

    public StationfallStationState() {
        for (StationSection section : StationSection.values()) {
            power.put(section, section == StationSection.DOCKING_RING ? StationPowerState.EMERGENCY : StationPowerState.OFFLINE);
            doors.put(section, section == StationSection.DOCKING_RING);
            logs.put(section, false);
            breaches.put(section, false);
        }
        for (StationfallObjective objective : StationfallObjective.values()) {
            objectives.put(objective, false);
        }
    }

    private StationfallStationState(
            boolean seeded,
            boolean bossActive,
            boolean bossDefeated,
            boolean blackboxRewarded,
            int lightingVersion,
            Map<String, String> power,
            Map<String, String> doors,
            Map<String, String> logs,
            Map<String, String> breaches,
            Map<String, String> objectives
    ) {
        this();
        this.seeded = seeded;
        this.bossActive = bossActive;
        this.bossDefeated = bossDefeated;
        this.blackboxRewarded = blackboxRewarded;
        this.lightingVersion = Math.max(0, lightingVersion);
        power.forEach((key, value) -> this.power.put(StationSection.byKey(key), StationPowerState.byName(value)));
        doors.forEach((key, value) -> this.doors.put(StationSection.byKey(key), Boolean.parseBoolean(value)));
        logs.forEach((key, value) -> this.logs.put(StationSection.byKey(key), Boolean.parseBoolean(value)));
        breaches.forEach((key, value) -> this.breaches.put(StationSection.byKey(key), Boolean.parseBoolean(value)));
        objectives.forEach((key, value) -> this.objectives.put(StationfallObjective.byKey(key), Boolean.parseBoolean(value)));
    }

    public static StationfallStationState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean bossDefeated() {
        return bossDefeated;
    }

    public boolean bossActive() {
        return bossActive;
    }

    public boolean blackboxRewarded() {
        return blackboxRewarded;
    }

    public int lightingVersion() {
        return lightingVersion;
    }

    public void ensureSeeded(ServerLevel level) {
        if (!seeded) {
            StationfallStationGenerator.generate(level);
            seeded = true;
            lightingVersion = CURRENT_LIGHTING_VERSION;
            setDirty();
        }
    }

    public void ensureLighting(ServerLevel level) {
        if (lightingVersion < CURRENT_LIGHTING_VERSION) {
            StationfallStationGenerator.repairLighting(level);
            lightingVersion = CURRENT_LIGHTING_VERSION;
            setDirty();
        }
    }

    public void mergeFromProgress(StationfallProgress progress) {
        boolean changed = false;
        for (StationSection section : StationSection.values()) {
            if (progress.powerState(section).ordinal() > powerState(section).ordinal()) {
                power.put(section, progress.powerState(section));
                changed = true;
            }
            if (progress.doorUnlocked(section) && !doorUnlocked(section)) {
                doors.put(section, true);
                changed = true;
            }
            if (progress.logDecoded(section) && !logDecoded(section)) {
                logs.put(section, true);
                changed = true;
            }
        }
        for (StationfallObjective objective : StationfallObjective.values()) {
            if (progress.objectiveComplete(objective) && !objectiveComplete(objective)) {
                objectives.put(objective, true);
                changed = true;
            }
        }
        if (progress.bossDefeated() && !bossDefeated) {
            bossDefeated = true;
            bossActive = false;
            changed = true;
        }
        if (progress.blackboxRetrieved() && !blackboxRewarded) {
            blackboxRewarded = true;
            changed = true;
        }
        if (changed) {
            setDirty();
        }
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

    public boolean breachRepaired(StationSection section) {
        return breaches.getOrDefault(section, false);
    }

    public boolean objectiveComplete(StationfallObjective objective) {
        return objectives.getOrDefault(objective, false);
    }

    public void setPower(StationSection section, StationPowerState state) {
        power.put(section, state);
        if (state.opensDoors()) {
            doors.put(section, true);
            if (section.next() != null) {
                doors.put(section.next(), true);
            }
        }
        setDirty();
    }

    public void setLogDecoded(StationSection section, boolean value) {
        logs.put(section, value);
        setDirty();
    }

    public void setBreachRepaired(StationSection section, boolean value) {
        breaches.put(section, value);
        setDirty();
    }

    public void setObjectiveComplete(StationfallObjective objective, boolean value) {
        objectives.put(objective, value);
        setDirty();
    }

    public void startBoss() {
        bossActive = true;
        setDirty();
    }

    public void defeatBoss() {
        bossActive = false;
        bossDefeated = true;
        setDirty();
    }

    public void markBlackboxRewarded() {
        blackboxRewarded = true;
        setDirty();
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

    private Map<String, String> powerMap() {
        Map<String, String> map = new LinkedHashMap<>();
        power.forEach((section, value) -> map.put(section.key(), value.name()));
        return map;
    }

    private Map<String, String> doorMap() {
        return bool(doors);
    }

    private Map<String, String> logMap() {
        return bool(logs);
    }

    private Map<String, String> breachMap() {
        return bool(breaches);
    }

    private Map<String, String> objectiveMap() {
        Map<String, String> map = new LinkedHashMap<>();
        objectives.forEach((objective, value) -> map.put(objective.key(), Boolean.toString(value)));
        return map;
    }

    private static Map<String, String> bool(EnumMap<StationSection, Boolean> source) {
        Map<String, String> map = new LinkedHashMap<>();
        source.forEach((section, value) -> map.put(section.key(), Boolean.toString(value)));
        return map;
    }
}
