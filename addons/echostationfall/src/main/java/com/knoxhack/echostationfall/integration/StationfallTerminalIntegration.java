package com.knoxhack.echostationfall.integration;

import com.knoxhack.echostationfall.progression.StationSection;
import com.knoxhack.echostationfall.progression.StationfallObjective;
import com.knoxhack.echostationfall.progression.StationfallProgress;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
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

public final class StationfallTerminalIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFFFF536A;

    private StationfallTerminalIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalTab tab = new StationfallTab();
        TerminalTabRegistry.register(tab);
        TerminalNavigationProfiles.register(tab.descriptor().id(),
                TerminalNavigationProfile.chapter("stationfall", "Chapter 3: Stationfall", "C3", 330));
        TerminalAddonInfoRegistry.register(new StationfallAddonInfoProvider());
    }

    private static final class StationfallAddonInfoProvider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return StationfallTerminalCommonIntegration.CHAPTER_ID.getPath();
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            if (player == null) {
                return new TerminalAddonInfo(
                        "Station route, live telemetry, crew logs, boss state, and blackbox handoff.",
                        List.of(new TerminalAddonMetric("Signal", "OFFLINE", "waiting for player telemetry", ACCENT)),
                        List.of(new TerminalAddonSection("Station Feed",
                                List.of("Open Stationfall after player telemetry is available."))),
                        links(),
                        guide());
            }
            StationfallProgress progress = StationfallProgress.get(player);
            return new TerminalAddonInfo(
                    "Station route, live telemetry, crew logs, boss state, and blackbox handoff.",
                    List.of(
                            new TerminalAddonMetric("Power", progress.poweredSectionCount() + "/" + StationSection.values().length,
                                    "stable sections", TerminalUi.GREEN),
                            new TerminalAddonMetric("Logs", progress.decodedLogCount() + "/" + StationSection.values().length,
                                    "decoded crew logs", TerminalUi.CYAN),
                            new TerminalAddonMetric("Objectives",
                                    progress.objectiveCount() + "/" + StationfallObjective.values().length,
                                    "route objectives", progress.allObjectivesComplete() ? TerminalUi.GREEN : TerminalUi.AMBER),
                            new TerminalAddonMetric("Blackbox", progress.blackboxRetrieved() ? "RECOVERED" : "PENDING",
                                    progress.bossDefeated() ? "station mother defeated" : "station boss active",
                                    progress.blackboxRetrieved() ? TerminalUi.GREEN : TerminalUi.AMBER)),
                    List.of(new TerminalAddonSection("Station Feed", List.of(
                            progress.boarded() ? "Boarding record confirmed." : "Boarding record pending.",
                            progress.coordinatesUnlocked() ? "Station coordinates unlocked." : "Coordinates still locked.",
                            progress.hasReturnPoint() ? "Return point stored in " + progress.returnDimension() + "." : "No return point stored."))),
                    links(),
                    guide());
        }

        private static TerminalAddonGuide guide() {
            return TerminalAddonGuide.mainline(3, 30, "After Orbital",
                    "Start Stationfall once Orbital route progress exposes the dead station and you can support suit hazards.",
                    List.of(
                            "Board only after station coordinates or network restoration are ready.",
                            "Restore station sections before pushing deep objectives.",
                            "Recover crew logs and the Stationfall Blackbox handoff."));
        }

        private static List<TerminalAddonLink> links() {
            return List.of(new TerminalAddonLink(StationfallTerminalCommonIntegration.CHAPTER_ID,
                    "Stationfall", "Station route records", ACCENT));
        }
    }

    private static final class StationfallTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor =
                new TerminalTabDescriptor(StationfallTerminalCommonIntegration.CHAPTER_ID, "STATIONFALL", 330, ACCENT);
        private final TerminalTabChrome chrome =
                TerminalTabChrome.of("Stationfall", TerminalTabChrome.GROUP_ORBITAL, "SF",
                        "Station route records", 330);
        private final TerminalMissionBrowser browser =
                new TerminalMissionBrowser(StationfallTerminalCommonIntegration.Provider.INSTANCE,
                        descriptor.id(), true,
                        TerminalMissionBrowser.ActionMode.TRACKING_ONLY,
                        TerminalMissionBrowser.InitialTreeFocus.ALIGN_SELECTED_TOP);

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
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                int mouseX, int mouseY, float partialTick) {
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
