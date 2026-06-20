package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.HashMap;
import java.util.Map;

/**
 * Server-side data for environmental events.
 * Persists across server restarts using the current saved-data API.
 */
public class EnvironmentalEventData extends SavedData {

    public static final int MIN_TIME_BETWEEN_EVENTS = 24000; // 20 minutes minimum
    public static final int MAX_EVENT_DURATION = 6000; // 5 minutes max event

    // Codec for EnvironmentalEventType
    private static final Codec<EnvironmentalEventType> EVENT_TYPE_CODEC = Codec.STRING.xmap(
        s -> {
            try {
                return EnvironmentalEventType.valueOf(s);
            } catch (IllegalArgumentException e) {
                return EnvironmentalEventType.NONE;
            }
        },
        EnvironmentalEventType::name
    );

    // Main codec for EnvironmentalEventData
    public static final Codec<EnvironmentalEventData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            EVENT_TYPE_CODEC.optionalFieldOf("currentEvent", EnvironmentalEventType.NONE).forGetter(d -> d.currentEvent),
            Codec.LONG.optionalFieldOf("eventStartTime", 0L).forGetter(d -> d.eventStartTime),
            Codec.INT.optionalFieldOf("eventDuration", 0).forGetter(d -> d.eventDuration),
            Codec.INT.optionalFieldOf("timeSinceLastEvent", 0).forGetter(d -> d.timeSinceLastEvent),
            Codec.INT.optionalFieldOf("radStormsSurvived", 0).forGetter(d -> d.radStormsSurvived),
            Codec.INT.optionalFieldOf("toxicStormsSurvived", 0).forGetter(d -> d.toxicStormsSurvived),
            Codec.INT.optionalFieldOf("blackoutsSurvived", 0).forGetter(d -> d.blackoutsSurvived),
            Codec.BOOL.optionalFieldOf("weatherSnapshotValid", false).forGetter(d -> d.weatherSnapshotValid),
            Codec.BOOL.optionalFieldOf("forcedWeatherEvent", false).forGetter(d -> d.forcedWeatherEvent),
            Codec.BOOL.optionalFieldOf("previousRaining", false).forGetter(d -> d.previousRaining),
            Codec.BOOL.optionalFieldOf("previousThundering", false).forGetter(d -> d.previousThundering),
            Codec.INT.optionalFieldOf("previousRainTime", 0).forGetter(d -> d.previousRainTime),
            Codec.INT.optionalFieldOf("previousThunderTime", 0).forGetter(d -> d.previousThunderTime),
            Codec.INT.optionalFieldOf("previousClearWeatherTime", 0).forGetter(d -> d.previousClearWeatherTime),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("eventSurvivalCounts", Map.<String, Integer>of())
                    .forGetter(d -> d.eventSurvivalCounts),
            Codec.LONG.optionalFieldOf("eventSeed", 0L).forGetter(d -> d.eventSeed)
        ).apply(instance, (event, start, duration, since, rad, toxic, blackout,
                           weatherSnapshotValid, forcedWeatherEvent, previousRaining, previousThundering,
                           previousRainTime, previousThunderTime, previousClearWeatherTime,
                           eventSurvivalCounts, eventSeed) -> {
            EnvironmentalEventData data = new EnvironmentalEventData();
            data.currentEvent = event;
            data.eventStartTime = start;
            data.eventDuration = duration;
            data.timeSinceLastEvent = since;
            data.radStormsSurvived = rad;
            data.toxicStormsSurvived = toxic;
            data.blackoutsSurvived = blackout;
            data.weatherSnapshotValid = weatherSnapshotValid;
            data.forcedWeatherEvent = forcedWeatherEvent;
            data.previousRaining = previousRaining;
            data.previousThundering = previousThundering;
            data.previousRainTime = previousRainTime;
            data.previousThunderTime = previousThunderTime;
            data.previousClearWeatherTime = previousClearWeatherTime;
            data.eventSurvivalCounts.clear();
            data.eventSurvivalCounts.putAll(eventSurvivalCounts);
            data.eventSeed = eventSeed;
            return data;
        })
    );

    // SavedDataType for registration
    public static final SavedDataType<EnvironmentalEventData> TYPE = new SavedDataType<EnvironmentalEventData>(
        Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "environmental_events"),
        EnvironmentalEventData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_MAP_DATA
    );

    private EnvironmentalEventType currentEvent = EnvironmentalEventType.NONE;
    private long eventStartTime = 0;
    private int eventDuration = 0; // in ticks
    private int timeSinceLastEvent = 0;
    private boolean weatherSnapshotValid = false;
    private boolean forcedWeatherEvent = false;
    private boolean previousRaining = false;
    private boolean previousThundering = false;
    private int previousRainTime = 0;
    private int previousThunderTime = 0;
    private int previousClearWeatherTime = 0;
    private long eventSeed = 0L;

    // Survival tracking for mission completion
    private int radStormsSurvived = 0;
    private int toxicStormsSurvived = 0;
    private int blackoutsSurvived = 0;
    private final Map<String, Integer> eventSurvivalCounts = new HashMap<>();

    public EnvironmentalEventData() {}

    /**
     * Gets or creates the EnvironmentalEventData for a server level.
     * Data is automatically persisted to the world's data folder.
     */
    public static EnvironmentalEventData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public EnvironmentalEventType getCurrentEvent() {
        return currentEvent;
    }

    public long getEventStartTime() {
        return eventStartTime;
    }

    public int getEventDuration() {
        return eventDuration;
    }

    public int getRemainingEventTicks(long gameTime) {
        if (currentEvent == EnvironmentalEventType.NONE) {
            return 0;
        }
        return Math.max(0, eventDuration - (int) (gameTime - eventStartTime));
    }

    public float getEventPhase(long gameTime) {
        if (currentEvent == EnvironmentalEventType.NONE || eventDuration <= 0) {
            return 0.0F;
        }
        float elapsed = (float) Math.max(0L, gameTime - eventStartTime);
        return Math.max(0.0F, Math.min(1.0F, elapsed / (float) eventDuration));
    }

    public long getEventSeed() {
        return eventSeed;
    }

    public float getEventIntensity() {
        if (currentEvent == EnvironmentalEventType.NONE) {
            return 0.0F;
        }
        long mixed = eventSeed ^ (eventSeed >>> 33) ^ ((long) currentEvent.ordinal() * 0x9E3779B97F4A7C15L);
        int bucket = (int) Math.floorMod(mixed, 51L);
        return 0.75F + bucket / 100.0F;
    }

    public void startEvent(EnvironmentalEventType type, long gameTime, int durationTicks, long seed) {
        this.currentEvent = type;
        this.eventStartTime = gameTime;
        this.eventDuration = Math.max(20, durationTicks);
        this.eventSeed = seed;
        this.timeSinceLastEvent = 0;
        clearWeatherSnapshotFields();
        setDirty();
    }

    public void endEvent(EnvironmentalEventType type) {
        if (currentEvent == type) {
            // Record survival
            switch (type) {
                case RADIATION_STORM -> radStormsSurvived++;
                case TOXIC_STORM -> toxicStormsSurvived++;
                case BLACKOUT -> blackoutsSurvived++;
                default -> {
                }
            }
            if (type != EnvironmentalEventType.NONE) {
                eventSurvivalCounts.merge(type.name(), 1, Integer::sum);
            }
            currentEvent = EnvironmentalEventType.NONE;
            eventSeed = 0L;
            setDirty();
        }
    }

    public void clearEventWithoutSurvivalCount() {
        if (currentEvent != EnvironmentalEventType.NONE) {
            currentEvent = EnvironmentalEventType.NONE;
            eventSeed = 0L;
            clearWeatherSnapshotFields();
            setDirty();
        }
    }

    public boolean advanceActiveEventForSleep(long gameTime, long skippedTicks) {
        if (currentEvent == EnvironmentalEventType.NONE || skippedTicks <= 0) {
            return false;
        }

        int remainingTicks = getRemainingEventTicks(gameTime);
        if (skippedTicks >= remainingTicks) {
            endEvent(currentEvent);
            return true;
        }

        eventStartTime -= skippedTicks;
        setDirty();
        return false;
    }

    public void tick(Level level) {
        timeSinceLastEvent++;

        // Check if current event should end
        if (currentEvent != EnvironmentalEventType.NONE) {
            long elapsed = level.getGameTime() - eventStartTime;
            if (elapsed > eventDuration) {
                endEvent(currentEvent);
            }
        }
    }

    public boolean canTriggerEvent() {
        return currentEvent == EnvironmentalEventType.NONE && timeSinceLastEvent > MIN_TIME_BETWEEN_EVENTS;
    }

    public void captureWeatherSnapshot(ServerLevel level) {
        if (weatherSnapshotValid) {
            return;
        }
        var weather = level.getWeatherData();
        previousRaining = weather.isRaining();
        previousThundering = weather.isThundering();
        previousRainTime = weather.getRainTime();
        previousThunderTime = weather.getThunderTime();
        previousClearWeatherTime = weather.getClearWeatherTime();
        weatherSnapshotValid = true;
        forcedWeatherEvent = true;
        setDirty();
    }

    public boolean hasWeatherSnapshot() {
        return weatherSnapshotValid;
    }

    public boolean isForcedWeatherEvent() {
        return forcedWeatherEvent;
    }

    public boolean wasRainingBeforeEvent() {
        return previousRaining;
    }

    public boolean wasThunderingBeforeEvent() {
        return previousThundering;
    }

    public int getPreviousRainTime() {
        return previousRainTime;
    }

    public int getPreviousThunderTime() {
        return previousThunderTime;
    }

    public int getPreviousClearWeatherTime() {
        return previousClearWeatherTime;
    }

    public void clearWeatherSnapshot() {
        if (weatherSnapshotValid || forcedWeatherEvent || previousRaining || previousThundering) {
            clearWeatherSnapshotFields();
            setDirty();
        }
    }

    private void clearWeatherSnapshotFields() {
        weatherSnapshotValid = false;
        forcedWeatherEvent = false;
        previousRaining = false;
        previousThundering = false;
        previousRainTime = 0;
        previousThunderTime = 0;
        previousClearWeatherTime = 0;
    }

    public boolean isInRadiationStorm() {
        return currentEvent == EnvironmentalEventType.RADIATION_STORM;
    }

    public boolean isInToxicStorm() {
        return currentEvent == EnvironmentalEventType.TOXIC_STORM;
    }

    public boolean isInBlackout() {
        return currentEvent == EnvironmentalEventType.BLACKOUT;
    }

    public int getEventsSurvived(EnvironmentalEventType type) {
        return eventSurvivalCounts.getOrDefault(type.name(), switch (type) {
            case RADIATION_STORM -> radStormsSurvived;
            case TOXIC_STORM -> toxicStormsSurvived;
            case BLACKOUT -> blackoutsSurvived;
            default -> 0;
        });
    }

    public int getRadStormsSurvived() {
        return radStormsSurvived;
    }

    public int getToxicStormsSurvived() {
        return toxicStormsSurvived;
    }

    public int getBlackoutsSurvived() {
        return blackoutsSurvived;
    }
}
