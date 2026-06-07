package com.knoxhack.echoashfallprotocol.endgame;

import com.knoxhack.echoashfallprotocol.entity.boss.NexusFinalBossEntity;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;

public record NexusFinalBossProfile(
        PostNexusData.NexusPath path,
        String entityPath,
        String title,
        String subtitle,
        String phaseTwoLine,
        String phaseThreeLine,
        String defeatLine,
        String counterplay,
        double maxHealth,
        double attackDamage,
        double armor,
        double movementSpeed,
        float pulseDamage,
        int accentColor,
        Supplier<EntityType<NexusFinalBossEntity>> entityType
) {
    public NexusFinalBossProfile {
        path = path == null ? PostNexusData.NexusPath.NONE : path;
        entityPath = entityPath == null ? "" : entityPath;
        title = title == null || title.isBlank() ? entityPath : title;
        subtitle = subtitle == null ? "" : subtitle;
        phaseTwoLine = phaseTwoLine == null ? "" : phaseTwoLine;
        phaseThreeLine = phaseThreeLine == null ? "" : phaseThreeLine;
        defeatLine = defeatLine == null ? "" : defeatLine;
        counterplay = counterplay == null ? "" : counterplay;
        maxHealth = Math.max(1.0D, maxHealth);
        attackDamage = Math.max(0.0D, attackDamage);
        armor = Math.max(0.0D, armor);
        movementSpeed = Math.max(0.05D, movementSpeed);
        pulseDamage = Math.max(0.0F, pulseDamage);
        accentColor = 0xFF000000 | (accentColor & 0x00FFFFFF);
    }

    public String entityId() {
        return "echoashfallprotocol:" + entityPath;
    }
}
