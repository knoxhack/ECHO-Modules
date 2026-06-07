package com.knoxhack.echonexusprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEnergyBridge;
import com.knoxhack.echonexusprotocol.block.entity.NexusMachineBlockEntity;

public final class ModEnergyCapabilities {
   private ModEnergyCapabilities() {
   }

   public static void register(Object event) {
      EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.NEXUS_MACHINE.get(),
         (blockEntity, side) -> blockEntity instanceof NexusMachineBlockEntity machine
            ? EchoBackendEnergyBridge.backendHandler(machine.energyStorage(), machine::setChanged)
            : null);
   }
}
