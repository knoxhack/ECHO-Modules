package com.knoxhack.echonexusprotocol.integration;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerPlayer;

public final class NexusTerminalCommonIntegration {
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

   private NexusTerminalCommonIntegration() {
   }

   public static void register() {
      if (REGISTERED.compareAndSet(false, true)) {
         boolean missionCoreLoaded = EchoRuntimeModules.isLoaded("echomissioncore");
         if (missionCoreLoaded) {
            NexusMissionCoreIntegration.register();
         } else {
            TerminalMissionRegistry.register(NexusTerminalMissionProvider.INSTANCE);
         }
         TerminalMissionActions.registerForTab(NexusTerminalIds.RESEARCH_TAB);
         TerminalActionRegistry.register(NexusTerminalIds.RESEARCH_TAB, NexusTerminalIds.SCAN_ACTION, (player, payload) -> {
            NexusPlayerData data = NexusPlayerData.get(player);
            data.unlockResearch("nexus_theory");
            if (player instanceof ServerPlayer) {
               NexusPlayerData.saveAndSync(player, data);
            }
         });
         registerArchiveEntries();
      }
   }

   private static void registerArchiveEntries() {
      TerminalArchiveRegistry.register(
         new TerminalArchiveEntry(
            NexusTerminalIds.id("nexus_charge_field_notes"),
            "ECHO-7",
            "Nexus Charge Field Notes",
            "OPEN",
            List.of(
               "Nexus Charge is stable machine energy extracted from broken matter, shards, and data-bearing salvage.",
               "Corruption is separate from charge. It is not fuel until forbidden systems choose to burn reality itself.",
               "Field Stabilizers, Filters, and Protocol Seals are the difference between research and another Collapse."
            ),
            false
         )
      );
      TerminalArchiveRegistry.register(
         new TerminalArchiveEntry(
            NexusTerminalIds.id("blackbox_monolith_warning"),
            "ECHO-7",
            "Blackbox Monolith Warning",
            "SEALED",
            List.of(
               "The Monolith route unlocks when the player activates a Blackbox structure.",
               "The Nexus did not cause the Collapse alone. Someone gave it instructions."
            ),
            false
         )
      );
   }
}
