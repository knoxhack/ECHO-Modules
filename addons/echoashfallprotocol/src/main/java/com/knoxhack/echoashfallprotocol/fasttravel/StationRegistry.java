package com.knoxhack.echoashfallprotocol.fasttravel;

import net.minecraft.core.BlockPos;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all Relay Stations in the Radio Network.
 * Defines the 3 relay station locations.
 */
public class StationRegistry {
    
    private static final Map<String, RadioNetwork.StationInfo> STATIONS = new HashMap<>();
    
    // Pre-defined station locations (would be set during world generation)
    public static final RadioNetwork.StationInfo STATION_NORTH = register(new RadioNetwork.StationInfo(
        "relay_station_north",
        "North Relay Station",
        new BlockPos(1000, 70, -500) // Example coordinates
    ));
    
    public static final RadioNetwork.StationInfo STATION_SOUTH = register(new RadioNetwork.StationInfo(
        "relay_station_south",
        "South Relay Station",
        new BlockPos(-800, 65, 1200) // Example coordinates
    ));
    
    public static final RadioNetwork.StationInfo STATION_EAST = register(new RadioNetwork.StationInfo(
        "relay_station_east",
        "East Relay Station",
        new BlockPos(1500, 75, 300) // Example coordinates
    ));
    
    public static final RadioNetwork.StationInfo STATION_DROP_POD = register(new RadioNetwork.StationInfo(
        "relay_station_drop_pod",
        "Drop Pod Relay (Starting)",
        new BlockPos(0, 64, 0) // Starting area
    ));
    
    private static RadioNetwork.StationInfo register(RadioNetwork.StationInfo station) {
        STATIONS.put(station.getId(), station);
        return station;
    }
    
    public static RadioNetwork.StationInfo getStation(String id) {
        return STATIONS.get(id);
    }

    public static RadioNetwork.StationInfo getOrCreateStation(BlockPos pos) {
        RadioNetwork.StationInfo nearest = getNearestStation(pos, 16);
        if (nearest != null) {
            return nearest;
        }

        String id = dynamicId(pos);
        return STATIONS.computeIfAbsent(id, key -> new RadioNetwork.StationInfo(
            key,
            "Relay " + pos.getX() + ", " + pos.getZ(),
            pos.immutable()
        ));
    }

    public static RadioNetwork.StationInfo getNearestStation(BlockPos pos, int maxDistance) {
        int maxDistanceSqr = maxDistance * maxDistance;
        return STATIONS.values().stream()
            .filter(station -> station.getPosition().distSqr(pos) <= maxDistanceSqr)
            .min(Comparator.comparingDouble(station -> station.getPosition().distSqr(pos)))
            .orElse(null);
    }

    private static String dynamicId(BlockPos pos) {
        return "relay_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
    }
    
    public static Map<String, RadioNetwork.StationInfo> getAllStations() {
        return new HashMap<>(STATIONS);
    }
    
    public static int getTotalStationCount() {
        return STATIONS.size();
    }
    
    public static void init() {
        // Static initialization happens above
    }
}
