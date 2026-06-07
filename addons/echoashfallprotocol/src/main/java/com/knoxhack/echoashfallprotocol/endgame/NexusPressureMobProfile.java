package com.knoxhack.echoashfallprotocol.endgame;

public record NexusPressureMobProfile(
        String entityPath,
        String title,
        double maxHealth,
        double attackDamage,
        double armor,
        double movementSpeed,
        float pulseDamage,
        int accentColor,
        Ability ability
) {
    public NexusPressureMobProfile {
        entityPath = entityPath == null ? "" : entityPath;
        title = title == null || title.isBlank() ? entityPath : title;
        maxHealth = Math.max(1.0D, maxHealth);
        attackDamage = Math.max(0.0D, attackDamage);
        armor = Math.max(0.0D, armor);
        movementSpeed = Math.max(0.05D, movementSpeed);
        pulseDamage = Math.max(0.0F, pulseDamage);
        accentColor = 0xFF000000 | (accentColor & 0x00FFFFFF);
        ability = ability == null ? Ability.GRID_PRESSURE : ability;
    }

    public String entityId() {
        return "echoashfallprotocol:" + entityPath;
    }

    public enum Ability {
        GRID_PRESSURE,
        WARDEN_BULWARK,
        SIGNAL_LEECH,
        NULL_FIELD
    }
}
