package com.knoxhack.echoarmory.integration;

import com.knoxhack.echoarmory.content.ArmoryContent;
import com.knoxhack.echoarmory.content.ArmoryLoadoutDefinition;
import com.knoxhack.echoarmory.content.BossRecommendationDefinition;
import com.knoxhack.echoarmory.content.ModuleDefinition;
import com.knoxhack.echoarmory.item.ArmoryData;
import com.knoxhack.echoarmory.service.ArmoryReadinessService;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ArmoryTerminalClientIntegration {
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final int ACCENT = 0xFFFFD166;

   private ArmoryTerminalClientIntegration() {
   }

   public static void register() {
      if (!REGISTERED.compareAndSet(false, true)) {
         return;
      }
      TerminalTab tab = new ArmoryTab();
      TerminalTabRegistry.register(tab);
      TerminalNavigationProfiles.register(tab.descriptor().id(), TerminalNavigationProfile.system(120));
      TerminalAddonInfoRegistry.register(new ArmoryAddonInfoProvider());
   }

   private static final class ArmoryAddonInfoProvider implements TerminalAddonInfoProvider {
      @Override
      public String chapterId() {
         return ArmoryCoreIntegration.CHAPTER_ID;
      }

      @Override
      public TerminalAddonInfo info(Player player) {
         int modules = player == null ? 0 : ArmoryData.modules(player.getMainHandItem()).modules().size();
         int fracture = player == null ? 0 : ArmoryData.protection(player, ArmoryData.ProtectionType.FRACTURE);
         ArmoryReadinessService.Report report = player == null ? null : ArmoryReadinessService.bestReport(player).orElse(null);
         return new TerminalAddonInfo(
            "Modular combat loadouts, energy weapons, faction gear, and hazard protection planning.",
            List.of(
               new TerminalAddonMetric("Modules", String.valueOf(modules), "main-hand installed", ACCENT),
               new TerminalAddonMetric("Fracture", fracture + "%", "equipped mitigation", fracture >= 45 ? TerminalUi.GREEN : TerminalUi.AMBER),
               new TerminalAddonMetric("Kit", report == null ? "NONE" : report.state().name(), report == null ? "no report" : report.loadout().title(), report == null ? TerminalUi.CYAN : colorForState(report.state()))),
            List.of(new TerminalAddonSection("Armory Feed", List.of(
               player == null ? "Open Armory with player telemetry available." : "Main hand: " + player.getMainHandItem().getHoverName().getString(),
               report == null ? "Route-kit readiness unavailable." : report.firstBlocker(),
               "Faction unlocks: " + ArmoryContent.factionUnlocks().size()))),
            List.of(new TerminalAddonLink(ArmoryTerminalIds.ARMORY_TAB, "Armory", "Mission-ready gear", ACCENT)),
            TerminalAddonGuide.optional(610, "Combat route",
               "Armory is optional combat depth; use it when hazards, bosses, or mission loadouts need tighter preparation.",
               List.of("Craft base gear.", "Install modules at the Module Upgrade Table.", "Preview mission and boss readiness before deployment.")));
      }
   }

   private static final class ArmoryTab implements ClientTerminalTab {
      private final TerminalTabDescriptor descriptor = new TerminalTabDescriptor(ArmoryTerminalIds.ARMORY_TAB, "ARMORY", 120, ACCENT);
      private final TerminalTabChrome chrome = TerminalTabChrome.of("Armory", TerminalTabChrome.GROUP_SYSTEMS, "AR", "Mission gear", 120);
      private int scanX;
      private int equipX;
      private int installX;
      private int stanceX;
      private int rechargeX;
      private int previewX;
      private int logisticsX;
      private int actionY;
      private int loadoutRowX;
      private int loadoutRowY;
      private int loadoutRowW;
      private int loadoutRowCount;
      private int moduleRowX;
      private int moduleRowY;
      private int moduleRowW;
      private int moduleRowCount;
      private int bossRowX;
      private int bossRowY;
      private int bossRowW;
      private int bossRowCount;
      private String selectedLoadoutPayload = "";
      private String selectedModulePayload = "";
      private String selectedBossPayload = "";

      @Override
      public TerminalTabDescriptor descriptor() {
         return descriptor;
      }

      @Override
      public TerminalTabChrome chrome() {
         return chrome;
      }

      @Override
      public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
         Player player = context.player();
         int x = context.contentX() + 12;
         int y = context.contentY() + 10 - context.scrollY();
         int w = context.contentWidth() - 24;
         int modules = player == null ? 0 : ArmoryData.modules(player.getMainHandItem()).modules().size();
         int toxic = player == null ? 0 : ArmoryData.protection(player, ArmoryData.ProtectionType.TOXIC);
         int radiation = player == null ? 0 : ArmoryData.protection(player, ArmoryData.ProtectionType.RADIATION);
         int cold = player == null ? 0 : ArmoryData.protection(player, ArmoryData.ProtectionType.COLD);
         int heat = player == null ? 0 : ArmoryData.protection(player, ArmoryData.ProtectionType.HEAT);
         int fracture = player == null ? 0 : ArmoryData.protection(player, ArmoryData.ProtectionType.FRACTURE);
         List<ArmoryLoadoutDefinition> loadouts = ArmoryContent.loadouts();
         List<ModuleDefinition> moduleDefinitions = ArmoryContent.modules();
         List<BossRecommendationDefinition> recommendations = ArmoryContent.bossRecommendations();
         selectedLoadoutPayload = ensureSelection(selectedLoadoutPayload, loadouts.stream().map(loadout -> loadout.id().toString()).toList());
         selectedModulePayload = ensureSelection(selectedModulePayload, moduleDefinitions.stream().map(module -> module.id().toString()).toList());
         selectedBossPayload = ensureSelection(selectedBossPayload, recommendations.stream().map(recommendation -> recommendation.id().toString()).toList());
         ArmoryReadinessService.Report selectedReport = selectedReport(player, selectedLoadoutPayload);

         y = TerminalUi.sectionHeader(context, graphics, "ARMORY READINESS", "SYSTEM", x, y, w, ACCENT);
         TerminalUi.flatHudPanel(context, graphics, x, y, w, 112, ACCENT);
         TerminalUi.line(context, graphics, "Main hand: " + (player == null ? "telemetry offline" : player.getMainHandItem().getHoverName().getString()), x + 14, y + 14, w - 28, TerminalUi.text(context));
         TerminalUi.line(context, graphics, "Installed modules: " + modules, x + 14, y + 34, 160, modules > 0 ? TerminalUi.success(context) : TerminalUi.warning(context));
         TerminalUi.line(context, graphics, "Protection T/R/C/H/F " + toxic + "/" + radiation + "/" + cold + "/" + heat + "/" + fracture,
            x + 14, y + 54, w - 28, fracture >= 45 ? TerminalUi.success(context) : TerminalUi.warning(context));
         TerminalUi.line(context, graphics, selectedReport == null ? "Selected kit: unavailable" : selectedReport.loadout().title() + " // " + selectedReport.state() + " // " + selectedReport.firstBlocker(),
            x + 14, y + 74, w - 28, selectedReport == null ? TerminalUi.muted(context) : colorForState(selectedReport.state()));
         TerminalUi.line(context, graphics, selectedReport == null ? "Logistics unavailable" : "Logistics " + (selectedReport.logisticsAvailable() ? "available" : "unavailable")
            + " | Loadouts " + ArmoryContent.loadouts().size() + " | Gear " + ArmoryContent.gear().size(), x + 14, y + 94, w - 28, TerminalUi.muted(context));

         int cy = y + 130;
         TerminalUi.section(context, graphics, "MISSION KITS", x, cy, ACCENT);
         cy += 16;
         loadoutRowX = x;
         loadoutRowY = cy;
         loadoutRowW = w;
         loadoutRowCount = Math.min(4, loadouts.size());
         for (int i = 0; i < loadoutRowCount; i++) {
            ArmoryLoadoutDefinition loadout = loadouts.get(i);
            ArmoryReadinessService.Report report = player == null ? null : ArmoryReadinessService.report(player, loadout);
            boolean selected = loadout.id().toString().equals(selectedLoadoutPayload);
            boolean hover = TerminalUi.inside(mouseX, mouseY, x, cy, w, 28);
            int rowColor = selected ? ACCENT : report == null ? TerminalUi.muted(context) : colorForState(report.state());
            TerminalUi.densePanel(context, graphics, x, cy, w, 28, rowColor);
            if (selected || hover) {
               graphics.outline(x, cy, w, 28, selected ? ACCENT : TerminalUi.accent(context));
            }
            TerminalUi.line(context, graphics, loadout.title(), x + 8, cy + 6, w / 2, selected ? TerminalUi.text(context) : ACCENT);
            TerminalUi.line(context, graphics, report == null ? "No report" : report.state() + " | Tier " + loadout.minTier(), x + w / 2, cy + 6, w / 2 - 14, TerminalUi.muted(context));
            TerminalUi.line(context, graphics, report == null ? "No readiness detail" : report.firstBlocker(), x + 8, cy + 17, w - 16, TerminalUi.muted(context));
            cy += 32;
         }

         TerminalUi.section(context, graphics, "AUGMENTS", x, cy + 2, ACCENT);
         cy += 20;
         moduleRowX = x;
         moduleRowY = cy;
         moduleRowW = w;
         moduleRowCount = Math.min(4, moduleDefinitions.size());
         for (int i = 0; i < moduleRowCount; i++) {
            ModuleDefinition module = moduleDefinitions.get(i);
            boolean selected = module.id().toString().equals(selectedModulePayload);
            boolean hover = TerminalUi.inside(mouseX, mouseY, x, cy, w, 24);
            int rowColor = selected ? ACCENT : module.instability() > 10 ? TerminalUi.warning(context) : TerminalUi.muted(context);
            TerminalUi.densePanel(context, graphics, x, cy, w, 24, rowColor);
            if (selected || hover) {
               graphics.outline(x, cy, w, 24, selected ? ACCENT : TerminalUi.accent(context));
            }
            TerminalUi.line(context, graphics, module.title(), x + 8, cy + 7, w / 2, selected ? TerminalUi.text(context) : rowColor);
            TerminalUi.line(context, graphics, module.slotType() + " | cost " + module.energyCost() + " | risk " + module.instability(),
               x + w / 2, cy + 7, w / 2 - 14, TerminalUi.muted(context));
            cy += 28;
         }

         TerminalUi.section(context, graphics, "BOSS PREVIEW", x, cy + 2, ACCENT);
         cy += 20;
         bossRowX = x;
         bossRowY = cy;
         bossRowW = w;
         bossRowCount = Math.min(3, recommendations.size());
         for (int i = 0; i < bossRowCount; i++) {
            BossRecommendationDefinition recommendation = recommendations.get(i);
            boolean selected = recommendation.id().toString().equals(selectedBossPayload);
            boolean hover = TerminalUi.inside(mouseX, mouseY, x, cy, w, 24);
            int color = selected ? ACCENT : fracture >= recommendation.fractureProtection() ? TerminalUi.success(context) : TerminalUi.muted(context);
            TerminalUi.densePanel(context, graphics, x, cy, w, 24, color);
            if (selected || hover) {
               graphics.outline(x, cy, w, 24, selected ? ACCENT : TerminalUi.accent(context));
            }
            TerminalUi.line(context, graphics, recommendation.bossName(), x + 8, cy + 7, w / 2, selected ? TerminalUi.text(context) : color);
            TerminalUi.line(context, graphics, "Tier " + recommendation.minTier() + ", fracture " + recommendation.fractureProtection(),
               x + w / 2, cy + 7, w / 2 - 14, TerminalUi.muted(context));
            cy += 28;
         }

         actionY = cy + 10;
         scanX = x;
         equipX = x + 68;
         installX = x + 136;
         stanceX = x + 214;
         rechargeX = x;
         previewX = x + 90;
         logisticsX = x;
         TerminalUi.compactButton(context, graphics, scanX, actionY, 58, "SCAN", ACCENT, true, TerminalUi.inside(mouseX, mouseY, scanX, actionY, 58, 18));
         TerminalUi.compactButton(context, graphics, equipX, actionY, 58, "EQUIP", ACCENT, !selectedLoadoutPayload.isBlank(), TerminalUi.inside(mouseX, mouseY, equipX, actionY, 58, 18));
         TerminalUi.compactButton(context, graphics, installX, actionY, 68, "INSTALL", ACCENT, !selectedModulePayload.isBlank(), TerminalUi.inside(mouseX, mouseY, installX, actionY, 68, 18));
         TerminalUi.compactButton(context, graphics, stanceX, actionY, 68, "STANCE", ACCENT, true, TerminalUi.inside(mouseX, mouseY, stanceX, actionY, 68, 18));
         TerminalUi.compactButton(context, graphics, rechargeX, actionY + 24, 80, "RECHARGE", ACCENT, true, TerminalUi.inside(mouseX, mouseY, rechargeX, actionY + 24, 80, 18));
         TerminalUi.compactButton(context, graphics, previewX, actionY + 24, 76, "PREVIEW", ACCENT, !selectedBossPayload.isBlank(), TerminalUi.inside(mouseX, mouseY, previewX, actionY + 24, 76, 18));
         TerminalUi.compactButton(context, graphics, logisticsX, actionY + 48, 84, "LOGISTICS", ACCENT, !selectedLoadoutPayload.isBlank(), TerminalUi.inside(mouseX, mouseY, logisticsX, actionY + 48, 84, 18));
      }

      private static String ensureSelection(String current, List<String> ids) {
         if (ids == null || ids.isEmpty()) {
            return "";
         }
         if (current != null && ids.contains(current)) {
            return current;
         }
         return ids.getFirst();
      }

      private static ArmoryReadinessService.Report selectedReport(Player player, String selectedLoadoutPayload) {
         if (player == null) {
            return null;
         }
         return ArmoryReadinessService.report(player, selectedLoadoutPayload)
            .or(() -> ArmoryReadinessService.bestReport(player))
            .orElse(null);
      }

      @Override
      public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
         if (button != 0) {
            return false;
         }
         if (TerminalUi.inside(mouseX, mouseY, scanX, actionY, 58, 18)) {
            context.sendAction(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.SCAN_ACTION, "");
            return true;
         }
         if (loadoutRowCount > 0 && TerminalUi.inside(mouseX, mouseY, loadoutRowX, loadoutRowY, loadoutRowW, loadoutRowCount * 32)) {
            int index = Math.max(0, Math.min(loadoutRowCount - 1, ((int)mouseY - loadoutRowY) / 32));
            List<ArmoryLoadoutDefinition> loadouts = ArmoryContent.loadouts();
            if (index < loadouts.size()) {
               selectedLoadoutPayload = loadouts.get(index).id().toString();
            }
            return true;
         }
         if (moduleRowCount > 0 && TerminalUi.inside(mouseX, mouseY, moduleRowX, moduleRowY, moduleRowW, moduleRowCount * 28)) {
            int index = Math.max(0, Math.min(moduleRowCount - 1, ((int)mouseY - moduleRowY) / 28));
            List<ModuleDefinition> modules = ArmoryContent.modules();
            if (index < modules.size()) {
               selectedModulePayload = modules.get(index).id().toString();
            }
            return true;
         }
         if (bossRowCount > 0 && TerminalUi.inside(mouseX, mouseY, bossRowX, bossRowY, bossRowW, bossRowCount * 28)) {
            int index = Math.max(0, Math.min(bossRowCount - 1, ((int)mouseY - bossRowY) / 28));
            List<BossRecommendationDefinition> recommendations = ArmoryContent.bossRecommendations();
            if (index < recommendations.size()) {
               selectedBossPayload = recommendations.get(index).id().toString();
            }
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, equipX, actionY, 58, 18) && !selectedLoadoutPayload.isBlank()) {
            context.sendAction(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.EQUIP_ACTION, selectedLoadoutPayload);
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, installX, actionY, 68, 18) && !selectedModulePayload.isBlank()) {
            context.sendAction(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.INSTALL_ACTION, selectedModulePayload);
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, stanceX, actionY, 68, 18)) {
            context.sendAction(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.STANCE_ACTION, "");
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, rechargeX, actionY + 24, 80, 18)) {
            context.sendAction(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.RECHARGE_ACTION, "");
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, previewX, actionY + 24, 76, 18) && !selectedBossPayload.isBlank()) {
            context.sendAction(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.PREVIEW_ACTION, selectedBossPayload);
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, logisticsX, actionY + 48, 84, 18) && !selectedLoadoutPayload.isBlank()) {
            context.sendAction(ArmoryTerminalIds.ARMORY_TAB, ArmoryTerminalIds.LOGISTICS_ACTION, selectedLoadoutPayload);
            return true;
         }
         return false;
      }

      @Override
      public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
         return false;
      }

      @Override
      public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
         return false;
      }

      @Override
      public int contentHeight(TerminalRenderContext context) {
         return 680;
      }
   }

   private static int colorForState(ArmoryReadinessService.State state) {
      return switch (state) {
         case READY -> TerminalUi.GREEN;
         case STAGED -> TerminalUi.CYAN;
         case MISSING -> TerminalUi.AMBER;
         case LOCKED -> TerminalUi.RED;
      };
   }
}
