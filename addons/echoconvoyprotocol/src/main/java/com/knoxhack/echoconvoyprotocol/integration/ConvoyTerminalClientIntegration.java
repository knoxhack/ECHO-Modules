package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echoconvoyprotocol.network.ConvoyTerminalClientState;
import com.knoxhack.echoconvoyprotocol.network.ConvoyTerminalStatePacket;
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

public final class ConvoyTerminalClientIntegration {
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final int ACCENT = 0xFF92D66B;

   private ConvoyTerminalClientIntegration() {
   }

   public static void register() {
      if (!REGISTERED.compareAndSet(false, true)) {
         return;
      }
      TerminalTab tab = new ConvoyTab();
      TerminalTabRegistry.register(tab);
      TerminalNavigationProfiles.register(tab.descriptor().id(), TerminalNavigationProfile.chapter("convoy_protocol", "Convoy Protocol", "CV", 260));
   }

   private static final class ConvoyTab implements ClientTerminalTab {
      private final TerminalTabDescriptor descriptor = new TerminalTabDescriptor(ConvoyTerminalIds.CONVOY_TAB, "CONVOY ROUTES", 260, ACCENT);
      private final TerminalTabChrome chrome = TerminalTabChrome.of("Convoy Routes", TerminalTabChrome.GROUP_FIELD, "CV", "Road operations", 260);
      private int scanX;
      private int startX;
      private int completeX;
      private int claimX;
      private int actionY;
      private int routeRowX;
      private int routeRowY;
      private int routeRowW;
      private int routeRowCount;
      private int selectedRouteIndex;
      private String selectedRoutePayload = "";
      private String selectedRouteAction = "";
      private String claimPayload = "";
      private boolean hasActiveRoute;
      private long lastRefreshTick = -200L;

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
         requestRefresh(context);
      }

      @Override
      public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
         maybeRefresh(context);
         ConvoyTerminalStatePacket snapshot = ConvoyTerminalClientState.snapshot();
         syncSelectedRoute(snapshot);
         claimPayload = snapshot.claimRouteId();
         hasActiveRoute = snapshot.hasActiveRoute();
         int x = context.contentX() + 12;
         int y = context.contentY() + 10 - context.scrollY();
         int w = context.contentWidth() - 24;
         y = TerminalUi.sectionHeader(context, graphics, "CONVOY ROUTES", "FIELD", x, y, w, ACCENT);
         TerminalUi.flatHudPanel(context, graphics, x, y, w, 92, ACCENT);
         TerminalUi.line(context, graphics, snapshot.vehicleTitle(), x + 14, y + 12, w - 28, TerminalUi.accent(context));
         TerminalUi.line(context, graphics, snapshot.vehicleStatus(), x + 14, y + 32, w - 28,
            snapshot.vehicleTitle().startsWith("No ") ? TerminalUi.warning(context) : TerminalUi.text(context));
         TerminalUi.line(context, graphics, snapshot.vehicleCargo(), x + 14, y + 52, w / 2 - 20, TerminalUi.text(context));
         TerminalUi.line(context, graphics, "Sync " + snapshot.gameTime() + "t", x + w / 2, y + 52, w / 2 - 20, TerminalUi.muted(context));
         TerminalUi.line(context, graphics, snapshot.activeRouteTitle(), x + 14, y + 72, w - 28,
            snapshot.hasActiveRoute() ? TerminalUi.success(context) : TerminalUi.muted(context));

         int cy = y + 110;
         TerminalUi.section(context, graphics, "ACTIVE ROUTE", x, cy, ACCENT);
         cy += 16;
         cy = infoRow(context, graphics, x, cy, w, snapshot.activeRouteStatus(), snapshot.hasActiveRoute() ? TerminalUi.success(context) : TerminalUi.muted(context));
         cy = infoRow(context, graphics, x, cy, w, snapshot.activeLegStatus(), snapshot.hasActiveRoute() ? TerminalUi.accent(context) : TerminalUi.muted(context));
         cy = infoRow(context, graphics, x, cy, w, snapshot.checkpointStatus(),
            snapshot.checkpointStatus().contains("blocked") ? TerminalUi.warning(context) : TerminalUi.text(context));

         TerminalUi.section(context, graphics, "FIELD ASSISTANT", x, cy + 4, ACCENT);
         cy += 20;
         cy = infoRow(context, graphics, x, cy, w, snapshot.assistantStatus(),
            snapshot.assistantStatus().startsWith("Prep") || snapshot.assistantStatus().startsWith("Setup")
               ? TerminalUi.warning(context)
               : TerminalUi.accent(context));
         cy = lineList(context, graphics, x, cy, w, snapshot.assistantLines(), 3, TerminalUi.text(context), "Scan routes for acquisition hints.");

