package com.knoxhack.echomissioncore.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionProgressView;
import com.echoplatform.echocore.api.mission.IObjectiveView;
import com.echoplatform.echocore.api.mission.IRewardView;
import com.echoplatform.echocore.api.mission.MissionActionView;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.MissionStatus;
import com.knoxhack.echomissioncore.EchoMissionCore;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelUnlock;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.api.mission.TerminalMissionVisuals;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class MissionCoreTerminalProvider implements TerminalMissionProvider {
    public static final MissionCoreTerminalProvider INSTANCE = new MissionCoreTerminalProvider();
    public static final Identifier CHAPTER_ID = Identifier.fromNamespaceAndPath(EchoMissionCore.MODID, "missions");

    private MissionCoreTerminalProvider() {
    }

    @Override
    public TerminalMissionChapter chapter() {
        return new TerminalMissionChapter(
                CHAPTER_ID,
                "MissionCore",
                "Shared mission feed from the ECHO backend service.",
                42,
                0x55FFDD,
                true);
    }

    @Override
    public List<TerminalMissionDefinition> missions(Player player) {
        return EchoCoreServices.missionService().missions(player).stream()
                .map(MissionCoreTerminalProvider::definition)
                .toList();
    }

    @Override
    public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
        return EchoCoreServices.missionService().mission(player, missionId)
                .map(MissionCoreTerminalProvider::snapshot)
                .orElseGet(() -> new TerminalMissionSnapshot(
                        missionId,
                        TerminalMissionStatus.LOCKED,
                        0.0F,
                        "Missing",
                        "Mission record not found.",
                        "Reload the terminal after content registration finishes.",
                        List.of()));
    }

    @Override
    public TerminalMissionPresentation presentation(
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        return new TerminalMissionPresentation(
                definition.title(),
                definition.briefing(),
                snapshot.actionHint(),
                definition.phaseTitle(),
                switch (snapshot.status()) {
                    case CLAIMABLE, COMPLETED, CLAIMED -> "success";
                    case UNLOCKED -> "active";
                    case LOCKED, VIEW_ONLY -> "muted";
                },
                List.of(definition.category(), definition.difficulty()),
                "");
    }

    @Override
    public TerminalMissionVisuals visuals(
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot) {
        return TerminalMissionVisuals.fallback(definition, snapshot);
    }

    @Override
    public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
        if (definition == null) {
            return TerminalMissionRole.fallback(null, snapshot);
        }
        return EchoCoreServices.missionService().mission(player, definition.id())
                .map(view -> view.status() == MissionStatus.VIEW_ONLY
                        ? TerminalMissionRole.REFERENCE
                        : resolvedRouteRole(definition, snapshot, null, view.definition()))
                .orElseGet(() -> TerminalMissionRole.fallback(definition, snapshot));
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
        Optional<MissionDefinition> imported = importedMission(definition.id());
        TerminalMissionRole safeRole = resolvedRouteRole(definition, snapshot, role, imported.orElse(null));
        return Optional.of(new TerminalMissionRoutePlacement(
                routePhase(definition, safeRole, imported.orElse(null)),
                routeOrder(definition, imported.orElse(null)),
                safeRole,
                routeVisible(imported.orElse(null), true)));
    }

    @Override
    public List<Identifier> routePrerequisites(
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        if (definition == null) {
            return List.of();
        }
        return importedMission(definition.id())
                .map(value -> routePrerequisites(value.metadata()))
                .orElseGet(List::of);
    }

    @Override
    public Optional<Identifier> routeAnchor(
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        if (definition == null) {
            return Optional.empty();
        }
        return importedMission(definition.id())
                .flatMap(value -> identifier(value.metadata().get("terminal_route_anchor"),
                        definition.id().getNamespace()));
    }

    @Override
    public List<TerminalMissionIntelUnlock> intelUnlocks(
            Player player,
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role) {
        if (definition == null) {
            return List.of();
        }
        return importedMission(definition.id())
                .map(value -> intelUnlocks(value.metadata(), definition.id().getNamespace()))
                .orElseGet(List::of);
    }

    @Override
    public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
        return EchoCoreServices.handleMissionAction(player, missionId, actionId);
    }

    private static TerminalMissionDefinition definition(IMissionProgressView view) {
        MissionDefinition definition = view.definition();
        int phaseOrder = intMetadata(definition, "terminal_route_phase", definition.phaseOrder());
        int missionOrder = intMetadata(definition, "terminal_route_order", definition.missionOrder());
        return new TerminalMissionDefinition(
                definition.id(),
                CHAPTER_ID,
                definition.phaseId(),
                phaseTitle(definition),
                phaseOrder,
                missionOrder,
                definition.title(),
                definition.briefing(),
                definition.fieldGuide(),
                definition.category(),
                definition.difficulty(),
                definition.icon(),
                definition.prerequisites().stream().map(Identifier::toString).toList(),
                view.objectives().stream().map(MissionCoreTerminalProvider::requirement).toList(),
                view.rewards().stream().map(MissionCoreTerminalProvider::reward).toList());
    }

    private static TerminalMissionSnapshot snapshot(IMissionProgressView view) {
        return new TerminalMissionSnapshot(
                view.id(),
                status(view.status()),
                view.progress(),
                view.statusLabel(),
                view.unlockReason(),
                view.actionHint(),
                view.actions().stream().map(MissionCoreTerminalProvider::action).toList());
    }

    private static TerminalMissionRequirement requirement(IObjectiveView objective) {
        TerminalMissionRequirement.Kind kind = requirementKind(objective.type());
        return new TerminalMissionRequirement(
                kind,
                objective.label(),
                objective.detail(),
                requirementIcon(objective, kind),
                objective.progress(),
                objective.required(),
                objective.complete());
    }

    private static TerminalMissionRequirement.Kind requirementKind(MissionObjectiveType type) {
        return switch (type == null ? MissionObjectiveType.CUSTOM : type) {
            case OBTAIN_ITEM, CRAFT_ITEM, DELIVER_ITEM -> TerminalMissionRequirement.Kind.ITEM;
            case PLACE_BLOCK, SCAN_BLOCK, REPAIR_MACHINE, BUILD_MULTIBLOCK -> TerminalMissionRequirement.Kind.BLOCK;
            case KILL_ENTITY, SCAN_ENTITY -> TerminalMissionRequirement.Kind.ENTITY_KILL;
            case DISCOVER_STRUCTURE, ENTER_REGION -> TerminalMissionRequirement.Kind.LOCATION;
            default -> TerminalMissionRequirement.Kind.CUSTOM;
        };
    }

    private static ItemStack requirementIcon(IObjectiveView objective, TerminalMissionRequirement.Kind kind) {
        ItemStack targetIcon = targetIcon(objective.type(), objective.criteria(), objective.id().getNamespace());
        if (!targetIcon.isEmpty()) {
            return targetIcon;
        }
        if (kind == TerminalMissionRequirement.Kind.ENTITY_KILL
                || kind == TerminalMissionRequirement.Kind.LOCATION) {
            return ItemStack.EMPTY;
        }
        ItemStack icon = objective.icon();
        if (icon == null || icon.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return icon.copy();
    }

    private static ItemStack targetIcon(
            MissionObjectiveType type,
            Map<String, String> criteria,
            String fallbackNamespace) {
        String target = criteria == null ? "" : criteria.getOrDefault("target", "").strip();
        if (target.isBlank()) {
            return ItemStack.EMPTY;
        }
        return switch (type == null ? MissionObjectiveType.CUSTOM : type) {
            case OBTAIN_ITEM, CRAFT_ITEM, DELIVER_ITEM -> itemIcon(target, fallbackNamespace);
            case PLACE_BLOCK, SCAN_BLOCK, REPAIR_MACHINE, BUILD_MULTIBLOCK -> {
                ItemStack blockIcon = blockIcon(target, fallbackNamespace);
                yield blockIcon.isEmpty() ? itemIcon(target, fallbackNamespace) : blockIcon;
            }
            default -> ItemStack.EMPTY;
        };
    }

    private static ItemStack itemIcon(String target, String fallbackNamespace) {
        for (Identifier id : targetIds(target, fallbackNamespace)) {
            ItemStack stack = BuiltInRegistries.ITEM.getOptional(id)
                    .filter(item -> item != Items.AIR)
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack blockIcon(String target, String fallbackNamespace) {
        for (Identifier id : targetIds(target, fallbackNamespace)) {
            ItemStack stack = BuiltInRegistries.BLOCK.getOptional(id)
                    .map(block -> block.asItem())
                    .filter(item -> item != Items.AIR)
                    .map(ItemStack::new)
                    .orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<Identifier> targetIds(String target, String fallbackNamespace) {
        if (target == null || target.isBlank()) {
            return List.of();
        }
        String value = target.strip();
        if (value.contains(":")) {
            Identifier id = Identifier.tryParse(value);
            return id == null ? List.of() : List.of(id);
        }
        List<Identifier> ids = new ArrayList<>();
        addTargetId(ids, fallbackNamespace, value);
        addTargetId(ids, "minecraft", value);
        return List.copyOf(ids);
    }

    private static void addTargetId(List<Identifier> ids, String namespace, String path) {
        if (namespace == null || namespace.isBlank() || path == null || path.isBlank()) {
            return;
        }
        Identifier id = Identifier.tryParse(namespace + ":" + path);
        if (id != null && !ids.contains(id)) {
            ids.add(id);
        }
    }

    private static TerminalMissionReward reward(IRewardView reward) {
        ItemStack stack = reward.stack();
        if (!stack.isEmpty()) {
            return new TerminalMissionReward(stack, reward.label(), reward.detail());
        }
        return TerminalMissionReward.text(reward.label(), reward.detail());
    }

    private static TerminalMissionAction action(MissionActionView action) {
        return action.enabled()
                ? TerminalMissionAction.enabled(action.id(), action.label())
                : TerminalMissionAction.disabled(action.id(), action.label(), action.disabledReason());
    }

    private static TerminalMissionStatus status(MissionStatus status) {
        return switch (status) {
            case LOCKED -> TerminalMissionStatus.LOCKED;
            case UNLOCKED, AVAILABLE, ACTIVE -> TerminalMissionStatus.UNLOCKED;
            case COMPLETED, COMPLETE -> TerminalMissionStatus.COMPLETED;
            case CLAIMABLE -> TerminalMissionStatus.CLAIMABLE;
            case CLAIMED -> TerminalMissionStatus.CLAIMED;
            case FAILED, VIEW_ONLY -> TerminalMissionStatus.VIEW_ONLY;
        };
    }

    private static String phaseTitle(MissionDefinition definition) {
        if (!definition.phaseTitle().isBlank()) {
            return definition.phaseTitle();
        }
        return readableId(definition.chapterId().getPath());
    }

    private static String readableId(String path) {
        if (path == null || path.isBlank()) {
            return "Mission Route";
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
        return label.length() == 0 ? "Mission Route" : label.toString();
    }

    private static Optional<MissionDefinition> importedMission(Identifier missionId) {
        return EchoCoreServices.missionService()
                .mission(null, missionId)
                .map(IMissionProgressView::definition);
    }

    private static int routePhase(
            TerminalMissionDefinition definition,
            TerminalMissionRole role,
            MissionDefinition imported) {
        if (imported != null) {
            String phase = imported.metadata().get("terminal_route_phase");
            if (phase != null && !phase.isBlank()) {
                try {
                    return Integer.parseInt(phase);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        String namespace = definition.id().getNamespace();
        String path = definition.id().getPath();
        if ("echoashfallprotocol".equals(namespace)) {
            return ashfallRoutePhase(path, definition.phaseOrder());
        }
        if ("echoindustrialnexus".equals(namespace) && definition.phaseOrder() >= 4 && definition.phaseOrder() <= 8) {
            return Math.min(15, Math.max(0, definition.phaseOrder() + 5));
        }
        return switch (namespace) {
            case "echotutorialcore", "echorecovery" -> Math.min(1, Math.max(0, definition.phaseOrder()));
            case "echoagriculturereclamation" -> reclamationRoutePhase(path, definition.phaseOrder());
            case "echoholomap", "echoindex", "echolens" -> reconRoutePhase(path, definition.phaseOrder());
            case "echoblockworks" -> 5;
            case "echomultiblockcore" -> 6;
            case "echoindustrialnexus" -> industrialRoutePhase(path, definition.phaseOrder());
            case "echoarmory" -> armoryRoutePhase(path, definition.phaseOrder());
            case "echologisticsnetwork", "echoconvoyprotocol" -> 8;
            case "echorelictech" -> definition.phaseOrder() >= 60 ? 14 : 10;
            case "echostationfall" -> definition.phaseOrder() <= 0 ? 10
                    : definition.phaseOrder() >= 3 ? 13 : 11;
            case "echoorbitalremnants" -> {
                if ("echo_zero".equals(path)) {
                    yield 14;
                }
                if (definition.phaseOrder() >= 8) {
                    yield 15;
                }
                yield definition.phaseOrder() >= 2 ? 13 : 10;
            }
            case "echonexusprotocol", "echoblackboxprotocol" -> definition.phaseOrder() >= 5 ? 14 : 13;
            default -> role == TerminalMissionRole.OPTIONAL ? 10 : 15;
        };
    }

    private static int routeOrder(TerminalMissionDefinition definition, MissionDefinition imported) {
        if (imported != null) {
            String value = imported.metadata().get("terminal_route_order");
            if (value != null && !value.isBlank()) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return definition.phaseOrder() * 100 + definition.missionOrder();
    }

    private static TerminalMissionRole routeRole(MissionDefinition definition, TerminalMissionRole fallback) {
        String value = definition.metadata().get("terminal_route_role");
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return TerminalMissionRole.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static TerminalMissionRole resolvedRouteRole(
            TerminalMissionDefinition definition,
            TerminalMissionSnapshot snapshot,
            TerminalMissionRole role,
            MissionDefinition imported) {
        if (snapshot != null && snapshot.status() == TerminalMissionStatus.VIEW_ONLY) {
            return TerminalMissionRole.REFERENCE;
        }
        TerminalMissionRole fallback = role;
        if (fallback == null && imported != null) {
            fallback = imported.kind() == MissionKind.MAIN
                    ? TerminalMissionRole.MAIN
                    : TerminalMissionRole.OPTIONAL;
        }
        if (fallback == null) {
            fallback = TerminalMissionRole.fallback(definition, snapshot);
        }
        return imported == null ? fallback : routeRole(imported, fallback);
    }

    private static boolean routeVisible(MissionDefinition definition, boolean fallback) {
        if (definition == null) {
            return fallback;
        }
        String value = definition.metadata().get("terminal_route_visible");
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value);
    }

    private static List<Identifier> routePrerequisites(Map<String, String> metadata) {
        List<Identifier> prerequisites = new ArrayList<>();
        for (String key : List.of("terminal_route_prerequisites", "terminal_route_gate")) {
            for (Identifier id : identifiers(metadata.get(key), null)) {
                if (!prerequisites.contains(id)) {
                    prerequisites.add(id);
                }
            }
        }
        return List.copyOf(prerequisites);
    }

    private static List<TerminalMissionIntelUnlock> intelUnlocks(Map<String, String> metadata, String fallbackNamespace) {
        List<TerminalMissionIntelUnlock> unlocks = new ArrayList<>();
        for (Identifier id : identifiers(metadata.get("terminal_intel_archives"), fallbackNamespace)) {
            unlocks.add(TerminalMissionIntelUnlock.archive(id, "", ""));
        }
        for (Identifier id : identifiers(metadata.get("terminal_intel_routes"), fallbackNamespace)) {
            unlocks.add(TerminalMissionIntelUnlock.route(id, "", ""));
        }
        for (Identifier id : identifiers(metadata.get("terminal_intel_discoveries"), fallbackNamespace)) {
            unlocks.add(TerminalMissionIntelUnlock.discovery(id, "", ""));
        }
        for (Identifier id : identifiers(metadata.get("terminal_intel_factions"), fallbackNamespace)) {
            unlocks.add(TerminalMissionIntelUnlock.faction(id, "", ""));
        }
        for (Identifier id : identifiers(metadata.get("terminal_intel_pois"), fallbackNamespace)) {
            unlocks.add(TerminalMissionIntelUnlock.poi(id, "", ""));
        }
        return unlocks.stream().distinct().toList();
    }

    private static List<Identifier> identifiers(String value, String fallbackNamespace) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Identifier> ids = new ArrayList<>();
        for (String raw : value.split(",")) {
            identifier(raw, fallbackNamespace).ifPresent(id -> {
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            });
        }
        return List.copyOf(ids);
    }

    private static Optional<Identifier> identifier(String raw, String fallbackNamespace) {
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

    private static int ashfallRoutePhase(String path, int fallback) {
        return switch (path) {
            case "secure_crash_outpost", "craft_scrap_knife", "drink_clean_water" -> 0;
            case "secure_emergency_water_loop", "forage_wasteland_food", "plant_mutated_sapling",
                    "build_rain_collector", "stockpile_rations", "secure_sleep_shelter" -> 1;
            case "build_water_purifier", "stockpile_clean_water" -> 2;
            case "assemble_wasteland_field_kit", "find_schematic_fragment" -> 3;
            case "build_hand_recycler", "make_machine_casing", "build_micro_generator",
                    "build_battery_bank", "build_scrap_dynamo", "charge_basic_battery",
                    "route_power_cable", "upgrade_power_cable", "install_energy_meter",
                    "set_power_priority" -> 4;
            case "build_scrap_press", "overclock_machine", "install_item_pipe",
                    "build_thermal_burner", "base_stability_check", "build_research_lab",
                    "first_schematic", "build_factory_controller" -> 5;
            case "equip_gas_mask", "fix_mask_filter", "build_filter_workbench",
                    "craft_advanced_filter" -> 6;
            case "craft_portable_scanner", "expedition_readiness", "scan_first_poi",
                    "loot_survivor_cache", "poi_explorer" -> 7;
            case "first_faction_contact", "complete_first_faction_task", "repair_echo_drone",
                    "recover_drone_intel", "faction_reputation", "first_perk" -> 8;
            case "enter_bio_lab", "recover_data_log", "survey_reactor_ruin",
                    "build_field_med_bay", "use_field_med_bay", "craft_radaway",
                    "stabilize_mutation_effects", "scout_radiation_zone",
                    "build_atmospheric_scrubber", "build_radiation_cleanser",
                    "collect_mutated_tissue", "craft_mutagen_vial" -> 9;
            case "clear_military_vault", "find_dense_alloy", "build_thermal_array",
                    "build_ore_grinder", "build_isotope_refiner", "forge_alloy_weapon",
                    "equip_alloy_kit", "stockpile_route_supplies", "calibrate_midgame_grid" -> 10;
            case "deploy_stationary_scanner", "activate_power_node", "build_nexus_capacitor",
                    "build_workshop", "activate_relay_station" -> 11;
            case "neutralize_plains_warlord", "neutralize_city_ruin_stalker",
                    "neutralize_industrial_juggernaut", "neutralize_toxic_hive_matriarch",
                    "neutralize_crash_zone_colossus", "neutralize_radiation_behemoth" -> 12;
            case "enter_cryogenic_ruins", "recover_cryo_sample", "warm_up_after_exposure",
                    "craft_cold_route_supplies", "neutralize_cryogenic_overseer" -> 13;
            case "neutralize_nexus_scar_avatar", "find_nexus_core", "awaken_nexus_core",
                    "scan_prime_relays", "resolve_prime_relays", "stabilize_nexus_grid",
                    "survive_core_countermeasure", "reach_decision" -> 14;
            case "restore_repair_nodes", "restore_purge_corruption", "restore_enter_archives",
                    "restore_guardian", "restore_world_lattice", "restore_finale",
                    "restore_epilogue", "destroy_scorched_earth", "destroy_survive_storms",
                    "destroy_enter_archives", "destroy_guardian", "destroy_dead_signal",
                    "destroy_finale", "destroy_epilogue", "control_signal_expansion",
                    "control_resource_dominance", "control_enter_archives", "control_guardian",
                    "control_command_lattice", "control_finale", "control_epilogue" -> 15;
            default -> Math.max(0, Math.min(15, fallback));
        };
    }

    private static int reclamationRoutePhase(String path, int fallback) {
        return switch (path) {
            case "mission/recover_seed", "recover_seed", "mission/analyze_soil", "analyze_soil" -> 2;
            case "mission/first_growth", "first_growth" -> 3;
            case "mission/gene_stabilization", "gene_stabilization" -> 6;
            case "mission/greenhouse_online", "greenhouse_online" -> 9;
            case "mission/restore_chunk", "restore_chunk" -> 15;
            default -> Math.max(2, Math.min(15, fallback + 2));
        };
    }

    private static int reconRoutePhase(String path, int fallback) {
        return switch (path) {
            case "discover_terrain", "open_search_entry", "read_tutorial_entry" -> 3;
            case "verified_deep_scan", "machine_diagnostic", "sync_route",
                    "reveal_marker", "inspect_recipe_source", "follow_source_note",
                    "bookmark_record", "pin_recipe_plan", "transfer_recipe_plan",
                    "use_lens_shortcut" -> 7;
            default -> Math.max(3, Math.min(8, fallback + 3));
        };
    }

    private static int industrialRoutePhase(String path, int fallback) {
        String signal = path.toLowerCase(java.util.Locale.ROOT);
        if (signal.contains("filter") || signal.contains("metal") || signal.contains("grind")
                || signal.contains("reclaim_power") || signal.contains("power")) {
            return 5;
        }
        if (fallback >= 4) {
            return Math.min(14, fallback + 5);
        }
        return 10;
    }

    private static int armoryRoutePhase(String path, int fallback) {
        return switch (path) {
            case "inspect_loadout", "forge_upgrade", "install_module", "recharge_core" -> 8;
            case "bind_loadout", "prepare_route_kit", "dispatch_route_kit" -> 10;
            default -> Math.max(8, Math.min(12, fallback + 8));
        };
    }

    private static int intMetadata(MissionDefinition definition, String key, int fallback) {
        String value = definition.metadata().get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
