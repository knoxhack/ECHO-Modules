package com.knoxhack.echoarcanacore.integration.veilbound;

import com.mojang.datafixers.util.Pair;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoarcanacore.EchoArcanaCore;
import com.knoxhack.echoarcanacore.api.VeilboundRuntimeSnapshot;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public final class VeilboundRuntimeBridge {
    private static final String PLAYER_DATA_CLASS = "com.knoxhack.arcanaveil.data.ArcanaPlayerData";
    private static final String WORLD_DATA_CLASS = "com.knoxhack.arcanaveil.world.VeilWorldData";
    private static final Set<String> MARKER_LANDMARKS = Set.of(
            "abandoned_ritual_circle",
            "ancient_observatory",
            "buried_research_vault",
            "fractured_grove",
            "sealed_archive");
    private static final Set<String> STRUCTURE_LANDMARKS = Set.of(
            "abandoned_ritual_circle",
            "ancient_observatory",
            "buried_research_vault",
            "deep_veil_gate",
            "fracture_rift",
            "fractured_grove",
            "harmonic_spring",
            "sealed_archive",
            "veil_monolith");
    private static final int LEGACY_STRUCTURE_BACKFILL_CHUNK_RADIUS = 96;
    private static final ConcurrentMap<UUID, ConcurrentMap<String, LocatedSignal>> LOCATIONS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Integer> LAST_SYNC = new ConcurrentHashMap<>();
    private static final AtomicBoolean WARNED_REFLECTION = new AtomicBoolean(false);

    private VeilboundRuntimeBridge() {
    }

    public static VeilboundRuntimeSnapshot snapshot(Player player) {
        boolean loaded = EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID);
        if (!loaded || player == null) {
            return VeilboundRuntimeSnapshot.unavailable(loaded);
        }
        ResourceKey<Level> dimension = player.level() == null ? Level.OVERWORLD : player.level().dimension();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        try {
            Class<?> playerDataClass = Class.forName(PLAYER_DATA_CLASS);
            Object data = playerDataClass.getMethod("get", Player.class).invoke(null, player);
            int scanCount = intValue(invoke(data, "scanCount"));
            Set<String> scannedTargets = strings(invoke(data, "scannedTargets"));
            Set<String> knownResonances = strings(invoke(data, "knownResonances"));
            Set<String> unlockedResearch = strings(invoke(data, "unlockedResearch"));
            Set<String> theories = strings(invoke(data, "theories"));
            Set<String> usedMachines = strings(invoke(data, "usedMachines"));
            Set<String> events = strings(invoke(data, "events"));
            Map<String, Integer> observations = observations(invoke(data, "observations"));
            Map<String, VeilboundRuntimeSnapshot.ScanCoordinate> scanCoordinates = scanCoordinates(invokeOptional(data, "scanCoordinates"));
            String activeResearch = stringValue(invoke(data, "activeResearch"));
            String endgamePath = stringValue(invoke(data, "endgamePath"));
            int lastVeilPressure = intValue(invoke(data, "lastVeilPressure"), -1);
            int lastFracturePressure = intValue(invoke(data, "lastFracturePressure"), -1);
            String lastFieldState = stringValue(invoke(data, "lastFieldState"));

            boolean worldDataAvailable = false;
            int localVeilPressure = -1;
            int localFracturePressure = -1;
            String localFieldState = "";
            boolean veilboundGuardianDefeated = false;
            boolean unwrittenOneDefeated = false;
            boolean fractureHeartDefeated = false;
            if (player.level() instanceof ServerLevel serverLevel) {
                try {
                    Class<?> worldDataClass = Class.forName(WORLD_DATA_CLASS);
                    Object worldData = worldDataClass.getMethod("get", ServerLevel.class).invoke(null, serverLevel);
                    ChunkPos chunk = player.chunkPosition();
                    localVeilPressure = intValue(worldDataClass.getMethod("veilPressure", ChunkPos.class)
                            .invoke(worldData, chunk), -1);
                    localFracturePressure = intValue(worldDataClass.getMethod("fracturePressure", ChunkPos.class)
                            .invoke(worldData, chunk), -1);
                    localFieldState = stringValue(worldDataClass.getMethod("fieldState", ChunkPos.class)
                            .invoke(worldData, chunk));
                    veilboundGuardianDefeated = boolValue(invoke(worldData, "veilboundGuardianDefeated"));
                    unwrittenOneDefeated = boolValue(invoke(worldData, "unwrittenOneDefeated"));
                    fractureHeartDefeated = boolValue(invoke(worldData, "fractureHeartDefeated"));
                    String worldPath = stringValue(invoke(worldData, "endgamePath"));
                    if (!worldPath.isBlank()) {
                        endgamePath = worldPath;
                    }
                    worldDataAvailable = true;
                } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                    warnOnce("ARCANA world data bridge could not read live pressure values.", exception);
                }
            }

            return new VeilboundRuntimeSnapshot(
                    true,
                    true,
                    scanCount,
                    scannedTargets,
                    knownResonances,
                    unlockedResearch,
                    theories,
                    usedMachines,
                    events,
                    observations,
                    scanCoordinates,
                    activeResearch,
                    endgamePath,
                    lastVeilPressure,
                    lastFracturePressure,
                    lastFieldState,
                    worldDataAvailable,
                    localVeilPressure,
                    localFracturePressure,
                    localFieldState,
                    veilboundGuardianDefeated,
                    unwrittenOneDefeated,
                    fractureHeartDefeated,
                    dimension,
                    x,
                    y,
                    z);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            warnOnce("ARCANA player data bridge could not read Field Journal state.", exception);
            return VeilboundRuntimeSnapshot.unavailable(true);
        }
    }

    public static void rememberScanLocation(ServerPlayer player, Identifier discoveryId, BlockPos pos) {
        if (player == null || discoveryId == null || pos == null || player.level() == null) {
            return;
        }
        LOCATIONS.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>())
                .put(discoveryId.toString(), new LocatedSignal(
                        discoveryId,
                        player.level().dimension(),
                        pos.getX() + 0.5D,
                        pos.getY(),
                        pos.getZ() + 0.5D,
                        player.level().getGameTime()));
    }

    public static Optional<LocatedSignal> location(Player player, Identifier discoveryId) {
        if (player == null || discoveryId == null) {
            return Optional.empty();
        }
        Map<String, LocatedSignal> playerLocations = LOCATIONS.get(player.getUUID());
        if (playerLocations == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playerLocations.get(discoveryId.toString()));
    }

    public static Optional<LocatedSignal> location(Player player, VeilboundRuntimeSnapshot snapshot, VeilboundBridgeCatalog.Entry entry) {
        if (entry == null) {
            return Optional.empty();
        }
        Identifier discoveryId = VeilboundBridgeCatalog.discoveryId(entry);
        Optional<LocatedSignal> remembered = location(player, discoveryId);
        if (remembered.isPresent()) {
            return remembered;
        }
        if (snapshot == null || !snapshot.available()) {
            return Optional.empty();
        }
        for (String key : coordinateKeys(entry)) {
            Optional<VeilboundRuntimeSnapshot.ScanCoordinate> coordinate = snapshot.scanCoordinate(key);
            if (coordinate.isPresent()) {
                VeilboundRuntimeSnapshot.ScanCoordinate value = coordinate.get();
                return Optional.of(new LocatedSignal(discoveryId, value.dimension(), value.x(), value.y(), value.z(), value.gameTime()));
            }
        }
        return backfillLegacyStructureLocation(player, snapshot, entry, discoveryId);
    }

    public static boolean isDiscovered(VeilboundRuntimeSnapshot snapshot, VeilboundBridgeCatalog.Entry entry) {
        if (snapshot == null || !snapshot.available() || entry == null) {
            return false;
        }
        return switch (entry.kind()) {
            case LANDMARK -> hasLandmark(snapshot, entry);
            case BOSS_GATE -> isBossDiscovered(snapshot, entry);
            case RESEARCH -> hasResearch(snapshot, entry);
            case ENTITY -> hasEntity(snapshot, entry);
            case BLOCK -> hasBlock(snapshot, entry);
            case ITEM -> hasItem(snapshot, entry);
            default -> false;
        };
    }

    public static boolean isChecked(VeilboundRuntimeSnapshot snapshot, VeilboundBridgeCatalog.Entry entry) {
        if (snapshot == null || !snapshot.available() || entry == null || entry.kind() != VeilboundBridgeCatalog.Kind.BOSS_GATE) {
            return false;
        }
        String path = VeilboundBridgeCatalog.entryPath(entry);
        return switch (path) {
            case "veilbound_guardian" -> snapshot.veilboundGuardianDefeated();
            case "unwritten_one" -> snapshot.unwrittenOneDefeated();
            case "fracture_heart" -> snapshot.fractureHeartDefeated();
            case "deep_veil_gate" -> snapshot.unwrittenOneDefeated() || snapshot.fractureHeartDefeated();
            default -> false;
        };
    }

    public static void syncServerProgress(ServerPlayer player, VeilboundRuntimeSnapshot snapshot) {
        if (player == null || snapshot == null || !snapshot.available()) {
            return;
        }
        int fingerprint = Objects.hash(
                snapshot.scanCount(),
                snapshot.scannedTargets(),
                snapshot.observations(),
                snapshot.scanCoordinates(),
                snapshot.unlockedResearch(),
                snapshot.usedMachines(),
                snapshot.events(),
                snapshot.activeResearch(),
                snapshot.endgamePath(),
                snapshot.effectiveVeilPressure(),
                snapshot.effectiveFracturePressure(),
                snapshot.effectiveFieldState(),
                snapshot.veilboundGuardianDefeated(),
                snapshot.unwrittenOneDefeated(),
                snapshot.fractureHeartDefeated());
        UUID uuid = player.getUUID();
        Integer previous = LAST_SYNC.put(uuid, fingerprint);
        if (previous != null && previous == fingerprint) {
            return;
        }
        unlockArchives(player, snapshot);
        discoverRuntimeEntries(player, snapshot);
        recordMissions(player, snapshot);
    }

    public static String structureObservation(VeilboundBridgeCatalog.Entry entry) {
        return "structure/" + VeilboundBridgeCatalog.MODID + ":" + VeilboundBridgeCatalog.entryPath(entry);
    }

    public static String scanId(String kind, String path) {
        return clean(kind) + ":" + VeilboundBridgeCatalog.MODID + ":" + clean(path);
    }

    private static void unlockArchives(ServerPlayer player, VeilboundRuntimeSnapshot snapshot) {
        if (snapshot.hasAnyProgress()) {
            unlock(player, "field_journal");
            unlock(player, "veil_lens");
            unlock(player, "the_veil");
            unlock(player, "veilbound/field_journal_summary");
            unlock(player, "veilbound/recent_observations");
        }
        if (!snapshot.activeResearch().isBlank()) {
            unlock(player, "veilbound/active_research");
            unlock(player, "veilbound/tracked_research");
        }
        if (snapshot.hasAnyFieldReading()) {
            unlock(player, "fracture_pressure");
            unlock(player, "veilbound/pressure_diagnostics");
        }
        if (hasLandmarkPath(snapshot, "fracture_rift") || snapshot.hasEvent("cleanse_fracture")
                || snapshot.hasObservation("event/cleanse_fracture")) {
            unlock(player, "veilbound/fracture_diagnostics");
        }
        if (snapshot.hasAnyStructureObservation()) {
            unlock(player, "veilbound/known_landmarks");
        }
        if (snapshot.hasAnyEntityObservation()) {
            unlock(player, "veilbound/known_entities");
        }
        if (!snapshot.endgamePath().isBlank() || snapshot.hasResearch("endgame_paths/final_choice")) {
            unlock(player, "deep_veil_gate");
            unlock(player, "veilbound/endgame_path_status");
        }
        if (snapshot.veilboundGuardianDefeated() || snapshot.unwrittenOneDefeated() || snapshot.fractureHeartDefeated()) {
            unlock(player, "fracture_heart");
            unlock(player, "veilbound/boss_gate_status");
            unlock(player, "veilbound/forbidden_studies");
            unlock(player, "forbidden_unwritten_one");
        }
        if (snapshot.fractureHeartDefeated() || snapshot.hasResearch("fractures/fracture_heart")) {
            unlock(player, "fracture_heart");
        }
        for (String research : snapshot.unlockedResearch()) {
            unlock(player, "veilbound/research/" + research);
        }
        for (VeilboundBridgeCatalog.Entry entry : VeilboundBridgeCatalog.entries(VeilboundBridgeCatalog.Kind.LANDMARK)) {
            if (hasLandmark(snapshot, entry)) {
                unlock(player, "veilbound/landmark/" + VeilboundBridgeCatalog.entryPath(entry));
            }
        }
        for (VeilboundBridgeCatalog.Entry entry : VeilboundBridgeCatalog.entries(VeilboundBridgeCatalog.Kind.BOSS_GATE)) {
            if (isBossDiscovered(snapshot, entry) || isChecked(snapshot, entry)) {
                unlock(player, "veilbound/boss-gate/" + VeilboundBridgeCatalog.entryPath(entry));
            }
        }
        for (VeilboundBridgeCatalog.Entry entry : VeilboundBridgeCatalog.entries(VeilboundBridgeCatalog.Kind.ENTITY)) {
            if (hasEntity(snapshot, entry)) {
                unlock(player, "veilbound/entity/" + VeilboundBridgeCatalog.entryPath(entry));
            }
        }
    }

    private static void discoverRuntimeEntries(ServerPlayer player, VeilboundRuntimeSnapshot snapshot) {
        if (snapshot.hasAnyProgress()) {
            EchoCoreServices.discoverFeature(player, EchoArcanaCore.id("veilbound/discovery/field_journal"));
        }
        for (VeilboundBridgeCatalog.Entry entry : VeilboundBridgeCatalog.entries(VeilboundBridgeCatalog.Kind.LANDMARK)) {
            if (hasLandmark(snapshot, entry)) {
                EchoCoreServices.discoverFeature(player, VeilboundBridgeCatalog.discoveryId(entry));
            }
        }
        for (VeilboundBridgeCatalog.Entry entry : VeilboundBridgeCatalog.entries(VeilboundBridgeCatalog.Kind.BOSS_GATE)) {
            if (isBossDiscovered(snapshot, entry) || isChecked(snapshot, entry)) {
                EchoCoreServices.discoverFeature(player, VeilboundBridgeCatalog.discoveryId(entry));
            }
        }
    }

    private static void recordMissions(ServerPlayer player, VeilboundRuntimeSnapshot snapshot) {
        if (snapshot.hasAnyProgress()) {
            record(player, "complete_first_field_scan", MissionObjectiveType.SCAN_BLOCK, "first_field_scan", snapshot);
            record(player, "record_first_observation", MissionObjectiveType.UNLOCK_RESEARCH, "fundamentals/first_contact", snapshot);
        }
        if (snapshot.hasEvent("open_field_journal")) {
            record(player, "open_field_journal", MissionObjectiveType.CUSTOM, "field_journal/open", snapshot);
        }
        if (!snapshot.unlockedResearch().isEmpty()) {
            record(player, "unlock_first_research_entry", MissionObjectiveType.UNLOCK_RESEARCH, "research/unlock_first", snapshot);
        }
        if (!snapshot.activeResearch().isBlank()) {
            record(player, "track_research_path", MissionObjectiveType.CUSTOM, "research/track_path", snapshot);
        }
        if (used(snapshot, "research_desk")) {
            record(player, "use_research_desk", MissionObjectiveType.REPAIR_MACHINE, "arcanaveil:research_desk", snapshot);
        }
        if (used(snapshot, "theory_board") || snapshot.hasEvent("theory_breakthrough")) {
            record(player, "use_theory_board", MissionObjectiveType.CUSTOM, "arcanaveil:theory_board", snapshot);
        }
        if (hasItem(snapshot, "resonance_shard")) {
            record(player, "discover_resonance_shard", MissionObjectiveType.OBTAIN_ITEM, "arcanaveil:resonance_shard", snapshot);
        }
        if (used(snapshot, "resonance_extractor")) {
            record(player, "extract_resonance", MissionObjectiveType.REPAIR_MACHINE, "arcanaveil:resonance_extractor", snapshot);
        }
        if (used(snapshot, "veil_condenser")) {
            record(player, "condense_resonance", MissionObjectiveType.REPAIR_MACHINE, "arcanaveil:veil_condenser", snapshot);
        }
        if (used(snapshot, "resonance_vessel") || snapshot.hasEvent("charge_resonance_vessel")) {
            record(player, "charge_resonance_vessel", MissionObjectiveType.REPAIR_MACHINE, "arcanaveil:resonance_vessel", snapshot);
        }
        if (used(snapshot, "ritual_basin")) {
            record(player, "build_ritual_basin_setup", MissionObjectiveType.PLACE_BLOCK, "arcanaveil:ritual_basin", snapshot);
        }
        if (snapshot.hasEvent("perform_ritual") || snapshot.hasEvent("complete_first_safe_ritual")) {
            record(player, "place_focus_pedestals", MissionObjectiveType.PLACE_BLOCK, "arcanaveil:focus_pedestal", snapshot);
            record(player, "complete_first_safe_ritual", MissionObjectiveType.CUSTOM, "ritual/first_safe", snapshot);
            record(player, "survive_or_prevent_backlash", MissionObjectiveType.SURVIVE_TIME, "ritual/backlash", snapshot);
        }
        if (snapshot.hasEvent("ritual_backlash") || snapshot.hasObservation("event/ritual_backlash")) {
            record(player, "survive_or_prevent_backlash", MissionObjectiveType.SURVIVE_TIME, "ritual/backlash", snapshot);
        }
        if (used(snapshot, "convergence_matrix")) {
            record(player, "build_convergence_matrix", MissionObjectiveType.PLACE_BLOCK, "arcanaveil:convergence_matrix", snapshot);
        }
        if (used(snapshot, "stabilizer_pillar") || snapshot.hasEvent("charge_stabilizer_pillar")) {
            record(player, "charge_stabilizer_pillar", MissionObjectiveType.REPAIR_MACHINE, "arcanaveil:stabilizer_pillar", snapshot);
        }
        if (snapshot.hasEvent("perform_convergence") || snapshot.hasEvent("complete_first_convergence")) {
            record(player, "complete_first_convergence", MissionObjectiveType.CUSTOM, "convergence/first", snapshot);
            record(player, "understand_stabilizer_budget", MissionObjectiveType.CUSTOM, "convergence/stabilizer_budget", snapshot);
        }
        if (snapshot.hasAnyFieldReading() || snapshot.hasEvent("checked_veil_pressure")) {
            record(player, "detect_fracture_pressure", MissionObjectiveType.SCAN_BLOCK, "fracture/pressure", snapshot);
        }
        if (hasLandmarkPath(snapshot, "fracture_rift")) {
            record(player, "locate_fracture_rift", MissionObjectiveType.DISCOVER_STRUCTURE, "arcanaveil:fracture_rift", snapshot);
        }
        if (snapshot.hasEvent("use_fracture_seal") || snapshot.hasEvent("cleanse_fracture")) {
            record(player, "use_fracture_seal", MissionObjectiveType.PLACE_BLOCK, "arcanaveil:fracture_seal", snapshot);
        }
        if (snapshot.hasEvent("cleanse_fracture")) {
            record(player, "cleanse_fractured_area", MissionObjectiveType.CUSTOM, "fracture/cleanse_area", snapshot);
        }
        if (used(snapshot, "construct_workbench")) {
            record(player, "build_construct_workbench", MissionObjectiveType.PLACE_BLOCK, "arcanaveil:construct_workbench", snapshot);
        }
        if (snapshot.hasEvent("create_construct_core") || hasItem(snapshot, "construct_core")
                || snapshot.hasObservation("convergence/construct_core")) {
            record(player, "create_construct_core", MissionObjectiveType.OBTAIN_ITEM, "arcanaveil:construct_core", snapshot);
        }
        if (snapshot.hasEvent("spawn_sigil_construct") || snapshot.hasResearch("constructs/sigil_constructs")
                || snapshot.hasObservation("entity/" + VeilboundBridgeCatalog.MODID + ":sigil_construct")) {
            record(player, "spawn_sigil_construct", MissionObjectiveType.CUSTOM, "arcanaveil:sigil_construct", snapshot);
        }
        if (snapshot.hasEvent("command_sigil_construct") || snapshot.hasEvent("assign_construct_order")) {
            record(player, "command_sigil_construct", MissionObjectiveType.CUSTOM, "construct/command", snapshot);
        }
        if (hasBlock(snapshot, "warding_obelisk")) {
            record(player, "discover_warding_obelisk", MissionObjectiveType.PLACE_BLOCK, "arcanaveil:warding_obelisk", snapshot);
        }
        if (hasLandmarkPath(snapshot, "deep_veil_gate")) {
            record(player, "locate_deep_veil_gate", MissionObjectiveType.DISCOVER_STRUCTURE, "arcanaveil:deep_veil_gate", snapshot);
        }
        if (snapshot.hasResearch("deep_veil/observatories") || snapshot.hasResearch("deep_veil/gatework")
                || snapshot.veilboundGuardianDefeated()) {
            record(player, "prepare_gate_requirements", MissionObjectiveType.CUSTOM, "gate/requirements", snapshot);
        }
        if (!snapshot.endgamePath().isBlank() || snapshot.hasResearch("endgame_paths/final_choice")) {
            record(player, "choose_endgame_path", MissionObjectiveType.UNLOCK_RESEARCH, "endgame_paths/final_choice", snapshot);
        }
        if (snapshot.hasResearch("fractures/fracture_heart")) {
            record(player, "begin_fracture_heart_path", MissionObjectiveType.UNLOCK_RESEARCH, "fractures/fracture_heart", snapshot);
        }
        if (snapshot.fractureHeartDefeated()) {
            record(player, "resolve_fracture_heart", MissionObjectiveType.KILL_ENTITY, "arcanaveil:fracture_heart", snapshot);
            record(player, "complete_veilbound_studies", MissionObjectiveType.CUSTOM, "veilbound/complete", snapshot);
        }
        if (snapshot.hasResearch("endgame_paths/path_modifiers")) {
            record(player, "unlock_postgame_research", MissionObjectiveType.UNLOCK_RESEARCH, "endgame_paths/path_modifiers", snapshot);
        }
    }

    private static void record(ServerPlayer player, String missionPath, MissionObjectiveType type, String targetPath,
            VeilboundRuntimeSnapshot snapshot) {
        Identifier mission = EchoArcanaCore.id("arcana_veilbound/" + missionPath);
        EchoCoreServices.recordMissionObjective(player, type,
                MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, mission, targetPath),
                1,
                Map.of(
                        "source", "arcanaveil_runtime",
                        "legacy_mission", mission.toString(),
                        "pressure", snapshot.pressureSummary()));
    }

    private static boolean hasLandmarkPath(VeilboundRuntimeSnapshot snapshot, String path) {
        return hasLandmark(snapshot, new VeilboundBridgeCatalog.Entry(
                VeilboundBridgeCatalog.Kind.LANDMARK,
                VeilboundBridgeCatalog.contentId("landmark/" + path),
                path,
                "",
                VeilboundBridgeCatalog.contentId(path),
                0));
    }

    private static boolean hasLandmark(VeilboundRuntimeSnapshot snapshot, VeilboundBridgeCatalog.Entry entry) {
        String path = VeilboundBridgeCatalog.entryPath(entry);
        String blockPath = MARKER_LANDMARKS.contains(path) ? path + "_marker" : path;
        return snapshot.hasObservation("structure/" + VeilboundBridgeCatalog.MODID + ":" + path)
                || snapshot.hasObservation("block/" + VeilboundBridgeCatalog.MODID + ":" + blockPath)
                || snapshot.hasScan(scanId("block", blockPath))
                || snapshot.hasScan(scanId("block", path));
    }

    private static boolean isBossDiscovered(VeilboundRuntimeSnapshot snapshot, VeilboundBridgeCatalog.Entry entry) {
        String path = VeilboundBridgeCatalog.entryPath(entry);
        return switch (path) {
            case "deep_veil_gate" -> hasLandmarkPath(snapshot, "deep_veil_gate")
                    || snapshot.hasResearch("deep_veil/gatework")
                    || !snapshot.endgamePath().isBlank();
            case "veilbound_guardian" -> snapshot.veilboundGuardianDefeated()
                    || snapshot.hasResearch("deep_veil/guardian_oath")
                    || snapshot.hasScan(scanId("entity", "veilbound_guardian"))
                    || snapshot.hasObservation("entity/" + VeilboundBridgeCatalog.MODID + ":veilbound_guardian");
            case "unwritten_one" -> snapshot.unwrittenOneDefeated()
                    || snapshot.hasScan(scanId("entity", "unwritten_one"))
                    || snapshot.hasObservation("entity/" + VeilboundBridgeCatalog.MODID + ":unwritten_one");
            case "fracture_heart" -> snapshot.fractureHeartDefeated()
                    || snapshot.hasResearch("fractures/fracture_heart")
                    || snapshot.hasScan(scanId("entity", "fracture_heart"))
                    || snapshot.hasObservation("entity/" + VeilboundBridgeCatalog.MODID + ":fracture_heart");
            default -> false;
        };
    }

    private static boolean hasResearch(VeilboundRuntimeSnapshot snapshot, VeilboundBridgeCatalog.Entry entry) {
        String path = VeilboundBridgeCatalog.entryPath(entry);
        return snapshot.hasResearch(path) || snapshot.hasResearch("research/" + path);
    }

    private static boolean hasEntity(VeilboundRuntimeSnapshot snapshot, VeilboundBridgeCatalog.Entry entry) {
        String path = VeilboundBridgeCatalog.entryPath(entry);
        return snapshot.hasScan(scanId("entity", path))
                || snapshot.hasObservation("entity/" + VeilboundBridgeCatalog.MODID + ":" + path);
    }

    private static boolean hasBlock(VeilboundRuntimeSnapshot snapshot, VeilboundBridgeCatalog.Entry entry) {
        String path = VeilboundBridgeCatalog.entryPath(entry);
        return snapshot.hasScan(scanId("block", path))
                || snapshot.hasObservation("block/" + VeilboundBridgeCatalog.MODID + ":" + path);
    }

    private static boolean hasItem(VeilboundRuntimeSnapshot snapshot, VeilboundBridgeCatalog.Entry entry) {
        String path = VeilboundBridgeCatalog.entryPath(entry);
        return snapshot.hasScan(scanId("item", path))
                || snapshot.hasObservation("item/" + VeilboundBridgeCatalog.MODID + ":" + path);
    }

    private static boolean used(VeilboundRuntimeSnapshot snapshot, String machinePath) {
        String path = clean(machinePath);
        return snapshot.usedMachines().contains(path)
                || snapshot.hasObservation("machine/" + path)
                || snapshot.hasObservation("machine/" + VeilboundBridgeCatalog.MODID + ":" + path);
    }

    private static boolean hasItem(VeilboundRuntimeSnapshot snapshot, String path) {
        String cleanPath = clean(path);
        return snapshot.hasScan(scanId("item", cleanPath))
                || snapshot.hasObservation("item/" + VeilboundBridgeCatalog.MODID + ":" + cleanPath);
    }

    private static boolean hasBlock(VeilboundRuntimeSnapshot snapshot, String path) {
        String cleanPath = clean(path);
        return snapshot.hasScan(scanId("block", cleanPath))
                || snapshot.hasObservation("block/" + VeilboundBridgeCatalog.MODID + ":" + cleanPath);
    }

    private static Optional<LocatedSignal> backfillLegacyStructureLocation(Player player, VeilboundRuntimeSnapshot snapshot,
            VeilboundBridgeCatalog.Entry entry, Identifier discoveryId) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(serverPlayer.level() instanceof ServerLevel serverLevel)
                || !isDiscovered(snapshot, entry)) {
            return Optional.empty();
        }
        String path = VeilboundBridgeCatalog.entryPath(entry);
        if (!STRUCTURE_LANDMARKS.contains(path)) {
            return Optional.empty();
        }
        try {
            ResourceKey<Structure> structureKey = ResourceKey.create(
                    Registries.STRUCTURE,
                    Identifier.fromNamespaceAndPath(VeilboundBridgeCatalog.MODID, path));
            Optional<Holder.Reference<Structure>> holder = serverLevel.registryAccess()
                    .lookupOrThrow(Registries.STRUCTURE)
                    .get(structureKey);
            if (holder.isEmpty()) {
                return Optional.empty();
            }
            Pair<BlockPos, Holder<Structure>> hit = serverLevel.getChunkSource()
                    .getGenerator()
                    .findNearestMapStructure(
                            serverLevel,
                            HolderSet.direct(holder.get()),
                            serverPlayer.blockPosition(),
                            LEGACY_STRUCTURE_BACKFILL_CHUNK_RADIUS,
                            false);
            if (hit == null || hit.getFirst() == null) {
                return Optional.empty();
            }
            BlockPos pos = hit.getFirst();
            persistLegacyStructureCoordinate(serverPlayer, entry, pos);
            return Optional.of(new LocatedSignal(
                    discoveryId,
                    serverLevel.dimension(),
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    serverLevel.getGameTime()));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            warnOnce("ARCANA legacy landmark coordinate backfill could not persist a generated structure location.", exception);
            return Optional.empty();
        }
    }

    private static void persistLegacyStructureCoordinate(ServerPlayer player, VeilboundBridgeCatalog.Entry entry, BlockPos pos)
            throws ReflectiveOperationException {
        Class<?> playerDataClass = Class.forName(PLAYER_DATA_CLASS);
        Object data = playerDataClass.getMethod("get", Player.class).invoke(null, player);
        ServerLevel level = (ServerLevel) player.level();
        for (String key : structureCoordinateKeys(entry)) {
            playerDataClass.getMethod("recordScanCoordinate", String.class, ServerLevel.class, BlockPos.class)
                    .invoke(data, key, level, pos);
        }
        playerDataClass.getMethod("saveAndSync", ServerPlayer.class, playerDataClass)
                .invoke(null, player, data);
    }

    private static void unlock(ServerPlayer player, String path) {
        EchoCoreServices.unlockArchive(player, Identifier.fromNamespaceAndPath("echogrimoire", "archive/" + path).toString());
    }

    private static Object invoke(Object target, String method) throws ReflectiveOperationException {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }

    private static Object invokeOptional(Object target, String method) throws ReflectiveOperationException {
        try {
            return invoke(target, method);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Object invokeQuietly(Object target, String method) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    private static Set<String> strings(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (Object next : iterable) {
            String text = clean(stringValue(next));
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return Set.copyOf(result);
    }

    private static Map<String, Integer> observations(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = clean(stringValue(entry.getKey()));
            if (!key.isBlank()) {
                result.put(key, intValue(entry.getValue()));
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, VeilboundRuntimeSnapshot.ScanCoordinate> scanCoordinates(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, VeilboundRuntimeSnapshot.ScanCoordinate> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = clean(stringValue(entry.getKey()));
            if (key.isBlank()) {
                continue;
            }
            Object coordinate = entry.getValue();
            String dimension = stringValue(invokeQuietly(coordinate, "dimension"));
            int x = intValue(invokeQuietly(coordinate, "x"));
            int y = intValue(invokeQuietly(coordinate, "y"));
            int z = intValue(invokeQuietly(coordinate, "z"));
            long gameTime = longValue(invokeQuietly(coordinate, "gameTime"));
            result.put(key, new VeilboundRuntimeSnapshot.ScanCoordinate(
                    dimensionKey(dimension),
                    x + 0.5D,
                    y,
                    z + 0.5D,
                    gameTime));
        }
        return Map.copyOf(result);
    }

    private static int intValue(Object value) {
        return intValue(value, 0);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static boolean boolValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static ResourceKey<Level> dimensionKey(String dimension) {
        try {
            String value = clean(dimension);
            return value.isBlank()
                    ? Level.OVERWORLD
                    : ResourceKey.create(Registries.DIMENSION, Identifier.parse(value));
        } catch (RuntimeException exception) {
            return Level.OVERWORLD;
        }
    }

    private static java.util.List<String> coordinateKeys(VeilboundBridgeCatalog.Entry entry) {
        String path = VeilboundBridgeCatalog.entryPath(entry);
        String blockPath = MARKER_LANDMARKS.contains(path) ? path + "_marker" : path;
        return java.util.List.of(
                structureObservation(entry),
                "block:" + VeilboundBridgeCatalog.MODID + ":" + blockPath,
                "block/" + VeilboundBridgeCatalog.MODID + ":" + blockPath,
                "block:" + VeilboundBridgeCatalog.MODID + ":" + path,
                "block/" + VeilboundBridgeCatalog.MODID + ":" + path,
                "entity:" + VeilboundBridgeCatalog.MODID + ":" + path,
                "entity/" + VeilboundBridgeCatalog.MODID + ":" + path);
    }

    private static java.util.List<String> structureCoordinateKeys(VeilboundBridgeCatalog.Entry entry) {
        String path = VeilboundBridgeCatalog.entryPath(entry);
        String blockPath = MARKER_LANDMARKS.contains(path) ? path + "_marker" : path;
        return java.util.List.of(
                structureObservation(entry),
                "block:" + VeilboundBridgeCatalog.MODID + ":" + blockPath,
                "block/" + VeilboundBridgeCatalog.MODID + ":" + blockPath,
                "block:" + VeilboundBridgeCatalog.MODID + ":" + path,
                "block/" + VeilboundBridgeCatalog.MODID + ":" + path);
    }

    private static void warnOnce(String message, Throwable throwable) {
        if (WARNED_REFLECTION.compareAndSet(false, true)) {
            EchoArcanaCore.LOGGER.warn(message, throwable);
        }
    }

    public record LocatedSignal(
            Identifier discoveryId,
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            long gameTime) {
    }
}
