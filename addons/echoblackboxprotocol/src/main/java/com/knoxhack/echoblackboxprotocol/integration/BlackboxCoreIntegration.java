package com.knoxhack.echoblackboxprotocol.integration;

import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEnding;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxProgress;
import com.knoxhack.echoblackboxprotocol.progression.MemoryType;
import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoHazardTelemetry;
import com.knoxhack.echocore.api.EchoHandoffs;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker.Severity;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public final class BlackboxCoreIntegration {
   private static final String CHAPTER_ID = "blackbox_protocol";
   private static final String STATIONFALL_HANDOFF = EchoHandoffs.STATIONFALL_BLACKBOX_RECOVERED;
   private static final String NEXUS_PROTOCOL_HANDOFF = EchoHandoffs.NEXUS_PROTOCOL_COMPLETE;

   private BlackboxCoreIntegration() {
   }

   public static void registerAddonChapter() {
      if (!EchoAddonRegistry.isRegistered("blackbox_protocol")) {
         EchoAddonRegistry.register(new EchoAddonChapter() {
            public String id() {
               return "blackbox_protocol";
            }

            public String modId() {
               return EchoBlackboxProtocol.MODID;
            }

            public String displayName() {
               return "ECHO: Blackbox Protocol";
            }

            public String summary() {
               return "Final chapter: typed memory fragments, blackbox archives, monolith dungeons, memory bosses, Core keys, and endings.";
            }

            public boolean isAvailable(Player player) {
               return BlackboxCoreIntegration.sagaGateOpen(player);
            }

            public String statusLine(Player player) {
               if (!BlackboxCoreIntegration.sagaGateOpen(player)) {
                  return "BLACKBOX: waiting on detectable late-game saga handoff.";
               } else {
                  BlackboxProgress progress = BlackboxProgress.get(player);
                  return "BLACKBOX: " + progress.decodedMemoryTotal() + " memories decoded, ending " + progress.ending().displayName() + ".";
               }
            }
         });
      }

      EchoCoreServices.registerRouteRecordService(BlackboxCoreIntegration::routeRecords);
      EchoCoreServices.registerDiagnosticService(BlackboxCoreIntegration::diagnostics);
      EchoCoreServices.registerHazardTelemetryService(BlackboxCoreIntegration::hazardTelemetry);
      BlackboxIndexProvider.register();
      EchoBlackboxProtocol.LOGGER.info("ECHO platform providers after Blackbox setup: {}", EchoCoreServices.platformProviderSummary());
   }

   public static void mirrorDecodedMemory(ServerPlayer player, MemoryType type, boolean firstOfType) {
      EchoCoreServices.unlockArchive(player, EchoBlackboxProtocol.MODID + ":" + type.getSerializedName() + "_memory");
      EchoCoreServices.recordMilestone(player, "echoblackboxprotocol:decoded_" + type.getSerializedName());
      if (firstOfType) {
         EchoCoreServices.mirrorIntel(player, EchoBlackboxProtocol.MODID, "blackbox_" + type.getSerializedName(), type.displayName(), memorySummary(type));
      }
   }

   public static void recordBossDefeat(Player player, String id, String title) {
      if (player instanceof ServerPlayer serverPlayer) {
         EchoCoreServices.recordMilestone(serverPlayer, "echoblackboxprotocol:boss_" + id);
         EchoCoreServices.mirrorIntel(
            serverPlayer, EchoBlackboxProtocol.MODID, "boss_" + id, title, "Blackbox boss proof recovered. Duplicate-safe key drop recorded by player progress."
         );
      }
   }

   public static void recordEnding(ServerPlayer player, BlackboxEnding ending) {
      EchoCoreServices.recordMilestone(player, "echoblackboxprotocol:ending_" + ending.getSerializedName());
      EchoCoreServices.mirrorIntel(
         player, EchoBlackboxProtocol.MODID, "ending_" + ending.getSerializedName(), "Blackbox Ending: " + ending.displayName(), ending.finalLine()
      );
   }

   private static List<EchoRouteRecord> routeRecords(Player player) {
      if (player == null) {
         return List.of();
      } else {
         BlackboxProgress progress = BlackboxProgress.get(player);
         List<EchoRouteRecord> records = new ArrayList<>();

         for (BlackboxDungeon dungeon : BlackboxDungeon.values()) {
            boolean complete = progress.completed(dungeon) || dungeon == BlackboxDungeon.CORE_CHAMBER && progress.bossDefeated("nexus_guardian");
            boolean open = progress.canEnter(dungeon);
            records.add(
               new EchoRouteRecord(
                  id("route_" + dungeon.getSerializedName()),
                  "blackbox_protocol",
                  dungeon.displayName(),
                  dungeon.category(),
                  EchoBlackboxProtocol.MODID + ":" + dungeon.getSerializedName(),
                  complete ? "COMPLETE" : (open ? "OPEN" : "LOCKED"),
                  complete ? "Blackbox route proof recovered." : (open ? "Monolith route can open." : progress.lockReason(dungeon)),
                  complete
               )
            );
         }

         return records;
      }
   }

   private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
      if (player == null) {
         return List.of();
      } else {
         BlackboxProgress progress = BlackboxProgress.get(player);
         List<EchoDiagnosticBlocker> blockers = new ArrayList<>();
         if (!sagaGateOpen(player)) {
            blockers.add(
               blocker(
                  "blackbox_saga_gate",
                  Severity.BLOCKED,
                  "Blackbox handoff locked",
                  "A detectable late-game handoff is still unresolved.",
                  "Complete Ashfall/Orbital or use standalone config/progression before opening Blackbox routes."
               )
            );
         }

         if (progress.stability() <= 25) {
            blockers.add(
               blocker(
                  "blackbox_memory_unstable",
                  Severity.WARNING,
                  "Memory stability critical",
                  "Decoded memory is causing hallucination damage and false signals.",
                  "Use a Memory Stabilizer with Static Fluid or the Memory Stabilizer Core."
               )
            );
         }

         if (!progress.hasNexusCoreAccessKey()) {
            blockers.add(
               blocker(
                  "blackbox_key_incomplete",
                  Severity.INFO,
                  "Nexus Core Access Key incomplete",
                  "Core chamber route needs assembled boss proof and Core Logs.",
                  "Defeat False ECHO and Command Remnant, then use the Core Key Assembler."
               )
            );
         }

         return blockers;
      }
   }

   private static EchoHazardTelemetry hazardTelemetry(Player player) {
      if (player == null) {
         return EchoHazardTelemetry.nominal();
      } else {
         BlackboxProgress progress = BlackboxProgress.get(player);
         int falseSignal = Math.min(100, progress.falseSignalCount() * 8);
         int stabilityPressure = Math.max(0, 100 - progress.stability());
         return new EchoHazardTelemetry(
            100,
            0,
            0,
            100,
            100,
            0,
            0,
            Math.max(falseSignal, stabilityPressure),
            "Blackbox memory stability " + progress.stability() + "% | false signals " + progress.falseSignalCount()
         );
      }
   }

   public static boolean sagaGateOpenForTest(Player player) {
      return sagaGateOpen(player);
   }

   public static boolean sagaGateOpenForTerminal(Player player) {
      return sagaGateOpen(player);
   }

   public static String sagaGateReason(Player player) {
      if (sagaGateOpen(player)) {
         return "";
      }
      return "Blackbox waits for late-game handoffs: "
         + STATIONFALL_HANDOFF
         + " and "
         + NEXUS_PROTOCOL_HANDOFF
         + ". Legacy Stationfall/Nexus path milestones are accepted by Echo Core aliases.";
   }

   public static boolean sagaGateDecisionForTest(
      boolean orbitalLoaded,
      Boolean orbitalFinalNetworkSealed,
      boolean stationfallLoaded,
      boolean stationfallMilestone,
      boolean nexusProtocolLoaded,
      boolean nexusProtocolMilestone
   ) {
      return sagaGateSatisfied(orbitalLoaded, orbitalFinalNetworkSealed, stationfallLoaded, stationfallMilestone, nexusProtocolLoaded, nexusProtocolMilestone);
   }

   private static boolean sagaGateOpen(Player player) {
      if (player == null) {
         return true;
      } else {
         boolean orbitalLoaded = EchoRuntimeModules.isLoaded("echoorbitalremnants");
         Boolean orbital = orbitalLoaded ? orbitalFinalNetworkSealed(player) : null;
         boolean stationfallLoaded = EchoRuntimeModules.isLoaded("echostationfall");
         boolean stationfall = EchoCoreServices.progressLedger(player).hasMilestone(STATIONFALL_HANDOFF);
         boolean nexusProtocolLoaded = EchoRuntimeModules.isLoaded("echonexusprotocol");
         boolean nexusProtocol = EchoCoreServices.progressLedger(player).hasMilestone(NEXUS_PROTOCOL_HANDOFF);
         return sagaGateSatisfied(orbitalLoaded, orbital, stationfallLoaded, stationfall, nexusProtocolLoaded, nexusProtocol);
      }
   }

   private static boolean sagaGateSatisfied(
      boolean orbitalLoaded,
      Boolean orbitalFinalNetworkSealed,
      boolean stationfallLoaded,
      boolean stationfallMilestone,
      boolean nexusProtocolLoaded,
      boolean nexusProtocolMilestone
   ) {
      if (orbitalLoaded && Boolean.FALSE.equals(orbitalFinalNetworkSealed)) {
         return false;
      } else if (stationfallLoaded && !stationfallMilestone) {
         return false;
      } else {
         return !nexusProtocolLoaded || nexusProtocolMilestone;
      }
   }

   private static Boolean orbitalFinalNetworkSealed(Player player) {
      try {
         Class<?> progressClass = Class.forName("com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress");
         Object progress = progressClass.getMethod("get", Player.class).invoke(null, player);
         Method finalNetworkSealed = progressClass.getMethod("finalNetworkSealed");
         return Boolean.TRUE.equals(finalNetworkSealed.invoke(progress));
      } catch (RuntimeException | ReflectiveOperationException var4) {
         return null;
      }
   }

   private static EchoDiagnosticBlocker blocker(String path, Severity severity, String title, String detail, String nextAction) {
      return new EchoDiagnosticBlocker(id(path), "blackbox_protocol", severity, title, detail, nextAction);
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoBlackboxProtocol.MODID, path);
   }

   private static String memorySummary(MemoryType type) {
      return switch (type) {
         case PERSONAL -> "Human survival logs reconstructed from the Collapse archive.";
         case ECHO -> "ECHO-7 origin fragments recovered, including corrupted helper behavior.";
         case SECURITY -> "Facility event logs expose containment and vault failures.";
         case COMMAND -> "Military command logs identify who treated extinction as obedience.";
         case CORE -> "Nexus behavior logs explain Core route pressure and final access.";
         case DELETED -> "Deleted logs expose hidden routes and the Merge ending precondition.";
      };
   }
}
