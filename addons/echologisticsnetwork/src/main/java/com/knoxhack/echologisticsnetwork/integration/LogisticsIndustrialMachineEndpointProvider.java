package com.knoxhack.echologisticsnetwork.integration;

import com.knoxhack.echoindustrialnexus.api.IndustrialMachineTelemetryView;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpoint;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpointProvider;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpointRole;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class LogisticsIndustrialMachineEndpointProvider implements LogisticsExternalEndpointProvider {
   private static final LogisticsIndustrialMachineEndpointProvider INSTANCE = new LogisticsIndustrialMachineEndpointProvider();
   private static final Identifier PROVIDER_ID = Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, "industrial_machine_endpoints");
   private static final int RADIUS = 36;
   private static final int Y_RADIUS = 8;
   private static final Set<LogisticsExternalEndpointRole> MACHINE_ROLES = Set.of(
      LogisticsExternalEndpointRole.STORAGE,
      LogisticsExternalEndpointRole.DELIVERY_TARGET
   );

   private LogisticsIndustrialMachineEndpointProvider() {
   }

   public static void register() {
      LogisticsNetworkService.registerExternalEndpointProvider(INSTANCE);
      LogisticsNetworkService.invalidateSnapshots();
      EchoLogisticsNetwork.LOGGER.info("ECHO Logistics Industrial machine endpoint provider registered.");
   }

   @Override
   public Identifier providerId() {
      return PROVIDER_ID;
   }

   @Override
   public List<LogisticsExternalEndpoint> endpoints(Level level, BlockPos origin, String networkId) {
      if (level == null || origin == null) {
         return List.of();
      }
      List<LogisticsExternalEndpoint> endpoints = new ArrayList<>();
      for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-RADIUS, -Y_RADIUS, -RADIUS), origin.offset(RADIUS, Y_RADIUS, RADIUS))) {
         if (!(level.getBlockEntity(pos) instanceof IndustrialMachineBlockEntity machine)) {
            continue;
         }
         IndustrialMachineTelemetryView telemetry = IndustrialMachineTelemetryView.from(machine);
         if (telemetry.inventory().totalSlots() <= 0 || !hasAutomationSurface(telemetry)) {
            continue;
         }
         endpoints.add(new LogisticsExternalEndpoint(pos.immutable(), networkId, null, null, MACHINE_ROLES));
      }
      return endpoints;
   }

   private static boolean hasAutomationSurface(IndustrialMachineTelemetryView telemetry) {
      return telemetry.side().upSlots().size() + telemetry.side().downSlots().size() + telemetry.side().sideSlots().size() > 0;
   }
}
