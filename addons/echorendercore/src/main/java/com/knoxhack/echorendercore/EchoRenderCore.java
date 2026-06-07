package com.knoxhack.echorendercore;

import com.knoxhack.echorendercore.particle.RenderCoreParticles;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EchoRenderCore {
   public static final String MODID = "echorendercore";
   public static final Logger LOGGER = LogUtils.getLogger();

   public EchoRenderCore() {
      RenderCoreParticles.register();
      com.knoxhack.echorendercore.integration.prime.RenderCorePrimeIntegration.register();
      LOGGER.info("ECHO: RenderCore online.");
   }
}
