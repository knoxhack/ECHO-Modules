package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record EchoMapMarker(
        Identifier id,
        Identifier layerId,
        Identifier providerId,
        IMapMarker.MarkerKind kind,
        IMapMarker.MarkerState state,
        String label,
        String detail,
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        float radius,
        Identifier icon,
        Identifier routeId,
        int sortOrder,
        boolean precise) implements IMapMarker {
    public EchoMapMarker {
        kind = kind == null ? IMapMarker.MarkerKind.GENERIC : kind;
        state = state == null ? IMapMarker.MarkerState.DISCOVERED : state;
        label = label == null ? "" : label;
        detail = detail == null ? "" : detail;
        dimension = dimension == null ? Level.OVERWORLD : dimension;
    }

    public EchoMapMarker(
            Identifier id,
            Identifier layerId,
            Identifier providerId,
            IMapMarker.MarkerKind kind,
            IMapMarker.MarkerState state,
            String label,
            String detail,
            String dimension,
            double x,
            double y,
            double z,
            float radius,
            Identifier icon,
            Identifier routeId,
            int sortOrder,
            boolean precise) {
        this(id, layerId, providerId, kind, state, label, detail,
                parseDimension(dimension), x, y, z, radius, icon, routeId, sortOrder, precise);
    }

    @Override
    public Identifier sourceId() {
        return providerId;
    }

    @Override
    public String title() {
        return label;
    }

    @Override
    public String summary() {
        return detail;
    }

    @Override
    public int routeOrder() {
        return sortOrder;
    }

    private static ResourceKey<Level> parseDimension(String value) {
        Identifier id = Identifier.tryParse(value == null || value.isBlank() ? "" : value);
        return id == null ? Level.OVERWORLD : ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
    }
}
