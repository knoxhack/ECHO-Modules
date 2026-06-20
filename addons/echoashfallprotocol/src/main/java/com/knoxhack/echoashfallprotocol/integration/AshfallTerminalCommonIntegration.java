package com.knoxhack.echoashfallprotocol.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.echo.AshfallMissionActions;
import com.knoxhack.echoashfallprotocol.echo.AshfallMissionRoute;
import com.knoxhack.echoashfallprotocol.echo.EchoGuideManager;
import com.knoxhack.echoashfallprotocol.echo.Mission;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.MissionUxSummary;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.endgame.NexusCampaignActions;
import com.knoxhack.echoashfallprotocol.endgame.NexusChoiceService;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.network.DroneCommandPacket;
import com.knoxhack.echoashfallprotocol.network.ModNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelKind;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelUnlock;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Common/server terminal registrations for Ashfall.
 *
 * <p>Keep this class free of client imports. Client tabs and screen rendering live
 * in {@link AshfallTerminalIntegration}; server-executed mission/action handlers
 * live here so dedicated servers can handle TerminalActionPacket safely.</p>
 */
public final class AshfallTerminalCommonIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final boolean LEGACY_NEXUS_TERMINAL_ENABLED = false;
    private static final String ASHFALL_CHAPTER_ID = "ashfall_protocol";

    private static final Identifier OVERVIEW = id("overview");
    private static final Identifier MISSIONS = id("missions");
    private static final Identifier SIDE_OPS = id("side_ops");
    private static final Identifier DRONE = id("drone");
    private static final Identifier CODEX = id("codex");
    private static final Identifier WORLD = id("world");
    private static final Identifier NEXUS = id("nexus");
    private static final Identifier TURN_IN = id("turn_in_mission");
    private static final Identifier CLAIM_REWARDS = id("claim_terminal_rewards");
    private static final Identifier DRONE_COMMAND = id("drone_command");
    private static final Identifier NEXUS_CHOICE = id("nexus_choice");
    private static final Identifier NEXUS_WARFRONT = id("nexus_warfront");

    private AshfallTerminalCommonIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }

        TerminalMissionRegistryFacade.registerProviders();
        registerAddonInfoProviderIfAbsent();
        TerminalMissionActions.registerForTab(MISSIONS);
        TerminalMissionActions.registerForTab(SIDE_OPS);
        TerminalActionRegistry.register(MISSIONS, TURN_IN, AshfallTerminalCommonIntegration::turnInCurrentMission);
        TerminalActionRegistry.register(MISSIONS, CLAIM_REWARDS, (player, payload) -> {
            EchoGuideManager.claimRewards(player, payload);
            QuestData.syncToClient(player);
        });
        TerminalActionRegistry.register(DRONE, DRONE_COMMAND,
                (player, payload) -> ModNetwork.handleDroneCommand(new DroneCommandPacket(payload), player));
        if (LEGACY_NEXUS_TERMINAL_ENABLED && !nexusProtocolLoaded()) {
            TerminalActionRegistry.register(NEXUS, NEXUS_CHOICE, AshfallTerminalCommonIntegration::chooseNexusPath);
            TerminalActionRegistry.register(NEXUS, NEXUS_WARFRONT, AshfallTerminalCommonIntegration::handleNexusWarfront);
        }
        registerRecipeProviderIfAbsent();

        registerFieldManualEntries();
    }

    public static boolean registeredForTests() {
        return REGISTERED.get();
    }

    private static void turnInCurrentMission(ServerPlayer player, String payload) {
        QuestData quest = QuestData.get(player);
        if (quest.repairMissionState(player)) {
            QuestData.saveAndSync(player, quest);
        }

        Mission target = AshfallMissionActions.resolveTarget(quest, payload);
        String rejection = AshfallMissionActions.turnInRejection(player, quest, target);
        if (!rejection.isBlank()) {
            AshfallMissionActions.sendTurnInRejection(player, rejection);
            QuestData.syncToClient(player);
            return;
        }

        EchoGuideManager.turnInMission(player, quest, target);
        QuestData.syncToClient(player);
    }

    private static void chooseNexusPath(ServerPlayer player, String payload) {
        NexusChoiceService.applyChoice(player, payload);
    }

    private static void handleNexusWarfront(ServerPlayer player, String payload) {
        NexusCampaignActions.handleTerminalAction(player, payload);
    }

    private static boolean nexusProtocolLoaded() {
        try {
            return EchoRuntimeModules.isLoaded("echonexusprotocol");
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.warn("Ashfall terminal Nexus ownership check failed; keeping legacy Nexus terminal available.", exception);
            return false;
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, path);
    }

    private static void registerAddonInfoProviderIfAbsent() {
        if (TerminalAddonInfoRegistry.provider(ASHFALL_CHAPTER_ID).isPresent()) {
            return;
        }
        try {
            TerminalAddonInfoRegistry.register(new AshfallAddonInfoProvider());
        } catch (IllegalArgumentException exception) {
            if (String.valueOf(exception.getMessage()).contains("Duplicate terminal addon info provider id: "
                    + ASHFALL_CHAPTER_ID)) {
                EchoAshfallProtocol.LOGGER.info("Ashfall Terminal addon info provider already registered; keeping existing provider.");
                return;
            }
            throw exception;
        }
    }

    static void registerRecipeProviderIfAbsent() {
        Identifier providerId = AshfallTerminalRecipeProvider.INSTANCE.id();
        boolean registered = TerminalRecipeRegistry.providers().stream()
                .anyMatch(provider -> providerId.equals(provider.id()));
        if (registered) {
            return;
        }
        try {
            TerminalRecipeRegistry.register(AshfallTerminalRecipeProvider.INSTANCE);
        } catch (IllegalArgumentException exception) {
            if (String.valueOf(exception.getMessage()).contains("Duplicate terminal recipe provider id: "
                    + providerId)) {
                EchoAshfallProtocol.LOGGER.info(
                        "Ashfall Terminal recipe provider already registered; keeping existing provider.");
                return;
            }
            throw exception;
        }
    }

    private static final class TerminalMissionRegistryFacade {
        private static final TerminalMissionProvider ASHFALL_MISSIONS = new AshfallMissionProvider();
        private static final TerminalMissionProvider ASHFALL_SIDE_OPS = new AshfallSideOpsProvider();

        private TerminalMissionRegistryFacade() {
        }

        private static void registerProviders() {
            if (EchoRuntimeModules.isLoaded("echomissioncore")) {
                EchoAshfallProtocol.LOGGER.info(
                        "Ashfall Terminal main mission provider skipped; MissionCore owns main mission display.");
                com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry.registerIfAbsent(ASHFALL_SIDE_OPS);
                return;
            }
            com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry.registerIfAbsent(ASHFALL_MISSIONS);
            com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry.registerIfAbsent(ASHFALL_SIDE_OPS);
        }
    }

    private static final class AshfallAddonInfoProvider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return ASHFALL_CHAPTER_ID;
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            if (player == null) {
                return new TerminalAddonInfo(
                        "Chapter 1 survival route, terminal repair, field safety, drone support, and early route intel.",
                        List.of(new TerminalAddonMetric("Signal", "OFFLINE", "waiting for player telemetry", TerminalUi.CYAN)),
                        List.of(new TerminalAddonSection("Start Feed",
                                List.of("Open Ashfall Command after player telemetry is available."))),
                        links(),
                        guide());
            }
            QuestData quest = QuestData.get(player);
            SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
            MissionUxSummary current = MissionUxSummary.current(player, quest);
            return new TerminalAddonInfo(
                    "Chapter 1 survival route, terminal repair, field safety, drone support, and early route intel.",
                    List.of(
                            new TerminalAddonMetric("Phase", String.valueOf(quest.getCurrentPhase() + 1),
                                    "active Ashfall route phase", TerminalUi.GREEN),
                            new TerminalAddonMetric("Mission", String.valueOf(quest.getCurrentMissionIndex() + 1),
                                    current.shortTitle(), TerminalUi.CYAN),
                            new TerminalAddonMetric("Hydration", survival.getHydration() + "%",
                                    "field survival reserve", survival.getHydration() <= 30 ? TerminalUi.AMBER : TerminalUi.GREEN),
                            new TerminalAddonMetric("Filter", Math.round(survival.getFilterPercent() * 100.0F) + "%",
                                    survival.hasMask() ? "mask equipped" : "mask not confirmed",
                                    survival.getFilterPercent() <= 0.25F ? TerminalUi.AMBER : TerminalUi.CYAN)),
                    List.of(new TerminalAddonSection("Start Feed", List.of(
                            current.objectiveSummary(),
                            current.nextStep(),
                            survival.isSafeZone() ? "Safe zone detected." : "No safe zone detected yet."))),
                    links(),
                    guide());
        }

        private static TerminalAddonGuide guide() {
            return TerminalAddonGuide.mainline(1, 10, "Start here",
                    "Begin with Ashfall to learn survival basics, repair the terminal, stabilize camp, and open the first route signals.",
                    List.of(
                            "Secure water, shelter, food, and filters before long trips.",
                            "Open Ashfall Command or Protocol Roadmap for the next mission.",
                            "Use Survival Route when you want the complete roadmap."));
        }

        private static List<TerminalAddonLink> links() {
            return List.of(
                    new TerminalAddonLink(OVERVIEW, "Ashfall Command", "Chapter 1 field dashboard", 0xFF66D9FF),
                    new TerminalAddonLink(MISSIONS, "Protocol Roadmap", "Active Ashfall mission chain", 0xFF92F7A6),
                    new TerminalAddonLink(WORLD, "Route Map", "POIs, routes, and field map", 0xFFC09BFF),
                    new TerminalAddonLink(CODEX, "Survival Index", "Intel and recipe planning", 0xFFFFD166));
        }
    }

    private static final class AshfallMissionProvider implements TerminalMissionProvider {
        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(
                    id("ashfall_protocol"),
                    "ECHO-7 PROTOCOL CHAIN",
                    "Required ECHO-7 field protocols for crash survival, route recovery, drone support, buried nodes, and the Nexus decision.",
                    10,
                    0xFF66D9FF,
                    true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            QuestData quest = QuestData.get(player);
            List<TerminalMissionDefinition> definitions = new ArrayList<>();
            for (int phase = 0; phase < MissionRegistry.getPhaseCount(); phase++) {
                List<Mission> missions = MissionRegistry.getMissionsForPhase(phase);
                for (int i = 0; i < missions.size(); i++) {
                    Mission mission = missions.get(i);
                    definitions.add(definition(player, quest, mission, phase, i + 1));
                }
            }
            return definitions;
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            Mission mission = missionId == null ? null : MissionRegistry.getMissionById(missionId.getPath());
            if (mission == null) {
                return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                        "LOCKED", "Ashfall protocol signal missing.",
                        "ECHO-7 has no clean field record for this identifier.", List.of());
            }
            QuestData quest = QuestData.get(player);
            QuestData.MissionStatus status = quest.getMissionStatus(mission.id());
            boolean preview = mission.isPathPreview(player);
            boolean pendingRewards = AshfallMissionActions.hasClaimableRewards(player, quest, mission);
            boolean completeNow = cheapMissionSatisfied(player, quest, mission);
            TerminalMissionStatus terminalStatus = preview
                    ? TerminalMissionStatus.VIEW_ONLY
                    : switch (status) {
                        case COMPLETED -> pendingRewards ? TerminalMissionStatus.CLAIMABLE : TerminalMissionStatus.COMPLETED;
                        case UNLOCKED -> TerminalMissionStatus.UNLOCKED;
                        case LOCKED -> TerminalMissionStatus.LOCKED;
                    };
            boolean current = isCurrentMission(quest, mission);
            boolean turnInReady = AshfallMissionActions.canTurnIn(player, quest, mission);
            MissionUxSummary summary = MissionUxSummary.forHud(player, quest, mission);
            String claimReason = AshfallMissionActions.claimReason(player, quest, mission);
            return new TerminalMissionSnapshot(
                    id(mission.id()),
                    terminalStatus,
                    cheapProgress(player, quest, mission, status, completeNow),
                    summary.statusLabel(),
                    terminalStatus == TerminalMissionStatus.LOCKED || terminalStatus == TerminalMissionStatus.VIEW_ONLY
                            ? MissionUxSummary.unlockReason(player, quest, mission)
                            : "",
                    summary.nextStep(),
                    List.of(
                            turnInReady
                                    ? TerminalMissionAction.enabled(TURN_IN.getPath(), "TURN IN")
                                    : TerminalMissionAction.disabled(TURN_IN.getPath(), "TURN IN",
                                            AshfallMissionActions.turnInReason(player, quest, mission, status, current, completeNow, preview)),
                            pendingRewards
                                    ? TerminalMissionAction.enabled(CLAIM_REWARDS.getPath(), "CLAIM REWARDS")
                                    : TerminalMissionAction.disabled(CLAIM_REWARDS.getPath(), "CLAIM REWARDS",
                                            claimReason.isBlank() ? "No sealed support cache is waiting for this protocol." : claimReason)));
        }

        @Override
        public TerminalMissionRole role(Player player, TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot) {
            Mission mission = definition == null ? null : MissionRegistry.getMissionById(definition.id().getPath());
            return mission != null && mission.isPathPreview(player)
                    ? TerminalMissionRole.REFERENCE
                    : mission != null && !AshfallMissionRoute.blocksPhase(mission)
                            ? TerminalMissionRole.OPTIONAL
                            : TerminalMissionRole.MAIN;
        }

        @Override
        public Optional<TerminalMissionRoutePlacement> routePlacement(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            if (definition == null) {
                return Optional.empty();
            }
            Mission mission = MissionRegistry.getMissionById(definition.id().getPath());
            TerminalMissionRole safeRole = mission != null && !AshfallMissionRoute.blocksPhase(mission)
                    ? TerminalMissionRole.OPTIONAL
                    : (role == null ? TerminalMissionRole.MAIN : role);
            return Optional.of(new TerminalMissionRoutePlacement(
                    mission == null
                            ? Math.max(0, Math.min(15, definition.phaseOrder()))
                            : AshfallMissionRoute.routePhase(mission.id(), definition.phaseOrder()),
                    mission == null
                            ? definition.missionOrder()
                            : AshfallMissionRoute.routeOrder(mission.id(), definition.missionOrder()),
                    safeRole,
                    true));
        }

        @Override
        public Optional<Identifier> routeAnchor(Player player, TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot, TerminalMissionRole role) {
            Mission mission = definition == null ? null : MissionRegistry.getMissionById(definition.id().getPath());
            if (mission == null || AshfallMissionRoute.blocksPhase(mission)) {
                return Optional.empty();
            }
            String anchor = AshfallMissionRoute.routeAnchor(mission.id());
            return anchor.isBlank() ? Optional.empty() : Optional.of(id(anchor));
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            if (TURN_IN.getPath().equals(actionId)) {
                turnInCurrentMission(player, missionId == null ? "" : missionId.getPath());
                return true;
            }
            if (CLAIM_REWARDS.getPath().equals(actionId)) {
                EchoGuideManager.claimRewards(player, missionId == null ? "" : missionId.getPath());
                QuestData.syncToClient(player);
                return true;
            }
            return false;
        }

        private static TerminalMissionDefinition definition(Player player, QuestData quest,
                Mission mission, int phase, int missionOrder) {
            return new TerminalMissionDefinition(
                    id(mission.id()),
                    id("ashfall_protocol"),
                    "phase_" + (phase + 1),
                    "P" + (phase + 1) + " " + MissionRegistry.getPhaseTitle(phase),
                    phase,
                    missionOrder,
                    mission.objectiveText(),
                    mission.echoMessage(),
                    mission.completionMessage(),
                    mission.category().getDisplayName(),
                    mission.difficulty().name(),
                    missionIcon(mission),
                    prerequisiteLabels(mission),
                    requirements(player, quest, mission),
                    mission.rewards().stream().map(TerminalMissionReward::of).toList());
        }

        private static List<TerminalMissionRequirement> requirements(Player player, QuestData quest, Mission mission) {
            List<TerminalMissionRequirement> requirements = new ArrayList<>();
            for (Mission.ItemProgress progress : mission.getItemProgress(player)) {
                requirements.add(TerminalMissionRequirement.item(
                        progress.item(), progress.have(), progress.need(), progress.satisfied()));
            }
            for (Mission.BlockRequirement requirement : mission.requiredBlocks()) {
                int have = quest.getBlockPlaceCount(requirement.blockId());
                int need = Math.max(1, requirement.count());
                requirements.add(TerminalMissionRequirement.custom(
                        requirement.displayName(),
                        Math.min(have, need) + "/" + need + " placed",
                        blockIcon(requirement.blockId(), requirement.displayName()),
                        Math.min(have, need),
                        need,
                        have >= need));
            }
            for (Mission.EntityKillRequirement requirement : mission.requiredEntityKills()) {
                int have = quest.getEntityKills(requirement.entityType());
                int need = Math.max(1, requirement.count());
                requirements.add(TerminalMissionRequirement.custom(
                        requirement.displayName(),
                        Math.min(have, need) + "/" + need + " neutralized",
                        missionIcon(mission),
                        Math.min(have, need),
                        need,
                        have >= need));
            }
            for (Mission.LocationRequirement requirement : mission.requiredLocations()) {
                boolean visited = quest.hasVisitedLocation(requirement.locationType(), requirement.locationId());
                requirements.add(TerminalMissionRequirement.custom(
                        requirement.displayName(),
                        visited ? "Archived" : "Not archived",
                        missionIcon(mission),
                        visited ? 1 : 0,
                        1,
                        visited));
            }
            for (Mission.EquipmentRequirement requirement : mission.requiredEquipment()) {
                ItemStack equipped = player == null ? ItemStack.EMPTY : player.getItemBySlot(requirement.slot());
                boolean wearing = !equipped.isEmpty() && equipped.getItem() == requirement.item().getItem();
                requirements.add(TerminalMissionRequirement.custom(
                        requirement.displayName(),
                        wearing ? "Equipped" : "Not equipped",
                        requirement.item(),
                        wearing ? 1 : 0,
                        1,
                        wearing));
            }
            if (requirements.isEmpty()) {
                boolean complete = cheapMissionSatisfied(player, quest, mission);
                requirements.add(TerminalMissionRequirement.custom(
                        mission.objectiveText(),
                        complete ? "Objective complete" : "Progress tracked by synced route state",
                        missionIcon(mission),
                        complete ? 1 : 0,
                        1,
                        complete));
            }
            return requirements;
        }

        private static List<String> prerequisiteLabels(Mission mission) {
            List<String> labels = new ArrayList<>();
            for (String prerequisite : mission.getPrerequisites()) {
                Mission prereq = MissionRegistry.getMissionById(prerequisite);
                labels.add(prereq == null ? prerequisite : prereq.objectiveText());
            }
            if (mission.isPathRestricted()) {
                labels.add("Nexus path: " + mission.requiredPath().name());
            }
            return labels;
        }

        private static boolean isCurrentMission(QuestData quest, Mission mission) {
            Mission current = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
            return current != null && current.id().equals(mission.id());
        }

        private static boolean cheapMissionSatisfied(Player player, QuestData quest, Mission mission) {
            if (player == null || quest == null || mission == null) {
                return false;
            }
            if (quest.isMissionCompleted(mission.id())) {
                return true;
            }
            if (mission.validatesRequiredItems() && !mission.hasRequiredItems(player)) {
                return false;
            }
            if (mission.hasBlockRequirements() && !mission.hasRequiredBlocks(player)) {
                return false;
            }
            for (Mission.EntityKillRequirement requirement : mission.requiredEntityKills()) {
                if (quest.getEntityKills(requirement.entityType()) < requirement.count()) {
                    return false;
                }
            }
            for (Mission.LocationRequirement requirement : mission.requiredLocations()) {
                if (!quest.hasVisitedLocation(requirement.locationType(), requirement.locationId())) {
                    return false;
                }
            }
            return mission.hasRequiredEquipment(player) && mission.hasAnyRequirements();
        }

        private static float cheapProgress(
                Player player,
                QuestData quest,
                Mission mission,
                QuestData.MissionStatus status,
                boolean completeNow) {
            if (status == QuestData.MissionStatus.COMPLETED || completeNow) {
                return 1.0F;
            }
            if (player == null || quest == null || mission == null) {
                return 0.0F;
            }

            float total = 0.0F;
            int entries = 0;
            if (mission.validatesRequiredItems()) {
                for (Mission.ItemProgress progress : mission.getItemProgress(player)) {
                    int need = Math.max(1, progress.need());
                    total += Math.min(1.0F, progress.have() / (float) need);
                    entries++;
                }
            }
            for (Mission.BlockRequirement requirement : mission.requiredBlocks()) {
                int need = Math.max(1, requirement.count());
                total += Math.min(1.0F, quest.getBlockPlaceCount(requirement.blockId()) / (float) need);
                entries++;
            }
            for (Mission.EntityKillRequirement requirement : mission.requiredEntityKills()) {
                int need = Math.max(1, requirement.count());
                total += Math.min(1.0F, quest.getEntityKills(requirement.entityType()) / (float) need);
                entries++;
            }
            for (Mission.LocationRequirement requirement : mission.requiredLocations()) {
                total += quest.hasVisitedLocation(requirement.locationType(), requirement.locationId()) ? 1.0F : 0.0F;
                entries++;
            }
            for (Mission.EquipmentRequirement requirement : mission.requiredEquipment()) {
                ItemStack equipped = player.getItemBySlot(requirement.slot());
                total += !equipped.isEmpty() && equipped.getItem() == requirement.item().getItem() ? 1.0F : 0.0F;
                entries++;
            }
            return entries == 0 ? 0.0F : total / entries;
        }
    }

    private static final class AshfallSideOpsProvider implements TerminalMissionProvider {
        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(
                    id("ashfall_side_ops"),
                    "ECHO-7 SIGNAL LEADS",
                    "Optional lore, recon, and world-context objectives.",
                    20,
                    0xFFFFD166,
                    true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            QuestData quest = player == null ? new QuestData() : QuestData.get(player);
            boolean nexusChoice = player != null && PostNexusData.get(player).hasMadeChoice();
            return List.of(
                    sideOp("crash_blackbox_signal", "Crash Blackbox Signal", "PERIMETER SIGNALS", 0, 1,
                            "Recover the first telemetry thread from the drop-pod perimeter.",
                            quest.isMissionCompleted("secure_crash_outpost"), Items.RECOVERY_COMPASS),
                    sideOp("wasteland_surface_report", "Wasteland Surface Report", "PERIMETER SIGNALS", 0, 2,
                            "Classify the ash-dirt surface and low vegetation around the starter basin.",
                            quest.isMissionCompleted("forage_wasteland_food")
                                    || quest.isMissionCompleted("plant_mutated_sapling"), Items.DIRT),
                    sideOp("supply_recovery", "Supply Recovery", "OUTPOST SUPPORT", 0, 3,
                            "Recover a starter supply cache and prove the pod route can restock itself.",
                            anyCompleted(quest, "forage_wasteland_food", "stockpile_rations"), Items.CHEST),
                    sideOp("scavenger_run", "Scavenger Run", "OUTPOST SUPPORT", 0, 4,
                            "Sweep the first safe salvage lane without pushing the main route forward.",
                            anyCompleted(quest, "loot_survivor_cache", "secure_crash_outpost"), Items.IRON_PICKAXE),
                    sideOp("first_ruin_signature", "First Ruin Signature", "Route Records", 1, 1,
                            "Use scanner-led exploration to prove the first ruin route exists.",
                            quest.isMissionCompleted("scan_first_poi"), Items.COMPASS),
                    sideOp("poi_field_atlas", "POI Field Atlas", "Route Records", 1, 2,
                            "Catalogue at least three POI profiles through the Route Map atlas.",
                            quest.getDiscoveredPOICount() >= 3, Items.MAP),
                    sideOp("echo_intercepts", "Echo Intercepts", "Route Records", 1, 3,
                            "Capture crossband fragments around the first scanned route marker.",
                            anyCompleted(quest, "scan_first_poi", "first_faction_contact"), Items.WRITABLE_BOOK),
                    sideOp("lost_prospector", "Lost Prospector", "Route Records", 1, 4,
                            "Trace a missing field operator through nearby route signatures.",
                            quest.getDiscoveredPOICount() >= 2, Items.SPYGLASS),
                sideOp("faction_crossband", "Faction Crossband", "Human Signals", 2, 1,
                        "Identify the first living social signal in the wasteland.",
                        quest.hasVisitedLocation("special", "faction_contact:any")
                                || quest.isMissionCompleted("first_faction_contact"), Items.EMERALD),
                sideOp("radwarden_containment_thread", "Radwarden Containment Thread", "Human Signals", 2, 2,
                        "Optional dossier: trace the Radwarden read on radiation, decon, and route discipline.",
                        quest.hasVisitedLocation("special", "faction_contact:radwarden_compact")
                                || quest.isMissionCompleted("contact_radwarden_compact")
                                || quest.isMissionCompleted("complete_radwarden_contract"), Items.SHIELD),
                sideOp("crashbreak_salvage_thread", "Crashbreak Salvage Thread", "Human Signals", 2, 3,
                        "Optional dossier: trace the Crashbreak read on wreck routes, salvage value, and survivor trade.",
                        quest.hasVisitedLocation("special", "faction_contact:crashbreak_salvage")
                                || quest.isMissionCompleted("contact_crashbreak_salvage")
                                || quest.isMissionCompleted("complete_crashbreak_contract"), Items.IRON_PICKAXE),
                sideOp("broken_convoy", "Broken Convoy", "Human Signals", 2, 4,
                        "Recover a convoy manifest before the salvage lane goes quiet.",
                        anyCompleted(quest, "first_faction_contact", "complete_first_faction_task",
                                "complete_crashbreak_contract"), Items.MINECART),
                sideOp("sporebound_adaptation_thread", "Sporebound Adaptation Thread", "Human Signals", 2, 4,
                        "Optional dossier: trace the Sporebound read on mutation, medicine, and living hazard work.",
                        quest.hasVisitedLocation("special", "faction_contact:sporebound_sanctum")
                                || quest.isMissionCompleted("contact_sporebound_sanctum")
                                || quest.isMissionCompleted("complete_sporebound_contract"), Items.RED_MUSHROOM),
                sideOp("medical_aid", "Medical Aid", "Human Signals", 2, 5,
                        "Audit med-bay readiness for stranded survivors and route injuries.",
                        anyCompleted(quest, "build_field_med_bay", "use_field_med_bay"), Items.POTION),
                sideOp("drone_memory_sweep", "Drone Memory Sweep", "Machine Echoes", 3, 1,
                        "Recover a drone-linked field memory without changing drone progression.",
                        quest.isMissionCompleted("recover_drone_intel"), Items.OBSERVER),
                    sideOp("drone_salvage", "Drone Salvage", "Machine Echoes", 3, 2,
                            "Recover drone hull fragments and classify the witness telemetry.",
                            anyCompleted(quest, "repair_echo_drone", "recover_drone_intel"), Items.OBSERVER),
                    sideOp("relictech_cache", "RelicTech Cache", "Machine Echoes", 3, 3,
                            "Recover a sealed RelicTech cache near the outpost approach.",
                            anyCompleted(quest, "scan_first_poi", "recover_data_log"), Items.AMETHYST_SHARD),
                    sideOp("relictech_data_salvage", "RelicTech Data Salvage", "Machine Echoes", 3, 4,
                            "Salvage encrypted data before the outpost route corrupts the payload.",
                            anyCompleted(quest, "recover_data_log", "survey_reactor_ruin"), Items.COPPER_INGOT),
                    sideOp("stabilize_power_grid_node", "Stabilize Power Grid Node", "Grid Recovery", 3, 5,
                            "Bring a power node into safe telemetry range for the route planner.",
                            anyCompleted(quest, "route_power_cable", "activate_power_node"), Items.REDSTONE),
                    sideOp("recover_arcana_archive", "Recover Arcana Archive", "Anomaly Chain", 4, 0,
                            "Recover an Arcana-side archive fragment linked to RelicTech residue.",
                            anyCompleted(quest, "recover_data_log", "survey_reactor_ruin",
                                    "build_research_lab"), Items.ENCHANTED_BOOK),
                    sideOp("guardian_signal_lattice", "Guardian Signal Lattice", "Anomaly Chain", 4, 1,
                            "Resolve the first buried guardian signal and classify the node lattice.",
                            anyCompleted(quest,
                                    "neutralize_plains_warlord",
                                    "neutralize_city_ruin_stalker",
                                    "neutralize_industrial_juggernaut",
                                    "neutralize_toxic_hive_matriarch",
                                    "neutralize_crash_zone_colossus",
                                    "neutralize_radiation_behemoth",
                                    "neutralize_cryogenic_overseer",
                                    "neutralize_nexus_scar_avatar"), Items.NETHER_STAR),
                    sideOp("nexus_choice_record", "Nexus Choice Record", "Nexus", 5, 1,
                            "Archive the final path commitment once RESTORE, DESTROY, or CONTROL is chosen.",
                            nexusChoice, Items.END_CRYSTAL),
                    sideOp("orbital_quarantine_echo", "Orbital Quarantine Echo", "ECHO-0", 6, 1,
                            "Preview the post-Nexus orbital thread that explains why the fall began above Earth.",
                            anyCompleted(quest, "restore_epilogue", "destroy_epilogue", "control_epilogue"),
                            Items.END_PORTAL_FRAME));
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            if (!sideOpPath(missionId)) {
                return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                        "LOCKED", "Signal lead not found.", "No optional record is available for this signal.", List.of());
            }
            QuestData quest = player == null ? new QuestData() : QuestData.get(player);
            boolean unlocked = sideOpUnlocked(quest, missionId);
            boolean complete = unlocked && sideOpComplete(player, quest, missionId);
            boolean archived = complete && intelArchived(player, missionId);
            TerminalMissionStatus status = archived
                    ? TerminalMissionStatus.COMPLETED
                    : complete ? TerminalMissionStatus.CLAIMABLE
                    : unlocked ? TerminalMissionStatus.UNLOCKED
                    : TerminalMissionStatus.VIEW_ONLY;
            return new TerminalMissionSnapshot(missionId, status, complete ? 1.0F : 0.0F,
                    archived ? "ARCHIVED" : complete ? "READY" : unlocked ? "OPTIONAL" : "VIEW",
                    unlocked ? "" : "Complete " + objectiveName(anchorPath(missionId)) + " first.",
                    archived ? "Intel record archived."
                            : complete ? "Intel recovered. Archive the side record to unlock contextual discoveries."
                            : unlocked ? "Complete the optional field condition to recover this intel."
                            : "Signal lead remains locked behind the owning route mission.",
                    complete && !archived
                            ? List.of(TerminalMissionAction.enabled("archive_intel", "Archive Intel"))
                            : List.of());
        }

        @Override
        public TerminalMissionRole role(Player player, TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot) {
            return TerminalMissionRole.OPTIONAL;
        }

        @Override
        public Optional<TerminalMissionRoutePlacement> routePlacement(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            int order = definition == null ? 0 : definition.missionOrder();
            return Optional.of(TerminalMissionRoutePlacement.optional(routePhase(definition == null ? null : definition.id()),
                    routeOrder(definition == null ? null : definition.id(), order)));
        }

        @Override
        public List<Identifier> routePrerequisites(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            String gate = routeGatePath(definition == null ? null : definition.id());
            return gate.isBlank() ? List.of() : List.of(id(gate));
        }

        @Override
        public Optional<Identifier> routeAnchor(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            String anchor = definition == null ? "" : anchorPath(definition.id());
            return anchor.isBlank() ? Optional.empty() : Optional.of(id(anchor));
        }

        @Override
        public List<TerminalMissionIntelUnlock> intelUnlocks(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return definition == null ? List.of() : sideOpIntel(definition.id());
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            if (!"archive_intel".equals(actionId) || !sideOpPath(missionId)) {
                return false;
            }
            QuestData quest = QuestData.get(player);
            if (!sideOpUnlocked(quest, missionId) || !sideOpComplete(player, quest, missionId)) {
                return false;
            }
            return applyIntelUnlocks(player, missionId);
        }

        private static TerminalMissionDefinition sideOp(String path, String title, String phase,
                int phaseOrder, int order, String briefing, boolean complete, Item icon) {
            return new TerminalMissionDefinition(
                    id(path),
                    id("ashfall_side_ops"),
                    phase.toLowerCase(Locale.ROOT).replace(' ', '_'),
                    phase,
                    phaseOrder,
                    order,
                    title,
                    briefing,
                    briefing,
                    "Field Recon",
                    "Recon",
                    safeItemStack(icon),
                    List.of(),
                    List.of(TerminalMissionRequirement.custom(title, complete ? "Archived" : "Pending",
                            safeItemStack(icon), complete ? 1 : 0, 1, complete)),
                    List.of(TerminalMissionReward.text("Archive Context",
                            "Adds tactical field context only; required route progress and caches stay unchanged.")));
        }

        private static ItemStack safeItemStack(Item item) {
            if (!EchoCoreServices.itemStackComponentsBound()) {
                return ItemStack.EMPTY;
            }
            try {
                return new ItemStack(item);
            } catch (RuntimeException | LinkageError ignored) {
                return ItemStack.EMPTY;
            }
        }

        private static boolean anyCompleted(QuestData quest, String... missionIds) {
            for (String missionId : missionIds) {
                if (quest.isMissionCompleted(missionId)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean sideOpPath(Identifier missionId) {
            return missionId != null && !anchorPath(missionId).isBlank();
        }

        private static String anchorPath(Identifier missionId) {
            if (missionId == null) {
                return "";
            }
            return switch (missionId.getPath()) {
                case "crash_blackbox_signal", "wasteland_surface_report",
                        "supply_recovery", "scavenger_run" -> "secure_crash_outpost";
                case "first_ruin_signature" -> "craft_portable_scanner";
                case "poi_field_atlas", "echo_intercepts", "lost_prospector",
                        "relictech_cache", "relictech_data_salvage" -> "scan_first_poi";
                case "faction_crossband", "radwarden_containment_thread", "crashbreak_salvage_thread",
                        "broken_convoy", "sporebound_adaptation_thread", "medical_aid" -> "first_faction_contact";
                case "drone_memory_sweep", "drone_salvage" -> "repair_echo_drone";
                case "stabilize_power_grid_node" -> "activate_power_node";
                case "recover_arcana_archive", "guardian_signal_lattice" -> "deploy_stationary_scanner";
                case "nexus_choice_record", "orbital_quarantine_echo" -> "reach_decision";
                default -> "";
            };
        }

        private static String routeGatePath(Identifier missionId) {
            if (missionId == null) {
                return "";
            }
            return switch (missionId.getPath()) {
                case "crash_blackbox_signal", "wasteland_surface_report",
                        "supply_recovery", "scavenger_run" -> "";
                default -> anchorPath(missionId);
            };
        }

        private static int routePhase(Identifier missionId) {
            if (missionId == null) {
                return 15;
            }
            return switch (missionId.getPath()) {
                case "crash_blackbox_signal" -> 0;
                case "wasteland_surface_report", "supply_recovery", "scavenger_run" -> 1;
                case "first_ruin_signature", "poi_field_atlas", "echo_intercepts",
                        "lost_prospector", "relictech_cache", "relictech_data_salvage" -> 7;
                case "faction_crossband", "radwarden_containment_thread", "crashbreak_salvage_thread",
                        "broken_convoy", "sporebound_adaptation_thread", "medical_aid",
                        "drone_memory_sweep", "drone_salvage", "stabilize_power_grid_node" -> 8;
                case "recover_arcana_archive", "guardian_signal_lattice" -> 12;
                case "nexus_choice_record" -> 14;
                case "orbital_quarantine_echo" -> 15;
                default -> 15;
            };
        }

        private static int routeOrder(Identifier missionId, int fallback) {
            if (missionId == null) {
                return fallback;
            }
            return switch (missionId.getPath()) {
                case "crash_blackbox_signal" -> 1;
                case "wasteland_surface_report" -> 2;
                case "supply_recovery" -> 3;
                case "scavenger_run" -> 4;
                case "first_ruin_signature" -> 10;
                case "poi_field_atlas" -> 20;
                case "echo_intercepts" -> 30;
                case "lost_prospector" -> 40;
                case "faction_crossband" -> 10;
                case "radwarden_containment_thread" -> 20;
                case "crashbreak_salvage_thread" -> 30;
                case "broken_convoy" -> 35;
                case "sporebound_adaptation_thread" -> 40;
                case "medical_aid" -> 45;
                case "drone_memory_sweep" -> 50;
                case "drone_salvage" -> 55;
                case "relictech_cache" -> 60;
                case "relictech_data_salvage" -> 65;
                case "stabilize_power_grid_node" -> 70;
                case "recover_arcana_archive" -> 5;
                case "guardian_signal_lattice", "nexus_choice_record", "orbital_quarantine_echo" -> 10;
                default -> fallback;
            };
        }

        private static boolean sideOpUnlocked(QuestData quest, Identifier missionId) {
            String gate = routeGatePath(missionId);
            return gate.isBlank() || quest.isMissionCompleted(gate);
        }

        private static boolean sideOpComplete(Player player, QuestData quest, Identifier missionId) {
            return switch (missionId.getPath()) {
                case "crash_blackbox_signal" -> quest.isMissionCompleted("secure_crash_outpost");
                case "wasteland_surface_report" -> quest.isMissionCompleted("secure_crash_outpost")
                        || quest.hasVisitedLocation("biome", "the_wasteland")
                        || quest.hasVisitedLocation("biome", "echoashfallprotocol:the_wasteland");
                case "supply_recovery" -> anyCompleted(quest, "forage_wasteland_food", "stockpile_rations");
                case "scavenger_run" -> anyCompleted(quest, "loot_survivor_cache", "secure_crash_outpost");
                case "first_ruin_signature" -> quest.getDiscoveredPOICount() >= 1;
                case "poi_field_atlas" -> quest.getDiscoveredPOICount() >= 3;
                case "echo_intercepts" -> anyCompleted(quest, "scan_first_poi", "first_faction_contact");
                case "lost_prospector" -> quest.getDiscoveredPOICount() >= 2;
                case "faction_crossband" -> quest.hasVisitedLocation("special", "faction_contact:any")
                        || quest.isMissionCompleted("first_faction_contact");
                case "radwarden_containment_thread" -> quest.hasVisitedLocation("special", "faction_contact:radwarden_compact")
                        || quest.isMissionCompleted("contact_radwarden_compact")
                        || quest.isMissionCompleted("complete_radwarden_contract");
                case "crashbreak_salvage_thread" -> quest.hasVisitedLocation("special", "faction_contact:crashbreak_salvage")
                        || quest.isMissionCompleted("contact_crashbreak_salvage")
                        || quest.isMissionCompleted("complete_crashbreak_contract");
                case "broken_convoy" -> anyCompleted(quest, "first_faction_contact", "complete_first_faction_task",
                        "complete_crashbreak_contract");
                case "sporebound_adaptation_thread" -> quest.hasVisitedLocation("special", "faction_contact:sporebound_sanctum")
                        || quest.isMissionCompleted("contact_sporebound_sanctum")
                        || quest.isMissionCompleted("complete_sporebound_contract");
                case "medical_aid" -> anyCompleted(quest, "build_field_med_bay", "use_field_med_bay");
                case "drone_memory_sweep" -> quest.hasVisitedLocation("special", "drone:intel_recovered")
                        || quest.isMissionCompleted("recover_drone_intel");
                case "drone_salvage" -> anyCompleted(quest, "repair_echo_drone", "recover_drone_intel");
                case "relictech_cache" -> anyCompleted(quest, "scan_first_poi", "recover_data_log");
                case "relictech_data_salvage" -> anyCompleted(quest, "recover_data_log", "survey_reactor_ruin");
                case "stabilize_power_grid_node" -> anyCompleted(quest, "route_power_cable", "activate_power_node");
                case "recover_arcana_archive" -> anyCompleted(quest, "recover_data_log", "survey_reactor_ruin",
                        "build_research_lab");
                case "guardian_signal_lattice" -> quest.getCompletedMissionIds().stream()
                        .anyMatch(id -> id.startsWith("neutralize_"));
                case "nexus_choice_record", "orbital_quarantine_echo" ->
                        player != null && PostNexusData.get(player).hasMadeChoice();
                default -> false;
            };
        }

        private static String objectiveName(String missionId) {
            if (missionId == null || missionId.isBlank()) {
                return "the owning route mission";
            }
            Mission mission = MissionRegistry.getMissionById(missionId);
            return mission == null ? missionId : mission.objectiveText();
        }

        private static List<TerminalMissionIntelUnlock> sideOpIntel(Identifier missionId) {
            if (missionId == null) {
                return List.of();
            }
            return switch (missionId.getPath()) {
                case "crash_blackbox_signal" -> List.of(
                        archive("ashfall_progression_manual", "Protocol Roadmap Rules",
                                "Crash telemetry explains why the route begins with a protected outpost."),
                        route("ashfall_active_protocol", "Ashfall Active Protocol",
                                "Adds the first route record signal beside the survival spine."));
                case "wasteland_surface_report" -> List.of(
                        archive("ashfall_survival_manual", "Ashfall Survival Manual",
                                "Clarifies wasteland surface rules, field shelter, and route pressure."),
                        discovery(AshfallDiscoveryProvider.biomeId("the_wasteland"), "Wasteland Biome",
                                "Reveals the starter basin as a hostile field region."));
                case "supply_recovery" -> List.of(
                        archive("ashfall_supply_recovery", "Supply Recovery",
                                "Adds cache discipline, ration margin, and field restock context to the route."));
                case "scavenger_run" -> List.of(
                        archive("ashfall_scavenger_run", "Scavenger Run",
                                "Frames early salvage as an optional supply loop, not a required route gate."),
                        poi(AshfallDiscoveryProvider.structureId("survivor_cache"), "Survivor Cache",
                                "Links nearby salvage to a concrete recovery site."));
                case "first_ruin_signature" -> List.of(
                        archive("ashfall_poi_atlas", "POI Field Atlas",
                                "Turns the first scanner hit into route-map context."),
                        poi(AshfallDiscoveryProvider.structureId("survivor_cache"), "Survivor Cache",
                                "Adds a concrete recovery-site lead to the discovery grid."));
                case "poi_field_atlas" -> List.of(
                        archive("ashfall_poi_atlas", "POI Field Atlas",
                                "Explains route profiles, template variants, and scanner cataloging."),
                        poi(AshfallDiscoveryProvider.structureId("survivor_cache"), "Survivor Cache",
                                "Adds a practical cache lead for route planning."));
                case "echo_intercepts" -> List.of(
                        archive("ashfall_echo_intercepts", "Echo Intercepts",
                                "Stores crossband field fragments from the first scanned route marker."));
                case "lost_prospector" -> List.of(
                        archive("ashfall_lost_prospector", "Lost Prospector",
                                "Adds a missing-operator thread to the POI recovery archive."),
                        poi(AshfallDiscoveryProvider.structureId("survivor_cache"), "Prospector Trace",
                                "Tags nearby cache routes as likely operator paths."));
                case "faction_crossband" -> List.of(
                        archive("ashfall_faction_threads", "Faction Threads",
                                "Frames Ashfall factions as optional context instead of mandatory standing."),
                        faction("radwarden_compact", "Radwarden Compact",
                                "Reveals the containment faction signal without changing reputation."),
                        faction("crashbreak_salvage", "Crashbreak Salvage",
                                "Reveals the salvage faction signal without changing reputation."),
                        faction("sporebound_sanctum", "Sporebound Sanctum",
                                "Reveals the adaptation faction signal without changing reputation."));
                case "radwarden_containment_thread" -> List.of(
                        archive("ashfall_faction_threads", "Radwarden Dossier",
                                "Adds containment doctrine to the field archive."),
                        faction("radwarden_compact", "Radwarden Compact",
                                "Reveals containment intel without touching contracts or standing."));
                case "crashbreak_salvage_thread" -> List.of(
                        archive("ashfall_faction_threads", "Crashbreak Dossier",
                                "Adds salvage doctrine to the field archive."),
                        faction("crashbreak_salvage", "Crashbreak Salvage",
                                "Reveals salvage intel without touching contracts or standing."));
                case "broken_convoy" -> List.of(
                        archive("ashfall_broken_convoy", "Broken Convoy",
                                "Adds convoy salvage, route security, and missing-manifest context."),
                        faction("crashbreak_salvage", "Crashbreak Convoy Read",
                                "Marks the convoy thread as optional salvage intelligence."));
                case "sporebound_adaptation_thread" -> List.of(
                        archive("ashfall_faction_threads", "Sporebound Dossier",
                                "Adds adaptation doctrine to the field archive."),
                        faction("sporebound_sanctum", "Sporebound Sanctum",
                                "Reveals biological intel without touching contracts or standing."));
                case "medical_aid" -> List.of(
                        archive("ashfall_medical_aid", "Medical Aid",
                                "Adds survivor aid and med-bay readiness notes to the route archive."));
                case "drone_memory_sweep" -> List.of(
                        archive("ashfall_drone_manual", "Drone Command Primer",
                                "Recasts drone support as recovered witness telemetry."));
                case "drone_salvage" -> List.of(
                        archive("ashfall_drone_salvage", "Drone Salvage",
                                "Links drone hull fragments to ECHO-7 witness telemetry."));
                case "relictech_cache" -> List.of(
                        archive("ashfall_relictech_cache", "RelicTech Cache",
                                "Adds sealed-cache context for the first RelicTech outpost lane."),
                        discovery(id("echorelictech/discoveries/ashfall_relic_first_contact"),
                                "RelicTech First Contact",
                                "Connects Ashfall route intel to the RelicTech discovery bridge."));
                case "relictech_data_salvage" -> List.of(
                        archive("ashfall_relic_field_note", "RelicTech Data Salvage",
                                "Restores the small RelicTech field-note side mission in the route line."),
                        discovery(id("echorelictech/discoveries/ashfall_relay_signal_fragment"),
                                "RelicTech Relay Fragment",
                                "Links salvaged data to the safe handoff record."));
                case "stabilize_power_grid_node" -> List.of(
                        archive("ashfall_power_grid_node", "Stabilize Power Grid Node",
                                "Adds grid-node recovery notes to the operational archive."),
                        route("ashfall_active_protocol", "Ashfall Active Protocol",
                                "Keeps the grid node tied to the active route spine."));
                case "recover_arcana_archive" -> List.of(
                        archive("ashfall_arcana_archive", "Recover Arcana Archive",
                                "Adds Arcana anomaly residue to the route context without requiring Arcana progression."));
                case "guardian_signal_lattice" -> List.of(
                        archive("ashfall_threat_manual", "Biome Guardian Dossier",
                                "Connects guardian nodes to the Nexus route lattice."),
                        discovery(AshfallDiscoveryProvider.guardianId("plains_warlord"), "Guardian Lattice",
                                "Adds the first buried guardian signal to contextual discoveries."));
                case "nexus_choice_record" -> List.of(
                        archive("ashfall_nexus_manual", "Nexus Path Protocols",
                                "Archives the selected RESTORE, DESTROY, or CONTROL path as route context."));
                case "orbital_quarantine_echo" -> List.of(
                        archive("ashfall_nexus_manual", "Orbital Handshake",
                                "Previews ECHO-0 quarantine context for optional post-Nexus routing."));
                default -> List.of();
            };
        }

        private static TerminalMissionIntelUnlock archive(String path, String title, String summary) {
            return TerminalMissionIntelUnlock.archive(id(path), title, summary);
        }

        private static TerminalMissionIntelUnlock route(String path, String title, String summary) {
            return TerminalMissionIntelUnlock.route(id(path), title, summary);
        }

        private static TerminalMissionIntelUnlock discovery(Identifier target, String title, String summary) {
            return TerminalMissionIntelUnlock.discovery(target, title, summary);
        }

        private static TerminalMissionIntelUnlock faction(String factionPath, String title, String summary) {
            return TerminalMissionIntelUnlock.faction(id("faction/" + factionPath), title, summary);
        }

        private static TerminalMissionIntelUnlock poi(Identifier target, String title, String summary) {
            return TerminalMissionIntelUnlock.poi(target, title, summary);
        }

        private static boolean intelArchived(Player player, Identifier missionId) {
            if (player == null) {
                return false;
            }
            for (TerminalMissionIntelUnlock unlock : sideOpIntel(missionId)) {
                if (!intelUnlocked(player, unlock)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean intelUnlocked(Player player, TerminalMissionIntelUnlock unlock) {
            if (unlock.kind() == TerminalMissionIntelKind.ARCHIVE) {
                return EchoCoreServices.isArchiveUnlocked(player, unlock.id().toString());
            }
            Identifier target = unlock.kind() == TerminalMissionIntelKind.ROUTE
                    ? EchoCoreServices.routeDiscoveryId(unlock.id())
                    : unlock.id();
            return EchoCoreServices.hasDiscoveredFeature(player, target);
        }

        private static boolean applyIntelUnlocks(ServerPlayer player, Identifier missionId) {
            boolean changed = false;
            for (TerminalMissionIntelUnlock unlock : sideOpIntel(missionId)) {
                if (unlock.kind() == TerminalMissionIntelKind.ARCHIVE) {
                    if (!EchoCoreServices.isArchiveUnlocked(player, unlock.id().toString())) {
                        EchoCoreServices.unlockArchive(player, unlock.id().toString());
                        changed = true;
                    }
                    continue;
                }
                Identifier target = unlock.kind() == TerminalMissionIntelKind.ROUTE
                        ? EchoCoreServices.routeDiscoveryId(unlock.id())
                        : unlock.id();
                changed |= EchoCoreServices.discoverFeature(player, target);
            }
            return changed;
        }
    }

    private static boolean safeComplete(Mission mission, Player player) {
        try {
            return mission != null && player != null && mission.isComplete(player);
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.warn("Ashfall terminal mission completion check failed for {}.",
                    mission == null ? "<null>" : mission.id(), exception);
            return false;
        }
    }

    private static ItemStack missionIcon(Mission mission) {
        ItemStack objective = mission.getObjectiveItem();
        if (!objective.isEmpty()) {
            return objective.copy();
        }
        if (mission.objectiveIcon() != null) {
            Item item = BuiltInRegistries.ITEM.getOptional(mission.objectiveIcon()).orElse(Items.AIR);
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return switch (mission.category()) {
            case SURVIVAL -> new ItemStack(Items.CAMPFIRE);
            case CRAFTING -> new ItemStack(Items.CRAFTING_TABLE);
            case EXPLORATION -> new ItemStack(Items.COMPASS);
            case COMBAT -> new ItemStack(Items.IRON_SWORD);
            case TECH -> new ItemStack(Items.REDSTONE);
            case STORY -> new ItemStack(Items.WRITABLE_BOOK);
        };
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

    private static void registerFieldManualEntries() {
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_survival_manual"),
                "Outpost Survival",
                "Ashfall Survival Manual",
                "OPEN",
                List.of(
                        "Gridfall did not leave a normal wilderness behind. The safe route is water, shelter, food, filters, and a powered outpost before distance.",
                        "Mission records are tactical briefings, not shortcuts. ECHO-7 can show the road ahead, but field validation still confirms every turn-in and reward.",
                        "Hazards stack quickly: toxic air, radiation, cold, acid contact, storms, and Nexus anomalies each need a different countermeasure."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_weather_protocols"),
                "Outpost Survival",
                "Weather Event Protocols",
                "OPEN",
                List.of(
                        "ECHO-7 treats weather as a route condition. Radiation storms, acid rain, blackouts, ash storms, cryo fronts, and Nexus surges each change what a safe expedition looks like.",
                        "Counters are practical: cover, filters, reserve power, clean water, heat, RadAway, scrubber pockets, or distance from unstable sources.",
                        "The Vitals Scan tab shows the active event, survival impact, counter guidance, and survived-event counts for route planning."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_systems_manual"),
                "Machine Systems",
                "Field Systems Primer",
                "OPEN",
                List.of(
                        "Recycler, generator, purifier, grinder, refiner, research, cable, and power blocks are the spine of recovery. They turn ruin into repeatable survival.",
                        "Research points unlock perks and schematic categories, while rare schematics let recovered knowledge catch up to field pressure.",
                        "Machine mission turn-ins verify field progress but do not bypass ECHO validation."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_progression_manual"),
                "Protocol Flow",
                "Protocol Roadmap Rules",
                "OPEN",
                List.of(
                        "Locked missions are visible for planning, but completion, rewards, and phase advancement remain gated by QuestData.",
                        "Public beta route guidance ends at Orbital handoff. Legacy Nexus save state remains readable, but Ashfall no longer owns the finale terminal."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_threat_manual"),
                "Threat Dossier",
                "Guardian Threat Dossier",
                "OPEN",
                List.of(
                        "Eight active biome guardian signals hold the old grid in place. Each one is tied to Radwarden containment, Crashbreak salvage, or Sporebound anomaly interpretation.",
                        "Each guardian has a unique threat profile, owner faction thread, surface entrance, arena route, defender set, and reward bundle. Scan, prepare, descend, and leave nothing unresolved."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_drone_manual"),
                "Machine Systems",
                "Companion Drone Protocols",
                "OPEN",
                List.of(
                        "Drone commands are tactical orders routed through the terminal and confirmed against live field state.",
                        "Follow unlocks immediately. Scout and light require partial repairs, combat and scavenge require operational integrity, and patrol requires enhanced integrity.",
                        "The drone is not only gear. It is ECHO-7's moving witness, and the Scout Drone backup keeps that witness active when the companion shell is gone."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_nexus_manual"),
                "Recovered Lore",
                "Nexus Path Interface",
                "OPEN",
                List.of(
                        "The Nexus Core can commit RESTORE, DESTROY, or CONTROL once the guardian chain, Warfront relays, countermeasure siege, and five Power Nodes are resolved.",
                        "The chosen path is mirrored through Echo Core services so addon chapters can react without owning Ashfall's route state.",
                        "When Nexus Protocol is installed, its chapter owns late-game terminal handoff and completion milestones."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_world_regions"),
                "Recovered Lore",
                "Wasteland Region Field Notes",
                "OPEN",
                List.of(
                        "The Wasteland is open ash-dirt and low cover: sparse survival pressure with early grass tufts as vegetation, not a healed living surface.",
                        "Crash zones carry slag, cables, twisted metal, and scorched debris. City and industrial belts become denser, sharper, and more useful for salvage.",
                        "Toxic swamps, radiation flats, cryogenic ridges, and Nexus scars each announce their danger through terrain language before the terminal confirms the hazard."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_poi_atlas"),
                "Signal Logs",
                "POI Field Atlas",
                "OPEN",
                List.of(
                        "The Route Map POI Atlas groups every surface template signal under the scanner profile that owns its gameplay identity.",
                        "Individual ruins are not separate save objectives. ECHO tracks the route profile, then lists template variants for field recognition.",
                        "Muted template rows mean the parent route has not been scanned yet."),
                false));
        TerminalArchiveRegistry.register(new TerminalArchiveEntry(
                id("ashfall_faction_threads"),
                "Recovered Lore",
                "Faction Signal Threads",
                "OPEN",
                List.of(
                        "Three Ashfall factions report through Echo Core: Radwarden Compact containment crews, Crashbreak Salvage route builders, and Sporebound Sanctum anomaly interpreters.",
                        "Faction work is not separate from the main route: contacts, contracts, services, and standing all feed the same synced Echo Core record.",
                        "Orbital lanes mirror those same three pressures after the Nexus choice reaches orbit: Radwarden containment, Crashbreak salvage, and Sporebound anomaly reading."),
                false));
    }
}
