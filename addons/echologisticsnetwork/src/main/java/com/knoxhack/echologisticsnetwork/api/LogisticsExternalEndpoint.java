package com.knoxhack.echologisticsnetwork.api;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public record LogisticsExternalEndpoint(
   BlockPos pos,
   String networkId,
   Identifier categoryId,
   Identifier loadoutId,
   Set<LogisticsExternalEndpointRole> roles
) {
   public LogisticsExternalEndpoint {
      pos = pos == null ? BlockPos.ZERO : pos.immutable();
      networkId = networkId == null || networkId.isBlank() ? "global" : networkId.strip();
      roles = roles == null || roles.isEmpty()
         ? Set.of(LogisticsExternalEndpointRole.STORAGE)
         : Set.copyOf(EnumSet.copyOf(roles));
   }

   public boolean hasRole(LogisticsExternalEndpointRole role) {
      return roles.contains(role);
   }
}
