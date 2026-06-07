package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.registry.ModBlocks;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpoint;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpointProvider;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpointRole;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public enum ConvoyLogisticsEndpointProvider implements LogisticsExternalEndpointProvider {
   INSTANCE;

   private static final int RADIUS = 24;

   public static void register() {
      LogisticsNetworkService.registerExternalEndpointProvider(INSTANCE);
   }

   @Override
   public Identifier providerId() {
      return Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, "convoy_logistics_endpoints");
   }

   @Override
   public List<LogisticsExternalEndpoint> endpoints(Level level, BlockPos origin, String networkId) {
      if (level == null || origin == null) {
         return List.of();
      }
      List<LogisticsExternalEndpoint> endpoints = new ArrayList<>();
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-RADIUS, -6, -RADIUS), origin.offset(RADIUS, 6, RADIUS))) {
         Block block = level.getBlockState(pos).getBlock();
         if (block == ModBlocks.CARGO_INPUT_CRATE.get() || block == ModBlocks.FUEL_INPUT_TANK.get()) {
            endpoints.add(endpoint(pos, networkId, "convoy_input", true));
         } else if (block == ModBlocks.CARGO_OUTPUT_CRATE.get() || block == ModBlocks.FUEL_OUTPUT_TANK.get()) {
            endpoints.add(endpoint(pos, networkId, "convoy_output", true));
         } else if (block == ModBlocks.DEPOT_ITEM_BUS.get()) {
            endpoints.add(endpoint(pos, networkId, "convoy_bus", false));
         }
      }
      return endpoints;
   }

   private static LogisticsExternalEndpoint endpoint(BlockPos pos, String networkId, String category, boolean deliveryTarget) {
      EnumSet<LogisticsExternalEndpointRole> roles = EnumSet.of(LogisticsExternalEndpointRole.STORAGE);
      if (deliveryTarget) {
         roles.add(LogisticsExternalEndpointRole.DELIVERY_TARGET);
      }
      return new LogisticsExternalEndpoint(
         pos,
         networkId,
         Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, category),
         null,
         roles
      );
   }
}
