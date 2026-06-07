package com.knoxhack.echorelictech.integration.terminal;

import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class RelicTechTerminalClientIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFF55FFDD;

    private RelicTechTerminalClientIntegration() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalTab tab = new RelicTechTab();
        TerminalTabRegistry.register(tab);
        TerminalNavigationProfiles.register(tab.descriptor().id(),
            TerminalNavigationProfile.chapter("relictech", "RelicTech", "RT", 50));
    }

    private static final class RelicTechTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor =
            new TerminalTabDescriptor(RelicTechTerminalIds.RELICTECH_TAB, "RELICTECH", 50, ACCENT);
        private final TerminalTabChrome chrome =
            TerminalTabChrome.of("RelicTech", TerminalTabChrome.GROUP_ADDONS, "RT", "Relic vault records", 50);

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
            int x = context.contentX() + 12;
            int y = context.contentY() + 10 - context.scrollY();
            int w = context.contentWidth() - 24;

            y = TerminalUi.sectionHeader(context, graphics, "RELICTECH", "ADDONS", x, y, w, ACCENT);
            y += 8;

            TerminalUi.flatHudPanel(context, graphics, x, y, w, 110, ACCENT);
            TerminalUi.line(context, graphics, "Relic Analyzer        — Identifies unknown relics", x + 14, y + 16, w - 28, TerminalUi.text(context));
            TerminalUi.line(context, graphics, "Prototype Workbench   — Stabilize / Overclock / Contain / Purge", x + 14, y + 34, w - 28, TerminalUi.text(context));
            TerminalUi.line(context, graphics, "Containment Locker    — Stores dangerous relics safely", x + 14, y + 52, w - 28, TerminalUi.text(context));
            TerminalUi.line(context, graphics, "Null Battery Dock     — Charges Null Batteries", x + 14, y + 70, w - 28, TerminalUi.text(context));
            y += 118;

            TerminalUi.section(context, graphics, "MVP RELICS", x, y, ACCENT);
            y += 20;
            TerminalUi.line(context, graphics, "Phase Anchor     — Teleportation bind/recall", x + 8, y, w - 16, TerminalUi.text(context));
            y += 18;
            TerminalUi.line(context, graphics, "Guardian Lens    — Scan for relic traces", x + 8, y, w - 16, TerminalUi.text(context));
            y += 18;
            TerminalUi.line(context, graphics, "Echo Mirror      — Defensive echo projection", x + 8, y, w - 16, TerminalUi.text(context));
            y += 18;
            TerminalUi.line(context, graphics, "Matter Stitcher  — Heal and armor repair", x + 8, y, w - 16, TerminalUi.text(context));
            y += 18;
            TerminalUi.line(context, graphics, "Null Battery     — Stores Null Charge", x + 8, y, w - 16, TerminalUi.text(context));
            y += 30;

            boolean referenceOnly = TerminalNavigationProfiles.referenceOnlyChapterContext(context);
            TerminalUi.compactButton(context, graphics, x, y, 100, "SCAN RELICS", ACCENT, !referenceOnly,
                !referenceOnly && mouseX >= x && mouseX <= x + 100 && mouseY >= y && mouseY <= y + 16);
            if (referenceOnly) {
                TerminalUi.line(context, graphics, "Reference view. Use Survival Route for progression commands.",
                    x + 108, y + 5, Math.max(80, w - 116), TerminalUi.muted(context));
            }
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            int x = context.contentX() + 12;
            int y = context.contentY() + 10 - context.scrollY();
            int w = context.contentWidth() - 24;
            y = TerminalUi.sectionHeader(context, null, "", "", x, y, w, ACCENT);
            y += 8 + 118 + 20 + 18 * 5 + 30;
            if (mouseX >= x && mouseX <= x + 100 && mouseY >= y && mouseY <= y + 16) {
                if (TerminalNavigationProfiles.referenceOnlyChapterContext(context)) {
                    context.playRejectedSound();
                    return true;
                }
                context.sendAction(RelicTechTerminalIds.RELICTECH_TAB, RelicTechTerminalIds.SCAN_RELICS, "");
                return true;
            }
            return false;
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return 360;
        }
    }
}
