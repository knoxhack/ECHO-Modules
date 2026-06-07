package com.knoxhack.echoindex.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echoindex.service.IndexRecipeProviderStats;
import com.knoxhack.echoindex.service.IndexRecipeQueryClientState;
import com.knoxhack.echoindex.service.IndexRecipeSnapshot;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echoindex.network.IndexActionPacket;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class IndexDiagnosticsScreen extends Screen {
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int scroll;
    private boolean warningsOnly;
    private final List<Hitbox> hitboxes = new ArrayList<>();

    public IndexDiagnosticsScreen() {
        super(Component.translatable("screen.echoindex.diagnostics"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        IndexRecipeUi.refreshTheme();
        hitboxes.clear();
        layout();
        Font font = Minecraft.getInstance().font;
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshot(Minecraft.getInstance().player);

        IndexThemeStyle.backdrop(graphics, width, height);
        IndexThemeStyle.panel(graphics, panelX, panelY, panelW, panelH, IndexRecipeUi.BG, IndexRecipeUi.CYAN);
        IndexThemeStyle.icon(graphics, panelX + 12, panelY + 9, 16);
        graphics.text(font, "ECHO: INDEX | DIAGNOSTICS", panelX + 32, panelY + 12, IndexRecipeUi.CYAN, true);
        graphics.text(font, "ESC", panelX + panelW - 36, panelY + panelH - 18, IndexRecipeUi.MUTED, false);
        chip(graphics, font, panelX + panelW - 140, panelY + 10, 48, "All", !warningsOnly, mouseX, mouseY,
                () -> {
                    warningsOnly = false;
                    scroll = 0;
                });
        chip(graphics, font, panelX + panelW - 88, panelY + 10, 54, "Warn", warningsOnly, mouseX, mouseY,
                () -> {
                    warningsOnly = true;
                    scroll = 0;
                });
        chip(graphics, font, panelX + panelW - 30, panelY + 10, 24, "R", false, mouseX, mouseY,
                () -> {
                    IndexService.INSTANCE.rebuildRecipes(Minecraft.getInstance().player, "diagnostics refresh button");
                    EchoNetClientActions.sendServerboundAction(
                            new IndexActionPacket(IndexActionPacket.Action.REQUEST_SYNC, null));
                    scroll = 0;
                });

        int x = panelX + 14;
        int y = panelY + 42 - scroll;
        int w = panelW - 28;
        graphics.enableScissor(panelX + 8, panelY + 38, panelX + panelW - 8, panelY + panelH - 28);
        IndexThemeStyle.card(graphics, x, y, w, 92, false);
        graphics.text(font, "Providers: " + snapshot.providerStats().size()
                + "  Categories: " + snapshot.categories().size()
                + "  Recipes: " + snapshot.recipes().size()
                + "  Source Cards: " + snapshot.sourceCardCount()
                + "  Facts: " + snapshot.sourceFactCount()
                + "  Warnings: " + snapshot.warnings().size(), x + 10, y + 10, IndexRecipeUi.TEXT, false);
        graphics.text(font, "Snapshot #" + snapshot.generation()
                + "  Age: " + snapshot.ageSeconds() + "s"
                + "  Reason: " + snapshot.buildReason(),
                x + 10, y + 24, IndexRecipeUi.MUTED, false);
        graphics.text(font, "Raw " + snapshot.rawRecipeCount()
                + " / adapted " + snapshot.adaptedRecipeCount()
                + " / source providers " + IndexService.INSTANCE.sourceProviderCount()
                + " / provider facts " + IndexService.INSTANCE.sourceFacts(Minecraft.getInstance().player).size()
                + " / skipped " + snapshot.skippedRecipeCount()
                + "  View: " + (warningsOnly ? "providers with warnings" : "all providers"),
                x + 10, y + 38, IndexRecipeUi.MUTED, false);
        var queryHealth = IndexRecipeQueryClientState.health();
        String queried = IndexRecipeQueryClientState.lastQueriedItem() == null
                ? "none"
                : IndexRecipeQueryClientState.lastQueriedItem().toString();
        graphics.text(font, "Client query: " + queried
                + " / server gen " + queryHealth.generation()
                + " / raw " + queryHealth.rawRecipeCount()
                + " / adapted " + queryHealth.adaptedRecipeCount()
                + " / uses " + queryHealth.usageItemCount(),
                x + 10, y + 52, IndexRecipeUi.MUTED, false);
        String hoveredRecipe = IndexRecipeUi.lastHoveredRecipeId() == null
                ? "none"
                : IndexRecipeUi.lastHoveredRecipeId().toString();
        String hoveredItem = IndexRecipeUi.lastHoveredItemId() == null
                ? "none"
                : IndexRecipeUi.lastHoveredItemId().toString();
        String uiQueried = IndexRecipeUi.lastQueriedItemId() == null
                ? "none"
                : IndexRecipeUi.lastQueriedItemId().toString();
        graphics.text(font, "UI: " + IndexRecipeUi.lastViewMode().label()
                + " card " + IndexRecipeUi.selectedCardLabel()
                + " / cache " + IndexRecipeUi.lastQueryCacheState()
                + " / query " + uiQueried
                + " / hover " + hoveredRecipe
                + " / item " + hoveredItem,
                x + 10, y + 66, IndexRecipeUi.MUTED, false);
        graphics.text(font, "V13 " + IndexRecipeTraceState.diagnosticsLine(),
                x + 10, y + 80, IndexRecipeUi.MUTED, false);
        String queryWarning = IndexRecipeQueryClientState.lastQueryWarning();
        y += 110;
        if (!queryWarning.isBlank()) {
            IndexThemeStyle.card(graphics, x, y, w, 28, true);
            graphics.textWithWordWrap(font, Component.literal("Last query warning: " + queryWarning),
                    x + 8, y + 8, w - 16, IndexRecipeUi.WARN);
            y += 34;
        }

        List<IndexRecipeProviderStats> providers = snapshot.providerStats().stream()
                .filter(stats -> !warningsOnly || stats.hasWarning())
                .toList();
        if (providers.isEmpty()) {
            IndexThemeStyle.card(graphics, x, y, w, 32, false);
            graphics.text(font, warningsOnly ? "No provider warnings in the current snapshot." : "No providers reported.",
                    x + 10, y + 11, IndexRecipeUi.MUTED, false);
            y += 40;
        }
        for (IndexRecipeProviderStats stats : providers) {
            String warning = firstWarningFor(snapshot, stats);
            int rowH = stats.lastError().isBlank() && warning.isBlank() ? 36 : 64;
            IndexThemeStyle.card(graphics, x, y, w, rowH, stats.hasWarning());
            EchoCyberGlassUi.frame(graphics, x, y, w, rowH, stats.hasWarning() ? 0x88FFD166 : 0x4438DFF4);
            graphics.text(font, IndexRecipeUi.trim(font, stats.providerId().toString(), w - 22),
                    x + 10, y + 7, stats.hasWarning() ? IndexRecipeUi.WARN : IndexRecipeUi.CYAN, false);
            graphics.text(font, "cat " + stats.categoryCount()
                    + " / raw " + stats.rawRecipeCount()
                    + " / adapted " + stats.adaptedRecipeCount()
                    + " / source facts " + stats.sourceFactCount()
                    + " / cards " + stats.sourceCardCount()
                    + " / skipped " + stats.skippedRecipeCount(), x + 10, y + 20, IndexRecipeUi.MUTED, false);
            if (!stats.lastError().isBlank()) {
                graphics.text(font, IndexRecipeUi.trim(font, stats.lastError(), w - 22),
                        x + 10, y + 34, IndexRecipeUi.WARN, false);
            } else if (!warning.isBlank()) {
                graphics.text(font, IndexRecipeUi.trim(font, warning, w - 22),
                        x + 10, y + 34, IndexRecipeUi.WARN, false);
            }
            y += rowH + 6;
        }

        if (!snapshot.warnings().isEmpty()) {
            graphics.text(font, "WARNINGS", x, y + 4, IndexRecipeUi.WARN, true);
            y += 20;
            for (String warning : snapshot.warnings().stream().limit(16).toList()) {
                IndexThemeStyle.card(graphics, x, y, w, 24, true);
                graphics.textWithWordWrap(font, Component.literal(warning), x + 8, y + 7, w - 16, IndexRecipeUi.WARN);
                y += 28;
            }
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (Hitbox hitbox : List.copyOf(hitboxes)) {
            if (inside(event.x(), event.y(), hitbox.x(), hitbox.y(), hitbox.w(), hitbox.h())) {
                hitbox.action().run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = IndexRecipeUi.clamp(scroll - (int) Math.round(scrollY * 18.0D), 0, 420);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_UP) {
            scroll = Math.max(0, scroll - 20);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DOWN) {
            scroll += 20;
            return true;
        }
        return super.keyPressed(event);
    }

    private void layout() {
        panelW = Math.min(560, Math.max(340, width - 44));
        panelH = Math.min(340, Math.max(240, height - 36));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    private static String firstWarningFor(IndexRecipeSnapshot snapshot, IndexRecipeProviderStats stats) {
        String providerId = stats.providerId().toString();
        return snapshot.warnings().stream()
                .filter(warning -> warning.contains(providerId))
                .findFirst()
                .orElse("");
    }

    private void chip(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, String label,
            boolean active, int mouseX, int mouseY, Runnable action) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 16);
        IndexThemeStyle.chip(graphics, font, x, y, w, 16, label, active, hover);
        hitboxes.add(new Hitbox(x, y, w, 16, action));
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseY >= y && mouseX < x + w && mouseY < y + h;
    }

    private record Hitbox(int x, int y, int w, int h, Runnable action) {
    }
}
