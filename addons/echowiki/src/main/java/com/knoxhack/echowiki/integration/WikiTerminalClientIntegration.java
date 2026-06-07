package com.knoxhack.echowiki.integration;

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
import com.knoxhack.echowiki.EchoWiki;
import com.knoxhack.echowiki.client.WikiScreenCorePages;
import com.knoxhack.echowiki.content.GuideBookRegistry;
import com.knoxhack.echowiki.content.WikiContentRegistry;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class WikiTerminalClientIntegration {
    public static final Identifier TAB_ID = EchoWiki.id("terminal/wiki");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFF66E8FF;

    private WikiTerminalClientIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalTab tab = new WikiTab();
        TerminalTabRegistry.register(tab);
        TerminalNavigationProfiles.register(tab.descriptor().id(), TerminalNavigationProfile.intel(148));
        EchoWiki.LOGGER.info("ECHO: Wiki registered Terminal Survival Codex tab.");
    }

    private static final class WikiTab implements ClientTerminalTab, TerminalScreenCorePageMetadata {
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(TAB_ID, "WIKI", 148, ACCENT);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Survival Codex", TerminalTabChrome.GROUP_FIELD, "WK",
                        "Guide articles, discoveries, and field reference", 148);

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
            return WikiScreenCorePages.DASHBOARD;
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x = context.contentX() + 12;
            int y = context.contentY() + 10;
            int w = context.contentWidth() - 24;
            y = TerminalUi.sectionHeader(context, graphics, "ECHO: WIKI", "SURVIVAL CODEX", x, y, w, ACCENT);
            TerminalUi.flatHudPanel(context, graphics, x, y, w, 86, ACCENT);
            TerminalUi.line(context, graphics, "ScreenCore Codex page: " + WikiScreenCorePages.DASHBOARD,
                    x + 12, y + 14, w - 24, TerminalUi.text(context));
            TerminalUi.line(context, graphics, WikiContentRegistry.articles().size()
                            + " article(s), " + WikiContentRegistry.collections().size() + " collection(s), "
                            + GuideBookRegistry.visibleGuideBooks().size() + " guide book(s), "
                            + WikiContentRegistry.warnings().size() + " warning(s).",
                    x + 12, y + 30, w - 24, TerminalUi.muted(context));
            TerminalUi.line(context, graphics,
                    "Guide-book library page: " + WikiScreenCorePages.GUIDE_BOOKS,
                    x + 12, y + 46, w - 24, TerminalUi.muted(context));
        }
    }
}
