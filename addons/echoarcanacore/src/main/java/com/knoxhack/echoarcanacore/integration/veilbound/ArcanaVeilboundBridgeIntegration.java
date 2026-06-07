package com.knoxhack.echoarcanacore.integration.veilbound;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiscoveryCategory;
import com.knoxhack.echocore.api.EchoDiscoveryEntry;
import com.knoxhack.echocore.api.EchoDiscoveryProvider;
import com.knoxhack.echocore.api.EchoDiscoveryState;
import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.EchoMapMarker;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echocore.api.mission.IMissionRegistry;
import com.knoxhack.echocore.api.mission.MissionChapterDefinition;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionKind;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echoarcanacore.EchoArcanaCore;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoarcanacore.api.ArcanaProviderInterfaces;
import com.knoxhack.echoarcanacore.api.RitualDefinition;
import com.knoxhack.echoarcanacore.api.RitualFamily;
import com.knoxhack.echoarcanacore.api.VeilboundRuntimeSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public final class ArcanaVeilboundBridgeIntegration {
    public static final Identifier PROVIDER_ID = EchoArcanaCore.id("veilbound_bridge");
    private static final Identifier CHAPTER = EchoArcanaCore.id("arcana_veilbound");
    private static final Identifier MAP_LAYER_LANDMARKS = EchoArcanaCore.id("holomap/veilbound_landmarks");
    private static final Identifier MAP_LAYER_FRACTURE = EchoArcanaCore.id("holomap/fracture_pressure");
    private static final Identifier MAP_LAYER_BOSS_GATES = EchoArcanaCore.id("holomap/boss_gates");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private ArcanaVeilboundBridgeIntegration() {
    }

    public static void register() {
        if (!EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID)) {
            return;
        }
        if (REGISTERED.compareAndSet(false, true)) {
            ArcanaCoreServices.registerProvider(Provider.INSTANCE);
            EchoCoreServices.registerDiscoveryProvider(DiscoveryProvider.INSTANCE);
            EchoCoreServices.registerMapDataProvider(MapProvider.INSTANCE);
            EchoCoreServices.registerMissionContent(EchoArcanaCore.MODID, ArcanaVeilboundBridgeIntegration::registerMissionContent);
            registerHookCoverage();
            EchoArcanaCore.LOGGER.info("ARCANA: Veilbound Studies bridge registered for Arcana Core.");
        }
    }

    private static void registerMissionContent(IMissionRegistry registry) {
        registry.registerChapter(EchoArcanaCore.MODID, new MissionChapterDefinition(
                CHAPTER,
                "ARCANA: Veilbound Studies",
                "Follow the first major Arcana Division campaign through observations, resonance, rituals, convergence, fracture containment, constructs, gates, and the Fracture Heart.",
                91,
                0xB78DFF));
        mission(registry, "obtain_veil_lens", "v1", "V1: First Observation", 10, "Obtain the Veil Lens",
                "Recover or craft the Veil Lens so hidden resonance can become readable.",
                MissionObjectiveType.OBTAIN_ITEM, "arcanaveil:veil_lens", Items.SPYGLASS);
        mission(registry, "complete_first_field_scan", "v1", "V1: First Observation", 11, "Complete First Field Scan",
                "Use the Veil Lens or ECHO Lens bridge to scan the first Veilbound target.",
                MissionObjectiveType.SCAN_BLOCK, "first_field_scan", Items.SPYGLASS);
        mission(registry, "record_first_observation", "v1", "V1: First Observation", 12, "Record First Observation",
                "Store the first observation in the Field Journal data path.",
                MissionObjectiveType.UNLOCK_RESEARCH, "fundamentals/first_contact", Items.WRITABLE_BOOK);
        mission(registry, "open_field_journal", "v1", "V1: First Observation", 13, "Open the Field Journal",
                "Use the Field Journal as the Veilbound-specific research UI.",
                MissionObjectiveType.CUSTOM, "field_journal/open", Items.WRITABLE_BOOK);

        mission(registry, "unlock_first_research_entry", "v2", "V2: Field Research", 20, "Unlock First Research Entry",
                "Claim a Veilbound research entry after meeting observation requirements.",
                MissionObjectiveType.UNLOCK_RESEARCH, "research/unlock_first", Items.KNOWLEDGE_BOOK);
        mission(registry, "track_research_path", "v2", "V2: Field Research", 21, "Track a Research Path",
                "Pick an active Field Journal direction so later scans have intent.",
                MissionObjectiveType.CUSTOM, "research/track_path", Items.MAP);
        mission(registry, "use_research_desk", "v2", "V2: Field Research", 22, "Use Research Desk",
                "Convert observations into usable theory work.",
                MissionObjectiveType.REPAIR_MACHINE, "arcanaveil:research_desk", Items.LECTERN);
        mission(registry, "use_theory_board", "v2", "V2: Field Research", 23, "Use Theory Board",
                "Organize theory pages without duplicating the Field Journal.",
                MissionObjectiveType.CUSTOM, "arcanaveil:theory_board", Items.PAINTING);

        mission(registry, "discover_resonance_shard", "v3", "V3: Resonance Handling", 30, "Discover Resonance Shard",
                "Find stable resonance material for the first extraction chain.",
                MissionObjectiveType.OBTAIN_ITEM, "arcanaveil:resonance_shard", Items.AMETHYST_SHARD);
        mission(registry, "extract_resonance", "v3", "V3: Resonance Handling", 31, "Extract Resonance",
                "Use the Resonance Extractor to turn hidden signals into components.",
                MissionObjectiveType.REPAIR_MACHINE, "arcanaveil:resonance_extractor", Items.AMETHYST_SHARD);
        mission(registry, "condense_resonance", "v3", "V3: Resonance Handling", 32, "Condense Resonance",
                "Use the Veil Condenser to create safer storage media.",
                MissionObjectiveType.REPAIR_MACHINE, "arcanaveil:veil_condenser", Items.GLASS_BOTTLE);
        mission(registry, "charge_resonance_vessel", "v3", "V3: Resonance Handling", 33, "Charge Resonance Vessel",
                "Hold a reserve of stable resonance for convergence budgets.",
                MissionObjectiveType.REPAIR_MACHINE, "arcanaveil:resonance_vessel", Items.AMETHYST_BLOCK);

        mission(registry, "build_ritual_basin_setup", "v4", "V4: Ritual Practice", 40, "Build Ritual Basin Setup",
                "Place the basin and prepare safe ritual geometry.",
                MissionObjectiveType.PLACE_BLOCK, "arcanaveil:ritual_basin", Items.CRYING_OBSIDIAN);
        mission(registry, "place_focus_pedestals", "v4", "V4: Ritual Practice", 41, "Place Focus Pedestals",
                "Place pedestal inputs around the ritual basin.",
                MissionObjectiveType.PLACE_BLOCK, "arcanaveil:focus_pedestal", Items.QUARTZ_PILLAR);
        mission(registry, "complete_first_safe_ritual", "v4", "V4: Ritual Practice", 42, "Complete First Safe Ritual",
                "Complete a low-risk Veilbound ritual without item loss.",
                MissionObjectiveType.CUSTOM, "ritual/first_safe", Items.CRYING_OBSIDIAN);
        mission(registry, "survive_or_prevent_backlash", "v4", "V4: Ritual Practice", 43, "Survive or Prevent Backlash",
                "Learn why instability is engineering data, not flavor.",
                MissionObjectiveType.SURVIVE_TIME, "ritual/backlash", Items.SHIELD);

        mission(registry, "build_convergence_matrix", "v5", "V5: Convergence Theory", 50, "Build Convergence Matrix",
                "Construct the matrix that aligns pressure, vessels, stabilizers, and inputs.",
                MissionObjectiveType.PLACE_BLOCK, "arcanaveil:convergence_matrix", Items.RESPAWN_ANCHOR);
        mission(registry, "charge_stabilizer_pillar", "v5", "V5: Convergence Theory", 51, "Charge Stabilizer Pillar",
                "Budget convergence instability with stabilizer geometry.",
                MissionObjectiveType.REPAIR_MACHINE, "arcanaveil:stabilizer_pillar", Items.LIGHTNING_ROD);
        mission(registry, "complete_first_convergence", "v5", "V5: Convergence Theory", 52, "Complete First Convergence",
                "Finish a convergence recipe and review the Arcane Index page.",
                MissionObjectiveType.CUSTOM, "convergence/first", Items.AMETHYST_CLUSTER);
        mission(registry, "understand_stabilizer_budget", "v5", "V5: Convergence Theory", 53, "Understand Stabilizer Budget",
                "Use Index or Grimoire context to avoid hidden impossible progression.",
                MissionObjectiveType.CUSTOM, "convergence/stabilizer_budget", Items.COMPARATOR);

        mission(registry, "detect_fracture_pressure", "v6", "V6: Fracture Containment", 60, "Detect Fracture Pressure",
                "Use monitors, detectors, or scans to read fracture pressure.",
                MissionObjectiveType.SCAN_BLOCK, "fracture/pressure", Items.RECOVERY_COMPASS);
        mission(registry, "locate_fracture_rift", "v6", "V6: Fracture Containment", 61, "Locate Fracture Rift",
                "Find a rift and mark it in the HoloMap bridge.",
                MissionObjectiveType.DISCOVER_STRUCTURE, "arcanaveil:fracture_rift", Items.RESPAWN_ANCHOR);
        mission(registry, "use_fracture_seal", "v6", "V6: Fracture Containment", 62, "Use Fracture Seal",
                "Deploy a Fracture Seal to suppress local rupture behavior.",
                MissionObjectiveType.PLACE_BLOCK, "arcanaveil:fracture_seal", Items.END_CRYSTAL);
        mission(registry, "cleanse_fractured_area", "v6", "V6: Fracture Containment", 63, "Cleanse Fractured Area",
                "Reduce or stabilize a fracture-contaminated area.",
                MissionObjectiveType.CUSTOM, "fracture/cleanse_area", Items.BEACON);

        mission(registry, "build_construct_workbench", "v7", "V7: Construct Awakening", 70, "Build Construct Workbench",
                "Prepare deterministic construct assembly.",
                MissionObjectiveType.PLACE_BLOCK, "arcanaveil:construct_workbench", Items.CRAFTING_TABLE);
        mission(registry, "create_construct_core", "v7", "V7: Construct Awakening", 71, "Create Construct Core",
                "Converge or craft a core without duplicating held items.",
                MissionObjectiveType.OBTAIN_ITEM, "arcanaveil:construct_core", Items.HEART_OF_THE_SEA);
        mission(registry, "spawn_sigil_construct", "v7", "V7: Construct Awakening", 72, "Spawn Sigil Construct",
                "Awaken a helper that remains a Veilbound identity, not a generic familiar.",
                MissionObjectiveType.CUSTOM, "arcanaveil:sigil_construct", Items.ARMOR_STAND);
        mission(registry, "command_sigil_construct", "v7", "V7: Construct Awakening", 73, "Command Sigil Construct",
                "Use construct behavior safely for maintenance or support.",
                MissionObjectiveType.CUSTOM, "construct/command", Items.NAME_TAG);

        mission(registry, "discover_warding_obelisk", "v8", "V8: Deep Veil Gate", 80, "Discover Warding Obelisk",
                "Learn persistent ward anchors before gatework.",
                MissionObjectiveType.PLACE_BLOCK, "arcanaveil:warding_obelisk", Items.OBSIDIAN);
        mission(registry, "locate_deep_veil_gate", "v8", "V8: Deep Veil Gate", 81, "Locate Deep Veil Gate",
                "Find or build the gate and mark it in the HoloMap bridge.",
                MissionObjectiveType.DISCOVER_STRUCTURE, "arcanaveil:deep_veil_gate", Items.END_PORTAL_FRAME);
        mission(registry, "prepare_gate_requirements", "v8", "V8: Deep Veil Gate", 82, "Prepare Gate Requirements",
                "Collect alloy, memory, warding, and research requirements.",
                MissionObjectiveType.CUSTOM, "gate/requirements", Items.ENDER_EYE);
        mission(registry, "choose_endgame_path", "v8", "V8: Deep Veil Gate", 83, "Choose Endgame Path",
                "Seal, harmonize, exploit, or architect the Veil.",
                MissionObjectiveType.UNLOCK_RESEARCH, "endgame_paths/final_choice", Items.COMPASS);

        mission(registry, "begin_fracture_heart_path", "v9", "V9: Fracture Heart", 90, "Begin Fracture Heart Path",
                "Commit to the final fracture route through Field Journal progression.",
                MissionObjectiveType.UNLOCK_RESEARCH, "fractures/fracture_heart", Items.NETHER_STAR);
        mission(registry, "resolve_fracture_heart", "v9", "V9: Fracture Heart", 91, "Defeat or Resolve Fracture Heart",
                "Finish the deepest fracture encounter without losing the addon identity.",
                MissionObjectiveType.KILL_ENTITY, "arcanaveil:fracture_heart", Items.NETHER_STAR);
        mission(registry, "complete_veilbound_studies", "v9", "V9: Fracture Heart", 92, "Complete Veilbound Studies",
                "Record the campaign route as complete in the Arcana Division.",
                MissionObjectiveType.CUSTOM, "veilbound/complete", Items.ENCHANTED_BOOK);
        mission(registry, "unlock_postgame_research", "v9", "V9: Fracture Heart", 93, "Unlock Postgame Research",
                "Open late Veil path modifiers after the final route.",
                MissionObjectiveType.UNLOCK_RESEARCH, "endgame_paths/path_modifiers", Items.KNOWLEDGE_BOOK);
    }

    private static void mission(IMissionRegistry registry, String path, String phaseId, String phaseTitle, int order,
            String title, String briefing, MissionObjectiveType type, String targetPath, net.minecraft.world.item.Item iconItem) {
        Identifier mission = EchoArcanaCore.id("arcana_veilbound/" + path);
        Identifier target = MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, mission, targetPath);
        registry.registerMission(EchoArcanaCore.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase(phaseId, phaseTitle, order, order)
                .text(title, briefing, "Official route context lives in Arcane Index, Grimoire, Lens, HoloMap, and the ARCANA Field Journal.")
                .category("ARCANA: Veilbound Studies", phaseTitle)
                .icon(new ItemStack(iconItem))
                .kind(MissionKind.MAIN)
                .metadata("terminal_route_role", "OPTIONAL")
                .metadata("terminal_route_visible", "false")
                .objective(new ObjectiveDefinition(
                        EchoArcanaCore.id("arcana_veilbound/" + path + "/objective"),
                        type,
                        title,
                        "",
                        new ItemStack(iconItem),
                        1,
                        false,
                        Map.of("target", target.toString())))
                .build());
    }

    private static void registerHookCoverage() {
        for (String mission : List.of("complete_first_field_scan", "record_first_observation", "locate_fracture_rift",
                "locate_deep_veil_gate", "resolve_fracture_heart")) {
            Identifier missionId = EchoArcanaCore.id("arcana_veilbound/" + mission);
            EchoCoreServices.registerMissionHookCoverage(EchoArcanaCore.MODID, missionId,
                    MissionHookTargets.objectiveTarget(EchoArcanaCore.MODID, missionId, mission));
        }
    }

    public enum Provider implements ArcanaProviderInterfaces.VeilboundBridgeProvider,
            ArcanaProviderInterfaces.TerminalArcanaProvider,
            ArcanaProviderInterfaces.ArcaneIndexProvider,
            ArcanaProviderInterfaces.ArcaneLensProvider,
            ArcanaProviderInterfaces.ArcaneHoloMapProvider,
            ArcanaProviderInterfaces.ArcaneMissionProvider,
            ArcanaProviderInterfaces.GrimoireEntryProvider,
            ArcanaProviderInterfaces.VeilboundRuntimeProvider,
            ArcanaProviderInterfaces.AetherMachineProvider,
            ArcanaProviderInterfaces.RitualProvider {
        INSTANCE;

        @Override
        public Identifier id() {
            return PROVIDER_ID;
        }

        @Override
        public Map<String, String> terminalSummary(Player player) {
            VeilboundRuntimeSnapshot snapshot = snapshot(player);
            Map<String, String> summary = new LinkedHashMap<>();
            summary.put("status", EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID) ? "arcanaveil present" : "arcanaveil absent");
            summary.put("research", Integer.toString(researchIds().size()));
            summary.put("resonance", Integer.toString(resonanceCategories().size()));
            summary.put("rituals", Integer.toString(ritualIds().size()));
            summary.put("landmarks", Integer.toString(landmarkIds(player).size()));
            if (snapshot.available()) {
                summary.put("field_scans", Integer.toString(snapshot.scanCount()));
                summary.put("unlocked_research", Integer.toString(snapshot.unlockedResearch().size()));
                summary.put("active_research", snapshot.activeResearch().isBlank() ? "none" : snapshot.activeResearch());
                summary.put("pressure", snapshot.pressureSummary());
                summary.put("endgame_path", snapshot.endgamePath().isBlank() ? "none" : snapshot.endgamePath());
                summary.put("boss_flags", bossSummary(snapshot));
            } else if (EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID)) {
                summary.put("runtime", "waiting for ARCANA player data");
            }
            return Map.copyOf(summary);
        }

        @Override
        public List<Identifier> pageIds(Player player) {
            return VeilboundBridgeCatalog.allEntries().stream()
                    .map(entry -> Identifier.fromNamespaceAndPath("echoarcaneindex", VeilboundBridgeCatalog.indexPagePath(entry)))
                    .toList();
        }

        @Override
        public List<String> scanHints(Player player, Identifier targetId) {
            return VeilboundBridgeCatalog.target(targetId)
                    .map(entry -> List.of(
                            "ARCANA target: " + entry.title(),
                            "Field Journal observation: " + entry.id(),
                            "Primary resonance: " + VeilboundBridgeCatalog.primaryResonance(targetId)))
                    .orElse(List.of());
        }

        @Override
        public List<Identifier> markerIds(Player player) {
            List<Identifier> markers = new ArrayList<>();
            landmarkIds(player).forEach(markers::add);
            bossGateIds(player).forEach(markers::add);
            return List.copyOf(markers);
        }

        @Override
        public List<Identifier> missionIds(Player player) {
            return List.of(CHAPTER);
        }

        @Override
        public List<Identifier> grimoireEntryIds(Player player) {
            return VeilboundBridgeCatalog.allEntries().stream()
                    .filter(entry -> entry.kind() == VeilboundBridgeCatalog.Kind.RESEARCH
                            || entry.kind() == VeilboundBridgeCatalog.Kind.LANDMARK
                            || entry.kind() == VeilboundBridgeCatalog.Kind.BOSS_GATE)
                    .map(entry -> Identifier.fromNamespaceAndPath("echogrimoire", grimoirePath(entry)))
                    .toList();
        }

        @Override
        public List<Identifier> blockIds() {
            return ids(VeilboundBridgeCatalog.Kind.BLOCK);
        }

        @Override
        public List<Identifier> itemIds() {
            return ids(VeilboundBridgeCatalog.Kind.ITEM);
        }

        @Override
        public List<Identifier> entityIds() {
            return ids(VeilboundBridgeCatalog.Kind.ENTITY);
        }

        @Override
        public List<Identifier> particleIds() {
            return ids(VeilboundBridgeCatalog.Kind.PARTICLE);
        }

        @Override
        public List<Identifier> researchIds() {
            return ids(VeilboundBridgeCatalog.Kind.RESEARCH);
        }

        @Override
        public List<Identifier> resonanceCategories() {
            return ids(VeilboundBridgeCatalog.Kind.RESONANCE_CATEGORY);
        }

        @Override
        public List<Identifier> resonanceAssignments() {
            return ids(VeilboundBridgeCatalog.Kind.RESONANCE_ASSIGNMENT);
        }

        @Override
        public List<Identifier> ritualIds() {
            return ids(VeilboundBridgeCatalog.Kind.RITUAL);
        }

        @Override
        public List<Identifier> convergenceIds() {
            return ids(VeilboundBridgeCatalog.Kind.CONVERGENCE);
        }

        @Override
        public List<Identifier> machineRecipeIds() {
            return ids(VeilboundBridgeCatalog.Kind.MACHINE_RECIPE);
        }

        @Override
        public List<Identifier> fractureTransformIds() {
            return ids(VeilboundBridgeCatalog.Kind.FRACTURE_TRANSFORM);
        }

        @Override
        public List<Identifier> pressureSources(Player player) {
            return List.of(
                    VeilboundBridgeCatalog.contentId("veil_monitor"),
                    VeilboundBridgeCatalog.contentId("fracture_detector"),
                    VeilboundBridgeCatalog.contentId("fracture_rift"),
                    VeilboundBridgeCatalog.contentId("deep_veil_gate"));
        }

        @Override
        public List<Identifier> landmarkIds(Player player) {
            return ids(VeilboundBridgeCatalog.Kind.LANDMARK);
        }

        @Override
        public List<Identifier> bossGateIds(Player player) {
            return ids(VeilboundBridgeCatalog.Kind.BOSS_GATE);
        }

        @Override
        public List<Identifier> machineIds() {
            return List.of(
                    VeilboundBridgeCatalog.contentId("research_desk"),
                    VeilboundBridgeCatalog.contentId("theory_board"),
                    VeilboundBridgeCatalog.contentId("resonance_extractor"),
                    VeilboundBridgeCatalog.contentId("veil_condenser"),
                    VeilboundBridgeCatalog.contentId("pattern_etcher"),
                    VeilboundBridgeCatalog.contentId("arcane_loom"),
                    VeilboundBridgeCatalog.contentId("thought_vessel"),
                    VeilboundBridgeCatalog.contentId("convergence_matrix"),
                    VeilboundBridgeCatalog.contentId("resonance_vessel"),
                    VeilboundBridgeCatalog.contentId("veil_monitor"),
                    VeilboundBridgeCatalog.contentId("fracture_detector"),
                    VeilboundBridgeCatalog.contentId("construct_workbench"));
        }

        @Override
        public List<RitualDefinition> rituals() {
            return VeilboundBridgeCatalog.entries(VeilboundBridgeCatalog.Kind.RITUAL).stream()
                    .map(entry -> new RitualDefinition(
                            entry.id(),
                            "ritual.arcanaveil." + entry.id().getPath().replace('/', '.'),
                            RitualFamily.VEILBOUND,
                            "ritual_basin",
                            VeilboundBridgeCatalog.contentId("structure/ritual_basin"),
                            VeilboundBridgeCatalog.contentId("ritual_basin"),
                            List.of(),
                            List.of(),
                            List.of(),
                            0.0D,
                            List.of(),
                            List.of("veilbound:data_driven"),
                            VeilboundBridgeCatalog.contentId("research/rituals/ritual_foundations"),
                            List.of(VeilboundBridgeCatalog.contentId("focus_pedestal")),
                            List.of("veil_pressure", "fracture_pressure"),
                            8.0D,
                            VeilboundBridgeCatalog.contentId("failure/" + entry.id().getPath()),
                            VeilboundBridgeCatalog.contentId("effect/" + entry.id().getPath()),
                            0.0D,
                            0.0D,
                            0.02D,
                            Identifier.fromNamespaceAndPath("echoarcaneindex", VeilboundBridgeCatalog.indexPagePath(entry)),
                            Identifier.fromNamespaceAndPath("echogrimoire", grimoirePath(entry))))
                    .toList();
        }

        @Override
        public VeilboundRuntimeSnapshot snapshot(Player player) {
            return VeilboundRuntimeBridge.snapshot(player);
        }

        private static List<Identifier> ids(VeilboundBridgeCatalog.Kind kind) {
            return VeilboundBridgeCatalog.entries(kind).stream().map(VeilboundBridgeCatalog.Entry::id).toList();
        }

        private static String grimoirePath(VeilboundBridgeCatalog.Entry entry) {
            return "archive/veilbound/" + VeilboundBridgeCatalog.kindPath(entry.kind()) + "/" + VeilboundBridgeCatalog.entryPath(entry);
        }

        private static String bossSummary(VeilboundRuntimeSnapshot snapshot) {
            int defeated = 0;
            defeated += snapshot.veilboundGuardianDefeated() ? 1 : 0;
            defeated += snapshot.unwrittenOneDefeated() ? 1 : 0;
            defeated += snapshot.fractureHeartDefeated() ? 1 : 0;
            return defeated + "/3 defeated";
        }
    }

    public enum DiscoveryProvider implements EchoDiscoveryProvider {
        INSTANCE;

        @Override
        public List<EchoDiscoveryEntry> entries(Player player) {
            List<EchoDiscoveryEntry> entries = new ArrayList<>();
            for (VeilboundBridgeCatalog.Entry entry : VeilboundBridgeCatalog.entries(VeilboundBridgeCatalog.Kind.LANDMARK)) {
                entries.add(discovery(entry, EchoDiscoveryCategory.STRUCTURE, "Unknown Veilbound Landmark"));
            }
            for (VeilboundBridgeCatalog.Entry entry : VeilboundBridgeCatalog.entries(VeilboundBridgeCatalog.Kind.BOSS_GATE)) {
                entries.add(discovery(entry, EchoDiscoveryCategory.GUARDIAN, "Sealed Veilbound Gate"));
            }
            entries.add(new EchoDiscoveryEntry(
                    EchoArcanaCore.id("veilbound/discovery/field_journal"),
                    CHAPTER,
                    EchoDiscoveryCategory.EVENT,
                    "Field Journal",
                    "Unknown ARCANA Journal",
                    "Open the Field Journal after the first Veil Lens scan.",
                    "Veilbound research stays in ARCANA and is mirrored into Arcana Division surfaces.",
                    VeilboundBridgeCatalog.contentId("field_journal"),
                    null,
                    0xB78DFF,
                    EchoArcanaCore.id("arcana_veilbound/open_field_journal"),
                    5));
            return List.copyOf(entries);
        }

        @Override
        public EchoDiscoveryState state(Player player, EchoDiscoveryEntry entry) {
            if (!EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID)) {
                return EchoDiscoveryState.LOCKED;
            }
            VeilboundRuntimeSnapshot snapshot = VeilboundRuntimeBridge.snapshot(player);
            if (player instanceof ServerPlayer serverPlayer) {
                VeilboundRuntimeBridge.syncServerProgress(serverPlayer, snapshot);
            }
            if (entry != null && entry.id().getPath().endsWith("field_journal")) {
                return snapshot.hasAnyProgress() ? EchoDiscoveryState.DISCOVERED : EchoDiscoveryState.LOCKED;
            }
            Optional<VeilboundBridgeCatalog.Entry> catalogEntry = catalogEntry(entry);
            if (catalogEntry.isPresent()) {
                VeilboundBridgeCatalog.Entry catalog = catalogEntry.get();
                if (VeilboundRuntimeBridge.isChecked(snapshot, catalog)) {
                    return EchoDiscoveryState.CHECKED;
                }
                if (VeilboundRuntimeBridge.isDiscovered(snapshot, catalog)) {
                    return EchoDiscoveryState.DISCOVERED;
                }
            }
            return EchoDiscoveryState.LOCKED;
        }

        private static Optional<VeilboundBridgeCatalog.Entry> catalogEntry(EchoDiscoveryEntry entry) {
            if (entry == null) {
                return Optional.empty();
            }
            return VeilboundBridgeCatalog.allEntries().stream()
                    .filter(candidate -> candidate.kind() == VeilboundBridgeCatalog.Kind.LANDMARK
                            || candidate.kind() == VeilboundBridgeCatalog.Kind.BOSS_GATE)
                    .filter(candidate -> VeilboundBridgeCatalog.discoveryId(candidate).equals(entry.id()))
                    .findFirst();
        }

        private static EchoDiscoveryEntry discovery(VeilboundBridgeCatalog.Entry entry, EchoDiscoveryCategory category,
                String lockedTitle) {
            return new EchoDiscoveryEntry(
                    VeilboundBridgeCatalog.discoveryId(entry),
                    CHAPTER,
                    category,
                    entry.title(),
                    lockedTitle,
                    "Scan the structure, gate, or entity with the Veil Lens or ECHO Lens bridge.",
                    entry.summary(),
                    entry.icon(),
                    null,
                    entry.kind() == VeilboundBridgeCatalog.Kind.BOSS_GATE ? 0xC94CFF : 0x7DE6D1,
                    CHAPTER,
                    entry.sortOrder());
        }
    }

    public enum MapProvider implements IMapDataProvider {
        INSTANCE;

        @Override
        public Identifier providerId() {
            return EchoArcanaCore.id("veilbound_holomap_bridge");
        }

        @Override
        public List<IMapLayer> layers(Player player) {
            return List.of(
                    new EchoMapLayer(MAP_LAYER_LANDMARKS, "Veilbound Landmarks", 86, 0xFF7DE6D1, true),
                    new EchoMapLayer(MAP_LAYER_FRACTURE, "Fracture Pressure", 87, 0xFFC94CFF, true),
                    new EchoMapLayer(MAP_LAYER_BOSS_GATES, "Boss Gates", 88, 0xFFFF5C7A, true));
        }

        @Override
        public List<IMapMarker> markers(Player player) {
            if (player == null || !EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID)) {
                return List.of();
            }
            VeilboundRuntimeSnapshot snapshot = VeilboundRuntimeBridge.snapshot(player);
            if (player instanceof ServerPlayer serverPlayer) {
                VeilboundRuntimeBridge.syncServerProgress(serverPlayer, snapshot);
            }
            List<IMapMarker> markers = new ArrayList<>();
            int order = 0;
            for (VeilboundBridgeCatalog.Entry entry : VeilboundBridgeCatalog.entries(VeilboundBridgeCatalog.Kind.LANDMARK)) {
                markers.add(marker(player, snapshot, entry, MAP_LAYER_LANDMARKS, IMapMarker.MarkerKind.REGION, order++));
            }
            for (VeilboundBridgeCatalog.Entry entry : VeilboundBridgeCatalog.entries(VeilboundBridgeCatalog.Kind.BOSS_GATE)) {
                markers.add(marker(player, snapshot, entry, MAP_LAYER_BOSS_GATES, IMapMarker.MarkerKind.MISSION, order++));
            }
            BlockPos pos = player.blockPosition();
            boolean hasPressure = snapshot.hasAnyFieldReading();
            String pressureSummary = snapshot.available()
                    ? "Live ARCANA field reading: " + snapshot.pressureSummary()
                    : "ARCANA player data is not available on this side yet.";
            markers.add(new EchoMapMarker(
                    EchoArcanaCore.id("veilbound/pressure/local_field"),
                    MAP_LAYER_FRACTURE,
                    PROVIDER_ID,
                    IMapMarker.MarkerKind.HAZARD,
                    hasPressure ? IMapMarker.MarkerState.DISCOVERED : IMapMarker.MarkerState.LOCKED,
                    "Local Veil Field",
                    pressureSummary,
                    snapshot.dimension(),
                    snapshot.available() ? snapshot.x() : pos.getX() + 0.5D,
                    snapshot.available() ? snapshot.y() : pos.getY(),
                    snapshot.available() ? snapshot.z() : pos.getZ() + 0.5D,
                    hasPressure ? 32.0F : 96.0F,
                    VeilboundBridgeCatalog.contentId("fracture_detector"),
                    null,
                    order,
                    hasPressure));
            return List.copyOf(markers);
        }

        @Override
        public boolean refresh(ServerPlayer player, String reason) {
            if (player == null || !EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID)) {
                return false;
            }
            VeilboundRuntimeBridge.syncServerProgress(player, VeilboundRuntimeBridge.snapshot(player));
            return true;
        }

        private static EchoMapMarker marker(Player player, VeilboundRuntimeSnapshot snapshot, VeilboundBridgeCatalog.Entry entry, Identifier layer,
                IMapMarker.MarkerKind kind, int order) {
            Identifier discoveryId = VeilboundBridgeCatalog.discoveryId(entry);
            boolean checked = VeilboundRuntimeBridge.isChecked(snapshot, entry);
            boolean discovered = checked
                    || VeilboundRuntimeBridge.isDiscovered(snapshot, entry)
                    || EchoCoreServices.hasDiscoveredFeature(player, discoveryId);
            Optional<VeilboundRuntimeBridge.LocatedSignal> location = VeilboundRuntimeBridge.location(player, snapshot, entry);
            boolean precise = location.isPresent();
            double x = precise ? location.get().x() : virtualCoordinate(entry.id(), 47);
            double y = precise ? location.get().y() : 64.0D;
            double z = precise ? location.get().z() : virtualCoordinate(entry.id(), 83);
            String coordinateSummary = precise
                    ? "Exact Lens, Veil Lens, or legacy generated-structure coordinate is recorded."
                    : discovered
                            ? "Discovery is known from ARCANA saved data, but no persisted coordinate has been exposed for this marker yet."
                            : "Scan with the Veil Lens or ECHO Lens bridge to reveal this marker.";
            return new EchoMapMarker(
                    EchoArcanaCore.id("veilbound/marker/" + VeilboundBridgeCatalog.entryPath(entry)),
                    layer,
                    entry.id(),
                    kind,
                    checked ? IMapMarker.MarkerState.CHECKED : discovered ? IMapMarker.MarkerState.DISCOVERED : IMapMarker.MarkerState.LOCKED,
                    entry.title(),
                    entry.summary() + " / " + coordinateSummary,
                    precise ? location.get().dimension() : player.level() == null ? Level.OVERWORLD : player.level().dimension(),
                    x,
                    y,
                    z,
                    precise ? 24.0F : discovered ? 128.0F : 256.0F,
                    entry.icon(),
                    discoveryId,
                    order,
                    precise);
        }

        private static double virtualCoordinate(Identifier id, int salt) {
            int hash = java.util.Objects.hash(id == null ? "unknown" : id.toString(), salt);
            return Math.floorMod(hash, 2400) - 1200;
        }
    }
}
