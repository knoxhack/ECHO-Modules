package com.knoxhack.echoholomap.map;

import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echoholomap.HoloMapIds;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class HoloMapLayers {
    public static final List<IMapLayer> REQUIRED = List.of(
            new EchoMapLayer(HoloMapIds.CRASH_SITES, "Crash Sites", 10, 0xFFFFA05B, true),
            new EchoMapLayer(HoloMapIds.ROUTES, "Routes", 20, 0xFF92F7A6, true),
            new EchoMapLayer(HoloMapIds.HAZARDS, "Hazards", 30, 0xFFFF5C7A, true),
            new EchoMapLayer(HoloMapIds.MISSIONS, "Missions", 40, 0xFF66E8FF, true),
            new EchoMapLayer(HoloMapIds.BASES_OUTPOSTS, "Bases/Outposts", 50, 0xFFFFD166, true),
            new EchoMapLayer(HoloMapIds.ORBITAL_SCANS, "Orbital Scans", 60, 0xFFA58BFF, true),
            new EchoMapLayer(HoloMapIds.NEXUS_ANOMALY, "Nexus/Anomaly", 70, 0xFFFF8FEA, true),
            new EchoMapLayer(HoloMapIds.DRONES_SCANS, "Drones/Scans", 80, 0xFF7CF7D4, true));

    private HoloMapLayers() {
    }

    public static IMapLayer fallbackLayer(Identifier layerId) {
        String title = layerId == null ? "Unknown" : layerId.getPath()
                .replace("layer/", "")
                .replace('_', ' ')
                .replace('/', ' ');
        return new EchoMapLayer(layerId == null ? HoloMapIds.layer("unknown") : layerId,
                title, 500, 0xFF8CA7B5, true);
    }
}
