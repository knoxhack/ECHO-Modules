package com.knoxhack.echo.hazardcore.api;

import net.minecraft.resources.Identifier;

/**
 * Canonical hazard types exposed by ECHO HazardCore.
 * Packs may register additional hazards via {@link HazardService#registerHazard(HazardType)}.
 */
public final class HazardType {
    public static final HazardType PRESSURE = builtin("pressure");
    public static final HazardType OXYGEN_DEPRIVATION = builtin("oxygen_deprivation");
    public static final HazardType COLD = builtin("cold");
    public static final HazardType HEAT = builtin("heat");
    public static final HazardType CORRUPTION = builtin("corruption");
    public static final HazardType DECOMPRESSION_SICKNESS = builtin("decompression_sickness");

    private final Identifier id;
    private final boolean builtin;

    private HazardType(Identifier id, boolean builtin) {
        this.id = id;
        this.builtin = builtin;
    }

    public static HazardType of(Identifier id) {
        return new HazardType(id, false);
    }

    private static HazardType builtin(String path) {
        return new HazardType(Identifier.fromNamespaceAndPath("echohazardcore", path), true);
    }

    public Identifier id() {
        return id;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof HazardType other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
