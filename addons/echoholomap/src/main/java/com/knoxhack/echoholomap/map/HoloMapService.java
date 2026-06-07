package com.knoxhack.echoholomap.map;

import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echocore.api.IMapMarkerService;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.api.HoloMapLayerData;
import com.knoxhack.echoholomap.api.HoloMapMarkerData;
import com.knoxhack.echoholomap.api.HoloMapOverlayData;
import com.knoxhack.echoholomap.api.HoloMapProviderDiagnostic;
import com.knoxhack.echoholomap.api.HoloMapQuery;
import com.knoxhack.echoholomap.api.HoloMapRouteData;
import com.knoxhack.echoholomap.api.HoloMapZoneData;
import com.knoxhack.echoholomap.api.IHoloMapDataProvider;
import com.knoxhack.echoholomap.integration.runtimeguard.HoloMapRuntimeGuardHooks;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class HoloMapService implements IMapMarkerService {
    public static final HoloMapService INSTANCE = new HoloMapService();

    private final List<IMapDataProvider> coreProviders = new CopyOnWriteArrayList<>();
    private final List<IHoloMapDataProvider> holoProviders = new CopyOnWriteArrayList<>();
    private final Map<String, ProviderHealth> health = new LinkedHashMap<>();

    private HoloMapService() {
    }

    public void registerBuiltins() {
        registerProvider(BuiltinMapDataProvider.INSTANCE);
    }

    @Override
    public boolean registerProvider(IMapDataProvider provider) {
        if (provider == null) {
            return false;
        }
        Identifier providerId = safeProviderId(provider);
        if (providerId == null) {
            return false;
        }
        if (containsProviderId(providerId)) {
            return false;
        }
        coreProviders.add(provider);
        health(providerId, "core");
        return true;
    }

    public boolean registerHoloProvider(IHoloMapDataProvider provider) {
        if (provider == null) {
            return false;
        }
        Identifier providerId = safeProviderId(provider);
        if (providerId == null) {
            return false;
        }
        if (containsProviderId(providerId)) {
            return false;
        }
        holoProviders.add(provider);
        health(providerId, "holomap");
        return true;
    }

    @Override
    public List<IMapLayer> layers(Player player) {
        return richLayers(player).stream()
                .map(HoloMapLayerData::toCore)
                .toList();
    }

    public List<HoloMapLayerData> richLayers(Player player) {
        Map<Identifier, HoloMapLayerData> layers = new LinkedHashMap<>();
        for (IMapLayer layer : HoloMapLayers.REQUIRED) {
            layers.put(layer.id(), HoloMapLayerData.from(layer));
        }
        for (IMapDataProvider provider : coreProviders) {
            for (IMapLayer layer : safeCoreLayers(provider, player)) {
                if (layer == null || layer.id() == null) {
                    continue;
                }
                layers.putIfAbsent(layer.id(), HoloMapLayerData.from(layer));
            }
        }
        for (IHoloMapDataProvider provider : holoProviders) {
            for (HoloMapLayerData layer : safeHoloLayers(provider, player)) {
                if (layer == null || layer.id() == null) {
                    continue;
                }
                layers.putIfAbsent(layer.id(), layer);
            }
        }
        return layers.values().stream()
                .sorted(Comparator.comparingInt(HoloMapLayerData::sortOrder)
                        .thenComparing(layer -> layer.id().toString()))
                .toList();
    }

    @Override
    public List<IMapMarker> markers(Player player) {
        return richMarkers(player).stream()
                .map(HoloMapMarkerData::toCore)
                .toList();
    }

    public List<HoloMapMarkerData> richMarkers(Player player) {
        return richMarkers(player, null);
    }

    public List<HoloMapMarkerData> richMarkers(Player player, HoloMapQuery query) {
        Map<Identifier, HoloMapMarkerData> markers = new LinkedHashMap<>();
        for (IMapDataProvider provider : coreProviders) {
            for (IMapMarker marker : safeCoreMarkers(provider, player)) {
                if (marker == null || marker.id() == null || marker.layerId() == null) {
                    continue;
                }
                HoloMapMarkerData richMarker = HoloMapMarkerData.fromCore(marker);
                HoloMapMarkerData existing = markers.putIfAbsent(richMarker.id(), richMarker);
                if (existing != null && !existing.equals(richMarker)) {
                    EchoHoloMap.LOGGER.debug("Duplicate HoloMap marker id {} from provider {} ignored.",
                            marker.id(), safeProviderId(provider));
                }
            }
        }
        for (IHoloMapDataProvider provider : holoProviders) {
            for (HoloMapMarkerData marker : safeHoloMarkers(provider, player, query)) {
                if (marker == null || marker.id() == null || marker.layerId() == null) {
                    continue;
                }
                HoloMapMarkerData existing = markers.putIfAbsent(marker.id(), marker);
                if (existing != null && !existing.equals(marker)) {
                    EchoHoloMap.LOGGER.debug("Duplicate rich HoloMap marker id {} from provider {} ignored.",
                            marker.id(), safeProviderId(provider));
                }
            }
        }
        return markers.values().stream()
                .sorted(Comparator.comparingInt(HoloMapMarkerData::priority).reversed()
                        .thenComparing(marker -> marker.layerId().toString())
                        .thenComparing(marker -> marker.state().ordinal())
                        .thenComparing(HoloMapMarkerData::title)
                        .thenComparing(marker -> marker.id().toString()))
                .toList();
    }

    public List<HoloMapRouteData> richRoutes(Player player) {
        return richRoutes(player, null);
    }

    public List<HoloMapRouteData> richRoutes(Player player, HoloMapQuery query) {
        Map<Identifier, HoloMapRouteData> routes = new LinkedHashMap<>();
        for (IHoloMapDataProvider provider : holoProviders) {
            for (HoloMapRouteData route : safeHoloRoutes(provider, player, query)) {
                if (route != null && route.id() != null) {
                    routes.putIfAbsent(route.id(), route);
                }
            }
        }
        return routes.values().stream()
                .sorted(Comparator.comparing(route -> route.id().toString()))
                .toList();
    }

    public List<HoloMapOverlayData> richOverlays(Player player) {
        return richOverlays(player, null);
    }

    public List<HoloMapOverlayData> richOverlays(Player player, HoloMapQuery query) {
        Map<Identifier, HoloMapOverlayData> overlays = new LinkedHashMap<>();
        for (IHoloMapDataProvider provider : holoProviders) {
            for (HoloMapOverlayData overlay : safeHoloOverlays(provider, player, query)) {
                if (overlay != null && overlay.id() != null) {
                    overlays.putIfAbsent(overlay.id(), overlay);
                }
            }
        }
        return overlays.values().stream()
                .sorted(Comparator.comparing(overlay -> overlay.id().toString()))
                .toList();
    }

    public List<HoloMapZoneData> richZones(Player player) {
        return richZones(player, null);
    }

    public List<HoloMapZoneData> richZones(Player player, HoloMapQuery query) {
        Map<Identifier, HoloMapZoneData> zones = new LinkedHashMap<>();
        for (IHoloMapDataProvider provider : holoProviders) {
            for (HoloMapZoneData zone : safeHoloZones(provider, player, query)) {
                if (zone != null && zone.id() != null && zone.layerId() != null) {
                    zones.putIfAbsent(zone.id(), zone);
                }
            }
        }
        return zones.values().stream()
                .sorted(Comparator.comparingInt(HoloMapZoneData::priority).reversed()
                        .thenComparing(zone -> zone.id().toString()))
                .toList();
    }

    @Override
    public boolean refresh(ServerPlayer player, String reason) {
        if (!HoloMapRuntimeGuardHooks.shouldRefreshMarkers(player, reason)) {
            return false;
        }
        boolean refreshed = false;
        for (IMapDataProvider provider : coreProviders) {
            try {
                refreshed |= provider.refresh(player, reason == null ? "" : reason);
                recordSuccess(safeProviderId(provider), "core", -1, -1, -1, -1);
            } catch (RuntimeException exception) {
                recordFailure(safeProviderId(provider), "core", exception);
                EchoHoloMap.LOGGER.warn("HoloMap provider {} failed while refreshing.",
                        safeProviderId(provider), exception);
            }
        }
        for (IHoloMapDataProvider provider : holoProviders) {
            try {
                refreshed |= provider.refresh(player, reason == null ? "" : reason);
                recordSuccess(safeProviderId(provider), "holomap", -1, -1, -1, -1);
            } catch (RuntimeException exception) {
                recordFailure(safeProviderId(provider), "holomap", exception);
                EchoHoloMap.LOGGER.warn("Rich HoloMap provider {} failed while refreshing.",
                        safeProviderId(provider), exception);
            }
        }
        return refreshed;
    }

    @Override
    public int providerCount() {
        return coreProviders.size() + holoProviders.size();
    }

    public int coreProviderCount() {
        return coreProviders.size();
    }

    public int richProviderCount() {
        return holoProviders.size();
    }

    public List<HoloMapProviderDiagnostic> diagnostics(Player player) {
        for (IMapDataProvider provider : coreProviders) {
            Identifier providerId = safeProviderId(provider);
            if (providerId != null) {
                health(providerId, "core");
            }
        }
        for (IHoloMapDataProvider provider : holoProviders) {
            Identifier providerId = safeProviderId(provider);
            if (providerId != null) {
                health(providerId, "holomap");
            }
        }
        return health.values().stream()
                .map(ProviderHealth::toDiagnostic)
                .limit(64)
                .toList();
    }

    public void clearForTests() {
        coreProviders.clear();
        holoProviders.clear();
        health.clear();
    }

    private boolean containsProviderId(Identifier providerId) {
        for (IMapDataProvider existing : coreProviders) {
            if (providerId.equals(safeProviderId(existing))) {
                return true;
            }
        }
        for (IHoloMapDataProvider existing : holoProviders) {
            if (providerId.equals(safeProviderId(existing))) {
                return true;
            }
        }
        return false;
    }

    private List<IMapLayer> safeCoreLayers(IMapDataProvider provider, Player player) {
        Identifier providerId = safeProviderId(provider);
        try {
            List<IMapLayer> layers = provider.layers(player);
            List<IMapLayer> safe = layers == null ? List.of() : layers;
            recordSuccess(providerId, "core", safe.size(), -1, -1, -1);
            return safe;
        } catch (RuntimeException exception) {
            recordFailure(providerId, "core", exception);
            EchoHoloMap.LOGGER.warn("HoloMap provider {} failed while listing layers.",
                    providerId, exception);
            return List.of();
        }
    }

    private List<IMapMarker> safeCoreMarkers(IMapDataProvider provider, Player player) {
        Identifier providerId = safeProviderId(provider);
        try {
            List<IMapMarker> markers = provider.markers(player);
            List<IMapMarker> safe = markers == null ? List.of() : markers;
            recordSuccess(providerId, "core", -1, safe.size(), -1, -1);
            return safe;
        } catch (RuntimeException exception) {
            recordFailure(providerId, "core", exception);
            EchoHoloMap.LOGGER.warn("HoloMap provider {} failed while listing markers.",
                    providerId, exception);
            return List.of();
        }
    }

    private List<HoloMapLayerData> safeHoloLayers(IHoloMapDataProvider provider, Player player) {
        Identifier providerId = safeProviderId(provider);
        try {
            List<HoloMapLayerData> layers = provider.layers(player);
            List<HoloMapLayerData> safe = layers == null ? List.of() : layers;
            recordSuccess(providerId, "holomap", safe.size(), -1, -1, -1);
            return safe;
        } catch (RuntimeException exception) {
            recordFailure(providerId, "holomap", exception);
            EchoHoloMap.LOGGER.warn("Rich HoloMap provider {} failed while listing layers.",
                    providerId, exception);
            return List.of();
        }
    }

    private List<HoloMapMarkerData> safeHoloMarkers(IHoloMapDataProvider provider, Player player,
            HoloMapQuery query) {
        Identifier providerId = safeProviderId(provider);
        try {
            List<HoloMapMarkerData> markers = query == null ? provider.markers(player) : provider.markers(player, query);
            List<HoloMapMarkerData> safe = markers == null ? List.of() : markers;
            recordSuccess(providerId, "holomap", -1, safe.size(), -1, -1);
            return safe;
        } catch (RuntimeException exception) {
            recordFailure(providerId, "holomap", exception);
            EchoHoloMap.LOGGER.warn("Rich HoloMap provider {} failed while listing markers.",
                    providerId, exception);
            return List.of();
        }
    }

    private List<HoloMapRouteData> safeHoloRoutes(IHoloMapDataProvider provider, Player player,
            HoloMapQuery query) {
        Identifier providerId = safeProviderId(provider);
        try {
            List<HoloMapRouteData> routes = query == null ? provider.routes(player) : provider.routes(player, query);
            List<HoloMapRouteData> safe = routes == null ? List.of() : routes;
            recordSuccess(providerId, "holomap", -1, -1, safe.size(), -1);
            return safe;
        } catch (RuntimeException exception) {
            recordFailure(providerId, "holomap", exception);
            EchoHoloMap.LOGGER.warn("Rich HoloMap provider {} failed while listing routes.",
                    providerId, exception);
            return List.of();
        }
    }

    private List<HoloMapOverlayData> safeHoloOverlays(IHoloMapDataProvider provider, Player player,
            HoloMapQuery query) {
        Identifier providerId = safeProviderId(provider);
        try {
            List<HoloMapOverlayData> overlays = query == null ? provider.overlays(player) : provider.overlays(player, query);
            List<HoloMapOverlayData> safe = overlays == null ? List.of() : overlays;
            recordSuccess(providerId, "holomap", -1, -1, -1, safe.size());
            return safe;
        } catch (RuntimeException exception) {
            recordFailure(providerId, "holomap", exception);
            EchoHoloMap.LOGGER.warn("Rich HoloMap provider {} failed while listing overlays.",
                    providerId, exception);
            return List.of();
        }
    }

    private List<HoloMapZoneData> safeHoloZones(IHoloMapDataProvider provider, Player player,
            HoloMapQuery query) {
        Identifier providerId = safeProviderId(provider);
        try {
            List<HoloMapZoneData> zones = query == null ? provider.zones(player) : provider.zones(player, query);
            return zones == null ? List.of() : zones;
        } catch (RuntimeException exception) {
            recordFailure(providerId, "holomap", exception);
            EchoHoloMap.LOGGER.warn("Rich HoloMap provider {} failed while listing zones.",
                    providerId, exception);
            return List.of();
        }
    }

    private static Identifier safeProviderId(IMapDataProvider provider) {
        try {
            return provider == null ? null : provider.providerId();
        } catch (RuntimeException exception) {
            EchoHoloMap.LOGGER.warn("HoloMap provider id lookup failed.", exception);
            return null;
        }
    }

    private static Identifier safeProviderId(IHoloMapDataProvider provider) {
        try {
            return provider == null ? null : provider.providerId();
        } catch (RuntimeException exception) {
            EchoHoloMap.LOGGER.warn("Rich HoloMap provider id lookup failed.", exception);
            return null;
        }
    }

    private void recordSuccess(Identifier providerId, String providerType,
            int layers, int markers, int routes, int overlays) {
        if (providerId == null) {
            return;
        }
        health(providerId, providerType).success(layers, markers, routes, overlays);
    }

    private void recordFailure(Identifier providerId, String providerType, RuntimeException exception) {
        if (providerId == null) {
            return;
        }
        health(providerId, providerType).failure(exception);
    }

    private ProviderHealth health(Identifier providerId, String providerType) {
        String key = providerType + ":" + providerId;
        synchronized (health) {
            return health.computeIfAbsent(key, ignored -> new ProviderHealth(providerId, providerType));
        }
    }

    private static final class ProviderHealth {
        private final Identifier providerId;
        private final String providerType;
        private boolean healthy = true;
        private int layers;
        private int markers;
        private int routes;
        private int overlays;
        private String message = "registered";
        private long failures;

        private ProviderHealth(Identifier providerId, String providerType) {
            this.providerId = providerId;
            this.providerType = providerType;
        }

        private void success(int layers, int markers, int routes, int overlays) {
            healthy = true;
            message = "ok";
            if (layers >= 0) {
                this.layers = layers;
            }
            if (markers >= 0) {
                this.markers = markers;
            }
            if (routes >= 0) {
                this.routes = routes;
            }
            if (overlays >= 0) {
                this.overlays = overlays;
            }
        }

        private void failure(RuntimeException exception) {
            healthy = false;
            failures++;
            message = exception == null || exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "provider failed"
                    : exception.getMessage();
        }

        private HoloMapProviderDiagnostic toDiagnostic() {
            return new HoloMapProviderDiagnostic(providerId, providerType, healthy,
                    layers, markers, routes, overlays, message, failures);
        }
    }
}
