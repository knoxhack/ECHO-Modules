package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoashfallprotocol.network.FactionDialogueOpenPacket;
import com.knoxhack.echoashfallprotocol.network.FactionNpcActionPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Playable Ashfall faction conversation screen backed by Echo Core snapshots.
 */
public class FactionDialogueScreen extends Screen {
    private static final int PANEL_BG = 0xF0060A0F;
    private static final int PANEL = 0xDD101820;
    private static final int PANEL_SOFT = 0xAA14212A;
    private static final int BORDER = 0x884DBAF4;
    private static final int CYAN = 0xFF4DBAF4;
    private static final int GREEN = 0xFF55D88A;
    private static final int YELLOW = 0xFFFFD166;
    private static final int RED = 0xFFFF6B6B;
    private static final int TEXT = 0xFFE7EEF6;
    private static final int MUTED = 0xFF91A3B8;
    private static final int DISABLED = 0xFF5F6D7A;

    private final FactionDialogueOpenPacket packet;
    private final List<Hitbox> hitboxes = new ArrayList<>();
    private String responseLog = "Awaiting field response.";

    public FactionDialogueScreen(FactionDialogueOpenPacket packet) {
        super(Component.literal("Faction Contact"));
        this.packet = packet;
        if (!packet.npcMemory().isBlank()) {
            this.responseLog = packet.npcMemory();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        hitboxes.clear();
        g.fill(0, 0, width, height, 0x99000000);

        Layout layout = layout();
        drawFrame(g, layout);
        drawHeader(g, layout);
        drawLeftPanel(g, layout.leftX(), layout.contentY(), layout.leftW(), layout.contentH());
        drawCenterPanel(g, layout.centerX(), layout.contentY(), layout.centerW(), layout.contentH());
        drawRightPanel(g, layout.rightX(), layout.contentY(), layout.rightW(), layout.contentH(), mouseX, mouseY);
        drawLog(g, layout.x(), layout.logY(), layout.w(), layout.logH());
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
                        responseLog = hitbox.lockedReason().isBlank() ? "This contact is not ready to answer." : hitbox.lockedReason();
                        return true;
                    }
                    send(hitbox.actionId(), hitbox.targetId());
                    responseLog = "Signal sent: " + hitbox.label();
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
    public boolean isPauseScreen() {
        return Minecraft.getInstance().isSingleplayer();
    }

    private void send(String actionId, String targetId) {
        EchoNetClientActions.sendServerboundAction(new FactionNpcActionPacket(packet.entityId(), actionId, targetId));
    }

    private Layout layout() {
        int margin = Math.max(12, Math.min(28, width / 28));
        int w = Math.min(920, width - margin * 2);
        int h = Math.min(520, height - margin * 2);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int headerH = 48;
        int logH = 58;
        int gap = 8;
        int contentY = y + headerH + gap;
        int contentH = h - headerH - logH - gap * 3;
        int leftW = Math.max(150, Math.min(210, w / 4));
        int rightW = Math.max(190, Math.min(260, w / 3));
        int centerW = Math.max(160, w - leftW - rightW - gap * 2);
        int leftX = x + gap;
        int centerX = leftX + leftW + gap;
        int rightX = centerX + centerW + gap;
        return new Layout(x, y, w, h, leftX, centerX, rightX, leftW, centerW, rightW,
                contentY, contentH, y + h - logH - gap, logH);
    }

    private void drawFrame(GuiGraphicsExtractor g, Layout layout) {
        g.fill(layout.x(), layout.y(), layout.x() + layout.w(), layout.y() + layout.h(), PANEL_BG);
        g.outline(layout.x(), layout.y(), layout.w(), layout.h(), BORDER);
        g.outline(layout.x() + 4, layout.y() + 4, layout.w() - 8, layout.h() - 8, 0x334DBAF4);
    }

    private void drawHeader(GuiGraphicsExtractor g, Layout layout) {
        int x = layout.x() + 10;
        int y = layout.y() + 10;
        int w = layout.w() - 20;
        g.fill(x, y, x + w, y + 34, 0xBB0B141C);
        g.outline(x, y, w, 34, 0x554DBAF4);
        g.text(font, fit(packet.factionName(), w / 2), x + 10, y + 8, CYAN, false);
        g.text(font, fit(packet.roleName() + " / " + packet.standing() + " / " + packet.reputation(), w / 2),
                x + w / 2, y + 8, MUTED, false);
    }

    private void drawLeftPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        drawPanel(g, x, y, w, h, "CONTACT", CYAN);
        int cy = y + 28;
        cy = labelValue(g, x, cy, w, "Faction", packet.shortName());
        cy = labelValue(g, x, cy, w, "Role", packet.roleName());
        cy = labelValue(g, x, cy, w, "Standing", packet.standing());
        cy = labelValue(g, x, cy, w, "Rep", Integer.toString(packet.reputation()));
        cy = labelValue(g, x, cy, w, "Contacts", Integer.toString(packet.contactCount()));
        if (packet.lastInteractionTick() > 0L) {
            cy = labelValue(g, x, cy, w, "Last", packet.lastInteractionTick() + "t");
        }
        if (!packet.lastRoleId().isBlank()) {
            labelValue(g, x, cy, w, "Last Role", packet.lastRoleId());
        }
    }

