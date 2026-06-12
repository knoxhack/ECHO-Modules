package com.knoxhack.echoblackboxprotocol.integration;

import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import com.echoplatform.echocore.api.EchoRuntimeModules;

public final class BlackboxTerminalCommonIntegration {
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

   private BlackboxTerminalCommonIntegration() {
   }

   public static void register() {
      if (REGISTERED.compareAndSet(false, true)) {
         boolean missionCoreLoaded = EchoRuntimeModules.isLoaded("echomissioncore");
         if (missionCoreLoaded) {
            BlackboxMissionCoreIntegration.register();
         } else {
            TerminalMissionRegistry.register(BlackboxMissionProvider.INSTANCE);
         }
         TerminalMissionActions.registerForTab(BlackboxTerminalIds.ACCESS_TAB);
         TerminalMissionActions.registerForTab(BlackboxTerminalIds.ARCHIVE_TAB);
         TerminalMissionActions.registerForTab(BlackboxTerminalIds.TRUTH_TAB);
         registerArchiveEntries();
      }
   }

   private static void registerArchiveEntries() {
      TerminalArchiveRegistry.register(
         new TerminalArchiveEntry(
            BlackboxTerminalIds.id("blackbox_protocol_manual"),
            "BLACKBOX",
            "Blackbox Protocol Manual",
            "OPEN",
            List.of(
               "Blackbox Fragments are typed memory pieces: Personal, ECHO, Security, Command, Core, and Deleted.",
               "Decode fragments at the Blackbox Decoder, then use monolith route records to open each late-game dungeon.",
               "The Core Key Assembler expects boss proof from False ECHO and the Command Remnant before the Nexus Core Chamber route can open."
            ),
            false
         )
      );
      TerminalArchiveRegistry.register(
         new TerminalArchiveEntry(
            BlackboxTerminalIds.id("truth_engine_context"),
            "BLACKBOX",
            "Truth Engine Context",
            "LOCKED BY CHOICE",
            List.of(
               "Restore stabilizes the Nexus and makes recovery possible.",
               "Control strengthens machines and upgrades protocol authority, but ECHO-7 records distrust.",
               "Destroy ends corruption spread at the cost of a harsher natural world.",
               "Merge requires Deleted Logs and every boss proof item. It is not a clean ending."
            ),
            false
         )
      );
   }
}
