package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echolens.registry.LensProviderRegistry;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReclamationLensIntegration {
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

   private ReclamationLensIntegration() {
   }

   public static void register() {
      if (REGISTERED.compareAndSet(false, true)) {
         LensProviderRegistry.register(ReclamationLensProvider.INSTANCE);
      }
   }
}
