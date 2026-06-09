package com.knoxhack.echocore.api.mission;

import java.util.Collection;
import java.util.Optional;

public interface IMissionRegistry {
    void register(MissionDefinition mission);

    Optional<MissionDefinition> find(String missionId);

    Collection<MissionDefinition> all();
}
