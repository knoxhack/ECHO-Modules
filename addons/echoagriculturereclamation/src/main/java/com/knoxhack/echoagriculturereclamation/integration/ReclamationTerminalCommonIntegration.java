package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.chat.Component;

public final class ReclamationTerminalCommonIntegration {
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

   private ReclamationTerminalCommonIntegration() {
   }

   public static void register() {
      if (!REGISTERED.compareAndSet(false, true)) {
         return;
      }
      boolean missionCoreLoaded = EchoRuntimeModules.isLoaded("echomissioncore");
      if (missionCoreLoaded) {
         ReclamationMissionCoreIntegration.register();
      } else {
         TerminalMissionRegistry.register(ReclamationMissionProvider.INSTANCE);
      }
      TerminalMissionActions.registerForTab(ReclamationTerminalIds.FIELD_TAB);
      TerminalActionRegistry.register(ReclamationTerminalIds.FIELD_TAB, ReclamationTerminalIds.SCAN_ACTION, (player, payload) -> {
         player.sendSystemMessage(Component.literal(ReclamationTerminalReport.summary(player)));
      });
      TerminalActionRegistry.register(ReclamationTerminalIds.FIELD_TAB, ReclamationTerminalIds.REPORT_ACTION, (player, payload) -> {
         player.sendSystemMessage(Component.literal(ReclamationTerminalReport.routeReport(player)));
      });
      registerArchives();
      EchoAgricultureReclamation.LOGGER.info("ECHO Agriculture Reclamation terminal chapter registered.");
   }

   private static void registerArchives() {
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(
         ReclamationTerminalIds.id("archive/seed_recovery"),
         "Agriculture Reclamation",
         "Seed Recovery",
         "OPEN",
         List.of(
            "Recovered seed capsules carry crop identity, contamination tier, and stability.",
            "Seed Vault Terminals identify capsules and record known recovered seeds.",
            "Hydroponic trays preserve a reusable seed culture; crouch-use extracts it if you need to move the route.",
            "Stabilized seeds are safer, but still depend on soil, greenhouse zones, or hydroponics."
         ),
         false
      ));
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(
         ReclamationTerminalIds.id("archive/restoration"),
         "Agriculture Reclamation",
         "Chunk Restoration",
         "OPEN",
         List.of(
            "Restoration is chunk-local and block-local. Agriculture Reclamation does not rewrite biome ids.",
            "Mature restoration crops, restored soil, safe greenhouse zones, and Ecology Scanner reports raise the local score.",
            "Higher scores convert nearby dead, contaminated, irradiated, and toxic soil toward purified, stabilized, then restored."
         ),
         false
      ));
   }
}
