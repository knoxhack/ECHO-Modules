package com.echoplatform.echocore.api.mission;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface IMissionService extends IMissionRegistry {
    default boolean available() {
        return false;
    }

    default Optional<MissionDefinition> mission(String missionId) {
        Identifier parsed = missionId == null || missionId.isBlank() ? null : Identifier.tryParse(missionId);
        return parsed == null ? Optional.empty() : missionDefinition(parsed);
    }

    default Optional<MissionDefinition> missionDefinition(Identifier missionId) {
        if (missionId == null) {
            return Optional.empty();
        }
        return all().stream()
                .filter(definition -> missionId.equals(definition.id()))
                .findFirst();
    }

    default Optional<IMissionProgressView> mission(Player player, Identifier missionId) {
        return missionDefinition(missionId).map(definition -> new MissionView(definition, MissionStatus.AVAILABLE));
    }

    default List<IMissionProgressView> missions(Player player) {
        return List.of();
    }

    default List<IMissionProgressView> missions(Player player, Identifier chapterId) {
        return missions(player).stream()
                .filter(view -> view.chapterId() != null && view.chapterId().equals(chapterId))
                .toList();
    }

    default MissionStatus status(String playerId, String missionId) {
        return MissionStatus.VIEW_ONLY;
    }

    default void setStatus(String playerId, String missionId, MissionStatus status) {
    }

    @Override
    default void register(MissionDefinition mission) {
        registerMission("runtime", mission);
    }

    @Override
    default Optional<MissionDefinition> find(String missionId) {
        return mission(missionId);
    }

    @Override
    default Collection<MissionDefinition> all() {
        return missionDefinitions();
    }

    default Optional<MissionChapterDefinition> chapter(Identifier chapterId) {
        return Optional.empty();
    }

    default List<MissionChapterDefinition> chapters() {
        return List.of();
    }

    default List<MissionDefinition> missionDefinitions() {
        return List.of();
    }

    default void replaceSourceContent(
            String source,
            List<MissionChapterDefinition> sourceChapters,
            List<MissionDefinition> sourceMissions) {
    }

    default void unregisterSource(String source) {
    }

    default boolean startMission(ServerPlayer player, Identifier missionId) {
        return false;
    }

    default boolean completeMission(ServerPlayer player, Identifier missionId) {
        return false;
    }

    default boolean claimReward(ServerPlayer player, Identifier missionId) {
        return false;
    }

    default boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
        return false;
    }

    default boolean recordObjective(
            ServerPlayer player,
            MissionObjectiveType type,
            Identifier target,
            int amount,
            Map<String, String> context) {
        return false;
    }

    default String debugState(Player player, Identifier missionId) {
        return "";
    }

    default void registerHookCoverage(String source, Identifier missionId, Identifier objectiveTarget) {
    }

    default Map<String, String> missionHookCoverageBySource() {
        return Map.of();
    }

    record MissionView(MissionDefinition definition, MissionStatus status) implements IMissionProgressView {
        @Override
        public List<IObjectiveView> objectives() {
            return definition == null ? java.util.List.of() : definition.objectives().stream()
                    .map(IObjectiveView.class::cast)
                    .toList();
        }

        @Override
        public List<IRewardView> rewards() {
            return definition == null ? java.util.List.of() : definition.rewards().stream()
                    .map(IRewardView.class::cast)
                    .toList();
        }
    }
}
