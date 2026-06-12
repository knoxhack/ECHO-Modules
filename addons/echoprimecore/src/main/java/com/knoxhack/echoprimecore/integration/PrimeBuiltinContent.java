package com.knoxhack.echoprimecore.integration;

import com.echoplatform.echocore.api.prime.PrimeAuditRegistry;
import com.echoplatform.echocore.api.prime.PrimeHoloMapRegistry;
import com.echoplatform.echocore.api.prime.PrimeIndexRegistry;
import com.echoplatform.echocore.api.prime.PrimeIntegrationContext;
import com.echoplatform.echocore.api.prime.PrimeLensRegistry;
import com.echoplatform.echocore.api.prime.PrimeLootRegistry;
import com.echoplatform.echocore.api.prime.PrimeMissionRegistry;
import com.echoplatform.echocore.api.prime.PrimeProgressionRegistry;
import com.echoplatform.echocore.api.prime.PrimeRouteRegistry;
import com.echoplatform.echocore.api.prime.PrimeTerminalRegistry;
import com.echoplatform.echocore.api.prime.PrimeWorldRegistry;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.PrimeIds;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class PrimeBuiltinContent {
    private PrimeBuiltinContent() {
    }

    public static void register(PrimeIntegrationContext context) {
        registerFlags(context);
        registerCoreRoutes(context);
        registerCoreMissions(context);
        registerIndex(context);
        registerLens(context);
        registerHoloMap(context);
        registerTerminal(context);
        registerLoot(context);
        registerWorld(context);
        registerDetectedModules(context);
    }

    private static void registerFlags(PrimeIntegrationContext context) {
        int order = 0;
        for (Identifier flag : PrimeIds.FLAGS) {
            context.progressionRegistry().registerFlag(new PrimeProgressionRegistry.PrimeProgressionFlag(
                    flag, title(flag), "Prime Survival progression flag.", order == 0, order++));
        }
    }

    private static void registerCoreRoutes(PrimeIntegrationContext context) {
        route(context, PrimeIds.ROUTE_SURVIVAL, "Prime Survival", "Stable overworld survival, discovery, first tech, and route branching.", EchoPrimeCore.id("started"), List.of("echoprimecore"), 0, 0xFF65E6D6);
        route(context, PrimeIds.ROUTE_TECH, "Technology", "Starter circuits, machine frames, scanner upgrades, and first machine readiness.", EchoPrimeCore.id("first_machine"), List.of("echoprimecore"), 10, 0xFFB8D36B);
        route(context, PrimeIds.ROUTE_POWER, "PowerGrid", "Power node scans, cells, coils, generators, and grid status.", EchoPrimeCore.id("powergrid_online"), List.of("echopowergrid"), 20, 0xFFFFD166);
        route(context, PrimeIds.ROUTE_STORAGE, "Storage and Logistics", "Storage chips, crates, route cards, depot requests, and logistics readiness.", EchoPrimeCore.id("storage_online"), List.of("echologisticsnetwork"), 30, 0xFF9AD7FF);
        route(context, PrimeIds.ROUTE_BASE, "BaseGrid", "Base anchors, field workbench progression, and stable base dashboards.", EchoPrimeCore.id("basegrid_online"), List.of("echobasegrid"), 40, 0xFF90E28B);
        route(context, PrimeIds.ROUTE_ARCANA, "Arcana", "Aether traces, grimoire hooks, rituals, spells, curses, and familiar readiness.", EchoPrimeCore.id("arcana_route_open"), List.of("echoarcanacore"), 50, 0xFFB68CFF);
        route(context, PrimeIds.ROUTE_RELIC, "RelicTech", "Relic vaults, analyzer warnings, unstable drops, and relic route readiness.", EchoPrimeCore.id("relic_route_open"), List.of("echorelictech"), 60, 0xFFFF9F7A);
        route(context, PrimeIds.ROUTE_NEXUS, "Nexus", "Late-game traces, orbital signals, Stationfall echoes, and Nexus route readiness.", EchoPrimeCore.id("nexus_trace_found"), List.of("echonexusprotocol"), 90, 0xFF7ACBFF);
    }

    private static void registerCoreMissions(PrimeIntegrationContext context) {
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                PrimeIds.CHAIN_SURVIVAL,
                "Prime Survival",
                "Begin, find the first signal, investigate a relay ruin, and branch into first tech routes.",
                PrimeIds.ROUTE_SURVIVAL,
                List.of(
                        EchoPrimeCore.id("mission/prime_survival_begin"),
                        EchoPrimeCore.id("mission/first_signal"),
                        EchoPrimeCore.id("mission/first_ruin"),
                        EchoPrimeCore.id("mission/first_tech"),
                        EchoPrimeCore.id("mission/powergrid_online"),
                        EchoPrimeCore.id("mission/storage_online"),
                        EchoPrimeCore.id("mission/base_online"),
                        EchoPrimeCore.id("mission/branch_discovery")),
                0));
    }

    private static void registerIndex(PrimeIntegrationContext context) {
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                PrimeIds.INDEX_CATEGORY_PRIME,
                "Prime Survival",
                "Starter survival spine, signal materials, scanner recipes, and first tech hints.",
                EchoPrimeCore.id("started"),
                EchoPrimeCore.MODID,
                0));
        recipeHint(context, "crude_scanner", "Crude Scanner", "Signal Shard + Scanner Handle + Basic Lens + Circuit Plate.", EchoPrimeCore.id("lens_online"), 10);
        recipeHint(context, "circuit_plate", "Circuit Plate", "Broken Circuit, Wire Bundle, and Relay Fragment reveal reliable starter circuits.", EchoPrimeCore.id("first_ruin"), 20);
        recipeHint(context, "prime_circuit", "Prime Circuit", "First Tech unlock. Stabilize circuits before route-specific machine work.", EchoPrimeCore.id("first_signal"), 30);
        recipeHint(context, "machine_frame", "Machine Frame", "Foundation for PowerGrid, storage, and industrial route machinery.", EchoPrimeCore.id("first_signal"), 40);
    }

    private static void registerLens(PrimeIntegrationContext context) {
        String[] types = {
                "prime_material", "prime_structure", "prime_machine", "prime_power_node", "prime_storage_node",
                "prime_mob", "prime_relic", "prime_arcana", "prime_nexus_trace", "prime_weather_device",
                "prime_basegrid_node"
        };
        for (int i = 0; i < types.length; i++) {
            context.lensRegistry().registerScanType(new PrimeLensRegistry.PrimeScanType(
                    EchoPrimeCore.id("scan/" + types[i]), title(types[i]), types[i], i));
        }
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                EchoPrimeCore.id("scan_data/signal_shard"),
                EchoPrimeCore.id("scan/prime_material"),
                EchoPrimeCore.id("signal_shard").toString(),
                "Crafts the Crude Scanner and first signal tooling.",
                "none",
                "Signal ore, relay caches, and Prime starter structures.",
                "Weakly resonant. Pair with a lens and circuit plate.",
                EchoPrimeCore.MODID));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                EchoPrimeCore.id("scan_data/abandoned_relay_post"),
                EchoPrimeCore.id("scan/prime_structure"),
                EchoPrimeCore.id("abandoned_relay_post").toString(),
                "First ruin, first cache, and first HoloMap marker.",
                "low",
                "Relay Fragment, Circuit Plate, Wire Bundle, Broken Circuit.",
                "A broken relay can still teach the route forward.",
                EchoPrimeCore.MODID));
        scanData(context, "relay_fragment", "prime_material", "Repairs starter relay hardware and unlocks circuit plate recipes.", "none", "Relay caches and old storage caches.", "Keep one for First Tech recipes.");
        scanData(context, "circuit_plate", "prime_material", "Starter circuit substrate for scanners, consoles, and machine frames.", "none", "Relay caches, old workshops, and Prime ruins.", "Circuit plates are the handoff from scavenging into crafting.");
        scanData(context, "signal_ore", "prime_material", "Early overworld signal source for Signal Shards.", "none", "Signal Shard.", "Mine sparingly until a Crude Scanner is online.");
        scanData(context, "relay_casing", "prime_structure", "Marks relay ruins, signal posts, and degraded Prime field sites.", "low", "Relay Fragment, Wire Bundle, Broken Circuit.", "Casing patterns usually point toward a nearby cache.");
        scanData(context, "dormant_relay_core", "prime_machine", "Dormant Prime machine core awaiting power, data, or route unlocks.", "medium", "Relay Coil, Data Core Fragment, Prime Key Fragment.", "A dormant core should be mapped before it is powered.");
        scanData(context, "storage_crate", "prime_storage_node", "Starter Prime storage node for early base and logistics readiness.", "none", "Storage Chip and field supplies.", "Storage is the first quality-of-life route.");
        scanData(context, "scrap_crawler", "prime_mob", "Low-tier salvage predator drawn to weak signal caches.", "low", "Broken Circuit, Scrap Deposit fragments.", "It is fragile, but it means the signal is not quiet.");
        scanData(context, "broken_survey_drone", "prime_mob", "Damaged survey drone with useful data salvage.", "low", "Data Wafer Blank, Wire Bundle.", "Drones often patrol near structures worth scanning.");
        scanData(context, "prime_guardian", "prime_mob", "Late Prime convergence guardian tied to Nexus readiness.", "critical", "Prime Key Fragment and Nexus-grade route data.", "Do not wake this route before your power, storage, and combat systems are online.");
    }

    private static void registerHoloMap(PrimeIntegrationContext context) {
        layer(context, PrimeIds.MAP_LAYER_SIGNALS, "Prime Signals", "Weak signal vectors, orbital traces, and first-route pings.", 0xFF65E6D6, true, 0);
        layer(context, PrimeIds.MAP_LAYER_RUINS, "Relay Ruins", "Relay ruins, vaults, blackbox locations, and Prime route structures.", 0xFFFFD166, true, 10);
        layer(context, EchoPrimeCore.id("layer/data_vaults"), "Data Vaults", "Decoded data vaults and blackbox cache targets.", 0xFF9AD7FF, true, 20);
        layer(context, EchoPrimeCore.id("layer/convoy_wrecks"), "Convoy Wrecks", "Supply wrecks and logistics reward markers.", 0xFFE6B86E, false, 25);
        layer(context, EchoPrimeCore.id("layer/powergrid_nodes"), "PowerGrid Nodes", "Power route anchors and grid scan targets.", 0xFFFFCC66, false, 30);
        layer(context, EchoPrimeCore.id("layer/basegrid_locations"), "BaseGrid Locations", "Base anchors, claims, and recovery points.", 0xFF90E28B, false, 40);
        layer(context, EchoPrimeCore.id("layer/storage_networks"), "Storage Networks", "Storage nodes, depot links, and logistics network markers.", 0xFF9AD7FF, false, 45);
        layer(context, EchoPrimeCore.id("layer/arcana_rifts"), "Arcana Rifts", "Aether traces, ritual sites, and arcane route markers.", 0xFFB68CFF, false, 50);
        layer(context, EchoPrimeCore.id("layer/weather_events"), "Weather Events", "Prime weather warnings and environmental route alerts.", 0xFF79C7D9, false, 55);
        layer(context, EchoPrimeCore.id("layer/nexus_traces"), "Nexus Traces", "Late-game route readiness and anomaly markers.", 0xFF7ACBFF, false, 90);
        layer(context, EchoPrimeCore.id("layer/death_recovery"), "Death Recovery", "Death markers, grave scans, and recovery routes.", 0xFFB8C7D9, true, 95);
        layer(context, EchoPrimeCore.id("layer/mission_targets"), "Mission Targets", "Prime Survival objectives and route-specific mission destinations.", 0xFFFFFFFF, true, 100);
        marker(context, PrimeIds.MARKER_RELAY_RUIN, PrimeIds.MAP_LAYER_RUINS, "Relay Ruin", "relay_ruin", 0);
        marker(context, EchoPrimeCore.id("marker/signal_source"), PrimeIds.MAP_LAYER_SIGNALS, "Signal Source", "signal_source", 10);
        marker(context, EchoPrimeCore.id("marker/data_vault"), EchoPrimeCore.id("layer/data_vaults"), "Data Vault", "data_vault", 20);
        marker(context, EchoPrimeCore.id("marker/blackbox_location"), EchoPrimeCore.id("layer/data_vaults"), "Blackbox Location", "blackbox_location", 22);
        marker(context, EchoPrimeCore.id("marker/convoy_wreck"), EchoPrimeCore.id("layer/convoy_wrecks"), "Convoy Wreck", "convoy_wreck", 25);
        marker(context, EchoPrimeCore.id("marker/power_node"), EchoPrimeCore.id("layer/powergrid_nodes"), "Power Node", "power_node", 30);
        marker(context, EchoPrimeCore.id("marker/base_anchor"), EchoPrimeCore.id("layer/basegrid_locations"), "Base Anchor", "base_anchor", 40);
        marker(context, EchoPrimeCore.id("marker/arcana_rift"), EchoPrimeCore.id("layer/arcana_rifts"), "Arcana Rift", "arcana_rift", 50);
        marker(context, EchoPrimeCore.id("marker/relic_vault"), EchoPrimeCore.id("layer/data_vaults"), "Relic Vault", "relic_vault", 60);
        marker(context, EchoPrimeCore.id("marker/orbital_signal"), PrimeIds.MAP_LAYER_SIGNALS, "Orbital Signal", "orbital_signal", 82);
        marker(context, EchoPrimeCore.id("marker/nexus_trace"), EchoPrimeCore.id("layer/nexus_traces"), "Nexus Trace", "nexus_trace", 90);
        marker(context, EchoPrimeCore.id("marker/death_marker"), EchoPrimeCore.id("layer/death_recovery"), "Death Marker", "death_marker", 95);
    }

    private static void registerTerminal(PrimeIntegrationContext context) {
        card(context, PrimeIds.DASHBOARD_CARD, PrimeIds.ROUTE_SURVIVAL, "Prime Dashboard",
                "Current objective, stage, signal level, route readiness, modules, and warnings.", EchoPrimeCore.id("started"), 0);
        for (PrimeRouteRegistry.PrimeRoute route : context.routeRegistry().routes()) {
            card(context, EchoPrimeCore.id("terminal/route_card/" + route.id().getPath().replace('/', '_')),
                    route.id(), route.title(), route.summary(), route.unlockFlag(), route.order() + 10);
        }
    }

    private static void registerLoot(PrimeIntegrationContext context) {
        String[] pools = {
                "prime_common_cache", "prime_field_cache", "prime_relay_cache", "prime_data_cache",
                "prime_vault_cache", "prime_relic_cache", "prime_nexus_cache"
        };
        for (int i = 0; i < pools.length; i++) {
            context.lootRegistry().registerPool(new PrimeLootRegistry.PrimeLootPool(
                    EchoPrimeCore.id("loot/" + pools[i]), pools[i], "Prime loot tier.", i));
        }
        loot(context, "signal_shard", "prime_common_cache", "signal_shard", 1, 3, 20);
        loot(context, "relay_fragment", "prime_relay_cache", "relay_fragment", 1, 3, 16);
        loot(context, "circuit_plate", "prime_relay_cache", "circuit_plate", 1, 2, 14);
        loot(context, "wire_bundle", "prime_field_cache", "wire_bundle", 2, 6, 20);
        loot(context, "broken_circuit", "prime_field_cache", "broken_circuit", 1, 4, 18);
        loot(context, "storage_chip", "prime_field_cache", "storage_chip", 1, 1, 8);
        loot(context, "data_wafer_blank", "prime_data_cache", "data_wafer_blank", 1, 2, 10);
        loot(context, "prime_key_fragment", "prime_nexus_cache", "prime_key_fragment", 1, 1, 3);
    }

    private static void registerWorld(PrimeIntegrationContext context) {
        structure(context, "abandoned_relay_post", "Abandoned Relay Post", "First signal ruin and relay cache.", PrimeIds.MARKER_RELAY_RUIN, EchoPrimeCore.id("loot/prime_relay_cache"), 0);
        structure(context, "broken_survey_station", "Broken Survey Station", "Damaged survey station with scanner parts and data wafers.", EchoPrimeCore.id("marker/signal_source"), EchoPrimeCore.id("loot/prime_field_cache"), 5);
        structure(context, "old_storage_cache", "Old Storage Cache", "Early storage and repair materials.", EchoPrimeCore.id("marker/data_vault"), EchoPrimeCore.id("loot/prime_field_cache"), 10);
        structure(context, "buried_cable_node", "Buried Cable Node", "Subsurface cable node that hints at PowerGrid and logistics routes.", EchoPrimeCore.id("marker/power_node"), EchoPrimeCore.id("loot/prime_common_cache"), 12);
        structure(context, "echo_field_lab", "ECHO Field Lab", "Field research lab with Index, Lens, and data console clues.", EchoPrimeCore.id("marker/data_vault"), EchoPrimeCore.id("loot/prime_data_cache"), 14);
        structure(context, "signal_tower", "Signal Tower", "Overworld signal landmark that escalates HoloMap and orbital hints.", EchoPrimeCore.id("marker/signal_source"), EchoPrimeCore.id("loot/prime_relay_cache"), 16);
        structure(context, "abandoned_workshop", "Abandoned Workshop", "Starter workshop with machine casing, plates, and first machine clues.", PrimeIds.MARKER_RELAY_RUIN, EchoPrimeCore.id("loot/prime_field_cache"), 18);
        structure(context, "data_vault", "Data Vault", "Encoded data cache that opens blackbox, relic, and Nexus routes.", EchoPrimeCore.id("marker/data_vault"), EchoPrimeCore.id("loot/prime_vault_cache"), 19);
        structure(context, "prime_relay_ruin", "Prime Relay Ruin", "Late starter structure that opens broader Prime route readiness.", PrimeIds.MARKER_RELAY_RUIN, EchoPrimeCore.id("loot/prime_data_cache"), 20);
        context.worldRegistry().registerWorldSignal(new PrimeWorldRegistry.PrimeWorldSignal(
                EchoPrimeCore.id("signal/low_activity"),
                "Low Signal Activity",
                "Stable overworld; weak ECHO signal records appear near relay ruins.",
                1,
                0));
        context.worldRegistry().registerWorldSignal(new PrimeWorldRegistry.PrimeWorldSignal(
                EchoPrimeCore.id("signal/route_ready"),
                "Route Ready",
                "Multiple systems are online; route signals can now converge into late-game traces.",
                3,
                30));
        context.worldRegistry().registerWorldSignal(new PrimeWorldRegistry.PrimeWorldSignal(
                EchoPrimeCore.id("signal/nexus_pressure"),
                "Nexus Pressure",
                "Anomaly records are active and Prime Guardian readiness should be audited.",
                5,
                90));
    }

    private static void registerDetectedModules(PrimeIntegrationContext context) {
        moduleRoute(context, "echoindustrialnexus", PrimeIds.ROUTE_TECH, "Technology route provider detected.", EchoPrimeCore.id("industrial_route_open"));
        moduleRoute(context, "echopowergrid", PrimeIds.ROUTE_POWER, "PowerGrid route provider detected.", EchoPrimeCore.id("powergrid_online"));
        moduleRoute(context, "echologisticsnetwork", PrimeIds.ROUTE_STORAGE, "Logistics route provider detected.", EchoPrimeCore.id("logistics_online"));
        moduleRoute(context, "echobasegrid", PrimeIds.ROUTE_BASE, "BaseGrid route provider detected.", EchoPrimeCore.id("basegrid_online"));
        moduleRoute(context, "echoagriculturereclamation", EchoPrimeCore.id("route/agriculture"), "Agriculture route provider detected.", EchoPrimeCore.id("agriculture_route_open"));
        moduleRoute(context, "echoarmory", EchoPrimeCore.id("route/combat"), "Combat route provider detected.", EchoPrimeCore.id("combat_route_open"));
        moduleRoute(context, "echorelictech", PrimeIds.ROUTE_RELIC, "RelicTech route provider detected.", EchoPrimeCore.id("relic_route_open"));
        moduleRoute(context, "echoarcanacore", PrimeIds.ROUTE_ARCANA, "Arcana route provider detected.", EchoPrimeCore.id("arcana_route_open"));
        moduleRoute(context, "echonexusprotocol", PrimeIds.ROUTE_NEXUS, "Nexus route provider detected.", EchoPrimeCore.id("nexus_trace_found"));
        moduleRoute(context, "echoorbitalremnants", EchoPrimeCore.id("route/orbital"), "Orbital signal route provider detected.", EchoPrimeCore.id("orbital_signal_found"));
        moduleRoute(context, "echostationfall", EchoPrimeCore.id("route/stationfall"), "Stationfall trace route provider detected.", EchoPrimeCore.id("stationfall_trace_found"));
    }

    private static void moduleRoute(PrimeIntegrationContext context, String modId, Identifier routeId, String summary, Identifier flag) {
        if (!context.moduleLoaded(modId)) {
            context.auditRegistry().registerDiagnostic(new PrimeAuditRegistry.PrimeAuditDiagnostic(
                    EchoPrimeCore.id("audit/missing_module/" + modId),
                    PrimeAuditRegistry.Severity.INFO,
                    "Optional Prime module not installed",
                    modId + " is not loaded; its Prime route will remain dormant.",
                    modId));
            return;
        }
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                EchoPrimeCore.id("terminal/module/" + modId),
                routeId,
                modId,
                summary,
                flag,
                modId,
                100));
    }

    private static void route(PrimeIntegrationContext context, Identifier id, String title, String summary,
            Identifier unlockFlag, List<String> requiredModules, int order, int color) {
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(id, title, summary, unlockFlag, requiredModules, order, color));
    }

    private static void recipeHint(PrimeIntegrationContext context, String path, String title, String hint,
            Identifier unlockFlag, int order) {
        context.indexRegistry().registerRecipeHint(new PrimeIndexRegistry.PrimeRecipeHint(
                EchoPrimeCore.id("recipe_hint/" + path),
                PrimeIds.INDEX_CATEGORY_PRIME,
                title,
                hint,
                unlockFlag,
                EchoPrimeCore.MODID,
                order));
    }

    private static void scanData(PrimeIntegrationContext context, String path, String type, String uses,
            String threat, String drops, String hint) {
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                EchoPrimeCore.id("scan_data/" + path),
                EchoPrimeCore.id("scan/" + type),
                EchoPrimeCore.id(path).toString(),
                uses,
                threat,
                drops,
                hint,
                EchoPrimeCore.MODID));
    }

    private static void layer(PrimeIntegrationContext context, Identifier id, String title, String summary,
            int color, boolean visible, int order) {
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(id, title, summary, color, visible, order));
    }

    private static void marker(PrimeIntegrationContext context, Identifier id, Identifier layerId, String title,
            String icon, int order) {
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(id, layerId, title, icon, order));
    }

    private static void card(PrimeIntegrationContext context, Identifier id, Identifier routeId, String title,
            String summary, Identifier flag, int order) {
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id, routeId, title, summary, flag, EchoPrimeCore.MODID, order));
    }

    private static void loot(PrimeIntegrationContext context, String id, String pool, String item, int min, int max, int weight) {
        context.lootRegistry().registerInjection(new PrimeLootRegistry.PrimeLootInjection(
                EchoPrimeCore.id("loot_injection/" + id),
                EchoPrimeCore.id("loot/" + pool),
                EchoPrimeCore.id(item),
                min,
                max,
                weight,
                EchoPrimeCore.MODID));
    }

    private static void structure(PrimeIntegrationContext context, String path, String title, String summary,
            Identifier marker, Identifier lootPool, int order) {
        context.worldRegistry().registerStructure(new PrimeWorldRegistry.PrimeStructure(
                EchoPrimeCore.id(path), title, summary, marker, lootPool, order));
    }

    private static String title(Identifier id) {
        return title(id.getPath());
    }

    private static String title(String value) {
        String[] parts = value.replace('/', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
