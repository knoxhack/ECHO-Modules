package com.knoxhack.echoworldcore.service;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoWorldRuntimeBus;
import com.knoxhack.echocore.api.IWorldRegionService;
import com.knoxhack.echocore.api.WorldContextSnapshot;
import com.knoxhack.echocore.api.WorldCoreValidationIssue;
import com.knoxhack.echocore.api.WorldCoreValidationReport;
import com.knoxhack.echocore.api.WorldDiscoverySource;
import com.knoxhack.echocore.api.WorldHazardDefinition;
import com.knoxhack.echocore.api.WorldHazardSnapshot;
import com.knoxhack.echocore.api.WorldMarker;
import com.knoxhack.echocore.api.WorldMarkerType;
import com.knoxhack.echocore.api.WorldRegionDefinition;
import com.knoxhack.echocore.api.WorldRegionInstance;
import com.knoxhack.echocore.api.WorldRegionType;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echo.adaptercore.EchoNativeBiomeHazardOverlayBridge;
import com.knoxhack.echo.adaptercore.EchoNativeDifficultyApplicationBridge;
import com.knoxhack.echo.adaptercore.EchoNativeHazardTickDamageBridge;
import com.knoxhack.echo.adaptercore.EchoNativeHazardFieldStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeSpawnRuleEventBridge;
import com.knoxhack.echo.adaptercore.EchoNativeSpawnZoneStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStatusEffectApplyBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStatusEffectExpiryBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStatusEffectLoadBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStatusEffectSaveBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStatusEffectStackingBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStructureDiscoveryStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStructurePoiLookupBridge;
import com.knoxhack.echo.adaptercore.EchoNativeStructurePoiMarkerStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWorldCellSampleBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWorldChunkStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWorldHazardTransitionBridge;
import com.knoxhack.echo.adaptercore.EchoNativeWorldRegionTransitionBridge;
import com.knoxhack.echo.adaptercore.EchoWorldContracts;
import com.knoxhack.echoworldcore.Config;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.knoxhack.echoworldcore.world.WorldRegionSavedData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class WorldRegionService implements IWorldRegionService {
    public static final WorldRegionService INSTANCE = new WorldRegionService();
    private static final Set<String> WEATHER_TICKING_HAZARD_IDS = Set.of(
            "echoworldcore:hazard/toxic_air",
            "echoworldcore:hazard/radiation",
            "echoworldcore:hazard/cryo_cold",
            "echoworldcore:hazard/nexus_anomaly",
            "echoworldcore:hazard/orbital_exposure");
    private static final int MAX_VALIDATION_WARNINGS = 64;

    private final Map<Identifier, WorldRegionDefinition> baseRegionDefinitions = new ConcurrentHashMap<>();
    private final Map<Identifier, WorldHazardDefinition> baseHazardDefinitions = new ConcurrentHashMap<>();
    private final Map<Identifier, WorldRegionDefinition> dataRegionDefinitions = new ConcurrentHashMap<>();
    private final Map<Identifier, WorldHazardDefinition> dataHazardDefinitions = new ConcurrentHashMap<>();
    private final Map<Identifier, WorldRegionDefinition> regionDefinitions = new ConcurrentHashMap<>();
    private final Map<Identifier, WorldHazardDefinition> hazardDefinitions = new ConcurrentHashMap<>();
    private final Map<UUID, WorldRegionInstance> lastPrimaryRegion = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoWorldRegionTransitionResult> lastRegionTransitions = new ConcurrentHashMap<>();
    private final Map<UUID, String> activeRegionIds = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> startedRegionMissions = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoWorldHazardTransitionResult> lastHazardTransitions =
            new ConcurrentHashMap<>();
    private final Map<UUID, String> activeHazardIds = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoSpawnRuleEventResult> lastSpawnRuleEvents = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoSpawnZoneStateResult> lastSpawnZoneStates = new ConcurrentHashMap<>();
    private final Map<String, EchoWorldContracts.EchoSpawnZoneStateResult> activeSpawnZoneStateResults =
            new ConcurrentHashMap<>();
    private final Map<String, Integer> activeSpawnPopulations = new ConcurrentHashMap<>();
    private final Map<String, SpawnZoneState> activeSpawnZoneStates = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoStructurePoiLookupResult> lastStructurePoiLookups = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoStructurePoiMarkerStateResult> lastStructurePoiMarkerStates =
            new ConcurrentHashMap<>();
    private final Map<String, EchoWorldContracts.EchoStructurePoiMarkerStateResult> resolvedStructurePoiMarkerStates =
            new ConcurrentHashMap<>();
    private final Map<String, StructurePoiState> resolvedStructurePoiStates = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoStructureDiscoveryStateResult> lastStructureDiscoveryStates =
            new ConcurrentHashMap<>();
    private final Map<String, EchoWorldContracts.EchoStructureDiscoveryStateResult> resolvedStructureDiscoveryStates =
            new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoStatusEffectApplyResult> lastStatusEffectApplications = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, EchoWorldContracts.EchoStatusEffectStackingResult>> lastStatusEffectStackings =
            new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoStatusEffectSaveResult> lastStatusEffectSaves = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoStatusEffectLoadResult> lastStatusEffectLoads = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, EchoWorldContracts.EchoStatusEffectExpiryResult>> lastStatusEffectExpiries =
            new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> activeStatusEffects = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, ActiveStatusEffectState>> activeStatusEffectStates = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, StatusProfileState>> activeStatusProfileStates = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoHazardTickDamageResult> lastHazardTickDamage = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoWorldCellSampleResult> lastWorldCellSamples = new ConcurrentHashMap<>();
    private final Map<String, EchoWorldContracts.EchoWorldCellSampleResult> sampledWorldCells = new ConcurrentHashMap<>();
    private final Map<String, EchoWorldContracts.EchoWorldCellSampleResult> sampledHazardFields = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoBiomeHazardOverlayResult> lastBiomeHazardOverlays = new ConcurrentHashMap<>();
    private final Map<String, EchoWorldContracts.EchoBiomeHazardOverlayResult> sampledBiomeHazardOverlays = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoWorldChunkStateResult> lastWorldChunkStates = new ConcurrentHashMap<>();
    private final Map<String, EchoWorldContracts.EchoWorldChunkStateResult> sampledWorldChunkStates = new ConcurrentHashMap<>();
    private final Map<String, WorldChunkState> sampledWorldChunks = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoHazardFieldStateResult> lastHazardFieldStates = new ConcurrentHashMap<>();
    private final Map<String, EchoWorldContracts.EchoHazardFieldStateResult> sampledHazardFieldStateResults = new ConcurrentHashMap<>();
    private final Map<String, HazardFieldState> sampledHazardFieldStates = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoDifficultyProfile> activeDifficultyProfiles = new ConcurrentHashMap<>();
    private final Map<String, EchoWorldContracts.EchoDifficultyProfile> regionDifficultyProfiles = new ConcurrentHashMap<>();
    private final Map<String, EchoWorldContracts.EchoDifficultyProfileSelectionResult> regionDifficultyProfileSelections =
            new ConcurrentHashMap<>();
    private final Map<UUID, DifficultyApplicationState> activeDifficultyApplicationStates = new ConcurrentHashMap<>();
    private final Map<String, DifficultyApplicationState> regionDifficultyApplicationStates = new ConcurrentHashMap<>();
    private final Map<UUID, EchoWorldContracts.EchoDifficultyApplicationResult> activeDifficultyApplicationResults =
            new ConcurrentHashMap<>();
    private final Map<String, EchoWorldContracts.EchoDifficultyApplicationResult> regionDifficultyApplicationResults =
            new ConcurrentHashMap<>();
    private final Map<UUID, WorldHazardSnapshot> lastHazards = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> emittedRegionDiscoveryEvents = new ConcurrentHashMap<>();
    private final Map<UUID, HazardTickResult> lastHazardTicks = new ConcurrentHashMap<>();
    private volatile List<String> reloadWarnings = List.of();

    public WorldRegionService() {
    }

    @Override
    public boolean registerRegionDefinition(WorldRegionDefinition definition) {
        if (definition == null) {
            return false;
        }
        boolean added = baseRegionDefinitions.putIfAbsent(definition.id(), definition) == null;
        rebuildDefinitionViews();
        if (added) {
            EchoCoreServices.invalidateIndexRecipes("world region definition registered");
        }
        return added;
    }

    @Override
    public List<WorldRegionDefinition> regionDefinitions() {
        return regionDefinitions.values().stream()
                .sorted(Comparator.comparingInt(WorldRegionDefinition::sortOrder)
                        .thenComparing(definition -> definition.id().toString()))
                .toList();
    }

    @Override
    public Optional<WorldRegionDefinition> regionDefinition(Identifier id) {
        return Optional.ofNullable(id == null ? null : regionDefinitions.get(id));
    }

    @Override
    public List<WorldRegionInstance> nearbyRegions(Level level, BlockPos pos, int radius) {
        if (level == null || pos == null) {
            return List.of();
        }
        int safeRadius = safeRadius(radius);
        LinkedHashMap<Identifier, WorldRegionInstance> regions = new LinkedHashMap<>();
        for (WorldRegionDefinition definition : regionDefinitions()) {
            if (definition.biomeBacked() && matchesBiome(level, pos, definition)) {
                WorldRegionInstance instance = instanceForDefinition(level, pos, definition, null);
                regions.put(instance.definitionId(), instance);
            }
        }
        for (WorldMarker marker : nearbyMarkers(level, pos, safeRadius)) {
            WorldRegionInstance instance = instanceForMarker(marker, null);
            regions.putIfAbsent(instance.definitionId(), instance);
        }
        return regions.values().stream()
                .sorted(regionComparator(pos))
                .toList();
    }

    @Override
    public List<WorldRegionInstance> activeRegions(Player player) {
        if (player == null) {
            return List.of();
        }
        return nearbyRegions(player.level(), player.blockPosition(), Config.activeRegionRadius()).stream()
                .map(instance -> instanceWithDiscovery(player, instance))
                .toList();
    }

    public List<WorldRegionInstance> nearbyRegions(Player player, int radius) {
        if (player == null) {
            return List.of();
        }
        return nearbyRegions(player.level(), player.blockPosition(), radius).stream()
                .map(instance -> instanceWithDiscovery(player, instance))
                .toList();
    }

    @Override
    public boolean registerHazardDefinition(WorldHazardDefinition definition) {
        if (definition == null) {
            return false;
        }
        boolean added = baseHazardDefinitions.putIfAbsent(definition.id(), definition) == null;
        rebuildDefinitionViews();
        if (added) {
            EchoCoreServices.invalidateIndexRecipes("world hazard definition registered");
        }
        return added;
    }

    @Override
    public List<WorldHazardDefinition> hazardDefinitions() {
        return hazardDefinitions.values().stream()
                .sorted(Comparator.comparing(definition -> definition.id().toString()))
                .toList();
    }

    @Override
    public Optional<WorldHazardDefinition> hazardDefinition(Identifier id) {
        return Optional.ofNullable(id == null ? null : hazardDefinitions.get(id));
    }

    @Override
    public WorldHazardSnapshot hazardSnapshot(Player player) {
        if (player == null) {
            return WorldHazardSnapshot.nominal();
        }
        LinkedHashSet<Identifier> regionIds = new LinkedHashSet<>();
        LinkedHashSet<Identifier> hazardIds = new LinkedHashSet<>();
        int severity = 0;
        for (WorldRegionInstance region : activeRegions(player)) {
            regionIds.add(region.definitionId());
            for (Identifier hazardId : region.hazardIds()) {
                hazardIds.add(hazardId);
                severity = Math.max(severity, hazardDefinition(hazardId)
                        .map(WorldHazardDefinition::defaultSeverity)
                        .orElse(25));
            }
        }
        if (hazardIds.isEmpty()) {
            return WorldHazardSnapshot.nominal();
        }
        String summary = hazardIds.stream()
                .map(id -> hazardDefinition(id).map(WorldHazardDefinition::displayName).orElse(id.toString()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("Shared world hazard active.");
        if (!regionIds.isEmpty()) {
            summary = summary + " across " + regionIds.size() + " region(s).";
        }
        return new WorldHazardSnapshot(List.copyOf(regionIds), List.copyOf(hazardIds), severity, false, summary);
    }

    @Override
    public WorldMarker revealMarker(ServerPlayer player, WorldMarker marker) {
        if (marker == null) {
            return null;
        }
        MarkerReveal reveal = revealMarkerInternal(player == null ? null : player.level(), marker);
        WorldMarker revealed = reveal.marker();
        if (player != null && revealed != null) {
            if (revealed.regionId() != null && regionDefinitions.containsKey(revealed.regionId())) {
                discoverRegion(player, revealed.regionId(), WorldDiscoverySource.MARKER);
            }
        }
        if (reveal.changed()) {
            EchoWorldRuntimeBus.fireMarkerRevealed(new EchoWorldRuntimeBus.MarkerRevealed(player, revealed));
        }
        return revealed;
    }

    @Override
    public WorldMarker revealMarker(Level level, WorldMarker marker) {
        return revealMarkerInternal(level, marker).marker();
    }

    private MarkerReveal revealMarkerInternal(Level level, WorldMarker marker) {
        if (marker == null) {
            return new MarkerReveal(null, false);
        }
        WorldMarker revealed = marker.discovered(true);
        boolean changed = true;
        if (level instanceof ServerLevel serverLevel) {
            changed = WorldRegionSavedData.get(serverLevel).saveMarker(revealed);
        }
        return new MarkerReveal(revealed, changed);
    }

    @Override
    public List<WorldMarker> nearbyMarkers(Level level, BlockPos pos, int radius) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null) {
            return List.of();
        }
        int safeRadius = safeRadius(radius);
        long maxDistance = (long) safeRadius * safeRadius;
        return WorldRegionSavedData.get(serverLevel).markers().stream()
                .filter(marker -> marker.dimension().equals(level.dimension()))
                .filter(marker -> marker.pos().distSqr(pos) <= maxDistance)
                .sorted(markerComparator(pos))
                .toList();
    }

    @Override
    public List<WorldMarker> markers(Player player) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        return WorldRegionSavedData.get(serverLevel).markers().stream()
                .filter(marker -> marker.dimension().equals(player.level().dimension()))
                .sorted(markerComparator(player.blockPosition()))
                .toList();
    }

    @Override
    public List<WorldMarker> markers(Level level, ResourceKey<Level> dimension) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        ResourceKey<Level> safeDimension = dimension == null ? level.dimension() : dimension;
        return WorldRegionSavedData.get(serverLevel).markers().stream()
                .filter(marker -> marker.dimension().equals(safeDimension))
                .sorted(Comparator.comparing(marker -> marker.id().toString()))
                .toList();
    }

    @Override
    public Optional<WorldMarker> markerById(Level level, Identifier markerId) {
        if (!(level instanceof ServerLevel serverLevel) || markerId == null) {
            return Optional.empty();
        }
        return WorldRegionSavedData.get(serverLevel).marker(markerId);
    }

    @Override
    public List<String> validateMarkers(Level level) {
        return validationReport(level).messages();
    }

    @Override
    public WorldCoreValidationReport validationReport(Level level) {
        List<WorldCoreValidationIssue> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (String warning : reloadWarnings) {
            addIssue(issues, warnings, "reload", warning, WorldCoreValidationIssue.Severity.WARNING);
        }
        if (hazardDefinitions.isEmpty()) {
            addIssue(issues, warnings, "hazard", "No WorldCore hazard definitions are registered.",
                    WorldCoreValidationIssue.Severity.WARNING);
        }
        Map<Identifier, Identifier> discoveryIds = new LinkedHashMap<>();
        for (WorldRegionDefinition definition : regionDefinitions()) {
            if (definition.displayName().isBlank()) {
                addIssue(issues, warnings, "region",
                        "Region " + definition.id() + " has a blank display name.",
                        WorldCoreValidationIssue.Severity.WARNING);
            }
            if (definition.summary().isBlank()) {
                addIssue(issues, warnings, "region",
                        "Region " + definition.id() + " has a blank summary.",
                        WorldCoreValidationIssue.Severity.WARNING);
            }
            if (definition.radius() < 16) {
                addIssue(issues, warnings, "region",
                        "Region " + definition.id() + " has invalid radius " + definition.radius(),
                        WorldCoreValidationIssue.Severity.ERROR);
            }
            Identifier previousRegion = discoveryIds.putIfAbsent(definition.discoveryId(), definition.id());
            if (previousRegion != null && !previousRegion.equals(definition.id())) {
                addIssue(issues, warnings, "discovery",
                        "Regions " + previousRegion + " and " + definition.id()
                                + " share discovery id " + definition.discoveryId(),
                        WorldCoreValidationIssue.Severity.WARNING);
            }
            for (Identifier hazardId : definition.hazardIds()) {
                if (!hazardDefinitions.containsKey(hazardId)) {
                    addIssue(issues, warnings, "hazard_ref",
                            "Region " + definition.id() + " references missing hazard " + hazardId,
                            WorldCoreValidationIssue.Severity.ERROR);
                }
            }
        }
        for (WorldHazardDefinition definition : hazardDefinitions()) {
            if (definition.displayName().isBlank()) {
                addIssue(issues, warnings, "hazard",
                        "Hazard " + definition.id() + " has a blank display name.",
                        WorldCoreValidationIssue.Severity.WARNING);
            }
            if (definition.summary().isBlank()) {
                addIssue(issues, warnings, "hazard",
                        "Hazard " + definition.id() + " has a blank summary.",
                        WorldCoreValidationIssue.Severity.WARNING);
            }
            if (definition.defaultSeverity() < 0 || definition.defaultSeverity() > 100) {
                addIssue(issues, warnings, "hazard",
                        "Hazard " + definition.id() + " has invalid severity " + definition.defaultSeverity(),
                        WorldCoreValidationIssue.Severity.ERROR);
            }
        }
        int markerCount = 0;
        if (level instanceof ServerLevel serverLevel) {
            List<WorldMarker> storedMarkers = WorldRegionSavedData.get(serverLevel).markers();
            markerCount = storedMarkers.size();
            for (WorldMarker marker : storedMarkers) {
                if (marker.regionId() != null && !regionDefinitions.containsKey(marker.regionId())) {
                    addIssue(issues, warnings, "marker_ref",
                            "Marker " + marker.id() + " references unknown region " + marker.regionId(),
                            WorldCoreValidationIssue.Severity.WARNING);
                }
            }
        }
        return new WorldCoreValidationReport(
                regionDefinitions.size(),
                dataRegionDefinitions.size(),
                hazardDefinitions.size(),
                dataHazardDefinitions.size(),
                markerCount,
                regionSourceCounts(),
                hazardSourceCounts(),
                boundedIssues(issues),
                reloadWarnings);
    }

    @Override
    public WorldContextSnapshot worldContext(Player player) {
        if (player == null) {
            return WorldContextSnapshot.empty();
        }
        return new WorldContextSnapshot(
                currentRegion(player).orElse(null),
                activeRegions(player),
                markers(player),
                hazardSnapshot(player),
                discoveredRegions(player));
    }

    public Optional<HazardTickResult> lastHazardTick(Player player) {
        return Optional.ofNullable(player == null ? null : lastHazardTicks.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoWorldRegionTransitionResult> lastRegionTransition(Player player) {
        return Optional.ofNullable(player == null ? null : lastRegionTransitions.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoWorldHazardTransitionResult> lastHazardTransition(Player player) {
        return Optional.ofNullable(player == null ? null : lastHazardTransitions.get(player.getUUID()));
    }

    public String activeRegionId(Player player) {
        return player == null ? "" : activeRegionIds.getOrDefault(player.getUUID(), "");
    }

    public String activeHazardId(Player player) {
        return player == null ? "" : activeHazardIds.getOrDefault(player.getUUID(), "");
    }

    public List<String> startedRegionMissions(Player player) {
        if (player == null) {
            return List.of();
        }
        return startedRegionMissions.getOrDefault(player.getUUID(), List.of());
    }

    public Optional<EchoWorldContracts.EchoSpawnRuleEventResult> lastSpawnRuleEvent(Player player) {
        return Optional.ofNullable(player == null ? null : lastSpawnRuleEvents.get(player.getUUID()));
    }

    public int activeSpawnPopulation(Identifier regionId, String ruleId) {
        String region = regionId == null ? "" : regionId.toString();
        return activeSpawnPopulations.getOrDefault(spawnZoneKey(region, ruleId), 0);
    }

    public Map<String, Integer> activeSpawnPopulations() {
        return Map.copyOf(activeSpawnPopulations);
    }

    public Optional<EchoWorldContracts.EchoSpawnZoneStateResult> lastSpawnZoneState(Player player) {
        return Optional.ofNullable(player == null ? null : lastSpawnZoneStates.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoSpawnZoneStateResult> activeSpawnZoneStateResult(Identifier regionId,
            String ruleId) {
        String region = regionId == null ? "" : regionId.toString();
        return activeSpawnZoneStateResult(region, ruleId);
    }

    public Optional<EchoWorldContracts.EchoSpawnZoneStateResult> activeSpawnZoneStateResult(String regionId,
            String ruleId) {
        return Optional.ofNullable(activeSpawnZoneStateResults.get(spawnZoneKey(regionId, ruleId)));
    }

    public Map<String, EchoWorldContracts.EchoSpawnZoneStateResult> activeSpawnZoneStateResults() {
        return Map.copyOf(activeSpawnZoneStateResults);
    }

    public Optional<Map<String, Object>> activeSpawnZoneState(Identifier regionId, String ruleId) {
        String region = regionId == null ? "" : regionId.toString();
        return activeSpawnZoneState(region, ruleId);
    }

    public Optional<Map<String, Object>> activeSpawnZoneState(String regionId, String ruleId) {
        return Optional.ofNullable(activeSpawnZoneStates.get(spawnZoneKey(regionId, ruleId)))
                .map(SpawnZoneState::toMap);
    }

    public Map<String, Map<String, Object>> activeSpawnZoneStates() {
        LinkedHashMap<String, Map<String, Object>> states = new LinkedHashMap<>();
        activeSpawnZoneStates.forEach((key, state) -> states.put(key, state.toMap()));
        return Map.copyOf(states);
    }

    public Optional<EchoWorldContracts.EchoStructurePoiLookupResult> lastStructurePoiLookup(Player player) {
        return Optional.ofNullable(player == null ? null : lastStructurePoiLookups.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoStructurePoiMarkerStateResult> lastStructurePoiMarkerState(Player player) {
        return Optional.ofNullable(player == null ? null : lastStructurePoiMarkerStates.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoStructurePoiMarkerStateResult> resolvedStructurePoiMarkerState(String markerId) {
        return Optional.ofNullable(markerId == null || markerId.isBlank()
                ? null
                : resolvedStructurePoiMarkerStates.get(markerId));
    }

    public Map<String, EchoWorldContracts.EchoStructurePoiMarkerStateResult> resolvedStructurePoiMarkerStates() {
        return Map.copyOf(resolvedStructurePoiMarkerStates);
    }

    public Optional<EchoWorldContracts.EchoStructureDiscoveryStateResult> lastStructureDiscoveryState(Player player) {
        return Optional.ofNullable(player == null ? null : lastStructureDiscoveryStates.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoStructureDiscoveryStateResult> resolvedStructureDiscoveryState(String markerId) {
        return Optional.ofNullable(markerId == null || markerId.isBlank()
                ? null
                : resolvedStructureDiscoveryStates.get(markerId));
    }

    public Map<String, EchoWorldContracts.EchoStructureDiscoveryStateResult> resolvedStructureDiscoveryStates() {
        return Map.copyOf(resolvedStructureDiscoveryStates);
    }

    public Optional<Map<String, Object>> resolvedStructurePoiState(String markerId) {
        return Optional.ofNullable(markerId == null || markerId.isBlank() ? null : resolvedStructurePoiStates.get(markerId))
                .map(StructurePoiState::toMap);
    }

    public Map<String, Map<String, Object>> resolvedStructurePoiStates() {
        LinkedHashMap<String, Map<String, Object>> states = new LinkedHashMap<>();
        resolvedStructurePoiStates.forEach((key, state) -> states.put(key, state.toMap()));
        return Map.copyOf(states);
    }

    public Optional<EchoWorldContracts.EchoStatusEffectSaveResult> lastStatusEffectSave(Player player) {
        return Optional.ofNullable(player == null ? null : lastStatusEffectSaves.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoStatusEffectApplyResult> lastStatusEffectApplication(Player player) {
        return Optional.ofNullable(player == null ? null : lastStatusEffectApplications.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoStatusEffectStackingResult> lastStatusEffectStacking(Player player,
            String effectId) {
        if (player == null || effectId == null || effectId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(lastStatusEffectStackings.getOrDefault(player.getUUID(), Map.of()).get(effectId));
    }

    public Map<String, EchoWorldContracts.EchoStatusEffectStackingResult> lastStatusEffectStackings(Player player) {
        if (player == null) {
            return Map.of();
        }
        return Map.copyOf(lastStatusEffectStackings.getOrDefault(player.getUUID(), Map.of()));
    }

    public Optional<EchoWorldContracts.EchoStatusEffectLoadResult> lastStatusEffectLoad(Player player) {
        return Optional.ofNullable(player == null ? null : lastStatusEffectLoads.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoStatusEffectExpiryResult> lastStatusEffectExpiry(Player player,
            String effectId) {
        if (player == null || effectId == null || effectId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(lastStatusEffectExpiries.getOrDefault(player.getUUID(), Map.of()).get(effectId));
    }

    public Map<String, EchoWorldContracts.EchoStatusEffectExpiryResult> lastStatusEffectExpiries(Player player) {
        if (player == null) {
            return Map.of();
        }
        return Map.copyOf(lastStatusEffectExpiries.getOrDefault(player.getUUID(), Map.of()));
    }

    public List<String> activeStatusEffects(Player player) {
        if (player == null) {
            return List.of();
        }
        return activeStatusEffects.getOrDefault(player.getUUID(), List.of());
    }

    public Optional<Map<String, Object>> activeStatusEffectState(Player player, String effectId) {
        if (player == null || effectId == null || effectId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeStatusEffectStates.getOrDefault(player.getUUID(), Map.of()).get(effectId))
                .map(ActiveStatusEffectState::toMap);
    }

    public Map<String, Map<String, Object>> activeStatusEffectStates(Player player) {
        if (player == null) {
            return Map.of();
        }
        LinkedHashMap<String, Map<String, Object>> states = new LinkedHashMap<>();
        activeStatusEffectStates.getOrDefault(player.getUUID(), Map.of()).forEach((effectId, state) ->
                states.put(effectId, state.toMap()));
        return Map.copyOf(states);
    }

    public Optional<Map<String, Object>> activeStatusProfileState(Player player, String effectId) {
        if (player == null || effectId == null || effectId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeStatusProfileStates.getOrDefault(player.getUUID(), Map.of()).get(effectId))
                .map(StatusProfileState::toMap);
    }

    public Map<String, Map<String, Object>> activeStatusProfileStates(Player player) {
        if (player == null) {
            return Map.of();
        }
        LinkedHashMap<String, Map<String, Object>> states = new LinkedHashMap<>();
        activeStatusProfileStates.getOrDefault(player.getUUID(), Map.of()).forEach((effectId, state) ->
                states.put(effectId, state.toMap()));
        return Map.copyOf(states);
    }

    public int tickStatusEffects(Player player, long gameTick) {
        if (player == null) {
            return 0;
        }
        pruneActiveStatusEffects(player, Math.max(0L, gameTick));
        return activeStatusEffects(player).size();
    }

    public Optional<EchoWorldContracts.EchoHazardTickDamageResult> lastHazardTickDamage(Player player) {
        return Optional.ofNullable(player == null ? null : lastHazardTickDamage.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoWorldCellSampleResult> lastWorldCellSample(Player player) {
        return Optional.ofNullable(player == null ? null : lastWorldCellSamples.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoWorldCellSampleResult> sampledWorldCell(String cellKey) {
        return Optional.ofNullable(cellKey == null || cellKey.isBlank() ? null : sampledWorldCells.get(cellKey));
    }

    public Optional<EchoWorldContracts.EchoWorldCellSampleResult> sampledHazardField(String hazardId) {
        return Optional.ofNullable(hazardId == null || hazardId.isBlank() ? null : sampledHazardFields.get(hazardId));
    }

    public Map<String, EchoWorldContracts.EchoWorldCellSampleResult> sampledWorldCells() {
        return Map.copyOf(sampledWorldCells);
    }

    public Map<String, EchoWorldContracts.EchoWorldCellSampleResult> sampledHazardFields() {
        return Map.copyOf(sampledHazardFields);
    }

    public Optional<EchoWorldContracts.EchoBiomeHazardOverlayResult> lastBiomeHazardOverlay(Player player) {
        return Optional.ofNullable(player == null ? null : lastBiomeHazardOverlays.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoBiomeHazardOverlayResult> sampledBiomeHazardOverlay(String cellKey) {
        return Optional.ofNullable(cellKey == null || cellKey.isBlank() ? null : sampledBiomeHazardOverlays.get(cellKey));
    }

    public Map<String, EchoWorldContracts.EchoBiomeHazardOverlayResult> sampledBiomeHazardOverlays() {
        return Map.copyOf(sampledBiomeHazardOverlays);
    }

    public Optional<EchoWorldContracts.EchoWorldChunkStateResult> lastWorldChunkState(Player player) {
        return Optional.ofNullable(player == null ? null : lastWorldChunkStates.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoWorldChunkStateResult> sampledWorldChunkState(String chunkKey) {
        return Optional.ofNullable(chunkKey == null || chunkKey.isBlank() ? null : sampledWorldChunkStates.get(chunkKey));
    }

    public Map<String, EchoWorldContracts.EchoWorldChunkStateResult> sampledWorldChunkStates() {
        return Map.copyOf(sampledWorldChunkStates);
    }

    public Optional<Map<String, Object>> sampledWorldChunk(String chunkKey) {
        return Optional.ofNullable(chunkKey == null || chunkKey.isBlank() ? null : sampledWorldChunks.get(chunkKey))
                .map(WorldChunkState::toMap);
    }

    public Map<String, Map<String, Object>> sampledWorldChunks() {
        LinkedHashMap<String, Map<String, Object>> chunks = new LinkedHashMap<>();
        sampledWorldChunks.forEach((key, state) -> chunks.put(key, state.toMap()));
        return Map.copyOf(chunks);
    }

    public Optional<EchoWorldContracts.EchoHazardFieldStateResult> lastHazardFieldState(Player player) {
        return Optional.ofNullable(player == null ? null : lastHazardFieldStates.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoHazardFieldStateResult> sampledHazardFieldStateResult(String hazardId) {
        return Optional.ofNullable(hazardId == null || hazardId.isBlank()
                ? null
                : sampledHazardFieldStateResults.get(hazardId));
    }

    public Map<String, EchoWorldContracts.EchoHazardFieldStateResult> sampledHazardFieldStateResults() {
        return Map.copyOf(sampledHazardFieldStateResults);
    }

    public Optional<Map<String, Object>> sampledHazardFieldState(String hazardId) {
        return Optional.ofNullable(hazardId == null || hazardId.isBlank() ? null : sampledHazardFieldStates.get(hazardId))
                .map(HazardFieldState::toMap);
    }

    public Map<String, Map<String, Object>> sampledHazardFieldStates() {
        LinkedHashMap<String, Map<String, Object>> fields = new LinkedHashMap<>();
        sampledHazardFieldStates.forEach((key, state) -> fields.put(key, state.toMap()));
        return Map.copyOf(fields);
    }

    public Optional<EchoWorldContracts.EchoDifficultyProfile> activeDifficultyProfile(Player player) {
        return Optional.ofNullable(player == null ? null : activeDifficultyProfiles.get(player.getUUID()));
    }

    public Optional<EchoWorldContracts.EchoDifficultyProfile> regionDifficultyProfile(Identifier regionId) {
        return Optional.ofNullable(regionId == null ? null : regionDifficultyProfiles.get(regionId.toString()));
    }

    public Optional<EchoWorldContracts.EchoDifficultyProfileSelectionResult> regionDifficultyProfileSelection(
            Identifier regionId) {
        return Optional.ofNullable(regionId == null ? null : regionDifficultyProfileSelections.get(regionId.toString()));
    }

    public Map<String, EchoWorldContracts.EchoDifficultyProfileSelectionResult> regionDifficultyProfileSelections() {
        return Map.copyOf(regionDifficultyProfileSelections);
    }

    public Optional<Map<String, Object>> activeDifficultyApplicationState(Player player) {
        return Optional.ofNullable(player == null ? null : activeDifficultyApplicationStates.get(player.getUUID()))
                .map(DifficultyApplicationState::toMap);
    }

    public Optional<EchoWorldContracts.EchoDifficultyApplicationResult> activeDifficultyApplicationResult(Player player) {
        return Optional.ofNullable(player == null ? null : activeDifficultyApplicationResults.get(player.getUUID()));
    }

    public Optional<Map<String, Object>> regionDifficultyApplicationState(Identifier regionId) {
        return Optional.ofNullable(regionId == null ? null : regionDifficultyApplicationStates.get(regionId.toString()))
                .map(DifficultyApplicationState::toMap);
    }

    public Optional<EchoWorldContracts.EchoDifficultyApplicationResult> regionDifficultyApplicationResult(Identifier regionId) {
        return Optional.ofNullable(regionId == null ? null : regionDifficultyApplicationResults.get(regionId.toString()));
    }

    public Map<String, Map<String, Object>> difficultyApplicationStates() {
        LinkedHashMap<String, Map<String, Object>> states = new LinkedHashMap<>();
        regionDifficultyApplicationStates.forEach((regionId, state) -> states.put(regionId, state.toMap()));
        return Map.copyOf(states);
    }

    public Map<String, EchoWorldContracts.EchoDifficultyApplicationResult> difficultyApplicationResults() {
        return Map.copyOf(regionDifficultyApplicationResults);
    }

    public synchronized void replaceDataDefinitions(Map<Identifier, WorldHazardDefinition> hazards,
            Map<Identifier, WorldRegionDefinition> regions) {
        replaceDataDefinitions(hazards, regions, List.of());
    }

    public synchronized void replaceDataDefinitions(Map<Identifier, WorldHazardDefinition> hazards,
            Map<Identifier, WorldRegionDefinition> regions, List<String> warnings) {
        dataHazardDefinitions.clear();
        dataRegionDefinitions.clear();
        reloadWarnings = boundedWarnings(warnings);
        if (hazards != null) {
            dataHazardDefinitions.putAll(hazards);
        }
        if (regions != null) {
            dataRegionDefinitions.putAll(regions);
        }
        rebuildDefinitionViews();
        EchoCoreServices.invalidateIndexRecipes("world definitions changed");
        for (Identifier id : dataHazardDefinitions.keySet()) {
            if (baseHazardDefinitions.containsKey(id)) {
                EchoWorldCore.LOGGER.info("WorldCore data hazard {} overrides bootstrap definition.", id);
            }
        }
        for (Identifier id : dataRegionDefinitions.keySet()) {
            if (baseRegionDefinitions.containsKey(id)) {
                EchoWorldCore.LOGGER.info("WorldCore data region {} overrides bootstrap definition.", id);
            }
        }
    }

    public int dataRegionDefinitionCount() {
        return dataRegionDefinitions.size();
    }

    public int dataHazardDefinitionCount() {
        return dataHazardDefinitions.size();
    }

    public List<String> reloadWarnings() {
        return reloadWarnings;
    }

    public Map<String, Integer> regionSourceCounts() {
        return sourceCounts(regionDefinitions.keySet());
    }

    public Map<String, Integer> hazardSourceCounts() {
        return sourceCounts(hazardDefinitions.keySet());
    }

    @Override
    public boolean recordStructureScan(ServerPlayer player, Identifier structureId, BlockPos pos,
            String displayName, String summary) {
        return recordStructure(player, structureId, pos, displayName, summary, WorldDiscoverySource.SCAN);
    }

    @Override
    public boolean recordStructureEntry(ServerPlayer player, Identifier structureId, BlockPos pos,
            String displayName, String summary) {
        return recordStructure(player, structureId, pos, displayName, summary, WorldDiscoverySource.STRUCTURE);
    }

    @Override
    public boolean hasDiscoveredRegion(Player player, Identifier regionId) {
        return player != null && regionDefinition(regionId)
                .map(definition -> EchoCoreServices.hasDiscoveredFeature(player, definition.discoveryId())
                        || savedDiscoveries(player).contains(definition.id()))
                .orElse(false);
    }

    @Override
    public Set<Identifier> discoveredRegions(Player player) {
        if (player == null) {
            return Set.of();
        }
        LinkedHashSet<Identifier> discovered = new LinkedHashSet<>();
        for (WorldRegionDefinition definition : regionDefinitions()) {
            if (EchoCoreServices.hasDiscoveredFeature(player, definition.discoveryId())) {
                discovered.add(definition.id());
            }
        }
        discovered.addAll(savedDiscoveries(player));
        return Set.copyOf(discovered);
    }

    @Override
    public boolean discoverRegion(ServerPlayer player, Identifier regionId, WorldDiscoverySource source) {
        if (player == null || regionId == null) {
            return false;
        }
        WorldRegionDefinition definition = regionDefinitions.get(regionId);
        if (definition == null) {
            return false;
        }
        WorldDiscoverySource safeSource = source == null ? WorldDiscoverySource.INTEGRATION : source;
        boolean already = hasDiscoveredRegion(player, regionId);
        boolean discovered = already ? false : EchoCoreServices.discoverFeature(player, definition.discoveryId());
        WorldRegionInstance instance = activeRegions(player).stream()
                .filter(candidate -> candidate.definitionId().equals(regionId))
                .findFirst()
                .orElse(instanceForDefinition(player.level(), player.blockPosition(), definition, player));
        boolean shouldRecord = !already || explicitDiscoverySource(safeSource);
        if (shouldRecord && player.level() instanceof ServerLevel serverLevel) {
            WorldRegionSavedData.get(serverLevel).recordDiscovery(player.getUUID(), definition.id(),
                    safeSource, player.blockPosition(),
                    serverLevel.getGameTime());
        }
        boolean firstDiscovery = !already;
        boolean explicit = explicitDiscoverySource(safeSource);
        if ((firstDiscovery || explicit) && shouldEmitRegionDiscovery(player, regionId, explicit)) {
            EchoWorldRuntimeBus.fireRegionDiscovered(new EchoWorldRuntimeBus.RegionDiscovered(
                    player, instance, safeSource, firstDiscovery && (discovered || savedDiscoveries(player).contains(regionId))));
        }
        return firstDiscovery || discovered;
    }

    private boolean shouldEmitRegionDiscovery(ServerPlayer player, Identifier regionId, boolean explicit) {
        Set<String> emitted = emittedRegionDiscoveryEvents.computeIfAbsent(player.getUUID(),
                ignored -> ConcurrentHashMap.newKeySet());
        String key = regionId.toString();
        if (explicit) {
            emitted.add(key);
            return true;
        }
        return emitted.add(key);
    }

    @Override
    public Optional<WorldRegionInstance> currentRegion(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return activeRegions(player).stream().findFirst();
    }

    @Override
    public void tickPlayer(ServerPlayer player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        pruneActiveStatusEffects(player, player.level().getGameTime());
        List<WorldRegionInstance> active = activeRegions(player);
        WorldRegionInstance previousPrimary = lastPrimaryRegion.get(player.getUUID());
        WorldRegionInstance primary = active.isEmpty() ? null : active.get(0);
        Optional<Identifier> currentMissionId = primary == null
                ? Optional.empty()
                : missionIdForRegion(primary.definitionId());
        EchoWorldContracts.EchoDifficultyProfile currentDifficulty = primary == null
                ? defaultDifficultyProfile()
                : difficultyProfileForRegion(primary.definitionId());
        activeDifficultyProfiles.put(player.getUUID(), currentDifficulty);
        if (primary != null) {
            regionDifficultyProfiles.put(primary.definitionId().toString(), currentDifficulty);
        }
        EchoWorldContracts.EchoWorldRegionTransitionResult transition = new EchoNativeWorldRegionTransitionBridge(EchoWorldCore.MODID)
                .transition(new EchoWorldContracts.EchoWorldRegionTransitionRequest(
                        player.getUUID().toString(),
                        previousPrimary == null ? "" : previousPrimary.definitionId().toString(),
                        primary == null ? "" : primary.definitionId().toString(),
                        currentMissionId.map(Identifier::toString).orElse(""),
                        player.level().getGameTime(),
                        "worldcore-player-tick"
                ));
        lastRegionTransitions.put(player.getUUID(), transition);
        if (transition.regionExited() && previousPrimary != null) {
            EchoWorldRuntimeBus.fireRegionExited(new EchoWorldRuntimeBus.RegionExited(player, previousPrimary));
        }
        if (!active.isEmpty()) {
            lastPrimaryRegion.put(player.getUUID(), primary);
            activeRegionIds.put(player.getUUID(), primary.definitionId().toString());
            if (transition.regionEntered()) {
                EchoWorldRuntimeBus.fireRegionEntered(new EchoWorldRuntimeBus.RegionEntered(player, primary));
                for (String missionEvent : transition.missionEvents()) {
                    Identifier missionId = Identifier.tryParse(missionEvent);
                    if (missionId != null) {
                        EchoCoreServices.startMission(player, missionId);
                    }
                }
                if (!transition.missionEvents().isEmpty()) {
                    ArrayList<String> missions = new ArrayList<>(
                            startedRegionMissions.getOrDefault(player.getUUID(), List.of()));
                    missions.addAll(transition.missionEvents());
                    startedRegionMissions.put(player.getUUID(), List.copyOf(missions));
                }
                triggerStructurePoiLookup(player, primary, defaultStructurePlacement(player, primary), Config.activeRegionRadius());
                triggerSpawnRuleEvent(player, primary, defaultSpawnRule(player, primary), currentDifficulty, 0);
            }
            for (WorldRegionInstance region : active) {
                discoverRegion(player, region.definitionId(), WorldDiscoverySource.ENTER);
            }
        } else {
            lastPrimaryRegion.remove(player.getUUID());
            activeRegionIds.remove(player.getUUID());
        }
        WorldHazardSnapshot currentHazard = hazardSnapshot(player);
        WorldHazardSnapshot previousHazard = lastHazards.put(player.getUUID(), currentHazard);
        if (previousHazard == null) {
            previousHazard = WorldHazardSnapshot.nominal();
        }
        if (!previousHazard.equals(currentHazard)) {
            EchoWorldRuntimeBus.fireHazardChanged(new EchoWorldRuntimeBus.HazardChanged(player, previousHazard, currentHazard));
        }
        if (primary != null) {
            samplePrimaryWorldCell(player, primary, currentHazard);
        }
        EchoWorldContracts.EchoDifficultyProfile hazardDifficulty = primary == null
                ? difficultyProfileForSnapshot(currentHazard)
                : currentDifficulty;
        applyHazardTick(player, currentHazard, hazardDifficulty).ifPresent(result -> lastHazardTicks.put(player.getUUID(), result));
        pruneActiveStatusEffects(player, player.level().getGameTime());
    }

    private Optional<EchoWorldContracts.EchoWorldCellSampleResult> samplePrimaryWorldCell(ServerPlayer player,
            WorldRegionInstance region,
            WorldHazardSnapshot snapshot) {
        Optional<WorldHazardDefinition> hazard = tickingHazard(snapshot);
        if (hazard.isEmpty()) {
            return Optional.empty();
        }
        return sampleWorldCell(
                player,
                region,
                worldHazardAtPlayer(player, hazard.get(), snapshot),
                defaultBiomeProfile(player, hazard.get().id()),
                defaultStructurePlacement(player, region),
                player.level().dimension().identifier().toString());
    }

    public Optional<HazardTickResult> applyHazardTick(ServerPlayer player, WorldHazardSnapshot snapshot) {
        return applyHazardTick(player, snapshot, difficultyProfileForSnapshot(snapshot));
    }

    private Optional<HazardTickResult> applyHazardTick(ServerPlayer player,
            WorldHazardSnapshot snapshot,
            EchoWorldContracts.EchoDifficultyProfile difficulty) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel) || snapshot == null || snapshot.safeZone()) {
            return Optional.empty();
        }
        Optional<WorldHazardDefinition> activeHazard = tickingHazard(snapshot);
        if (activeHazard.isEmpty()) {
            return Optional.empty();
        }
        WorldHazardDefinition hazard = activeHazard.get();
        float before = player.getHealth();
        EchoWorldContracts.EchoHazardTickDamageResult damageResult = applyHazardTickDamage(
                player,
                hazard,
                snapshot,
                difficulty == null ? defaultDifficultyProfile() : difficulty).orElseThrow();
        float damage = (float) damageResult.damageApplied();
        if (damage > 0.0F && before > 0.0F) {
            player.hurtServer(serverLevel, player.damageSources().magic(), damage);
        }
        Identifier statusEffectId = statusEffectId(hazard.id());
        int amplifier = statusAmplifier(hazard);
        player.addEffect(new MobEffectInstance(statusEffect(hazard.id()), 100, amplifier, false, true));
        float after = player.getHealth();
        if (damage > 0.0F && before > 0.0F && after >= before) {
            player.setHealth(Math.max(0.0F, before - damage));
            after = player.getHealth();
        }
        persistStatusEffect(player, hazard.id(), new EchoWorldContracts.EchoStatusEffect(
                statusEffectId.toString(),
                100,
                amplifier,
                statusSaveKey(hazard.id())), damage, serverLevel.getGameTime());
        return Optional.of(new HazardTickResult(
                hazard.id(),
                statusEffectId,
                damage,
                before,
                after,
                true));
    }

    public Optional<EchoWorldContracts.EchoHazardTickDamageResult> applyHazardTickDamage(ServerPlayer player,
            WorldHazardDefinition hazard,
            WorldHazardSnapshot snapshot,
            EchoWorldContracts.EchoDifficultyProfile difficulty) {
        if (player == null || hazard == null || snapshot == null || difficulty == null) {
            return Optional.empty();
        }
        int severity = Math.max(hazard.defaultSeverity(), snapshot.severity());
        EchoWorldContracts.EchoHazardTickDamageResult result = new EchoNativeHazardTickDamageBridge(EchoWorldCore.MODID)
                .apply(new EchoWorldContracts.EchoHazardTickDamageRequest(
                        player.getUUID().toString(),
                        player.getHealth(),
                        severity,
                        player.level().getGameTime(),
                        "worldcore-hazard-tick",
                        new EchoWorldContracts.EchoWorldHazard(
                                hazard.id().toString(),
                                hazard.id().getPath(),
                                player.blockPosition().getX(),
                                player.blockPosition().getZ(),
                                0,
                                hazardBaseDamage(hazard, snapshot),
                                statusEffectId(hazard.id()).toString()),
                        difficulty
                ));
        lastHazardTickDamage.put(player.getUUID(), result);
        activeDifficultyProfiles.put(player.getUUID(), difficulty);
        recordDifficultyApplication(
                player,
                firstRegionId(snapshot),
                difficulty,
                result.hazardId(),
                result.baseDamage(),
                result.damageApplied(),
                "",
                0,
                0,
                0,
                result.gameTick(),
                "WorldRegionService.applyHazardTickDamage");
        return Optional.of(result);
    }

    public Optional<EchoWorldContracts.EchoStatusEffectSaveResult> persistStatusEffect(ServerPlayer player,
            Identifier hazardId,
            EchoWorldContracts.EchoStatusEffect statusEffect,
            float damageApplied,
            long gameTick) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel) || hazardId == null || statusEffect == null) {
            return Optional.empty();
        }
        EchoWorldContracts.EchoStatusEffectSaveResult result = new EchoNativeStatusEffectSaveBridge(EchoWorldCore.MODID)
                .persist(new EchoWorldContracts.EchoStatusEffectSaveRequest(
                        player.getUUID().toString(),
                        hazardId.toString(),
                        Math.max(0.0F, damageApplied),
                        Math.max(0L, gameTick),
                        "worldcore-hazard-status-save",
                        statusEffect
                ));
        Identifier statusEffectId = Identifier.tryParse(result.effectId());
        WorldRegionSavedData.get(serverLevel).recordHazardExposure(
                player.getUUID(),
                hazardId,
                statusEffectId,
                result.saveKey(),
                result.durationTicks(),
                result.amplifier(),
                result.damageApplied(),
                result.gameTick());
        lastStatusEffectSaves.put(player.getUUID(), result);
        EchoWorldContracts.EchoStatusEffectApplyResult application = applyStatusEffectState(
                player,
                result.hazardId(),
                new EchoWorldContracts.EchoStatusEffect(
                        result.effectId(),
                        result.durationTicks(),
                        result.amplifier(),
                        result.saveKey()),
                result.damageApplied(),
                result.gameTick(),
                "worldcore-hazard-status-apply",
                false).orElseThrow();
        recordActiveStatusEffect(player, result.effectId(), result.hazardId(), result.saveKey(),
                application.durationTicks(), application.amplifier(), application.damageApplied(),
                application.appliedGameTick(), application.loaded());
        recordStatusProfileApplication(player, result.hazardId(), result.effectId(), result.saveKey(),
                result.durationTicks(), result.amplifier(), result.damageApplied(), result.gameTick(), true, false);
        EchoWorldRuntimeBus.fireStatusEffectSaved(new EchoWorldRuntimeBus.StatusEffectSaved(
                player,
                result.hazardId(),
                result.effectId(),
                result.saveKey(),
                result.saved()));
        return Optional.of(result);
    }

    public Optional<EchoWorldContracts.EchoStatusEffectLoadResult> loadStatusEffect(ServerPlayer player,
            Identifier hazardId,
            long gameTick) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel) || hazardId == null) {
            return Optional.empty();
        }
        WorldRegionSavedData data = WorldRegionSavedData.get(serverLevel);
        String saveKey = data.hazardExposureSaveKey(player.getUUID(), hazardId);
        Map<String, Object> savedStatusState = data.hazardExposureStatusState(player.getUUID(), hazardId);
        if (saveKey.isBlank() || savedStatusState.isEmpty()) {
            return Optional.empty();
        }
        return loadStatusEffect(player, hazardId, saveKey, savedStatusState, gameTick);
    }

    public Optional<EchoWorldContracts.EchoStatusEffectLoadResult> loadStatusEffect(ServerPlayer player,
            Identifier hazardId,
            EchoWorldContracts.EchoStatusEffectSaveResult savedStatus,
            long gameTick) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel) || hazardId == null || savedStatus == null) {
            return Optional.empty();
        }
        if (!WorldRegionSavedData.get(serverLevel).hazardExposures(player.getUUID()).contains(hazardId)) {
            return Optional.empty();
        }
        return loadStatusEffect(player, hazardId, savedStatus.saveKey(), savedStatus.savedStatusState(), gameTick);
    }

    private Optional<EchoWorldContracts.EchoStatusEffectLoadResult> loadStatusEffect(ServerPlayer player,
            Identifier hazardId,
            String saveKey,
            Map<String, Object> savedStatusState,
            long gameTick) {
        if (player == null || hazardId == null || saveKey == null || saveKey.isBlank()
                || savedStatusState == null || savedStatusState.isEmpty()) {
            return Optional.empty();
        }
        EchoWorldContracts.EchoStatusEffectLoadResult result = new EchoNativeStatusEffectLoadBridge(EchoWorldCore.MODID)
                .load(new EchoWorldContracts.EchoStatusEffectLoadRequest(
                        player.getUUID().toString(),
                        hazardId.toString(),
                        saveKey,
                        savedStatusState,
                        Math.max(0L, gameTick),
                        "worldcore-hazard-status-load"
                ));
        if (result.loaded()) {
            player.addEffect(new MobEffectInstance(statusEffect(hazardId), result.durationTicks(),
                    result.amplifier(), false, true));
        }
        lastStatusEffectLoads.put(player.getUUID(), result);
        if (result.loaded()) {
            EchoWorldContracts.EchoStatusEffectApplyResult application = applyStatusEffectState(
                    player,
                    result.hazardId(),
                    new EchoWorldContracts.EchoStatusEffect(
                            result.effectId(),
                            result.durationTicks(),
                            result.amplifier(),
                            result.saveKey()),
                    result.damageApplied(),
                    result.loadedGameTick(),
                    "worldcore-hazard-status-load-apply",
                    true).orElseThrow();
            recordActiveStatusEffect(player, result.effectId(), result.hazardId(), result.saveKey(),
                    application.durationTicks(), application.amplifier(), application.damageApplied(),
                    application.appliedGameTick(), application.loaded());
            recordStatusProfileApplication(player, result.hazardId(), result.effectId(), result.saveKey(),
                    result.durationTicks(), result.amplifier(), result.damageApplied(), result.loadedGameTick(), true, true);
        }
        EchoWorldRuntimeBus.fireStatusEffectLoaded(new EchoWorldRuntimeBus.StatusEffectLoaded(
                player,
                result.hazardId(),
                result.effectId(),
                result.saveKey(),
                result.loaded()));
        return Optional.of(result);
    }

    private Optional<EchoWorldContracts.EchoStatusEffectApplyResult> applyStatusEffectState(Player player,
            String hazardId,
            EchoWorldContracts.EchoStatusEffect statusEffect,
            float damageApplied,
            long gameTick,
            String sourceReason,
            boolean loaded) {
        if (player == null || hazardId == null || hazardId.isBlank() || statusEffect == null) {
            return Optional.empty();
        }
        EchoWorldContracts.EchoStatusEffectApplyResult result = new EchoNativeStatusEffectApplyBridge(EchoWorldCore.MODID)
                .apply(new EchoWorldContracts.EchoStatusEffectApplyRequest(
                        player.getUUID().toString(),
                        hazardId,
                        Math.max(0.0F, damageApplied),
                        Math.max(0L, gameTick),
                        sourceReason,
                        statusEffect,
                        loaded
                ));
        lastStatusEffectApplications.put(player.getUUID(), result);
        return Optional.of(result);
    }

    public Optional<EchoWorldContracts.EchoSpawnRuleEventResult> triggerSpawnRuleEvent(ServerPlayer player,
            WorldRegionInstance region,
            EchoWorldContracts.EchoSpawnRule spawnRule,
            EchoWorldContracts.EchoDifficultyProfile difficulty,
            int activeMobCount) {
        if (player == null || region == null || spawnRule == null || difficulty == null) {
            return Optional.empty();
        }
        EchoWorldContracts.EchoSpawnRuleEventResult result = new EchoNativeSpawnRuleEventBridge(EchoWorldCore.MODID)
                .plan(new EchoWorldContracts.EchoSpawnRuleEventRequest(
                        player.getUUID().toString(),
                        region.definitionId().toString(),
                        player.blockPosition().getX(),
                        player.blockPosition().getY(),
                        player.blockPosition().getZ(),
                        Math.max(0, activeMobCount),
                        player.level().getGameTime(),
                        "worldcore-region-entry",
                        spawnRule,
                        difficulty
                ));
        lastSpawnRuleEvents.put(player.getUUID(), result);
        EchoWorldContracts.EchoSpawnZoneStateResult zoneState = new EchoNativeSpawnZoneStateBridge(EchoWorldCore.MODID)
                .persist(new EchoWorldContracts.EchoSpawnZoneStateRequest(
                        result.playerId(),
                        "worldcore-spawn-zone-state",
                        result
                ));
        lastSpawnZoneStates.put(player.getUUID(), zoneState);
        activeSpawnZoneStateResults.put(zoneState.zoneKey(), zoneState);
        activeSpawnPopulations.put(zoneState.zoneKey(), zoneState.activePopulation());
        activeSpawnZoneStates.put(zoneState.zoneKey(), SpawnZoneState.from(zoneState));
        activeDifficultyProfiles.put(player.getUUID(), difficulty);
        regionDifficultyProfiles.put(result.regionId(), difficulty);
        recordDifficultyApplication(
                player,
                result.regionId(),
                difficulty,
                "",
                0.0D,
                0.0D,
                result.ruleId(),
                result.maxCount(),
                result.scaledBudget(),
                result.activeMobCount() + result.spawnCount(),
                result.gameTick(),
                "WorldRegionService.triggerSpawnRuleEvent");
        EchoWorldRuntimeBus.fireSpawnRuleTriggered(new EchoWorldRuntimeBus.SpawnRuleTriggered(
                player,
                region,
                result.ruleId(),
                result.entityId(),
                result.scaledBudget(),
                result.spawnCount(),
                result.spawnMultiplier()));
        return Optional.of(result);
    }

    public Optional<EchoWorldContracts.EchoWorldCellSampleResult> sampleWorldCell(ServerPlayer player,
            WorldRegionInstance region,
            EchoWorldContracts.EchoWorldHazard hazard,
            EchoWorldContracts.EchoBiomeProfile biome,
            EchoWorldContracts.EchoStructurePlacement structure,
            String worldId) {
        if (player == null || region == null || hazard == null || biome == null || structure == null) {
            return Optional.empty();
        }
        BlockPos pos = player.blockPosition();
        EchoWorldContracts.EchoWorldCellSampleResult result = new EchoNativeWorldCellSampleBridge(EchoWorldCore.MODID)
                .sample(new EchoWorldContracts.EchoWorldCellSampleRequest(
                        player.getUUID().toString(),
                        worldId == null || worldId.isBlank() ? player.level().dimension().identifier().toString() : worldId,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        player.level().getGameTime(),
                        "worldcore-cell-sample",
                        worldRegionContract(region),
                        hazard,
                        biome,
                        structure
                ));
        lastWorldCellSamples.put(player.getUUID(), result);
        sampledWorldCells.put(result.cellKey(), result);
        String previousHazardId = activeHazardIds.getOrDefault(player.getUUID(), "");
        String currentHazardId = result.inHazard() ? result.activeHazardId() : "";
        EchoWorldContracts.EchoWorldHazardTransitionResult hazardTransition =
                new EchoNativeWorldHazardTransitionBridge(EchoWorldCore.MODID)
                        .transition(new EchoWorldContracts.EchoWorldHazardTransitionRequest(
                                result.playerId(),
                                previousHazardId,
                                currentHazardId,
                                hazard.statusEffectId(),
                                result.gameTick(),
                                "worldcore-hazard-field-transition"
                        ));
        lastHazardTransitions.put(player.getUUID(), hazardTransition);
        if (currentHazardId.isBlank()) {
            activeHazardIds.remove(player.getUUID());
        } else {
            activeHazardIds.put(player.getUUID(), currentHazardId);
        }
        EchoWorldContracts.EchoBiomeHazardOverlayResult overlay = new EchoNativeBiomeHazardOverlayBridge("echobiomecore")
                .resolve(new EchoWorldContracts.EchoBiomeHazardOverlayRequest(
                        result.playerId(),
                        result.worldId(),
                        result.x(),
                        result.y(),
                        result.z(),
                        result.gameTick(),
                        "worldcore-biome-hazard-overlay",
                        biome,
                        hazard,
                        result.inRegion(),
                        result.inHazard()
                ));
        lastBiomeHazardOverlays.put(player.getUUID(), overlay);
        sampledBiomeHazardOverlays.put(overlay.cellKey(), overlay);
        EchoWorldContracts.EchoWorldChunkStateResult chunkState = new EchoNativeWorldChunkStateBridge(EchoWorldCore.MODID)
                .resolve(new EchoWorldContracts.EchoWorldChunkStateRequest(
                        result.playerId(),
                        result.worldId(),
                        result.x(),
                        result.y(),
                        result.z(),
                        result.gameTick(),
                        "worldcore-chunk-state",
                        result
                ));
        lastWorldChunkStates.put(player.getUUID(), chunkState);
        sampledWorldChunkStates.put(chunkState.chunkKey(), chunkState);
        sampledWorldChunks.put(chunkState.chunkKey(), WorldChunkState.from(chunkState));
        if (result.inHazard()) {
            sampledHazardFields.put(result.activeHazardId(), result);
            EchoWorldContracts.EchoHazardFieldStateResult hazardFieldState =
                    new EchoNativeHazardFieldStateBridge(EchoWorldCore.MODID)
                            .resolve(new EchoWorldContracts.EchoHazardFieldStateRequest(
                                    result.playerId(),
                                    result.worldId(),
                                    result.gameTick(),
                                    "worldcore-hazard-field-state",
                                    hazard,
                                    result
                            ));
            lastHazardFieldStates.put(player.getUUID(), hazardFieldState);
            sampledHazardFieldStateResults.put(hazardFieldState.hazardId(), hazardFieldState);
            sampledHazardFieldStates.put(hazardFieldState.hazardId(), HazardFieldState.from(hazardFieldState));
        }
        EchoWorldRuntimeBus.fireWorldCellSampled(new EchoWorldRuntimeBus.WorldCellSampled(
                player,
                result.worldId(),
                new BlockPos(result.x(), result.y(), result.z()),
                result.activeRegionId(),
                result.activeHazardId(),
                result.inRegion(),
                result.inHazard()));
        return Optional.of(result);
    }

    public Optional<EchoWorldContracts.EchoStructurePoiLookupResult> triggerStructurePoiLookup(ServerPlayer player,
            WorldRegionInstance region,
            EchoWorldContracts.EchoStructurePlacement structure,
            int maxDistance) {
        if (player == null || region == null || structure == null) {
            return Optional.empty();
        }
        EchoWorldContracts.EchoStructurePoiLookupResult result = new EchoNativeStructurePoiLookupBridge(EchoWorldCore.MODID)
                .lookup(new EchoWorldContracts.EchoStructurePoiLookupRequest(
                        player.getUUID().toString(),
                        region.definitionId().toString(),
                        player.blockPosition().getX(),
                        player.blockPosition().getY(),
                        player.blockPosition().getZ(),
                        Math.max(0, maxDistance),
                        player.level().getGameTime(),
                        "worldcore-structure-poi-lookup",
                        structure
                ));
        lastStructurePoiLookups.put(player.getUUID(), result);
        BlockPos pos = new BlockPos(result.x(), result.y(), result.z());
        EchoWorldRuntimeBus.fireStructurePoiResolved(new EchoWorldRuntimeBus.StructurePoiResolved(
                player,
                region,
                result.structureId(),
                result.poiId(),
                pos,
                result.inRange()));
        if (result.inRange()) {
            EchoWorldContracts.EchoStructurePoiMarkerStateResult markerState =
                    new EchoNativeStructurePoiMarkerStateBridge(EchoWorldCore.MODID)
                            .persist(new EchoWorldContracts.EchoStructurePoiMarkerStateRequest(
                                    result.playerId(),
                                    "worldcore-structure-poi-marker-state",
                                    result
                            ));
            lastStructurePoiMarkerStates.put(player.getUUID(), markerState);
            resolvedStructurePoiMarkerStates.put(markerState.markerId(), markerState);
            resolvedStructurePoiStates.put(markerState.markerId(), StructurePoiState.from(markerState));
            EchoWorldContracts.EchoStructureDiscoveryStateResult discoveryState =
                    new EchoNativeStructureDiscoveryStateBridge("echostructurecore")
                            .discover(new EchoWorldContracts.EchoStructureDiscoveryStateRequest(
                                    result.playerId(),
                                    "worldcore-structure-discovery-state",
                                    markerState
                            ));
            lastStructureDiscoveryStates.put(player.getUUID(), discoveryState);
            resolvedStructureDiscoveryStates.put(discoveryState.markerId(), discoveryState);
            Identifier structureId = Identifier.tryParse(result.structureId());
            if (structureId != null) {
                recordStructureScan(player, structureId, pos,
                        readableStructureName(structureId),
                        "AdapterCore structure/POI lookup resolved " + result.poiId());
            }
        }
        return Optional.of(result);
    }

    private boolean recordStructure(ServerPlayer player, Identifier structureId, BlockPos pos,
            String displayName, String summary, WorldDiscoverySource source) {
        if (player == null || structureId == null || pos == null) {
            return false;
        }
        Optional<WorldRegionDefinition> definition = definitionForStructure(structureId);
        Identifier regionId = definition.map(WorldRegionDefinition::id).orElse(structureId);
        WorldMarkerType markerType = definition.map(def -> switch (def.type()) {
            case CRASH_ZONE -> WorldMarkerType.CRASH_SITE;
            case CONVOY_ROUTE -> WorldMarkerType.ROUTE_CHECKPOINT;
            case ORBITAL_DEBRIS_FIELD -> WorldMarkerType.ORBITAL_DEBRIS;
            case SECURE_OUTPOST -> WorldMarkerType.OUTPOST;
            case ANOMALY_ZONE, NEXUS_SCAR -> WorldMarkerType.ANOMALY;
            default -> WorldMarkerType.STRUCTURE;
        }).orElse(WorldMarkerType.STRUCTURE);
        WorldMarker marker = new WorldMarker(markerId(structureId, pos), regionId, markerType,
                displayName, summary, player.level().dimension(), pos, 64, true, player.level().getGameTime());
        revealMarker(player, marker);
        WorldRegionInstance instance = definition
                .map(def -> instanceForDefinition(player.level(), pos, def, player))
                .orElse(new WorldRegionInstance(regionId, regionId, WorldRegionType.ANOMALY_ZONE,
                        displayName, player.level().dimension(), pos, 64, List.of(), true));
        if (definition.isPresent()) {
            discoverRegion(player, regionId, source);
        }
        EchoWorldRuntimeBus.fireRegionScanned(new EchoWorldRuntimeBus.RegionScanned(player, instance, marker));
        return true;
    }

    private Optional<WorldRegionDefinition> definitionForStructure(Identifier structureId) {
        for (WorldRegionDefinition definition : regionDefinitions()) {
            if (definition.matchesStructure(structureId)
                    || definition.id().equals(structureId)
                    || definition.id().getPath().equals(structureId.getPath())) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }

    private WorldRegionInstance instanceForDefinition(Level level, BlockPos pos,
            WorldRegionDefinition definition, Player player) {
        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        ResourceKey<Level> dimension = level == null ? Level.OVERWORLD : level.dimension();
        Identifier dimensionId = dimension.identifier();
        Identifier instanceId = Identifier.fromNamespaceAndPath(definition.id().getNamespace(),
                "region/" + definition.id().getPath() + "/" + dimensionId.getNamespace() + "/"
                        + dimensionId.getPath() + "/" + chunk.x() + "_" + chunk.z());
        boolean discovered = player != null && hasDiscoveredRegion(player, definition.id());
        return new WorldRegionInstance(instanceId, definition.id(), definition.type(), definition.displayName(),
                dimension, pos.immutable(), definition.radius(), definition.hazardIds(), discovered);
    }

    private WorldRegionInstance instanceForMarker(WorldMarker marker, Player player) {
        WorldRegionDefinition definition = marker.regionId() == null ? null : regionDefinitions.get(marker.regionId());
        if (definition == null) {
            return new WorldRegionInstance(marker.id(), marker.regionId() == null ? marker.id() : marker.regionId(),
                    WorldRegionType.ANOMALY_ZONE, marker.displayName(), marker.dimension(), marker.pos(),
                    marker.radius(), List.of(), marker.discovered());
        }
        boolean discovered = player != null && hasDiscoveredRegion(player, definition.id());
        return new WorldRegionInstance(marker.id(), definition.id(), definition.type(), definition.displayName(),
                marker.dimension(), marker.pos(), Math.max(marker.radius(), definition.radius()),
                definition.hazardIds(), discovered || marker.discovered());
    }

    private WorldRegionInstance instanceWithDiscovery(Player player, WorldRegionInstance instance) {
        WorldRegionDefinition definition = regionDefinitions.get(instance.definitionId());
        boolean discovered = instance.discovered() || (definition != null && hasDiscoveredRegion(player, definition.id()));
        return new WorldRegionInstance(instance.id(), instance.definitionId(), instance.type(), instance.displayName(),
                instance.dimension(), instance.center(), instance.radius(), instance.hazardIds(), discovered);
    }

    private Set<Identifier> savedDiscoveries(Player player) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return Set.of();
        }
        return WorldRegionSavedData.get(serverLevel).discoveries(player.getUUID());
    }

    private static boolean explicitDiscoverySource(WorldDiscoverySource source) {
        return source == WorldDiscoverySource.DEBUG
                || source == WorldDiscoverySource.MARKER
                || source == WorldDiscoverySource.SCAN
                || source == WorldDiscoverySource.STRUCTURE;
    }

    private boolean matchesBiome(Level level, BlockPos pos, WorldRegionDefinition definition) {
        Holder<Biome> biome = level.getBiome(pos);
        Identifier biomeId = biome.unwrapKey().map(key -> key.identifier()).orElse(null);
        if (biomeId != null && definition.biomeIds().contains(biomeId)) {
            return true;
        }
        for (Identifier tagId : definition.biomeTags()) {
            if (biome.is(TagKey.create(Registries.BIOME, tagId))) {
                return true;
            }
        }
        return false;
    }

    private Optional<WorldHazardDefinition> tickingHazard(WorldHazardSnapshot snapshot) {
        Optional<WorldHazardDefinition> explicitTickingHazard = snapshot.hazardIds().stream()
                .map(hazardDefinitions::get)
                .filter(definition -> definition != null && definition.ticking())
                .max(Comparator.comparingInt(WorldHazardDefinition::defaultSeverity)
                        .thenComparing(definition -> definition.id().toString()));
        if (explicitTickingHazard.isPresent() || snapshot.safeZone()) {
            return explicitTickingHazard;
        }
        return snapshot.hazardIds().stream()
                .map(hazardDefinitions::get)
                .filter(this::isWeatherTickingHazard)
                .max(Comparator.comparingInt(WorldHazardDefinition::defaultSeverity)
                        .thenComparing(definition -> definition.id().toString()));
    }

    private boolean isWeatherTickingHazard(WorldHazardDefinition definition) {
        return definition != null && WEATHER_TICKING_HAZARD_IDS.contains(definition.id().toString());
    }

    private EchoWorldContracts.EchoSpawnRule defaultSpawnRule(ServerPlayer player, WorldRegionInstance region) {
        Identifier entityId = defaultSpawnEntity(player);
        return new EchoWorldContracts.EchoSpawnRule(
                "echospawncore:spawn/" + sanitize(entityId.getPath()) + "/" + sanitize(region.definitionId().getPath()),
                entityId.toString(),
                region.definitionId().toString(),
                2,
                1.0D);
    }

    private Optional<Identifier> missionIdForRegion(Identifier regionId) {
        if (regionId == null) {
            return Optional.empty();
        }
        try {
            return EchoCoreServices.missionService().missionDefinitions().stream()
                    .filter(mission -> regionId.toString().equals(mission.metadata().get("worldRegion")))
                    .sorted(Comparator.comparing(mission -> mission.id().toString()))
                    .map(MissionDefinition::id)
                    .findFirst()
                    .or(() -> referenceMissionIdForRegion(regionId));
        } catch (RuntimeException exception) {
            return referenceMissionIdForRegion(regionId);
        }
    }

    private EchoWorldContracts.EchoDifficultyProfile difficultyProfileForSnapshot(WorldHazardSnapshot snapshot) {
        if (snapshot != null) {
            for (Identifier regionId : snapshot.regionIds()) {
                return difficultyProfileForRegion(regionId);
            }
        }
        return defaultDifficultyProfile();
    }

    private EchoWorldContracts.EchoDifficultyProfile difficultyProfileForRegion(Identifier regionId) {
        if (regionId == null) {
            return defaultDifficultyProfile();
        }
        Optional<Identifier> missionId = missionIdForRegion(regionId);
        Optional<String> referenceDifficulty = referenceDifficultyNameForRegion(regionId);
        String requestedDifficulty = referenceDifficulty.orElse("normal");
        boolean explicitDifficulty = referenceDifficulty.isPresent();
        if (missionId.isPresent()) {
            try {
                Optional<MissionDefinition> mission = EchoCoreServices.missionService().missionDefinition(missionId.get());
                if (mission.isPresent()) {
                    String missionDifficulty = mission.get().difficulty();
                    if (missionDifficulty != null && !missionDifficulty.isBlank()) {
                        requestedDifficulty = missionDifficulty;
                        explicitDifficulty = true;
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }
        EchoWorldContracts.EchoDifficultyProfileSelectionRequest request =
                new EchoWorldContracts.EchoDifficultyProfileSelectionRequest(
                "",
                regionId.toString(),
                missionId.map(Identifier::toString).orElse(""),
                requestedDifficulty,
                0L,
                "worldcore-difficulty-profile-selection"
        );
        EchoWorldContracts.EchoDifficultyProfileSelectionResult selection = explicitDifficulty
                ? selectDifficultyProfile(request)
                : defaultDifficultyProfileSelection(request);
        regionDifficultyProfileSelections.put(regionId.toString(), selection);
        return new EchoWorldContracts.EchoDifficultyProfile(
                selection.difficultyId(),
                selection.hazardMultiplier(),
                selection.spawnMultiplier());
    }

    private static EchoWorldContracts.EchoDifficultyProfileSelectionResult selectDifficultyProfile(
            EchoWorldContracts.EchoDifficultyProfileSelectionRequest request) {
        try {
            Class<?> bridgeClass = Class.forName("com.knoxhack.echo.adaptercore.EchoNativeDifficultyProfileSelectionBridge");
            Object bridge = bridgeClass.getConstructor(String.class).newInstance("echodifficultycore");
            Object result = bridgeClass
                    .getMethod("select", EchoWorldContracts.EchoDifficultyProfileSelectionRequest.class)
                    .invoke(bridge, request);
            if (result instanceof EchoWorldContracts.EchoDifficultyProfileSelectionResult selection) {
                return selection;
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return localDifficultyProfileSelection(request);
    }

    private static EchoWorldContracts.EchoDifficultyProfileSelectionResult localDifficultyProfileSelection(
            EchoWorldContracts.EchoDifficultyProfileSelectionRequest request) {
        String selectedDifficulty = normalizeDifficulty(request.requestedDifficulty());
        EchoWorldContracts.EchoDifficultyProfile profile = profileForNormalizedDifficulty(selectedDifficulty);
        return difficultyProfileSelection(request, selectedDifficulty, profile);
    }

    private static EchoWorldContracts.EchoDifficultyProfileSelectionResult defaultDifficultyProfileSelection(
            EchoWorldContracts.EchoDifficultyProfileSelectionRequest request) {
        return difficultyProfileSelection(request, normalizeDifficulty(request.requestedDifficulty()),
                defaultDifficultyProfile());
    }

    private static EchoWorldContracts.EchoDifficultyProfileSelectionResult difficultyProfileSelection(
            EchoWorldContracts.EchoDifficultyProfileSelectionRequest request,
            String selectedDifficulty,
            EchoWorldContracts.EchoDifficultyProfile profile) {
        return new EchoWorldContracts.EchoDifficultyProfileSelectionResult(
                request.playerId(),
                request.regionId(),
                request.missionId(),
                request.requestedDifficulty(),
                selectedDifficulty,
                profile.id(),
                profile.hazardMultiplier(),
                profile.spawnMultiplier(),
                request.gameTick(),
                request.sourceReason(),
                true);
    }

    private static Optional<Identifier> referenceMissionIdForRegion(Identifier regionId) {
        if (regionId != null
                && "echoashfallprotocol".equals(regionId.getNamespace())
                && "crash_zone_wasteland".equals(regionId.getPath())) {
            return Optional.of(Identifier.fromNamespaceAndPath("echoashfallprotocol", "mission/secure_crash_outpost"));
        }
        if (regionId != null
                && "echoashfallprotocol".equals(regionId.getNamespace())
                && "toxic_swamp".equals(regionId.getPath())) {
            return Optional.of(Identifier.fromNamespaceAndPath("echoashfallprotocol", "mission/first_relay_station_route"));
        }
        return Optional.empty();
    }

    private static Optional<EchoWorldContracts.EchoDifficultyProfile> referenceDifficultyProfileForRegion(Identifier regionId) {
        return referenceDifficultyNameForRegion(regionId).map(WorldRegionService::difficultyProfile);
    }

    private static Optional<String> referenceDifficultyNameForRegion(Identifier regionId) {
        if (regionId != null
                && "echoashfallprotocol".equals(regionId.getNamespace())
                && "crash_zone_wasteland".equals(regionId.getPath())) {
            return Optional.of("easy");
        }
        if (regionId != null
                && "echoashfallprotocol".equals(regionId.getNamespace())
                && "toxic_swamp".equals(regionId.getPath())) {
            return Optional.of("hard");
        }
        return Optional.empty();
    }

    private EchoWorldContracts.EchoStructurePlacement defaultStructurePlacement(ServerPlayer player, WorldRegionInstance region) {
        Identifier structureId = regionDefinitions.getOrDefault(region.definitionId(), null) == null
                || regionDefinitions.get(region.definitionId()).structureIds().isEmpty()
                ? Identifier.fromNamespaceAndPath(region.definitionId().getNamespace(), "drop_pod")
                : regionDefinitions.get(region.definitionId()).structureIds().get(0);
        BlockPos origin = player == null ? region.center() : player.blockPosition();
        return new EchoWorldContracts.EchoStructurePlacement(
                structureId.toString(),
                structureId.getNamespace() + ":poi/" + structureId.getPath(),
                origin.getX(),
                origin.getY(),
                origin.getZ());
    }

    private static EchoWorldContracts.EchoWorldRegion worldRegionContract(WorldRegionInstance region) {
        BlockPos center = region.center();
        int radius = Math.max(0, region.radius());
        return new EchoWorldContracts.EchoWorldRegion(
                region.definitionId().toString(),
                region.displayName(),
                center.getX() - radius,
                center.getX() + radius,
                center.getZ() - radius,
                center.getZ() + radius,
                "");
    }

    private static EchoWorldContracts.EchoWorldHazard worldHazardAtPlayer(ServerPlayer player,
            WorldHazardDefinition hazard,
            WorldHazardSnapshot snapshot) {
        BlockPos pos = player.blockPosition();
        return new EchoWorldContracts.EchoWorldHazard(
                hazard.id().toString(),
                hazard.id().getPath(),
                pos.getX(),
                pos.getZ(),
                Math.max(0, Config.activeRegionRadius()),
                hazardBaseDamage(hazard, snapshot),
                statusEffectId(hazard.id()).toString());
    }

    private static EchoWorldContracts.EchoBiomeProfile defaultBiomeProfile(ServerPlayer player, Identifier hazardId) {
        Holder<Biome> biome = player.level().getBiome(player.blockPosition());
        Identifier biomeId = biome.unwrapKey().map(key -> key.identifier()).orElse(Identifier.withDefaultNamespace("plains"));
        return new EchoWorldContracts.EchoBiomeProfile(
                biomeId.toString(),
                "#" + biomeId.getNamespace() + ":" + biomeId.getPath(),
                hazardId == null ? "" : hazardId.toString());
    }

    private static String readableStructureName(Identifier structureId) {
        String[] parts = sanitize(structureId.getPath()).replace('/', '_').split("_+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? structureId.toString() : builder.toString();
    }

    private static Identifier defaultSpawnEntity(ServerPlayer player) {
        if (player != null) {
            Holder<Biome> biome = player.level().getBiome(player.blockPosition());
            Identifier biomeId = biome.unwrapKey().map(key -> key.identifier()).orElse(null);
            if (biomeId != null && "echoashfallprotocol".equals(biomeId.getNamespace())
                    && biomeId.getPath().contains("crash_zone")) {
                return Identifier.fromNamespaceAndPath("echoashfallprotocol", "rad_zombie");
            }
        }
        return Identifier.withDefaultNamespace("zombie");
    }

    private static EchoWorldContracts.EchoDifficultyProfile defaultDifficultyProfile() {
        return new EchoWorldContracts.EchoDifficultyProfile("echodifficultycore:normal", 1.0D, 1.0D);
    }

    private static EchoWorldContracts.EchoDifficultyProfile difficultyProfile(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return null;
        }
        String normalized = normalizeDifficulty(difficulty);
        return profileForNormalizedDifficulty(normalized);
    }

    private static String normalizeDifficulty(String difficulty) {
        String normalized = difficulty == null ? "normal" : difficulty.toLowerCase(java.util.Locale.ROOT).strip();
        if (normalized.startsWith("echodifficultycore:")) {
            normalized = normalized.substring("echodifficultycore:".length());
        }
        return switch (normalized) {
            case "easy", "normal", "hard", "extreme" -> normalized;
            default -> "normal";
        };
    }

    private static EchoWorldContracts.EchoDifficultyProfile profileForNormalizedDifficulty(String normalized) {
        return switch (normalized) {
            case "easy" -> new EchoWorldContracts.EchoDifficultyProfile("echodifficultycore:easy", 1.0D, 0.85D);
            case "normal" -> new EchoWorldContracts.EchoDifficultyProfile("echodifficultycore:normal", 1.25D, 1.0D);
            case "hard" -> new EchoWorldContracts.EchoDifficultyProfile("echodifficultycore:hard", 1.5D, 1.25D);
            case "extreme" -> new EchoWorldContracts.EchoDifficultyProfile("echodifficultycore:extreme", 2.0D, 1.5D);
            default -> defaultDifficultyProfile();
        };
    }

    private static float hazardDamage(WorldHazardDefinition hazard, WorldHazardSnapshot snapshot) {
        return hazardBaseDamage(hazard, snapshot);
    }

    private static float hazardBaseDamage(WorldHazardDefinition hazard, WorldHazardSnapshot snapshot) {
        int severity = Math.max(hazard.defaultSeverity(), snapshot.severity());
        return Math.max(0.5F, severity / 50.0F);
    }

    private static Identifier statusEffectId(Identifier hazardId) {
        String path = hazardId.getPath();
        if (!EchoWorldCore.MODID.equals(hazardId.getNamespace()) && path.startsWith("hazard/")) {
            path = path.substring("hazard/".length());
        }
        return Identifier.fromNamespaceAndPath("echostatuscore", "status/" + sanitize(path));
    }

    private static String statusSaveKey(Identifier hazardId) {
        return "echoworldcore.hazard." + sanitize(hazardId.getPath()).replace('/', '_') + ".status";
    }

    private static String chunkKey(String worldId, int x, int z) {
        return (worldId == null ? "" : worldId) + ":chunk:" + Math.floorDiv(x, 16) + ":" + Math.floorDiv(z, 16);
    }

    private static String spawnZoneKey(String regionId, String ruleId) {
        return (regionId == null ? "" : regionId) + "|" + (ruleId == null ? "" : ruleId);
    }

    private static String firstRegionId(WorldHazardSnapshot snapshot) {
        if (snapshot == null || snapshot.regionIds().isEmpty()) {
            return "";
        }
        return snapshot.regionIds().iterator().next().toString();
    }

    private void recordDifficultyApplication(ServerPlayer player,
            String regionId,
            EchoWorldContracts.EchoDifficultyProfile difficulty,
            String hazardId,
            double baseDamage,
            double scaledDamage,
            String spawnRuleId,
            int maxSpawnCount,
            int scaledSpawnBudget,
            int activeSpawnPopulation,
            long gameTick,
            String sourceReason) {
        if (player == null || difficulty == null) {
            return;
        }
        DifficultyApplicationState previous = activeDifficultyApplicationStates.get(player.getUUID());
        if ((regionId == null || regionId.isBlank()) && previous != null) {
            regionId = previous.regionId();
        }
        DifficultyApplicationState state = DifficultyApplicationState.merge(
                previous,
                player.getUUID().toString(),
                regionId == null ? "" : regionId,
                difficulty,
                hazardId,
                baseDamage,
                scaledDamage,
                spawnRuleId,
                maxSpawnCount,
                scaledSpawnBudget,
                activeSpawnPopulation,
                gameTick,
                sourceReason);
        activeDifficultyApplicationStates.put(player.getUUID(), state);
        EchoWorldContracts.EchoDifficultyApplicationResult result =
                new EchoNativeDifficultyApplicationBridge(EchoWorldCore.MODID)
                        .apply(new EchoWorldContracts.EchoDifficultyApplicationRequest(
                                state.playerId(),
                                state.regionId(),
                                state.appliedHazardId(),
                                state.baseHazardDamage(),
                                state.scaledHazardDamage(),
                                state.appliedSpawnRuleId(),
                                state.maxSpawnCount(),
                                state.scaledSpawnBudget(),
                                state.activeSpawnPopulation(),
                                state.lastGameTick(),
                                state.sourceReason(),
                                difficulty));
        activeDifficultyApplicationResults.put(player.getUUID(), result);
        if (!state.regionId().isBlank()) {
            regionDifficultyApplicationStates.put(state.regionId(), state);
            regionDifficultyApplicationResults.put(state.regionId(), result);
        }
    }

    private void recordActiveStatusEffect(Player player,
            String effectId,
            String hazardId,
            String saveKey,
            int durationTicks,
            int amplifier,
            double damageApplied,
            long appliedGameTick,
            boolean loaded) {
        if (player == null || effectId == null || effectId.isBlank()) {
            return;
        }
        ActiveStatusEffectState previous =
                activeStatusEffectStates.getOrDefault(player.getUUID(), Map.of()).get(effectId);
        EchoWorldContracts.EchoStatusEffectStackingResult stacking =
                new EchoNativeStatusEffectStackingBridge(EchoWorldCore.MODID)
                        .stack(new EchoWorldContracts.EchoStatusEffectStackingRequest(
                                player.getUUID().toString(),
                                hazardId == null ? "" : hazardId,
                                "REFRESH_DURATION",
                                previous == null ? 0 : previous.durationTicks(),
                                previous == null ? 0 : previous.amplifier(),
                                previous == null ? 0.0D : previous.damageApplied(),
                                previous == null ? 0L : previous.appliedGameTick(),
                                previous == null ? 0L : previous.expiresAtTick(),
                                (float) Math.max(0.0D, damageApplied),
                                Math.max(0L, appliedGameTick),
                                loaded ? "worldcore-status-load-stacking" : "worldcore-status-apply-stacking",
                                new EchoWorldContracts.EchoStatusEffect(
                                        effectId,
                                        Math.max(0, durationTicks),
                                        Math.max(0, amplifier),
                                        saveKey == null ? "" : saveKey),
                                previous != null,
                                loaded));
        lastStatusEffectStackings.compute(player.getUUID(), (id, stackings) -> {
            LinkedHashMap<String, EchoWorldContracts.EchoStatusEffectStackingResult> updated = new LinkedHashMap<>();
            if (stackings != null) {
                updated.putAll(stackings);
            }
            updated.put(effectId, stacking);
            return Map.copyOf(updated);
        });
        ActiveStatusEffectState state = new ActiveStatusEffectState(
                effectId,
                hazardId == null ? "" : hazardId,
                saveKey == null ? "" : saveKey,
                stacking.durationTicks(),
                stacking.amplifier(),
                stacking.damageApplied(),
                stacking.appliedGameTick(),
                stacking.expiresAtTick(),
                stacking.loaded());
        activeStatusEffectStates.compute(player.getUUID(), (id, states) -> {
            LinkedHashMap<String, ActiveStatusEffectState> updated = new LinkedHashMap<>();
            if (states != null) {
                updated.putAll(states);
            }
            updated.put(effectId, state);
            return Map.copyOf(updated);
        });
        ArrayList<String> effects = new ArrayList<>(activeStatusEffects.getOrDefault(player.getUUID(), List.of()));
        if (!effects.contains(effectId)) {
            effects.add(effectId);
        }
        activeStatusEffects.put(player.getUUID(), List.copyOf(effects));
    }

    private void recordStatusProfileApplication(Player player,
            String hazardId,
            String effectId,
            String saveKey,
            int durationTicks,
            int amplifier,
            double damageApplied,
            long gameTick,
            boolean persisted,
            boolean loaded) {
        if (player == null || effectId == null || effectId.isBlank()) {
            return;
        }
        Identifier parsedHazardId = Identifier.tryParse(hazardId == null ? "" : hazardId);
        int severity = parsedHazardId == null
                ? 0
                : hazardDefinitions.getOrDefault(parsedHazardId, null) == null
                        ? 0
                        : hazardDefinitions.get(parsedHazardId).defaultSeverity();
        StatusProfileState state = new StatusProfileState(
                player.getUUID().toString(),
                hazardId == null ? "" : hazardId,
                effectId,
                saveKey == null ? "" : saveKey,
                statusKind(hazardId),
                severityBand(severity),
                "REFRESH_DURATION",
                Math.max(0, durationTicks),
                Math.max(0, amplifier),
                Math.max(0.0D, damageApplied),
                Math.max(1.0D, severity / 25.0D),
                Math.max(0.2D, severity / 125.0D),
                persisted,
                loaded,
                Math.max(0L, gameTick),
                loaded ? "WorldRegionService.loadStatusEffect" : "WorldRegionService.persistStatusEffect");
        activeStatusProfileStates.compute(player.getUUID(), (id, states) -> {
            LinkedHashMap<String, StatusProfileState> updated = new LinkedHashMap<>();
            if (states != null) {
                updated.putAll(states);
            }
            updated.put(effectId, state);
            return Map.copyOf(updated);
        });
    }

    private void pruneActiveStatusEffects(Player player, long gameTick) {
        if (player == null) {
            return;
        }
        Map<String, ActiveStatusEffectState> states = activeStatusEffectStates.get(player.getUUID());
        if (states == null || states.isEmpty()) {
            activeStatusEffects.remove(player.getUUID());
            activeStatusProfileStates.remove(player.getUUID());
            return;
        }
        LinkedHashMap<String, ActiveStatusEffectState> retained = new LinkedHashMap<>();
        for (Map.Entry<String, ActiveStatusEffectState> entry : states.entrySet()) {
            ActiveStatusEffectState state = entry.getValue();
            EchoWorldContracts.EchoStatusEffectExpiryResult expiry =
                    new EchoNativeStatusEffectExpiryBridge(EchoWorldCore.MODID)
                            .evaluate(new EchoWorldContracts.EchoStatusEffectExpiryRequest(
                                    player.getUUID().toString(),
                                    state.hazardId(),
                                    state.effectId(),
                                    state.saveKey(),
                                    state.appliedGameTick(),
                                    state.expiresAtTick(),
                                    gameTick,
                                    "worldcore-status-expiry"));
            lastStatusEffectExpiries.compute(player.getUUID(), (id, expiries) -> {
                LinkedHashMap<String, EchoWorldContracts.EchoStatusEffectExpiryResult> updated = new LinkedHashMap<>();
                if (expiries != null) {
                    updated.putAll(expiries);
                }
                updated.put(expiry.effectId(), expiry);
                return Map.copyOf(updated);
            });
            if (expiry.retained()) {
                retained.put(entry.getKey(), entry.getValue());
            }
        }
        if (retained.isEmpty()) {
            activeStatusEffectStates.remove(player.getUUID());
            activeStatusEffects.remove(player.getUUID());
            activeStatusProfileStates.remove(player.getUUID());
            return;
        }
        activeStatusEffectStates.put(player.getUUID(), Map.copyOf(retained));
        activeStatusEffects.put(player.getUUID(), List.copyOf(retained.keySet()));
        Map<String, StatusProfileState> profileStates = activeStatusProfileStates.get(player.getUUID());
        if (profileStates != null && !profileStates.isEmpty()) {
            LinkedHashMap<String, StatusProfileState> retainedProfiles = new LinkedHashMap<>();
            retained.keySet().forEach(effectId -> {
                StatusProfileState state = profileStates.get(effectId);
                if (state != null) {
                    retainedProfiles.put(effectId, state);
                }
            });
            if (retainedProfiles.isEmpty()) {
                activeStatusProfileStates.remove(player.getUUID());
            } else {
                activeStatusProfileStates.put(player.getUUID(), Map.copyOf(retainedProfiles));
            }
        }
    }

    private static net.minecraft.core.Holder<MobEffect> statusEffect(Identifier hazardId) {
        String path = hazardId.getPath();
        if (path.contains("toxic") || path.contains("radiation")) {
            return MobEffects.POISON;
        }
        if (path.contains("cryo") || path.contains("cold")) {
            return MobEffects.SLOWNESS;
        }
        if (path.contains("nexus") || path.contains("anomaly")) {
            return MobEffects.NAUSEA;
        }
        return MobEffects.WEAKNESS;
    }

    private static int statusAmplifier(WorldHazardDefinition hazard) {
        return Math.max(0, hazard.defaultSeverity() / 50);
    }

    private static String statusKind(String hazardId) {
        String path = hazardId == null ? "" : hazardId.toLowerCase(java.util.Locale.ROOT);
        if (path.contains("radiation")) {
            return "RADIATION";
        }
        if (path.contains("toxic")) {
            return "TOXIN";
        }
        if (path.contains("cryo") || path.contains("cold")) {
            return "THERMAL";
        }
        if (path.contains("nexus") || path.contains("anomaly")) {
            return "ANOMALY";
        }
        return "ENVIRONMENTAL_HAZARD";
    }

    private static String severityBand(int severity) {
        if (severity >= 100) {
            return "CRITICAL";
        }
        if (severity >= 75) {
            return "HIGH";
        }
        if (severity >= 35) {
            return "MEDIUM";
        }
        if (severity > 0) {
            return "LOW";
        }
        return "UNKNOWN";
    }

    private synchronized void rebuildDefinitionViews() {
        hazardDefinitions.clear();
        hazardDefinitions.putAll(baseHazardDefinitions);
        hazardDefinitions.putAll(dataHazardDefinitions);
        regionDefinitions.clear();
        regionDefinitions.putAll(baseRegionDefinitions);
        regionDefinitions.putAll(dataRegionDefinitions);
    }

    private static void addWarning(List<String> warnings, String warning) {
        if (warnings.size() < MAX_VALIDATION_WARNINGS) {
            warnings.add(warning);
        } else if (warnings.size() == MAX_VALIDATION_WARNINGS) {
            warnings.add("Additional WorldCore validation warnings omitted.");
        }
    }

    private static void addIssue(List<WorldCoreValidationIssue> issues, List<String> warnings,
            String category, String message, WorldCoreValidationIssue.Severity severity) {
        if (message == null || message.isBlank()) {
            return;
        }
        addWarning(warnings, message.strip());
        if (issues.size() < MAX_VALIDATION_WARNINGS) {
            issues.add(new WorldCoreValidationIssue(issueId(category, issues.size()), severity, category, message));
        } else if (issues.size() == MAX_VALIDATION_WARNINGS) {
            issues.add(new WorldCoreValidationIssue(issueId("omitted", issues.size()),
                    WorldCoreValidationIssue.Severity.WARNING,
                    "omitted",
                    "Additional WorldCore validation warnings omitted."));
        }
    }

    private static List<WorldCoreValidationIssue> boundedIssues(List<WorldCoreValidationIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        List<WorldCoreValidationIssue> bounded = new ArrayList<>();
        for (WorldCoreValidationIssue issue : issues) {
            if (issue != null && bounded.size() < MAX_VALIDATION_WARNINGS + 1) {
                bounded.add(issue);
            }
        }
        return List.copyOf(bounded);
    }

    private static List<String> boundedWarnings(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return List.of();
        }
        List<String> bounded = new ArrayList<>();
        for (String warning : warnings) {
            if (warning != null && !warning.isBlank()) {
                addWarning(bounded, warning.strip());
            }
        }
        return List.copyOf(bounded);
    }

    private static Map<String, Integer> sourceCounts(Set<Identifier> ids) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        ids.stream()
                .sorted(Comparator.comparing(Identifier::toString))
                .forEach(id -> counts.merge(id.getNamespace(), 1, Integer::sum));
        return Map.copyOf(counts);
    }

    private static int safeRadius(int radius) {
        return Math.min(Math.max(1, radius), Config.markerQueryRadiusCap());
    }

    private Comparator<WorldRegionInstance> regionComparator(BlockPos origin) {
        return Comparator.comparingInt((WorldRegionInstance region) -> regionSortOrder(region.definitionId()))
                .thenComparingDouble(region -> origin == null ? 0.0D : region.center().distSqr(origin))
                .thenComparing(region -> region.definitionId().toString())
                .thenComparing(region -> region.id().toString());
    }

    private int regionSortOrder(Identifier definitionId) {
        WorldRegionDefinition definition = regionDefinitions.get(definitionId);
        return definition == null ? Integer.MAX_VALUE : definition.sortOrder();
    }

    private static Comparator<WorldMarker> markerComparator(BlockPos origin) {
        return Comparator.comparingDouble((WorldMarker marker) -> origin == null ? 0.0D : marker.pos().distSqr(origin))
                .thenComparing(marker -> marker.type().name())
                .thenComparing(marker -> marker.id().toString());
    }

    private static Identifier markerId(Identifier source, BlockPos pos) {
        String path = "marker/" + sanitize(source.getPath()) + "/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
        return Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, path);
    }

    private static Identifier issueId(String category, int index) {
        return Identifier.fromNamespaceAndPath(EchoWorldCore.MODID,
                "validation/" + sanitize(category) + "/" + Math.max(0, index));
    }

    private static String sanitize(String value) {
        return value == null ? "unknown" : value.replace(':', '_').replace('\\', '/');
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private record MarkerReveal(WorldMarker marker, boolean changed) {
    }

    private record DifficultyApplicationState(
            String playerId,
            String regionId,
            String difficultyId,
            double hazardMultiplier,
            double spawnMultiplier,
            String appliedHazardId,
            double baseHazardDamage,
            double scaledHazardDamage,
            String appliedSpawnRuleId,
            int maxSpawnCount,
            int scaledSpawnBudget,
            int activeSpawnPopulation,
            long lastGameTick,
            String sourceReason) {
        private static DifficultyApplicationState merge(DifficultyApplicationState previous,
                String playerId,
                String regionId,
                EchoWorldContracts.EchoDifficultyProfile difficulty,
                String hazardId,
                double baseDamage,
                double scaledDamage,
                String spawnRuleId,
                int maxSpawnCount,
                int scaledSpawnBudget,
                int activeSpawnPopulation,
                long gameTick,
                String sourceReason) {
            boolean hasHazard = hazardId != null && !hazardId.isBlank();
            boolean hasSpawn = spawnRuleId != null && !spawnRuleId.isBlank();
            return new DifficultyApplicationState(
                    playerId,
                    regionId,
                    difficulty.id(),
                    difficulty.hazardMultiplier(),
                    difficulty.spawnMultiplier(),
                    hasHazard || previous == null ? text(hazardId) : previous.appliedHazardId(),
                    hasHazard || previous == null ? Math.max(0.0D, baseDamage) : previous.baseHazardDamage(),
                    hasHazard || previous == null ? Math.max(0.0D, scaledDamage) : previous.scaledHazardDamage(),
                    hasSpawn || previous == null ? text(spawnRuleId) : previous.appliedSpawnRuleId(),
                    hasSpawn || previous == null ? Math.max(0, maxSpawnCount) : previous.maxSpawnCount(),
                    hasSpawn || previous == null ? Math.max(0, scaledSpawnBudget) : previous.scaledSpawnBudget(),
                    hasSpawn || previous == null ? Math.max(0, activeSpawnPopulation) : previous.activeSpawnPopulation(),
                    Math.max(0L, gameTick),
                    text(sourceReason));
        }

        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> state = new LinkedHashMap<>();
            state.put("playerId", playerId);
            state.put("regionId", regionId);
            state.put("difficultyId", difficultyId);
            state.put("hazardMultiplier", hazardMultiplier);
            state.put("spawnMultiplier", spawnMultiplier);
            state.put("appliedHazardId", appliedHazardId);
            state.put("baseHazardDamage", baseHazardDamage);
            state.put("scaledHazardDamage", scaledHazardDamage);
            state.put("appliedSpawnRuleId", appliedSpawnRuleId);
            state.put("maxSpawnCount", maxSpawnCount);
            state.put("scaledSpawnBudget", scaledSpawnBudget);
            state.put("activeSpawnPopulation", activeSpawnPopulation);
            state.put("lastGameTick", lastGameTick);
            state.put("sourceReason", sourceReason);
            return Map.copyOf(state);
        }
    }

    private record ActiveStatusEffectState(
            String effectId,
            String hazardId,
            String saveKey,
            int durationTicks,
            int amplifier,
            double damageApplied,
            long appliedGameTick,
            long expiresAtTick,
            boolean loaded) {
        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> state = new LinkedHashMap<>();
            state.put("effectId", effectId);
            state.put("hazardId", hazardId);
            state.put("saveKey", saveKey);
            state.put("durationTicks", durationTicks);
            state.put("amplifier", amplifier);
            state.put("damageApplied", damageApplied);
            state.put("appliedGameTick", appliedGameTick);
            state.put("expiresAtTick", expiresAtTick);
            state.put("loaded", loaded);
            return Map.copyOf(state);
        }
    }

    private record StatusProfileState(
            String playerId,
            String hazardId,
            String effectId,
            String saveKey,
            String statusKind,
            String severity,
            String stackingPolicy,
            int durationTicks,
            int amplifier,
            double damageApplied,
            double exposureIntensity,
            double accumulationPerSecond,
            boolean persisted,
            boolean loaded,
            long lastGameTick,
            String sourceReason) {
        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> state = new LinkedHashMap<>();
            state.put("playerId", playerId);
            state.put("hazardId", hazardId);
            state.put("effectId", effectId);
            state.put("saveKey", saveKey);
            state.put("statusKind", statusKind);
            state.put("severity", severity);
            state.put("stackingPolicy", stackingPolicy);
            state.put("durationTicks", durationTicks);
            state.put("amplifier", amplifier);
            state.put("damageApplied", damageApplied);
            state.put("exposureIntensity", exposureIntensity);
            state.put("accumulationPerSecond", accumulationPerSecond);
            state.put("persisted", persisted);
            state.put("loaded", loaded);
            state.put("lastGameTick", lastGameTick);
            state.put("sourceReason", sourceReason);
            return Map.copyOf(state);
        }
    }

    private record SpawnZoneState(
            String regionId,
            String ruleId,
            String entityId,
            String difficultyId,
            int maxCount,
            int activeMobCount,
            int scaledBudget,
            int spawnCount,
            int activePopulation,
            double spawnMultiplier,
            double difficultyWeight,
            String eventType,
            int x,
            int y,
            int z,
            long lastGameTick) {
        private static SpawnZoneState from(EchoWorldContracts.EchoSpawnZoneStateResult result) {
            return new SpawnZoneState(
                    result.regionId(),
                    result.ruleId(),
                    result.entityId(),
                    result.difficultyId(),
                    result.maxCount(),
                    result.activeMobCount(),
                    result.scaledBudget(),
                    result.spawnCount(),
                    result.activePopulation(),
                    result.spawnMultiplier(),
                    result.difficultyWeight(),
                    result.eventType(),
                    result.x(),
                    result.y(),
                    result.z(),
                    result.lastGameTick());
        }

        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> state = new LinkedHashMap<>();
            state.put("regionId", regionId);
            state.put("ruleId", ruleId);
            state.put("entityId", entityId);
            state.put("difficultyId", difficultyId);
            state.put("maxCount", maxCount);
            state.put("activeMobCount", activeMobCount);
            state.put("scaledBudget", scaledBudget);
            state.put("spawnCount", spawnCount);
            state.put("activePopulation", activePopulation);
            state.put("spawnMultiplier", spawnMultiplier);
            state.put("difficultyWeight", difficultyWeight);
            state.put("eventType", eventType);
            state.put("x", x);
            state.put("y", y);
            state.put("z", z);
            state.put("lastGameTick", lastGameTick);
            return Map.copyOf(state);
        }
    }

    private record WorldChunkState(
            String worldId,
            String chunkKey,
            int chunkX,
            int chunkZ,
            String lastCellKey,
            int lastSampleX,
            int lastSampleY,
            int lastSampleZ,
            String activeRegionId,
            String activeHazardId,
            String biomeProfileId,
            String structureId,
            String poiId,
            boolean inRegion,
            boolean inHazard,
            long lastGameTick) {
        private static WorldChunkState from(EchoWorldContracts.EchoWorldCellSampleResult result) {
            int chunkX = Math.floorDiv(result.x(), 16);
            int chunkZ = Math.floorDiv(result.z(), 16);
            return new WorldChunkState(
                    result.worldId(),
                    WorldRegionService.chunkKey(result.worldId(), result.x(), result.z()),
                    chunkX,
                    chunkZ,
                    result.cellKey(),
                    result.x(),
                    result.y(),
                    result.z(),
                    result.activeRegionId(),
                    result.activeHazardId(),
                    result.biomeProfileId(),
                    result.structureId(),
                    result.poiId(),
                    result.inRegion(),
                    result.inHazard(),
                    result.gameTick());
        }

        private static WorldChunkState from(EchoWorldContracts.EchoWorldChunkStateResult result) {
            return new WorldChunkState(
                    result.worldId(),
                    result.chunkKey(),
                    result.chunkX(),
                    result.chunkZ(),
                    result.lastCellKey(),
                    result.lastSampleX(),
                    result.lastSampleY(),
                    result.lastSampleZ(),
                    result.activeRegionId(),
                    result.activeHazardId(),
                    result.biomeProfileId(),
                    result.structureId(),
                    result.poiId(),
                    result.inRegion(),
                    result.inHazard(),
                    result.lastGameTick());
        }

        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> state = new LinkedHashMap<>();
            state.put("worldId", worldId);
            state.put("chunkKey", chunkKey);
            state.put("chunkX", chunkX);
            state.put("chunkZ", chunkZ);
            state.put("lastCellKey", lastCellKey);
            state.put("lastSampleX", lastSampleX);
            state.put("lastSampleY", lastSampleY);
            state.put("lastSampleZ", lastSampleZ);
            state.put("activeRegionId", activeRegionId);
            state.put("activeHazardId", activeHazardId);
            state.put("biomeProfileId", biomeProfileId);
            state.put("structureId", structureId);
            state.put("poiId", poiId);
            state.put("inRegion", inRegion);
            state.put("inHazard", inHazard);
            state.put("lastGameTick", lastGameTick);
            return Map.copyOf(state);
        }
    }

    private record HazardFieldState(
            String hazardId,
            String type,
            int centerX,
            int centerZ,
            int radius,
            double damagePerTick,
            String statusEffectId,
            String lastCellKey,
            String worldId,
            boolean sampledInside,
            long lastGameTick) {
        private static HazardFieldState from(EchoWorldContracts.EchoWorldHazard hazard,
                EchoWorldContracts.EchoWorldCellSampleResult result) {
            return new HazardFieldState(
                    hazard.id(),
                    hazard.type(),
                    hazard.centerX(),
                    hazard.centerZ(),
                    hazard.radius(),
                    hazard.damagePerTick(),
                    hazard.statusEffectId(),
                    result.cellKey(),
                    result.worldId(),
                    result.inHazard(),
                    result.gameTick());
        }

        private static HazardFieldState from(EchoWorldContracts.EchoHazardFieldStateResult result) {
            return new HazardFieldState(
                    result.hazardId(),
                    result.type(),
                    result.centerX(),
                    result.centerZ(),
                    result.radius(),
                    result.damagePerTick(),
                    result.statusEffectId(),
                    result.lastCellKey(),
                    result.worldId(),
                    result.sampledInside(),
                    result.lastGameTick());
        }

        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> state = new LinkedHashMap<>();
            state.put("hazardId", hazardId);
            state.put("type", type);
            state.put("centerX", centerX);
            state.put("centerZ", centerZ);
            state.put("radius", radius);
            state.put("damagePerTick", damagePerTick);
            state.put("statusEffectId", statusEffectId);
            state.put("lastCellKey", lastCellKey);
            state.put("worldId", worldId);
            state.put("sampledInside", sampledInside);
            state.put("lastGameTick", lastGameTick);
            return Map.copyOf(state);
        }
    }

    private record StructurePoiState(
            String markerId,
            String regionId,
            String structureId,
            String poiId,
            int x,
            int y,
            int z,
            long distanceSquared,
            int maxDistance,
            boolean inRange,
            String lookupType,
            long lastGameTick) {
        private static StructurePoiState from(EchoWorldContracts.EchoStructurePoiLookupResult result) {
            return new StructurePoiState(
                    result.markerId(),
                    result.regionId(),
                    result.structureId(),
                    result.poiId(),
                    result.x(),
                    result.y(),
                    result.z(),
                    result.distanceSquared(),
                    result.maxDistance(),
                    result.inRange(),
                    result.lookupType(),
                    result.gameTick());
        }

        private static StructurePoiState from(EchoWorldContracts.EchoStructurePoiMarkerStateResult result) {
            return new StructurePoiState(
                    result.markerId(),
                    result.regionId(),
                    result.structureId(),
                    result.poiId(),
                    result.x(),
                    result.y(),
                    result.z(),
                    result.distanceSquared(),
                    result.maxDistance(),
                    result.inRange(),
                    result.lookupType(),
                    result.lastGameTick());
        }

        private Map<String, Object> toMap() {
            LinkedHashMap<String, Object> state = new LinkedHashMap<>();
            state.put("markerId", markerId);
            state.put("regionId", regionId);
            state.put("structureId", structureId);
            state.put("poiId", poiId);
            state.put("x", x);
            state.put("y", y);
            state.put("z", z);
            state.put("distanceSquared", distanceSquared);
            state.put("maxDistance", maxDistance);
            state.put("inRange", inRange);
            state.put("lookupType", lookupType);
            state.put("lastGameTick", lastGameTick);
            return Map.copyOf(state);
        }
    }

    public record HazardTickResult(
            Identifier hazardId,
            Identifier statusEffectId,
            float damageApplied,
            float healthBefore,
            float healthAfter,
            boolean saved) {
    }
}
