package com.knoxhack.echoashfallprotocol.endgame;

import java.util.Locale;

public enum NexusRelayType {
    REACTOR("Reactor Relay", "radiation and power routing"),
    CRYO("Cryo Relay", "cold pressure and heat timing"),
    BIO("Bio Relay", "mutation pressure and purification"),
    TRANSIT("Transit Relay", "ambush routes and locked doors"),
    INDUSTRIAL("Industrial Relay", "machine repair and toxic air"),
    SCAR("Scar Relay", "anomaly mechanics and late Nexus pressure");

    private final String displayName;
    private final String routeIdentity;

    NexusRelayType(String displayName, String routeIdentity) {
        this.displayName = displayName;
        this.routeIdentity = routeIdentity;
    }

    public String displayName() {
        return displayName;
    }

    public String routeIdentity() {
        return routeIdentity;
    }

    public static NexusRelayType byName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (NexusRelayType type : values()) {
            if (type.name().equals(normalized) || type.displayName.toUpperCase(Locale.ROOT).startsWith(normalized)) {
                return type;
            }
        }
        return null;
    }
}
