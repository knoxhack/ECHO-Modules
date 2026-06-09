package com.knoxhack.echocore.api.mission;

public record ObjectiveDefinition(String id, String label, MissionObjectiveType type, int targetCount) {
}
