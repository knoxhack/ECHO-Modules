package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.ResearchLabMenu;
import com.knoxhack.echoashfallprotocol.item.SchematicFragmentItem;
import com.knoxhack.echoashfallprotocol.network.ResearchAnalyzeFragmentPacket;
import com.knoxhack.echoashfallprotocol.network.ResearchPurchasePacket;
import com.knoxhack.echoashfallprotocol.research.Perk;
import com.knoxhack.echoashfallprotocol.research.PerkRegistry;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ECHO-styled Research Lab interface for browsing perks and analyzing schematics.
 */
public class ResearchLabScreen extends AbstractContainerScreen<ResearchLabMenu> {

    private static final int COL_BG = 0xEE0A0F1A;
    private static final int COL_PANEL = 0xAA101A26;
    private static final int COL_PANEL_DARK = 0xFF0D1218;
    private static final int COL_HEADER = 0xFF162535;
    private static final int COL_ACCENT = 0xFF4DBAF4;
    private static final int COL_ACCENT_DIM = 0x664DBAF4;
    private static final int COL_TEXT = 0xFFE8F0F5;
    private static final int COL_DIM = 0xFF8A9BB0;
    private static final int COL_GREEN = 0xFF42D67E;
    private static final int COL_YELLOW = 0xFFF0C94B;
    private static final int COL_RED = 0xFFE25959;
    private static final int COL_LOCK = 0xFF555E6B;

    private static final Perk.Branch[] BRANCHES = {
        Perk.Branch.RADWARDEN_TECH,
        Perk.Branch.CRASHBREAK_SALVAGE,
        Perk.Branch.SPOREBOUND_BIO
    };

    private static final int GUI_WIDTH = 600;
    private static final int GUI_HEIGHT = 380;
    private static final int HEADER_H = 42;
    private static final int TAB_H = 24;
    private static final int LIST_W = 372;
    private static final int DETAIL_W = 188;
    private static final int SCHEMATIC_H = 60;
    private static final int COMMAND_H = 24;

    private final List<PerkHitbox> perkHitboxes = new ArrayList<>();
    private final List<TabHitbox> tabHitboxes = new ArrayList<>();
    private final List<FragmentHitbox> fragmentHitboxes = new ArrayList<>();
    private final List<UnlockHitbox> unlockHitboxes = new ArrayList<>();

    private int selectedBranch = 0;
    private String selectedPerkId = null;
    private long animTick = 0;

    public ResearchLabScreen(ResearchLabMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.literal("Research Lab"), GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        animTick++;
        this.leftPos = (width - imageWidth) / 2;
        this.topPos = (height - imageHeight) / 2;

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ResearchData data = ResearchData.get(player);
        validateSelectedPerk();
        perkHitboxes.clear();
        tabHitboxes.clear();
        fragmentHitboxes.clear();
        unlockHitboxes.clear();

        drawBackground(graphics, leftPos, topPos);
        drawHeader(graphics, leftPos, topPos, data);
        drawTabs(graphics, leftPos, topPos, data, mouseX, mouseY);
        drawPerkLanes(graphics, leftPos, topPos, player, data, mouseX, mouseY);
        drawDetailPanel(graphics, leftPos, topPos, player, data, mouseX, mouseY);
        drawSchematicAnalyzer(graphics, leftPos, topPos, player, data, mouseX, mouseY);

        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // This screen draws all labels in absolute coordinates.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x();
            int my = (int) event.y();

            for (TabHitbox tab : tabHitboxes) {
                if (tab.contains(mx, my)) {
                    selectedBranch = tab.branchIndex;
                    selectedPerkId = null;
                    return true;
                }
            }

            for (FragmentHitbox hitbox : fragmentHitboxes) {
                if (hitbox.contains(mx, my)) {
                    if (hitbox.count > 0) {
                        sendResearchAction(new ResearchAnalyzeFragmentPacket(hitbox.typeKey()));
                    }
                    return true;
                }
            }

            for (UnlockHitbox hitbox : unlockHitboxes) {
                if (hitbox.contains(mx, my)) {
                    if (hitbox.enabled) {
                        sendResearchAction(new ResearchPurchasePacket(hitbox.perk.getId()));
                    }
                    return true;
                }
            }

            Player player = Minecraft.getInstance().player;
            if (player != null) {
                for (PerkHitbox hitbox : perkHitboxes) {
                    if (hitbox.contains(mx, my)) {
                        selectedPerkId = hitbox.perk.getId();
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void sendResearchAction(CustomPacketPayload payload) {
        if (EchoNetClientActions.trySendServerboundAction(payload)) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Research link unavailable. Reopen the lab and try again.")
                .withStyle(ChatFormatting.RED));
        }
    }

    private void drawBackground(GuiGraphicsExtractor g, int x, int y) {
        if (!MachineGuiSkins.renderResearchLab(g, x, y, imageWidth, imageHeight)) {
            g.fill(x, y, x + imageWidth, y + imageHeight, COL_BG);
        } else {
            g.fill(x, y, x + imageWidth, y + imageHeight, 0x5A050A12);
        }

        for (int row = y + 4; row < y + imageHeight - 4; row += 4) {
            int alpha = ((row / 4) % 3 == 0) ? 0x12 : 0x08;
            g.fill(x + 3, row, x + imageWidth - 3, row + 1, (alpha << 24) | 0x00FFFFFF);
        }

        int pulse = 188 + (int) (46 * Math.sin(animTick / 44.0));
        int border = (Math.max(120, pulse) << 24) | 0x004DBAF4;
        g.fill(x, y, x + imageWidth, y + 2, border);
        g.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, border);
        g.fill(x, y, x + 2, y + imageHeight, border);
        g.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, border);
    }

