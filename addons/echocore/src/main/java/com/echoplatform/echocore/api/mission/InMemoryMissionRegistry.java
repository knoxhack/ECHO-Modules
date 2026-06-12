package com.echoplatform.echocore.api.mission;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class InMemoryMissionRegistry implements IMissionRegistry {
    private final Map<Identifier, MissionChapterDefinition> chapters = new LinkedHashMap<>();
    private final Map<String, MissionDefinition> missions = new LinkedHashMap<>();

    @Override
    public void register(MissionDefinition mission) {
        missions.put(mission.id() == null ? "" : mission.id().toString(), mission);
    }

    @Override
    public void registerChapter(String moduleId, MissionChapterDefinition chapter) {
        if (chapter != null && chapter.id() != null) {
            chapters.put(chapter.id(), chapter);
        }
    }

    @Override
    public Optional<MissionDefinition> find(String missionId) {
        return Optional.ofNullable(missions.get(missionId));
    }

    @Override
    public Collection<MissionDefinition> all() {
        return List.copyOf(missions.values());
    }

    public Optional<MissionChapterDefinition> chapter(Identifier chapterId) {
        return Optional.ofNullable(chapters.get(chapterId));
    }

    public List<MissionChapterDefinition> chapters() {
        return List.copyOf(chapters.values());
    }

    public Optional<MissionDefinition> missionDefinition(Identifier missionId) {
        return missionId == null ? Optional.empty() : find(missionId.toString());
    }

    public List<MissionDefinition> missionDefinitions() {
        return List.copyOf(missions.values());
    }
}
