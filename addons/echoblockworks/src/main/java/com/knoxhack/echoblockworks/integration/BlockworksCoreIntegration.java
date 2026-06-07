package com.knoxhack.echoblockworks.integration;

import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.entity.player.Player;

public final class BlockworksCoreIntegration {
   public static final String CHAPTER_ID = "blockworks";
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

   private BlockworksCoreIntegration() {
   }

   public static void registerAddonChapter() {
      if (REGISTERED.compareAndSet(false, true) && !EchoAddonRegistry.isRegistered(CHAPTER_ID)) {
         EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
               return CHAPTER_ID;
            }

            @Override
            public String modId() {
               return EchoBlockworks.MODID;
            }

            @Override
            public String displayName() {
               return "ECHO Blockworks";
            }

            @Override
            public String summary() {
               return "First-party decorative and structural block library for Ashfall ruins, ECHO facilities, Nexus chambers, Blackbox vaults, and reclamation domes.";
            }

            @Override
            public boolean isAvailable(Player player) {
               return true;
            }

            @Override
            public String statusLine(Player player) {
               return "BLOCKWORKS: catalog online; conversion table and pattern cutter available.";
            }
         });
      }
   }
}
