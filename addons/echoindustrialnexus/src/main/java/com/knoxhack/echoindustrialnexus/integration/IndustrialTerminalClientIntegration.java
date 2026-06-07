package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echomultiblockcore.client.MultiblockClientPackets;
import com.knoxhack.echomultiblockcore.network.AutomationRecipeMetadataPacket;
import com.knoxhack.echoindustrialnexus.client.IndustrialFactoryClientState;
import com.knoxhack.echoindustrialnexus.network.IndustrialFactorySnapshotPacket;
import com.knoxhack.echoindustrialnexus.progress.IndustrialProgress;
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
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import com.knoxhack.echoterminal.client.mission.TerminalMissionBrowser;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class IndustrialTerminalClientIntegration {
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final int ACCENT = 0xFFFF9F3D;

   private IndustrialTerminalClientIntegration() {
   }

   public static void register() {
      if (!REGISTERED.compareAndSet(false, true)) {
         return;
      }
      TerminalTab tab = new IndustrialNexusTab();
      TerminalTabRegistry.register(tab);
      TerminalNavigationProfiles.register(
         tab.descriptor().id(),
         TerminalNavigationProfile.chapter("industrial", "Optional: Industrial Nexus", "OP", 70)
      );
      TerminalAddonInfoRegistry.register(new IndustrialAddonInfoProvider());
      TerminalRecipeRegistry.register(IndustrialTerminalRecipeProvider.INSTANCE);
   }

   private static final class IndustrialAddonInfoProvider implements TerminalAddonInfoProvider {
      @Override
      public String chapterId() {
         return IndustrialCoreIntegration.CHAPTER_ID;
      }

      @Override
      public TerminalAddonInfo info(Player player) {
         if (player == null) {
            return new TerminalAddonInfo(
               "Factory recovery, scrubber support, Thermal Flux, and Furnace Warden production survival.",
               List.of(new TerminalAddonMetric("Signal", "OFFLINE", "waiting for player telemetry", ACCENT)),
               List.of(new TerminalAddonSection("Factory Feed",
                  List.of("Open Industrial Nexus after player telemetry is available."))),
               links(),
               guide());
         }
         int flux = IndustrialProgress.value(player, "thermal_flux_generated");
         int machines = IndustrialProgress.value(player, "machines");
         int ducts = IndustrialProgress.value(player, "item_ducts") + IndustrialProgress.value(player, "flux_ducts");
         boolean safeZone = IndustrialProgress.flag(player, "safe_zone");
         boolean wardenDefeated = IndustrialProgress.flag(player, "furnace_warden_defeated");
         IndustrialFactorySnapshotPacket factorySnapshot = IndustrialFactoryClientState.snapshot();
         long blocked = factorySnapshot.entries().stream().filter(entry -> "BLOCKED".equals(entry.alertLevel())).count();
         return new TerminalAddonInfo(
            "Factory recovery, scrubber support, Thermal Flux, and Furnace Warden production survival.",
            List.of(
               new TerminalAddonMetric("Thermal Flux", String.valueOf(flux), "generated TF", ACCENT),
               new TerminalAddonMetric("Machines", String.valueOf(machines), "scanned machines", TerminalUi.CYAN),
               new TerminalAddonMetric("Ducts", String.valueOf(ducts), "item and flux network", TerminalUi.GREEN),
               new TerminalAddonMetric("Facilities", String.valueOf(factorySnapshot.entries().size()),
                  blocked + " blocked", blocked > 0 ? TerminalUi.AMBER : TerminalUi.CYAN),
               new TerminalAddonMetric("Safe Zone", safeZone ? "ONLINE" : "PENDING",
                  "industrial scrubber support", safeZone ? TerminalUi.GREEN : TerminalUi.AMBER)),
            List.of(new TerminalAddonSection("Factory Feed", List.of(
               "Factory scan: " + (IndustrialProgress.flag(player, "factory_scanned") ? "recorded" : "pending"),
               "Controllers: " + IndustrialProgress.value(player, "controllers"),
               "Hot machines: " + IndustrialProgress.value(player, "hot_machines"),
               "Factory Command: " + factorySnapshot.statusLine(),
               "Furnace Warden: " + (wardenDefeated ? "defeated" : "active")))),
            links(),
            guide());
      }

      private static TerminalAddonGuide guide() {
         return TerminalAddonGuide.optional(600, "Side route",
            "Industrial Nexus is optional factory progression; start it when your base can support heat, power, and machine recovery.",
            List.of(
               "Open Industrial Nexus and scan factory route telemetry.",
               "Build toward scrubbers and safe-zone support.",
               "Treat Furnace Warden progress as production survival, not a main saga gate."));
      }

      private static List<TerminalAddonLink> links() {
         return List.of(new TerminalAddonLink(IndustrialTerminalIds.ECHO_TAB,
            "Industrial Nexus", "Factory route telemetry", ACCENT));
      }
   }

   private static final class IndustrialNexusTab implements ClientTerminalTab {
      private final TerminalTabDescriptor descriptor =
         new TerminalTabDescriptor(IndustrialTerminalIds.ECHO_TAB, "INDUSTRIAL NEXUS", 70, ACCENT);
      private final TerminalTabChrome chrome =
         TerminalTabChrome.of("Industrial Nexus", TerminalTabChrome.GROUP_ADDONS, "IN", "Factory route telemetry", 70);
      private final TerminalMissionBrowser browser =
         new TerminalMissionBrowser(IndustrialMissionProvider.INSTANCE, descriptor.id(), true,
            TerminalMissionBrowser.ActionMode.TRACKING_ONLY,
            TerminalMissionBrowser.InitialTreeFocus.ALIGN_SELECTED_TOP);
      private final List<Hit> hits = new ArrayList<>();
      private boolean missionView;
      private int selectedIndex;

      public TerminalTabDescriptor descriptor() {
         return descriptor;
      }

      public TerminalTabChrome chrome() {
         return chrome;
      }

      public void onSelected(TerminalRenderContext context) {
         if (!TerminalNavigationProfiles.referenceOnlyChapterContext(context)
            && IndustrialFactoryClientState.shouldRequest(1000L)) {
            context.sendAction(IndustrialTerminalIds.ECHO_TAB, IndustrialTerminalIds.FACTORY_SYNC, "");
         }
         browser.onSelected(context);
      }

      public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
         hits.clear();
         drawModeBar(context, graphics, mouseX, mouseY);
         if (missionView) {
            browser.render(innerContext(context), graphics, mouseX, mouseY, partialTick);
            return;
         }
         renderDashboard(context, graphics, mouseX, mouseY);
      }

      public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
         if (button != 0) {
            return missionView && browser.mouseClicked(innerContext(context), mouseX, mouseY, button);
         }
         int x = context.contentX();
         int y = context.contentY();
         if (inside(mouseX, mouseY, x, y, 86, 18)) {
            missionView = false;
            context.playCommandSound();
            if (!TerminalNavigationProfiles.referenceOnlyChapterContext(context)
               && IndustrialFactoryClientState.shouldRequest(250L)) {
               context.sendAction(IndustrialTerminalIds.ECHO_TAB, IndustrialTerminalIds.FACTORY_SYNC, "");
            }
            return true;
         }
         if (inside(mouseX, mouseY, x + 92, y, 78, 18)) {
            missionView = true;
            context.playCommandSound();
            return true;
         }
         if (missionView) {
            return browser.mouseClicked(innerContext(context), mouseX, mouseY, button);
         }
         for (Hit hit : hits) {
            if (inside(mouseX, mouseY, hit.x(), hit.y(), hit.w(), hit.h())) {
               if (!hit.enabled()) {
                  context.playRejectedSound();
                  return true;
               }
               if (hit.selectIndex() >= 0) {
                  selectedIndex = hit.selectIndex();
                  context.playCommandSound();
               } else {
                  context.sendAction(IndustrialTerminalIds.ECHO_TAB, hit.actionId(), hit.payload());
               }
               return true;
            }
         }
         return false;
      }

      public boolean mouseDragged(TerminalRenderContext context, double mouseX, double mouseY, int button,
            double dragX, double dragY) {
         return missionView && browser.mouseDragged(innerContext(context), mouseX, mouseY, button, dragX, dragY);
      }

      public boolean mouseReleased(TerminalRenderContext context, double mouseX, double mouseY, int button) {
         return missionView && browser.mouseReleased(innerContext(context), mouseX, mouseY, button);
      }

      public boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
         return missionView && browser.mouseScrolled(innerContext(context), mouseX, mouseY, delta);
      }

      public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
         return browser.keyPressed(context, event);
      }

      public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
         return browser.charTyped(context, event);
      }

      public int contentHeight(TerminalRenderContext context) {
         return missionView ? 32 + browser.contentHeight(innerContext(context)) : 420;
      }

      private void drawModeBar(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
         int x = context.contentX();
         int y = context.contentY();
         drawButton(context, graphics, x, y, 86, 18, "FACILITIES", mouseX, mouseY, !missionView, true);
         drawButton(context, graphics, x + 92, y, 78, 18, "MISSIONS", mouseX, mouseY, missionView, true);
      }

      private void renderDashboard(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
         IndustrialFactorySnapshotPacket snapshot = IndustrialFactoryClientState.snapshot();
         List<IndustrialFactorySnapshotPacket.Entry> entries = snapshot.entries();
         selectedIndex = entries.isEmpty() ? 0 : Math.max(0, Math.min(selectedIndex, entries.size() - 1));
         IndustrialFactorySnapshotPacket.Entry selected = entries.isEmpty() ? null : entries.get(selectedIndex);
         int x = context.contentX();
         int y = context.contentY() + 26;
         int w = context.contentWidth();
         int leftW = Math.min(218, Math.max(176, w / 2));
         int rightX = x + leftW + 12;
         int rightW = Math.max(180, w - leftW - 12);

         long active = entries.stream().filter(entry -> "ACTIVE".equals(entry.alertLevel())).count();
         long blocked = entries.stream().filter(entry -> "BLOCKED".equals(entry.alertLevel())).count();
         int queued = entries.stream().mapToInt(IndustrialFactorySnapshotPacket.Entry::taskCount).sum();
         int robots = entries.stream().mapToInt(IndustrialFactorySnapshotPacket.Entry::robotCount).sum();
         graphics.text(context.minecraft().font, Component.literal("FACTORY COMMAND // " + snapshot.statusLine()),
            x, y, TerminalUi.CYAN, false);
         drawMetric(context, graphics, x, y + 18, "ONLINE", entries.size(), 0xFF64D97B);
         drawMetric(context, graphics, x + 82, y + 18, "ACTIVE", (int)active, 0xFFFFA23F);
         drawMetric(context, graphics, x + 164, y + 18, "BLOCKED", (int)blocked, 0xFFFF5D4D);
         drawMetric(context, graphics, x + 246, y + 18, "QUEUE", queued, TerminalUi.CYAN);
         drawMetric(context, graphics, x + 328, y + 18, "ROBOTS", robots, 0xFF92F7A6);

         drawFacilityList(context, graphics, entries, x, y + 48, leftW, mouseX, mouseY);
         drawFacilityDetail(context, graphics, selected, rightX, y + 48, rightW, mouseX, mouseY);
      }

      private void drawMetric(TerminalRenderContext context, GuiGraphicsExtractor graphics, int x, int y,
                              String label, int value, int color) {
         frame(graphics, x, y, 72, 32, color);
         graphics.text(context.minecraft().font, Component.literal(label), x + 6, y + 5, 0xFFBFD9E1, false);
         graphics.text(context.minecraft().font, Component.literal(Integer.toString(value)), x + 6, y + 17, color, false);
      }

      private void drawFacilityList(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                                    List<IndustrialFactorySnapshotPacket.Entry> entries, int x, int y, int w,
                                    int mouseX, int mouseY) {
         graphics.text(context.minecraft().font, Component.literal("LOADED FACILITIES"), x, y, TerminalUi.CYAN, false);
         if (entries.isEmpty()) {
            graphics.text(context.minecraft().font, Component.literal("No loaded Industrial multiblocks synced."),
               x, y + 18, 0xFF8EA2AA, false);
            drawAction(context, graphics, x, y + 42, 88, 18, "REFRESH", mouseX, mouseY,
               IndustrialTerminalIds.FACTORY_SYNC, "", true);
            return;
         }
         for (int i = 0; i < Math.min(entries.size(), 8); i++) {
            IndustrialFactorySnapshotPacket.Entry entry = entries.get(i);
            int rowY = y + 16 + i * 34;
            int color = i == selectedIndex ? entry.alertColor() : 0xFF34444C;
            frame(graphics, x, rowY, w, 30, color);
            graphics.fill(x + 1, rowY + 1, x + w - 1, rowY + 29, i == selectedIndex ? 0xAA14312B : 0x99070D11);
            graphics.text(context.minecraft().font, Component.literal(fit(context, entry.displayName(), w - 12)),
               x + 6, rowY + 5, entry.alertColor(), false);
            graphics.text(context.minecraft().font, Component.literal(entry.alertLevel() + " / " + entry.controllerPos().toShortString()),
               x + 6, rowY + 17, 0xFFBFD9E1, false);
            hits.add(Hit.select(x, rowY, w, 30, i));
         }
      }

      private void drawFacilityDetail(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                                      IndustrialFactorySnapshotPacket.Entry entry, int x, int y, int w,
                                      int mouseX, int mouseY) {
         graphics.text(context.minecraft().font, Component.literal("FACILITY DETAIL"), x, y, TerminalUi.CYAN, false);
         if (entry == null) {
            graphics.text(context.minecraft().font, Component.literal("Select a synced Industrial facility."),
               x, y + 18, 0xFF8EA2AA, false);
            return;
         }
         int panelY = y + 16;
         frame(graphics, x, panelY, w, 104, entry.alertColor());
         graphics.fill(x + 1, panelY + 1, x + w - 1, panelY + 103, 0x99101820);
         line(context, graphics, x + 8, panelY + 8, "State", entry.alertLevel() + " / " + entry.state(), entry.alertColor());
         line(context, graphics, x + 8, panelY + 22, "Integrity", Math.round(entry.integrity()) + "%", 0xFF92F7A6);
         line(context, graphics, x + 8, panelY + 36, "Completion", Math.round(entry.completion() * 100.0D) + "%", TerminalUi.CYAN);
         line(context, graphics, x + 8, panelY + 50, "Task", progressLine(entry), 0xFFFFA23F);
         line(context, graphics, x + 8, panelY + 64, "Robotics", entry.robotCount() + " arm(s), queue "
            + entry.taskCount() + "/" + entry.queueCapacity(), 0xFFBFD9E1);
         line(context, graphics, x + 8, panelY + 78, "Warning", entry.warningCount() == 0 ? "none" : entry.firstWarning(), 0xFFFFC857);
         line(context, graphics, x + 8, panelY + 92, "Restock", (entry.logisticsAutoRestockEnabled() ? "ON" : "OFF")
            + " x" + entry.logisticsRestockTargetRuns() + " / " + entry.logisticsRestockStatus(), TerminalUi.CYAN);

         int actionY = panelY + 114;
         drawAction(context, graphics, x, actionY, 74, 18, "REVALIDATE", mouseX, mouseY,
            IndustrialTerminalIds.FACTORY_REVALIDATE, payload(entry, null, 1), true);
         drawAction(context, graphics, x + 80, actionY, 58, 18, "CLEAR", mouseX, mouseY,
            IndustrialTerminalIds.FACTORY_CLEAR_QUEUE, payload(entry, null, 1), entry.taskCount() > 0);
         drawAction(context, graphics, x + 144, actionY, 58, 18, "RETRY", mouseX, mouseY,
            IndustrialTerminalIds.FACTORY_RETRY_BLOCKED, payload(entry, null, 1), !entry.blockedReason().isBlank());
         drawAction(context, graphics, x + 208, actionY, 60, 18, "REFRESH", mouseX, mouseY,
            IndustrialTerminalIds.FACTORY_SYNC, "", true);

         int restockY = actionY + 24;
         drawAction(context, graphics, x, restockY, 58, 18, entry.logisticsAutoRestockEnabled() ? "AUTO ON" : "AUTO OFF", mouseX, mouseY,
            IndustrialTerminalIds.FACTORY_TOGGLE_LOGISTICS_RESTOCK, payload(entry, null, 1), true);
         drawAction(context, graphics, x + 64, restockY, 28, 18, "x1", mouseX, mouseY,
            IndustrialTerminalIds.FACTORY_SET_LOGISTICS_RESTOCK_TARGET, payload(entry, null, 1), true);
         drawAction(context, graphics, x + 98, restockY, 28, 18, "x3", mouseX, mouseY,
            IndustrialTerminalIds.FACTORY_SET_LOGISTICS_RESTOCK_TARGET, payload(entry, null, 3), true);
         drawAction(context, graphics, x + 132, restockY, 28, 18, "x5", mouseX, mouseY,
            IndustrialTerminalIds.FACTORY_SET_LOGISTICS_RESTOCK_TARGET, payload(entry, null, 5), true);
         drawAction(context, graphics, x + 166, restockY, 56, 18, "RESTOCK", mouseX, mouseY,
            IndustrialTerminalIds.FACTORY_REQUEST_LOGISTICS_RESTOCK_NOW, payload(entry, null, 1), entry.logisticsAutoRestockEnabled());

         graphics.text(context.minecraft().font, Component.literal("QUEUE RECIPES"), x, actionY + 54, TerminalUi.CYAN, false);
         for (int i = 0; i < Math.min(entry.recipeIds().size(), 5); i++) {
            Identifier recipeId = entry.recipeIds().get(i);
            AutomationRecipeMetadataPacket.Entry metadata = MultiblockClientPackets.recipeMetadata(recipeId);
            int rowY = actionY + 70 + i * 24;
            frame(graphics, x, rowY, w, 22, 0xFF34444C);
            graphics.fill(x + 1, rowY + 1, x + w - 1, rowY + 21, 0x99101820);
            String label = metadata == null ? recipeId.getPath() : metadata.displayName();
            graphics.text(context.minecraft().font, Component.literal(fit(context, label, Math.max(72, w - 120))),
               x + 6, rowY + 7, 0xFFD8F6FF, false);
            int bx = x + w - 108;
            drawAction(context, graphics, bx, rowY + 3, 20, 16, "1", mouseX, mouseY,
               IndustrialTerminalIds.FACTORY_QUEUE_TASK, payload(entry, recipeId, 1), entry.taskCount() < entry.queueCapacity());
            drawAction(context, graphics, bx + 24, rowY + 3, 20, 16, "3", mouseX, mouseY,
               IndustrialTerminalIds.FACTORY_QUEUE_TASK, payload(entry, recipeId, 3), entry.taskCount() < entry.queueCapacity());
            drawAction(context, graphics, bx + 48, rowY + 3, 20, 16, "5", mouseX, mouseY,
               IndustrialTerminalIds.FACTORY_QUEUE_TASK, payload(entry, recipeId, 5), entry.taskCount() < entry.queueCapacity());
            drawAction(context, graphics, bx + 74, rowY + 3, 30, 16, "LOG", mouseX, mouseY,
               IndustrialTerminalIds.FACTORY_REQUEST_LOGISTICS, payload(entry, recipeId, 1), true);
         }
      }

      private void line(TerminalRenderContext context, GuiGraphicsExtractor graphics, int x, int y,
                        String label, String value, int color) {
         graphics.text(context.minecraft().font, Component.literal(label + ":"), x, y, 0xFF8EA2AA, false);
         graphics.text(context.minecraft().font, Component.literal(fit(context, value, 210)), x + 68, y, color, false);
      }

      private String progressLine(IndustrialFactorySnapshotPacket.Entry entry) {
         if ("Idle".equals(entry.activeTask())) {
            return "Idle";
         }
         return entry.activeTask() + " " + entry.activeProgress() + "/" + entry.activeDuration();
      }

      private void drawAction(TerminalRenderContext context, GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                              String label, int mouseX, int mouseY, Identifier action, String payload, boolean enabled) {
         boolean effectiveEnabled = enabled && !TerminalNavigationProfiles.referenceOnlyChapterContext(context);
         drawButton(context, graphics, x, y, w, h, label, mouseX, mouseY, false, effectiveEnabled);
         hits.add(Hit.action(x, y, w, h, action, payload, effectiveEnabled));
      }

      private void drawButton(TerminalRenderContext context, GuiGraphicsExtractor graphics, int x, int y, int w, int h,
                              String label, int mouseX, int mouseY, boolean selected, boolean enabled) {
         int border = !enabled ? 0xFF273136 : selected ? ACCENT : inside(mouseX, mouseY, x, y, w, h) ? TerminalUi.CYAN : 0xFF44656E;
         frame(graphics, x, y, w, h, border);
         graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, enabled ? 0xAA142027 : 0xAA101417);
         int color = enabled ? 0xFFD8F6FF : 0xFF66777D;
         graphics.text(context.minecraft().font, Component.literal(fit(context, label, w - 6)), x + 4, y + 5, color, false);
      }

      private TerminalRenderContext innerContext(TerminalRenderContext context) {
         return new TerminalRenderContext(context.minecraft(), context.player(), context.screenWidth(),
            context.screenHeight(), context.contentX(), context.contentY() + 28,
            context.contentWidth(), Math.max(0, context.contentHeight() - 28), context.scrollY(),
            context.tabNavigator(), context.tabAvailability(), context.theme(), context.themeContext());
      }

      private static String payload(IndustrialFactorySnapshotPacket.Entry entry, Identifier recipeId, int quantity) {
         StringBuilder builder = new StringBuilder();
         builder.append('{')
            .append("\"dimension\":\"").append(entry.dimension()).append("\",")
            .append("\"controller_pos\":").append(entry.controllerPos().asLong()).append(',')
            .append("\"quantity\":").append(Math.max(1, Math.min(5, quantity)));
         if (recipeId != null) {
            builder.append(',').append("\"recipe_id\":\"").append(recipeId).append('"');
         }
         return builder.append('}').toString();
      }

      private void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
         graphics.fill(x, y, x + w, y + 1, color);
         graphics.fill(x, y + h - 1, x + w, y + h, color);
         graphics.fill(x, y, x + 1, y + h, color);
         graphics.fill(x + w - 1, y, x + w, y + h, color);
      }

      private boolean inside(double px, double py, int x, int y, int w, int h) {
         return px >= x && px < x + w && py >= y && py < y + h;
      }

      private String fit(TerminalRenderContext context, String text, int maxWidth) {
         if (context.minecraft().font.width(text) <= maxWidth) {
            return text;
         }
         String suffix = "...";
         int suffixW = context.minecraft().font.width(suffix);
         if (maxWidth <= suffixW) {
            return context.minecraft().font.plainSubstrByWidth(text, maxWidth);
         }
         return context.minecraft().font.plainSubstrByWidth(text, maxWidth - suffixW) + suffix;
      }

      private record Hit(int x, int y, int w, int h, Identifier actionId, String payload,
                         boolean enabled, int selectIndex) {
         static Hit action(int x, int y, int w, int h, Identifier actionId, String payload, boolean enabled) {
            return new Hit(x, y, w, h, actionId, payload == null ? "" : payload, enabled, -1);
         }

         static Hit select(int x, int y, int w, int h, int index) {
            return new Hit(x, y, w, h, null, "", true, index);
         }
      }
   }
}
