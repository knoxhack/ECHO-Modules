package com.knoxhack.echonexusprotocol.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoHandoffs;
import com.knoxhack.echonexusprotocol.Config;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class NexusProgression {
   public static final String STATIONFALL_GATE = EchoHandoffs.STATIONFALL_BLACKBOX_RECOVERED;
   public static final String NEXUS_PROTOCOL_COMPLETE = EchoHandoffs.NEXUS_PROTOCOL_COMPLETE;
   public static final String DEV_UNLOCK = "nexus:dev_unlock";
   public static final String BLACKBOX_MONOLITH = "nexus:blackbox_monolith_activated";
   public static final String PATH_RESTORE = "nexus:path:restore";
   public static final String PATH_CONTROL = "nexus:path:control";
   public static final String PATH_DESTROY = "nexus:path:destroy";
   public static final String PATH_MERGE = "nexus:path:merge";

   private NexusProgression() {
   }

   public static boolean isNexusUnlocked(Player player) {
      return player == null
         ? false
         : safeForceUnlock()
            || player.hasInfiniteMaterials()
            || player.getAbilities().instabuild
            || EchoCoreServices.progressLedger(player).hasMilestone(STATIONFALL_GATE)
            || EchoCoreServices.progressLedger(player).hasMilestone(DEV_UNLOCK);
   }

   public static void grantDevelopmentUnlock(ServerPlayer player) {
      EchoCoreServices.recordMilestone(player, DEV_UNLOCK);
      NexusPlayerData data = NexusPlayerData.get(player);
      data.unlockResearch("nexus_theory");
      NexusPlayerData.saveAndSync(player, data);
   }

   public static String milestoneForPath(String path) {
      String var1 = path == null ? "" : path.trim().toLowerCase(Locale.ROOT);

      return switch (var1) {
         case "restore" -> PATH_RESTORE;
         case "control" -> PATH_CONTROL;
         case "destroy" -> PATH_DESTROY;
         case "merge" -> PATH_MERGE;
         default -> "";
      };
   }

   private static boolean safeForceUnlock() {
      try {
         return (Boolean)Config.FORCE_NEXUS_UNLOCK.get();
      } catch (IllegalStateException var1) {
         return false;
      }
   }
}
