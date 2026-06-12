package com.knoxhack.echomultiblockcore.client;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoThemeToken;
import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echomultiblockcore.api.MultiblockState;
import com.knoxhack.echomultiblockcore.menu.MultiblockControllerMenu;
import com.knoxhack.echomultiblockcore.network.AutomationRecipeMetadataPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class MultiblockControllerScreen extends AbstractContainerScreen<MultiblockControllerMenu> {
    private static final int RECIPE_PAGE_SIZE = 4;
    private static final int PANEL = 0xEE071018;
    private static final int CYAN = 0xFF66E8FF;
    private static final int GREEN = 0xFF8AF6B6;
    private static final int AMBER = 0xFFFFD166;
    private static final int RED = 0xFFFF8FA3;
    private int recipePage;

    public MultiblockControllerScreen(MultiblockControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MultiblockControllerMenu.GUI_WIDTH, MultiblockControllerMenu.GUI_HEIGHT);
        this.titleLabelX = 16;
        this.titleLabelY = 12;
        this.inventoryLabelY = 10000;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, CYAN);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 42, 0xDD101D24);
        drawBars(graphics, x, y, mouseX, mouseY);
        drawButtons(graphics, x, y, mouseX, mouseY);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        drawHoverHelp(graphics, x, y, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.literal("ECHO MULTIBLOCK // CONTROLLER"), titleLabelX, titleLabelY, CYAN, true);
        graphics.text(font, Component.literal("State " + stateName()
                + " | Integrity " + menu.integrity() + "%"
                + " | Completion " + menu.completion() + "%"), 16, 30, stateColor(), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = leftPos;
        int y = topPos;
        List<RecipeCandidate> allowed = allowedRecipes();
        int pageCount = recipePageCount(allowed.size());
        if (localClick(event, x + 278, y + 146, 14, 12, recipePage > 0)) {
            recipePage--;
            return true;
        }
        if (localClick(event, x + 302, y + 146, 14, 12, recipePage < pageCount - 1)) {
            recipePage++;
            return true;
        }
        for (RecipeRow row : visibleRecipeRows()) {
            int rowY = y + 160 + row.visibleIndex() * 14;
            if (click(event, x + 72, rowY, 244, 12,
                    MultiblockControllerMenu.BUTTON_RECIPE_BASE + row.globalIndex(), true)) {
                return true;
            }
        }
        if (click(event, x + 16, y + 226, 72, 18, MultiblockControllerMenu.BUTTON_VALIDATE, true)) return true;
        if (click(event, x + 96, y + 226, 72, 18, MultiblockControllerMenu.BUTTON_START, true)) return true;
        if (click(event, x + 176, y + 226, 60, 18, MultiblockControllerMenu.BUTTON_RETRY, menu.blocked())) return true;
        if (click(event, x + 244, y + 226, 72, 18, MultiblockControllerMenu.BUTTON_AUTOBUILD, true)) return true;
        if (click(event, x + 16, y + 250, 72, 18, MultiblockControllerMenu.BUTTON_PAUSE, menu.queueSize() > 0)) return true;
        if (click(event, x + 96, y + 250, 72, 18, MultiblockControllerMenu.BUTTON_RESUME, menu.queueSize() > 0)) return true;
        if (click(event, x + 176, y + 250, 60, 18, MultiblockControllerMenu.BUTTON_CLEAR, menu.queueSize() > 0)) return true;
        if (click(event, x + 244, y + 250, 72, 18, MultiblockControllerMenu.BUTTON_REPAIR, menu.integrity() < 100)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    private void drawBars(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        drawMetric(graphics, x + 16, y + 58, 138, "Integrity", menu.integrity(), menu.integrity() >= 70 ? GREEN : AMBER);
        drawMetric(graphics, x + 176, y + 58, 138, "Completion", menu.completion(), menu.completion() >= 100 ? GREEN : CYAN);
        graphics.text(font, Component.literal("Robots " + menu.robots() + " | Queue " + menu.queueSize()
                + "/" + Math.max(1, menu.queueCapacity()) + " | Active " + menu.activeTasks()
                + " | Blocked " + menu.blockedTasks()), x + 16, y + 96, 0xFFD8F6FF, false);
        graphics.text(font, Component.literal("Progression T" + menu.progressionTier()
                + " | Featured " + menu.featuredRecipes()), x + 220, y + 96,
                menu.progressionTier() > 0 ? GREEN : 0xFF8CA7B5, false);
        graphics.text(font, Component.literal("Capabilities " + (menu.capabilityOk() ? "READY" : "BLOCKED")
                + " | Needs " + menu.capabilityDiagnostics()
                + " | Damage " + menu.damageGroups()), x + 16, y + 112,
                menu.capabilityOk() ? GREEN : RED, false);
        graphics.text(font, Component.literal("Upgrades " + menu.upgrades()
                + "/" + Math.max(menu.upgrades(), menu.upgradeSlots())
                + " | Repair actions " + menu.repairActions()), x + 220, y + 112,
                menu.repairActions() > 0 ? AMBER : 0xFF8CA7B5, false);
        graphics.text(font, Component.literal(menu.blocked()
                ? "Task status BLOCKED"
                : "Task status READY"), x + 16, y + 130,
                menu.blocked() ? AMBER : 0xFF8CA7B5, false);
        drawRecipeSelector(graphics, x, y, mouseX, mouseY);
    }

    private void drawMetric(GuiGraphicsExtractor graphics, int x, int y, int width, String label, int value, int color) {
        int fill = Math.max(0, Math.min(width, Math.round(width * Math.max(0, Math.min(100, value)) / 100.0F)));
        graphics.text(font, Component.literal(label + " " + value + "%"), x, y - 12, color, false);
        EchoCyberGlassUi.meter(graphics, x, y, width, 8, fill, color);
    }

    private void drawButtons(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        drawButton(graphics, x + 16, y + 226, 72, 18, "VALIDATE", mouseX, mouseY, true);
        drawButton(graphics, x + 96, y + 226, 72, 18, "START", mouseX, mouseY, true);
        drawButton(graphics, x + 176, y + 226, 60, 18, "RETRY", mouseX, mouseY, menu.blocked());
        drawButton(graphics, x + 244, y + 226, 72, 18, "BUILD", mouseX, mouseY, true);
        drawButton(graphics, x + 16, y + 250, 72, 18, "PAUSE", mouseX, mouseY, menu.queueSize() > 0);
        drawButton(graphics, x + 96, y + 250, 72, 18, "RESUME", mouseX, mouseY, menu.queueSize() > 0);
        drawButton(graphics, x + 176, y + 250, 60, 18, "CLEAR", mouseX, mouseY, menu.queueSize() > 0);
        drawButton(graphics, x + 244, y + 250, 72, 18, "REPAIR", mouseX, mouseY, menu.integrity() < 100);
    }

    private void drawRecipeSelector(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        List<RecipeCandidate> allowed = allowedRecipes();
        int pageCount = recipePageCount(allowed.size());
        int page = clampedRecipePage(allowed.size());
        graphics.text(font, Component.literal("Recipes " + (allowed.isEmpty() ? 0 : page + 1) + "/" + pageCount
                + " (" + allowed.size() + ")"), x + 16, y + 146, CYAN, false);
        drawButton(graphics, x + 278, y + 146, 14, 12, "<", mouseX, mouseY, page > 0);
        drawButton(graphics, x + 302, y + 146, 14, 12, ">", mouseX, mouseY, page < pageCount - 1);
        List<RecipeRow> rows = visibleRecipeRows(allowed);
        if (rows.isEmpty()) {
            graphics.text(font, Component.literal("No synced recipes"), x + 80, y + 160, 0xFF66777D, false);
            return;
        }
        for (RecipeRow row : rows) {
            int rowY = y + 160 + row.visibleIndex() * 14;
            AutomationRecipeMetadataPacket.Entry entry = row.entry();
            String label = entry.displayName();
            String meta = entry.requiredWorkcell() + " " + Math.max(1, entry.durationTicks() / 20) + "s";
            boolean hover = inside(mouseX, mouseY, x + 72, rowY, 244, 12);
            graphics.fill(x + 72, rowY, x + 316, rowY + 12, hover ? 0xAA122530 : 0x6612202A);
            EchoCyberGlassUi.frame(graphics, x + 72, rowY, 244, 12, hover ? CYAN : 0xFF2E4B58);
            graphics.text(font, Component.literal(truncate(label, 26)), x + 76, rowY + 2, 0xFFE9FBFF, false);
            graphics.text(font, Component.literal(truncate(meta, 18)), x + 218, rowY + 2, 0xFF8CA7B5, false);
        }
    }

    private List<RecipeRow> visibleRecipeRows() {
        return visibleRecipeRows(allowedRecipes());
    }

    private List<RecipeRow> visibleRecipeRows(List<RecipeCandidate> allowed) {
        int start = clampedRecipePage(allowed.size()) * RECIPE_PAGE_SIZE;
        List<RecipeRow> rows = new ArrayList<>();
        for (int i = start; i < allowed.size() && rows.size() < RECIPE_PAGE_SIZE; i++) {
            RecipeCandidate candidate = allowed.get(i);
            rows.add(new RecipeRow(candidate.globalIndex(), rows.size(), candidate.entry()));
        }
        return rows;
    }

    private List<RecipeCandidate> allowedRecipes() {
        List<AutomationRecipeMetadataPacket.Entry> entries = MultiblockClientPackets.recipeMetadataEntries();
        Identifier definitionId = menu.definitionId();
        List<RecipeCandidate> rows = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            AutomationRecipeMetadataPacket.Entry entry = entries.get(i);
            if (entry.allows(definitionId)) {
                rows.add(new RecipeCandidate(i, entry));
            }
        }
        return rows;
    }

    private int clampedRecipePage(int recipeCount) {
        int maxPage = Math.max(0, (Math.max(1, recipeCount) - 1) / RECIPE_PAGE_SIZE);
        if (recipePage > maxPage) {
            recipePage = maxPage;
        } else if (recipePage < 0) {
            recipePage = 0;
        }
        return recipePage;
    }

    private int recipePageCount(int recipeCount) {
        return Math.max(1, (Math.max(0, recipeCount) + RECIPE_PAGE_SIZE - 1) / RECIPE_PAGE_SIZE);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + ".";
    }

    private boolean click(MouseButtonEvent event, int x, int y, int w, int h, int id, boolean enabled) {
        if (!enabled || event.button() != 0 || !inside(event.x(), event.y(), x, y, w, h)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
        return true;
    }

    private boolean localClick(MouseButtonEvent event, int x, int y, int w, int h, boolean enabled) {
        return enabled && event.button() == 0 && inside(event.x(), event.y(), x, y, w, h);
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String label, int mouseX, int mouseY, boolean enabled) {
        EchoCyberGlassUi.button(graphics, font, x, y, w, h, label, inside(mouseX, mouseY, x, y, w, h), enabled, CYAN);
    }

    private void drawHoverHelp(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        List<String> lines = null;
        if (inside(mouseX, mouseY, x + 16, y + 226, 72, 18)) {
            lines = List.of("Validate", "Runs server validation and refreshes diagnostics.");
        } else if (inside(mouseX, mouseY, x + 96, y + 226, 72, 18)) {
            lines = List.of("Start", "Queues the deterministic default eligible recipe.");
        } else if (inside(mouseX, mouseY, x + 176, y + 226, 60, 18)) {
            lines = List.of("Retry", "Retries blocked work after fixing inputs, output, tooling, or power.");
        } else if (inside(mouseX, mouseY, x + 244, y + 226, 72, 18)) {
            lines = List.of("Build Assist", "Uses linked input crates to place valid missing cells.");
        } else if (inside(mouseX, mouseY, x + 16, y + 250, 72, 18)) {
            lines = List.of("Pause Queue", "Pauses waiting, blocked, and active automation tasks.");
        } else if (inside(mouseX, mouseY, x + 96, y + 250, 72, 18)) {
            lines = List.of("Resume Queue", "Returns paused tasks to the waiting queue.");
        } else if (inside(mouseX, mouseY, x + 176, y + 250, 60, 18)) {
            lines = List.of("Clear Queue", "Cancels all queued controller tasks.");
        } else if (inside(mouseX, mouseY, x + 244, y + 250, 72, 18)) {
            lines = List.of("Repair", "Queues a repair task when integrity is below full.");
        } else {
            for (RecipeRow row : visibleRecipeRows()) {
                int rowY = y + 160 + row.visibleIndex() * 14;
                if (inside(mouseX, mouseY, x + 72, rowY, 244, 12)) {
                    lines = tooltipForRecipe(row.entry());
                    break;
                }
            }
        }
        if (lines != null) {
            drawTooltipPanel(graphics, mouseX, mouseY, lines);
        }
    }

    private List<String> tooltipForRecipe(AutomationRecipeMetadataPacket.Entry entry) {
        return List.of(
                entry.displayName(),
                "Requires " + entry.requiredWorkcell() + (entry.tools().isBlank() ? "" : " / " + entry.tools()),
                "Input: " + truncate(entry.inputs(), 52),
                "Output: " + truncate(entry.outputs(), 52),
                entry.effects().isBlank() ? "No side effects declared" : "Effects: " + truncate(entry.effects(), 52));
    }

    private void drawTooltipPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, List<String> lines) {
        EchoCyberGlassUi.tooltipPanel(graphics, font, mouseX, mouseY, leftPos, topPos, imageWidth, imageHeight, lines);
    }

    private static int theme(String token, int fallback) {
        try {
            return EchoCoreServices.themeService().resolveColor(token, EchoThemeToken.resolveDefault(token, fallback));
        } catch (RuntimeException exception) {
            return EchoThemeToken.resolveDefault(token, fallback);
        }
    }

    private boolean inside(double px, double py, int x, int y, int w, int h) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    private String stateName() {
        MultiblockState[] values = MultiblockState.values();
        int ordinal = menu.stateOrdinal();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal].name() : "UNKNOWN";
    }

    private int stateColor() {
        return switch (stateName()) {
            case "FORMED", "ACTIVE" -> GREEN;
            case "DAMAGED", "JAMMED", "OVERLOADED" -> AMBER;
            case "OFFLINE" -> RED;
            default -> CYAN;
        };
    }

    private record RecipeCandidate(int globalIndex, AutomationRecipeMetadataPacket.Entry entry) {
    }

    private record RecipeRow(int globalIndex, int visibleIndex, AutomationRecipeMetadataPacket.Entry entry) {
    }
}
