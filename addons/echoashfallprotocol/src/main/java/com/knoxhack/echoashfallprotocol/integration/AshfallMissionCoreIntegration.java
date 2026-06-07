package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionChapterDefinition;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echocore.api.mission.MissionKind;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echocore.api.mission.MissionRewardClaimMode;
import com.knoxhack.echocore.api.mission.MissionStatus;
import com.knoxhack.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echocore.api.mission.RewardDefinition;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.echo.AshfallMissionRoute;
import com.knoxhack.echoashfallprotocol.echo.EchoGuideManager;
import com.knoxhack.echoashfallprotocol.echo.Mission;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AshfallMissionCoreIntegration {
    public static final Identifier CHAPTER_ID = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "ashfall_protocol");
    public static final Identifier NATIVE_JSON_CHAPTER_ID =
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "ashfall_crash_landing");

    private static boolean registered;

    private AshfallMissionCoreIntegration() {
    }

    public static void register() {
        registerWhenReady();
    }

    public static boolean registerWhenReady() {
        if (registered) {
            return true;
        }
        if (!itemStackComponentsBound()) {
            return false;
        }
        if (nativeJsonOwnsRoute()) {
            registered = true;
            EchoAshfallProtocol.LOGGER.info("Ashfall MissionCore native JSON route detected; Java fallback exporter skipped.");
            return true;
        }
        registered = true;
        try {
            EchoCoreServices.registerMissionContent(EchoAshfallProtocol.MODID, registry -> {
                registry.registerChapter(EchoAshfallProtocol.MODID, new MissionChapterDefinition(
                        CHAPTER_ID,
                        "ECHO-7 Protocol Chain",
                        "Required ECHO-7 field protocols for crash survival, route recovery, drone support, buried nodes, and the Nexus decision.",
                        10,
                        0x66D9FF));
                for (int phase = 0; phase < MissionRegistry.getPhaseCount(); phase++) {
                    List<Mission> missions = MissionRegistry.getMissionsForPhase(phase);
                    for (int index = 0; index < missions.size(); index++) {
                        registry.registerMission(EchoAshfallProtocol.MODID, definition(missions.get(index), phase, index + 1));
                    }
                }
            });
            return true;
        } catch (RuntimeException | LinkageError exception) {
            registered = false;
            EchoAshfallProtocol.LOGGER.warn("Ashfall MissionCore content is not ready yet; it will be retried.", exception);
            return false;
        }
    }

    public static boolean nativeJsonOwnsRoute() {
        try {
            return EchoCoreServices.missionCoreAvailable()
                    && EchoCoreServices.missionService()
                            .missionDefinition(missionId("secure_crash_outpost"))
                            .map(definition -> NATIVE_JSON_CHAPTER_ID.equals(definition.chapterId())
                                    && "echomissioncore".equals(definition.metadata().get("nativeProvider")))
                            .orElse(false);
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static boolean itemStackComponentsBound() {
        try {
            return !new ItemStack(Items.STONE).isEmpty();
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    public static boolean hasClaimableReward(Player player, Mission mission) {
        if (player == null || mission == null || mission.rewards().isEmpty()) {
            return false;
        }
        try {
            return EchoCoreServices.missionCoreAvailable()
                    && EchoCoreServices.missionService()
                            .mission(player, missionId(mission.id()))
                            .map(view -> (view.status() == MissionStatus.CLAIMABLE
                                    || view.status() == MissionStatus.COMPLETED)
                                    && view.rewards().stream().anyMatch(reward -> reward.claimable() && !reward.claimed()))
                            .orElse(false);
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    public static boolean claimReward(ServerPlayer player, String missionId) {
        if (player == null || missionId == null || missionId.isBlank()) {
            return false;
        }
        try {
            return EchoCoreServices.missionCoreAvailable()
                    && EchoCoreServices.claimMissionReward(player, missionId(missionId));
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static MissionDefinition definition(Mission mission, int phase, int order) {
        Identifier id = missionId(mission.id());
        boolean mainSpine = AshfallMissionRoute.blocksPhase(mission);
        int routePhase = AshfallMissionRoute.routePhase(mission.id(), phase);
        int routeOrder = AshfallMissionRoute.routeOrder(mission.id(), order);
        MissionDefinition.Builder builder = MissionDefinition.builder(id, CHAPTER_ID)
                .phase("phase_" + (phase + 1), "P" + (phase + 1) + " " + MissionRegistry.getPhaseTitle(phase), phase, order)
                .text(mission.objectiveText(), mission.echoMessage(), mission.completionMessage())
                .category(mission.category().getDisplayName(), mission.difficulty().name())
                .icon(missionIcon(mission))
                .kind(mainSpine ? MissionKind.MAIN : MissionKind.SIDE_OP)
                .metadata("legacy_id", mission.id())
                .metadata("source", EchoAshfallProtocol.MODID)
                .metadata("terminal_route_phase", Integer.toString(routePhase))
                .metadata("terminal_route_order", Integer.toString(routeOrder))
                .metadata("terminal_route_role", mainSpine ? "MAIN" : "OPTIONAL")
                .metadata("terminal_route_visible", "true")
                .statusRule((player, definition) -> legacyStatus(player, mission))
                .completionRule((player, definition) -> canComplete(player, mission))
                .completionHandler((player, definition) -> mirrorCompletion(player, mission));
        String routeAnchor = AshfallMissionRoute.routeAnchor(mission.id());
        if (!routeAnchor.isBlank()) {
            builder.metadata("terminal_route_anchor", missionId(routeAnchor).toString());
        }
        List<String> prerequisites = mainSpine
                ? AshfallMissionRoute.mainlinePrerequisites(mission)
                : mission.getPrerequisites();
        for (String prerequisite : prerequisites) {
            builder.prerequisite(missionId(prerequisite));
        }
        for (ObjectiveDefinition objective : objectives(mission, id)) {
            builder.objective(objective);
        }
        int rewardIndex = 0;
        for (ItemStack reward : mission.rewards()) {
            builder.reward(RewardDefinition.item(
                    childId(id, "reward_" + rewardIndex++),
                    MissionRewardClaimMode.CLAIMABLE,
                    reward));
        }
        return builder.build();
    }

    private static java.util.Optional<MissionStatus> legacyStatus(Player player, Mission mission) {
        if (player == null) {
            return java.util.Optional.empty();
        }
        if (mission.isPathPreview(player)) {
            return java.util.Optional.of(MissionStatus.VIEW_ONLY);
        }
        QuestData quest = QuestData.get(player);
        if (quest.isMissionCompleted(mission.id())) {
            return java.util.Optional.of(MissionStatus.CLAIMED);
        }
        return switch (quest.getMissionStatus(mission.id())) {
            case COMPLETED -> java.util.Optional.of(MissionStatus.CLAIMED);
            case UNLOCKED -> java.util.Optional.of(MissionStatus.UNLOCKED);
            case LOCKED -> java.util.Optional.of(MissionStatus.LOCKED);
        };
    }

    private static boolean canComplete(Player player, Mission mission) {
        if (player == null || mission.isPathPreview(player)) {
            return false;
        }
        QuestData quest = QuestData.get(player);
        if (!quest.isMissionUnlocked(mission.id())) {
            return false;
        }
        if (quest.isMissionCompleted(mission.id())) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return EchoGuideManager.hasAllRequirements(serverPlayer, mission);
        }
        try {
            return mission.isComplete(player);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static void mirrorCompletion(ServerPlayer player, Mission mission) {
        QuestData quest = QuestData.get(player);
        if (!quest.isMissionCompleted(mission.id())) {
            quest.completeMission(player, mission.id(), Collections.emptyList());
            quest.clearTurnInReminder(mission.id());
            quest.repairMissionState(player);
            QuestData.saveAndSync(player, quest);
        }
    }

    private static List<ObjectiveDefinition> objectives(Mission mission, Identifier missionId) {
        List<ObjectiveDefinition> objectives = new ArrayList<>();
        int index = 0;
        for (ItemStack stack : mission.requiredItems()) {
            Map<String, String> criteria = new LinkedHashMap<>();
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            criteria.put("target", itemId.toString());
            objectives.add(new ObjectiveDefinition(
                    childId(missionId, "item_" + index++),
                    mission.validatesRequiredItems() ? MissionObjectiveType.DELIVER_ITEM : MissionObjectiveType.OBTAIN_ITEM,
                    stack.getHoverName().getString(),
                    stack.getCount() + " required",
                    stack,
                    Math.max(1, stack.getCount()),
                    false,
                    criteria));
        }
        for (Mission.BlockRequirement requirement : mission.requiredBlocks()) {
            objectives.add(new ObjectiveDefinition(
                    childId(missionId, "place_" + requirement.blockId()),
                    MissionObjectiveType.PLACE_BLOCK,
                    requirement.displayName(),
                    requirement.count() + " placed",
                    blockIcon(requirement.blockId(), requirement.displayName()),
                    Math.max(1, requirement.count()),
                    false,
                    Map.of("target", requirement.blockId())));
        }
        for (Mission.EntityKillRequirement requirement : mission.requiredEntityKills()) {
            objectives.add(new ObjectiveDefinition(
                    childId(missionId, "kill_" + safePath(requirement.entityType())),
                    MissionObjectiveType.KILL_ENTITY,
                    requirement.displayName(),
                    requirement.count() + " neutralized",
                    missionIcon(mission),
                    Math.max(1, requirement.count()),
                    false,
                    Map.of("target", requirement.entityType())));
        }
        for (Mission.LocationRequirement requirement : mission.requiredLocations()) {
            MissionObjectiveType type = switch (requirement.locationType()) {
                case "poi" -> MissionObjectiveType.DISCOVER_STRUCTURE;
                case "dimension", "biome", "special" -> MissionObjectiveType.ENTER_REGION;
                default -> MissionObjectiveType.CUSTOM;
            };
            objectives.add(new ObjectiveDefinition(
                    childId(missionId, "visit_" + safePath(requirement.locationId())),
                    type,
                    requirement.displayName(),
                    requirement.locationType(),
                    missionIcon(mission),
                    1,
                    false,
                    Map.of("target", requirement.locationId(), "location_type", requirement.locationType())));
        }
        for (Mission.EquipmentRequirement requirement : mission.requiredEquipment()) {
            objectives.add(new ObjectiveDefinition(
                    childId(missionId, "equip_" + index++),
                    MissionObjectiveType.CUSTOM,
                    requirement.displayName(),
                    "Equipped",
                    requirement.item(),
                    1,
                    false,
                    Map.of("slot", requirement.slot().getName())));
        }
        if (objectives.isEmpty()) {
            objectives.add(ObjectiveDefinition.simple(
                    childId(missionId, "complete"),
                    MissionObjectiveType.CUSTOM,
                    mission.objectiveText(),
                    "Progress tracked by Ashfall route state",
                    missionIcon(mission),
                    1));
        }
        return objectives;
    }

    public static Identifier missionId(String missionId) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, safePath(missionId));
    }

    private static Identifier childId(Identifier parent, String child) {
        return Identifier.fromNamespaceAndPath(parent.getNamespace(), parent.getPath() + "/" + safePath(child));
    }

    private static String safePath(String path) {
        return path == null || path.isBlank() ? "unknown" : path.toLowerCase(java.util.Locale.ROOT)
                .replace(':', '/')
                .replace(' ', '_');
    }

    private static ItemStack missionIcon(Mission mission) {
        ItemStack objective = mission.getObjectiveItem();
        return objective.isEmpty() ? ItemStack.EMPTY : objective.copy();
    }

    private static ItemStack blockIcon(String blockId, String displayName) {
        for (Identifier id : blockIds(blockId)) {
            ItemStack icon = BuiltInRegistries.BLOCK.getOptional(id)
                    .map(block -> block.asItem())
                    .filter(item -> item != Items.AIR)
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
            if (!icon.isEmpty()) {
                return icon;
            }
        }
        String fallback = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        if (fallback.contains("campfire")) {
            return new ItemStack(Items.CAMPFIRE);
        }
        if (fallback.contains("collector")) {
            return new ItemStack(Items.CAULDRON);
        }
        if (fallback.contains("generator")) {
            return new ItemStack(Items.FURNACE);
        }
        if (fallback.contains("lab")) {
            return new ItemStack(Items.LECTERN);
        }
        if (fallback.contains("node")) {
            return new ItemStack(Items.REDSTONE_BLOCK);
        }
        return new ItemStack(Items.STONE);
    }

    private static List<Identifier> blockIds(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return List.of();
        }
        String value = blockId.strip();
        if (value.contains(":")) {
            Identifier parsed = Identifier.tryParse(value);
            return parsed == null ? List.of() : List.of(parsed);
        }
        List<Identifier> ids = new ArrayList<>();
        addBlockId(ids, EchoAshfallProtocol.MODID, value);
        addBlockId(ids, "minecraft", value);
        return List.copyOf(ids);
    }

    private static void addBlockId(List<Identifier> ids, String namespace, String path) {
        Identifier id = Identifier.tryParse(namespace + ":" + path);
        if (id != null && !ids.contains(id)) {
            ids.add(id);
        }
    }
}
