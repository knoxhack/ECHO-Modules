package com.knoxhack.echocore.api.mission;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryMissionService implements IMissionService {
    private final IMissionRegistry registry;
    private final Map<String, MissionStatus> statuses = new ConcurrentHashMap<>();

    public InMemoryMissionService(IMissionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Optional<MissionDefinition> mission(String missionId) {
        return registry.find(missionId);
    }

    @Override
    public MissionStatus status(String playerId, String missionId) {
        return statuses.getOrDefault(playerId + ":" + missionId, MissionStatus.AVAILABLE);
    }

    @Override
    public void setStatus(String playerId, String missionId, MissionStatus status) {
        statuses.put(playerId + ":" + missionId, status);
    }
}