    private void drawHeader(GuiGraphicsExtractor g, int x, int y, ResearchData data) {
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + HEADER_H - 2, COL_HEADER);
        g.fill(x + 2, y + HEADER_H - 2, x + imageWidth - 2, y + HEADER_H, COL_ACCENT);

        g.text(font, "RESEARCH LAB - PERK MATRIX", x + 14, y + 10, COL_ACCENT, false);

        String points = data.getPoints() + " / " + ResearchData.MAX_POINTS + " RP";
        int pointsColor = data.getPoints() >= 50 ? COL_GREEN : COL_YELLOW;
        g.text(font, points, x + imageWidth - font.width(points) - 14, y + 9, pointsColor, false);

        String tier = "Tier " + data.getCurrentTier();
        g.text(font, tier, x + imageWidth - font.width(tier) - 14, y + 23, COL_DIM, false);

        int barX = x + 208;
        int barY = y + 24;
        int barW = 176;
        float pct = data.getPoints() / (float) ResearchData.MAX_POINTS;
        g.fill(barX, barY, barX + barW, barY + 5, 0xFF111722);
        g.fill(barX, barY, barX + Math.max(1, (int) (barW * pct)), barY + 5, COL_ACCENT);
    }

    private void drawTabs(GuiGraphicsExtractor g, int x, int y, ResearchData data, int mouseX, int mouseY) {
        int tabY = y + HEADER_H + 8;
        int tabX = x + 14;
        int tabW = 122;

        for (int i = 0; i < BRANCHES.length; i++) {
            int tx = tabX + i * (tabW + 5);
            int color = BRANCHES[i].getColor();
            boolean active = i == selectedBranch;
            boolean hovered = contains(mouseX, mouseY, tx, tabY, tabW, TAB_H);
            int bg = active ? 0xFF182A3C : hovered ? 0xFF142233 : COL_PANEL_DARK;

            g.fill(tx, tabY, tx + tabW, tabY + TAB_H, bg);
            g.fill(tx, tabY + TAB_H - 2, tx + tabW, tabY + TAB_H, active ? color : COL_ACCENT_DIM);
            g.fill(tx, tabY, tx + 2, tabY + TAB_H, active ? color : 0x662A3A4A);

            String name = fitText(BRANCHES[i].getName().toUpperCase(Locale.ROOT), tabW - 12);
            String progress = branchProgressLabel(BRANCHES[i], data);
            g.text(font, name, tx + 7, tabY + 4, active ? color : COL_DIM, false);
            g.text(font, progress, tx + 7, tabY + 15, active ? COL_TEXT : COL_LOCK, false);
            tabHitboxes.add(new TabHitbox(i, tx, tabY, tabW, TAB_H));
        }
    }

    private void drawPerkLanes(GuiGraphicsExtractor g, int x, int y, Player player, ResearchData data, int mouseX, int mouseY) {
        Perk.Branch branch = BRANCHES[selectedBranch];
        int listX = x + 14;
        int listY = y + HEADER_H + TAB_H + 18;
        int listH = imageHeight - HEADER_H - TAB_H - SCHEMATIC_H - 42;

        g.fill(listX, listY, listX + LIST_W, listY + listH, COL_PANEL);
        g.fill(listX, listY, listX + LIST_W, listY + 1, branch.getColor());

        String summary = fitText(branch.getDescription() + "  |  Click a node to inspect. Unlock from DETAILS.", LIST_W - 16);
        g.text(font, summary, listX + 8, listY + 8, COL_DIM, false);

        int laneY = listY + 27;
        for (PerkChain chain : branchChains(branch)) {
            drawChainLane(g, chain, listX + 8, laneY, LIST_W - 16, player, data, mouseX, mouseY);
            laneY += 54;
        }
    }

    private void drawChainLane(GuiGraphicsExtractor g, PerkChain chain, int x, int y, int w,
                               Player player, ResearchData data, int mouseX, int mouseY) {
        Perk.Branch branch = BRANCHES[selectedBranch];
        int labelW = 92;
        int cardGap = 8;
        int cardW = (w - labelW - cardGap * 2) / 3;
        int cardH = 42;
        int cardY = y + 8;
        int cardsX = x + labelW;

        g.text(font, fitText(chain.label, labelW - 8), x, y + 2, branch.getColor(), false);
        g.text(font, chain.unlockedCount(data) + "/" + chain.perks.size() + " nodes", x, y + 15, COL_DIM, false);
        g.fill(x, y + 28, x + labelW - 10, y + 29, COL_ACCENT_DIM);

        for (int tier = 1; tier <= 3; tier++) {
            Perk perk = chain.perkAtTier(tier);
            int cardX = cardsX + (tier - 1) * (cardW + cardGap);
            if (tier > 1) {
                g.fill(cardX - cardGap, cardY + 20, cardX, cardY + 21, 0xFF2A3A4A);
            }

            if (perk == null) {
                g.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0x66101722);
                g.text(font, "Tier " + tier, cardX + 8, cardY + 13, COL_LOCK, false);
                continue;
            }

            PerkState state = perkState(perk, player, data);
            int stateColor = stateColor(state, branch.getColor());
            boolean hovered = contains(mouseX, mouseY, cardX, cardY, cardW, cardH);
            boolean selected = selectedPerkId != null && selectedPerkId.equals(perk.getId());
            int bg = selected ? 0xFF182A3C : hovered ? 0xFF1A2E40 : COL_PANEL_DARK;

            g.fill(cardX, cardY, cardX + cardW, cardY + cardH, bg);
            g.fill(cardX, cardY, cardX + 3, cardY + cardH, stateColor);
            g.fill(cardX + 3, cardY, cardX + cardW, cardY + 1, selected ? stateColor : 0x332A3A4A);
            g.fill(cardX, cardY + cardH - 1, cardX + cardW, cardY + cardH, 0x332A3A4A);
            if (selected) {
                g.outline(cardX, cardY, cardW, cardH, stateColor);
            }

            String tierText = "T" + tier;
            g.text(font, tierText, cardX + 8, cardY + 5, stateColor, false);

            String cost = perk.getCost() + " RP";
            int costColor = state == PerkState.NEED_POINTS ? COL_RED : state == PerkState.READY ? COL_YELLOW : COL_DIM;
            String fitCost = fitText(cost, Math.max(28, cardW - 34));
            g.text(font, fitCost, cardX + cardW - font.width(fitCost) - 7, cardY + 5, costColor, false);

            g.text(font, fitText(perk.getName(), cardW - 16), cardX + 8, cardY + 17,
                state == PerkState.UNLOCKED ? COL_TEXT : COL_DIM, false);
            g.text(font, fitText(shortStatus(perk, state, data), cardW - 16), cardX + 8, cardY + 29,
                stateColor, false);

            perkHitboxes.add(new PerkHitbox(perk, cardX, cardY, cardW, cardH));
        }
    }

    private void drawDetailPanel(GuiGraphicsExtractor g, int x, int y, Player player, ResearchData data, int mouseX, int mouseY) {
        int panelX = x + 14 + LIST_W + 12;
        int panelY = y + HEADER_H + TAB_H + 18;
        int panelH = imageHeight - HEADER_H - TAB_H - SCHEMATIC_H - 42;
        Perk.Branch branch = BRANCHES[selectedBranch];
        Perk perk = selectedPerk();

        g.fill(panelX, panelY, panelX + DETAIL_W, panelY + panelH, COL_PANEL);
        g.fill(panelX, panelY, panelX + DETAIL_W, panelY + 1, branch.getColor());

        g.text(font, "DETAILS", panelX + 10, panelY + 9, branch.getColor(), false);

        if (perk == null) {
            drawBranchSummary(g, panelX, panelY + 30, panelH - 40, data);
            return;
        }

        PerkState state = perkState(perk, player, data);
        int color = stateColor(state, branch.getColor());
        int cy = panelY + 30;
        int commandY = panelY + panelH - COMMAND_H - 8;

        for (String line : wrapByWords(perk.getName(), DETAIL_W - 20, 2)) {
            g.text(font, line, panelX + 10, cy, color, false);
            cy += 11;
        }

        cy += 5;
        g.text(font, "Tier " + perk.getTier() + " / Cost " + perk.getCost() + " RP", panelX + 10, cy, COL_DIM, false);
        cy += 15;

        for (String line : wrapByWords(perk.getDescription(), DETAIL_W - 20, 3)) {
            g.text(font, line, panelX + 10, cy, COL_TEXT, false);
            cy += 11;
        }

        cy += 7;
        g.fill(panelX + 10, cy, panelX + DETAIL_W - 10, cy + 1, COL_ACCENT_DIM);
        cy += 10;

        g.text(font, "STATE", panelX + 10, cy, COL_DIM, false);
        drawSmallPill(g, shortStatus(perk, state, data).toUpperCase(Locale.ROOT),
                panelX + DETAIL_W - 82, cy - 2, 72, color, state == PerkState.READY);
        cy += 12;
        for (String line : wrapByWords(statusText(perk, state, data), DETAIL_W - 20, 2)) {
            g.text(font, line, panelX + 10, cy, color, false);
            cy += 11;
        }
        cy += 6;

        g.text(font, "REQUIREMENT", panelX + 10, cy, COL_DIM, false);
        cy += 12;
        String prereq = prereqText(perk, data);
        for (String line : wrapByWords(prereq, DETAIL_W - 20, 2)) {
            g.text(font, line, panelX + 10, cy, state == PerkState.REQUIRES_PREREQ ? COL_RED : COL_DIM, false);
            cy += 11;
        }

        cy = Math.min(cy + 6, commandY - 34);
        for (String line : wrapByWords(stateHint(perk, state, data), DETAIL_W - 20, 2)) {
            g.text(font, line, panelX + 10, cy, state == PerkState.READY ? COL_YELLOW : COL_DIM, false);
            cy += 11;
        }
        drawUnlockCommand(g, perk, state, data, panelX + 10, commandY, DETAIL_W - 20, mouseX, mouseY);
    }

    private void drawSchematicAnalyzer(GuiGraphicsExtractor g, int x, int y, Player player, ResearchData data, int mouseX, int mouseY) {
        int panelX = x + 14;
        int panelY = y + imageHeight - SCHEMATIC_H - 14;
        int panelW = imageWidth - 28;

        g.fill(panelX, panelY, panelX + panelW, panelY + SCHEMATIC_H, COL_PANEL);
        g.fill(panelX, panelY, panelX + panelW, panelY + 1, COL_ACCENT);
        g.text(font, "SCHEMATIC ANALYSIS", panelX + 8, panelY + 8, COL_ACCENT, false);
        g.text(font, "Fragments decode recipes and produce RP.", panelX + 146, panelY + 8, COL_DIM, false);

        int chipY = panelY + 27;
        int chipW = (panelW - 16 - 4 * 6) / 5;
        int chipX = panelX + 8;
        for (SchematicFragmentItem.SchematicType type : SchematicFragmentItem.SchematicType.values()) {
            int count = countFragments(player, type);
            String key = type.getDisplayName().toLowerCase(Locale.ROOT);
            boolean unlocked = data.hasSchematic(key);
            boolean hovered = contains(mouseX, mouseY, chipX, chipY, chipW, 24);
            int color = type.getHexColor();
            int bg = hovered && count > 0 ? 0xFF1A2E40 : COL_PANEL_DARK;

            g.fill(chipX, chipY, chipX + chipW, chipY + 24, bg);
            g.fill(chipX, chipY, chipX + 2, chipY + 24, count > 0 ? color : COL_LOCK);
            g.outline(chipX, chipY, chipW, 24, count > 0 ? color : 0x44384652);
            g.text(font, fitText(type.getDisplayName() + " x" + count, chipW - 8), chipX + 6, chipY + 4,
                count > 0 ? color : COL_DIM, false);

            String action = count <= 0 ? "NO FRAGMENT" : unlocked ? "ARCHIVE +5 RP" : "DECODE +25 RP";
            g.text(font, fitText(action, chipW - 8), chipX + 6, chipY + 15,
                count > 0 ? COL_YELLOW : COL_LOCK, false);

            fragmentHitboxes.add(new FragmentHitbox(key, count, chipX, chipY, chipW, 24));
            chipX += chipW + 6;
        }
    }

    private void drawBranchSummary(GuiGraphicsExtractor g, int x, int y, int height, ResearchData data) {
        Perk.Branch branch = BRANCHES[selectedBranch];
        List<Perk> perks = sortedPerks(branch);
        int unlocked = unlockedCount(branch, data);
        int ready = readyCount(branch, data);
        int locked = Math.max(0, perks.size() - unlocked - ready);

        g.text(font, fitText(branch.getName(), DETAIL_W - 20), x + 10, y, branch.getColor(), false);
        g.text(font, unlocked + "/" + perks.size() + " online", x + 10, y + 14, COL_TEXT, false);
        g.text(font, ready + " ready / " + locked + " locked", x + 10, y + 28, ready > 0 ? COL_YELLOW : COL_DIM, false);
        int barX = x + 10;
        int barY = y + 45;
        int barW = DETAIL_W - 20;
        g.fill(barX, barY, barX + barW, barY + 5, 0xFF111722);
        if (!perks.isEmpty()) {
            g.fill(barX, barY, barX + Math.max(1, barW * unlocked / perks.size()), barY + 5, branch.getColor());
        }
        int cy = y + 64;
        for (String line : wrapByWords(branch.getDescription(), DETAIL_W - 20, 4)) {
            g.text(font, line, x + 10, cy, COL_DIM, false);
            cy += 11;
        }
        cy = Math.min(cy + 8, y + height - 36);
        g.fill(x + 10, cy, x + DETAIL_W - 10, cy + 1, COL_ACCENT_DIM);
        cy += 10;
        g.text(font, "Select a node to inspect.", x + 10, cy, COL_TEXT, false);
        g.text(font, "Unlocks run from DETAILS.", x + 10, cy + 12, COL_DIM, false);
    }

    private Perk selectedPerk() {
        if (selectedPerkId != null) {
            Perk selected = PerkRegistry.get(selectedPerkId);
            if (selected != null && selected.getBranch() == BRANCHES[selectedBranch]) {
                return selected;
            }
        }
        return null;
    }

    private void validateSelectedPerk() {
        if (selectedPerkId == null) {
            return;
        }
        Perk selected = PerkRegistry.get(selectedPerkId);
        if (selected == null || selected.getBranch() != BRANCHES[selectedBranch]) {
            selectedPerkId = null;
        }
    }

    private void drawUnlockCommand(GuiGraphicsExtractor g, Perk perk, PerkState state, ResearchData data,
                                   int x, int y, int w, int mouseX, int mouseY) {
        boolean enabled = state == PerkState.READY;
        boolean hovered = enabled && contains(mouseX, mouseY, x, y, w, COMMAND_H);
        int color = enabled ? COL_YELLOW : stateColor(state, BRANCHES[selectedBranch].getColor());
        int bg = enabled ? (hovered ? 0xFF2E2A16 : 0xFF231F12) : 0xFF10151B;
        g.fill(x, y, x + w, y + COMMAND_H, bg);
        g.outline(x, y, w, COMMAND_H, enabled ? color : 0x55384652);
        g.fill(x, y + COMMAND_H - 2, x + w, y + COMMAND_H, color);
        g.centeredText(font, fitText(unlockLabel(perk, state, data), w - 10), x + w / 2, y + 8,
                enabled ? COL_TEXT : COL_DIM);
        unlockHitboxes.add(new UnlockHitbox(perk, enabled, x, y, w, COMMAND_H));
    }

    private void drawSmallPill(GuiGraphicsExtractor g, String label, int x, int y, int w, int color, boolean hot) {
        g.fill(x, y, x + w, y + 13, hot ? 0xFF2E2813 : 0xFF10151B);
        g.outline(x, y, w, 13, hot ? color : 0x55384652);
        g.centeredText(font, fitText(label, w - 8), x + w / 2, y + 3, hot ? COL_TEXT : color);
    }

    private String unlockLabel(Perk perk, PerkState state, ResearchData data) {
        return switch (state) {
            case UNLOCKED -> "ONLINE";
            case READY -> "UNLOCK";
            case NEED_POINTS -> "NEED " + (perk.getCost() - data.getPoints()) + " RP";
            case REQUIRES_PREREQ -> "REQUIRES " + missingPrereqName(perk, data);
        };
    }

    private String branchProgressLabel(Perk.Branch branch, ResearchData data) {
        List<Perk> perks = sortedPerks(branch);
        return unlockedCount(branch, data) + "/" + perks.size() + " online";
    }

    private int unlockedCount(Perk.Branch branch, ResearchData data) {
        int unlocked = 0;
        for (Perk perk : sortedPerks(branch)) {
            if (data.hasPerk(perk.getId())) {
                unlocked++;
            }
        }
        return unlocked;
    }

    private int readyCount(Perk.Branch branch, ResearchData data) {
        int ready = 0;
        for (Perk perk : sortedPerks(branch)) {
            if (!data.hasPerk(perk.getId()) && prerequisitesMet(perk, data) && data.hasPoints(perk.getCost())) {
                ready++;
            }
        }
        return ready;
    }

    private boolean prerequisitesMet(Perk perk, ResearchData data) {
        for (String id : perk.getPrerequisites()) {
            if (!data.hasPerk(id)) {
                return false;
            }
        }
        return true;
    }

    private List<PerkChain> branchChains(Perk.Branch branch) {
        Map<String, List<Perk>> chains = new LinkedHashMap<>();
        for (Perk perk : sortedPerks(branch)) {
            String root = rootId(perk);
            chains.computeIfAbsent(root, ignored -> new ArrayList<>()).add(perk);
        }

        List<PerkChain> result = new ArrayList<>();
        for (Map.Entry<String, List<Perk>> entry : chains.entrySet()) {
            entry.getValue().sort(Comparator.comparingInt(Perk::getTier).thenComparing(Perk::getName));
            Perk root = PerkRegistry.get(entry.getKey());
            String label = root != null ? chainLabel(root.getName()) : chainLabel(entry.getValue().get(0).getName());
            result.add(new PerkChain(label, entry.getValue()));
        }
        result.sort(Comparator.comparingInt(PerkChain::minTier).thenComparing(chain -> chain.label));
        return result;
    }

    private String rootId(Perk perk) {
        Perk current = perk;
        while (current.getPrerequisites().length > 0) {
            Perk parent = PerkRegistry.get(current.getPrerequisites()[0]);
            if (parent == null || parent.getBranch() != perk.getBranch()) {
                break;
            }
            current = parent;
        }
        return current.getId();
    }

    private String chainLabel(String name) {
        return name
            .replace(" I", "")
            .replace(" II", "")
            .replace(" III", "")
            .replace("Mastery", "Mastery")
            .trim();
    }

    private List<Perk> sortedPerks(Perk.Branch branch) {
        List<Perk> perks = new ArrayList<>(PerkRegistry.getByBranch(branch).values());
        perks.sort(Comparator
            .comparing(Perk::getName)
            .thenComparingInt(Perk::getTier));
        return perks;
    }

    private int countFragments(Player player, SchematicFragmentItem.SchematicType type) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof SchematicFragmentItem fragment && fragment.getType() == type) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private PerkState perkState(Perk perk, Player player, ResearchData data) {
        if (data.hasPerk(perk.getId())) {
            return PerkState.UNLOCKED;
        }
        if (!perk.hasPrerequisites(player)) {
            return PerkState.REQUIRES_PREREQ;
        }
        if (!data.hasPoints(perk.getCost())) {
            return PerkState.NEED_POINTS;
        }
        return PerkState.READY;
    }

    private int stateColor(PerkState state, int branchColor) {
        return switch (state) {
            case UNLOCKED -> branchColor;
            case READY -> COL_YELLOW;
            case NEED_POINTS -> COL_RED;
            case REQUIRES_PREREQ -> COL_LOCK;
        };
    }

    private String shortStatus(Perk perk, PerkState state, ResearchData data) {
        return switch (state) {
            case UNLOCKED -> "ONLINE";
            case READY -> "READY";
            case NEED_POINTS -> "Need " + (perk.getCost() - data.getPoints()) + " RP";
            case REQUIRES_PREREQ -> "Prereq";
        };
    }

    private String statusText(Perk perk, PerkState state, ResearchData data) {
        return switch (state) {
            case UNLOCKED -> "Unlocked";
            case READY -> "Ready";
            case NEED_POINTS -> "Need " + (perk.getCost() - data.getPoints()) + " RP";
            case REQUIRES_PREREQ -> "Requires " + missingPrereqName(perk, data);
        };
    }

    private String stateHint(Perk perk, PerkState state, ResearchData data) {
        return switch (state) {
            case UNLOCKED -> "This research is active.";
            case READY -> "Press UNLOCK to spend " + perk.getCost() + " RP.";
            case NEED_POINTS -> "Earn RP from POIs, missions, scanner scans, relay stations, and schematic analysis.";
            case REQUIRES_PREREQ -> "Unlock the previous node in this lane first.";
        };
    }

    private String prereqText(Perk perk, ResearchData data) {
        String[] prereqs = perk.getPrerequisites();
        if (prereqs.length == 0) {
            return "Prerequisite: none";
        }

        List<String> names = new ArrayList<>();
        for (String id : prereqs) {
            Perk prereq = PerkRegistry.get(id);
            String name = prereq != null ? prereq.getName() : id;
            names.add((data.hasPerk(id) ? "OK " : "Missing ") + name);
        }
        return "Requires: " + String.join(", ", names);
    }

    private String missingPrereqName(Perk perk, ResearchData data) {
        for (String id : perk.getPrerequisites()) {
            if (!data.hasPerk(id)) {
                Perk prereq = PerkRegistry.get(id);
                return prereq != null ? prereq.getName() : id;
            }
        }
        return "Previous Perk";
    }

    private boolean contains(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private String fitText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int suffixW = font.width(suffix);
        if (maxWidth <= suffixW) {
            return font.plainSubstrByWidth(text, maxWidth);
        }
        return font.plainSubstrByWidth(text, maxWidth - suffixW) + suffix;
    }

    private List<String> wrapByWords(String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String current = "";

        for (String word : text.split(" ")) {
            String next = current.isEmpty() ? word : current + " " + word;
            if (font.width(next) <= maxWidth) {
                current = next;
                continue;
            }

            if (!current.isEmpty()) {
                lines.add(current);
                current = word;
            } else {
                String chunk = font.plainSubstrByWidth(word, maxWidth);
                lines.add(chunk);
                current = word.substring(Math.min(chunk.length(), word.length()));
            }

            if (lines.size() == maxLines) {
                return trimLastLine(lines, maxWidth);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current);
        }
        if (lines.size() > maxLines) {
            return trimLastLine(lines.subList(0, maxLines), maxWidth);
        }
        return lines;
    }

    private List<String> trimLastLine(List<String> source, int maxWidth) {
        List<String> lines = new ArrayList<>(source);
        if (lines.isEmpty()) {
            return lines;
        }

        int last = lines.size() - 1;
        String line = lines.get(last);
        if (!line.endsWith("...")) {
            lines.set(last, fitText(line + "...", maxWidth));
        }
        return lines;
    }

    private enum PerkState {
        UNLOCKED,
        READY,
        NEED_POINTS,
        REQUIRES_PREREQ
    }

    private record PerkChain(String label, List<Perk> perks) {
        Perk perkAtTier(int tier) {
            for (Perk perk : perks) {
                if (perk.getTier() == tier) {
                    return perk;
                }
            }
            return null;
        }

        int minTier() {
            int min = 99;
            for (Perk perk : perks) {
                min = Math.min(min, perk.getTier());
            }
            return min;
        }

        int unlockedCount(ResearchData data) {
            int count = 0;
            for (Perk perk : perks) {
                if (data.hasPerk(perk.getId())) {
                    count++;
                }
            }
            return count;
        }
    }

    private record PerkHitbox(Perk perk, int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private record TabHitbox(int branchIndex, int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private record FragmentHitbox(String typeKey, int count, int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private record UnlockHitbox(Perk perk, boolean enabled, int x, int y, int width, int height) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
