package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoDiagnosticService;
import com.knoxhack.echocore.api.EchoRecoveryService;
import com.knoxhack.echocore.api.EchoRouteRecord;
import com.knoxhack.echocore.api.EchoRouteRecordService;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationMetrics;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ReclamationCoreIntegration {
   public static final String CHAPTER_ID = "agriculture_reclamation";
   private static final String RECOVERY_SEED_CACHE = "agriculture_seed_cache";
   private static final String RECOVERY_SEED_CACHE_CLAIM = "recovery_agriculture_seed_cache";
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final EchoRouteRecordService ROUTE_SERVICE = ReclamationCoreIntegration::routeRecords;
   private static final EchoDiagnosticService DIAGNOSTIC_SERVICE = ReclamationCoreIntegration::diagnostics;
   private static final EchoRecoveryService RECOVERY_SERVICE = ReclamationCoreIntegration::recover;
   private static final List<CoreMission> CORE_MISSIONS = List.of(
      new CoreMission("recover_seed", "Recover Seed", "Recover or open a seed capsule from ruined ecology sources.", "Seed Recovery", "Seed", "Craft a capsule from wheat seeds, bone meal, glass bottle, and copper, or search ruined ecology caches."),
      new CoreMission("analyze_soil", "Analyze Soil", "Scan contaminated ground and run the first purification pass.", "Soil Recovery", "Soil", "Use Ecology Scanner or Soil Purifier near dead ecology blocks."),
      new CoreMission("first_growth", "First Growth", "Grow and harvest a recovered crop in soil or hydroponics.", "Cultivation", "Growth", "Plant a profiled seed on supported soil or insert it into a Hydroponic Tray."),
      new CoreMission("gene_stabilization", "Gene Stabilization", "Stabilize one contaminated seed route.", "Cultivation", "Genes", "Use Bio-Reactor crop output to make Bio-Gel, then use Gene Stabilizer with a contaminated seed and Bio-Gel or Gene Sample."),
      new CoreMission("greenhouse_online", "Greenhouse Online", "Build a greenhouse zone that reaches safe growth envelope.", "Greenhouse", "Safety", "Use Greenhouse Glass, Spore Filter, Pollinator Dock, trays, a controller scan, and deploy the dock drone for active crop service."),
      new CoreMission("restore_chunk", "Restore a Chunk", "Raise local restoration score to 100 through crops, restored soil, and safe greenhouse support.", "Restoration", "Restoration", "Mature restoration crops and keep scanning ecology while soil improves.")
   );

   private ReclamationCoreIntegration() {
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
               return EchoAgricultureReclamation.MODID;
            }

            @Override
            public String displayName() {
               return "ECHO: Agriculture Reclamation";
            }

            @Override
            public String summary() {
               return "Ruined-world agriculture chapter for recovered seeds, purified soil, hydroponics, greenhouses, and chunk-local ecological restoration.";
            }

            @Override
            public boolean isAvailable(Player player) {
               return player != null;
            }

            @Override
            public String statusLine(Player player) {
               if (player == null) {
                  return "AGRICULTURE: telemetry offline until player context is available.";
               }
               ReclamationMetrics metrics = ReclamationProgress.metrics(player);
               return "AGRICULTURE: seeds " + metrics.knownSeeds() + "/" + com.knoxhack.echoagriculturereclamation.content.CropSpec.ALL.size()
                  + ", food " + metrics.foodSecurity() + "%, stability " + metrics.cropStability()
                  + "%, restoration " + metrics.restorationScore() + "%.";
            }
         });
      }
      EchoCoreServices.registerRouteRecordService(ROUTE_SERVICE);
      EchoCoreServices.registerDiagnosticService(DIAGNOSTIC_SERVICE);
      EchoCoreServices.registerRecoveryService(RECOVERY_SERVICE);
      EchoCoreServices.registerIndexContentProvider(ReclamationIndexProvider.INSTANCE);
      EchoCoreServices.registerMapDataProvider(ReclamationMapDataProvider.INSTANCE);
      EchoAgricultureReclamation.LOGGER.info("ECHO Agriculture Reclamation core chapter registered.");
   }

   private static List<EchoRouteRecord> routeRecords(Player player) {
      List<EchoRouteRecord> records = new ArrayList<>();
      for (CoreMission mission : CORE_MISSIONS) {
         float progress = progress(player, mission.key());
         boolean complete = progress >= 1.0F;
         boolean claimed = player != null && ReclamationProgress.claimed(player, mission.key());
         records.add(new EchoRouteRecord(
            routeId(mission),
            CHAPTER_ID,
            mission.title(),
            mission.category(),
            "Overworld / Field Reclamation",
            claimed ? "CLAIMED" : complete ? "CACHE READY" : progress > 0.0F ? "ACTIVE" : "PENDING",
            (complete ? "Complete. " : Math.round(progress * 100.0F) + "% complete. ") + mission.briefing(),
            complete || claimed
         ));
      }
      return List.copyOf(records);
   }

   private static List<EchoDiagnosticBlocker> diagnostics(Player player) {
      if (player == null) {
         return List.of();
      }
      for (CoreMission mission : CORE_MISSIONS) {
         float progress = progress(player, mission.key());
         if (progress < 1.0F && !ReclamationProgress.claimed(player, mission.key())) {
            return List.of(new EchoDiagnosticBlocker(
               diagnosticId(mission),
               CHAPTER_ID,
               progress > 0.0F ? EchoDiagnosticBlocker.Severity.INFO : EchoDiagnosticBlocker.Severity.BLOCKED,
               mission.title(),
               mission.briefing(),
               mission.nextAction()
            ));
         }
      }
      return List.of();
   }

   private static boolean recover(ServerPlayer player, String recoveryId) {
      if (player == null || recoveryId == null || !recoveryId.equals(RECOVERY_SEED_CACHE)) {
         return false;
      }
      if (ReclamationProgress.claimed(player, RECOVERY_SEED_CACHE_CLAIM)) {
         player.sendSystemMessage(Component.literal("ECHO FIELD // Agriculture seed recovery cache already claimed."));
         return false;
      }
      ItemStack capsule = new ItemStack(ModItems.RECOVERED_SEED_CAPSULE.get(), 2);
      if (!player.getInventory().add(capsule)) {
         player.drop(capsule, false);
      }
      ReclamationProgress.claim(player, RECOVERY_SEED_CACHE_CLAIM);
      ReclamationProgress.mark(player, "seed_recovered");
      EchoCoreServices.discoverVisibleRouteRecords(player);
      player.sendSystemMessage(Component.literal("ECHO FIELD // Agriculture seed recovery cache restored: 2x Recovered Seed Capsule."));
      return true;
   }

   private static float progress(Player player, String key) {
      if (player == null) {
         return 0.0F;
      }
      ReclamationMetrics metrics = ReclamationProgress.metrics(player);
      return switch (key) {
         case "recover_seed" -> ReclamationProgress.flag(player, "seed_recovered") || metrics.knownSeeds() > 0 ? 1.0F : 0.0F;
         case "analyze_soil" -> ReclamationProgress.flag(player, "soil_analyzed") || ReclamationProgress.value(player, "soil_purified") > 0 ? 1.0F : 0.0F;
         case "first_growth" -> Math.min(1.0F, ReclamationProgress.value(player, "crops_grown"));
         case "gene_stabilization" -> ReclamationProgress.flag(player, "gene_stabilization") ? 1.0F : 0.0F;
         case "greenhouse_online" -> Math.min(1.0F, metrics.greenhouseSafety() / (float)Math.max(1, ReclamationContent.progression().greenhouseSafeThreshold()));
         case "restore_chunk" -> Math.min(1.0F, metrics.restorationScore() / (float)Math.max(1, ReclamationContent.progression().restoreThreshold()));
         default -> 0.0F;
      };
   }

   private static Identifier routeId(CoreMission mission) {
      return ReclamationTerminalIds.id("route/" + mission.key());
   }

   private static Identifier diagnosticId(CoreMission mission) {
      return ReclamationTerminalIds.id("diagnostic/" + mission.key());
   }

   private record CoreMission(String key, String title, String briefing, String phase, String category, String nextAction) {
   }
}
