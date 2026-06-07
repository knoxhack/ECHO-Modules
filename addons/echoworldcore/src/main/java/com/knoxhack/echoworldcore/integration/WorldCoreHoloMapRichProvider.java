package com.knoxhack.echoworldcore.integration;

import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echocore.api.WorldHazardSnapshot;
import com.knoxhack.echocore.api.WorldRegionInstance;
import com.knoxhack.echocore.api.WorldRegionType;
import com.knoxhack.echoholomap.api.HoloMapPrecision;
import com.knoxhack.echoholomap.api.HoloMapQuery;
import com.knoxhack.echoholomap.api.HoloMapZoneData;
import com.knoxhack.echoholomap.api.HoloMapZonePattern;
import com.knoxhack.echoholomap.api.HoloMapZoneShape;
import com.knoxhack.echoholomap.api.IHoloMapDataProvider;
import com.knoxhack.echoholomap.map.HoloMapService;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.knoxhack.echoworldcore.service.WorldRegionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum WorldCoreHoloMapRichProvider implements IHoloMapDataProvider {
    INSTANCE;

    private static final Identifier PROVIDER_ID = id("provider/holomap_zones");
    private static final Identifier HAZARDS = holomapLayer("hazards");
    private static final int DEFAULT_QUERY_RADIUS_BLOCKS = 512;

    public static void register() {
        HoloMapService.INSTANCE.registerHoloProvider(INSTANCE);
    }

    @Override
    public Identifier providerId() {
        return PROVIDER_ID;
    }

    @Override
    public List<HoloMapZoneData> zones(Player player) {
        return zones(player, HoloMapQuery.from(player, DEFAULT_QUERY_RADIUS_BLOCKS));
    }

    @Override
    public List<HoloMapZoneData> zones(Player player, HoloMapQuery query) {
        if (player == null) {
            return List.of();
        }
        HoloMapQuery safeQuery = query == null ? HoloMapQuery.from(player, DEFAULT_QUERY_RADIUS_BLOCKS) : query;
        List<HoloMapZoneData> zones = new ArrayList<>();
        for (WorldRegionInstance region : WorldRegionService.INSTANCE.nearbyRegions(player, safeQuery.radius())) {
            if (!region.discovered()
                    || !safeQuery.intersectsCircle(region.dimension(), region.center().getX() + 0.5D,
                            region.center().getZ() + 0.5D, region.radius())) {
                continue;
            }
            zones.add(regionZone(region));
        }
        WorldHazardSnapshot hazard = WorldRegionService.INSTANCE.hazardSnapshot(player);
        if (!hazard.safeZone()) {
            HoloMapZoneData zone = hazardZone(player, hazard);
            if (safeQuery.intersectsCircle(zone.dimension(), zone.x(), zone.z(), zone.radius())) {
                zones.add(zone);
            }
        }
        return List.copyOf(zones);
    }

    private static HoloMapZoneData regionZone(WorldRegionInstance region) {
        int color = colorForRegion(region.type());
        int fillAlpha = region.discovered() ? 0x30 : 0x20;
        int outlineAlpha = region.discovered() ? 0xAA : 0x66;
        return new HoloMapZoneData(
                id("zone/region/" + region.id().getNamespace() + "/" + sanitize(region.id().getPath())),
                layerForRegion(region.type()),
                PROVIDER_ID,
                HoloMapZoneShape.CIRCLE,
                patternForRegion(region.type()),
                region.discovered() ? IMapMarker.MarkerState.DISCOVERED : IMapMarker.MarkerState.LOCKED,
                region.discovered() ? region.displayName() : "Undiscovered Region",
                regionSummary(region),
                region.dimension(),
                region.center().getX() + 0.5D,
                region.center().getY(),
                region.center().getZ() + 0.5D,
                region.radius(),
                region.radius() * 2.0F,
                region.radius() * 2.0F,
                withAlpha(color, fillAlpha),
                withAlpha(color, outlineAlpha),
                region.discovered() ? HoloMapPrecision.PRECISE : HoloMapPrecision.ESTIMATED,
                priorityForRegion(region.type()),
                List.of());
    }

    private static HoloMapZoneData hazardZone(Player player, WorldHazardSnapshot hazard) {
        BlockPos pos = player.blockPosition();
        float radius = Math.max(48.0F, hazard.severity() * 2.0F);
        return new HoloMapZoneData(
                id("zone/hazard/world_snapshot"),
                HAZARDS,
                PROVIDER_ID,
                HoloMapZoneShape.CIRCLE,
                HoloMapZonePattern.HAZARD_STRIPES,
                IMapMarker.MarkerState.DISCOVERED,
                "World Hazard Overlay",
                hazard.summary(),
                player.level().dimension(),
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                radius,
                radius * 2.0F,
                radius * 2.0F,
                withAlpha(0xFFFF5C7A, 0x2C),
                withAlpha(0xFFFF5C7A, 0xB8),
                HoloMapPrecision.ESTIMATED,
                100,
                List.of());
    }

    private static Identifier layerForRegion(WorldRegionType type) {
        return switch (type == null ? WorldRegionType.ANOMALY_ZONE : type) {
            case CONVOY_ROUTE -> holomapLayer("routes");
            case SECURE_OUTPOST -> holomapLayer("bases_outposts");
            case ORBITAL_DEBRIS_FIELD -> holomapLayer("orbital_scans");
            case NEXUS_SCAR, ANOMALY_ZONE -> holomapLayer("nexus_anomaly");
            case CRASH_ZONE -> holomapLayer("crash_sites");
            default -> HAZARDS;
        };
    }

    private static HoloMapZonePattern patternForRegion(WorldRegionType type) {
        return switch (type == null ? WorldRegionType.ANOMALY_ZONE : type) {
            case TOXIC_SWAMP, RADIATION_ZONE, CRYOGENIC_RUINS -> HoloMapZonePattern.HAZARD_STRIPES;
            case ORBITAL_DEBRIS_FIELD -> HoloMapZonePattern.SCAN_GRID;
            case NEXUS_SCAR, ANOMALY_ZONE -> HoloMapZonePattern.ANOMALY_NOISE;
            case CONVOY_ROUTE -> HoloMapZonePattern.ROUTE_BANDS;
            default -> HoloMapZonePattern.SOLID;
        };
    }

    private static int priorityForRegion(WorldRegionType type) {
        return switch (type == null ? WorldRegionType.ANOMALY_ZONE : type) {
            case TOXIC_SWAMP, RADIATION_ZONE, CRYOGENIC_RUINS -> 92;
            case NEXUS_SCAR, ANOMALY_ZONE -> 84;
            case CONVOY_ROUTE -> 78;
            case ORBITAL_DEBRIS_FIELD -> 72;
            case SECURE_OUTPOST -> 68;
            case CRASH_ZONE -> 64;
            case RUINED_CITY -> 58;
        };
    }

    private static int colorForRegion(WorldRegionType type) {
        return switch (type == null ? WorldRegionType.ANOMALY_ZONE : type) {
            case CONVOY_ROUTE -> 0xFF92F7A6;
            case SECURE_OUTPOST -> 0xFFFFD166;
            case ORBITAL_DEBRIS_FIELD -> 0xFFA58BFF;
            case NEXUS_SCAR, ANOMALY_ZONE -> 0xFFFF8FEA;
            case CRASH_ZONE -> 0xFFFFA05B;
            case RUINED_CITY -> 0xFF80F0A0;
            case TOXIC_SWAMP, RADIATION_ZONE, CRYOGENIC_RUINS -> 0xFFFF5C7A;
        };
    }

    private static String regionSummary(WorldRegionInstance region) {
        String type = readable(region.type().name());
        if (region.hazardIds().isEmpty()) {
            return type + " zone / no active hazard references.";
        }
        return type + " zone / hazards " + region.hazardIds();
    }

    private static String readable(String value) {
        String clean = value == null ? "region" : value.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String part : clean.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? "Region" : builder.toString();
    }

    private static Identifier holomapLayer(String path) {
        return Identifier.fromNamespaceAndPath("echoholomap", "layer/" + sanitize(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, sanitize(path));
    }

    private static String sanitize(String value) {
        String clean = value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
        clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }

    private static int withAlpha(int color, int alpha) {
        return ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
}
