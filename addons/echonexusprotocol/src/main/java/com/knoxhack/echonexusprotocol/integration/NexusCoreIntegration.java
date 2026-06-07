package com.knoxhack.echonexusprotocol.integration;

import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoHazardTelemetry;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echocore.api.NexusCampaignService;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker.Severity;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class NexusCoreIntegration {
   private static final String CHAPTER_ID = "nexus_protocol";
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

   private NexusCoreIntegration() {
   }

   public static void register() {
      if (REGISTERED.compareAndSet(false, true)) {
         if (!EchoAddonRegistry.isRegistered("nexus_protocol")) {
            EchoAddonRegistry.register(
               new EchoAddonChapter() {
                  public String id() {
                     return "nexus_protocol";
                  }

                  public String modId() {
                     return "echonexusprotocol";
                  }

                  public String displayName() {
                     return "ECHO: Nexus Protocol";
                  }

                  public String summary() {
                     return "Chapter IV: Nexus Charge, field stability, corruption containment, matter rewriting, Blackbox Monolith, and Core endings.";
                  }

                  public boolean isAvailable(Player player) {
                     return NexusProgression.isNexusUnlocked(player);
                  }

                  public String statusLine(Player player) {
                     if (!this.isAvailable(player)) {
                        return "NEXUS PROTOCOL: Locked until " + NexusProgression.STATIONFALL_GATE + " or development unlock.";
                     } else {
                        NexusPlayerData data = NexusPlayerData.get(player);
                        return data.hasEndingPath()
                           ? "NEXUS PROTOCOL: Path " + data.endingPath().toUpperCase(Locale.ROOT) + " committed."
                           : "NEXUS PROTOCOL: Nexus signal available. Field reality is editable.";
                     }
                  }
               }
            );
         }

         EchoCoreServices.registerNexusPathService(player -> player != null && NexusPlayerData.get(player).hasEndingPath());
         EchoCoreServices.registerNexusCampaignService(new NexusCoreIntegration.NexusCampaign());
         EchoCoreServices.registerHazardTelemetryService(NexusCoreIntegration::hazardTelemetry);
         EchoCoreServices.registerDiagnosticService(NexusCoreIntegration::diagnostics);
         EchoCoreServices.registerRouteRecordService(NexusCoreIntegration::routeRecords);
         NexusIndexProvider.register();
         EchoNexusProtocol.LOGGER.info("ECHO platform providers after Nexus setup: {}", EchoCoreServices.platformProviderSummary());
      }
   }

   private static EchoHazardTelemetry hazardTelemetry(Player player) {
      if (player != null && player.level() instanceof ServerLevel serverLevel) {
         NexusWorldData data = NexusWorldData.get(serverLevel);
         int field = data.fieldValue(player.chunkPosition());
         int corruption = data.corruptionPressure(player.chunkPosition());
         return new EchoHazardTelemetry(
            field,
            0,
            0,
            100,
            100,
            corruption,
            Math.max(0, 100 - field),
            0,
            "Nexus field " + data.fieldState(player.chunkPosition()) + ", corruption " + corruption + "%."
         );
      } else {
         return EchoHazardTelemetry.nominal();
      }
   }

   private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
      if (player == null) {
         return List.of();
      } else if (!NexusProgression.isNexusUnlocked(player)) {
         return List.of(
            blocker(
               "nexus_stationfall_gate",
               Severity.BLOCKED,
               "Nexus Protocol locked",
               "The Core refuses clean access until the Stationfall blackbox is recovered.",
               "Complete " + NexusProgression.STATIONFALL_GATE + " or use /nexusprotocol unlock during development."
            )
         );
      } else {
         if (player.level() instanceof ServerLevel serverLevel) {
            NexusWorldData data = NexusWorldData.get(serverLevel);
            if (data.fieldValue(player.chunkPosition()) < 40) {
               return List.of(
                  blocker(
                     "nexus_field_fractured",
                     Severity.WARNING,
                     "Local Nexus Field fractured",
                     "Reality stability is low enough for corruption spread and anomaly behavior.",
                     "Deploy Field Stabilizers or Purify/Quarantine Protocol Seals."
                  )
               );
            }
         }

         return List.of();
      }
   }

   private static List<EchoRouteRecord> routeRecords(Player player) {
      if (player == null) {
         return List.of();
      } else {
         NexusPlayerData data = NexusPlayerData.get(player);
         String dimension = player.level().dimension().identifier().toString();
         return List.of(
            route(
               "nexus_signal_discovery",
               "Signal Discovery",
               "Research",
               dimension,
               data.hasResearch("nexus_theory") ? "SCANNED" : "LOCKED",
               "Scanner visor, shard scan, and first charge telemetry.",
               data.hasResearch("nexus_theory")
            ),
            route(
               "nexus_blackbox_monolith",
               "Blackbox Monolith",
               "Memory",
               "Nexus",
               data.blackboxMonolithActivated() ? "ACTIVATED" : "SEALED",
               "The Monolith proves the Nexus had instructions.",
               data.blackboxMonolithActivated()
            ),
            route(
               "nexus_final_path",
               "Core Ending",
               "Endgame",
               "Nexus Core Chamber",
               data.hasEndingPath() ? data.endingPath().toUpperCase(Locale.ROOT) : "UNRESOLVED",
               "Restore, Control, Destroy, or Merge with the Nexus Core.",
               data.hasEndingPath()
            )
         );
      }
   }

   private static EchoRouteRecord route(String path, String title, String category, String dimension, String status, String summary, boolean complete) {
      return new EchoRouteRecord(id(path), "nexus_protocol", title, category, dimension, status, summary, complete);
   }

   private static EchoDiagnosticBlocker blocker(String path, Severity severity, String title, String detail, String nextAction) {
      return new EchoDiagnosticBlocker(id(path), "nexus_protocol", severity, title, detail, nextAction);
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("echonexusprotocol", path);
   }

   private static final class NexusCampaign implements NexusCampaignService {
      public String pathId(Player player) {
         return player == null ? "" : NexusPlayerData.get(player).endingPath();
      }

      public int instability(Player player) {
         return player != null && player.level() instanceof ServerLevel serverLevel
            ? Math.max(0, 100 - NexusWorldData.get(serverLevel).fieldValue(player.chunkPosition()))
            : 0;
      }

      public boolean isWarfrontComplete(Player player) {
         return player != null && NexusPlayerData.get(player).blackboxMonolithActivated();
      }

      public boolean isFinalProtocolComplete(Player player) {
         return player != null && NexusPlayerData.get(player).hasEndingPath();
      }

      public List<String> relaySummary(Player player) {
         if (player == null) {
            return List.of("No Nexus signal.");
         } else {
            NexusPlayerData data = NexusPlayerData.get(player);
            return List.of(
               "Research tabs: " + data.researchUnlocks().size() + "/6",
               "Scans: " + data.scanCount(),
               "Blackbox fragments: " + data.blackboxFragments(),
               "Monolith: " + (data.blackboxMonolithActivated() ? "activated" : "sealed"),
               "Ending: " + (data.hasEndingPath() ? data.endingPath() : "unresolved")
            );
         }
      }

      public boolean isFinalBossDefeated(Player player) {
         return player != null && NexusPlayerData.get(player).guardianDefeated();
      }

      public String statusLine(Player player) {
         if (player == null) {
            return "Nexus campaign signal unavailable.";
         } else if (!NexusProgression.isNexusUnlocked(player)) {
            return "Nexus Protocol locked behind Stationfall blackbox recovery.";
         } else {
            NexusPlayerData data = NexusPlayerData.get(player);
            return data.hasEndingPath()
               ? "Nexus Core path committed: " + data.endingPath()
               : "Nexus campaign active: field containment and Blackbox route pending.";
         }
      }
   }
}
