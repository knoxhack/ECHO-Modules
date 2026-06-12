package com.knoxhack.echoholomap.map;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiscoveryCategory;
import com.echoplatform.echocore.api.EchoDiscoveryEntry;
import com.echoplatform.echocore.api.EchoDiscoveryState;
import com.echoplatform.echocore.api.EchoHazardTelemetry;
import com.echoplatform.echocore.api.EchoMapMarker;
import com.echoplatform.echocore.api.EchoRouteRecord;
import com.echoplatform.echocore.api.IMapDataProvider;
import com.echoplatform.echocore.api.IMapLayer;
import com.echoplatform.echocore.api.IMapMarker;
import com.echoplatform.echocore.api.WorldHazardSnapshot;
import com.echoplatform.echocore.api.WorldMarker;
import com.echoplatform.echocore.api.WorldMarkerType;
import com.echoplatform.echocore.api.WorldRegionInstance;
import com.echoplatform.echocore.api.WorldRegionType;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.HoloMapIds;
import com.knoxhack.echoholomap.platform.HoloMapModuleAccess;
import com.knoxhack.echoholomap.world.HoloMapSavedData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class BuiltinMapDataProvider implements IMapDataProvider {
    public static final BuiltinMapDataProvider INSTANCE = new BuiltinMapDataProvider();

    private BuiltinMapDataProvider() {
    }

    @Override
    public Identifier providerId() {
        return HoloMapIds.CORE_SOURCE;
    }

    @Override
    public List<IMapLayer> layers(Player player) {
        return HoloMapLayers.REQUIRED;
    }

    @Override
    public List<IMapMarker> markers(Player player) {
        List<IMapMarker> markers = new ArrayList<>();
        markers.addAll(discoveryMarkers(player));
        markers.addAll(routeMarkers(player));
        if (!worldCoreProviderPresent()) {
            markers.addAll(worldMarkers(player));
            markers.addAll(regionMarkers(player));
        }
        markers.addAll(hazardMarkers(player));
        markers.addAll(debugMarkers(player));
        return markers;
    }

    @Override
    public boolean refresh(ServerPlayer player, String reason) {
        if (player != null) {
            EchoCoreServices.discoverVisibleRouteRecords(player);
        }
        return false;
    }

    private static List<IMapMarker> discoveryMarkers(Player player) {
        List<IMapMarker> markers = new ArrayList<>();
        int index = 0;
        for (EchoDiscoveryEntry entry : EchoCoreServices.discoveryEntries(player)) {
            EchoDiscoveryState discoveryState = EchoCoreServices.discoveryState(player, entry);
            IMapMarker.MarkerState state = state(discoveryState);
            if (state == IMapMarker.MarkerState.HIDDEN) {
                continue;
            }
            Identifier layerId = layerForDiscovery(entry);
            Identifier markerId = HoloMapIds.id("discovery/" + entry.id().getNamespace() + "/" + entry.id().getPath());
            markers.add(new EchoMapMarker(
                    markerId,
                    layerId,
                    HoloMapIds.DISCOVERY_SOURCE,
                    kindForLayer(layerId),
                    state,
                    state == IMapMarker.MarkerState.LOCKED ? entry.lockedHintTitle() : entry.revealedTitle(),
                    state == IMapMarker.MarkerState.LOCKED ? entry.hintText() : entry.revealedSummary(),
                    Level.OVERWORLD,
                    virtualCoordinate(entry.id(), 17 + index),
                    64,
                    virtualCoordinate(entry.id(), 71 + index),
                    36.0F,
                    entry.iconArt(),
                    null,
                    -1,
                    false));
            index++;
        }
        return markers;
    }

    private static List<IMapMarker> routeMarkers(Player player) {
        List<IMapMarker> markers = new ArrayList<>();
        int index = 0;
        for (EchoRouteRecord record : EchoCoreServices.routeRecords(player)) {
            IMapMarker.MarkerState state = routeState(record);
            Identifier routeMarkerId = HoloMapIds.id("route/" + record.id().getNamespace() + "/" + record.id().getPath());
            Identifier dimension = Identifier.tryParse(record.dimensionHint());
            markers.add(new EchoMapMarker(
                    routeMarkerId,
                    HoloMapIds.ROUTES,
                    HoloMapIds.ROUTE_SOURCE,
                    IMapMarker.MarkerKind.ROUTE,
                    state,
                    record.title(),
                    record.status() + " / " + record.summary(),
                    dimension == null ? Level.OVERWORLD : net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION, dimension),
                    virtualCoordinate(record.id(), 103 + index),
                    64,
                    virtualCoordinate(record.id(), 197 + index),
                    28.0F,
                    null,
                    record.id(),
                    index,
                    false));
            index++;
        }
        return markers;
    }

    private static List<IMapMarker> worldMarkers(Player player) {
        if (player == null) {
            return List.of();
        }
        List<IMapMarker> markers = new ArrayList<>();
        for (WorldMarker marker : EchoCoreServices.worldMarkerService().nearbyMarkers(
                player.level(), player.blockPosition(), Config.mapInterestRadiusBlocks())) {
            if (!marker.discovered()) {
                continue;
            }
            Identifier layerId = layerForWorldMarker(marker.type());
            markers.add(new EchoMapMarker(
                    HoloMapIds.id("world_marker/" + marker.id().getNamespace() + "/" + marker.id().getPath()),
                    layerId,
                    HoloMapIds.WORLD_SOURCE,
                    kindForWorldMarker(marker.type()),
                    marker.discovered() ? IMapMarker.MarkerState.DISCOVERED : IMapMarker.MarkerState.LOCKED,
                    marker.discovered() ? marker.displayName() : lockedTitle(marker.type()),
                    marker.discovered() ? markerSummary(marker) : lockedSummary(marker.type()),
                    marker.dimension(),
                    marker.pos().getX() + 0.5D,
                    marker.pos().getY(),
                    marker.pos().getZ() + 0.5D,
                    marker.radius(),
                    iconForWorldMarker(marker.type()),
                    null,
                    -1,
                    true));
        }
        return markers;
    }

    private static List<IMapMarker> regionMarkers(Player player) {
        if (player == null) {
            return List.of();
        }
        List<IMapMarker> markers = new ArrayList<>();
        for (WorldRegionInstance region : EchoCoreServices.worldRegions().activeRegions(player)) {
            if (!region.discovered()) {
                continue;
            }
            Identifier layerId = layerForRegion(region.type());
            markers.add(new EchoMapMarker(
                    HoloMapIds.id("region/" + region.id().getNamespace() + "/" + region.id().getPath()),
                    layerId,
                    HoloMapIds.WORLD_SOURCE,
                    kindForRegion(region.type()),
                    region.discovered() ? IMapMarker.MarkerState.DISCOVERED : IMapMarker.MarkerState.LOCKED,
                    region.discovered() ? region.displayName() : "Undiscovered Region",
                    regionSummary(region),
                    region.dimension(),
                    region.center().getX() + 0.5D,
                    region.center().getY(),
                    region.center().getZ() + 0.5D,
                    region.radius(),
                    iconForRegion(region.type()),
                    null,
                    -1,
                    true));
        }
        return markers;
    }

    private static List<IMapMarker> hazardMarkers(Player player) {
        if (player == null) {
            return List.of();
        }
        List<IMapMarker> markers = new ArrayList<>();
        BlockPos pos = player.blockPosition();
        EchoHazardTelemetry telemetry = EchoCoreServices.hazardTelemetry(player);
        if (telemetry.warning()) {
            int severity = List.of(
                    100 - telemetry.hydration(),
                    telemetry.radiation(),
                    telemetry.toxicAir(),
                    100 - telemetry.oxygen(),
                    100 - telemetry.pressure(),
                    telemetry.cold(),
                    telemetry.heat(),
                    telemetry.exposure()).stream().mapToInt(Integer::intValue).max().orElse(0);
            markers.add(new EchoMapMarker(
                    HoloMapIds.id("hazard/live_vitals"),
                    HoloMapIds.HAZARDS,
                    HoloMapIds.HAZARD_SOURCE,
                    IMapMarker.MarkerKind.HAZARD,
                    IMapMarker.MarkerState.DISCOVERED,
                    "Live Hazard Telemetry",
                    telemetry.statusLine(),
                    player.level().dimension(),
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    Math.max(32.0F, severity * 1.5F),
                    null,
                    null,
                    -1,
                    true));
        }
        if (worldCoreProviderPresent()) {
            return markers;
        }
        WorldHazardSnapshot worldHazard = EchoCoreServices.hazardService().hazardSnapshot(player);
        if (!worldHazard.safeZone()) {
            markers.add(new EchoMapMarker(
                    HoloMapIds.id("hazard/world_snapshot"),
                    HoloMapIds.HAZARDS,
                    HoloMapIds.HAZARD_SOURCE,
                    IMapMarker.MarkerKind.HAZARD,
                    IMapMarker.MarkerState.DISCOVERED,
                    "World Hazard Overlay",
                    worldHazard.summary(),
                    player.level().dimension(),
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    Math.max(48.0F, worldHazard.severity() * 2.0F),
                    null,
                    null,
                    -1,
                    true));
        }
        return markers;
    }

    private static boolean worldCoreProviderPresent() {
        return HoloMapModuleAccess.isLoaded("echoworldcore");
    }

    private static List<IMapMarker> debugMarkers(Player player) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        return List.copyOf(HoloMapSavedData.get(serverLevel).debugMarkers(player.level()));
    }

    private static Identifier layerForDiscovery(EchoDiscoveryEntry entry) {
        EchoDiscoveryCategory category = entry.category();
        String path = entry.id().getPath().toLowerCase(Locale.ROOT);
        if (entry.relatedMissionId() != null || path.contains("mission") || path.contains("objective")) {
            return HoloMapIds.MISSIONS;
        }
        return switch (category) {
            case REGION, BIOME -> HoloMapIds.HAZARDS;
            case GUARDIAN -> HoloMapIds.NEXUS_ANOMALY;
            case FACTION -> HoloMapIds.BASES_OUTPOSTS;
            case EVENT -> path.contains("orbital") ? HoloMapIds.ORBITAL_SCANS : HoloMapIds.HAZARDS;
            case STRUCTURE -> structureLayer(path);
        };
    }

    private static Identifier structureLayer(String path) {
        if (path.contains("outpost") || path.contains("base") || path.contains("camp")
                || path.contains("beacon") || path.contains("depot")) {
            return HoloMapIds.BASES_OUTPOSTS;
        }
        if (path.contains("route") || path.contains("convoy") || path.contains("road")) {
            return HoloMapIds.ROUTES;
        }
        if (path.contains("orbital") || path.contains("station") || path.contains("debris")) {
            return HoloMapIds.ORBITAL_SCANS;
        }
        if (path.contains("nexus") || path.contains("anomaly") || path.contains("scar")) {
            return HoloMapIds.NEXUS_ANOMALY;
        }
        if (path.contains("drone") || path.contains("scan")) {
            return HoloMapIds.DRONES_SCANS;
        }
        return HoloMapIds.CRASH_SITES;
    }

    private static Identifier layerForWorldMarker(WorldMarkerType type) {
        return switch (type == null ? WorldMarkerType.STRUCTURE : type) {
            case CRASH_SITE, STRUCTURE, REGION_CENTER -> HoloMapIds.CRASH_SITES;
            case ROUTE_START, ROUTE_CHECKPOINT, ROUTE_DESTINATION -> HoloMapIds.ROUTES;
            case HAZARD -> HoloMapIds.HAZARDS;
            case ORBITAL_DEBRIS -> HoloMapIds.ORBITAL_SCANS;
            case OUTPOST -> HoloMapIds.BASES_OUTPOSTS;
            case ANOMALY -> HoloMapIds.NEXUS_ANOMALY;
        };
    }

    private static Identifier layerForRegion(WorldRegionType type) {
        return switch (type == null ? WorldRegionType.ANOMALY_ZONE : type) {
            case CONVOY_ROUTE -> HoloMapIds.ROUTES;
            case SECURE_OUTPOST -> HoloMapIds.BASES_OUTPOSTS;
            case ORBITAL_DEBRIS_FIELD -> HoloMapIds.ORBITAL_SCANS;
            case NEXUS_SCAR, ANOMALY_ZONE -> HoloMapIds.NEXUS_ANOMALY;
            case CRASH_ZONE -> HoloMapIds.CRASH_SITES;
            default -> HoloMapIds.HAZARDS;
        };
    }

    private static IMapMarker.MarkerKind kindForWorldMarker(WorldMarkerType type) {
        return switch (type == null ? WorldMarkerType.STRUCTURE : type) {
            case ROUTE_START, ROUTE_CHECKPOINT, ROUTE_DESTINATION -> IMapMarker.MarkerKind.ROUTE;
            case HAZARD -> IMapMarker.MarkerKind.HAZARD;
            case ORBITAL_DEBRIS -> IMapMarker.MarkerKind.ORBITAL_SCAN;
            case OUTPOST -> IMapMarker.MarkerKind.BASE_OUTPOST;
            case ANOMALY -> IMapMarker.MarkerKind.NEXUS_ANOMALY;
            case CRASH_SITE, STRUCTURE, REGION_CENTER -> IMapMarker.MarkerKind.CRASH_SITE;
        };
    }

    private static IMapMarker.MarkerKind kindForRegion(WorldRegionType type) {
        return switch (type == null ? WorldRegionType.ANOMALY_ZONE : type) {
            case CRASH_ZONE -> IMapMarker.MarkerKind.CRASH_SITE;
            case CONVOY_ROUTE -> IMapMarker.MarkerKind.ROUTE;
            case ORBITAL_DEBRIS_FIELD -> IMapMarker.MarkerKind.ORBITAL_SCAN;
            case SECURE_OUTPOST -> IMapMarker.MarkerKind.BASE_OUTPOST;
            case NEXUS_SCAR, ANOMALY_ZONE -> IMapMarker.MarkerKind.NEXUS_ANOMALY;
            case TOXIC_SWAMP, RADIATION_ZONE, CRYOGENIC_RUINS -> IMapMarker.MarkerKind.HAZARD;
            case RUINED_CITY, CUSTOM -> IMapMarker.MarkerKind.REGION;
        };
    }

    private static Identifier iconForWorldMarker(WorldMarkerType type) {
        WorldMarkerType safeType = type == null ? WorldMarkerType.STRUCTURE : type;
        return HoloMapIds.id("icon/world/" + safeType.name().toLowerCase(Locale.ROOT));
    }

    private static Identifier iconForRegion(WorldRegionType type) {
        WorldRegionType safeType = type == null ? WorldRegionType.ANOMALY_ZONE : type;
        return HoloMapIds.id("icon/region/" + safeType.name().toLowerCase(Locale.ROOT));
    }

    private static String markerSummary(WorldMarker marker) {
        String prefix = readable(marker.type().name());
        String summary = marker.summary() == null || marker.summary().isBlank()
                ? "WorldCore marker telemetry."
                : marker.summary();
        if (marker.regionId() != null) {
            return prefix + " / " + marker.regionId() + " / " + summary;
        }
        return prefix + " / " + summary;
    }

    private static String lockedTitle(WorldMarkerType type) {
        return switch (type == null ? WorldMarkerType.STRUCTURE : type) {
            case ROUTE_START, ROUTE_CHECKPOINT, ROUTE_DESTINATION -> "Locked Route Marker";
            case HAZARD -> "Locked Hazard Marker";
            case ORBITAL_DEBRIS -> "Locked Orbital Marker";
            case OUTPOST -> "Locked Outpost Marker";
            case ANOMALY -> "Locked Anomaly Marker";
            case CRASH_SITE -> "Locked Crash Marker";
            case REGION_CENTER, STRUCTURE -> "Unresolved Field Marker";
        };
    }

    private static String lockedSummary(WorldMarkerType type) {
        return switch (type == null ? WorldMarkerType.STRUCTURE : type) {
            case ROUTE_START, ROUTE_CHECKPOINT, ROUTE_DESTINATION ->
                    "Route telemetry is present, but checkpoint details are still locked.";
            case HAZARD -> "Hazard telemetry is present, but exposure details are still locked.";
            case ORBITAL_DEBRIS -> "Orbital telemetry is present, but debris details are still locked.";
            case OUTPOST -> "Outpost telemetry is present, but access details are still locked.";
            case ANOMALY -> "Anomaly telemetry is present, but field details are still locked.";
            case CRASH_SITE -> "Crash-site telemetry is present, but recovery details are still locked.";
            case REGION_CENTER, STRUCTURE ->
                    "Field telemetry has found this marker, but the local record is still locked.";
        };
    }

    private static String regionSummary(WorldRegionInstance region) {
        String type = readable(region.type().name());
        if (region.hazardIds().isEmpty()) {
            return type + " overlay / no active hazard references.";
        }
        return type + " overlay / hazards " + region.hazardIds();
    }

    private static String readable(String value) {
        String clean = value == null ? "marker" : value.toLowerCase(Locale.ROOT).replace('_', ' ');
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
        return builder.isEmpty() ? "Marker" : builder.toString();
    }

    private static IMapMarker.MarkerKind kindForLayer(Identifier layerId) {
        if (HoloMapIds.CRASH_SITES.equals(layerId)) {
            return IMapMarker.MarkerKind.CRASH_SITE;
        }
        if (HoloMapIds.ROUTES.equals(layerId)) {
            return IMapMarker.MarkerKind.ROUTE;
        }
        if (HoloMapIds.HAZARDS.equals(layerId)) {
            return IMapMarker.MarkerKind.HAZARD;
        }
        if (HoloMapIds.MISSIONS.equals(layerId)) {
            return IMapMarker.MarkerKind.MISSION;
        }
        if (HoloMapIds.BASES_OUTPOSTS.equals(layerId)) {
            return IMapMarker.MarkerKind.BASE_OUTPOST;
        }
        if (HoloMapIds.ORBITAL_SCANS.equals(layerId)) {
            return IMapMarker.MarkerKind.ORBITAL_SCAN;
        }
        if (HoloMapIds.NEXUS_ANOMALY.equals(layerId)) {
            return IMapMarker.MarkerKind.NEXUS_ANOMALY;
        }
        if (HoloMapIds.DRONES_SCANS.equals(layerId)) {
            return IMapMarker.MarkerKind.DRONE_SCAN;
        }
        return IMapMarker.MarkerKind.GENERIC;
    }

    private static IMapMarker.MarkerState state(EchoDiscoveryState state) {
        return switch (state == null ? EchoDiscoveryState.LOCKED : state) {
            case LOCKED -> IMapMarker.MarkerState.LOCKED;
            case DISCOVERED -> IMapMarker.MarkerState.DISCOVERED;
            case CHECKED -> IMapMarker.MarkerState.CHECKED;
        };
    }

    private static IMapMarker.MarkerState routeState(EchoRouteRecord record) {
        if (record.complete()) {
            return IMapMarker.MarkerState.CHECKED;
        }
        String status = record.status().toLowerCase(Locale.ROOT);
        if (status.contains("locked") || status.contains("sealed") || status.contains("pending")
                || status.contains("waiting") || status.contains("blocked")) {
            return IMapMarker.MarkerState.LOCKED;
        }
        return IMapMarker.MarkerState.DISCOVERED;
    }

    private static double virtualCoordinate(Identifier id, int salt) {
        int hash = Objects.hash(id == null ? "unknown" : id.toString(), salt);
        double base = Math.floorMod(hash, 2200) - 1100;
        return base * virtualScale();
    }

    private static double virtualScale() {
        try {
            return Config.VIRTUAL_MAP_SCALE.get();
        } catch (RuntimeException exception) {
            return 1.0D;
        }
    }
}
