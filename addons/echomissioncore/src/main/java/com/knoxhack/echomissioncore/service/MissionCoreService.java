package com.knoxhack.echomissioncore.service;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionProgressView;
import com.echoplatform.echocore.api.mission.IMissionService;
import com.echoplatform.echocore.api.mission.IObjectiveView;
import com.echoplatform.echocore.api.mission.IRewardView;
import com.echoplatform.echocore.api.mission.MissionActionView;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.MissionRepeatPolicy;
import com.echoplatform.echocore.api.mission.MissionRewardClaimMode;
import com.echoplatform.echocore.api.mission.MissionRuntimeBus;
import com.echoplatform.echocore.api.mission.MissionRuntimeEvent;
import com.echoplatform.echocore.api.mission.MissionStatus;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echomissioncore.EchoMissionCore;
import com.knoxhack.echomissioncore.content.MissionCoreNativeJsonBootstrap;
import com.knoxhack.echomissioncore.storage.MissionPlayerData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MissionCoreService implements IMissionService {
    public static final MissionCoreService INSTANCE = new MissionCoreService();

    private final Map<Identifier, MissionChapterDefinition> chapters = new LinkedHashMap<>();
    private final Map<Identifier, MissionDefinition> missions = new LinkedHashMap<>();
    private final Map<Identifier, String> chapterSources = new LinkedHashMap<>();
    private final Map<Identifier, String> missionSources = new LinkedHashMap<>();
    private final Map<String, Map<Identifier, Set<Identifier>>> hookCoverage = new LinkedHashMap<>();
    private final List<String> validationWarnings = new ArrayList<>();
    private final Set<String> jsonOwnedNamespaces = new LinkedHashSet<>();
    private boolean builtInContentRegistered;
    private static final Set<String> MIGRATED_TERMINAL_SOURCES = Set.of(
            "echoashfallprotocol",
            "echoagriculturereclamation",
            "echoarmory",
            "echoblockworks",
            "echoindustrialnexus",
            "echologisticsnetwork",
            "echoconvoyprotocol",
            "echoholomap",
            "echoindex",
            "echolens",
            "echomultiblockcore",
            "echoorbitalremnants",
            "echonexusprotocol",
            "echoblackboxprotocol",
            "echorecovery",
            "echorelictech",
            "echosignalos",
            "echostationfall");

    private MissionCoreService() {
    }

    @Override
    public boolean available() {
        return true;
    }

    public synchronized void registerBuiltInContent() {
        if (builtInContentRegistered) {
            return;
        }
        builtInContentRegistered = true;
        EchoCoreServices.replayMissionContent(this);
        EchoCoreServices.invalidateIndexRecipes("mission built-in content registered");
    }

    public synchronized void replaceJsonContent(List<MissionChapterDefinition> jsonChapters, List<MissionDefinition> jsonMissions) {
        validationWarnings.clear();
        Set<String> jsonNamespaces = representedNamespaces(jsonChapters, jsonMissions);
        removeSourceContent("json");
        removeNamespaceContent(jsonNamespaces);
        jsonOwnedNamespaces.clear();
        jsonOwnedNamespaces.addAll(jsonNamespaces);
        replaceSourceContent("json", jsonChapters, jsonMissions);
        EchoCoreServices.replayMissionContent(this, jsonNamespaces);
        EchoCoreServices.invalidateIndexRecipes("mission content changed");
    }

    @Override
    public synchronized void replaceSourceContent(
            String source,
            List<MissionChapterDefinition> sourceChapters,
            List<MissionDefinition> sourceMissions) {
        String safeSource = safeSource(source);
        removeSourceContent(safeSource);
        if (sourceChapters != null) {
            for (MissionChapterDefinition chapter : sourceChapters) {
                registerChapter(safeSource, chapter);
            }
        }
        if (sourceMissions != null) {
            Set<Identifier> knownMissionIds = new LinkedHashSet<>(missions.keySet());
            for (MissionDefinition mission : sourceMissions) {
                if (mission != null) {
                    knownMissionIds.add(mission.id());
                }
            }
            Set<Identifier> seenSourceMissions = new LinkedHashSet<>();
            for (MissionDefinition mission : sourceMissions) {
                if (mission == null) {
                    continue;
                }
                if (!seenSourceMissions.add(mission.id())) {
                    warnValidation("Duplicate MissionCore mission id " + mission.id()
                            + " from source " + safeSource + " ignored.");
                    continue;
                }
                if (!chapters.containsKey(mission.chapterId())) {
                    warnValidation("MissionCore mission " + mission.id() + " from " + safeSource
                            + " skipped because chapter " + mission.chapterId() + " is not registered.");
                    continue;
                }
                Optional<Identifier> missingPrerequisite = mission.prerequisites().stream()
                        .filter(prerequisite -> !knownMissionIds.contains(prerequisite))
                        .findFirst();
                if (missingPrerequisite.isPresent()) {
                    warnValidation("MissionCore mission " + mission.id() + " from " + safeSource
                            + " skipped because prerequisite " + missingPrerequisite.get() + " is not registered.");
                    continue;
                }
                registerMission(safeSource, mission);
            }
        }
        hookCoverage.remove(safeSource);
        EchoCoreServices.invalidateIndexRecipes("mission source " + safeSource + " changed");
    }

    @Override
    public synchronized void unregisterSource(String source) {
        String safeSource = safeSource(source);
        removeSourceContent(safeSource);
        hookCoverage.remove(safeSource);
        EchoCoreServices.invalidateIndexRecipes("mission source " + safeSource + " unregistered");
    }

    @Override
    public synchronized void registerChapter(String source, MissionChapterDefinition chapter) {
        if (chapter == null) {
            return;
        }
        String safeSource = safeSource(source);
        if (chapter.id() == null || chapter.id().getPath().isBlank()) {
            warnValidation("MissionCore chapter from " + safeSource + " has an invalid id and was ignored.");
            return;
        }
        if (jsonNamespaceOwns(safeSource, chapter.id())) {
            return;
        }
        MissionChapterDefinition previous = chapters.get(chapter.id());
        String previousSource = chapterSources.get(chapter.id());
        if (previous != null && !previous.equals(chapter) && !safeSource.equals(previousSource)) {
            warnValidation("MissionCore chapter " + chapter.id() + " from " + safeSource
                    + " conflicts with existing source " + previousSource + " and was ignored.");
            return;
        }
        chapters.put(chapter.id(), chapter);
        chapterSources.put(chapter.id(), safeSource);
        if (previous != null && !previous.equals(chapter)) {
            EchoMissionCore.LOGGER.debug("Mission chapter {} replaced by source {}.", chapter.id(), safeSource);
        }
    }

    @Override
    public synchronized void registerMission(String source, MissionDefinition mission) {
        if (mission == null) {
            return;
        }
        String safeSource = safeSource(source);
        if (!isMissionStructurallyValid(safeSource, mission)) {
            return;
        }
        if (jsonNamespaceOwns(safeSource, mission.id()) || jsonNamespaceOwns(safeSource, mission.chapterId())) {
            return;
        }
        if (!chapters.containsKey(mission.chapterId())) {
            warnValidation("MissionCore mission " + mission.id() + " from " + safeSource
                    + " skipped because chapter " + mission.chapterId() + " is not registered.");
            return;
        }
        MissionDefinition previous = missions.get(mission.id());
        String previousSource = missionSources.get(mission.id());
        if (previous != null && !previous.equals(mission) && !safeSource.equals(previousSource)) {
            warnValidation("MissionCore mission " + mission.id() + " from " + safeSource
                    + " conflicts with existing source " + previousSource + " and was ignored.");
            return;
        }
        missions.put(mission.id(), mission);
        missionSources.put(mission.id(), safeSource);
        if (previous != null && !previous.equals(mission)) {
            EchoMissionCore.LOGGER.debug("Mission {} replaced by source {}.", mission.id(), safeSource);
        }
    }

    @Override
    public synchronized Optional<MissionChapterDefinition> chapter(Identifier chapterId) {
        ensureNativeJsonContentLoaded("chapter");
        return Optional.ofNullable(chapters.get(chapterId));
    }

    @Override
    public synchronized Optional<MissionDefinition> missionDefinition(Identifier missionId) {
        ensureNativeJsonContentLoaded("mission_definition");
        return Optional.ofNullable(missions.get(missionId));
    }

    @Override
    public synchronized List<MissionChapterDefinition> chapters() {
        ensureNativeJsonContentLoaded("chapters");
        return chapters.values().stream()
                .sorted(Comparator.comparingInt(MissionChapterDefinition::order).thenComparing(chapter -> chapter.id().toString()))
                .toList();
    }

    @Override
    public synchronized List<MissionDefinition> missionDefinitions() {
        ensureNativeJsonContentLoaded("mission_definitions");
        return missions.values().stream()
                .sorted(Comparator.comparingInt(MissionDefinition::phaseOrder)
                        .thenComparingInt(MissionDefinition::missionOrder)
                        .thenComparing(mission -> mission.id().toString()))
                .toList();
    }

    public synchronized List<String> validationWarnings() {
        ensureNativeJsonContentLoaded("validation_warnings");
        return List.copyOf(validationWarnings);
    }

    public synchronized Map<String, Integer> sourceCounts() {
        ensureNativeJsonContentLoaded("source_counts");
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String source : missionSources.values()) {
            counts.merge(source, 1, Integer::sum);
        }
        return Map.copyOf(counts);
    }

    @Override
    public synchronized void registerHookCoverage(String source, Identifier missionId, Identifier objectiveTarget) {
        if (missionId == null || objectiveTarget == null) {
            return;
        }
        String safeSource = safeSource(source);
        hookCoverage.computeIfAbsent(safeSource, ignored -> new LinkedHashMap<>())
                .computeIfAbsent(missionId, ignored -> new LinkedHashSet<>())
                .add(objectiveTarget);
    }

    @Override
    public synchronized Map<String, String> missionHookCoverageBySource() {
        Map<String, String> coverage = new LinkedHashMap<>();
        for (String source : sourceCounts().keySet()) {
            coverage.put(source, hookCoverageMode(source));
        }
        return Map.copyOf(coverage);
    }

    public synchronized List<String> validateContent() {
        ensureNativeJsonContentLoaded("validate_content");
        List<String> warnings = new ArrayList<>(validationWarnings);
        for (MissionDefinition mission : missions.values()) {
            if (!chapters.containsKey(mission.chapterId())) {
                warnings.add("Mission " + mission.id() + " references missing chapter " + mission.chapterId() + ".");
            }
            for (Identifier prerequisite : mission.prerequisites()) {
                if (!missions.containsKey(prerequisite)) {
                    warnings.add("Mission " + mission.id() + " references missing prerequisite " + prerequisite + ".");
                }
            }
        }
        Map<String, String> coverage = missionHookCoverageBySource();
        for (String source : MIGRATED_TERMINAL_SOURCES) {
            if (!coverage.containsKey(source)) {
                continue;
            }
            String mode = coverage.get(source);
            if ("adapter-state".equals(mode)) {
                warnings.add("MissionCore source " + source
                        + " is migrated through adapter-state only; no direct objective hook coverage is registered.");
            } else if ("mixed".equals(mode)) {
                warnings.add("MissionCore source " + source
                        + " has mixed hook coverage; some migrated objectives still rely on adapter-state fallback.");
            }
        }
        return List.copyOf(warnings);
    }

    public synchronized void clearForTests() {
        chapters.clear();
        missions.clear();
        chapterSources.clear();
        missionSources.clear();
        hookCoverage.clear();
        validationWarnings.clear();
        builtInContentRegistered = false;
        jsonOwnedNamespaces.clear();
    }

    private void ensureNativeJsonContentLoaded(String reason) {
        MissionCoreNativeJsonBootstrap.ensureLoaded(reason);
    }

    @Override
    public List<IMissionProgressView> missions(Player player) {
        return missionDefinitions().stream()
                .map(mission -> view(player, mission))
                .filter(view -> view.status() != MissionStatus.LOCKED || !view.definition().hidden())
                .toList();
    }

    @Override
    public List<IMissionProgressView> missions(Player player, Identifier chapterId) {
        return missionDefinitions().stream()
                .filter(mission -> mission.chapterId().equals(chapterId))
                .map(mission -> view(player, mission))
                .filter(view -> view.status() != MissionStatus.LOCKED || !view.definition().hidden())
                .toList();
    }

    @Override
    public Optional<IMissionProgressView> mission(Player player, Identifier missionId) {
        return missionDefinition(missionId).map(definition -> view(player, definition));
    }

    @Override
    public boolean startMission(ServerPlayer player, Identifier missionId) {
        MissionDefinition definition = missionDefinition(missionId).orElse(null);
        if (player == null || definition == null || !isUnlocked(player, definition)) {
            return false;
        }
        MissionPlayerData data = MissionPlayerData.get(player);
        MissionPlayerData.MissionState state = data.state(missionId);
        if (state.status() == MissionStatus.LOCKED || state.status() == MissionStatus.UNLOCKED) {
            state.status(MissionStatus.ACTIVE);
            data.trackMission(missionId);
            MissionPlayerData.saveAndSync(player, data);
            MissionRuntimeBus.fire(MissionRuntimeEvent.of(
                    MissionRuntimeEvent.MISSION_STARTED, player, missionId, null, null, 1, Map.of()));
        } else if (isTerminalStatusComplete(state.status()) && definition.repeatPolicy() == MissionRepeatPolicy.REPEATABLE) {
            state.clearProgressAndRewards();
            state.status(MissionStatus.ACTIVE);
            data.trackMission(missionId);
            MissionPlayerData.saveAndSync(player, data);
            MissionRuntimeBus.fire(MissionRuntimeEvent.of(
                    MissionRuntimeEvent.MISSION_STARTED, player, missionId, null, null, state.repeatCompletions() + 1, Map.of("repeat", "true")));
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean completeMission(ServerPlayer player, Identifier missionId) {
        MissionDefinition definition = missionDefinition(missionId).orElse(null);
        if (player == null || definition == null || !isUnlocked(player, definition)) {
            return false;
        }
        return completeMission(player, definition, true);
    }

    @Override
    public boolean claimReward(ServerPlayer player, Identifier missionId) {
        MissionDefinition definition = missionDefinition(missionId).orElse(null);
        if (player == null || definition == null) {
            return false;
        }
        MissionPlayerData data = MissionPlayerData.get(player);
        MissionPlayerData.MissionState state = data.state(missionId);
        MissionStatus currentStatus = status(player, definition);
        if (currentStatus != MissionStatus.CLAIMABLE && currentStatus != MissionStatus.COMPLETED) {
            return false;
        }
        boolean claimedAny = false;
        for (RewardDefinition reward : definition.rewards()) {
            if (reward.claimMode() != MissionRewardClaimMode.CLAIMABLE || state.isRewardClaimed(reward.id())) {
                continue;
            }
            giveReward(player, reward);
            state.claimReward(reward.id());
            claimedAny = true;
            MissionRuntimeBus.fire(MissionRuntimeEvent.of(
                    MissionRuntimeEvent.REWARD_CLAIMED, player, missionId, null, reward.id(), 1, reward.metadata()));
        }
        if (claimedAny) {
            state.status(MissionStatus.CLAIMED);
            MissionPlayerData.saveAndSync(player, data);
        }
        return claimedAny;
    }

    @Override
    public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
        if (player == null || missionId == null || actionId == null) {
            return false;
        }
        String normalized = actionId.toLowerCase(Locale.ROOT);
        MissionDefinition definition = missionDefinition(missionId).orElse(null);
        if (definition == null) {
            return false;
        }
        return switch (normalized) {
            case "start", "track" -> startMission(player, missionId);
            case "claim", "claim_reward" -> claimReward(player, missionId);
            case "complete", "turn_in" -> {
                if (!canComplete(player, definition)) {
                    player.sendSystemMessage(Component.literal("[ECHO-7] Mission requirements are not complete."), true);
                    yield false;
                }
                yield completeMission(player, definition, true);
            }
            default -> definition.actionHandler().handle(player, definition, actionId);
        };
    }

    @Override
    public boolean recordObjective(
            ServerPlayer player,
            MissionObjectiveType type,
            Identifier target,
            int amount,
            Map<String, String> context) {
        if (player == null || amount <= 0) {
            return false;
        }
        MissionObjectiveType safeType = type == null ? MissionObjectiveType.CUSTOM : type;
        boolean changedAny = false;
        for (MissionDefinition mission : missionDefinitions()) {
            if (!isUnlocked(player, mission) || isTerminalStatusComplete(status(player, mission))) {
                continue;
            }
            MissionPlayerData data = MissionPlayerData.get(player);
            MissionPlayerData.MissionState state = data.state(mission.id());
            boolean changedMission = false;
            for (ObjectiveDefinition objective : mission.objectives()) {
                if (!matches(objective, safeType, target, context)) {
                    continue;
                }
                int progress = state.addObjectiveProgress(objective.id(), amount, objective.required());
                changedMission = true;
                changedAny = true;
                MissionRuntimeBus.fire(MissionRuntimeEvent.of(
                        MissionRuntimeEvent.OBJECTIVE_PROGRESSED,
                        player,
                        mission.id(),
                        objective.id(),
                        null,
                        progress,
                        context == null ? Map.of() : context));
                if (progress >= objective.required()) {
                    state.revealObjective(objective.id());
                }
            }
            if (changedMission && canComplete(player, mission)) {
                completeMission(player, mission, true);
            } else if (changedMission) {
                MissionPlayerData.saveAndSync(player, data);
            }
        }
        return changedAny;
    }

    @Override
    public String debugState(Player player, Identifier missionId) {
        if (missionId == null) {
            return "MissionCore: " + missions.size() + " missions, " + chapters.size() + " chapters.";
        }
        MissionDefinition definition = missionDefinition(missionId).orElse(null);
        if (definition == null) {
            return "Mission not found: " + missionId;
        }
        IMissionProgressView view = view(player, definition);
        return "Mission " + missionId
                + " status=" + view.status()
                + " progress=" + Math.round(view.progress() * 100.0F) + "%"
                + " objectives=" + view.objectives().stream()
                .map(objective -> objective.id().getPath() + "=" + objective.progress() + "/" + objective.required())
                .toList()
                + " rewards=" + view.rewards().stream()
                .map(reward -> reward.id().getPath() + (reward.claimed() ? ":claimed" : reward.claimable() ? ":claimable" : ":pending"))
                .toList();
    }

    public boolean forceProgress(ServerPlayer player, Identifier missionId, Identifier objectiveId, int amount) {
        MissionDefinition definition = missionDefinition(missionId).orElse(null);
        if (player == null || definition == null) {
            return false;
        }
        MissionPlayerData data = MissionPlayerData.get(player);
        MissionPlayerData.MissionState state = data.state(missionId);
        ObjectiveDefinition objective = definition.objectives().stream()
                .filter(candidate -> candidate.id().equals(objectiveId))
                .findFirst()
                .orElse(null);
        if (objective == null) {
            return false;
        }
        int progress = state.addObjectiveProgress(objective.id(), Math.max(1, amount), objective.required());
        if (progress >= objective.required()) {
            state.revealObjective(objective.id());
        }
        MissionPlayerData.saveAndSync(player, data);
        MissionRuntimeBus.fire(MissionRuntimeEvent.of(
                MissionRuntimeEvent.OBJECTIVE_PROGRESSED, player, missionId, objectiveId, null, amount, Map.of("debug", "true")));
        if (canComplete(player, definition)) {
            completeMission(player, definition, true);
        }
        return true;
    }

    private boolean completeMission(ServerPlayer player, MissionDefinition definition, boolean callHandler) {
        MissionPlayerData data = MissionPlayerData.get(player);
        MissionPlayerData.MissionState state = data.state(definition.id());
        if (isTerminalStatusComplete(state.status())) {
            return false;
        }
        for (ObjectiveDefinition objective : definition.objectives()) {
            state.setObjectiveProgress(objective.id(), objective.required());
            state.revealObjective(objective.id());
        }
        boolean hasClaimableRewards = false;
        for (RewardDefinition reward : definition.rewards()) {
            if (reward.claimMode() == MissionRewardClaimMode.IMMEDIATE && !state.isRewardClaimed(reward.id())) {
                giveReward(player, reward);
                state.claimReward(reward.id());
                MissionRuntimeBus.fire(MissionRuntimeEvent.of(
                        MissionRuntimeEvent.REWARD_CLAIMED, player, definition.id(), null, reward.id(), 1, reward.metadata()));
            } else if (reward.claimMode() == MissionRewardClaimMode.CLAIMABLE && !state.isRewardClaimed(reward.id())) {
                hasClaimableRewards = true;
            }
        }
        state.status(hasClaimableRewards ? MissionStatus.CLAIMABLE : definition.rewards().isEmpty()
                ? MissionStatus.COMPLETED
                : MissionStatus.CLAIMED);
        state.incrementRepeatCompletions();
        state.lastCompletedGameTime(player.level().getGameTime());
        if (callHandler) {
            definition.completionHandler().onCompleted(player, definition);
        }
        applyTerminalIntelUnlocks(player, definition);
        MissionPlayerData.saveAndSync(player, data);
        MissionRuntimeBus.fire(MissionRuntimeEvent.of(
                MissionRuntimeEvent.MISSION_COMPLETED, player, definition.id(), null, null, 1, definition.metadata()));
        fireNewChapterUnlocks(player);
        return true;
    }

    private void applyTerminalIntelUnlocks(ServerPlayer player, MissionDefinition definition) {
        Map<String, String> metadata = definition.metadata();
        for (Identifier archive : metadataIdentifiers(metadata.get("terminal_intel_archives"),
                definition.id().getNamespace())) {
            EchoCoreServices.unlockArchive(player, archive.toString());
        }
        for (Identifier route : metadataIdentifiers(metadata.get("terminal_intel_routes"),
                definition.id().getNamespace())) {
            EchoCoreServices.discoverFeature(player, EchoCoreServices.routeDiscoveryId(route));
        }
        for (String key : List.of("terminal_intel_discoveries", "terminal_intel_factions", "terminal_intel_pois")) {
            for (Identifier discovery : metadataIdentifiers(metadata.get(key), definition.id().getNamespace())) {
                EchoCoreServices.discoverFeature(player, discovery);
            }
        }
    }

    private static List<Identifier> metadataIdentifiers(String value, String fallbackNamespace) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Identifier> ids = new ArrayList<>();
        for (String raw : value.split(",")) {
            metadataIdentifier(raw, fallbackNamespace).ifPresent(id -> {
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            });
        }
        return List.copyOf(ids);
    }

    private static Optional<Identifier> metadataIdentifier(String raw, String fallbackNamespace) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim();
        Identifier parsed = value.contains(":") ? Identifier.tryParse(value) : null;
        if (parsed == null && fallbackNamespace != null && !fallbackNamespace.isBlank()) {
            parsed = Identifier.tryParse(fallbackNamespace + ":" + value);
        }
        if (parsed == null) {
            parsed = Identifier.tryParse(value);
        }
        return Optional.ofNullable(parsed);
    }

    private void fireNewChapterUnlocks(ServerPlayer player) {
        for (MissionChapterDefinition chapter : chapters()) {
            boolean hasUnlocked = missions(player, chapter.id()).stream()
                    .anyMatch(view -> view.status() != MissionStatus.LOCKED);
            MissionPlayerData data = MissionPlayerData.get(player);
            if (hasUnlocked && data.markUnlockedChapter(chapter.id())) {
                MissionPlayerData.saveAndSync(player, data);
                MissionRuntimeBus.fire(MissionRuntimeEvent.of(
                        MissionRuntimeEvent.CHAPTER_UNLOCKED, player, chapter.id(), null, null, 1, Map.of()));
            }
        }
    }

    private IMissionProgressView view(Player player, MissionDefinition definition) {
        MissionStatus status = status(player, definition);
        boolean completeNow = canComplete(player, definition);
        List<IObjectiveView> objectives = objectiveViews(player, definition);
        List<IRewardView> rewards = rewardViews(player, definition, status);
        float progress = calculateProgress(objectives, definition, player);
        return new MissionProgressView(
                definition,
                definition.id(),
                definition.chapterId(),
                status,
                progress,
                label(status),
                status == MissionStatus.LOCKED ? unlockReason(definition) : "",
                actionHint(status, definition, completeNow),
                objectives,
                rewards,
                actions(player, status, definition, completeNow));
    }

    private MissionStatus status(Player player, MissionDefinition definition) {
        if (player == null) {
            return MissionStatus.VIEW_ONLY;
        }
        MissionPlayerData data = MissionPlayerData.get(player);
        MissionPlayerData.MissionState state = data.stateIfPresent(definition.id());
        if (state != null && isTerminalStatusComplete(state.status())) {
            return state.status();
        }
        boolean unlocked = isUnlocked(player, definition);
        Optional<MissionStatus> externalStatus = definition.statusRule().status(player, definition);
        if (externalStatus.isPresent()) {
            MissionStatus imported = externalStatus.get();
            if (!unlocked) {
                return MissionStatus.LOCKED;
            }
            if (isTerminalStatusComplete(imported)) {
                importExternalStatus(player, definition, imported);
            }
            return imported;
        }
        if (!unlocked) {
            return MissionStatus.LOCKED;
        }
        return state == null ? MissionStatus.UNLOCKED : state.status();
    }

    private void importExternalStatus(Player player, MissionDefinition definition, MissionStatus status) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        MissionPlayerData data = MissionPlayerData.get(serverPlayer);
        MissionPlayerData.MissionState state = data.state(definition.id());
        if (isTerminalStatusComplete(state.status())) {
            return;
        }
        state.status(status);
        for (ObjectiveDefinition objective : definition.objectives()) {
            state.setObjectiveProgress(objective.id(), objective.required());
        }
        if (status == MissionStatus.CLAIMED) {
            for (RewardDefinition reward : definition.rewards()) {
                state.claimReward(reward.id());
            }
        }
        MissionPlayerData.saveAndSync(serverPlayer, data);
    }

    private boolean canComplete(Player player, MissionDefinition definition) {
        if (player == null) {
            return false;
        }
        MissionPlayerData data = MissionPlayerData.get(player);
        MissionPlayerData.MissionState state = data.stateIfPresent(definition.id());
        boolean objectivesComplete = !definition.objectives().isEmpty()
                && definition.objectives().stream().allMatch(objective ->
                state != null && state.objectiveProgress(objective.id()) >= objective.required());
        return objectivesComplete || definition.completionRule().isComplete(player, definition);
    }

    private boolean isUnlocked(Player player, MissionDefinition definition) {
        if (definition.hidden() && definition.prerequisites().isEmpty()) {
            return false;
        }
        for (Identifier prerequisite : definition.prerequisites()) {
            MissionStatus prerequisiteStatus = missionDefinition(prerequisite)
                    .map(prerequisiteDefinition -> status(player, prerequisiteDefinition))
                    .orElse(MissionStatus.LOCKED);
            if (!isTerminalStatusComplete(prerequisiteStatus)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTerminalStatusComplete(MissionStatus status) {
        return status == MissionStatus.COMPLETED || status == MissionStatus.COMPLETE
                || status == MissionStatus.CLAIMABLE || status == MissionStatus.CLAIMED;
    }

    private List<IObjectiveView> objectiveViews(Player player, MissionDefinition definition) {
        MissionPlayerData.MissionState state = player == null ? null : MissionPlayerData.get(player).stateIfPresent(definition.id());
        boolean dynamicComplete = player != null && definition.completionRule().isComplete(player, definition);
        return definition.objectives().stream()
                .filter(objective -> !objective.hidden() || state != null && state.isObjectiveRevealed(objective.id()) || dynamicComplete)
                .<IObjectiveView>map(objective -> {
                    int progress = dynamicComplete ? objective.required() : state == null ? 0 : state.objectiveProgress(objective.id());
                    return new ObjectiveView(
                            objective.id(),
                            objective.type(),
                            objective.label(),
                            objective.detail(),
                            objective.icon(),
                            Math.min(progress, objective.required()),
                            objective.required(),
                            progress >= objective.required(),
                            objective.hidden(),
                            objective.criteria());
                })
                .toList();
    }

    private List<IRewardView> rewardViews(Player player, MissionDefinition definition, MissionStatus status) {
        MissionPlayerData.MissionState state = player == null ? null : MissionPlayerData.get(player).stateIfPresent(definition.id());
        boolean complete = isTerminalStatusComplete(status);
        return definition.rewards().stream()
                .<IRewardView>map(reward -> new RewardView(
                        reward.id(),
                        reward.claimMode(),
                        reward.stack(),
                        reward.label(),
                        reward.detail(),
                        complete && (status == MissionStatus.CLAIMABLE || status == MissionStatus.COMPLETED)
                                && reward.claimMode() == MissionRewardClaimMode.CLAIMABLE
                                && (state == null || !state.isRewardClaimed(reward.id())),
                        state != null && state.isRewardClaimed(reward.id()),
                        reward.metadata()))
                .toList();
    }

    private static float calculateProgress(List<IObjectiveView> objectives, MissionDefinition definition, Player player) {
        if (definition.completionRule().isComplete(player, definition)) {
            return 1.0F;
        }
        if (objectives.isEmpty()) {
            return 0.0F;
        }
        int have = 0;
        int need = 0;
        for (IObjectiveView objective : objectives) {
            have += Math.min(objective.progress(), objective.required());
            need += objective.required();
        }
        return need <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, have / (float) need));
    }

    private static List<MissionActionView> actions(Player player, MissionStatus status, MissionDefinition definition, boolean completeNow) {
        List<MissionActionView> actions = new ArrayList<>();
        if (status == MissionStatus.UNLOCKED) {
            actions.add(MissionActionView.enabled("start", "Track"));
            if (completeNow) {
                actions.add(MissionActionView.enabled("complete", "Turn In"));
            }
        } else if (status == MissionStatus.ACTIVE) {
            actions.add(completeNow
                    ? MissionActionView.enabled("complete", "Turn In")
                    : MissionActionView.disabled("complete", "Turn In", "Requirements incomplete"));
        } else if (status == MissionStatus.COMPLETED || status == MissionStatus.CLAIMABLE) {
            boolean hasClaimable = definition.rewards().stream().anyMatch(reward -> reward.claimMode() == MissionRewardClaimMode.CLAIMABLE);
            if (hasClaimable) {
                actions.add(MissionActionView.enabled("claim", "Claim"));
            }
        } else if (status == MissionStatus.CLAIMED && definition.repeatPolicy() == MissionRepeatPolicy.REPEATABLE) {
            actions.add(MissionActionView.enabled("start", "Repeat"));
        }
        List<MissionActionView> customActions = definition.actionProvider().actions(player, definition, status, completeNow);
        if (customActions != null && !customActions.isEmpty()) {
            Set<String> actionIds = actions.stream()
                    .map(MissionActionView::id)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            for (MissionActionView action : customActions) {
                if (action != null && actionIds.add(action.id())) {
                    actions.add(action);
                }
            }
        }
        return actions;
    }

    private static String label(MissionStatus status) {
        return switch (status) {
            case LOCKED -> "Locked";
            case UNLOCKED, AVAILABLE -> "Available";
            case ACTIVE -> "Active";
            case COMPLETED, COMPLETE -> "Completed";
            case CLAIMABLE -> "Reward Ready";
            case CLAIMED -> "Claimed";
            case FAILED -> "Failed";
            case VIEW_ONLY -> "View Only";
        };
    }

    private static String actionHint(MissionStatus status, MissionDefinition definition, boolean completeNow) {
        return switch (status) {
            case LOCKED -> unlockReason(definition);
            case UNLOCKED, AVAILABLE -> completeNow ? "Turn in the completed objective." : "Track this objective.";
            case ACTIVE -> completeNow ? "Turn in the completed objective." : definition.objectives().isEmpty() ? definition.briefing() : definition.objectives().get(0).label();
            case COMPLETED, COMPLETE -> "Mission complete.";
            case CLAIMABLE -> "Claim the pending reward cache.";
            case CLAIMED -> "Reward claimed.";
            case FAILED -> "Mission failed.";
            case VIEW_ONLY -> definition.briefing();
        };
    }

    private static String unlockReason(MissionDefinition definition) {
        if (definition.prerequisites().isEmpty()) {
            return definition.hidden() ? "Find the hidden objective in the field." : "Unlock the previous route step.";
        }
        Identifier first = definition.prerequisites().get(0);
        String label = readableId(first.getPath());
        if (definition.prerequisites().size() == 1) {
            return "Complete " + label + " first.";
        }
        int remaining = definition.prerequisites().size() - 1;
        return "Complete " + label + " and " + remaining + " other prerequisite" + (remaining == 1 ? "." : "s.");
    }

    private static String readableId(String path) {
        if (path == null || path.isBlank()) {
            return "the previous route step";
        }
        StringBuilder label = new StringBuilder();
        for (String word : path.replace('/', '_').split("_")) {
            if (word.isBlank()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                label.append(word.substring(1));
            }
        }
        return label.length() == 0 ? "the previous route step" : label.toString();
    }

    private static boolean matches(
            ObjectiveDefinition objective,
            MissionObjectiveType type,
            Identifier target,
            Map<String, String> context) {
        if (objective.type() != type && objective.type() != MissionObjectiveType.CUSTOM) {
            return false;
        }
        String expected = objective.criteria().getOrDefault("target", "");
        if (expected.isBlank()) {
            expected = objective.criteria().getOrDefault("id", "");
        }
        if (expected.isBlank() || target == null) {
            return true;
        }
        Set<String> candidates = Set.of(target.toString(), target.getPath());
        return candidates.contains(expected);
    }

    private String hookCoverageMode(String source) {
        Set<Identifier> requiredTargets = objectiveTargetsForSource(source);
        Set<Identifier> coveredTargets = coveredTargetsForSource(source);
        if (requiredTargets.isEmpty()) {
            return coveredTargets.isEmpty() ? "adapter-state" : "mixed";
        }
        int covered = 0;
        for (Identifier required : requiredTargets) {
            if (coveredTargets.contains(required)) {
                covered++;
            }
        }
        if (covered == 0) {
            return "adapter-state";
        }
        return covered >= requiredTargets.size() ? "direct-hooks" : "mixed";
    }

    private Set<Identifier> objectiveTargetsForSource(String source) {
        LinkedHashSet<Identifier> targets = new LinkedHashSet<>();
        for (Map.Entry<Identifier, MissionDefinition> entry : missions.entrySet()) {
            if (!source.equals(missionSources.get(entry.getKey()))) {
                continue;
            }
            for (ObjectiveDefinition objective : entry.getValue().objectives()) {
                String value = objective.criteria().getOrDefault("target", "");
                Identifier target = value.isBlank() ? null : Identifier.tryParse(value);
                if (target != null) {
                    targets.add(target);
                }
            }
        }
        return Set.copyOf(targets);
    }

    private Set<Identifier> coveredTargetsForSource(String source) {
        LinkedHashSet<Identifier> targets = new LinkedHashSet<>();
        Map<Identifier, Set<Identifier>> sourceCoverage = hookCoverage.get(source);
        if (sourceCoverage == null) {
            return Set.of();
        }
        for (Set<Identifier> covered : sourceCoverage.values()) {
            targets.addAll(covered);
        }
        return Set.copyOf(targets);
    }

    private static void giveReward(ServerPlayer player, RewardDefinition reward) {
        ItemStack stack = stackForReward(reward);
        if (stack.isEmpty()) {
            return;
        }
        player.sendSystemMessage(Component.literal("[MissionCore] + " + stack.getCount() + "x ").append(stack.getHoverName()), true);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static ItemStack stackForReward(RewardDefinition reward) {
        if (!reward.stack().isEmpty()) {
            return reward.stack().copy();
        }
        String itemId = reward.metadata().getOrDefault("item", "");
        if (itemId.isBlank()) {
            return ItemStack.EMPTY;
        }
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        int count = 1;
        try {
            count = Math.max(1, Integer.parseInt(reward.metadata().getOrDefault("count", "1")));
        } catch (NumberFormatException ignored) {
            count = 1;
        }
        return new ItemStack(item, count);
    }

    private static String safeSource(String source) {
        return source == null || source.isBlank() ? "unknown" : source;
    }

    private boolean jsonNamespaceOwns(String source, Identifier id) {
        return id != null && !"json".equals(source) && jsonOwnedNamespaces.contains(id.getNamespace());
    }

    private void removeSourceContent(String source) {
        chapterSources.entrySet().removeIf(entry -> {
            if (source.equals(entry.getValue())) {
                chapters.remove(entry.getKey());
                return true;
            }
            return false;
        });
        missionSources.entrySet().removeIf(entry -> {
            if (source.equals(entry.getValue())) {
                missions.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    private void removeNamespaceContent(Set<String> namespaces) {
        if (namespaces == null || namespaces.isEmpty()) {
            return;
        }
        chapterSources.entrySet().removeIf(entry -> {
            if (entry.getKey() != null && namespaces.contains(entry.getKey().getNamespace())) {
                chapters.remove(entry.getKey());
                return true;
            }
            return false;
        });
        missionSources.entrySet().removeIf(entry -> {
            if (entry.getKey() != null && namespaces.contains(entry.getKey().getNamespace())) {
                missions.remove(entry.getKey());
                return true;
            }
            return false;
        });
        hookCoverage.keySet().removeIf(namespaces::contains);
    }

    private static Set<String> representedNamespaces(
            List<MissionChapterDefinition> sourceChapters,
            List<MissionDefinition> sourceMissions) {
        LinkedHashSet<String> namespaces = new LinkedHashSet<>();
        if (sourceChapters != null) {
            for (MissionChapterDefinition chapter : sourceChapters) {
                if (chapter != null && chapter.id() != null) {
                    namespaces.add(chapter.id().getNamespace());
                }
            }
        }
        if (sourceMissions != null) {
            for (MissionDefinition mission : sourceMissions) {
                if (mission != null && mission.id() != null) {
                    namespaces.add(mission.id().getNamespace());
                }
                if (mission != null && mission.chapterId() != null) {
                    namespaces.add(mission.chapterId().getNamespace());
                }
            }
        }
        return Set.copyOf(namespaces);
    }

    private boolean isMissionStructurallyValid(String source, MissionDefinition mission) {
        if (mission.id() == null || mission.id().getPath().isBlank()) {
            warnValidation("MissionCore mission from " + source + " has an invalid id and was ignored.");
            return false;
        }
        Set<Identifier> objectiveIds = new LinkedHashSet<>();
        for (ObjectiveDefinition objective : mission.objectives()) {
            if (objective == null || objective.id() == null || objective.id().getPath().isBlank()) {
                warnValidation("MissionCore mission " + mission.id() + " has an invalid objective id and was ignored.");
                return false;
            }
            if (!objectiveIds.add(objective.id())) {
                warnValidation("MissionCore mission " + mission.id() + " has duplicate objective " + objective.id() + " and was ignored.");
                return false;
            }
        }
        Set<Identifier> rewardIds = new LinkedHashSet<>();
        for (RewardDefinition reward : mission.rewards()) {
            if (reward == null || reward.id() == null || reward.id().getPath().isBlank()) {
                warnValidation("MissionCore mission " + mission.id() + " has an invalid reward id and was ignored.");
                return false;
            }
            if (!rewardIds.add(reward.id())) {
                warnValidation("MissionCore mission " + mission.id() + " has duplicate reward " + reward.id() + " and was ignored.");
                return false;
            }
        }
        return true;
    }

    private void warnValidation(String warning) {
        if (warning == null || warning.isBlank()) {
            return;
        }
        validationWarnings.add(warning);
        if (validationWarnings.size() > 250) {
            validationWarnings.remove(0);
        }
        EchoMissionCore.LOGGER.warn(warning);
    }

    private static String titleFromId(Identifier id) {
        String path = id == null ? "mission_core" : id.getPath();
        String[] parts = path.replace('_', ' ').replace('/', ' ').split(" ");
        StringBuilder title = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return title.isEmpty() ? "MissionCore" : title.toString();
    }

    private record MissionProgressView(
            MissionDefinition definition,
            Identifier id,
            Identifier chapterId,
            MissionStatus status,
            float progress,
            String statusLabel,
            String unlockReason,
            String actionHint,
            List<IObjectiveView> objectives,
            List<IRewardView> rewards,
            List<MissionActionView> actions) implements IMissionProgressView {
    }

    private record ObjectiveView(
            Identifier id,
            MissionObjectiveType type,
            String label,
            String detail,
            ItemStack icon,
            int progress,
            int required,
            boolean complete,
            boolean hidden,
            Map<String, String> criteria) implements IObjectiveView {
        private ObjectiveView {
            icon = icon == null ? ItemStack.EMPTY : icon.copy();
            criteria = Map.copyOf(criteria == null ? Map.of() : criteria);
        }
    }

    private record RewardView(
            Identifier id,
            MissionRewardClaimMode claimMode,
            ItemStack stack,
            String label,
            String detail,
            boolean claimable,
            boolean claimed,
            Map<String, String> metadata) implements IRewardView {
        private RewardView {
            stack = stack == null ? ItemStack.EMPTY : stack.copy();
            metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
        }
    }
}
