package com.knoxhack.echoorbitalremnants.world;

import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import java.util.Locale;
import net.minecraft.world.level.block.Block;

public enum GroundRecoverySiteType {
    ABANDONED_LAUNCH_PAD("Abandoned Launch Pad", "launch platform, frame, fuel, and oxygen checks", () -> ModBlocks.NAVIGATION_CONSOLE.get()),
    CRASHED_SATELLITE_FIELD("Crashed Satellite Field", "orbital alloy, circuits, and heat-shield salvage", () -> ModBlocks.VACUUM_CIRCUIT_BLOCK.get()),
    ORBITAL_COMMS_ARRAY("Orbital Comms Array", "transponder, route beacon, and navigation recovery", () -> ModBlocks.DOCKING_BEACON.get()),
    CRYO_CREW_BUNKER("Cryo Crew Bunker", "sealed suit fragments, oxygen reserves, and cold-storage warnings", () -> ModBlocks.OXYGEN_PIPE.get()),
    FALLEN_ESCAPE_POD("Fallen Escape Pod", "engine, ECHO core, pod telemetry, and Station ECHO fall-path evidence", () -> ModBlocks.STATION_WALL_PANEL.get());

    private final String displayName;
    private final String rewardRole;
    private final java.util.function.Supplier<Block> landmark;

    GroundRecoverySiteType(String displayName, String rewardRole, java.util.function.Supplier<Block> landmark) {
        this.displayName = displayName;
        this.rewardRole = rewardRole;
        this.landmark = landmark;
    }

    public String displayName() {
        return displayName;
    }

    public String rewardRole() {
        return rewardRole;
    }

    public Block landmark() {
        return landmark.get();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static GroundRecoverySiteType byName(String name) {
        try {
            return GroundRecoverySiteType.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return ABANDONED_LAUNCH_PAD;
        }
    }
}
