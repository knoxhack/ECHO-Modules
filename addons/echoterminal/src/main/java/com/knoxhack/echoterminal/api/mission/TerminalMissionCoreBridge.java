package com.knoxhack.echoterminal.api.mission;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.MissionActionView;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.MissionRewardClaimMode;
import com.echoplatform.echocore.api.mission.MissionStatus;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class TerminalMissionCoreBridge {
    private TerminalMissionCoreBridge() {
    }

    public static void registerProvider(String source, TerminalMissionProvider provider) {
        EchoCoreServices.registerMissionContent(source, registry -> {
            TerminalMissionChapter chapter = provider.chapter();
            registry.registerChapter(source, new MissionChapterDefinition(
                    chapter.id(), chapter.title(), chapter.summary(), chapter.order(), chapter.accentColor()));
            for (TerminalMissionDefinition terminalMission : safeMissions(provider, null)) {
                registry.registerMission(source, definition(source, provider, terminalMission));
            }
        });
    }

    private static MissionDefinition definition(
            String source,
            TerminalMissionProvider provider,
            TerminalMissionDefinition terminalMission) {
        TerminalMissionSnapshot routeSnapshot = snapshot(provider, null, terminalMission.id());
        TerminalMissionRole routeRole = safeRole(provider, terminalMission, routeSnapshot);
        MissionDefinition.Builder builder = MissionDefinition.builder(terminalMission.id(), terminalMission.chapterId())
                .phase(terminalMission.phaseId(), terminalMission.phaseTitle(), terminalMission.phaseOrder(), terminalMission.missionOrder())
                .text(terminalMission.title(), terminalMission.briefing(), terminalMission.fieldGuide())
                .category(terminalMission.category(), terminalMission.difficulty())
                .icon(terminalMission.icon())
                .kind(routeRole == TerminalMissionRole.OPTIONAL ? MissionKind.SIDE_OP : MissionKind.MAIN)
                .metadata("terminal_source", source)
                .metadata("terminal_chapter", terminalMission.chapterId().toString())
                .statusRule((player, mission) -> java.util.Optional.of(status(snapshot(provider, player, mission.id()).status())))
                .completionRule((player, mission) -> isComplete(snapshot(provider, player, mission.id()).status()))
                .completionHandler((player, mission) -> handle(provider, player, mission.id(), "complete"))
                .actionProvider((player, mission, status, completeNow) -> actions(snapshot(provider, player, mission.id()).actions()))
                .actionHandler((player, mission, actionId) -> handle(provider, player, mission.id(), actionId));

        safeRoutePlacement(provider, terminalMission, routeSnapshot, routeRole).ifPresent(placement -> {
            builder.metadata("terminal_route_phase", Integer.toString(placement.phaseOrder()));
            builder.metadata("terminal_route_order", Integer.toString(placement.missionOrder()));
            builder.metadata("terminal_route_role", placement.role().name());
            builder.metadata("terminal_route_visible", Boolean.toString(placement.includeInSurvivalRoute()));
        });
        List<Identifier> routePrerequisites = safeRoutePrerequisites(provider, terminalMission, routeSnapshot, routeRole);
        if (!routePrerequisites.isEmpty()) {
            builder.metadata("terminal_route_prerequisites",
                    String.join(",", routePrerequisites.stream().map(Identifier::toString).toList()));
        }
        safeRouteAnchor(provider, terminalMission, routeSnapshot, routeRole)
                .ifPresent(anchor -> builder.metadata("terminal_route_anchor", anchor.toString()));
        for (Map.Entry<String, String> entry : intelMetadata(safeIntelUnlocks(
                provider, terminalMission, routeSnapshot, routeRole)).entrySet()) {
            builder.metadata(entry.getKey(), entry.getValue());
        }

        for (String prerequisite : terminalMission.prerequisites()) {
            Identifier prerequisiteId = Identifier.tryParse(prerequisite);
            if (prerequisiteId != null) {
                builder.prerequisite(prerequisiteId);
            }
        }
        List<TerminalMissionRequirement> requirements = terminalMission.requirements();
        for (int index = 0; index < requirements.size(); index++) {
            builder.objective(objective(source, terminalMission.id(), requirements.get(index), index));
        }
        List<TerminalMissionReward> rewards = terminalMission.rewards();
        for (int index = 0; index < rewards.size(); index++) {
            builder.reward(reward(terminalMission.id(), rewards.get(index), index));
        }
        return builder.build();
    }

    private static ObjectiveDefinition objective(String source, Identifier missionId, TerminalMissionRequirement requirement, int index) {
        Identifier hookTarget = MissionHookTargets.objectiveTarget(source, missionId, index);
        return new ObjectiveDefinition(
                childId(missionId, "requirement_" + index),
                type(requirement.kind()),
                requirement.label(),
                requirement.detail(),
                requirement.icon(),
                Math.max(1, requirement.need()),
                false,
                Map.of(
                        "terminal_requirement", requirement.kind().name().toLowerCase(Locale.ROOT),
                        "target", hookTarget.toString(),
                        "hook_source", source == null ? "unknown" : source,
                        "legacy_mission", missionId.toString()));
    }

    private static RewardDefinition reward(Identifier missionId, TerminalMissionReward reward, int index) {
        ItemStack stack = reward.stack();
        return new RewardDefinition(
                childId(missionId, "reward_" + index),
                MissionRewardClaimMode.IMMEDIATE,
                ItemStack.EMPTY,
                reward.label(),
                reward.detail(),
                stack.isEmpty() ? Map.of("terminal_reward", "text") : Map.of(
                        "terminal_reward", "external",
                        "item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                        "count", Integer.toString(stack.getCount())));
    }

    private static List<MissionActionView> actions(List<TerminalMissionAction> terminalActions) {
        List<MissionActionView> actions = new ArrayList<>();
        for (TerminalMissionAction action : terminalActions == null ? List.<TerminalMissionAction>of() : terminalActions) {
            actions.add(action.enabled()
                    ? MissionActionView.enabled(action.id(), action.label())
                    : MissionActionView.disabled(action.id(), action.label(), action.disabledReason()));
        }
        return actions;
    }

    private static MissionObjectiveType type(TerminalMissionRequirement.Kind kind) {
        return switch (kind) {
            case ITEM -> MissionObjectiveType.OBTAIN_ITEM;
            case BLOCK -> MissionObjectiveType.PLACE_BLOCK;
            case ENTITY_KILL -> MissionObjectiveType.KILL_ENTITY;
            case EQUIPMENT, LOCATION, CUSTOM -> MissionObjectiveType.CUSTOM;
        };
    }

    private static MissionStatus status(TerminalMissionStatus status) {
        return switch (status == null ? TerminalMissionStatus.LOCKED : status) {
            case LOCKED -> MissionStatus.LOCKED;
            case UNLOCKED -> MissionStatus.UNLOCKED;
            case COMPLETED -> MissionStatus.COMPLETED;
            case CLAIMABLE -> MissionStatus.CLAIMABLE;
            case CLAIMED -> MissionStatus.CLAIMED;
            case VIEW_ONLY -> MissionStatus.VIEW_ONLY;
        };
    }

    private static boolean isComplete(TerminalMissionStatus status) {
        return status == TerminalMissionStatus.COMPLETED
                || status == TerminalMissionStatus.CLAIMABLE
                || status == TerminalMissionStatus.CLAIMED;
    }

    private static Identifier childId(Identifier missionId, String suffix) {
        return Identifier.fromNamespaceAndPath(missionId.getNamespace(), missionId.getPath() + "/" + suffix);
    }

    private static List<TerminalMissionDefinition> safeMissions(TerminalMissionProvider provider, Player player) {
        try {
            List<TerminalMissionDefinition> missions = provider.missions(player);
            return missions == null ? List.of() : missions;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static TerminalMissionSnapshot snapshot(TerminalMissionProvider provider, Player player, Identifier missionId) {
        try {
            TerminalMissionSnapshot snapshot = provider.snapshot(player, missionId);
            if (snapshot != null) {
                return snapshot;
            }
        } catch (RuntimeException ignored) {
        }
        return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.VIEW_ONLY, 0.0F, "", "", "", List.of());
    }

    private static java.util.Optional<TerminalMissionRoutePlacement> safeRoutePlacement(
            TerminalMissionProvider provider,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        try {
            return provider.routePlacement(null, definition, snapshot, role);
        } catch (RuntimeException ignored) {
            return java.util.Optional.empty();
        }
    }

    private static List<Identifier> safeRoutePrerequisites(
            TerminalMissionProvider provider,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        try {
            List<Identifier> prerequisites = provider.routePrerequisites(null, definition, snapshot, role);
            return prerequisites == null ? List.of() : prerequisites.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static java.util.Optional<Identifier> safeRouteAnchor(
            TerminalMissionProvider provider,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        try {
            return provider.routeAnchor(null, definition, snapshot, role);
        } catch (RuntimeException ignored) {
            return java.util.Optional.empty();
        }
    }

    private static List<TerminalMissionIntelUnlock> safeIntelUnlocks(
            TerminalMissionProvider provider,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        try {
            List<TerminalMissionIntelUnlock> unlocks = provider.intelUnlocks(null, definition, snapshot, role);
            return unlocks == null ? List.of() : unlocks.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static Map<String, String> intelMetadata(List<TerminalMissionIntelUnlock> unlocks) {
        if (unlocks == null || unlocks.isEmpty()) {
            return Map.of();
        }
        Map<String, List<Identifier>> byKey = new java.util.LinkedHashMap<>();
        for (TerminalMissionIntelUnlock unlock : unlocks) {
            String key = switch (unlock.kind()) {
                case ARCHIVE -> "terminal_intel_archives";
                case ROUTE -> "terminal_intel_routes";
                case DISCOVERY -> "terminal_intel_discoveries";
                case FACTION -> "terminal_intel_factions";
                case POI -> "terminal_intel_pois";
            };
            byKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(unlock.id());
        }
        Map<String, String> metadata = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<Identifier>> entry : byKey.entrySet()) {
            String value = String.join(",", entry.getValue().stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .map(Identifier::toString)
                    .toList());
            if (!value.isBlank()) {
                metadata.put(entry.getKey(), value);
            }
        }
        return Map.copyOf(metadata);
    }

    private static TerminalMissionRole safeRole(
            TerminalMissionProvider provider,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        try {
            return provider.role(null, definition, snapshot);
        } catch (RuntimeException ignored) {
            return TerminalMissionRole.fallback(definition, snapshot);
        }
    }

    private static boolean handle(TerminalMissionProvider provider, net.minecraft.server.level.ServerPlayer player, Identifier missionId, String actionId) {
        try {
            return provider.handleAction(player, missionId, actionId);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
