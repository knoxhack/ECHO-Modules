package com.knoxhack.echoashfallprotocol.world;

import com.knoxhack.echoashfallprotocol.echo.QuestData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compatibility facade for older systems that still ask for POIData.
 * ExplorationSiteRegistry is the authoritative site profile source.
 */
public final class POIRegistry {

    private static final Map<String, POIData> POIS = new LinkedHashMap<>();
    private static final List<POIData> FACTION_HUBS = new ArrayList<>();
    private static final List<POIData> WORLD_POIS = new ArrayList<>();
    private static final List<POIData> RELAY_STATIONS = new ArrayList<>();

    static {
        for (ExplorationSiteRegistry.SiteProfile profile : ExplorationSiteRegistry.allSorted()) {
            register(fromProfile(profile));
        }
    }

    private POIRegistry() {
    }

    private static POIData fromProfile(ExplorationSiteRegistry.SiteProfile profile) {
        return new POIData(
                profile.id(),
                profile.displayName(),
                profile.description(),
                profile.poiType(),
                profile.dangerLevel(),
                profile.faction(),
                profile.lootDescriptions(),
                profile.features(),
                profile.researchPoints(),
                profile.fastTravel(),
                profile.requiredGear()
        );
    }

    private static void register(POIData poi) {
        POIS.put(poi.getId(), poi);

        if (poi.getType() == POIData.POIType.FACTION_HUB) {
            FACTION_HUBS.add(poi);
        } else {
            WORLD_POIS.add(poi);
        }

        if (poi.hasFastTravel()) {
            RELAY_STATIONS.add(poi);
        }
    }

    public static POIData get(String id) {
        return POIS.get(ExplorationSiteRegistry.normalize(id));
    }

    public static List<POIData> getAll() {
        return new ArrayList<>(POIS.values());
    }

    public static List<POIData> getFactionHubs() {
        return new ArrayList<>(FACTION_HUBS);
    }

    public static List<POIData> getWorldPOIs() {
        return new ArrayList<>(WORLD_POIS);
    }

    public static List<POIData> getRelayStations() {
        return new ArrayList<>(RELAY_STATIONS);
    }

    public static List<POIData> getByFaction(Identifier faction) {
        List<POIData> result = new ArrayList<>();
        for (POIData poi : FACTION_HUBS) {
            if (faction != null && faction.equals(poi.getAssociatedFaction())) {
                result.add(poi);
            }
        }
        return result;
    }

    public static int getTotalPOICount() {
        return POIS.size();
    }

    public static int getDiscoveredCount(Player player) {
        return QuestData.get(player).getDiscoveredPOICount();
    }

    public static void init() {
        // Static initialization happens above.
    }
}
