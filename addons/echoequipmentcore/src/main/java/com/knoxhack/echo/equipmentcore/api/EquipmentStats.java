package com.knoxhack.echo.equipmentcore.api;

/**
 * Aggregated hazard protection and durability stats for a piece of equipment.
 */
public record EquipmentStats(
        float pressureResistance,
        float oxygenBonus,
        float coldResistance,
        float heatResistance,
        float corruptionResistance,
        int durability,
        int maxDurability
) {
    public static final EquipmentStats ZERO = new EquipmentStats(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0);

    public EquipmentStats {
        pressureResistance = Math.max(0.0F, pressureResistance);
        oxygenBonus = Math.max(0.0F, oxygenBonus);
        coldResistance = Math.max(0.0F, coldResistance);
        heatResistance = Math.max(0.0F, heatResistance);
        corruptionResistance = Math.max(0.0F, corruptionResistance);
        durability = Math.max(0, durability);
        maxDurability = Math.max(0, maxDurability);
    }

    public EquipmentStats withDurability(int durability) {
        return new EquipmentStats(pressureResistance, oxygenBonus, coldResistance, heatResistance, corruptionResistance, durability, maxDurability);
    }

    public EquipmentStats add(EquipmentStats other) {
        return new EquipmentStats(
                pressureResistance + other.pressureResistance,
                oxygenBonus + other.oxygenBonus,
                coldResistance + other.coldResistance,
                heatResistance + other.heatResistance,
                corruptionResistance + other.corruptionResistance,
                durability + other.durability,
                maxDurability + other.maxDurability
        );
    }
}
