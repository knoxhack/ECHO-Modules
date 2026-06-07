package com.knoxhack.echoblackboxprotocol;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echoblackboxprotocol.client.BlackboxMachineScreen;
import com.knoxhack.echoblackboxprotocol.integration.BlackboxMissionProvider;
import com.knoxhack.echoblackboxprotocol.integration.BlackboxTerminalIds;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEnding;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEndings;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxProgress;
import com.knoxhack.echoblackboxprotocol.progression.MemoryType;
import com.knoxhack.echoblackboxprotocol.registry.ModMenus;
import com.knoxhack.echoblackboxprotocol.registry.ModEntities;
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
import com.knoxhack.echocore.client.model.EchoMobFamily;
import com.knoxhack.echocore.client.model.EchoMobFamilyRenderer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public class EchoBlackboxProtocolClient {
   public EchoBlackboxProtocolClient() {
      this(null);
   }

   public EchoBlackboxProtocolClient(Object modEventBus) {
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoBlackboxProtocolClient::registerEntityRenderers);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoBlackboxProtocolClient::registerMenuScreens);
      if (EchoRuntimeModules.isLoaded("echoterminal")) {
         BlackboxClientTabs.register();
      }
   }

   static void registerEntityRenderers(Object event) {
      if (EchoRuntimeModules.isLoaded("echorendercore") && registerRenderCoreEntityRenderers(event)) {
         return;
      }
      registerFallbackEntityRenderers(event);
   }

   private static void registerFallbackEntityRenderers(Object event) {
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.ARCHIVE_HUSK.get(),
            renderer("archive_husk", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.SECURITY_ECHO.get(),
            renderer("security_echo", EchoMobFamily.HUMANOID, 1.03F, 0.52F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.MEMORY_PARASITE.get(),
            renderer("memory_parasite", EchoMobFamily.CRAWLER, 0.72F, 0.28F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.FALSE_ECHO_MINION.get(),
            renderer("false_echo_minion", EchoMobFamily.HUMANOID, 1.0F, 0.5F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.COMMAND_REMNANT_MINION.get(),
            renderer("command_remnant_minion", EchoMobFamily.HUMANOID, 1.05F, 0.55F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.BLACKBOX_SENTINEL.get(),
            renderer("blackbox_sentinel", EchoMobFamily.HEAVY_BOSS, 1.25F, 0.72F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.FALSE_ECHO.get(),
            renderer("false_echo", EchoMobFamily.HEAVY_BOSS, 1.16F, 0.65F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.COMMAND_REMNANT.get(),
            renderer("command_remnant", EchoMobFamily.HEAVY_BOSS, 1.25F, 0.75F));
      EchoBackendClientBridge.registerEntityRenderer(event, ModEntities.NEXUS_GUARDIAN.get(),
            renderer("nexus_guardian", EchoMobFamily.HEAVY_BOSS, 1.38F, 0.88F));
   }

   private static boolean registerRenderCoreEntityRenderers(Object event) {
      try {
         Class.forName("com.knoxhack.echoblackboxprotocol.integration.BlackboxRenderCoreClientIntegration")
            .getMethod("registerEntityRenderers", Object.class)
            .invoke(null, event);
         return true;
      } catch (ReflectiveOperationException | LinkageError exception) {
         EchoBlackboxProtocol.LOGGER.warn("ECHO Blackbox Protocol RenderCore entity renderer integration unavailable; using generated fallback renderers.", exception);
         return false;
      }
   }

   private static <T extends Mob> EntityRendererProvider<T> renderer(String entityName, EchoMobFamily family,
         float scale, float shadow) {
      return context -> new EchoMobFamilyRenderer<>(context, EchoBlackboxProtocol.MODID, entityName, family, scale, shadow);
   }

   static void registerMenuScreens(Object event) {
      EchoBackendClientBridge.registerMenuScreen(event, ModMenus.BLACKBOX_MACHINE.get(), BlackboxMachineScreen.class);
   }

   private static final class BlackboxClientTabs {
      private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

      private BlackboxClientTabs() {
      }

      static void register() {
         if (REGISTERED.compareAndSet(false, true)) {
            registerTab(new AccessTab(), 420);
            registerTab(new MemoryArchiveTab(), 421);
            registerTab(new TruthEngineTab(), 422);
            TerminalAddonInfoRegistry.register(new BlackboxAddonInfoProvider());
         }
      }

      private static void registerTab(TerminalTab tab, int order) {
         TerminalTabRegistry.register(tab);
         TerminalNavigationProfiles.register(
            tab.descriptor().id(),
            TerminalNavigationProfile.chapter("blackbox", "Chapter 5: Blackbox Protocol", "C5", order)
         );
      }

      private static final class BlackboxAddonInfoProvider implements TerminalAddonInfoProvider {
         @Override
         public String chapterId() {
            return BlackboxTerminalIds.CHAPTER_ID.getPath();
         }

         @Override
         public TerminalAddonInfo info(Player player) {
            if (player == null) {
               return new TerminalAddonInfo(
                  "Final chapter access, memory archive, and Truth Engine readiness.",
                  List.of(new TerminalAddonMetric("Signal", "OFFLINE", "waiting for player telemetry", 0xFF7BDEFF)),
                  List.of(new TerminalAddonSection("Archive Feed",
                     List.of("Open Blackbox Access after player telemetry is available."))),
                  links(),
                  guide());
            }
            BlackboxProgress progress = BlackboxProgress.get(player);
            return new TerminalAddonInfo(
               "Final chapter access, memory archive, and Truth Engine readiness.",
               List.of(
                  new TerminalAddonMetric("Memories", String.valueOf(progress.decodedMemoryTotal()),
                     "decoded memory records", 0xFF9FD1FF),
                  new TerminalAddonMetric("Stability", progress.stability() + "%",
                     "memory integrity", progress.stability() <= 25 ? TerminalUi.RED : TerminalUi.GREEN),
                  new TerminalAddonMetric("False Signals", String.valueOf(progress.falseSignalCount()),
                     "active interference", progress.falseSignalCount() > 4 ? TerminalUi.AMBER : TerminalUi.MUTED),
                  new TerminalAddonMetric("Ending", progress.ending().displayName(),
                     progress.hasNexusCoreAccessKey() ? "core access key assembled" : "core key missing",
                     progress.ending() == BlackboxEnding.NONE ? TerminalUi.MUTED : TerminalUi.AMBER)),
               List.of(new TerminalAddonSection("Archive Feed", List.of(
                  memoryLine(progress, MemoryType.PERSONAL),
                  memoryLine(progress, MemoryType.ECHO),
                  memoryLine(progress, MemoryType.SECURITY),
                  memoryLine(progress, MemoryType.COMMAND),
                  memoryLine(progress, MemoryType.CORE),
                  memoryLine(progress, MemoryType.DELETED)))),
               links(),
               guide());
         }

         private static TerminalAddonGuide guide() {
            return TerminalAddonGuide.mainline(5, 50, "Final chapter",
               "Start Blackbox Protocol after late-story handoffs are complete and you are ready to resolve memory archives and endings.",
               List.of(
                  "Open Blackbox Access to confirm route availability.",
                  "Decode memory archives before committing to final proof.",
                  "Use Truth Engine only when ending readiness is clear."));
         }

         private static String memoryLine(BlackboxProgress progress, MemoryType type) {
            return type.displayName() + ": " + progress.memoryCount(type) + " record(s)";
         }

         private static List<TerminalAddonLink> links() {
            return List.of(
               new TerminalAddonLink(BlackboxTerminalIds.ACCESS_TAB, "Blackbox Access",
                  "Final chapter access routes", 0xFF7BDEFF),
               new TerminalAddonLink(BlackboxTerminalIds.ARCHIVE_TAB, "Memory Archive",
                  "Decoded blackbox records", 0xFF9FD1FF),
               new TerminalAddonLink(BlackboxTerminalIds.TRUTH_TAB, "Truth Engine",
                  "Final ending readiness", 0xFFC09BFF));
         }
      }
   }

   private static final class AccessTab implements ClientTerminalTab {
      private final TerminalTabDescriptor descriptor = new TerminalTabDescriptor(BlackboxTerminalIds.ACCESS_TAB, "BLACKBOX ACCESS", 420, 0xFF7BDEFF);
      private final TerminalTabChrome chrome = TerminalTabChrome.of("Blackbox", TerminalTabChrome.GROUP_ENDGAME, "BB", "Final chapter access routes", 420);
      private final TerminalMissionBrowser browser = new TerminalMissionBrowser(
         BlackboxMissionProvider.INSTANCE, this.descriptor.id(), true,
         TerminalMissionBrowser.ActionMode.TRACKING_ONLY,
         TerminalMissionBrowser.InitialTreeFocus.ALIGN_SELECTED_TOP);

      public TerminalTabDescriptor descriptor() {
         return this.descriptor;
      }

      public TerminalTabChrome chrome() {
         return this.chrome;
      }

      public void onSelected(TerminalRenderContext context) {
         this.browser.onSelected(context);
      }

      public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
         this.browser.render(context, graphics, mouseX, mouseY, partialTick);
      }

      public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
         return this.browser.mouseClicked(context, mouseX, mouseY, button);
      }

      public boolean mouseDragged(TerminalRenderContext context, double mouseX, double mouseY, int button,
            double dragX, double dragY) {
         return this.browser.mouseDragged(context, mouseX, mouseY, button, dragX, dragY);
      }

      public boolean mouseReleased(TerminalRenderContext context, double mouseX, double mouseY, int button) {
         return this.browser.mouseReleased(context, mouseX, mouseY, button);
      }

      public boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
         return this.browser.mouseScrolled(context, mouseX, mouseY, delta);
      }

      public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
         return this.browser.keyPressed(context, event);
      }

      public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
         return this.browser.charTyped(context, event);
      }

      public int contentHeight(TerminalRenderContext context) {
         return this.browser.contentHeight(context);
      }
   }

   private static final class MemoryArchiveTab implements ClientTerminalTab {
      private final TerminalTabDescriptor descriptor = new TerminalTabDescriptor(BlackboxTerminalIds.ARCHIVE_TAB, "MEMORY ARCHIVE", 421, 0xFF9FD1FF);
      private final TerminalTabChrome chrome = TerminalTabChrome.of("Memory", TerminalTabChrome.GROUP_FIELD, "MA", "Decoded blackbox records", 421);

      public TerminalTabDescriptor descriptor() {
         return this.descriptor;
      }

      public TerminalTabChrome chrome() {
         return this.chrome;
      }

      public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
         BlackboxProgress progress = BlackboxProgress.get(context.player());
         int x = context.contentX();
         int y = context.contentY();
         int w = context.contentWidth();
         int cy = TerminalUi.flatDataPanel(
            context,
            graphics,
            x,
            y,
            w,
            Math.max(156, context.contentHeight() / 2),
            "MEMORY ARCHIVE",
            progress.decodedMemoryTotal() + " decoded",
            this.descriptor.accentColor()
         );
         cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28, "Stability", progress.stability() + "%", progress.stability() <= 25 ? TerminalUi.RED : TerminalUi.GREEN);
         cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28, "False Signals", String.valueOf(progress.falseSignalCount()), progress.falseSignalCount() > 4 ? TerminalUi.AMBER : TerminalUi.MUTED);
         cy += 8;
         for (MemoryType type : MemoryType.values()) {
            int count = progress.memoryCount(type);
            cy = TerminalUi.checklistRow(context, graphics, x + 14, cy, w - 28, type.displayName(), count > 0, count + " record(s) reconstructed");
         }
      }

      public int contentHeight(TerminalRenderContext context) {
         return Math.max(context.contentHeight(), 260);
      }
   }

   private static final class TruthEngineTab implements ClientTerminalTab {
      private final TerminalTabDescriptor descriptor = new TerminalTabDescriptor(BlackboxTerminalIds.TRUTH_TAB, "TRUTH ENGINE", 422, 0xFFC09BFF);
      private final TerminalTabChrome chrome = TerminalTabChrome.of("Truth", TerminalTabChrome.GROUP_NEXUS, "TE", "Final ending readiness", 422);

      public TerminalTabDescriptor descriptor() {
         return this.descriptor;
      }

      public TerminalTabChrome chrome() {
         return this.chrome;
      }

      public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
         BlackboxProgress progress = BlackboxProgress.get(context.player());
         int x = context.contentX();
         int y = context.contentY();
         int w = context.contentWidth();
         String status = progress.ending() == BlackboxEnding.NONE ? "UNCOMMITTED" : progress.ending().displayName();
         int cy = TerminalUi.flatDataPanel(context, graphics, x, y, w, Math.max(180, context.contentHeight() / 2), "TRUTH ENGINE", status, this.descriptor.accentColor());
         cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28, "Core Chamber", progress.completed(BlackboxDungeon.CORE_CHAMBER) ? "sealed" : "pending", progress.completed(BlackboxDungeon.CORE_CHAMBER) ? TerminalUi.GREEN : TerminalUi.AMBER);
         cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28, "Guardian", progress.bossDefeated("nexus_guardian") ? "defeated" : "active", progress.bossDefeated("nexus_guardian") ? TerminalUi.GREEN : TerminalUi.AMBER);
         cy = TerminalUi.keyValue(context, graphics, x + 14, cy, w - 28, "Core Key", progress.hasNexusCoreAccessKey() ? "assembled" : "missing", progress.hasNexusCoreAccessKey() ? TerminalUi.GREEN : TerminalUi.AMBER);
         cy += 8;
         for (BlackboxEnding ending : new BlackboxEnding[]{BlackboxEnding.RESTORE, BlackboxEnding.CONTROL, BlackboxEnding.DESTROY, BlackboxEnding.MERGE}) {
            boolean committed = progress.ending() == ending;
            boolean ready = BlackboxEndings.eligible(context.player(), progress, ending);
            String detail = committed ? ending.finalLine() : (ready ? "Directive can be committed at the Truth Engine." : ending == BlackboxEnding.MERGE ? "Requires Deleted Logs and all boss proof." : "Requires Guardian defeat and Nexus Core Access Key.");
            cy = TerminalUi.checklistRow(context, graphics, x + 14, cy, w - 28, ending.displayName(), committed || ready, detail);
         }
      }

      public int contentHeight(TerminalRenderContext context) {
         return Math.max(context.contentHeight(), 260);
      }
   }
}
