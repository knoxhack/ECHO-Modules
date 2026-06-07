package com.knoxhack.echo.scriptcore.adapter;

import com.knoxhack.echo.scriptcore.model.EchoMissionDefinition;
import com.knoxhack.echo.scriptcore.model.EchoObjective;
import com.knoxhack.echo.scriptcore.model.EchoReward;
import com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry;
import com.knoxhack.echocore.api.mission.IMissionRegistry;
import com.knoxhack.echocore.api.mission.MissionChapterDefinition;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echocore.api.mission.MissionKind;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echocore.api.mission.MissionRepeatPolicy;
import com.knoxhack.echocore.api.mission.MissionRewardClaimMode;
import com.knoxhack.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echocore.api.mission.RewardDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

final class ScriptCoreMissionContent {
    static final String SOURCE = "echoscriptcore";

    private ScriptCoreMissionContent() {
    }

    static void register(IMissionRegistry registry) {
        Snapshot snapshot = snapshot();
        registry.replaceSourceContent(SOURCE, snapshot.chapters(), snapshot.missions());
    }

    static Snapshot snapshot() {
        Set<String> packs = new LinkedHashSet<>();
        for (var definition : EchoScriptRegistry.INSTANCE.getByType("mission")) {
            if (definition instanceof EchoMissionDefinition mission) {
                packs.add(mission.pack());
            }
        }
        List<MissionChapterDefinition> chapters = new ArrayList<>();
        int order = 700;
        for (String pack : packs) {
            chapters.add(new MissionChapterDefinition(
                    chapterId(pack),
                    "ScriptCore: " + pack,
                    "ScriptCore-authored missions for pack " + pack + ".",
                    order++,
                    0x66D9EF));
        }
        List<MissionDefinition> missions = new ArrayList<>();
        for (var definition : EchoScriptRegistry.INSTANCE.getByType("mission")) {
            if (definition instanceof EchoMissionDefinition mission) {
                missions.add(convert(mission));
            }
        }
        return new Snapshot(List.copyOf(chapters), List.copyOf(missions));
    }

    private static MissionDefinition convert(EchoMissionDefinition mission) {
        MissionDefinition.Builder builder = MissionDefinition.builder(mission.id(), chapterId(mission.pack()))
                .phase(phaseId(mission), phaseTitle(mission), phaseOrder(mission), missionOrder(mission))
                .text(mission.title().orElse(mission.id().getPath()), mission.briefing(), mission.description().orElse(""))
                .category(mission.route(), "")
                .kind("optional".equals(mission.role()) ? MissionKind.SIDE_OP : MissionKind.MAIN)
                .repeatPolicy("repeatable".equals(mission.role()) ? MissionRepeatPolicy.REPEATABLE : MissionRepeatPolicy.ONCE)
                .hidden("hidden".equals(mission.role()))
                .metadata("scriptcore_pack", mission.pack())
                .metadata("scriptcore_type", "mission");
        for (var prerequisite : mission.prerequisites()) {
            prerequisite.mission().ifPresent(builder::prerequisite);
        }
        int index = 0;
        for (EchoObjective objective : mission.objectives()) {
            builder.objective(convert(mission.id(), objective, index++));
        }
        index = 0;
        for (EchoReward reward : mission.rewards()) {
            builder.reward(convert(mission.id(), reward, index++));
        }
        return builder.build();
    }

    private static ObjectiveDefinition convert(Identifier missionId, EchoObjective objective, int index) {
        Map<String, String> criteria = new LinkedHashMap<>();
        objective.target().ifPresent(id -> criteria.put("target", id.toString()));
        objective.item().ifPresent(id -> criteria.put("target", id.toString()));
        objective.block().ifPresent(id -> criteria.put("target", id.toString()));
        objective.entity().ifPresent(id -> criteria.put("target", id.toString()));
        objective.region().ifPresent(id -> criteria.put("region", id.toString()));
        return new ObjectiveDefinition(
                childId(missionId, objective.id().isBlank() ? "objective_" + index : objective.id()),
                objectiveType(objective.type()),
                objective.title(),
                objective.description(),
                ItemStack.EMPTY,
                objective.count(),
                objective.hidden(),
                criteria);
    }

    private static RewardDefinition convert(Identifier missionId, EchoReward reward, int index) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("scriptcore_reward_type", reward.type());
        reward.item().ifPresent(id -> metadata.put("item", id.toString()));
        if (reward.count() > 0) {
            metadata.put("count", Integer.toString(reward.count()));
        }
        reward.entry().ifPresent(id -> metadata.put("entry", id.toString()));
        reward.layer().ifPresent(id -> metadata.put("layer", id.toString()));
        reward.marker().ifPresent(id -> metadata.put("marker", id.toString()));
        reward.state().ifPresent(id -> metadata.put("state", id.toString()));
        return new RewardDefinition(
                childId(missionId, "reward_" + index),
                MissionRewardClaimMode.IMMEDIATE,
                ItemStack.EMPTY,
                reward.type(),
                metadata.toString(),
                metadata);
    }

    private static Identifier chapterId(String pack) {
        String namespace = pack == null || pack.isBlank() ? "echoscriptcore" : pack.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return Identifier.fromNamespaceAndPath(namespace, "scriptcore");
    }

    private static Identifier childId(Identifier missionId, String child) {
        return Identifier.fromNamespaceAndPath(missionId.getNamespace(), missionId.getPath() + "/" + child.replaceAll("[^a-z0-9_./-]", "_"));
    }

    private static String phaseId(EchoMissionDefinition mission) {
        return mission.phase().isBlank() ? "scriptcore" : mission.phase();
    }

    private static String phaseTitle(EchoMissionDefinition mission) {
        return mission.phase().isBlank() ? "ScriptCore" : readable(mission.phase());
    }

    private static int phaseOrder(EchoMissionDefinition mission) {
        Object value = mission.metadata().get("phase_order");
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static int missionOrder(EchoMissionDefinition mission) {
        Object value = mission.metadata().get("mission_order");
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static MissionObjectiveType objectiveType(String type) {
        return switch (type) {
            case "collect_item", "obtain_item" -> MissionObjectiveType.OBTAIN_ITEM;
            case "craft_item" -> MissionObjectiveType.CRAFT_ITEM;
            case "kill_entity" -> MissionObjectiveType.KILL_ENTITY;
            case "interact_block", "build_structure" -> MissionObjectiveType.PLACE_BLOCK;
            default -> MissionObjectiveType.CUSTOM;
        };
    }

    private static String readable(String value) {
        StringBuilder label = new StringBuilder();
        for (String word : value.replace('-', '_').split("_")) {
            if (word.isBlank()) {
                continue;
            }
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0))).append(word.length() > 1 ? word.substring(1) : "");
        }
        return label.isEmpty() ? value : label.toString();
    }

    record Snapshot(List<MissionChapterDefinition> chapters, List<MissionDefinition> missions) {
    }
}
