package com.knoxhack.echoweathercore.api.region;

import com.knoxhack.echoweathercore.api.weather.ActiveWeatherEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.resources.Identifier;

public class WeatherRegionState {
    private final Identifier regionId;
    private final List<ActiveWeatherEvent> activeEvents = new ArrayList<>();

    public WeatherRegionState(Identifier regionId) {
        this.regionId = regionId;
    }

    public Identifier regionId() {
        return regionId;
    }

    public List<ActiveWeatherEvent> activeEvents() {
        return Collections.unmodifiableList(activeEvents);
    }

    public void addEvent(ActiveWeatherEvent event) {
        activeEvents.add(event);
    }

    public void removeEvent(java.util.UUID eventId) {
        activeEvents.removeIf(e -> e.eventId().equals(eventId));
    }

    public void clearEndedEvents(long currentTick) {
        activeEvents.removeIf(e -> !e.isActive(currentTick));
    }
}
