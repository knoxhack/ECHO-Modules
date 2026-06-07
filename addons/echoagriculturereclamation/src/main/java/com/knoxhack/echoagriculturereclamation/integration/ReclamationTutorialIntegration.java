package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echotutorialcore.api.TutorialCategory;
import com.knoxhack.echotutorialcore.api.TutorialCoreApi;
import com.knoxhack.echotutorialcore.api.TutorialGuideMode;
import com.knoxhack.echotutorialcore.api.TutorialHintType;
import com.knoxhack.echotutorialcore.api.card.TutorialCard;
import com.knoxhack.echotutorialcore.api.hint.TutorialHint;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;

public final class ReclamationTutorialIntegration {
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

   private ReclamationTutorialIntegration() {
   }

   public static void register() {
      if (!REGISTERED.compareAndSet(false, true)) {
         return;
      }
      card("first_capsule", "Recover a Seed Capsule", "Open or analyze a recovered capsule to create a profiled seed.",
         List.of("Craft a capsule from wheat seeds, bone meal, glass, and copper, or recover one from ruined ecology caches.",
            "Use it directly or at a Seed Vault Terminal.",
            "The resulting seed profile controls crop identity, stability, and contamination."));
      card("first_growth", "Start Field Cultivation", "Plant a profiled seed in compatible soil or a Hydroponic Tray.",
         List.of("Check seed tooltip data before planting.",
            "Hydroponic Trays preserve the seed culture and can be fed with Soil Nutrient Mix.",
            "A greenhouse improves safety and yield once the controller scan is established."));
      card("greenhouse_restore", "Restore a Local Chunk", "Use greenhouse support, restoration crops, and Ecology Scanner pulses to raise restoration pressure.",
         List.of("Build Greenhouse Glass, Spore Filters, Pollinator Dock support, and crop/tray targets.",
            "Scan the Greenhouse Controller to save the zone.",
            "Keep scanning ecology until purification, stabilization, and restored-soil thresholds are reached."));
      hint("first_capsule", TutorialHintType.ROUTE_HELP, "FIELD seed route", "Recover or craft a Recovered Seed Capsule to start Agriculture Reclamation.");
      hint("greenhouse_scan", TutorialHintType.MACHINE_HELP, "Greenhouse unread", "Use the Greenhouse Controller to save a zone before expecting safe greenhouse bonuses.");
      hint("restore_chunk", TutorialHintType.PROGRESSION, "Restoration pressure", "Mature restoration crops and use the Ecology Scanner to raise chunk-local restoration.");
   }

   private static void card(String path, String title, String summary, List<String> steps) {
      Identifier id = id("card/" + path);
      TutorialCoreApi.registerCard(new TutorialCard(
         id,
         TutorialCategory.ADDONS,
         title,
         summary,
         List.of(summary),
         steps,
         List.of(),
         List.of(),
         List.of("echoagriculturereclamation:" + path),
         false,
         EchoAgricultureReclamation.MODID,
         130
      ));
   }

   private static void hint(String path, TutorialHintType type, String title, String message) {
      TutorialCoreApi.registerHint(new TutorialHint(
         id("hint/" + path),
         type,
         TutorialCategory.ADDONS,
         title,
         message,
         "",
         "Open FIELD > Reclamation",
         id("card/" + path),
         20 * 60,
         Set.copyOf(EnumSet.of(TutorialGuideMode.NORMAL, TutorialGuideMode.ASSISTED)),
         120,
         true,
         List.of()
      ));
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, path);
   }
}
