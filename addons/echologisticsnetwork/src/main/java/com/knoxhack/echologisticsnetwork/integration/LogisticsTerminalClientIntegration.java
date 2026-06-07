package com.knoxhack.echologisticsnetwork.integration;

import com.knoxhack.echologisticsnetwork.block.entity.LogisticsBlockEntity;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public final class LogisticsTerminalClientIntegration {
   private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
   private static final int ACCENT = 0xFF66E8FF;

   private LogisticsTerminalClientIntegration() {
   }

   public static void register() {
      if (!REGISTERED.compareAndSet(false, true)) {
         return;
      }
      TerminalTab tab = new LogisticsTab();
      TerminalTabRegistry.register(tab);
      TerminalNavigationProfiles.register(tab.descriptor().id(), TerminalNavigationProfile.system(110));
   }

   private static final class LogisticsTab implements ClientTerminalTab {
      private final TerminalTabDescriptor descriptor = new TerminalTabDescriptor(LogisticsTerminalIds.LOGISTICS_TAB, "LOGISTICS", 110, ACCENT);
      private final TerminalTabChrome chrome = TerminalTabChrome.of("Logistics", TerminalTabChrome.GROUP_SYSTEMS, "LN", "Supply operations", 110);
      private int scanX;
      private int requestX;
      private int dispatchX;
      private int cancelX;
      private int offersX;
      private int relayX;
      private int actionY;
      private int secondaryActionY;
      private String requestPayload = "";
      private boolean canRequest;
      private boolean canDispatch;
      private boolean canCancel;
      private boolean canClaimRelay;
      private boolean canRefreshOffers;

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
         LogisticsTerminalSupport.TerminalView view = LogisticsTerminalSupport.view(context.player());
         LogisticsNetworkService.LogisticsSnapshot snapshot = view.snapshot();
         int x = context.contentX() + 12;
         int y = context.contentY() + 10 - context.scrollY();
         int w = context.contentWidth() - 24;
         long readyLoadouts = snapshot.loadoutReadiness().stream().filter(LogisticsNetworkService.LoadoutReadiness::ready).count();
         int pendingRelayRewards = LogisticsNetworkService.pendingRelayRewards(context.player());
         requestPayload = view.requestPayload();
         canRequest = snapshot.canRequest();
         canDispatch = snapshot.canDispatch();
         canCancel = snapshot.activeDeliveries() > 0;
         canClaimRelay = pendingRelayRewards > 0 && view.relayOnline();
         canRefreshOffers = view.depotOnline() && !snapshot.depotOffers().isEmpty();

         y = TerminalUi.sectionHeader(context, graphics, "SUPPLY NETWORK", "SYSTEM", x, y, w, ACCENT);
         TerminalUi.flatHudPanel(context, graphics, x, y, w, 96, ACCENT);
         TerminalUi.line(context, graphics, "Network: " + view.networkId(), x + 14, y + 14, Math.max(120, w / 2 - 20), TerminalUi.text(context));
         TerminalUi.line(context, graphics, "Dock: " + (view.dockOnline() ? "ONLINE" : "OFFLINE"), x + w / 2, y + 14, w / 2 - 18,
            view.dockOnline() ? TerminalUi.success(context) : TerminalUi.warning(context));
         TerminalUi.line(context, graphics, "Supply categories: " + snapshot.stockRows().size(), x + 14, y + 34, 150, TerminalUi.text(context));
         TerminalUi.line(context, graphics, "Low stock: " + snapshot.missingRows().size(), x + 174, y + 34, 120,
            snapshot.missingRows().isEmpty() ? TerminalUi.success(context) : TerminalUi.warning(context));
         TerminalUi.line(context, graphics, "Ready kits: " + readyLoadouts + "/" + snapshot.loadoutReadiness().size(),
            x + 14, y + 54, 160, readyLoadouts > 0 ? TerminalUi.success(context) : TerminalUi.warning(context));
         TerminalUi.line(context, graphics, "Active drones: " + snapshot.activeDeliveries(), x + 174, y + 54, 120, TerminalUi.accent(context));
         TerminalUi.line(context, graphics, "Depot offers: " + snapshot.depotOffers().size() + " | Relay rewards: "
            + pendingRelayRewards, x + 14, y + 74, w - 28, TerminalUi.muted(context));

         int cy = y + 114;
         TerminalUi.section(context, graphics, "PROGRESSION LOOP", x, cy, ACCENT);
         cy += 16;
         String targetLine = view.endpoint() == null ? "No locker, requester, or restock station in range." : targetLabel(view.endpoint());
         cy = TerminalUi.objectiveRow(context, graphics, x, cy, w, "Route endpoint", targetLine, view.endpoint() != null, ACCENT);
         cy = TerminalUi.objectiveRow(context, graphics, x, cy, w, "Selected loadout",
            view.selectedLoadoutTitle() + (view.selectedReady() ? " ready" : " missing " + view.selectedMissing()),
            view.selectedReady(), ACCENT);
         cy = TerminalUi.objectiveRow(context, graphics, x, cy, w, "Courier dock",
            view.dockOnline() ? "Online for network " + view.networkId() : "No Drone Delivery Dock discovered nearby.",
            view.dockOnline(), ACCENT);
         if (!snapshot.dispatchBlockedReason().isBlank()) {
            cy = TerminalUi.objectiveRow(context, graphics, x, cy, w, "Dispatch state",
               snapshot.dispatchBlockedReason(), false, ACCENT);
         }
         cy = TerminalUi.objectiveRow(context, graphics, x, cy, w, "Depot and relay",
            (view.depotOnline() ? "Depot online" + (view.depotCooldown() > 0 ? " cooling " + view.depotCooldown() + "t" : "") : "Depot offline")
               + " | relay " + (view.relayOnline() ? "online" : "offline"),
            view.depotOnline() || view.relayOnline(), ACCENT);

         if (!snapshot.deliveryJobs().isEmpty()) {
            TerminalUi.section(context, graphics, "ACTIVE DELIVERIES", x, cy, ACCENT);
            cy += 16;
            int shownJobs = 0;
            long gameTime = context.player() == null ? 0L : context.player().level().getGameTime();
            for (LogisticsNetworkService.DeliveryJob job : snapshot.deliveryJobs()) {
               TerminalUi.densePanel(context, graphics, x, cy, w, 28, ACCENT);
               TerminalUi.line(context, graphics, job.presetId().getPath().replace('_', ' '), x + 8, cy + 6, w / 2, TerminalUi.accent(context));
               TerminalUi.line(context, graphics, job.status().toUpperCase(java.util.Locale.ROOT) + " | ETA " + Math.max(0, job.etaTick() - gameTime) + "t",
                  x + w / 2, cy + 6, w / 2 - 14, TerminalUi.muted(context));
               TerminalUi.line(context, graphics, "Target " + job.targetPos().toShortString(), x + 8, cy + 17, w - 16, TerminalUi.muted(context));
               cy += 32;
               if (++shownJobs >= 3) {
                  break;
               }
            }
         }

         TerminalUi.section(context, graphics, "LOADOUT READINESS", x, cy, ACCENT);
         cy += 16;
         int row = 0;
         for (LogisticsNetworkService.LoadoutReadiness readiness : snapshot.loadoutReadiness()) {
            int color = readiness.ready() ? TerminalUi.success(context) : TerminalUi.warning(context);
            TerminalUi.densePanel(context, graphics, x, cy, w, 24, color);
            TerminalUi.line(context, graphics, readiness.title(), x + 8, cy + 7, w / 2, color);
            TerminalUi.line(context, graphics, readiness.ready() ? "READY" : "MISSING " + readiness.missingCount(), x + w - 120, cy + 7, 112, color);
            cy += 28;
            if (++row >= 5) {
               break;
            }
         }

         actionY = cy + 8;
         secondaryActionY = actionY + 24;
         scanX = x;
         requestX = x + 70;
         dispatchX = x + 160;
         relayX = x + 254;
         offersX = x;
         cancelX = x + 88;
         TerminalUi.compactButton(context, graphics, scanX, actionY, 58, "SCAN", ACCENT, true, TerminalUi.inside(mouseX, mouseY, scanX, actionY, 58, 18));
         TerminalUi.compactButton(context, graphics, requestX, actionY, 78, "REQUEST", ACCENT, canRequest, TerminalUi.inside(mouseX, mouseY, requestX, actionY, 78, 18));
         TerminalUi.compactButton(context, graphics, dispatchX, actionY, 82, "DISPATCH", ACCENT, canDispatch, TerminalUi.inside(mouseX, mouseY, dispatchX, actionY, 82, 18));
         TerminalUi.compactButton(context, graphics, relayX, actionY, 72, "CLAIM", ACCENT, canClaimRelay, TerminalUi.inside(mouseX, mouseY, relayX, actionY, 72, 18));
         TerminalUi.compactButton(context, graphics, offersX, secondaryActionY, 76, "OFFERS", ACCENT, canRefreshOffers, TerminalUi.inside(mouseX, mouseY, offersX, secondaryActionY, 76, 18));
         TerminalUi.compactButton(context, graphics, cancelX, secondaryActionY, 76, "CANCEL", ACCENT, canCancel, TerminalUi.inside(mouseX, mouseY, cancelX, secondaryActionY, 76, 18));
      }

      @Override
      public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
         if (button != 0) {
            return false;
         }
         if (TerminalUi.inside(mouseX, mouseY, scanX, actionY, 58, 18)) {
            context.sendAction(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.SCAN_ACTION, "");
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, requestX, actionY, 78, 18) && canRequest) {
            context.sendAction(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.REQUEST_ACTION, requestPayload);
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, dispatchX, actionY, 82, 18) && canDispatch) {
            context.sendAction(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.DISPATCH_ACTION, requestPayload);
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, relayX, actionY, 72, 18) && canClaimRelay) {
            context.sendAction(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.CLAIM_RELAY_ACTION, "");
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, offersX, secondaryActionY, 76, 18) && canRefreshOffers) {
            context.sendAction(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.REFRESH_OFFERS_ACTION, "");
            return true;
         }
         if (TerminalUi.inside(mouseX, mouseY, cancelX, secondaryActionY, 76, 18) && canCancel) {
            context.sendAction(LogisticsTerminalIds.LOGISTICS_TAB, LogisticsTerminalIds.CANCEL_ACTION, requestPayload);
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
         return 520;
      }

      private static String targetLabel(LogisticsBlockEntity endpoint) {
         return endpoint.kind().displayName() + " @ " + endpoint.getBlockPos().toShortString();
      }
   }
}
