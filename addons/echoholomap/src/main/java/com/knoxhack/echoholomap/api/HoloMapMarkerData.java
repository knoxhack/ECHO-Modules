package com.knoxhack.echoholomap.api;

import com.echoplatform.echocore.api.EchoMapMarker;
import com.echoplatform.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.HoloMapIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record HoloMapMarkerData(
        Identifier id,
        Identifier layerId,
        Identifier sourceId,
        IMapMarker.MarkerKind kind,
        IMapMarker.MarkerState state,
        String title,
        String summary,
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        float radius,
        Identifier icon,
        Identifier routeId,
        int routeOrder,
        HoloMapPrecision precision,
        int priority) {
    public HoloMapMarkerData {
        id = id == null ? HoloMapIds.id("marker/unknown") : id;
        layerId = layerId == null ? HoloMapIds.layer("unknown") : layerId;
        sourceId = sourceId == null ? id : sourceId;
        kind = kind == null ? IMapMarker.MarkerKind.GENERIC : kind;
        state = state == null ? IMapMarker.MarkerState.DISCOVERED : state;
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        summary = summary == null ? "" : summary.strip();
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        radius = Math.max(0.0F, radius);
        precision = precision == null ? HoloMapPrecision.ESTIMATED : precision;
    }

    public static HoloMapMarkerData fromCore(IMapMarker marker) {
        if (marker == null) {
            return null;
        }
        return new HoloMapMarkerData(
                marker.id(),
                marker.layerId(),
                marker.sourceId(),
                marker.kind(),
                marker.state(),
                marker.title(),
                marker.summary(),
                marker.dimension(),
                marker.x(),
                marker.y(),
                marker.z(),
                marker.radius(),
                marker.icon(),
                marker.routeId(),
                marker.routeOrder(),
                HoloMapPrecision.fromCore(marker),
                0);
    }

    public IMapMarker toCore() {
        return new EchoMapMarker(
                id,
                layerId,
                sourceId,
                kind,
                state,
                title,
                summary,
                dimension,
                x,
                y,
                z,
                radius,
                icon,
                routeId,
                routeOrder,
                precision == HoloMapPrecision.PRECISE);
    }
}
