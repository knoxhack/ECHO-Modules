package com.knoxhack.echoindustrialnexus.integration;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import com.echoplatform.echocore.api.EchoRuntimeModules;

public final class IndustrialPowerGridBridge {
   private static final String POWERGRID_MODID = "echopowergrid";
   private static final String POWERGRID_API = "com.knoxhack.echopowergrid.api.EchoPowerGridApi";
   private static final long EP_PER_THERMAL_FLUX = 1L;
   private static Method drawPowerMethod;
   private static boolean lookupAttempted;

   private IndustrialPowerGridBridge() {
   }

   public static int drawThermalFlux(Level level, BlockPos machinePos, int requested, boolean simulate) {
      if (level == null || machinePos == null || requested <= 0 || level.isClientSide() || !EchoRuntimeModules.isLoaded(POWERGRID_MODID)) {
         return 0;
      }
      Method drawPower = drawPowerMethod();
      if (drawPower == null) {
         return 0;
      }
      int remaining = requested;
      int drawn = drawAt(drawPower, level, machinePos, remaining, simulate);
      remaining -= drawn;
      for (Direction direction : Direction.values()) {
         if (remaining <= 0) {
            break;
         }
         int adjacentDrawn = drawAt(drawPower, level, machinePos.relative(direction), remaining, simulate);
         drawn += adjacentDrawn;
         remaining -= adjacentDrawn;
      }
      return Math.min(requested, Math.max(0, drawn));
   }

   public static int availableThermalFlux(Level level, BlockPos machinePos, int requested) {
      return drawThermalFlux(level, machinePos, requested, true);
   }

   private static Method drawPowerMethod() {
      if (lookupAttempted) {
         return drawPowerMethod;
      }
      lookupAttempted = true;
      try {
         Class<?> api = Class.forName(POWERGRID_API);
         drawPowerMethod = api.getMethod("drawPower", Level.class, BlockPos.class, long.class, boolean.class);
      } catch (ClassNotFoundException | NoSuchMethodException | LinkageError ignored) {
         drawPowerMethod = null;
      }
      return drawPowerMethod;
   }

   private static int drawAt(Method drawPower, Level level, BlockPos pos, int requested, boolean simulate) {
      if (requested <= 0) {
         return 0;
      }
      try {
         Object result = drawPower.invoke(null, level, pos, (long)requested * EP_PER_THERMAL_FLUX, simulate);
         Object drawn = result.getClass().getMethod("drawn").invoke(result);
         if (drawn instanceof Number number) {
            long flux = number.longValue() / EP_PER_THERMAL_FLUX;
            return (int)Math.min(Integer.MAX_VALUE, Math.max(0L, flux));
         }
      } catch (ReflectiveOperationException | LinkageError ignored) {
         return 0;
      }
      return 0;
   }
}