    private void drawCenterPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        drawPanel(g, x, y, w, h, "DIALOGUE", GREEN);
        int cy = y + 28;
        cy = wrapped(g, packet.greeting(), x + 10, cy, w - 20, TEXT, y + h - 64);
        cy += 10;
        if (!packet.localContext().isBlank()) {
            g.text(font, "Route Context", x + 10, cy, YELLOW, false);
            cy += 13;
            cy = wrapped(g, packet.localContext(), x + 10, cy, w - 20, MUTED, y + h - 32);
        }
        if (!packet.activeContractId().isBlank()) {
            g.fill(x + 10, y + h - 28, x + w - 10, y + h - 11, 0x3320A060);
            g.text(font, fit("Active contract: " + packet.activeContractId(), w - 24), x + 14, y + h - 23, GREEN, false);
        }
    }

    private void drawRightPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int mouseX, int mouseY) {
        drawPanel(g, x, y, w, h, "ACTIONS", YELLOW);
        int cy = y + 27;
        for (FactionDialogueOpenPacket.ActionEntry action : packet.actions()) {
            cy = actionRow(g, x + 8, cy, w - 16, action.label(), action.description(),
                    action.id(), "", action.enabled(), action.lockedReason(), action.service() ? GREEN : CYAN,
                    mouseX, mouseY);
        }

        cy += 5;
        g.text(font, "Field Contracts", x + 10, cy, YELLOW, false);
        cy += 13;
        for (FactionDialogueOpenPacket.ContractEntry contract : packet.contracts()) {
            String actionId = contract.active()
                    ? EchoCoreServices.COMPLETE_FACTION_CONTRACT_ACTION.toString()
                    : EchoCoreServices.ACCEPT_FACTION_CONTRACT_ACTION.toString();
            boolean enabled = contract.active() ? contract.canComplete() : contract.canAccept();
            String state = contract.completed() ? "Completed"
                    : contract.active() ? "Complete"
                    : enabled ? "Accept" : "Locked";
            String lock = contract.completed() ? "This field contract is already archived."
                    : contract.lockedReason().isBlank()
                            ? packet.activeContractId().isBlank() ? "" : "Another faction contract is already active."
                            : contract.lockedReason();
            String detail = contract.active() && !contract.progressLine().isBlank()
                    ? contract.progressLine()
                    : contract.objective();
            cy = actionRow(g, x + 8, cy, w - 16, state + ": " + contract.title(), detail,
                    actionId, contract.id(), enabled, lock, contract.active() ? GREEN : YELLOW, mouseX, mouseY);
        }
    }

    private int actionRow(GuiGraphicsExtractor g, int x, int y, int w, String title, String detail,
            String actionId, String targetId, boolean enabled, String lockedReason, int accent, int mouseX, int mouseY) {
        int h = 36;
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int color = enabled ? accent : DISABLED;
        g.fill(x, y, x + w, y + h, hovered && enabled ? 0xCC15232D : PANEL_SOFT);
        g.outline(x, y, w, h, enabled ? 0x664DBAF4 : 0x445F6D7A);
        g.fill(x, y, x + 3, y + h, color);
        g.text(font, fit(title, w - 12), x + 8, y + 6, color, false);
        g.text(font, fit(detail, w - 12), x + 8, y + 20, enabled ? MUTED : DISABLED, false);
        hitboxes.add(new Hitbox(x, y, w, h, actionId, targetId, title, enabled, lockedReason));
        return y + h + 6;
    }

    private void drawLog(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 8, y, x + w - 8, y + h, 0xCC091019);
        g.outline(x + 8, y, w - 16, h, 0x554DBAF4);
        g.text(font, "FIELD RESPONSE", x + 18, y + 8, CYAN, false);
        wrapped(g, responseLog, x + 18, y + 22, w - 36, TEXT, y + h - 6);
    }

    private void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title, int color) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.outline(x, y, w, h, 0x554DBAF4);
        g.fill(x, y, x + w, y + 2, color);
        g.text(font, title, x + 9, y + 9, color, false);
    }

    private int labelValue(GuiGraphicsExtractor g, int x, int y, int w, String label, String value) {
        g.text(font, label, x + 10, y, MUTED, false);
        g.text(font, fit(value, w - 20), x + 10, y + 10, TEXT, false);
        return y + 27;
    }

    private int wrapped(GuiGraphicsExtractor g, String text, int x, int y, int maxW, int color, int maxY) {
        int cy = y;
        Component component = Component.literal(text == null ? "" : text);
        for (var line : font.split(component, maxW)) {
            if (cy + 9 > maxY) {
                return cy;
            }
            g.text(font, line, x, cy, color, false);
            cy += 11;
        }
        return cy;
    }

    private String fit(String text, int maxW) {
        String safe = text == null ? "" : text;
        if (font.width(safe) <= maxW) {
            return safe;
        }
        if (maxW <= font.width("...")) {
            return "";
        }
        return font.plainSubstrByWidth(safe, maxW - font.width("...")) + "...";
    }

    private record Layout(int x, int y, int w, int h, int leftX, int centerX, int rightX, int leftW,
            int centerW, int rightW, int contentY, int contentH, int logY, int logH) {
    }

    private record Hitbox(int x, int y, int w, int h, String actionId, String targetId, String label,
            boolean enabled, String lockedReason) {
        boolean contains(int px, int py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }
    }
}
