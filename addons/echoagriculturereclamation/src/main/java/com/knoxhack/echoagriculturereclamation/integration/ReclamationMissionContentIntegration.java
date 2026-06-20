package com.knoxhack.echoagriculturereclamation.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class ReclamationMissionContentIntegration {
   private static final Identifier CHAPTER = id("field_reclamation");

   private ReclamationMissionContentIntegration() {
   }

   public static void register() {
      EchoCoreServices.registerMissionContent(EchoAgricultureReclamation.MODID, ReclamationMissionContentIntegration::registerContent);
      ReclamationMissionHooks.registerCoverage();
   }

   public static void registerContent(IMissionRegistry registry) {
      registry.registerChapter(EchoAgricultureReclamation.MODID, new MissionChapterDefinition(
         CHAPTER,
         "Field Reclamation / Ecology Recovery",
         "Recover seeds, cleanse soil, stabilize crop genes, establish greenhouses, and restore local chunks.",
         64,
         0xFF92F7A6));
      mission(registry, "recover_seed", "Seed Capsule Recovery", "Recover and analyze a seed capsule.",
         "Recovered capsules unlock crop profiles for standalone farming and Ashfall salvage tables.",
         safeStack(() -> (ItemLike) ModItems.RECOVERED_SEED_CAPSULE.get(), Items.WHEAT_SEEDS, 1), 0, "Recover or analyze a seed capsule");
      mission(registry, "analyze_soil", "Soil Health Survey", "Inspect contaminated, dead, irradiated, or restored soil.",
         "Soil state drives crop support, purifier output, and restoration thresholds.",
         safeStack(() -> (ItemLike) ModBlocks.ECOLOGY_SCANNER.get(), Items.COMPASS, 1), 1, "Analyze local soil");
      mission(registry, "first_growth", "First Clean Growth", "Grow any Reclamation crop or hydroponic culture.",
         "A living crop proves the seed profile can survive outside vault storage.",
         safeStack(() -> (ItemLike) ModBlocks.HYDROPONIC_TRAY.get(), Items.FLOWER_POT, 1), 2, "Grow a Reclamation crop");
      mission(registry, "gene_stabilization", "Gene Stabilization", "Stabilize a contaminated seed profile.",
         "Stable seeds improve yields, reduce contamination returns, and push restoration faster.",
         safeStack(() -> (ItemLike) ModBlocks.GENE_STABILIZER.get(), Items.BREWING_STAND, 1), 3, "Stabilize a seed");
      mission(registry, "greenhouse_online", "Greenhouse Zone Online", "Scan and register a safe greenhouse envelope.",
         "Controllers save zone quality and protect crops from exposed-weather growth pressure.",
         safeStack(() -> (ItemLike) ModBlocks.GREENHOUSE_CONTROLLER.get(), Items.GLASS, 1), 4, "Bring a greenhouse zone online");
      mission(registry, "restore_chunk", "Chunk Restoration", "Raise a local restoration score to its configured target.",
         "Crop maturity, purifier passes, and scanner pulses convert a field from toxic to restored.",
         safeStack(() -> (ItemLike) ModBlocks.RESTORED_SOIL.get(), Items.DIRT, 1), 5, "Restore a local field");
   }

   private static void mission(IMissionRegistry registry, String path, String title, String briefing,
         String guide, ItemStack icon, int order, String objectiveLabel) {
      Identifier mission = id("mission/" + path);
      registry.registerMission(EchoAgricultureReclamation.MODID, MissionDefinition.builder(mission, CHAPTER)
         .phase("ecology_recovery", "Ecology Recovery", 0, order)
         .text(title, briefing, guide)
         .category("Reclamation", "Field")
         .icon(icon)
         .kind(MissionKind.SIDE_OP)
         .objective(new ObjectiveDefinition(
            id("mission/" + path + "/objective"),
            MissionObjectiveType.CUSTOM,
            objectiveLabel,
            "",
            icon,
            1,
            false,
            Map.of("target", MissionHookTargets.objectiveTarget(EchoAgricultureReclamation.MODID, mission, 0).toString())))
         .build());
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, path);
   }

   private static ItemStack safeStack(Supplier<? extends ItemLike> item, ItemLike fallback, int count) {
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
}
