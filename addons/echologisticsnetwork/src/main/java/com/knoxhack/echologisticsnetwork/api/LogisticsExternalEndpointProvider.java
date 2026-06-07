package com.knoxhack.echologisticsnetwork.api;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public interface LogisticsExternalEndpointProvider {
   Identifier providerId();

   List<LogisticsExternalEndpoint> endpoints(Level level, BlockPos origin, String networkId);
}
