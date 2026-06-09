package com.knoxhack.echocore.api.mission;

import java.util.List;

public record MissionDefinition(String id, String title, MissionKind kind, List<ObjectiveDefinition> objectives, List<RewardDefinition> rewards) {
    public MissionDefinition {
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
    }
}
