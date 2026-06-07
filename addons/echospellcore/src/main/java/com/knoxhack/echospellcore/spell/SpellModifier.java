package com.knoxhack.echospellcore.spell;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum SpellModifier {
    RANGE("range", 1, 1.18D, 1.25D, 1.0D, 0.01D,
            "Extends targeting vectors and projectile life."),
    EFFICIENCY("efficiency", 1, 0.82D, 1.0D, 1.0D, 0.0D,
            "Reduces Aether draw and shortens cooldown recovery."),
    OVERCHARGE("overcharge", 2, 1.35D, 1.12D, 1.45D, 0.07D,
            "Raises output sharply, but consumes two sockets and risks curse feedback.");

    private final String id;
    private final int socketCost;
    private final double costScale;
    private final double rangeScale;
    private final double damageScale;
    private final double curseRisk;
    private final String description;

    SpellModifier(String id, int socketCost, double costScale, double rangeScale, double damageScale,
            double curseRisk, String description) {
        this.id = id;
        this.socketCost = socketCost;
        this.costScale = costScale;
        this.rangeScale = rangeScale;
        this.damageScale = damageScale;
        this.curseRisk = curseRisk;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public int socketCost() {
        return socketCost;
    }

    public double costScale() {
        return costScale;
    }

    public double rangeScale() {
        return rangeScale;
    }

    public double damageScale() {
        return damageScale;
    }

    public double curseRisk() {
        return curseRisk;
    }

    public String description() {
        return description;
    }

    public String title() {
        String text = id.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public static List<SpellModifier> ordered() {
        return List.of(values());
    }

    public static Optional<SpellModifier> byId(String id) {
        return Arrays.stream(values()).filter(modifier -> modifier.id.equals(id)).findFirst();
    }
}
