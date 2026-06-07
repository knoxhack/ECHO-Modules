package com.knoxhack.echoprimecore;

import java.util.List;
import net.minecraft.resources.Identifier;

public final class PrimeIds {
    public static final List<Identifier> FLAGS = List.of(
            id("started"),
            id("index_online"),
            id("terminal_online"),
            id("first_signal"),
            id("lens_online"),
            id("first_ruin"),
            id("first_blackbox"),
            id("holomap_online"),
            id("first_machine"),
            id("powergrid_online"),
            id("basegrid_online"),
            id("storage_online"),
            id("logistics_online"),
            id("first_combat_upgrade"),
            id("first_agriculture_upgrade"),
            id("first_relic"),
            id("first_aether_trace"),
            id("arcana_route_open"),
            id("industrial_route_open"),
            id("logistics_route_open"),
            id("agriculture_route_open"),
            id("combat_route_open"),
            id("relic_route_open"),
            id("orbital_signal_found"),
            id("stationfall_trace_found"),
            id("nexus_trace_found"),
            id("prime_guardian_defeated")
    );

    public static final Identifier ROUTE_SURVIVAL = id("route/survival");
    public static final Identifier ROUTE_TECH = id("route/technology");
    public static final Identifier ROUTE_POWER = id("route/powergrid");
    public static final Identifier ROUTE_STORAGE = id("route/storage_logistics");
    public static final Identifier ROUTE_BASE = id("route/basegrid");
    public static final Identifier ROUTE_ARCANA = id("route/arcana");
    public static final Identifier ROUTE_RELIC = id("route/relictech");
    public static final Identifier ROUTE_NEXUS = id("route/nexus");

    public static final Identifier CHAIN_SURVIVAL = id("mission_chain/prime_survival");
    public static final Identifier CHAPTER_SURVIVAL = id("chapter/prime_survival");
    public static final Identifier MAP_LAYER_SIGNALS = id("layer/prime_signals");
    public static final Identifier MAP_LAYER_RUINS = id("layer/relay_ruins");
    public static final Identifier MARKER_RELAY_RUIN = id("marker/relay_ruin");
    public static final Identifier DASHBOARD_CARD = id("terminal/prime_dashboard");
    public static final Identifier INDEX_CATEGORY_PRIME = id("index/prime_survival");

    private PrimeIds() {
    }

    public static Identifier id(String path) {
        return EchoPrimeCore.id(path);
    }
}
