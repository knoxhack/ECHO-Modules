package com.knoxhack.echocore.api.mission;

import java.util.Optional;

public interface IMissionService {
    Optional<MissionDefinition> mission(String missionId);

    MissionStatus status(String playerId, String missionId);

    void setStatus(String playerId, String missionId, MissionStatus status);
}
