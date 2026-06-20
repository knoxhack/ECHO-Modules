package com.knoxhack.echoagriculturereclamation.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationMetrics;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class ReclamationMissionProvider implements TerminalMissionProvider {
   public static final ReclamationMissionProvider INSTANCE = new ReclamationMissionProvider();
   public static final String ACTION_SCAN = "scan_reclamation";
   public static final String ACTION_REPORT = "field_report";
   public static final String ACTION_CLAIM = "claim_cache";
   private static final int ACCENT = 0xFF92F7A6;
   private static final List<Mission> MISSIONS = List.of(
      mission("recover_seed", "Recover Seed", "Recover or open a seed capsule from ruined ecology sources.", "Seed Recovery", 0, "Seed", "Craft one from wheat seeds, bone meal, glass bottle, and copper, or search Seed Vaults, Bio Labs, caches, toxic salvage, and cryogenic ruins.", 1, () -> stack(() -> (ItemLike) ModItems.RECOVERED_SEED_CAPSULE.get(), Items.WHEAT_SEEDS, 1)),
      mission("analyze_soil", "Analyze Soil", "Scan contaminated ground and run the first purification pass.", "Soil Recovery", 1, "Soil", "Use Ecology Scanner or Soil Purifier near dead ecology blocks.", 1, () -> stack(() -> (ItemLike) ModBlocks.SOIL_PURIFIER.get(), Items.COMPOSTER, 1)),
      mission("first_growth", "First Growth", "Grow and harvest a recovered crop in soil or hydroponics.", "Cultivation", 2, "Growth", "Plant a profiled seed on supported soil or insert it into a reusable Hydroponic Tray culture.", 1, () -> stack(() -> (ItemLike) ModBlocks.HYDROPONIC_TRAY.get(), Items.FLOWER_POT, 1)),
      mission("gene_stabilization", "Gene Stabilization", "Stabilize one contaminated seed route.", "Cultivation", 3, "Genes", "Craft a Bio-Reactor with Soil Nutrient Mix, make Bio-Gel from crop matter, then use a contaminated seed cutting in the Gene Stabilizer.", 1, () -> stack(() -> (ItemLike) ModBlocks.GENE_STABILIZER.get(), Items.BREWING_STAND, 1)),
      mission("greenhouse_online", "Greenhouse Online", "Build a greenhouse zone that reaches safe growth envelope.", "Greenhouse", 4, "Safety", "Use Greenhouse Glass, Spore Filter, Pollinator Dock, trays, and a controller scan to establish the zone; deploy the dock drone for active crop service.", 70, () -> stack(() -> (ItemLike) ModBlocks.GREENHOUSE_CONTROLLER.get(), Items.GLASS, 1)),
      mission("restore_chunk", "Restore a Chunk", "Raise local restoration score to 100 through crops, restored soil, and safe greenhouse support.", "Restoration", 5, "Restoration", "Mature restoration crops and keep scanning ecology while soil improves.", 100, () -> stack(() -> (ItemLike) ModBlocks.RESTORED_SOIL.get(), Items.DIRT, 1))
   );

   private ReclamationMissionProvider() {
   }

   @Override
   public TerminalMissionChapter chapter() {
      return new TerminalMissionChapter(ReclamationTerminalIds.CHAPTER, "FIELD > Reclamation", "Seed recovery, greenhouse safety, crop stability, food security, and restoration score.", 240, ACCENT, true);
   }

   @Override
   public List<TerminalMissionDefinition> missions(Player player) {
      return MISSIONS.stream().map(mission -> definition(player, mission)).toList();
   }

   @Override
   public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
      Mission mission = mission(missionId);
      if (mission == null) {
         return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F, "LOCKED", "Unknown Reclamation mission.", "Run FIELD scan.", List.of());
      }
      float progress = progress(player, mission.key());
      boolean complete = progress >= 1.0F;
      boolean claimed = player != null && ReclamationProgress.claimed(player, mission.key());
      boolean unlocked = player != null && unlocked(player, mission);
      TerminalMissionStatus status = claimed ? TerminalMissionStatus.CLAIMED : complete ? TerminalMissionStatus.CLAIMABLE : unlocked ? TerminalMissionStatus.UNLOCKED : TerminalMissionStatus.LOCKED;
      String detail = player == null ? "Telemetry offline." : detail(player, mission);
      return new TerminalMissionSnapshot(
         mission.id(),
         status,
         progress,
         claimed ? "CLAIMED" : complete ? "CACHE READY" : unlocked ? "ACTIVE" : "LOCKED",
         detail,
         complete ? "Claim the Reclamation support cache." : unlocked ? ReclamationTerminalReport.nextStep(player) : "Complete the previous field milestone to unlock this record.",
         List.of(
            TerminalMissionAction.enabled(ACTION_SCAN, "SCAN RECLAMATION"),
            TerminalMissionAction.enabled(ACTION_REPORT, "FIELD REPORT"),
            claimed ? TerminalMissionAction.disabled(ACTION_CLAIM, "CLAIM CACHE", "Support cache already claimed.")
               : complete ? TerminalMissionAction.enabled(ACTION_CLAIM, "CLAIM CACHE")
               : TerminalMissionAction.disabled(ACTION_CLAIM, "CLAIM CACHE", "Complete this milestone first.")
         )
      );
   }

   @Override
   public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
      Mission mission = mission(missionId);
      if (mission == null) {
         return false;
      }
      if (ACTION_SCAN.equals(actionId)) {
         player.sendSystemMessage(Component.literal(ReclamationTerminalReport.summary(player)));
         EchoCoreServices.discoverVisibleRouteRecords(player);
         return true;
      }
      if (ACTION_REPORT.equals(actionId)) {
         player.sendSystemMessage(Component.literal(ReclamationTerminalReport.routeReport(player)));
         EchoCoreServices.discoverVisibleRouteRecords(player);
         return true;
      }
      if (!ACTION_CLAIM.equals(actionId) || progress(player, mission.key()) < 1.0F || ReclamationProgress.claimed(player, mission.key())) {
         return false;
      }
      List<ItemStack> rewards = rewards(mission);
      if (!EchoCoreServices.storeTerminalRewards(player, mission.id().toString(), rewards)) {
         for (ItemStack reward : rewards) {
            ItemStack copy = reward.copy();
            if (!player.getInventory().add(copy)) {
               player.drop(copy, false);
            }
         }
      }
      ReclamationProgress.claim(player, mission.key());
      EchoCoreServices.discoverVisibleRouteRecords(player);
      player.sendSystemMessage(Component.literal("ECHO FIELD // Reclamation support cache claimed for " + mission.title() + "."));
      return true;
   }

   @Override
   public TerminalMissionPresentation presentation(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
      Mission mission = definition == null ? null : mission(definition.id());
      if (mission == null) {
         return TerminalMissionProvider.super.presentation(player, definition, snapshot);
      }
      return new TerminalMissionPresentation(
         mission.title(),
         mission.briefing(),
         player == null ? mission.guide() : ReclamationTerminalReport.nextStep(player),
         mission.phase(),
         snapshot == null ? "neutral" : switch (snapshot.status()) {
            case CLAIMABLE, COMPLETED, CLAIMED -> "success";
            case UNLOCKED -> "active";
            case LOCKED, VIEW_ONLY -> "muted";
         },
         List.of("FIELD", mission.category()),
         "agriculture_reclamation/" + mission.key()
      );
   }

   @Override
   public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
      return TerminalMissionRole.OPTIONAL;
   }

   @Override
   public Optional<TerminalMissionRoutePlacement> routePlacement(
      Player player,
      TerminalMissionDefinition definition,
      TerminalMissionSnapshot snapshot,
      TerminalMissionRole role
   ) {
      int order = definition == null ? 0 : definition.missionOrder();
      return Optional.of(TerminalMissionRoutePlacement.optional(
         routePhase(definition == null ? null : definition.id()), order));
   }

   public static List<RouteMission> routeMissions() {
      return MISSIONS.stream()
         .map(mission -> new RouteMission(mission.id(), mission.key(), mission.title(), mission.briefing(), mission.phase(), mission.category(), mission.guide()))
         .toList();
   }

   public static float progress(Player player, String key) {
      if (player == null) {
         return 0.0F;
      }
      ReclamationMetrics metrics = ReclamationProgress.metrics(player);
      return switch (key) {
         case "recover_seed" -> ReclamationProgress.flag(player, "seed_recovered") || metrics.knownSeeds() > 0 ? 1.0F : 0.0F;
         case "analyze_soil" -> ReclamationProgress.flag(player, "soil_analyzed") || ReclamationProgress.value(player, "soil_purified") > 0 ? 1.0F : 0.0F;
         case "first_growth" -> ReclamationProgress.value(player, "crops_grown") > 0 ? 1.0F
            : ReclamationProgress.flag(player, "hydroponics_online") || ReclamationProgress.flag(player, "first_growth_started") ? 0.5F : 0.0F;
         case "gene_stabilization" -> geneProgress(player);
         case "greenhouse_online" -> ratio(metrics.greenhouseSafety(), ReclamationContent.progression().greenhouseSafeThreshold());
         case "restore_chunk" -> ratio(metrics.restorationScore(), ReclamationContent.progression().restoreThreshold());
         default -> 0.0F;
      };
   }

   private static TerminalMissionDefinition definition(Player player, Mission mission) {
      int need = requirementNeed(mission);
      int have = Math.round(progress(player, mission.key()) * need);
      boolean done = have >= need;
      return new TerminalMissionDefinition(
         mission.id(),
         ReclamationTerminalIds.CHAPTER,
         mission.phase().toLowerCase(java.util.Locale.ROOT).replace(' ', '_'),
         mission.phase(),
         mission.order(),
         mission.order(),
         mission.title(),
         mission.briefing(),
         mission.guide(),
         mission.category(),
         "Field",
         mission.icon(),
         List.of(),
         List.of(TerminalMissionRequirement.custom(mission.title(), have + "/" + need + " telemetry", mission.icon(), have, need, done)),
         rewards(mission).stream().map(TerminalMissionReward::of).toList()
      );
   }

   private static String detail(Player player, Mission mission) {
      ReclamationMetrics metrics = ReclamationProgress.metrics(player);
      ReclamationProgress.GreenhouseContext greenhouse = player == null
         ? ReclamationProgress.GreenhouseScan.empty().asContext()
         : ReclamationProgress.greenhouseContext(player.level(), player.blockPosition());
      return switch (mission.key()) {
         case "recover_seed" -> "Known seeds: " + metrics.knownSeeds() + "/" + CropSpec.ALL.size() + ".";
         case "analyze_soil" -> "Local soil: " + metrics.soilLabel() + ". Purified blocks: " + ReclamationProgress.value(player, "soil_purified") + ".";
         case "first_growth" -> "Crops harvested: " + ReclamationProgress.value(player, "crops_grown") + ". Food security " + metrics.foodSecurity() + "%.";
         case "gene_stabilization" -> "Stabilized seeds: " + ReclamationProgress.value(player, "stabilized_seeds")
            + ". Bio-Gel carried " + count(player, () -> ModItems.BIO_GEL.get())
            + ". Seed cutting " + (ReclamationProgress.flag(player, "stabilization_seed_recovered") ? "available" : "pending") + ".";
         case "greenhouse_online" -> "Greenhouse safety: " + metrics.greenhouseSafety() + "/" + ReclamationContent.progression().greenhouseSafeThreshold()
            + " (" + greenhouse.summaryLabel() + ", " + countLabel(greenhouse.scan().deployedDrones(), "drone") + ", "
            + countLabel(greenhouse.scan().serviceTargets(), "service target")
            + "). " + greenhouse.nextAction();
         case "restore_chunk" -> "Restoration score: " + metrics.restorationScore() + "/" + ReclamationContent.progression().restoreThreshold() + ".";
         default -> mission.briefing();
      };
   }

   private static boolean unlocked(Player player, Mission mission) {
      if (mission.order() <= 1) {
         return true;
      }
      return switch (mission.key()) {
         case "first_growth" -> progress(player, "recover_seed") >= 1.0F || progress(player, "analyze_soil") >= 1.0F;
         case "gene_stabilization" -> progress(player, "first_growth") >= 1.0F;
         case "greenhouse_online" -> progress(player, "gene_stabilization") >= 1.0F;
         case "restore_chunk" -> progress(player, "greenhouse_online") >= 1.0F;
         default -> true;
      };
   }

   private static float geneProgress(Player player) {
      if (ReclamationProgress.flag(player, "gene_stabilization")) {
         return 1.0F;
      }
      float progress = 0.0F;
      if (ReclamationProgress.value(player, "crops_grown") > 0) {
         progress = 0.35F;
      }
      if (ReclamationProgress.flag(player, "stabilization_seed_recovered") || count(player, () -> ModItems.CONTAMINATED_SEED.get()) > 0) {
         progress = Math.max(progress, 0.55F);
      }
      if (count(player, () -> ModItems.BIO_GEL.get()) > 0 || count(player, () -> ModItems.GENE_SAMPLE.get()) > 0
         || ReclamationProgress.value(player, "bio_gel_created") > 0) {
         progress = Math.max(progress, 0.75F);
      }
      return progress;
   }

   private static float ratio(int have, int need) {
      if (need <= 0) {
         return 1.0F;
      }
      return Math.min(1.0F, have / (float)need);
   }

   private static int requirementNeed(Mission mission) {
      return switch (mission.key()) {
         case "greenhouse_online" -> Math.max(1, ReclamationContent.progression().greenhouseSafeThreshold());
         case "restore_chunk" -> Math.max(1, ReclamationContent.progression().restoreThreshold());
         default -> mission.need();
      };
   }

   private static int routePhase(Identifier missionId) {
      String path = missionId == null ? "" : missionId.getPath();
      return switch (path) {
         case "mission/recover_seed", "recover_seed", "mission/analyze_soil", "analyze_soil" -> 2;
         case "mission/first_growth", "first_growth" -> 3;
         case "mission/gene_stabilization", "gene_stabilization" -> 6;
         case "mission/greenhouse_online", "greenhouse_online" -> 9;
         case "mission/restore_chunk", "restore_chunk" -> 15;
         default -> 9;
      };
   }

   private static String countLabel(int count, String noun) {
      return count + " " + noun + (count == 1 ? "" : "s");
   }

   private static Mission mission(String key, String title, String briefing, String phase, int order, String category, String guide, int need, Supplier<ItemStack> icon) {
      return new Mission(ReclamationTerminalIds.id("mission/" + key), key, title, briefing, phase, order, category, guide, need, icon);
   }

   private static Mission mission(Identifier id) {
      return MISSIONS.stream().filter(mission -> mission.id().equals(id)).findFirst().orElse(null);
   }

   private static List<ItemStack> rewards(Mission mission) {
      return switch (mission.key()) {
         case "recover_seed" -> List.of(stack(() -> (ItemLike) ModItems.SOIL_NUTRIENT_MIX.get(), Items.BONE_MEAL, 2), stack(() -> (ItemLike) ModItems.PURIFICATION_ENZYME.get(), Items.HONEY_BOTTLE, 1));
         case "analyze_soil" -> List.of(stack(() -> (ItemLike) ModItems.RECOVERED_SEED_CAPSULE.get(), Items.WHEAT_SEEDS, 2), stack(() -> (ItemLike) ModItems.SOIL_NUTRIENT_MIX.get(), Items.BONE_MEAL, 2));
         case "first_growth" -> List.of(stack(() -> (ItemLike) ModItems.GENE_SAMPLE.get(), Items.WHEAT_SEEDS, 2), stack(() -> (ItemLike) ModItems.BIO_GEL.get(), Items.SLIME_BALL, 1));
         case "gene_stabilization" -> List.of(stack(() -> (ItemLike) ModItems.RECOVERED_SEED_CAPSULE.get(), Items.WHEAT_SEEDS, 1), stack(() -> (ItemLike) ModBlocks.GREENHOUSE_GLASS.get(), Items.GLASS, 8));
         case "greenhouse_online" -> List.of(stack(() -> (ItemLike) ModItems.PURIFICATION_ENZYME.get(), Items.HONEY_BOTTLE, 2), stack(() -> (ItemLike) ModItems.BIO_GEL.get(), Items.SLIME_BALL, 2));
         case "restore_chunk" -> List.of(stack(() -> (ItemLike) ModItems.RECOVERED_SEED_CAPSULE.get(), Items.WHEAT_SEEDS, 4), stack(() -> (ItemLike) ModItems.GENE_SAMPLE.get(), Items.WHEAT_SEEDS, 3));
         default -> List.of();
      };
   }

   private static ItemStack stack(Supplier<? extends ItemLike> item, ItemLike fallback, int count) {
      if (!EchoCoreServices.itemStackComponentsBound()) {
         return ItemStack.EMPTY;
      }
      try {
         ItemLike value = nativeLoaderActive() || item == null ? fallback : item.get();
         return value == null ? ItemStack.EMPTY : new ItemStack(value, Math.max(1, count));
      } catch (RuntimeException | LinkageError ignored) {
         return fallback == null ? ItemStack.EMPTY : new ItemStack(fallback, Math.max(1, count));
      }
   }

   private static boolean nativeLoaderActive() {
      return Boolean.getBoolean("echo.native.loader")
         || !System.getProperty("echo.native.moduleIds", "").isBlank()
         || !System.getProperty("echo.native.moduleClasspath", "").isBlank()
         || !System.getProperty("echo.native.moduleClasspathFile", "").isBlank();
   }

   private static int count(Player player, Supplier<? extends Item> item) {
      if (nativeLoaderActive() || item == null) {
         return 0;
      }
      try {
         Item value = item.get();
         return value == null ? 0 : ReclamationProgress.count(player, value);
      } catch (RuntimeException | LinkageError ignored) {
         return 0;
      }
   }

   private record Mission(Identifier id, String key, String title, String briefing, String phase, int order, String category, String guide, int need, Supplier<ItemStack> iconSupplier) {
      ItemStack icon() {
         ItemStack stack = iconSupplier == null ? ItemStack.EMPTY : iconSupplier.get();
         return stack == null ? ItemStack.EMPTY : stack.copy();
      }
   }

   public record RouteMission(Identifier id, String key, String title, String briefing, String phase, String category, String nextAction) {
   }
}
