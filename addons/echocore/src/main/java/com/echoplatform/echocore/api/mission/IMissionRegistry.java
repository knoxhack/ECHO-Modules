package com.echoplatform.echocore.api.mission;

import java.util.Collection;
import java.util.Optional;

public interface IMissionRegistry {
    void register(MissionDefinition mission);

    default void registerChapter(String moduleId, MissionChapterDefinition chapter) {
    }

    default void registerMission(String moduleId, MissionDefinition mission) {
        register(mission);
    }

    default void replaceSourceContent(
            String moduleId,
            Collection<MissionChapterDefinition> chapters,
            Collection<MissionDefinition> missions) {
        if (chapters != null) {
            for (MissionChapterDefinition chapter : chapters) {
                registerChapter(moduleId, chapter);
            }
        }
        if (missions != null) {
            for (MissionDefinition mission : missions) {
                registerMission(moduleId, mission);
            }
        }
    }

    Optional<MissionDefinition> find(String missionId);

    Collection<MissionDefinition> all();
}
