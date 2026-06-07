package com.knoxhack.echoorbitalremnants.progression;

import net.minecraft.network.chat.Component;

public enum OrbitalEventType {
    DEBRIS_STORM("Debris Storm", "Use station plating for cover. Solar draw may collapse."),
    SOLAR_FLARE("Solar Flare", "Radiation climbing. Electronics may stutter before you do."),
    STATION_BLACKOUT("Station Blackout", "Life support unstable. Vacuum signatures moving through dark corridors."),
    NEXUS_PULSE("Nexus Pulse", "Gravity shear detected. ECHO memory interference rising.");

    private final String title;
    private final String diagnostic;

    OrbitalEventType(String title, String diagnostic) {
        this.title = title;
        this.diagnostic = diagnostic;
    }

    public Component diagnosticMessage() {
        return Component.literal("ECHO-7 // " + title + ": " + diagnostic);
    }
}
