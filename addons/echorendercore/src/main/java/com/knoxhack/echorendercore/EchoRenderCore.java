package com.knoxhack.echorendercore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echorendercore.particle.RenderCoreParticles;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EchoRenderCore {
   public static final String MODID = "echorendercore";
   public static final Logger LOGGER = LogUtils.getLogger();

   public EchoRenderCore() {
      this(null);
   }

   public EchoRenderCore(Object modEventBus) {
      RenderCoreParticles.register();
      com.knoxhack.echorendercore.integration.prime.RenderCorePrimeIntegration.register();
      EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
            "com.knoxhack.echorendercore.client.EchoRenderCoreClient");
      LOGGER.info("ECHO: RenderCore online.");
   }
}
