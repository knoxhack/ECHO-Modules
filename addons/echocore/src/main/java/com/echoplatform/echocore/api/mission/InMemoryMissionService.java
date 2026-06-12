package com.echoplatform.echocore.api.mission;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import net.minecraft.world.entity.player.Player;

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
    public void register(MissionDefinition mission) {
        registry.register(mission);
    }

    @Override
    public Optional<MissionDefinition> find(String missionId) {
        return registry.find(missionId);
    }

    @Override
    public Collection<MissionDefinition> all() {
        return registry.all();
    }

    @Override
    public java.util.List<MissionDefinition> missionDefinitions() {
        return java.util.List.copyOf(registry.all());
    }

    @Override
    public MissionStatus status(String playerId, String missionId) {
        return statuses.getOrDefault(playerId + ":" + missionId, MissionStatus.AVAILABLE);
    }

    @Override
    public void setStatus(String playerId, String missionId, MissionStatus status) {
        statuses.put(playerId + ":" + missionId, status);
    }

    @Override
    public java.util.List<IMissionProgressView> missions(Player player) {
        String playerId = player == null ? "" : player.getUUID().toString();
        return registry.all().stream()
                .map(definition -> new MissionView(definition,
                        status(playerId, definition.id() == null ? "" : definition.id().toString())))
                .map(IMissionProgressView.class::cast)
                .toList();
    }
}
