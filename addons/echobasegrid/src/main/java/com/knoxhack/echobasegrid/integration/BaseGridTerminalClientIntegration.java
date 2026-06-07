package com.knoxhack.echobasegrid.integration;

import com.knoxhack.echobasegrid.EchoBaseGrid;
import com.knoxhack.echobasegrid.client.BaseGridClientState;
import com.knoxhack.echobasegrid.client.BaseGridDataProviders;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalScreenCorePageMetadata;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class BaseGridTerminalClientIntegration {
    public static final Identifier TAB_ID = EchoBaseGrid.id("terminal/base_grid");
    public static final Identifier PAGE_ID = EchoBaseGrid.id("base_grid");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFF66E8FF;

    private BaseGridTerminalClientIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        BaseGridDataProviders.register();
        TerminalTab tab = new BaseGridTab();
        TerminalTabRegistry.register(tab);
        TerminalNavigationProfiles.register(tab.descriptor().id(), TerminalNavigationProfile.system(165));
        EchoBaseGrid.LOGGER.info("ECHO: Base Grid registered Terminal ScreenCore tab.");
    }

    private static final class BaseGridTab implements ClientTerminalTab, TerminalScreenCorePageMetadata {
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(TAB_ID, "BASE GRID", 165, ACCENT);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Base Grid", TerminalTabChrome.GROUP_SYSTEMS, "BG",
                        "Chunk claim grid and base protection controls", 165);

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public Identifier screenCorePageId() {
            return PAGE_ID;
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                float partialTick) {
            int x = context.contentX() + 12;
            int y = context.contentY() + 10;
            int w = context.contentWidth() - 24;
            y = TerminalUi.sectionHeader(context, graphics, "ECHO: BASE GRID", "SCREENCORE CLAIM CONTROL",
                    x, y, w, ACCENT);
            TerminalUi.flatHudPanel(context, graphics, x, y, w, 104, ACCENT);
            TerminalUi.line(context, graphics, "ScreenCore page: " + PAGE_ID,
                    x + 12, y + 14, w - 24, TerminalUi.text(context));
            TerminalUi.line(context, graphics, "Claims: "
                            + BaseGridClientState.snapshot().claimCount() + " / "
                            + BaseGridClientState.snapshot().maxClaims(),
                    x + 12, y + 30, w - 24, TerminalUi.muted(context));
            TerminalUi.line(context, graphics, "Selected chunk: "
                            + BaseGridClientState.snapshot().selectedChunkX() + ", "
                            + BaseGridClientState.snapshot().selectedChunkZ() + " / "
                            + BaseGridClientState.snapshot().selectedState(),
                    x + 12, y + 46, w - 24, TerminalUi.muted(context));
            TerminalUi.line(context, graphics,
                    "Open through ScreenCore Terminal rendering for the interactive grid.",
                    x + 12, y + 62, w - 24, TerminalUi.muted(context));
        }
    }
}
