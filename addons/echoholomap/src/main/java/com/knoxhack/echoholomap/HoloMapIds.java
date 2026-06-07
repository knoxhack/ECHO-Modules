package com.knoxhack.echoholomap;

import java.util.Locale;
import net.minecraft.resources.Identifier;

public final class HoloMapIds {
    public static final Identifier TAB = id("terminal/holomap");
    public static final Identifier REFRESH_ACTION = id("action/refresh");
    public static final Identifier TEST_MARKER_ACTION = id("action/test_marker");
    public static final Identifier DEBUG_SOURCE = id("debug");
    public static final Identifier CORE_SOURCE = id("core");
    public static final Identifier WORLD_SOURCE = id("worldcore");
    public static final Identifier DISCOVERY_SOURCE = id("core_discovery");
    public static final Identifier ROUTE_SOURCE = id("core_routes");
    public static final Identifier HAZARD_SOURCE = id("core_hazards");

    public static final Identifier CRASH_SITES = layer("crash_sites");
    public static final Identifier ROUTES = layer("routes");
    public static final Identifier HAZARDS = layer("hazards");
    public static final Identifier MISSIONS = layer("missions");
    public static final Identifier BASES_OUTPOSTS = layer("bases_outposts");
    public static final Identifier ORBITAL_SCANS = layer("orbital_scans");
    public static final Identifier NEXUS_ANOMALY = layer("nexus_anomaly");
    public static final Identifier DRONES_SCANS = layer("drones_scans");

    private HoloMapIds() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, sanitize(path));
    }

    public static Identifier layer(String path) {
        return id("layer/" + sanitize(path));
    }

    public static Identifier layerFromInput(String value) {
        String clean = sanitize(value == null ? "" : value.toLowerCase(Locale.ROOT));
        return switch (clean) {
            case "crash", "crash_site", "crash_sites" -> CRASH_SITES;
            case "route", "routes" -> ROUTES;
            case "hazard", "hazards" -> HAZARDS;
            case "mission", "missions" -> MISSIONS;
            case "base", "bases", "outpost", "outposts", "bases_outposts" -> BASES_OUTPOSTS;
            case "orbital", "orbital_scan", "orbital_scans" -> ORBITAL_SCANS;
            case "nexus", "anomaly", "nexus_anomaly" -> NEXUS_ANOMALY;
            case "drone", "drones", "scan", "scans", "drones_scans" -> DRONES_SCANS;
            default -> layer(clean.isBlank() ? "debug" : clean);
        };
    }

    private static String sanitize(String value) {
        String clean = value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
        clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }
}
