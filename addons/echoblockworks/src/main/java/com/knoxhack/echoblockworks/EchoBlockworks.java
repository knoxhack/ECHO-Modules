package com.knoxhack.echoblockworks;

import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoblockworks.integration.BlockworksCoreIntegration;
import com.knoxhack.echoblockworks.integration.BlockworksIndexProvider;
import com.knoxhack.echoblockworks.integration.BlockworksMissionCoreIntegration;
import com.knoxhack.echoblockworks.registry.ModBlockEntities;
import com.knoxhack.echoblockworks.registry.ModBlocks;
import com.knoxhack.echoblockworks.registry.ModCreativeTabs;
import com.knoxhack.echoblockworks.registry.ModItems;
import com.knoxhack.echoblockworks.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class EchoBlockworks {
   public static final String MODID = "echoblockworks";
   public static final Logger LOGGER = LogUtils.getLogger();

   public EchoBlockworks() {
      ModBlocks.register();
      ModBlockEntities.register();
      ModItems.register();
      ModMenus.register();
      ModCreativeTabs.register();

      Config.registerEchoConfig();
      commonSetup();
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MODID, path);
   }

   public void commonSetup() {
      BlockworksCoreIntegration.registerAddonChapter();
      if (EchoRuntimeModules.isLoaded("echomissioncore")) {
         BlockworksMissionCoreIntegration.register();
      }
      if (EchoRuntimeModules.isLoaded("echoindex")) {
         BlockworksIndexProvider.register();
      }
      LOGGER.info("ECHO Blockworks catalog online.");
   }
}
