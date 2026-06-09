package com.knoxhack.echocore.api.mission;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryMissionRegistry implements IMissionRegistry {
    private final Map<String, MissionDefinition> missions = new LinkedHashMap<>();

    @Override
    public void register(MissionDefinition mission) {
        missions.put(mission.id(), mission);
    }

    @Override
    public Optional<MissionDefinition> find(String missionId) {
        return Optional.ofNullable(missions.get(missionId));
    }

    @Override
    public Collection<MissionDefinition> all() {
        return List.copyOf(missions.values());
    }
}
