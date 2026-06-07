package com.knoxhack.echoindustrialnexus.registry;

import com.knoxhack.echo.adaptercore.EchoBackendFluidBridge;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialFluidPipeBlockEntity;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import net.minecraft.core.Direction;

public final class ModCapabilities {
   private ModCapabilities() {
   }

   public static void register(Object event) {
      EchoBackendFluidBridge.registerBlockEntityFluid(
         event,
         ModBlockEntities.INDUSTRIAL_MACHINE.get(),
         (machine, direction) -> ((IndustrialMachineBlockEntity) machine).fluidHandler((Direction) direction)
      );
      EchoBackendFluidBridge.registerBlockEntityFluid(
         event,
         ModBlockEntities.FLUID_PIPE.get(),
         (pipe, direction) -> ((IndustrialFluidPipeBlockEntity) pipe).fluidHandler((Direction) direction)
      );
   }
}
