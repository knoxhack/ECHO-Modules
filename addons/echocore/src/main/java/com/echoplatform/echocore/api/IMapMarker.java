package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface IMapMarker {
    Identifier id();

    Identifier layerId();

    MarkerKind kind();

    MarkerState state();

    default String label() {
        return id() == null ? "" : id().toString();
    }

    default Identifier sourceId() {
        return id();
    }

    default String title() {
        return label();
    }

    default String summary() {
        return "";
    }

    default ResourceKey<Level> dimension() {
        return Level.OVERWORLD;
    }

    default double x() {
        return 0.0D;
    }

    default double y() {
        return 0.0D;
    }

    default double z() {
        return 0.0D;
    }

    default float radius() {
        return 0.0F;
    }

    default Identifier icon() {
        return null;
    }

    default Identifier routeId() {
        return null;
    }

    default int routeOrder() {
        return -1;
    }

    default boolean precise() {
        return false;
    }

    enum MarkerKind {
        GENERIC,
        DRONE_SCAN,
        STRUCTURE,
        HAZARD,
        MISSION,
        FACTION,
        ROUTE,
        REGION,
        CRASH_SITE,
        BASE_OUTPOST,
        ORBITAL_SCAN,
        NEXUS_ANOMALY
    }

    enum MarkerState {
        HIDDEN,
        LOCKED,
        DISCOVERED,
        CHECKED
    }
}
