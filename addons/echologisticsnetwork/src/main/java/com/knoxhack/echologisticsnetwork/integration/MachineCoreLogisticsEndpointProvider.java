package com.knoxhack.echologisticsnetwork.integration;

import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpoint;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpointProvider;
import com.knoxhack.echologisticsnetwork.api.LogisticsExternalEndpointRole;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class MachineCoreLogisticsEndpointProvider implements LogisticsExternalEndpointProvider {
   private static final MachineCoreLogisticsEndpointProvider INSTANCE = new MachineCoreLogisticsEndpointProvider();
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final Identifier PROVIDER_ID = Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, "machinecore_machine_endpoints");
   private static final int RADIUS = 36;
   private static final int Y_RADIUS = 8;
   private static final Set<LogisticsExternalEndpointRole> MACHINE_ROLES = Set.of(
      LogisticsExternalEndpointRole.STORAGE,
      LogisticsExternalEndpointRole.DELIVERY_TARGET
   );

   private MachineCoreLogisticsEndpointProvider() {
   }

   public static void register() {
      if (REGISTERED.compareAndSet(false, true)) {
         LogisticsNetworkService.registerExternalEndpointProvider(INSTANCE);
         LogisticsNetworkService.invalidateSnapshots();
         EchoLogisticsNetwork.LOGGER.info("ECHO Logistics MachineCore endpoint provider registered.");
      }
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
         if (!level.isLoaded(pos)) {
            continue;
         }
         EchoMachineRuntimeSnapshot snapshot = EchoMachineRuntimeRegistry.snapshot(level, pos).orElse(null);
         if (snapshot == null || !EchoMachineUiBridge.hasAutomationSurface(snapshot)) {
            continue;
         }
         endpoints.add(new LogisticsExternalEndpoint(pos.immutable(), networkId, null, null, MACHINE_ROLES));
      }
      return List.copyOf(endpoints);
   }
}
