package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echoagriculturereclamation.content.ReclamationMetrics;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.client.mission.TerminalMissionBrowser;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.entity.player.Player;

public final class ReclamationTerminalClientIntegration {
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final int ACCENT = 0xFF92F7A6;

   private ReclamationTerminalClientIntegration() {
   }

   public static void register() {
      if (!REGISTERED.compareAndSet(false, true)) {
         return;
      }
      TerminalTab tab = new ReclamationTab();
      TerminalTabRegistry.register(tab);
      TerminalNavigationProfiles.register(tab.descriptor().id(), TerminalNavigationProfile.intel(240));
      TerminalAddonInfoRegistry.register(new ReclamationAddonInfoProvider());
   }

   private static final class ReclamationAddonInfoProvider implements TerminalAddonInfoProvider {
      @Override
      public String chapterId() {
         return ReclamationCoreIntegration.CHAPTER_ID;
      }

      @Override
      public TerminalAddonInfo info(Player player) {
         if (player == null) {
            return new TerminalAddonInfo(
               "Field reclamation, seed recovery, greenhouse safety, and restoration telemetry.",
               List.of(new TerminalAddonMetric("Signal", "OFFLINE", "waiting for player telemetry", ACCENT)),
               List.of(new TerminalAddonSection("Field Feed", List.of("Open FIELD > Reclamation after telemetry is available."))),
               links(),
               guide()
            );
         }
         ReclamationMetrics metrics = ReclamationProgress.metrics(player);
         ReclamationProgress.GreenhouseContext greenhouse = ReclamationProgress.greenhouseContext(player.level(), player.blockPosition());
         return new TerminalAddonInfo(
            "Field reclamation, seed recovery, greenhouse safety, and restoration telemetry.",
            List.of(
               new TerminalAddonMetric("Known Seeds", String.valueOf(metrics.knownSeeds()), "recovered crop profiles", ACCENT),
               new TerminalAddonMetric("Soil", metrics.soilLabel(), "strongest nearby soil state", metrics.soilState().safe() ? TerminalUi.GREEN : TerminalUi.AMBER),
               new TerminalAddonMetric("Greenhouse", metrics.greenhouseSafety() + "/" + ReclamationContent.progression().greenhouseSafeThreshold(), greenhouse.summaryLabel(), metrics.greenhouseSafety() >= ReclamationContent.progression().greenhouseSafeThreshold() ? TerminalUi.GREEN : TerminalUi.AMBER),
               new TerminalAddonMetric("Food", metrics.foodSecurity() + "%", "food security", metrics.foodSecurity() >= 60 ? TerminalUi.GREEN : TerminalUi.AMBER),
               new TerminalAddonMetric("Stability", metrics.cropStability() + "%", "seed and crop stability", metrics.cropStability() >= 80 ? TerminalUi.GREEN : TerminalUi.AMBER),
               new TerminalAddonMetric("Restoration", metrics.restorationScore() + "/" + ReclamationContent.progression().restoreThreshold(), "local chunk score", metrics.restorationScore() >= ReclamationContent.progression().restoreThreshold() ? TerminalUi.GREEN : TerminalUi.CYAN)
            ),
            List.of(new TerminalAddonSection("Field Feed", ReclamationTerminalReport.fieldFeed(player))),
            links(),
            guide()
         );
      }

      private static TerminalAddonGuide guide() {
         return TerminalAddonGuide.optional(240, "Field route",
            "Agriculture Reclamation is optional recovery progression: start with seed capsules, then stabilize soil, crops, greenhouse zones, and local ecology.",
            List.of(
               "Craft a seed capsule from wheat seeds, bone meal, glass bottle, and copper, then open it directly or identify it in a Seed Vault Terminal.",
               "Plant the profiled seed on dirt, grass, farmland, or a reusable Hydroponic Tray, then harvest the first crop.",
               "Craft a Bio-Reactor with Soil Nutrient Mix, turn crop matter into Bio-Gel, then stabilize recovered seed cuttings before scaling restoration crops."
            ));
      }

      private static List<TerminalAddonLink> links() {
         return List.of(new TerminalAddonLink(ReclamationTerminalIds.FIELD_TAB, "FIELD > Reclamation", "Food security, crop stability, and restoration status", ACCENT));
      }
   }

   private static final class ReclamationTab implements ClientTerminalTab {
      private final TerminalTabDescriptor descriptor =
         new TerminalTabDescriptor(ReclamationTerminalIds.FIELD_TAB, "RECLAMATION", 240, ACCENT);
      private final TerminalTabChrome chrome =
         TerminalTabChrome.of("Reclamation", TerminalTabChrome.GROUP_FIELD, "AR", "Agriculture recovery telemetry", 240);
      private final TerminalMissionBrowser browser =
         new TerminalMissionBrowser(ReclamationMissionProvider.INSTANCE, descriptor.id(), true);

      @Override
      public TerminalTabDescriptor descriptor() {
         return descriptor;
      }

      @Override
      public TerminalTabChrome chrome() {
         return chrome;
      }

      @Override
      public void onSelected(TerminalRenderContext context) {
         browser.onSelected(context);
      }

      @Override
      public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
         browser.render(context, graphics, mouseX, mouseY, partialTick);
      }

      @Override
      public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
         return browser.mouseClicked(context, mouseX, mouseY, button);
      }

      @Override
      public boolean mouseDragged(TerminalRenderContext context, double mouseX, double mouseY, int button,
            double dragX, double dragY) {
         return browser.mouseDragged(context, mouseX, mouseY, button, dragX, dragY);
      }

      @Override
      public boolean mouseReleased(TerminalRenderContext context, double mouseX, double mouseY, int button) {
         return browser.mouseReleased(context, mouseX, mouseY, button);
      }

      @Override
      public boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
         return browser.mouseScrolled(context, mouseX, mouseY, delta);
      }

      @Override
      public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
         return browser.keyPressed(context, event);
      }

      @Override
      public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
         return browser.charTyped(context, event);
      }

      @Override
      public int contentHeight(TerminalRenderContext context) {
         return browser.contentHeight(context);
      }
   }
}
