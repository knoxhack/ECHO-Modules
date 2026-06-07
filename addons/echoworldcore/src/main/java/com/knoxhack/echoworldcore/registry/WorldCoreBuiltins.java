package com.knoxhack.echoworldcore.registry;

import com.knoxhack.echocore.api.WorldHazardDefinition;
import com.knoxhack.echoworldcore.EchoWorldCore;
import com.knoxhack.echoworldcore.service.WorldRegionService;
import net.minecraft.resources.Identifier;

public final class WorldCoreBuiltins {
    public static final Identifier SALVAGE_DEBRIS = id("hazard/salvage_debris");
    public static final Identifier TOXIC_AIR = id("hazard/toxic_air");
    public static final Identifier RADIATION = id("hazard/radiation");
    public static final Identifier CRYO_COLD = id("hazard/cryo_cold");
    public static final Identifier NEXUS_ANOMALY = id("hazard/nexus_anomaly");
    public static final Identifier ORBITAL_EXPOSURE = id("hazard/orbital_exposure");
    public static final Identifier CONVOY_THREAT = id("hazard/convoy_threat");
    public static final Identifier SECURE_ZONE = id("hazard/secure_zone");

    private WorldCoreBuiltins() {
    }

    public static void register(WorldRegionService service) {
        registerHazards(service);
    }

    private static void registerHazards(WorldRegionService service) {
        service.registerHazardDefinition(new WorldHazardDefinition(SALVAGE_DEBRIS,
                "Salvage Debris", "Sharp wreckage, unstable scraps, and damaged hull fragments.", 25, false));
        service.registerHazardDefinition(new WorldHazardDefinition(TOXIC_AIR,
                "Toxic Air", "Airborne chemical and spore contamination.", 55, false));
        service.registerHazardDefinition(new WorldHazardDefinition(RADIATION,
                "Radiation", "Irradiated terrain and unstable fallout pockets.", 70, false));
        service.registerHazardDefinition(new WorldHazardDefinition(CRYO_COLD,
                "Cryogenic Cold", "Extreme cold around ruptured cryogenic infrastructure.", 60, false));
        service.registerHazardDefinition(new WorldHazardDefinition(NEXUS_ANOMALY,
                "Nexus Anomaly", "Reality instability and corrupted field pressure.", 85, false));
        service.registerHazardDefinition(new WorldHazardDefinition(ORBITAL_EXPOSURE,
                "Orbital Exposure", "Vacuum, debris, oxygen, and pressure instability.", 75, false));
        service.registerHazardDefinition(new WorldHazardDefinition(CONVOY_THREAT,
                "Convoy Threat", "Route ambush and vehicle attrition pressure.", 45, false));
        service.registerHazardDefinition(new WorldHazardDefinition(SECURE_ZONE,
                "Secure Zone", "Stabilized or faction-held field position.", 0, false));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoWorldCore.MODID, path);
    }
}
