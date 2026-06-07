package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationMetrics;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import java.util.List;
import net.minecraft.world.entity.player.Player;

final class ReclamationTerminalReport {
   private ReclamationTerminalReport() {
   }

   static String summary(Player player) {
      ReclamationMetrics metrics = ReclamationProgress.metrics(player);
      ReclamationProgress.GreenhouseContext greenhouse = greenhouse(player);
      return "ECHO FIELD // Reclamation status: seeds " + metrics.knownSeeds() + "/" + CropSpec.ALL.size()
         + ", soil " + metrics.soilLabel()
         + ", greenhouse " + metrics.greenhouseSafety() + "/" + ReclamationContent.progression().greenhouseSafeThreshold()
         + " (" + greenhouse.summaryLabel() + ")"
         + ", stability " + metrics.cropStability()
         + "%, food " + metrics.foodSecurity()
         + "%, restoration " + metrics.restorationScore() + "/" + ReclamationContent.progression().restoreThreshold()
         + ".";
   }

   static String routeReport(Player player) {
      ReclamationMetrics metrics = ReclamationProgress.metrics(player);
      return summary(player) + " Next: " + nextStep(player, metrics);
   }

   static List<String> fieldFeed(Player player) {
      ReclamationMetrics metrics = ReclamationProgress.metrics(player);
      ReclamationProgress.GreenhouseContext greenhouse = greenhouse(player);
      return List.of(
         "Soil state: " + metrics.soilLabel(),
         "Greenhouse safety: " + metrics.greenhouseSafety() + "/" + ReclamationContent.progression().greenhouseSafeThreshold()
            + " (" + greenhouse.summaryLabel() + ")",
         "Pollination: " + countLabel(greenhouse.scan().deployedDrones(), "drone") + " | "
            + countLabel(greenhouse.scan().serviceTargets(), "service target"),
         "Hydroponics: " + (ReclamationProgress.flag(player, "hydroponics_online") ? "culture online" : "tray route pending"),
         "Harvests: " + ReclamationProgress.value(player, "crops_grown")
            + " | stabilization seed: " + (ReclamationProgress.flag(player, "stabilization_seed_recovered") ? "available" : "pending"),
         "Bio-Gel: " + ReclamationProgress.count(player, ModItems.BIO_GEL.get())
            + " carried | created " + ReclamationProgress.value(player, "bio_gel_created"),
         "Gene stabilization: " + (ReclamationProgress.flag(player, "gene_stabilization") ? "complete" : "pending"),
         "Next action: " + nextStep(player, metrics)
      );
   }

   static String nextStep(Player player) {
      return nextStep(player, ReclamationProgress.metrics(player));
   }

   private static String nextStep(Player player, ReclamationMetrics metrics) {
      if (metrics.knownSeeds() <= 0 && !ReclamationProgress.flag(player, "seed_recovered")) {
         return "craft a seed capsule from wheat seeds, bone meal, glass bottle, and copper, then open it or identify it at a Seed Vault Terminal.";
      }
      if (!ReclamationProgress.flag(player, "soil_analyzed") && ReclamationProgress.value(player, "soil_purified") <= 0) {
         return "scan local soil, or grow the first profiled seed on dirt, grass, farmland, or a Hydroponic Tray while purifier parts come online.";
      }
      if (ReclamationProgress.value(player, "crops_grown") <= 0) {
         return "plant a profiled seed on compatible soil or start a Hydroponic Tray culture, then harvest the first crop.";
      }
      if (!ReclamationProgress.flag(player, "stabilization_seed_recovered")) {
         return "harvest one unstable crop to recover a seed cutting for stabilization.";
      }
      if (ReclamationProgress.count(player, ModItems.BIO_GEL.get()) <= 0
         && ReclamationProgress.count(player, ModItems.GENE_SAMPLE.get()) <= 0
         && ReclamationProgress.value(player, "bio_gel_created") <= 0) {
         return "craft a Bio-Reactor with Soil Nutrient Mix, then process any crop matter into Bio-Gel.";
      }
      if (!ReclamationProgress.flag(player, "gene_stabilization")) {
         return "craft the Gene Stabilizer with Bio-Gel and Soil Nutrient Mix, then use it with a contaminated seed and Bio-Gel or Gene Sample.";
      }
      if (metrics.greenhouseSafety() < ReclamationContent.progression().greenhouseSafeThreshold()) {
         return greenhouse(player).nextAction();
      }
      if (metrics.restorationScore() < ReclamationContent.progression().restoreThreshold()) {
         return "grow restoration-weight crops and use Ecology Scanner reports until this chunk restores.";
      }
      return "local field route complete; keep collecting seed profiles and expand restored soil block by block.";
   }

   private static ReclamationProgress.GreenhouseContext greenhouse(Player player) {
      if (player == null) {
         return ReclamationProgress.GreenhouseScan.empty().asContext();
      }
      return ReclamationProgress.greenhouseContext(player.level(), player.blockPosition());
   }

   private static String countLabel(int count, String noun) {
      return count + " " + noun + (count == 1 ? "" : "s");
   }
}
