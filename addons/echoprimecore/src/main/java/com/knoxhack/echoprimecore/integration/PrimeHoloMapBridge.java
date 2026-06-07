package com.knoxhack.echoprimecore.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.EchoMapMarker;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echocore.api.WorldMarker;
import com.knoxhack.echocore.api.WorldMarkerType;
import com.knoxhack.echocore.api.prime.PrimeHoloMapRegistry;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.PrimeIds;
import com.knoxhack.echoprimecore.progression.PrimePlayerData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum PrimeHoloMapBridge implements IMapDataProvider {
    INSTANCE;

    private static PrimeIntegrationRegistry registry;

    public static void register(PrimeIntegrationRegistry source) {
        registry = source;
        EchoCoreServices.registerMapDataProvider(INSTANCE);
    }

    @Override
    public Identifier providerId() {
        return EchoPrimeCore.id("provider/prime_holomap");
    }

    @Override
    public List<IMapLayer> layers(Player player) {
        PrimeIntegrationRegistry safeRegistry = registry == null ? PrimeIntegrationLoader.registry() : registry;
        return safeRegistry.layers().stream()
                .map(PrimeHoloMapBridge::layer)
                .map(IMapLayer.class::cast)
                .toList();
    }

    @Override
    public List<IMapMarker> markers(Player player) {
        if (player == null) {
            return List.of();
        }
        List<IMapMarker> markers = new ArrayList<>();
        for (WorldMarker marker : EchoCoreServices.worldMarkerService().markers(player)) {
            if (isPrimeWorldMarker(marker)) {
                markers.add(worldMarker(marker));
            }
        }
        PrimePlayerData data = PrimePlayerData.get(player);
        if (!data.starterRelayPlaced() || !data.hasFlag(EchoPrimeCore.id("holomap_online"))) {
            return List.copyOf(markers);
        }
        var pos = data.relayPos();
        markers.add(new EchoMapMarker(
                EchoPrimeCore.id("marker/player_relay_post/" + player.getUUID().toString().replace("-", "")),
                PrimeIds.MAP_LAYER_RUINS,
                PrimeIds.MARKER_RELAY_RUIN,
                IMapMarker.MarkerKind.MISSION,
                data.hasFlag(EchoPrimeCore.id("first_ruin")) ? IMapMarker.MarkerState.CHECKED : IMapMarker.MarkerState.DISCOVERED,
                "Abandoned Relay Post",
                "Weak Prime signal source. Loot the cache for Relay Fragment and Circuit Plate.",
                player.level().dimension(),
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                18.0F,
                EchoPrimeCore.id("textures/gui/markers/relay_ruin.png"),
                PrimeIds.ROUTE_SURVIVAL,
                0,
                true));
        return List.copyOf(markers);
    }

    private static EchoMapLayer layer(PrimeHoloMapRegistry.PrimeMapLayer layer) {
        return new EchoMapLayer(layer.id(), layer.title(), layer.order(), layer.color(), layer.visibleByDefault());
    }

    private static boolean isPrimeWorldMarker(WorldMarker marker) {
        if (marker == null) {
            return false;
        }
        return isPrimeId(marker.id()) || isPrimeId(marker.regionId());
    }

    private static boolean isPrimeId(Identifier id) {
        return id != null && id.getNamespace().equals(EchoPrimeCore.MODID);
    }

    private static EchoMapMarker worldMarker(WorldMarker marker) {
        Identifier markerType = markerType(marker);
        Identifier layer = layerFor(markerType);
        return new EchoMapMarker(
                EchoPrimeCore.id("marker/worldcore/" + marker.id().getPath().replace('/', '_')),
                layer,
                markerType,
                kind(marker.type()),
                marker.discovered() ? IMapMarker.MarkerState.DISCOVERED : IMapMarker.MarkerState.LOCKED,
                marker.displayName(),
                marker.summary(),
                marker.dimension(),
                marker.pos().getX() + 0.5D,
                marker.pos().getY(),
                marker.pos().getZ() + 0.5D,
                marker.radius(),
                EchoPrimeCore.id("textures/gui/markers/" + markerType.getPath().substring("marker/".length()) + ".png"),
                PrimeIds.ROUTE_SURVIVAL,
                0,
                true);
    }

    private static Identifier markerType(WorldMarker marker) {
        String path = marker.regionId() == null ? marker.id().getPath() : marker.regionId().getPath();
        if (path.contains("data_vault")) {
            return EchoPrimeCore.id("marker/data_vault");
        }
        if (path.contains("blackbox")) {
            return EchoPrimeCore.id("marker/blackbox_location");
        }
        if (path.contains("convoy")) {
            return EchoPrimeCore.id("marker/convoy_wreck");
        }
        if (path.contains("power") || path.contains("cable")) {
            return EchoPrimeCore.id("marker/power_node");
        }
        if (path.contains("base")) {
            return EchoPrimeCore.id("marker/base_anchor");
        }
        if (path.contains("arcana") || path.contains("aether")) {
            return EchoPrimeCore.id("marker/arcana_rift");
        }
        if (path.contains("relic")) {
            return EchoPrimeCore.id("marker/relic_vault");
        }
        if (path.contains("orbital")) {
            return EchoPrimeCore.id("marker/orbital_signal");
        }
        if (path.contains("nexus")) {
            return EchoPrimeCore.id("marker/nexus_trace");
        }
        if (path.contains("death") || path.contains("grave")) {
            return EchoPrimeCore.id("marker/death_marker");
        }
        if (path.contains("signal")) {
            return EchoPrimeCore.id("marker/signal_source");
        }
        return PrimeIds.MARKER_RELAY_RUIN;
    }

    private static Identifier layerFor(Identifier markerType) {
        String path = markerType.getPath();
        if (path.endsWith("signal_source") || path.endsWith("orbital_signal")) {
            return PrimeIds.MAP_LAYER_SIGNALS;
        }
        if (path.endsWith("data_vault") || path.endsWith("blackbox_location") || path.endsWith("relic_vault")) {
            return EchoPrimeCore.id("layer/data_vaults");
        }
        if (path.endsWith("convoy_wreck")) {
            return EchoPrimeCore.id("layer/convoy_wrecks");
        }
        if (path.endsWith("power_node")) {
            return EchoPrimeCore.id("layer/powergrid_nodes");
        }
        if (path.endsWith("base_anchor")) {
            return EchoPrimeCore.id("layer/basegrid_locations");
        }
        if (path.endsWith("arcana_rift")) {
            return EchoPrimeCore.id("layer/arcana_rifts");
        }
        if (path.endsWith("nexus_trace")) {
            return EchoPrimeCore.id("layer/nexus_traces");
        }
        if (path.endsWith("death_marker")) {
            return EchoPrimeCore.id("layer/death_recovery");
        }
        return PrimeIds.MAP_LAYER_RUINS;
    }

    private static IMapMarker.MarkerKind kind(WorldMarkerType type) {
        return switch (type) {
            case ROUTE_START, ROUTE_CHECKPOINT, ROUTE_DESTINATION -> IMapMarker.MarkerKind.ROUTE;
            case HAZARD -> IMapMarker.MarkerKind.HAZARD;
            case OUTPOST -> IMapMarker.MarkerKind.BASE_OUTPOST;
            case ORBITAL_DEBRIS -> IMapMarker.MarkerKind.ORBITAL_SCAN;
            case ANOMALY -> IMapMarker.MarkerKind.NEXUS_ANOMALY;
            case REGION_CENTER -> IMapMarker.MarkerKind.REGION;
            case CRASH_SITE -> IMapMarker.MarkerKind.CRASH_SITE;
            case STRUCTURE -> IMapMarker.MarkerKind.GENERIC;
        };
    }
}
