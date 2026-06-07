package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.progress.IndustrialProgress;
import com.knoxhack.echomultiblockcore.api.AutomationEffectHandler;
import com.knoxhack.echomultiblockcore.api.AutomationEffectInvocation;
import com.knoxhack.echomultiblockcore.api.AutomationEffectResult;
import net.minecraft.resources.Identifier;

public final class IndustrialNexusArrayEffectHandler implements AutomationEffectHandler {
   public static final IndustrialNexusArrayEffectHandler INSTANCE = new IndustrialNexusArrayEffectHandler();
   public static final Identifier NEXUS_ARRAY_PRESSURE = EchoIndustrialNexus.id("nexus_array_pressure");

   private IndustrialNexusArrayEffectHandler() {
   }

   @Override
   public Identifier providerId() {
      return EchoIndustrialNexus.id("nexus_array_effects");
   }

   @Override
   public boolean handles(Identifier effectId) {
      return NEXUS_ARRAY_PRESSURE.equals(effectId);
   }

   @Override
   public AutomationEffectResult onStart(AutomationEffectInvocation invocation) {
      record(invocation, 3);
      return AutomationEffectResult.allow("Nexus Furnace Array pressure stabilized.");
   }

   @Override
   public AutomationEffectResult onComplete(AutomationEffectInvocation invocation) {
      record(invocation, 6);
      return AutomationEffectResult.allow("Nexus Furnace Array pressure resolved.");
   }

   private static void record(AutomationEffectInvocation invocation, int intensity) {
      if (invocation == null || invocation.level() == null) {
         return;
      }
      IndustrialProgress.recordNexusThermalWarning(invocation.level(), invocation.controllerPos());
      IndustrialCompat.recordNexusThermalPressure(invocation.level(), invocation.controllerPos(), intensity);
   }
}
