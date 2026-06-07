package com.knoxhack.echo.creatorcore.client;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import com.knoxhack.echo.creatorcore.EchoCreatorCoreClient;
import com.knoxhack.echo.creatorcore.api.CreatorCoreApi;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardModel;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalScreenCorePageMetadata;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class CreatorCoreTerminalClientIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Identifier TAB_ID = EchoCreatorCore.id("creatorcore");
    private static final int ACCENT = 0xFF38DFF4;

    private CreatorCoreTerminalClientIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalTabRegistry.register(new CreatorCoreTerminalTab());
        TerminalAddonInfoRegistry.register(new CreatorCoreAddonInfoProvider());
        EchoCreatorCore.LOGGER.info("CreatorCore Terminal entry registered.");
    }

    private static final class CreatorCoreTerminalTab implements ClientTerminalTab, TerminalScreenCorePageMetadata {
        @Override
        public TerminalTabDescriptor descriptor() {
            return new TerminalTabDescriptor(TAB_ID, "CREATORCORE", 175, ACCENT);
        }

        @Override
        public TerminalTabChrome chrome() {
            return TerminalTabChrome.of("CreatorCore", TerminalTabChrome.GROUP_ADDONS, "CC",
                    "Creator dashboard and validation tools", 175);
        }

        @Override
        public Identifier screenCorePageId() {
            return EchoCreatorCore.id("creator_dashboard");
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            CreatorDashboardModel model = new CreatorDashboardModel();
            int x = context.contentX();
            int y = context.contentY() + 8;
            int w = context.contentWidth();
            TerminalUi.densePanel(context, graphics, x, y, w, 118, ACCENT);
            TerminalUi.line(context, graphics, "ECHO: CreatorCore", x + 10, y + 10, w - 20, TerminalUi.text(context));
            TerminalUi.line(context, graphics, "Creator dashboard, validation, drafts, and ScriptCore authoring bridge.",
                    x + 10, y + 24, w - 20, TerminalUi.muted(context));
            TerminalUi.line(context, graphics, "Adapters: " + model.doctorReport().adaptersAvailable()
                    + "/" + model.doctorReport().adaptersTotal()
                    + "   Definitions: " + model.definitions().size()
                    + "   Diagnostics: " + model.diagnostics().size(),
                    x + 10, y + 42, w - 20, TerminalUi.accent(context));
            TerminalUi.line(context, graphics, "Writes: " + (model.writeLocked() ? "locked" : "allowed")
                    + "   Exports: " + (model.exportsLocked() ? "locked" : "allowed")
                    + "   Drafts: " + model.drafts().size(),
                    x + 10, y + 56, w - 20, TerminalUi.warning(context));
            boolean hover = TerminalUi.inside(mouseX, mouseY, x + 10, y + 78, Math.min(170, w - 20), 22);
            TerminalUi.button(context, graphics, x + 10, y + 78, Math.min(170, w - 20),
                    "Open Dashboard", ACCENT, true, hover);
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            int x = context.contentX() + 10;
            int y = context.contentY() + 86;
            int w = Math.min(170, context.contentWidth() - 20);
            if (TerminalUi.inside(mouseX, mouseY, x, y, w, 22)) {
                EchoCreatorCoreClient.openDashboard();
                return true;
            }
            return false;
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return Math.max(context.contentHeight(), 150);
        }
    }

    private static final class CreatorCoreAddonInfoProvider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return EchoCreatorCore.CHAPTER_ID;
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            CreatorDashboardModel model = new CreatorDashboardModel();
            List<TerminalAddonMetric> metrics = List.of(
                    new TerminalAddonMetric("Adapters",
                            model.doctorReport().adaptersAvailable() + "/" + model.doctorReport().adaptersTotal(),
                            "available creator bridges", ACCENT),
                    new TerminalAddonMetric("Definitions", String.valueOf(model.definitions().size()),
                            "browser summaries", TerminalUi.CYAN),
                    new TerminalAddonMetric("Diagnostics", String.valueOf(model.diagnostics().size()),
                            "validation center items", model.doctorReport().errors() > 0 ? TerminalUi.RED : TerminalUi.GREEN),
                    new TerminalAddonMetric("Drafts", String.valueOf(model.drafts().size()),
                            model.writeLocked() ? "writes locked" : "writes allowed", TerminalUi.AMBER));
            List<TerminalAddonSection> sections = List.of(
                    new TerminalAddonSection("Safety", List.of(
                            "Writes: " + (model.writeLocked() ? "locked by config" : "allowed by config"),
                            "Exports: " + (model.exportsLocked() ? "locked by config" : "allowed by config"))),
                    new TerminalAddonSection("Authoring", List.of(
                            "ScriptCore definitions and diagnostics are bridged when ScriptCore is present.",
                            "Mission Studio edits draft JSON only in 0.2.0.")));
            List<TerminalAddonLink> links = List.of(new TerminalAddonLink(TAB_ID,
                    "Open CreatorCore", "Dashboard, validation, drafts, and previews", ACCENT));
            return new TerminalAddonInfo("In-game creator/admin authoring suite for ECHO-powered packs.",
                    metrics, sections, links);
        }
    }
}
