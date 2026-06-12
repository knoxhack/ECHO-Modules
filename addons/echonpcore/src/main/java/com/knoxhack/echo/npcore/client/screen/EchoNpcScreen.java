package com.knoxhack.echo.npcore.client.screen;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echo.npcore.network.CloseNpcInteractionPacket;
import com.knoxhack.echo.npcore.network.EchoNpcScreenState;
import com.knoxhack.echo.npcore.network.RequestNpcScreenRefreshPacket;
import com.knoxhack.echo.npcore.network.RequestNpcServicePacket;
import com.knoxhack.echo.npcore.network.RequestNpcTradePacket;
import com.knoxhack.echo.npcore.network.SelectDialogueOptionPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class EchoNpcScreen extends Screen {
    private static final int BG = 0xF0050A0F;
    private static final int PANEL = 0xDD0E171D;
    private static final int PANEL_SOFT = 0xAA14212A;
    private static final int BORDER = 0x884DBAF4;
    private static final int CYAN = 0xFF4DBAF4;
    private static final int GREEN = 0xFF55D88A;
    private static final int YELLOW = 0xFFFFD166;
    private static final int TEXT = 0xFFE7EEF6;
    private static final int MUTED = 0xFF91A3B8;
    private static final int DISABLED = 0xFF60717C;
    private static final int RED = 0xFFFF6B6B;

    private EchoNpcScreenState state;
    private final List<Hitbox> hitboxes = new ArrayList<>();
    private String localStatus = "";

    public EchoNpcScreen(EchoNpcScreenState state) {
        super(Component.literal("ECHO NPC"));
        this.state = state;
    }

    public static void open(EchoNpcScreenState state) {
        Minecraft.getInstance().setScreen(new EchoNpcScreen(state));
    }

    public static void updateOrOpen(EchoNpcScreenState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if ("close".equals(state.currentTab())) {
            minecraft.setScreen(null);
            return;
        }
        if (minecraft.screen instanceof EchoNpcScreen screen && screen.state.entityId() == state.entityId()) {
            screen.state = state;
            screen.localStatus = "";
        } else {
            minecraft.setScreen(new EchoNpcScreen(state));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hitboxes.clear();
        EchoCyberGlassUi.screenBackdrop(g, width, height, EchoCyberGlassUi.Surface.ECHO_APP);
        Layout layout = layout();
        EchoCyberGlassUi.panel(g, layout.x(), layout.y(), layout.w(), layout.h(), BG, BORDER);
        EchoCyberGlassUi.frame(g, layout.x() + 4, layout.y() + 4, layout.w() - 8, layout.h() - 8, 0x334DBAF4);
        drawHeader(g, layout);
        drawContactDossier(g, layout.leftX(), layout.contentY(), layout.leftW(), layout.contentH());
        drawMainPanel(g, layout.centerX(), layout.contentY(), layout.centerW(), layout.contentH(), mouseX, mouseY);
        drawChannelRail(g, layout.railX(), layout.contentY(), layout.railW(), layout.contentH(), mouseX, mouseY);
        drawFooter(g, layout.x() + 10, layout.footerY(), layout.w() - 20, layout.footerH(), mouseX, mouseY);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x();
            int my = (int) event.y();
            for (Hitbox hitbox : hitboxes) {
                if (hitbox.contains(mx, my)) {
                    if (!hitbox.enabled()) {
                        localStatus = hitbox.disabledReason().isBlank()
                                ? "That command is currently unavailable."
                                : hitbox.disabledReason();
                        return true;
                    }
                    localStatus = hitbox.status();
                    hitbox.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        EchoNetClientActions.sendServerboundAction(new CloseNpcInteractionPacket(state.entityId()));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return Minecraft.getInstance().isSingleplayer();
    }

    private void drawHeader(GuiGraphicsExtractor g, Layout layout) {
        int x = layout.x() + 10;
        int y = layout.y() + 10;
        int w = layout.w() - 20;
        EchoCyberGlassUi.panel(g, x, y, w, 40, 0xBB0B141C, 0x554DBAF4);
        statusPill(g, x + 8, y + 11, 48, "LIVE", GREEN);
        g.text(font, fit(state.displayName(), Math.max(80, w / 2 - 74)), x + 66, y + 9, CYAN, false);
        g.text(font, fit(state.role() + " / " + shortId(state.faction()) + " / " + state.relationship(),
                Math.max(80, w / 2)), x + Math.max(260, w / 2), y + 9, MUTED, false);
        g.text(font, fit(callsign(), 90), x + w - 100, y + 9, YELLOW, false);
    }

    private void drawContactDossier(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        EchoCyberGlassUi.panel(g, x, y, w, h, PANEL, 0x554DBAF4);
        g.text(font, "CONTACT DOSSIER", x + 9, y + 9, CYAN, false);
        statusPill(g, x + w - 62, y + 7, 52, statusLabel(), statusColor());
        int portraitY = y + 30;
        int portraitH = Math.max(96, Math.min(154, h - 226));
        Identifier portrait = parse(state.portraitTexture());
        if (portrait != null) {
            EchoCyberGlassUi.blitContain(g, portrait, x + 16, portraitY, w - 32, portraitH);
        } else {
            EchoCyberGlassUi.panel(g, x + 16, portraitY, w - 32, portraitH, 0x66091218, 0x334DBAF4);
            g.centeredText(font, "NO PORTRAIT", x + w / 2, portraitY + portraitH / 2 - 4, MUTED);
        }
        Identifier frame = parse(state.frameTexture());
        if (frame != null) {
            EchoCyberGlassUi.blitContain(g, frame, x + 18, portraitY + portraitH + 5, w - 36, 22);
        } else {
            EchoCyberGlassUi.frame(g, x + 18, portraitY + portraitH + 5, w - 36, 22, 0x334DBAF4);
        }

        int badgeY = portraitY + portraitH + 34;
        Identifier badge = parse(state.badgeTexture());
        EchoCyberGlassUi.panel(g, x + 10, badgeY, w - 20, 48, 0x77071421, 0x554DBAF4);
        if (badge != null) {
            EchoCyberGlassUi.blitContain(g, badge, x + 16, badgeY + 7, 34, 34);
        } else {
            EchoCyberGlassUi.panel(g, x + 16, badgeY + 7, 34, 34, 0x66091218, 0x334DBAF4);
        }
        g.text(font, fit(state.displayName(), w - 68), x + 58, badgeY + 9, CYAN, false);
        g.text(font, fit(state.role(), w - 68), x + 58, badgeY + 23, MUTED, false);

        int meterY = badgeY + 58;
        drawMeter(g, x + 10, meterY, w - 20, "Relationship", relationshipValue(), relationshipColor());
        drawMeter(g, x + 10, meterY + 30, w - 20, "Bridge Sync", bridgeValue(), bridgeValue() >= 70 ? GREEN : YELLOW);
        int lineY = meterY + 66;
        label(g, x, lineY, w, "Faction", shortId(state.faction()));
        label(g, x, lineY + 24, w, "Profile", shortId(state.profileId()));
        label(g, x, lineY + 48, w, "Theme", shortId(state.themeId()));
    }

    private void drawMainPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        EchoCyberGlassUi.panel(g, x, y, w, h, PANEL, 0x554DBAF4);
        String tab = state.currentTab();
        String title = switch (tab) {
            case "trade" -> "TRADE";
            case "services" -> "SERVICES";
            case "intel" -> "INTEL";
            default -> "DIALOGUE";
        };
        String subtitle = switch (tab) {
            case "trade" -> "Verified barter manifest";
            case "services" -> "Field support services";
            case "intel" -> "Local contact dossier";
            default -> "Conversation channel";
        };
        g.text(font, title, x + 10, y + 9, CYAN, false);
        statusPill(g, x + w - 70, y + 7, 58, activeChip(), activeColor());
        g.text(font, fit(subtitle, w - 24), x + 12, y + 26, YELLOW, false);
        int cy = y + 43;
        switch (tab) {
            case "trade" -> drawTrades(g, x, cy, w, h - 48, mouseX, mouseY);
            case "services" -> drawServices(g, x, cy, w, h - 48, mouseX, mouseY);
            case "intel" -> drawIntel(g, x, cy, w, h - 48);
            default -> drawDialogue(g, x, cy, w, h - 48, mouseX, mouseY);
        }
    }

    private void drawDialogue(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        int cy = wrapped(g, state.dialogueText().isBlank()
                ? Component.translatable("screen.echonpcore.npc.no_dialogue").getString()
                : state.dialogueText(), x + 12, y, w - 24, TEXT, y + Math.min(h, 68)) + 10;
        for (EchoNpcScreenState.DialogueOptionState option : state.dialogueOptions()) {
            if (cy + 54 > y + h - 4) {
                break;
            }
            boolean enabled = option.available();
            String detail = option.action().isBlank() ? "Next: " + value(option.next(), "intro") : actionLabel(option.action());
            cy = actionRow(g, x + 12, cy, w - 24, option.label(), detail,
                    enabled ? dialogueColor(option) : DISABLED, enabled, option.disabledReason(), mouseX, mouseY,
                    () -> EchoNetClientActions.sendServerboundAction(new SelectDialogueOptionPacket(state.entityId(), option.id())),
                    "Dialogue signal sent: " + option.label());
        }
        if (state.dialogueOptions().isEmpty()) {
            empty(g, x + 12, cy, w - 24, "No dialogue", Component.translatable("screen.echonpcore.npc.no_dialogue").getString());
        }
    }

    private void drawTrades(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        int cy = y;
        if (state.tradeGroups().isEmpty()) {
            empty(g, x + 12, cy, w - 24, "No trades", Component.translatable("screen.echonpcore.npc.no_trades").getString());
            return;
        }
        for (EchoNpcScreenState.TradeGroupState group : state.tradeGroups()) {
            g.text(font, fit(group.title(), w - 24), x + 12, cy, YELLOW, false);
            cy += 14;
            for (EchoNpcScreenState.TradeOfferState offer : group.offers()) {
                if (cy + 54 > y + h - 4) {
                    return;
                }
                boolean outOfStock = offer.limitedStock() && offer.stock() <= 0;
                boolean locked = !offer.missionAllowed() || !offer.factionAllowed();
                boolean enabled = !outOfStock && !locked;
                String detail = costLine(offer.input()) + " -> " + costLine(List.of(offer.output()))
                        + (offer.limitedStock() ? " / " + stockLabel(offer) : " / open stock");
                String disabled = locked ? value(offer.disabledReason(), !offer.missionAllowed()
                        ? value(offer.missionMessage(), "Mission requirement not met.")
                        : value(offer.factionMessage(), "Faction standing requirement not met."))
                        : outOfStock ? "This offer is out of stock." : "";
                cy = actionRow(g, x + 12, cy, w - 24, offer.title(), detail,
                        enabled ? GREEN : outOfStock ? YELLOW : DISABLED, enabled, disabled, mouseX, mouseY,
                        () -> EchoNetClientActions.sendServerboundAction(new RequestNpcTradePacket(state.entityId(), offer.id())),
                        "Trade request sent: " + offer.title());
            }
            cy += 4;
        }
    }

    private void drawServices(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        int cy = y;
        if (state.services().isEmpty()) {
            empty(g, x + 12, cy, w - 24, "No services", Component.translatable("screen.echonpcore.npc.no_services").getString());
            return;
        }
        for (EchoNpcScreenState.ServiceState service : state.services()) {
            if (cy + 54 > y + h - 4) {
                return;
            }
            boolean coolingDown = service.cooldownRemaining() > 0;
            boolean locked = !service.missionAllowed() || !service.factionAllowed();
            boolean enabled = !coolingDown && !locked;
            String detail = coolingDown ? cooldownLabel(service.cooldownRemaining()) + " / " + service.description()
                    : (service.cost().isEmpty() ? "free" : costLine(service.cost())) + " / " + service.description();
            String disabled = locked ? value(service.disabledReason(),
                    !service.missionAllowed() ? service.missionMessage() : "Faction standing requirement not met.")
                    : coolingDown ? "Service cooldown remaining: " + cooldownLabel(service.cooldownRemaining()) : "";
            cy = actionRow(g, x + 12, cy, w - 24, service.title(), detail,
                    enabled ? GREEN : coolingDown ? YELLOW : DISABLED, enabled, disabled, mouseX, mouseY,
                    () -> EchoNetClientActions.sendServerboundAction(new RequestNpcServicePacket(state.entityId(), service.id())),
                    "Service request sent: " + service.title());
        }
    }

    private void drawIntel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        int cy = wrapped(g, "Local contact context from the synced NPC state and installed ECHO integrations.",
                x + 12, y, w - 24, TEXT, y + h - 12) + 10;
        cy = intelRow(g, x + 12, cy, w - 24, "Profile", state.profileId(), CYAN);
        cy = intelRow(g, x + 12, cy, w - 24, "Theme", state.themeId(), YELLOW);
        cy = intelRow(g, x + 12, cy, w - 24, "Relationship", state.relationship(), relationshipColor());
        intelRow(g, x + 12, cy, w - 24, "Optional Bridges", bridgeLine(), GREEN);
    }

    private int intelRow(GuiGraphicsExtractor g, int x, int y, int w, String title, String detail, int color) {
        g.fill(x, y, x + w, y + 34, PANEL_SOFT);
        EchoCyberGlassUi.frame(g, x, y, w, 34, 0x554DBAF4);
        g.fill(x, y, x + 3, y + 34, color);
        g.text(font, fit(title, w - 16), x + 8, y + 5, color, false);
        g.text(font, fit(detail, w - 16), x + 8, y + 19, MUTED, false);
        return y + 40;
    }

    private void drawChannelRail(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        EchoCyberGlassUi.panel(g, x, y, w, h, PANEL, 0x554DBAF4);
        g.text(font, "CHANNEL RAIL", x + 9, y + 9, CYAN, false);
        int cy = y + 28;
        cy = tabRow(g, x + 8, cy, w - 16, "talk", "Talk", countLabel(dialogueCount(), "signal"), "TALK", mouseX, mouseY);
        cy = tabRow(g, x + 8, cy, w - 16, "trade", "Trade", countLabel(tradeCount(), "signal"), "TRD", mouseX, mouseY);
        cy = tabRow(g, x + 8, cy, w - 16, "services", "Services", countLabel(state.services().size(), "signal"), "SVC", mouseX, mouseY);
        cy = tabRow(g, x + 8, cy, w - 16, "intel", "Intel", "4 records", "INT", mouseX, mouseY);
        cy = tabRow(g, x + 8, cy, w - 16, "exit", "Exit", "Close channel", "EXT", mouseX, mouseY);

        int statusY = Math.min(y + h - 120, cy + 8);
        g.text(font, "LINK STATUS", x + 9, statusY, YELLOW, false);
        statusPill(g, x + 9, statusY + 16, 70, statusLabel(), statusColor());
        wrapped(g, statusText(), x + 9, statusY + 40, w - 18, MUTED, y + h - 14);
    }

    private int tabRow(GuiGraphicsExtractor g, int x, int y, int w, String tab, String label, String detail,
            String chip, int mouseX, int mouseY) {
        int h = 31;
        boolean selected = state.currentTab().equals(tab);
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int accent = selected ? CYAN : "exit".equals(tab) ? RED : GREEN;
        g.fill(x, y, x + w, y + h, selected ? 0xEE0B2E38 : hovered ? 0xCC15232D : PANEL_SOFT);
        EchoCyberGlassUi.frame(g, x, y, w, h, selected ? 0xDD35DCFF : 0x554DBAF4);
        statusPill(g, x + 5, y + 7, 36, chip, accent);
        g.text(font, fit(label, w - 52), x + 48, y + 5, accent, false);
        g.text(font, fit(detail, w - 52), x + 48, y + 18, MUTED, false);
        hitboxes.add(new Hitbox(x, y, w, h, true, "",
                () -> {
                    if ("exit".equals(tab)) {
                        onClose();
                    } else if (!selected) {
                        switchTab(tab);
                    }
                },
                selected ? "Channel already active: " + label
                        : "exit".equals(tab) ? "Closing NPC channel." : "Channel selected: " + label));
        return y + h + 6;
    }

    private void drawFooter(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        EchoCyberGlassUi.panel(g, x, y, w, h, 0xCC091019, 0x554DBAF4);
        statusPill(g, x + 8, y + 7, 64, "NPCORE", statusColor());
        g.text(font, fit(statusText(), w - 260), x + 80, y + 10, statusColor(), false);
        footerButton(g, x + w - 176, y + 5, 82, "Refresh", mouseX, mouseY,
                () -> EchoNetClientActions.sendServerboundAction(
                        new RequestNpcScreenRefreshPacket(state.entityId(), state.currentTab())),
                "Refresh requested.");
        footerButton(g, x + w - 88, y + 5, 78, "Close", mouseX, mouseY, this::onClose, "Closing NPC channel.");
    }

    private void footerButton(GuiGraphicsExtractor g, int x, int y, int w, String label, int mouseX, int mouseY,
            Runnable action, String status) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 22;
        EchoCyberGlassUi.button(g, font, x, y, w, 22, label, hovered, true, CYAN);
        hitboxes.add(new Hitbox(x, y, w, 22, true, "", action, status));
    }

    private int actionRow(GuiGraphicsExtractor g, int x, int y, int w, String title, String detail,
            int accent, boolean enabled, String disabledReason, int mouseX, int mouseY, Runnable action, String status) {
        int h = 48;
        boolean hovered = enabled && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        g.fill(x, y, x + w, y + h, hovered ? 0xCC15232D : enabled ? PANEL_SOFT : 0xAA0A1017);
        EchoCyberGlassUi.frame(g, x, y, w, h, enabled ? 0x664DBAF4 : 0x445F6D7A);
        statusPill(g, x + 7, y + 15, 50, enabled ? "READY" : "LOCK", accent);
        g.text(font, fit(title, w - 74), x + 66, y + 7, accent, false);
        g.text(font, fit(detail, w - 74), x + 66, y + 22, enabled ? MUTED : DISABLED, false);
        hitboxes.add(new Hitbox(x, y, w, h, enabled, disabledReason, action, status));
        return y + h + 6;
    }

    private void drawMeter(GuiGraphicsExtractor g, int x, int y, int w, String label, int value, int color) {
        g.text(font, label, x, y, MUTED, false);
        String right = value + "/100";
        g.text(font, right, x + w - font.width(right), y, MUTED, false);
        EchoCyberGlassUi.meter(g, x, y + 12, w, 10, Math.max(0, Math.min(w - 2, (w - 2) * value / 100)), color);
    }

    private void empty(GuiGraphicsExtractor g, int x, int y, int w, String title, String body) {
        EchoCyberGlassUi.calmPanel(g, x, y, w, 48, 0xAA0A1017, 0x554DBAF4);
        g.text(font, fit(title, w - 16), x + 8, y + 9, YELLOW, false);
        g.text(font, fit(body, w - 16), x + 8, y + 24, MUTED, false);
    }

    private void switchTab(String tab) {
        this.state = new EchoNpcScreenState(state.entityId(), state.profileId(), state.displayName(), state.role(),
                state.faction(), state.relationship(), state.portraitTexture(), state.badgeTexture(), state.frameTexture(),
                state.themeId(), tab, state.dialogueNodeId(), state.dialogueText(), state.dialogueOptions(),
                state.tradeGroups(), state.services(), state.status());
        this.localStatus = "Channel selected: " + tab;
    }

    private void label(GuiGraphicsExtractor g, int x, int y, int w, String label, String value) {
        g.text(font, label, x + 10, y, MUTED, false);
        g.text(font, fit(value, w - 20), x + 10, y + 10, TEXT, false);
    }

    private int wrapped(GuiGraphicsExtractor g, String text, int x, int y, int maxW, int color, int maxY) {
        int cy = y;
        for (var line : font.split(Component.literal(text == null ? "" : text), maxW)) {
            if (cy + 9 > maxY) {
                return cy;
            }
            g.text(font, line, x, cy, color, false);
            cy += 11;
        }
        return cy;
    }

    private String costLine(List<EchoNpcScreenState.CostState> costs) {
        if (costs == null || costs.isEmpty()) {
            return "free";
        }
        return costs.stream()
                .filter(cost -> cost != null && !cost.item().isBlank() && cost.count() > 0)
                .map(cost -> cost.count() + "x " + shortId(cost.item()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("free");
    }

    private String stockLabel(EchoNpcScreenState.TradeOfferState offer) {
        if (!offer.limitedStock()) {
            return "open stock";
        }
        return offer.stock() <= 0 ? "out of stock" : "stock " + offer.stock();
    }

    private String cooldownLabel(long ticks) {
        if (ticks <= 0) {
            return "ready";
        }
        long seconds = Math.max(1L, Math.round(ticks / 20.0D));
        return seconds + "s cooldown";
    }

    private String bridgeLine() {
        return "Terminal " + loadedLabel("echoterminal") + " / Mission " + loadedLabel("echomissioncore")
                + " / World " + loadedLabel("echoworldcore") + " / HoloMap " + loadedLabel("echoholomap")
                + " / Data " + loadedLabel("echodatacore") + " / Fallback ready";
    }

    private String loadedLabel(String modid) {
        return EchoRuntimeModules.isLoaded(modid) ? "ready" : "absent";
    }

    private int bridgeValue() {
        int ready = 1;
        int total = 6;
        ready += EchoRuntimeModules.isLoaded("echoterminal") ? 1 : 0;
        ready += EchoRuntimeModules.isLoaded("echomissioncore") ? 1 : 0;
        ready += EchoRuntimeModules.isLoaded("echoworldcore") ? 1 : 0;
        ready += EchoRuntimeModules.isLoaded("echoholomap") ? 1 : 0;
        ready += EchoRuntimeModules.isLoaded("echodatacore") ? 1 : 0;
        return ready * 100 / total;
    }

    private int relationshipValue() {
        String lower = value(state.relationship(), "neutral").toLowerCase(Locale.ROOT);
        if (lower.contains("allied") || lower.contains("trusted") || lower.contains("friendly")) {
            return 88;
        }
        if (lower.contains("contact") || lower.contains("known")) {
            return 68;
        }
        if (lower.contains("hostile") || lower.contains("enemy")) {
            return 18;
        }
        if (lower.contains("cold") || lower.contains("strained")) {
            return 34;
        }
        return 52;
    }

    private int relationshipColor() {
        int value = relationshipValue();
        return value >= 70 ? GREEN : value >= 45 ? YELLOW : RED;
    }

    private int activeColor() {
        return switch (state.currentTab()) {
            case "trade" -> GREEN;
            case "services" -> YELLOW;
            case "intel" -> CYAN;
            default -> CYAN;
        };
    }

    private String activeChip() {
        return switch (state.currentTab()) {
            case "trade" -> "TRD";
            case "services" -> "SVC";
            case "intel" -> "INT";
            default -> "TALK";
        };
    }

    private int dialogueColor(EchoNpcScreenState.DialogueOptionState option) {
        return switch (option.action()) {
            case "open_trade" -> GREEN;
            case "open_services" -> YELLOW;
            case "open_intel" -> CYAN;
            case "close" -> RED;
            default -> CYAN;
        };
    }

    private String actionLabel(String action) {
        return switch (action) {
            case "open_trade" -> "Open trade channel";
            case "open_services" -> "Open service channel";
            case "open_intel" -> "Open intel dossier";
            case "discover_contact" -> "Record Terminal contact";
            case "close" -> "Close interaction";
            default -> value(action, "Action");
        };
    }

    private int dialogueCount() {
        return state.dialogueOptions().size();
    }

    private int tradeCount() {
        int count = 0;
        for (EchoNpcScreenState.TradeGroupState group : state.tradeGroups()) {
            count += group.offers().size();
        }
        return count;
    }

    private String callsign() {
        String safe = value(state.displayName(), shortId(state.profileId())).trim();
        String[] words = safe.split("\\s+");
        String left = words.length == 0 ? safe : words[0];
        String right = words.length > 1 ? words[words.length - 1] : shortId(state.profileId());
        return (segment(left, 3) + "-" + segment(right, 3)).toUpperCase(Locale.ROOT);
    }

    private String segment(String value, int length) {
        String cleaned = value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "");
        if (cleaned.isBlank()) {
            return "NPC";
        }
        return cleaned.substring(0, Math.min(length, cleaned.length()));
    }

    private String statusText() {
        if (!localStatus.isBlank()) {
            return localStatus;
        }
        return state.status().isBlank() ? "Server-authoritative NPC channel open." : state.status();
    }

    private String statusLabel() {
        String lower = statusText().toLowerCase(Locale.ROOT);
        return lower.contains("missing") || lower.contains("unavailable") || lower.contains("cooling")
                || lower.contains("stock") ? "CHECK" : "ONLINE";
    }

    private int statusColor() {
        return "CHECK".equals(statusLabel()) ? YELLOW : GREEN;
    }

    private void statusPill(GuiGraphicsExtractor g, int x, int y, int w, String label, int color) {
        g.fill(x, y, x + w, y + 14, 0x4410243A);
        EchoCyberGlassUi.frame(g, x, y, w, 14, color);
        g.centeredText(font, fit(label, w - 6), x + w / 2, y + 4, color);
    }

    private Identifier parse(String value) {
        return value == null || value.isBlank() ? null : Identifier.tryParse(value);
    }

    private String shortId(String id) {
        int idx = id == null ? -1 : id.indexOf(':');
        return idx >= 0 && idx + 1 < id.length() ? id.substring(idx + 1) : value(id, "");
    }

    private String value(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private String fit(String text, int maxW) {
        String safe = text == null ? "" : text;
        if (maxW <= 0) {
            return "";
        }
        if (font.width(safe) <= maxW) {
            return safe;
        }
        if (maxW <= font.width("...")) {
            return "";
        }
        return font.plainSubstrByWidth(safe, maxW - font.width("...")) + "...";
    }

    private String countLabel(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    private Layout layout() {
        int margin = Math.max(12, Math.min(28, width / 28));
        int w = Math.min(980, width - margin * 2);
        int h = Math.min(540, height - margin * 2);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int header = 56;
        int footer = 36;
        int gap = 8;
        int contentY = y + header + gap;
        int contentH = h - header - footer - gap * 3;
        int leftW = Math.max(172, Math.min(236, w / 4));
        int railW = Math.max(164, Math.min(210, w / 5));
        int centerW = Math.max(220, w - leftW - railW - gap * 4);
        int leftX = x + gap;
        int centerX = leftX + leftW + gap;
        int railX = centerX + centerW + gap;
        return new Layout(x, y, w, h, leftX, centerX, railX, leftW, centerW, railW, contentY, contentH,
                y + h - footer - gap, footer);
    }

    private record Layout(int x, int y, int w, int h, int leftX, int centerX, int railX, int leftW,
            int centerW, int railW, int contentY, int contentH, int footerY, int footerH) {
    }

    private record Hitbox(int x, int y, int w, int h, boolean enabled, String disabledReason,
            Runnable action, String status) {
        boolean contains(int px, int py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }
}
