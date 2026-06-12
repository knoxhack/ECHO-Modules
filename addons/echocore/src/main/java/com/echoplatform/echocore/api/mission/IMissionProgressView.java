package com.echoplatform.echocore.api.mission;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface IMissionProgressView {
    MissionDefinition definition();

    default Identifier id() {
        MissionDefinition definition = definition();
        return definition == null ? null : definition.id();
    }

    default Identifier chapterId() {
        MissionDefinition definition = definition();
        return definition == null ? null : definition.chapterId();
    }

    MissionStatus status();

    default float progress() {
        return 0.0F;
    }

    default String statusLabel() {
        MissionStatus status = status();
        return status == null ? "" : status.name();
    }

    default String unlockReason() {
        return "";
    }

    default String actionHint() {
        return "";
    }

    default List<IObjectiveView> objectives() {
        MissionDefinition definition = definition();
        return definition == null ? List.of() : definition.objectives().stream()
                .map(IObjectiveView.class::cast)
                .toList();
    }

    default List<IRewardView> rewards() {
        MissionDefinition definition = definition();
        return definition == null ? List.of() : definition.rewards().stream()
                .map(IRewardView.class::cast)
                .toList();
    }

    default List<MissionActionView> actions() {
        return List.of();
    }
}
