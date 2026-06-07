package com.knoxhack.echo.npcore.data;

import java.util.UUID;
import net.minecraft.resources.Identifier;

public record NpcConversionRecord(
        UUID oldEntityUuid,
        UUID newEntityUuid,
        String sourceType,
        String sourceProfession,
        Identifier echoNpcProfile,
        long convertedAtGameTime) {
}
