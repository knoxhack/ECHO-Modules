package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class ModDimensions {
    public static final ResourceKey<Level> LOW_EARTH_ORBIT = key("low_earth_orbit");
    public static final ResourceKey<Level> LUNAR_SCAR_ZONE = key("lunar_scar_zone");
    public static final ResourceKey<Level> MARS_ASH_BASIN = key("mars_ash_basin");
    public static final ResourceKey<Level> EUROPA_CRYO_OCEAN = key("europa_cryo_ocean");
    public static final ResourceKey<Level> SATURN_RING_GRAVEYARD = key("saturn_ring_graveyard");
    public static final ResourceKey<Level> TITAN_METHANE_SHELF = key("titan_methane_shelf");
    public static final ResourceKey<Level> NEXUS_ANOMALY_BELT = key("nexus_anomaly_belt");

    private ModDimensions() {
    }

    public static boolean isSpaceLevel(Level level) {
        ResourceKey<Level> dimension = level.dimension();
        return dimension == LOW_EARTH_ORBIT
                || dimension == LUNAR_SCAR_ZONE
                || dimension == MARS_ASH_BASIN
                || dimension == EUROPA_CRYO_OCEAN
                || dimension == SATURN_RING_GRAVEYARD
                || dimension == TITAN_METHANE_SHELF
                || dimension == NEXUS_ANOMALY_BELT;
    }

    public static ServerLevel resolve(MinecraftServer server, ResourceKey<Level> dimension, ServerLevel fallback) {
        ServerLevel level = server.getLevel(dimension);
        return level != null ? level : fallback;
    }

    public static ResourceKey<Level> key(String path) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, path));
    }

    public static ResourceKey<Level> keyFromString(String id) {
        try {
            return ResourceKey.create(Registries.DIMENSION, Identifier.parse(id));
        } catch (Exception ignored) {
            return Level.OVERWORLD;
        }
    }
}
