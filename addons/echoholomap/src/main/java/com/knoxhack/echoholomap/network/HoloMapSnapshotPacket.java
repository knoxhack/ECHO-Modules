package com.knoxhack.echoholomap.network;

import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.api.HoloMapLayerData;
import com.knoxhack.echoholomap.api.HoloMapMarkerData;
import com.knoxhack.echoholomap.api.HoloMapOverlayData;
import com.knoxhack.echoholomap.api.HoloMapOverlayKind;
import com.knoxhack.echoholomap.api.HoloMapPrecision;
import com.knoxhack.echoholomap.api.HoloMapProviderDiagnostic;
import com.knoxhack.echoholomap.api.HoloMapRouteData;
import com.knoxhack.echoholomap.api.HoloMapRoutePoint;
import com.knoxhack.echoholomap.api.HoloMapZoneData;
import com.knoxhack.echoholomap.api.HoloMapZonePattern;
import com.knoxhack.echoholomap.api.HoloMapZonePoint;
import com.knoxhack.echoholomap.api.HoloMapZoneShape;
import com.knoxhack.echoholomap.map.HoloMapSnapshotBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record HoloMapSnapshotPacket(
        List<LayerData> layers,
        List<MarkerData> markers,
        List<RouteData> routes,
        List<OverlayData> overlays,
        List<ZoneData> zones,
        List<ProviderDiagnosticData> diagnostics,
        String statusLine,
        long gameTime) implements CustomPacketPayload {
    public static final int MAX_LAYERS = 32;
    private static final int MAX_MARKERS_PACKET = 2048;
    private static final int MAX_ROUTES_PACKET = 512;
    private static final int MAX_ROUTE_POINTS = 256;
    private static final int MAX_OVERLAYS_PACKET = 1024;
    private static final int MAX_ZONES_PACKET = 1024;
    private static final int MAX_ZONE_POINTS = 192;
    private static final int MAX_DIAGNOSTICS = 64;
    private static final int MAX_TEXT = 240;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "snapshot");
    public static final Type<HoloMapSnapshotPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HoloMapSnapshotPacket> CODEC =
            StreamCodec.of(HoloMapSnapshotPacket::write, HoloMapSnapshotPacket::read);

    public HoloMapSnapshotPacket {
        layers = copyLayers(layers);
        markers = copyMarkers(markers);
        routes = copyRoutes(routes);
        overlays = copyOverlays(overlays);
        zones = copyZones(zones);
        diagnostics = copyDiagnostics(diagnostics);
        statusLine = safe(statusLine, "HoloMap awaiting field sync.");
        gameTime = Math.max(0L, gameTime);
    }

    public static HoloMapSnapshotPacket empty() {
        return new HoloMapSnapshotPacket(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                "HoloMap offline. Press SYNC after Terminal handshake.", 0L);
    }

    public static HoloMapSnapshotPacket from(ServerPlayer player) {
        return HoloMapSnapshotBuilder.from(player);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, HoloMapSnapshotPacket packet) {
        buffer.writeVarInt(packet.layers().size());
        for (LayerData layer : packet.layers()) {
            EchoPayloadCodecs.writeIdentifier(buffer, layer.id());
            buffer.writeUtf(layer.title(), MAX_TEXT);
            buffer.writeVarInt(layer.sortOrder());
            buffer.writeInt(layer.color());
            buffer.writeBoolean(layer.visibleByDefault());
        }
        buffer.writeVarInt(packet.markers().size());
        for (MarkerData marker : packet.markers()) {
            EchoPayloadCodecs.writeIdentifier(buffer, marker.id());
            EchoPayloadCodecs.writeIdentifier(buffer, marker.layerId());
            EchoPayloadCodecs.writeIdentifier(buffer, marker.sourceId());
            buffer.writeEnum(marker.kind());
            buffer.writeEnum(marker.state());
            buffer.writeUtf(marker.title(), MAX_TEXT);
            buffer.writeUtf(marker.summary(), MAX_TEXT);
            buffer.writeUtf(marker.dimension(), EchoPayloadCodecs.ID);
            buffer.writeDouble(marker.x());
            buffer.writeDouble(marker.y());
            buffer.writeDouble(marker.z());
            buffer.writeFloat(marker.radius());
            buffer.writeBoolean(marker.icon() != null);
            if (marker.icon() != null) {
                EchoPayloadCodecs.writeIdentifier(buffer, marker.icon());
            }
            buffer.writeUtf(marker.routeId(), EchoPayloadCodecs.ID);
            buffer.writeVarInt(marker.routeOrder());
            buffer.writeEnum(marker.precision());
            buffer.writeVarInt(marker.priority());
        }
        buffer.writeVarInt(packet.routes().size());
        for (RouteData route : packet.routes()) {
            EchoPayloadCodecs.writeIdentifier(buffer, route.id());
            EchoPayloadCodecs.writeIdentifier(buffer, route.layerId());
            EchoPayloadCodecs.writeIdentifier(buffer, route.sourceId());
            buffer.writeUtf(route.title(), MAX_TEXT);
            buffer.writeUtf(route.summary(), MAX_TEXT);
            buffer.writeUtf(route.dimension(), EchoPayloadCodecs.ID);
            buffer.writeInt(route.color());
            buffer.writeEnum(route.state());
            buffer.writeVarInt(route.points().size());
            for (RoutePointData point : route.points()) {
                buffer.writeUtf(point.dimension(), EchoPayloadCodecs.ID);
                buffer.writeDouble(point.x());
                buffer.writeDouble(point.y());
                buffer.writeDouble(point.z());
                buffer.writeVarInt(point.order());
                buffer.writeUtf(point.label(), MAX_TEXT);
                buffer.writeEnum(point.precision());
            }
        }
        buffer.writeVarInt(packet.overlays().size());
        for (OverlayData overlay : packet.overlays()) {
            EchoPayloadCodecs.writeIdentifier(buffer, overlay.id());
            EchoPayloadCodecs.writeIdentifier(buffer, overlay.layerId());
            EchoPayloadCodecs.writeIdentifier(buffer, overlay.sourceId());
            buffer.writeEnum(overlay.kind());
            buffer.writeEnum(overlay.state());
            buffer.writeUtf(overlay.title(), MAX_TEXT);
            buffer.writeUtf(overlay.summary(), MAX_TEXT);
            buffer.writeUtf(overlay.dimension(), EchoPayloadCodecs.ID);
            buffer.writeDouble(overlay.x());
            buffer.writeDouble(overlay.y());
            buffer.writeDouble(overlay.z());
            buffer.writeFloat(overlay.radius());
            buffer.writeInt(overlay.color());
            buffer.writeEnum(overlay.precision());
        }
        buffer.writeVarInt(packet.zones().size());
        for (ZoneData zone : packet.zones()) {
            EchoPayloadCodecs.writeIdentifier(buffer, zone.id());
            EchoPayloadCodecs.writeIdentifier(buffer, zone.layerId());
            EchoPayloadCodecs.writeIdentifier(buffer, zone.sourceId());
            buffer.writeEnum(zone.shape());
            buffer.writeEnum(zone.pattern());
            buffer.writeEnum(zone.state());
            buffer.writeUtf(zone.title(), MAX_TEXT);
            buffer.writeUtf(zone.summary(), MAX_TEXT);
            buffer.writeUtf(zone.dimension(), EchoPayloadCodecs.ID);
            buffer.writeDouble(zone.x());
            buffer.writeDouble(zone.y());
            buffer.writeDouble(zone.z());
            buffer.writeFloat(zone.radius());
            buffer.writeFloat(zone.width());
            buffer.writeFloat(zone.depth());
            buffer.writeInt(zone.fillColor());
            buffer.writeInt(zone.outlineColor());
            buffer.writeEnum(zone.precision());
            buffer.writeVarInt(zone.priority());
            buffer.writeVarInt(zone.points().size());
            for (ZonePointData point : zone.points()) {
                buffer.writeUtf(point.dimension(), EchoPayloadCodecs.ID);
                buffer.writeDouble(point.x());
                buffer.writeDouble(point.y());
                buffer.writeDouble(point.z());
                buffer.writeVarInt(point.order());
            }
        }
        buffer.writeVarInt(packet.diagnostics().size());
        for (ProviderDiagnosticData diagnostic : packet.diagnostics()) {
            EchoPayloadCodecs.writeIdentifier(buffer, diagnostic.providerId());
            buffer.writeUtf(diagnostic.providerType(), MAX_TEXT);
            buffer.writeBoolean(diagnostic.healthy());
            buffer.writeVarInt(diagnostic.layers());
            buffer.writeVarInt(diagnostic.markers());
            buffer.writeVarInt(diagnostic.routes());
            buffer.writeVarInt(diagnostic.overlays());
            buffer.writeUtf(diagnostic.message(), MAX_TEXT);
            buffer.writeVarLong(diagnostic.failures());
        }
        buffer.writeUtf(packet.statusLine(), MAX_TEXT);
        buffer.writeVarLong(packet.gameTime());
    }

    private static HoloMapSnapshotPacket read(RegistryFriendlyByteBuf buffer) {
        int layerCount = Math.max(0, Math.min(MAX_LAYERS, buffer.readVarInt()));
        List<LayerData> layers = new ArrayList<>();
        for (int i = 0; i < layerCount; i++) {
            layers.add(new LayerData(
                    EchoPayloadCodecs.readIdentifier(buffer),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readVarInt(),
                    buffer.readInt(),
                    buffer.readBoolean()));
        }
        int markerCount = Math.max(0, Math.min(MAX_MARKERS_PACKET, buffer.readVarInt()));
        List<MarkerData> markers = new ArrayList<>();
        for (int i = 0; i < markerCount; i++) {
            markers.add(new MarkerData(
                    EchoPayloadCodecs.readIdentifier(buffer),
                    EchoPayloadCodecs.readIdentifier(buffer),
                    EchoPayloadCodecs.readIdentifier(buffer),
                    buffer.readEnum(IMapMarker.MarkerKind.class),
                    buffer.readEnum(IMapMarker.MarkerState.class),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(EchoPayloadCodecs.ID),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readFloat(),
                    buffer.readBoolean() ? EchoPayloadCodecs.readIdentifier(buffer) : null,
                    buffer.readUtf(EchoPayloadCodecs.ID),
                    buffer.readVarInt(),
                    buffer.readEnum(HoloMapPrecision.class),
                    buffer.readVarInt()));
        }
        int routeCount = Math.max(0, Math.min(MAX_ROUTES_PACKET, buffer.readVarInt()));
        List<RouteData> routes = new ArrayList<>();
        for (int i = 0; i < routeCount; i++) {
            Identifier id = EchoPayloadCodecs.readIdentifier(buffer);
            Identifier layerId = EchoPayloadCodecs.readIdentifier(buffer);
            Identifier sourceId = EchoPayloadCodecs.readIdentifier(buffer);
            String title = buffer.readUtf(MAX_TEXT);
            String summary = buffer.readUtf(MAX_TEXT);
            String dimension = buffer.readUtf(EchoPayloadCodecs.ID);
            int color = buffer.readInt();
            IMapMarker.MarkerState state = buffer.readEnum(IMapMarker.MarkerState.class);
            int pointCount = Math.max(0, Math.min(MAX_ROUTE_POINTS, buffer.readVarInt()));
            List<RoutePointData> points = new ArrayList<>();
            for (int p = 0; p < pointCount; p++) {
                points.add(new RoutePointData(
                        buffer.readUtf(EchoPayloadCodecs.ID),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readVarInt(),
                        buffer.readUtf(MAX_TEXT),
                        buffer.readEnum(HoloMapPrecision.class)));
            }
            routes.add(new RouteData(id, layerId, sourceId, title, summary, dimension, color, state, points));
        }
        int overlayCount = Math.max(0, Math.min(MAX_OVERLAYS_PACKET, buffer.readVarInt()));
        List<OverlayData> overlays = new ArrayList<>();
        for (int i = 0; i < overlayCount; i++) {
            overlays.add(new OverlayData(
                    EchoPayloadCodecs.readIdentifier(buffer),
                    EchoPayloadCodecs.readIdentifier(buffer),
                    EchoPayloadCodecs.readIdentifier(buffer),
                    buffer.readEnum(HoloMapOverlayKind.class),
                    buffer.readEnum(IMapMarker.MarkerState.class),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readUtf(EchoPayloadCodecs.ID),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readFloat(),
                    buffer.readInt(),
                    buffer.readEnum(HoloMapPrecision.class)));
        }
        int zoneCount = Math.max(0, Math.min(MAX_ZONES_PACKET, buffer.readVarInt()));
        List<ZoneData> zones = new ArrayList<>();
        for (int i = 0; i < zoneCount; i++) {
            Identifier id = EchoPayloadCodecs.readIdentifier(buffer);
            Identifier layerId = EchoPayloadCodecs.readIdentifier(buffer);
            Identifier sourceId = EchoPayloadCodecs.readIdentifier(buffer);
            HoloMapZoneShape shape = buffer.readEnum(HoloMapZoneShape.class);
            HoloMapZonePattern pattern = buffer.readEnum(HoloMapZonePattern.class);
            IMapMarker.MarkerState state = buffer.readEnum(IMapMarker.MarkerState.class);
            String title = buffer.readUtf(MAX_TEXT);
            String summary = buffer.readUtf(MAX_TEXT);
            String dimension = buffer.readUtf(EchoPayloadCodecs.ID);
            double x = buffer.readDouble();
            double y = buffer.readDouble();
            double z = buffer.readDouble();
            float radius = buffer.readFloat();
            float width = buffer.readFloat();
            float depth = buffer.readFloat();
            int fillColor = buffer.readInt();
            int outlineColor = buffer.readInt();
            HoloMapPrecision precision = buffer.readEnum(HoloMapPrecision.class);
            int priority = buffer.readVarInt();
            int pointCount = Math.max(0, Math.min(MAX_ZONE_POINTS, buffer.readVarInt()));
            List<ZonePointData> points = new ArrayList<>();
            for (int p = 0; p < pointCount; p++) {
                points.add(new ZonePointData(
                        buffer.readUtf(EchoPayloadCodecs.ID),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readVarInt()));
            }
            zones.add(new ZoneData(id, layerId, sourceId, shape, pattern, state, title, summary,
                    dimension, x, y, z, radius, width, depth, fillColor, outlineColor, precision, priority, points));
        }
        int diagnosticCount = Math.max(0, Math.min(MAX_DIAGNOSTICS, buffer.readVarInt()));
        List<ProviderDiagnosticData> diagnostics = new ArrayList<>();
        for (int i = 0; i < diagnosticCount; i++) {
            diagnostics.add(new ProviderDiagnosticData(
                    EchoPayloadCodecs.readIdentifier(buffer),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readUtf(MAX_TEXT),
                    buffer.readVarLong()));
        }
        return new HoloMapSnapshotPacket(layers, markers, routes, overlays, zones, diagnostics,
                buffer.readUtf(MAX_TEXT), buffer.readVarLong());
    }

    private static List<LayerData> copyLayers(List<LayerData> layers) {
        if (layers == null || layers.isEmpty()) {
            return List.of();
        }
        return layers.stream()
                .filter(layer -> layer != null && layer.id() != null)
                .limit(MAX_LAYERS)
                .toList();
    }

    private static List<MarkerData> copyMarkers(List<MarkerData> markers) {
        if (markers == null || markers.isEmpty()) {
            return List.of();
        }
        return markers.stream()
                .filter(marker -> marker != null && marker.id() != null && marker.layerId() != null)
                .limit(maxMarkers())
                .toList();
    }

    private static List<RouteData> copyRoutes(List<RouteData> routes) {
        if (routes == null || routes.isEmpty()) {
            return List.of();
        }
        return routes.stream()
                .filter(route -> route != null && route.id() != null && route.layerId() != null)
                .limit(maxRoutes())
                .toList();
    }

    private static List<OverlayData> copyOverlays(List<OverlayData> overlays) {
        if (overlays == null || overlays.isEmpty()) {
            return List.of();
        }
        return overlays.stream()
                .filter(overlay -> overlay != null && overlay.id() != null && overlay.layerId() != null)
                .limit(maxOverlays())
                .toList();
    }

    private static List<ZoneData> copyZones(List<ZoneData> zones) {
        if (zones == null || zones.isEmpty()) {
            return List.of();
        }
        return zones.stream()
                .filter(zone -> zone != null && zone.id() != null && zone.layerId() != null)
                .limit(maxZones())
                .toList();
    }

    private static List<ProviderDiagnosticData> copyDiagnostics(List<ProviderDiagnosticData> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return List.of();
        }
        return diagnostics.stream()
                .filter(diagnostic -> diagnostic != null && diagnostic.providerId() != null)
                .limit(MAX_DIAGNOSTICS)
                .toList();
    }

    public static int maxMarkers() {
        try {
            return Math.max(32, Math.min(MAX_MARKERS_PACKET, Config.MAX_MARKERS.get()));
        } catch (RuntimeException exception) {
            return 384;
        }
    }

    public static int maxRoutes() {
        try {
            return Math.max(16, Math.min(MAX_ROUTES_PACKET, Config.MAX_ROUTES.get()));
        } catch (RuntimeException exception) {
            return 128;
        }
    }

    public static int maxOverlays() {
        try {
            return Math.max(16, Math.min(MAX_OVERLAYS_PACKET, Config.MAX_OVERLAYS.get()));
        } catch (RuntimeException exception) {
            return 256;
        }
    }

    public static int maxZones() {
        try {
            return Math.max(16, Math.min(MAX_ZONES_PACKET, Config.MAX_ZONES.get()));
        } catch (RuntimeException exception) {
            return 256;
        }
    }

    public static int maxDiagnostics() {
        return MAX_DIAGNOSTICS;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    public record LayerData(Identifier id, String title, int sortOrder, int color, boolean visibleByDefault) {
        public LayerData {
            title = safe(title, id == null ? "Layer" : id.getPath());
            color = color == 0 ? 0xFF66E8FF : color;
        }

        public static LayerData from(IMapLayer layer) {
            return new LayerData(layer.id(), layer.title(), layer.sortOrder(), layer.color(), layer.visibleByDefault());
        }

        public static LayerData from(HoloMapLayerData layer) {
            return new LayerData(layer.id(), layer.title(), layer.sortOrder(), layer.color(), layer.visibleByDefault());
        }
    }

    public record MarkerData(
            Identifier id,
            Identifier layerId,
            Identifier sourceId,
            IMapMarker.MarkerKind kind,
            IMapMarker.MarkerState state,
            String title,
            String summary,
            String dimension,
            double x,
            double y,
            double z,
            float radius,
            Identifier icon,
            String routeId,
            int routeOrder,
            HoloMapPrecision precision,
            int priority) {
        public MarkerData {
            sourceId = sourceId == null ? id : sourceId;
            kind = kind == null ? IMapMarker.MarkerKind.GENERIC : kind;
            state = state == null ? IMapMarker.MarkerState.DISCOVERED : state;
            title = safe(title, id == null ? "Marker" : id.getPath());
            summary = safe(summary, "");
            dimension = safe(dimension, "minecraft:overworld");
            radius = Math.max(0.0F, radius);
            routeId = safe(routeId, "");
            precision = precision == null ? HoloMapPrecision.ESTIMATED : precision;
        }

        public boolean precise() {
            return precision == HoloMapPrecision.PRECISE;
        }

        public static MarkerData from(IMapMarker marker) {
            return from(HoloMapMarkerData.fromCore(marker));
        }

        public static MarkerData from(HoloMapMarkerData marker) {
            return new MarkerData(
                    marker.id(),
                    marker.layerId(),
                    marker.sourceId(),
                    marker.kind(),
                    marker.state(),
                    marker.title(),
                    marker.summary(),
                    marker.dimension().identifier().toString(),
                    marker.x(),
                    marker.y(),
                    marker.z(),
                    marker.radius(),
                    marker.icon(),
                    marker.routeId() == null ? "" : marker.routeId().toString(),
                    marker.routeOrder(),
                    marker.precision(),
                    marker.priority());
        }
    }

    public record RoutePointData(
            String dimension,
            double x,
            double y,
            double z,
            int order,
            String label,
            HoloMapPrecision precision) {
        public RoutePointData {
            dimension = safe(dimension, "minecraft:overworld");
            label = safe(label, "");
            precision = precision == null ? HoloMapPrecision.ESTIMATED : precision;
        }

        public static RoutePointData from(HoloMapRoutePoint point) {
            return new RoutePointData(point.dimension().identifier().toString(), point.x(), point.y(), point.z(),
                    point.order(), point.label(), point.precision());
        }
    }

    public record RouteData(
            Identifier id,
            Identifier layerId,
            Identifier sourceId,
            String title,
            String summary,
            String dimension,
            int color,
            IMapMarker.MarkerState state,
            List<RoutePointData> points) {
        public RouteData {
            sourceId = sourceId == null ? id : sourceId;
            title = safe(title, id == null ? "Route" : id.getPath());
            summary = safe(summary, "");
            dimension = safe(dimension, "minecraft:overworld");
            color = color == 0 ? 0xFF92F7A6 : color;
            state = state == null ? IMapMarker.MarkerState.DISCOVERED : state;
            points = points == null ? List.of() : points.stream()
                    .filter(point -> point != null)
                    .sorted(java.util.Comparator.comparingInt(RoutePointData::order)
                            .thenComparing(RoutePointData::label))
                    .limit(MAX_ROUTE_POINTS)
                    .toList();
        }

        public static RouteData from(HoloMapRouteData route) {
            return new RouteData(route.id(), route.layerId(), route.sourceId(), route.title(), route.summary(),
                    route.dimension().identifier().toString(), route.color(), route.state(),
                    route.points().stream().map(RoutePointData::from).toList());
        }
    }

    public record OverlayData(
            Identifier id,
            Identifier layerId,
            Identifier sourceId,
            HoloMapOverlayKind kind,
            IMapMarker.MarkerState state,
            String title,
            String summary,
            String dimension,
            double x,
            double y,
            double z,
            float radius,
            int color,
            HoloMapPrecision precision) {
        public OverlayData {
            sourceId = sourceId == null ? id : sourceId;
            kind = kind == null ? HoloMapOverlayKind.CIRCLE : kind;
            state = state == null ? IMapMarker.MarkerState.DISCOVERED : state;
            title = safe(title, id == null ? "Overlay" : id.getPath());
            summary = safe(summary, "");
            dimension = safe(dimension, "minecraft:overworld");
            radius = Math.max(0.0F, radius);
            color = color == 0 ? 0x66FF5C7A : color;
            precision = precision == null ? HoloMapPrecision.ESTIMATED : precision;
        }

        public static OverlayData from(HoloMapOverlayData overlay) {
            return new OverlayData(overlay.id(), overlay.layerId(), overlay.sourceId(), overlay.kind(),
                    overlay.state(), overlay.title(), overlay.summary(),
                    overlay.dimension().identifier().toString(), overlay.x(), overlay.y(), overlay.z(),
                    overlay.radius(), overlay.color(), overlay.precision());
        }
    }

    public record ZonePointData(
            String dimension,
            double x,
            double y,
            double z,
            int order) {
        public ZonePointData {
            dimension = safe(dimension, "minecraft:overworld");
        }

        public static ZonePointData from(HoloMapZonePoint point) {
            return new ZonePointData(point.dimension().identifier().toString(),
                    point.x(), point.y(), point.z(), point.order());
        }
    }

    public record ZoneData(
            Identifier id,
            Identifier layerId,
            Identifier sourceId,
            HoloMapZoneShape shape,
            HoloMapZonePattern pattern,
            IMapMarker.MarkerState state,
            String title,
            String summary,
            String dimension,
            double x,
            double y,
            double z,
            float radius,
            float width,
            float depth,
            int fillColor,
            int outlineColor,
            HoloMapPrecision precision,
            int priority,
            List<ZonePointData> points) {
        public ZoneData {
            sourceId = sourceId == null ? id : sourceId;
            shape = shape == null ? HoloMapZoneShape.CIRCLE : shape;
            pattern = pattern == null ? HoloMapZonePattern.SOLID : pattern;
            state = state == null ? IMapMarker.MarkerState.DISCOVERED : state;
            title = safe(title, id == null ? "Zone" : id.getPath());
            summary = safe(summary, "");
            dimension = safe(dimension, "minecraft:overworld");
            radius = Math.max(0.0F, radius);
            width = Math.max(0.0F, width);
            depth = Math.max(0.0F, depth);
            fillColor = fillColor == 0 ? 0x335CDAFF : fillColor;
            outlineColor = outlineColor == 0 ? 0xAA5CDAFF : outlineColor;
            precision = precision == null ? HoloMapPrecision.ESTIMATED : precision;
            points = points == null ? List.of() : points.stream()
                    .filter(point -> point != null)
                    .sorted(java.util.Comparator.comparingInt(ZonePointData::order))
                    .limit(MAX_ZONE_POINTS)
                    .toList();
        }

        public static ZoneData from(HoloMapZoneData zone) {
            return new ZoneData(zone.id(), zone.layerId(), zone.sourceId(), zone.shape(), zone.pattern(),
                    zone.state(), zone.title(), zone.summary(), zone.dimension().identifier().toString(),
                    zone.x(), zone.y(), zone.z(), zone.radius(), zone.width(), zone.depth(),
                    zone.fillColor(), zone.outlineColor(), zone.precision(), zone.priority(),
                    zone.points().stream().map(ZonePointData::from).toList());
        }
    }

    public record ProviderDiagnosticData(
            Identifier providerId,
            String providerType,
            boolean healthy,
            int layers,
            int markers,
            int routes,
            int overlays,
            String message,
            long failures) {
        public ProviderDiagnosticData {
            providerType = safe(providerType, "unknown");
            layers = Math.max(0, layers);
            markers = Math.max(0, markers);
            routes = Math.max(0, routes);
            overlays = Math.max(0, overlays);
            message = safe(message, "");
            failures = Math.max(0L, failures);
        }

        public static ProviderDiagnosticData from(HoloMapProviderDiagnostic diagnostic) {
            return new ProviderDiagnosticData(diagnostic.providerId(), diagnostic.providerType(), diagnostic.healthy(),
                    diagnostic.layers(), diagnostic.markers(), diagnostic.routes(), diagnostic.overlays(),
                    diagnostic.message(), diagnostic.failures());
        }
    }
}
