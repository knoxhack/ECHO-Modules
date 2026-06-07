package com.knoxhack.echoholomap.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.network.HoloMapWaypointClientState;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint.Scope;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class HoloMapLocalWaypointStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean loaded;

    private HoloMapLocalWaypointStore() {
    }

    public static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = path();
        if (!Files.exists(path)) {
            HoloMapWaypointClientState.setLocalWaypoints(List.of());
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement json = JsonParser.parseReader(reader);
            List<HoloMapWaypoint> waypoints = HoloMapWaypoint.CODEC.listOf()
                    .parse(JsonOps.INSTANCE, json)
                    .result()
                    .orElse(List.of())
                    .stream()
                    .filter(waypoint -> waypoint.scope() == Scope.LOCAL)
                    .toList();
            HoloMapWaypointClientState.setLocalWaypoints(waypoints);
        } catch (IOException | RuntimeException exception) {
            EchoHoloMap.LOGGER.warn("ECHO HoloMap local waypoint store could not be loaded.", exception);
            HoloMapWaypointClientState.setLocalWaypoints(List.of());
        }
    }

    public static synchronized void upsert(HoloMapWaypoint waypoint) {
        ensureLoaded();
        if (waypoint == null || waypoint.scope() != Scope.LOCAL) {
            return;
        }
        HoloMapWaypointClientState.upsertLocal(waypoint);
        save();
    }

    public static synchronized boolean remove(Identifier id) {
        ensureLoaded();
        boolean removed = HoloMapWaypointClientState.removeLocal(id);
        if (removed) {
            save();
        }
        return removed;
    }

    public static synchronized void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            JsonElement json = HoloMapWaypoint.CODEC.listOf()
                    .encodeStart(JsonOps.INSTANCE, HoloMapWaypointClientState.localWaypoints())
                    .result()
                    .orElseThrow();
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException | RuntimeException exception) {
            EchoHoloMap.LOGGER.warn("ECHO HoloMap local waypoint store could not be saved.", exception);
        }
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("echoholomap-local-waypoints.json");
    }
}
