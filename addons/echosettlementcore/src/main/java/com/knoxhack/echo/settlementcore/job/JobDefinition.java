package com.knoxhack.echo.settlementcore.job;

import com.knoxhack.echo.settlementcore.api.JobType;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Data class describing a settlement NPC job.
 */
public record JobDefinition(
    Identifier id,
    String title,
    JobType type,
    Identifier poiBlock,
    List<String> duties
) {
    public JobDefinition {
        duties = List.copyOf(duties == null ? List.of() : duties);
    }
}