         TerminalUi.section(context, graphics, "CARGO", x, cy + 4, ACCENT);
         cy += 20;
         cy = lineList(context, graphics, x, cy, w, snapshot.cargoLines(), 4, TerminalUi.text(context), "Cargo bay empty.");

         TerminalUi.section(context, graphics, "NEARBY ROADSIDE POIS", x, cy + 4, ACCENT);
         cy += 20;
         cy = lineList(context, graphics, x, cy, w, snapshot.nearbyPoiLines(), 5, TerminalUi.muted(context), "No convoy roadside POIs within 18 blocks.");

         TerminalUi.section(context, graphics, "ROUTE BOARD", x, cy + 4, ACCENT);
         cy += 20;
         cy = routeBoard(context, graphics, x, cy, w, snapshot, mouseX, mouseY);

         actionY = cy + 8;
         scanX = x;
         startX = x + 82;
         completeX = x + 180;
         claimX = x + 286;
         boolean referenceOnly = TerminalNavigationProfiles.referenceOnlyChapterContext(context);
         boolean canStartSelectedRoute = "start".equals(selectedRouteAction) && !hasActiveRoute && !selectedRoutePayload.isBlank();
         boolean canClaimSelectedRoute = "claim".equals(selectedRouteAction) && !selectedRoutePayload.isBlank();
         TerminalUi.compactButton(context, graphics, scanX, actionY, 70, "SCAN", ACCENT, !referenceOnly,
            !referenceOnly && TerminalUi.inside(mouseX, mouseY, scanX, actionY, 70, 18));
         TerminalUi.compactButton(context, graphics, startX, actionY, 86, "START", ACCENT, !referenceOnly && canStartSelectedRoute,
            !referenceOnly && TerminalUi.inside(mouseX, mouseY, startX, actionY, 86, 18));
         TerminalUi.compactButton(context, graphics, completeX, actionY, 94, "SIGNAL", ACCENT, !referenceOnly && hasActiveRoute,
            !referenceOnly && TerminalUi.inside(mouseX, mouseY, completeX, actionY, 94, 18));
         TerminalUi.compactButton(context, graphics, claimX, actionY, 74, "CLAIM", ACCENT,
            !referenceOnly && (canClaimSelectedRoute || !claimPayload.isBlank()),
            !referenceOnly && TerminalUi.inside(mouseX, mouseY, claimX, actionY, 74, 18));
         if (referenceOnly) {
            TerminalUi.line(context, graphics, "Reference view. Use Survival Route for progression commands.",
               x, actionY + 24, w, TerminalUi.muted(context));
         }
      }

      private static int infoRow(TerminalRenderContext context, GuiGraphicsExtractor graphics, int x, int y, int w, String text, int color) {
         TerminalUi.densePanel(context, graphics, x, y, w, 24, color);
         TerminalUi.line(context, graphics, text, x + 8, y + 7, w - 16, color);
         return y + 28;
      }

      private static int lineList(
         TerminalRenderContext context,
         GuiGraphicsExtractor graphics,
         int x,
         int y,
         int w,
         java.util.List<String> lines,
         int max,
         int color,
         String emptyText
      ) {
         if (lines == null || lines.isEmpty()) {
            TerminalUi.line(context, graphics, emptyText, x + 4, y + 4, w - 8, TerminalUi.muted(context));
            return y + 20;
         }
         int shown = 0;
         for (String line : lines) {
            TerminalUi.densePanel(context, graphics, x, y, w, 23, color);
            TerminalUi.line(context, graphics, line, x + 8, y + 7, w - 16, color);
            y += 27;
            if (++shown >= max) {
               break;
            }
         }
         return y;
      }

      private int routeBoard(
         TerminalRenderContext context,
         GuiGraphicsExtractor graphics,
         int x,
         int y,
         int w,
         ConvoyTerminalStatePacket snapshot,
         int mouseX,
         int mouseY
      ) {
         List<String> lines = snapshot.routeBoardLines();
         routeRowX = x;
         routeRowY = y;
         routeRowW = w;
         routeRowCount = lines == null ? 0 : Math.min(7, lines.size());
         if (routeRowCount == 0) {
            TerminalUi.line(context, graphics, "No route definitions loaded.", x + 4, y + 4, w - 8, TerminalUi.muted(context));
            return y + 20;
         }
         for (int i = 0; i < routeRowCount; i++) {
            boolean selected = i == selectedRouteIndex;
            boolean hover = TerminalUi.inside(mouseX, mouseY, x, y, w, 23);
            int rowColor = selected ? ACCENT : routeColor(actionAt(snapshot, i), context);
            TerminalUi.densePanel(context, graphics, x, y, w, 23, rowColor);
            if (selected || hover) {
               graphics.outline(x, y, w, 23, selected ? ACCENT : TerminalUi.accent(context));
            }
            TerminalUi.line(context, graphics, lines.get(i), x + 8, y + 7, w - 16, selected ? TerminalUi.text(context) : rowColor);
            y += 27;
         }
         return y;
      }

      private void syncSelectedRoute(ConvoyTerminalStatePacket snapshot) {
         List<String> routeIds = snapshot.routeBoardRouteIds();
         if (routeIds == null || routeIds.isEmpty()) {
            selectedRouteIndex = 0;
            selectedRoutePayload = "";
            selectedRouteAction = "";
            return;
         }
         int current = indexOf(routeIds, selectedRoutePayload);
         selectedRouteIndex = current >= 0 ? current : preferredRouteIndex(snapshot);
         selectedRoutePayload = routeIds.get(Math.max(0, Math.min(selectedRouteIndex, routeIds.size() - 1)));
         selectedRouteAction = actionAt(snapshot, selectedRouteIndex);
      }

      private int preferredRouteIndex(ConvoyTerminalStatePacket snapshot) {
         int claimIndex = indexOf(snapshot.routeBoardActions(), "claim");
         if (claimIndex >= 0) {
            return claimIndex;
         }
         int startIndex = indexOf(snapshot.routeBoardActions(), "start");
         if (startIndex >= 0) {
            return startIndex;
         }
         int activeIndex = indexOf(snapshot.routeBoardActions(), "active");
         return Math.max(0, activeIndex);
      }

      private static int indexOf(List<String> values, String value) {
         if (values == null || value == null || value.isBlank()) {
            return -1;
         }
         for (int i = 0; i < values.size(); i++) {
            if (value.equals(values.get(i))) {
               return i;
            }
         }
         return -1;
      }

      private static String actionAt(ConvoyTerminalStatePacket snapshot, int index) {
         List<String> actions = snapshot.routeBoardActions();
         return actions != null && index >= 0 && index < actions.size() ? actions.get(index) : "";
      }

      private static int routeColor(String action, TerminalRenderContext context) {
         return switch (action) {
            case "start" -> TerminalUi.success(context);
            case "claim" -> TerminalUi.warning(context);
            case "active" -> TerminalUi.accent(context);
            case "claimed" -> TerminalUi.muted(context);
            default -> TerminalUi.muted(context);
         };
      }

      private void maybeRefresh(TerminalRenderContext context) {
         if (context.player() == null) {
            return;
         }
         if (ConvoyTerminalClientState.snapshot().gameTime() == 0L && lastRefreshTick < 0L) {
            requestRefresh(context);
         }
      }

      private void requestRefresh(TerminalRenderContext context) {
         if (context.player() == null || TerminalNavigationProfiles.referenceOnlyChapterContext(context)) {
            return;
         }
         long gameTime = context.player().level().getGameTime();
         lastRefreshTick = gameTime;
         context.sendAction(ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.SCAN_ACTION, "");
      }

      @Override
      public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
         if (button != 0) {
            return false;
         }
         boolean referenceOnly = TerminalNavigationProfiles.referenceOnlyChapterContext(context);
         if (TerminalUi.inside(mouseX, mouseY, scanX, actionY, 70, 18)) {
            if (referenceOnly) {
               context.playRejectedSound();
               return true;
            }
            context.sendAction(ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.SCAN_ACTION, "");
            return true;
         }
         if (routeRowCount > 0 && TerminalUi.inside(mouseX, mouseY, routeRowX, routeRowY, routeRowW, routeRowCount * 27)) {
            selectedRouteIndex = Math.max(0, Math.min(routeRowCount - 1, ((int)mouseY - routeRowY) / 27));
            ConvoyTerminalStatePacket snapshot = ConvoyTerminalClientState.snapshot();
            List<String> routeIds = snapshot.routeBoardRouteIds();
            if (selectedRouteIndex < routeIds.size()) {
               selectedRoutePayload = routeIds.get(selectedRouteIndex);
               selectedRouteAction = actionAt(snapshot, selectedRouteIndex);
            }
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, startX, actionY, 86, 18)
            && "start".equals(selectedRouteAction)
            && !hasActiveRoute
            && !selectedRoutePayload.isBlank()) {
            if (referenceOnly) {
               context.playRejectedSound();
               return true;
            }
            context.sendAction(ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.START_ACTION, selectedRoutePayload);
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, completeX, actionY, 94, 18) && hasActiveRoute) {
            if (referenceOnly) {
               context.playRejectedSound();
               return true;
            }
            context.sendAction(ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.COMPLETE_ACTION, "");
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, claimX, actionY, 74, 18)) {
            if (referenceOnly) {
               context.playRejectedSound();
               return true;
            }
            String payload = "claim".equals(selectedRouteAction) ? selectedRoutePayload : claimPayload;
            if (!payload.isBlank()) {
               context.sendAction(ConvoyTerminalIds.CONVOY_TAB, ConvoyTerminalIds.CLAIM_ACTION, payload);
               return true;
            }
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
         return 720;
      }
   }
}
