package com.knoxhack.echoorbitalremnants.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoorbitalremnants.network.EchoTerminalActionPayload;
import com.knoxhack.echoorbitalremnants.network.EchoTerminalSnapshot;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class EchoTerminalScreen extends Screen {
    private static final String[] TABS = {"COMMAND", "LAUNCH", "ROUTES", "SURVEY", "ECHO"};
    private EchoTerminalSnapshot snapshot;
    private int activeTab;
    private int actionFeedbackTicks;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public EchoTerminalScreen(EchoTerminalSnapshot snapshot) {
        super(Component.literal("ECHO-7 Terminal"));
        this.snapshot = snapshot;
        this.activeTab = switch (snapshot.activeTab()) {
            case "LAUNCH" -> 1;
            case "ORBITAL", "STATION", "LUNAR" -> 2;
            case "DEEP SPACE" -> 4;
            default -> 0;
        };
    }

    public void updateSnapshot(EchoTerminalSnapshot snapshot) {
        this.snapshot = snapshot;
        this.actionFeedbackTicks = 12;
    }

    @Override
    public void tick() {
        if (actionFeedbackTicks > 0) {
            actionFeedbackTicks--;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        layout();
        graphics.fill(0, 0, width, height, 0xDD02070A);
        EchoCyberGlassUi.panel(graphics, panelX, panelY, panelWidth, panelHeight, 0xF20A1218, 0xFF62DFFF);
        graphics.fill(panelX + 12, panelY + 26, panelX + panelWidth - 12, panelY + 27, 0x6632BFD7);

        graphics.text(font, Component.literal("ECHO-7 ORBITAL TERMINAL"), panelX + 14, panelY + 10, 0x66E8FF, true);
        graphics.text(font, Component.literal(snapshot.location()), panelX + panelWidth - 14 - font.width(snapshot.location()), panelY + 10, 0xB7EFFF, false);

        drawTabs(graphics);
        drawBody(graphics);
        drawActions(graphics, mouseX, mouseY);
        drawFooter(graphics);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        layout();
        int x = panelX + 14;
        int y = panelY + 32;
        int tabWidth = tabWidth();
        int step = tabStep();
        for (int i = 0; i < TABS.length; i++) {
            if (inside(event.x(), event.y(), x + i * step, y, tabWidth, 18)) {
                activeTab = i;
                return true;
            }
        }
        if (inside(event.x(), event.y(), refreshX(), actionY(), 62, 18)) {
            sendAction(EchoTerminalActionPayload.Action.REFRESH);
            return true;
        }
        if (inside(event.x(), event.y(), scanX(), actionY(), 62, 18)) {
            sendAction(EchoTerminalActionPayload.Action.SCAN);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void layout() {
        panelWidth = Math.min(430, Math.max(320, width - 44));
        panelHeight = Math.min(276, Math.max(232, height - 36));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
    }

    private void drawTabs(GuiGraphicsExtractor graphics) {
        int x = panelX + 14;
        int y = panelY + 32;
        int tabWidth = tabWidth();
        int step = tabStep();
        for (int i = 0; i < TABS.length; i++) {
            int tx = x + i * step;
            int color = i == activeTab ? 0xFF123241 : 0xFF101820;
            int line = i == activeTab ? 0xFF66E8FF : 0xFF2B4650;
            graphics.fill(tx, y, tx + tabWidth, y + 18, color);
            EchoCyberGlassUi.frame(graphics, tx, y, tabWidth, 18, line);
            graphics.centeredText(font, TABS[i], tx + tabWidth / 2, y + 5, i == activeTab ? 0xE9FBFF : 0x96B4BE);
        }
    }

    private void drawBody(GuiGraphicsExtractor graphics) {
        int x = panelX + 14;
        int y = panelY + 60;
        int w = panelWidth - 28;
        int h = panelHeight - 92;
        EchoCyberGlassUi.panel(graphics, x, y, w, h, 0xAA071014, 0x6632BFD7);

        switch (activeTab) {
            case 1 -> drawLaunch(graphics, x + 10, y + 10, w - 20);
            case 2 -> drawRoutes(graphics, x + 10, y + 10, w - 20);
            case 3 -> drawSurvey(graphics, x + 10, y + 10, w - 20);
            case 4 -> drawEcho(graphics, x + 10, y + 10, w - 20);
            default -> drawStatus(graphics, x + 10, y + 10, w - 20);
        }
    }

    private void drawStatus(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.text(font, Component.literal("ORBITAL COMMAND | " + snapshot.missionStep()), x, y, 0x66E8FF, true);
        graphics.text(font, Component.literal("NEXT STEP"), x, y + 12, 0xFFD166, true);
        graphics.textWithWordWrap(font, Component.literal(snapshot.nextObjective()), x + 56, y + 12, width - 56, 0xE2F7FF);
        graphics.text(font, Component.literal("SCAN"), x, y + 42, 0x66E8FF, true);
        graphics.textWithWordWrap(font, Component.literal(snapshot.scanRequirement()), x + 34, y + 42, width - 34, 0xCBEFFF);
        graphics.text(font, Component.literal("LAST"), x, y + 66, 0xA8F7C5, true);
        graphics.textWithWordWrap(font, Component.literal(snapshot.scanReport()), x + 34, y + 66, width - 34, 0xD8F6FF);
        graphics.text(font, Component.literal("SURVEY " + snapshot.surveyStatus()), x, y + 90, 0xFFD166, false);
        if ("GROUND".equals(snapshot.activeTab()) && !snapshot.groundSiteLines().isEmpty()) {
            int row = y + 104;
            for (String line : snapshot.groundSiteLines().stream().limit(5).toList()) {
                graphics.textWithWordWrap(font, Component.literal(line), x, row, width, line.startsWith("OK") ? 0xA8F7C5 : 0xD8F6FF);
                row += 15;
            }
            return;
        }
        int row = y + 98;
        drawMeter(graphics, x, row, "O2", snapshot.oxygen(), 0xFF57D68D);
        drawMeter(graphics, x, row + 20, "PRESSURE", snapshot.pressure(), snapshot.sealSecure() ? 0xFF66E8FF : 0xFFFF6B6B);
        drawMeter(graphics, x + 176, row, "RAD", snapshot.radiation(), 0xFFE09CFF);
        String seal = snapshot.sealSecure() ? "SEAL OK" : snapshot.suitLeak() ? "LEAK" : "SEAL BAD";
        graphics.text(font, Component.literal("Gravity " + snapshot.gravity() + " | " + seal), x + 176, row + 19, 0xCBEFFF, false);
        graphics.text(font, Component.literal("Station " + snapshot.stationPower() + "% | " + (snapshot.orbitalExposure() ? "ORBITAL" : "GROUND")), x + 176, row + 33, 0xCBEFFF, false);
    }

    private void drawLaunch(GuiGraphicsExtractor graphics, int x, int y, int width) {
        drawStateLine(graphics, x, y, "Launch readiness", snapshot.launchReady());
        drawStateLine(graphics, x, y + 15, "Rocket assembly", snapshot.assemblyReady());
        drawStateLine(graphics, x, y + 30, "Earth return vector", snapshot.earthReturnSaved());
        drawRocketStatus(graphics, x, y + 48, width);
        int listY = y + 82;
        if (!snapshot.launchReady()) {
            drawList(graphics, x, listY, "Missing launch systems", snapshot.launchMissing(), 4);
        } else if (!snapshot.assemblyReady()) {
            drawList(graphics, x, listY, "Missing assembly parts", snapshot.assemblyMissing(), 4);
        } else {
            graphics.textWithWordWrap(font, Component.literal(snapshot.rocketLaunchDetail()), x, listY, width, 0xD8F6FF);
        }
    }

    private void drawRoutes(GuiGraphicsExtractor graphics, int x, int y, int width) {
        drawRoute(graphics, x, y, "Low Earth Orbit", snapshot.lowOrbitReached(), "Emergency Rocket");
        drawRoute(graphics, x, y + 23, "Lunar Scar Zone", snapshot.lunarOpen(), "Orbital Shuttle");
        drawRoute(graphics, x, y + 46, "Mars Ash Basin", snapshot.marsOpen(), "Mars Transfer Window");
        drawRoute(graphics, x, y + 69, "Europa Cryo Ocean", snapshot.europaOpen(), "Europa Transfer Window");
        drawRoute(graphics, x, y + 92, "Saturn Ring Graveyard", snapshot.saturnOpen(), "Saturn Transfer Window");
        drawRoute(graphics, x, y + 115, "Titan Methane Shelf", snapshot.titanOpen(), "Titan Transfer Window");
        drawRoute(graphics, x, y + 138, "Nexus Anomaly Belt", snapshot.nexusOpen(), "Nexus Drive Vessel");
        graphics.text(font, Component.literal("Docking vector: " + (snapshot.routeReturnSaved() ? "SAVED" : "NONE")), x, y + 161, 0xCBEFFF, false);
        graphics.textWithWordWrap(font, Component.literal(snapshot.localHazard()), x + 170, y + 154, width - 170, 0xA8C6CE);
    }

    private void drawEcho(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.text(font, Component.literal("ECHO MEMORY: " + snapshot.echoMemory()), x, y, 0xE9FBFF, true);
        drawStateLine(graphics, x, y + 18, "Station coordinates", snapshot.stationCoordinates());
        drawStateLine(graphics, x, y + 33, "Lunar signal investigated", snapshot.lunarInvestigated());
        drawStateLine(graphics, x, y + 48, "Mars visited", snapshot.marsVisited());
        drawStateLine(graphics, x, y + 63, "Europa visited", snapshot.europaVisited());
        drawStateLine(graphics, x, y + 78, "Saturn visited", snapshot.saturnVisited());
        drawStateLine(graphics, x, y + 93, "Titan visited", snapshot.titanVisited());
        drawStateLine(graphics, x, y + 108, "Anomaly entered", snapshot.anomalyEntered());
        drawStateLine(graphics, x, y + 123, "ECHO-0 contact", snapshot.echoZero());
        int rightX = x + width / 2 + 12;
        int rightWidth = width - (rightX - x);
        graphics.text(font, Component.literal("FACTION RELAY"), rightX, y + 18, 0x66E8FF, true);
        graphics.text(font, Component.literal("Radwarden " + snapshot.orbitalRemnantStanding()), rightX, y + 34, 0xD8F6FF, false);
        graphics.text(font, Component.literal("Crashbreak " + snapshot.voidSalvagerStanding()), rightX, y + 48, 0xD8F6FF, false);
        graphics.text(font, Component.literal("Sporebound " + snapshot.nexusChoirStanding()), rightX, y + 62, 0xD8F6FF, false);
        graphics.text(font, Component.literal("CONTRACT"), rightX, y + 80, 0xFFD166, true);
        graphics.textWithWordWrap(font, Component.literal(snapshot.factionContract()), rightX, y + 94, rightWidth, 0xD8F6FF);
        graphics.text(font, Component.literal("ECHO NOTE"), x, y + 128, 0xFFD166, true);
        graphics.textWithWordWrap(font, Component.literal(snapshot.missionHelp()), x + 62, y + 128, width - 62, 0xCBEFFF);
    }

    private void drawSurvey(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.text(font, Component.literal("ORBITAL SURVEY NETWORK"), x, y, 0x66E8FF, true);
        graphics.textWithWordWrap(font, Component.literal(snapshot.localHazard()), x, y + 13, width, 0xCBEFFF);
        int row = y + 34;
        for (String line : snapshot.surveyLines().stream().limit(7).toList()) {
            graphics.textWithWordWrap(font, Component.literal(line), x, row, width, line.contains("3/3") ? 0xA8F7C5 : 0xD8F6FF);
            row += 17;
        }
        graphics.text(font, Component.literal("LAST"), x, row + 2, 0xFFD166, true);
        graphics.textWithWordWrap(font, Component.literal(snapshot.scanReport()), x + 34, row + 2, width - 34, 0xD8F6FF);
    }

    private void drawFooter(GuiGraphicsExtractor graphics) {
        String footer = "Active tab: " + snapshot.activeTab();
        graphics.text(font, Component.literal(footer), panelX + 14, panelY + panelHeight - 20, 0x8CB8C2, false);
        graphics.text(font, Component.literal("ESC"), panelX + panelWidth - 36, panelY + panelHeight - 20, 0x8CB8C2, false);
        if (actionFeedbackTicks > 0) {
            graphics.text(font, Component.literal("SYNCED"), panelX + panelWidth - 112, panelY + panelHeight - 20, 0xA8F7C5, false);
        }
    }

    private void drawActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawButton(graphics, refreshX(), actionY(), 62, "REFRESH",
                inside(mouseX, mouseY, refreshX(), actionY(), 62, 18));
        drawButton(graphics, scanX(), actionY(), 62, "SCAN",
                inside(mouseX, mouseY, scanX(), actionY(), 62, 18));
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int width, String label, boolean hovered) {
        EchoCyberGlassUi.button(graphics, font, x, y, width, 18, label, hovered, true, 0xFF66E8FF);
    }

    private void drawMeter(GuiGraphicsExtractor graphics, int x, int y, String label, int value, int color) {
        int clamped = Math.max(0, Math.min(100, value));
        graphics.text(font, Component.literal(label + " " + clamped + "%"), x, y, 0xE9FBFF, false);
        EchoCyberGlassUi.meter(graphics, x + 70, y + 2, 88, 7, clamped * 88 / 100, color);
    }

    private void drawStateLine(GuiGraphicsExtractor graphics, int x, int y, String label, boolean ready) {
        graphics.text(font, Component.literal((ready ? "ONLINE  " : "LOCKED  ") + label), x, y, ready ? 0xA8F7C5 : 0xFFD166, false);
    }

    private void drawRocketStatus(GuiGraphicsExtractor graphics, int x, int y, int width) {
        int statusColor = snapshot.rocketLaunching() ? 0xAA7B2E12
                : snapshot.rocketCountingDown() ? 0xAA4E3D11
                : snapshot.rocketStaged() ? 0xAA123241
                : snapshot.launchReady() && snapshot.assemblyReady() ? 0xAA23573E : 0xAA392A13;
        graphics.fill(x, y, x + Math.min(width, 184), y + 18, statusColor);
        graphics.fill(x, y + 16, x + Math.min(width, 184), y + 18,
                snapshot.rocketLaunching() ? 0xFFFF8A3D : snapshot.rocketCountingDown() ? 0xFFFFD166 : 0xFF66E8FF);
        graphics.text(font, Component.literal(snapshot.rocketLaunchStatus()), x + 6, y + 5,
                snapshot.rocketLaunching() ? 0xFFFFD8A8 : 0xE9FBFF, true);
        if (snapshot.rocketCountdownSeconds() > 0) {
            graphics.text(font, Component.literal(snapshot.rocketCountdownSeconds() + "s"), x + 154, y + 5, 0xFFFFFF, true);
        }
    }

    private void drawRoute(GuiGraphicsExtractor graphics, int x, int y, String name, boolean open, String vessel) {
        graphics.fill(x, y, x + 155, y + 17, open ? 0x5523573E : 0x55392A13);
        graphics.text(font, Component.literal(open ? "OPEN" : "LOCKED"), x + 6, y + 5, open ? 0xA8F7C5 : 0xFFD166, false);
        graphics.text(font, Component.literal(name), x + 54, y + 5, 0xE2F7FF, false);
        graphics.text(font, Component.literal(vessel), x + 188, y + 5, 0xA8C6CE, false);
    }

    private void drawList(GuiGraphicsExtractor graphics, int x, int y, String title, List<String> values, int limit) {
        graphics.text(font, Component.literal(title), x, y, 0x66E8FF, true);
        int row = y + 14;
        for (String value : values.stream().limit(limit).toList()) {
            graphics.text(font, Component.literal("- " + value), x + 4, row, 0xFFD166, false);
            row += 11;
        }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private void sendAction(EchoTerminalActionPayload.Action action) {
        EchoNetClientActions.sendServerboundAction(new EchoTerminalActionPayload(action));
    }

    private int actionY() {
        return panelY + panelHeight - 45;
    }

    private int refreshX() {
        return panelX + panelWidth - 148;
    }

    private int scanX() {
        return panelX + panelWidth - 80;
    }

    private int tabWidth() {
        return Math.max(52, Math.min(70, (panelWidth - 34) / TABS.length));
    }

    private int tabStep() {
        return tabWidth() + 4;
    }
}
