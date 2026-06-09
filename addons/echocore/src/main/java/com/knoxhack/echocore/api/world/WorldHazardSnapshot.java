package com.knoxhack.echocore.api.world;

public record WorldHazardSnapshot(String id, String worldId, double severity, String description) {
}
