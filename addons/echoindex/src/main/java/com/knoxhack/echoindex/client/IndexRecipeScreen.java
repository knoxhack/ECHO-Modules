package com.knoxhack.echoindex.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echoindex.Config;
import com.knoxhack.echoindex.EchoIndexClient;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoindex.network.IndexActionPacket;
import com.knoxhack.echoindex.service.ClientIndexState;
import com.knoxhack.echoindex.service.IndexRecipePlan;
import com.knoxhack.echoindex.service.IndexRecipePlanner;
import com.knoxhack.echoindex.service.IndexService;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class IndexRecipeScreen extends Screen {
    private static int BG = IndexRecipeUi.BG;
    private static int PANEL = IndexRecipeUi.PANEL;
    private static int ROW = IndexRecipeUi.ROW;
    private static int CYAN = IndexRecipeUi.CYAN;
    private static int TEXT = IndexRecipeUi.TEXT;
    private static int MUTED = IndexRecipeUi.MUTED;
    private static int WARN = IndexRecipeUi.WARN;
    private static final int HEADER_HEIGHT = 34;

    private final ItemStack focusStack;
    private Mode mode;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int selected;
    private int firstVisible;
    private Identifier categoryFilter;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private long searchCursorBlink;
    private final List<IndexRecipeUi.SlotHit> slotHits = new ArrayList<>();

    public IndexRecipeScreen(ItemStack focusStack, Mode mode) {
        super(Component.translatable("screen.echoindex.recipes"));
        this.focusStack = focusStack == null ? ItemStack.EMPTY : focusStack.copy();
        this.mode = mode == null ? Mode.RECIPES : mode;
    }

    @Override
    protected void init() {
        EchoNetClientActions.sendServerboundAction(new IndexActionPacket(IndexActionPacket.Action.REQUEST_SYNC, null));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        refreshThemeAliases();
        slotHits.clear();
        layout();
        IndexThemeStyle.backdrop(graphics, width, height);
        drawScreenChrome(graphics);

        Font font = Minecraft.getInstance().font;
        List<IndexRecipeView> recipes = recipes(focusStack.getItem());
        selected = clamp(selected, 0, Math.max(0, recipes.size() - 1));
        firstVisible = clamp(firstVisible, 0, Math.max(0, recipes.size() - visibleRows()));
        if (selected < firstVisible) {
            firstVisible = selected;
        } else if (selected >= firstVisible + visibleRows()) {
            firstVisible = Math.max(0, selected - visibleRows() + 1);
        }
        IndexThemeStyle.icon(graphics, panelX + 12, panelY + 10, 16);
        graphics.text(font, titleLine(), panelX + 32, panelY + 11, CYAN, true);
        graphics.text(font, focusStack.getHoverName(), panelX + 32, panelY + 23, TEXT, false);
        graphics.item(focusStack, panelX + panelW - 34, panelY + 10);

        drawModeButton(graphics, font, panelX + 14, panelY + 42, Mode.RECIPES, mouseX, mouseY);
        drawModeButton(graphics, font, panelX + 98, panelY + 42, Mode.USES, mouseX, mouseY);
        drawModeButton(graphics, font, panelX + 182, panelY + 42, Mode.SOURCES, mouseX, mouseY);
        drawSearchBar(graphics, font, mouseX, mouseY);
        drawCategoryChips(graphics, font, mouseX, mouseY);
        drawCloseHint(graphics, font);
        drawShortcutHints(graphics, font);

        if (recipes.isEmpty()) {
            drawEmpty(graphics, font);
            return;
        }
        IndexRecipeUi.recordCardSelection(mode.viewMode(), selected, recipes.size());
        drawRecipeList(graphics, font, recipes, mouseX, mouseY);
        drawRecipeDetails(graphics, font, recipes.get(selected), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (EchoIndexClient.nativeLoaderClientActiveForScreens()) {
            return EchoIndexClient.dispatchNativeRecipeScreenMouse(
                    this,
                    "click",
                    event.x(),
                    event.y(),
                    event.button(),
                    event.modifiers(),
                    doubleClick);
        }
        return handleNativeRouteMouse("click", event.x(), event.y(), event.button(), event.modifiers(), doubleClick)
                || super.mouseClicked(event, doubleClick);
    }

    public boolean handleNativeRouteMouse(
            String phase,
            double mouseX,
            double mouseY,
            int button,
            int modifiers,
            boolean doubleClick
    ) {
        if (!"click".equals(phase)) {
            return false;
        }
        layout();
        if (inside(mouseX, mouseY, panelX + 14, panelY + 42, 76, 18)) {
            mode = Mode.RECIPES;
            selected = 0;
            firstVisible = 0;
            categoryFilter = null;
            searchQuery = "";
            return true;
        }
        if (inside(mouseX, mouseY, panelX + 98, panelY + 42, 76, 18)) {
            mode = Mode.USES;
            selected = 0;
            firstVisible = 0;
            categoryFilter = null;
            searchQuery = "";
            return true;
        }
        if (inside(mouseX, mouseY, panelX + 182, panelY + 42, 76, 18)) {
            mode = Mode.SOURCES;
            selected = 0;
            firstVisible = 0;
            categoryFilter = null;
            searchQuery = "";
            return true;
        }
        // Search bar focus
        Rect searchRect = searchBarRect();
        if (inside(mouseX, mouseY, searchRect.x(), searchRect.y(), searchRect.w(), searchRect.h())) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;
        if (handleCategoryClick(mouseX, mouseY)) {
            return true;
        }
        IndexRecipeView actionRecipe = selectedRecipe();
        if (actionRecipe != null) {
        Rect detail = detailRect();
        int detailX = detail.x();
        int detailY = detail.y();
        int detailW = detail.w();
        int traceHForClick = traceApplies(IndexRecipeTraceState.current()) ? 28 : 0;
        int cardHForClick = Math.max(70, detail.h() - traceHForClick - 25);
        int actionY = detailY + traceHForClick + cardHForClick + 4;
            IndexRecipeTraceState.Trace trace = IndexRecipeTraceState.current();
            if (traceApplies(trace)
                    && inside(mouseX, mouseY, detailX + detailW - 74, detailY + 4, 68, 16)) {
                EchoNativeLoadStatus lifecycleStatus = publishNativeRecipeScreenOpen(
                        trace.rootStack(),
                        Mode.RECIPES,
                        "recipe_trace_root_button");
                if (EchoIndexClient.nativeLoaderClientActiveForScreens()
                        && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
                    return false;
                }
                Minecraft.getInstance().setScreen(new IndexRecipeScreen(trace.rootStack(), Mode.RECIPES));
                return true;
            }
            IndexRecipePlan plan = IndexRecipePlanner.plan(Minecraft.getInstance().player, actionRecipe);
            int pinW = plan.pinned() ? 48 : 34;
            if (inside(mouseX, mouseY, detailX + 6, actionY, pinW, 16)) {
                sendRecipeAction(plan.pinned() ? IndexActionPacket.Action.UNPIN_RECIPE : IndexActionPacket.Action.PIN_RECIPE,
                        actionRecipe.id());
                return true;
            }
            if (plan.canTransfer() && inside(mouseX, mouseY, detailX + pinW + 11, actionY, 54, 16)) {
                sendRecipeAction(IndexActionPacket.Action.TRANSFER_RECIPE, actionRecipe.id());
                return true;
            }
            int traceX = detailX + pinW + 11 + (plan.canTransfer() ? 59 : 0);
            if (plan.missingCount() > 0 && inside(mouseX, mouseY, traceX, actionY, 44, 16)) {
                IndexRecipeTraceState.open(focusStack, actionRecipe, plan);
                return true;
            }
        }
        for (IndexRecipeUi.SlotHit hit : List.copyOf(slotHits)) {
            if (inside(mouseX, mouseY, hit.x(), hit.y(), hit.w(), hit.h())) {
                if (button == 2) {
                    toggleBookmark(IndexService.itemId(hit.stack().getItem()));
                } else if (button == 1 && hit.choiceCyclable()) {
                    IndexRecipeUi.cycleChoice(hit, 1);
                } else {
                    EchoNativeLoadStatus lifecycleStatus = publishNativeRecipeScreenOpen(
                            hit.stack(),
                            modeForSlot(hit, button),
                            "recipe_slot_navigation");
                    if (EchoIndexClient.nativeLoaderClientActiveForScreens()
                            && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
                        return false;
                    }
                    Minecraft.getInstance().setScreen(new IndexRecipeScreen(hit.stack(), modeForSlot(hit, button)));
                }
                return true;
            }
        }
        List<IndexRecipeView> recipes = recipes(focusStack.getItem());
        Rect list = listRect();
        int listX = list.x();
        int listY = list.y();
        int rowH = 24;
        int visible = Math.min(recipes.size() - firstVisible, visibleRows());
        for (int i = 0; i < visible; i++) {
            if (inside(mouseX, mouseY, listX, listY + i * rowH, listWidth(), 21)) {
                selected = firstVisible + i;
                return true;
            }
        }
        return false;
    }

    private EchoNativeLoadStatus publishNativeRecipeScreenOpen(ItemStack stack, Mode targetMode, String transitionSource) {
        return EchoIndexClient.publishNativeScreenLifecycle(
                "open",
                "index.recipe_screen.open_recipe",
                getClass().getName(),
                Map.of(
                        "targetScreenClass", IndexRecipeScreen.class.getName(),
                        "transitionSource", transitionSource,
                        "itemId", IndexService.itemId(stack.getItem()).toString(),
                        "recipeMode", (targetMode == null ? Mode.RECIPES : targetMode).name()));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (EchoIndexClient.nativeLoaderClientActiveForScreens()) {
            return EchoIndexClient.dispatchNativeRecipeScreenScroll(this, mouseX, mouseY, scrollX, scrollY);
        }
        return handleNativeRouteScroll(mouseX, mouseY, scrollX, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean handleNativeRouteScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        layout();
        if (!inside(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            return false;
        }
        for (IndexRecipeUi.SlotHit hit : List.copyOf(slotHits)) {
            if (hit.choiceCyclable() && inside(mouseX, mouseY, hit.x(), hit.y(), hit.w(), hit.h())) {
                IndexRecipeUi.cycleChoice(hit, scrollY > 0 ? 1 : -1);
                return true;
            }
        }
        List<IndexRecipeView> recipes = recipes(focusStack.getItem());
        int maxFirst = Math.max(0, recipes.size() - visibleRows());
        if (scrollY < 0 && firstVisible < maxFirst) {
            firstVisible++;
            selected = clamp(selected, firstVisible, Math.max(firstVisible, firstVisible + visibleRows() - 1));
            return true;
        }
        if (scrollY > 0 && firstVisible > 0) {
            firstVisible--;
            selected = clamp(selected, firstVisible, Math.max(firstVisible, firstVisible + visibleRows() - 1));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (EchoIndexClient.nativeLoaderClientActiveForScreens()) {
            return EchoIndexClient.dispatchNativeRecipeScreenKey(
                    this,
                    event.key(),
                    EchoIndexClient.SHOW_RECIPE_KEY.matches(event),
                    EchoIndexClient.SHOW_USAGE_KEY.matches(event));
        }
        return handleNativeRouteKey(
                event.key(),
                EchoIndexClient.SHOW_RECIPE_KEY.matches(event),
                EchoIndexClient.SHOW_USAGE_KEY.matches(event))
                || super.keyPressed(event);
    }

    public boolean handleNativeRouteKey(int key, boolean recipeKey, boolean usageKey) {
        List<IndexRecipeView> recipes = recipes(focusStack.getItem());
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    selected = 0;
                    firstVisible = 0;
                }
                return true;
            }
            return false;
        }
        if (recipeKey) {
            mode = Mode.RECIPES;
            selected = 0;
            firstVisible = 0;
            categoryFilter = null;
            return true;
        }
        if (usageKey) {
            mode = Mode.USES;
            selected = 0;
            firstVisible = 0;
            categoryFilter = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP && selected > 0) {
            selected--;
            if (selected < firstVisible) {
                firstVisible = selected;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN && selected + 1 < recipes.size()) {
            selected++;
            if (selected >= firstVisible + visibleRows()) {
                firstVisible++;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_PAGE_UP) {
            selected = Math.max(0, selected - visibleRows());
            firstVisible = Math.max(0, firstVisible - visibleRows());
            return true;
        }
        if (key == GLFW.GLFW_KEY_PAGE_DOWN) {
            selected = Math.min(Math.max(0, recipes.size() - 1), selected + visibleRows());
            firstVisible = Math.min(Math.max(0, recipes.size() - visibleRows()), firstVisible + visibleRows());
            return true;
        }
        // Start search on any printable character when not focused
        if (key == GLFW.GLFW_KEY_SLASH || key == GLFW.GLFW_KEY_F) {
            searchFocused = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (EchoIndexClient.nativeLoaderClientActiveForScreens()) {
            return EchoIndexClient.dispatchNativeRecipeScreenChar(
                    this,
                    event == null ? "" : event.codepointAsString(),
                    event != null && event.isAllowedChatCharacter());
        }
        return handleNativeRouteChar(
                event == null ? "" : event.codepointAsString(),
                event != null && event.isAllowedChatCharacter())
                || super.charTyped(event);
    }

    public boolean handleNativeRouteChar(String character, boolean allowedChatCharacter) {
        if (searchFocused && allowedChatCharacter) {
            searchQuery += character == null ? "" : character;
            selected = 0;
            firstVisible = 0;
            return true;
        }
        return false;
    }

    private void layout() {
        panelW = Math.min(760, Math.max(360, width - 44));
        panelH = Math.min(480, Math.max(280, height - 36));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    private void drawScreenChrome(GuiGraphicsExtractor graphics) {
        IndexThemeStyle.panel(graphics, panelX, panelY, panelW, panelH, BG, CYAN);
        if (cinematicStyle()) {
            renderCoreFrame(graphics, panelX, panelY, panelW, panelH);
        }
        graphics.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + HEADER_HEIGHT, 0x33163843);
        graphics.fill(panelX + 14, panelY + 30, panelX + panelW - 14, panelY + 31, 0x6632BFD7);
    }

    private void drawModeButton(GuiGraphicsExtractor graphics, Font font, int x, int y, Mode target, int mouseX, int mouseY) {
        boolean active = mode == target;
        boolean hover = inside(mouseX, mouseY, x, y, 76, 18);
        IndexThemeStyle.button(graphics, font, x, y, 76, 18, target.label(), hover, true, active ? CYAN : 0xFF2F5B68);
    }

    private void drawSearchBar(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        Rect rect = searchBarRect();
        int x = rect.x();
        int y = rect.y();
        int w = rect.w();
        boolean hover = inside(mouseX, mouseY, x, y, w, 16);
        graphics.fill(x, y, x + w, y + 16, searchFocused ? 0xFF123241 : hover ? 0xCC102630 : PANEL);
        EchoCyberGlassUi.frame(graphics, x, y, w, 16, searchFocused ? CYAN : 0x4438DFF4);

        String display = searchQuery.isEmpty() ? "Search... (/)" : searchQuery;
        int textColor = searchQuery.isEmpty() ? 0xFF5A7A8A : TEXT;
        int textX = x + 6;
        int textY = y + 5;
        String trimmed = trim(font, display, w - 14);
        graphics.text(font, trimmed, textX, textY, textColor, false);

        // Blinking cursor
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = textX + font.width(trimmed);
            graphics.text(font, "_", cursorX, textY, CYAN, false);
        }

        // Clear button
        if (!searchQuery.isEmpty()) {
            int clearX = x + w - 14;
            graphics.text(font, "x", clearX, textY, MUTED, false);
            if (hover && inside(mouseX, mouseY, clearX - 2, y, 14, 16)) {
                // Clicking the x area will be handled in mouseClicked by resetting search
            }
        }
    }

    private Rect searchBarRect() {
        int modeEnd = panelX + 182 + 76 + 8;
        int w = Math.max(80, panelX + panelW - 16 - modeEnd);
        return new Rect(modeEnd, panelY + 43, w, 16);
    }

    private void drawCategoryChips(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        int x = panelX + 14;
        int y = panelY + 64;
        chip(graphics, font, x, y, 34, "All", categoryFilter == null, mouseX, mouseY);
        int cx = x + 38;
        int hidden = 0;
        for (Identifier category : categories()) {
            int w = Math.min(86, Math.max(44, font.width(category.getPath()) + 12));
            if (cx + w > panelX + panelW - 16) {
                hidden++;
                break;
            }
            chip(graphics, font, cx, y, w, category.getPath(), category.equals(categoryFilter), mouseX, mouseY);
            cx += w + 4;
        }
        if (hidden > 0 && cx + 36 <= panelX + panelW - 16) {
            chip(graphics, font, cx, y, 36, "+" + hidden, false, mouseX, mouseY);
        }
    }

    private void chip(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, String label,
            boolean active, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, 16);
        IndexThemeStyle.chip(graphics, font, x, y, w, 16, trim(font, label, w - 4), active, hover);
    }

    private boolean handleCategoryClick(double mouseX, double mouseY) {
        int x = panelX + 14;
        int y = panelY + 64;
        if (inside(mouseX, mouseY, x, y, 34, 16)) {
            categoryFilter = null;
            selected = 0;
            firstVisible = 0;
            return true;
        }
        int cx = x + 38;
        Font font = Minecraft.getInstance().font;
        for (Identifier category : categories()) {
            int w = Math.min(86, Math.max(44, font.width(category.getPath()) + 12));
            if (cx + w > panelX + panelW - 16) {
                break;
            }
            if (inside(mouseX, mouseY, cx, y, w, 16)) {
                categoryFilter = category.equals(categoryFilter) ? null : category;
                selected = 0;
                firstVisible = 0;
                return true;
            }
            cx += w + 4;
        }
        // Check clear search button
        Rect searchRect = searchBarRect();
        if (!searchQuery.isEmpty() && inside(mouseX, mouseY, searchRect.x() + searchRect.w() - 16, searchRect.y(), 14, 16)) {
            searchQuery = "";
            selected = 0;
            firstVisible = 0;
            return true;
        }
        return false;
    }

    private void smallButton(GuiGraphicsExtractor graphics, Font font, int x, int y, int w, int h,
            String label, boolean active) {
        IndexThemeStyle.button(graphics, font, x, y, w, h, label, false, active, CYAN);
    }

    private void drawCloseHint(GuiGraphicsExtractor graphics, Font font) {
        graphics.text(font, "ESC", panelX + panelW - 36, panelY + panelH - 18, MUTED, false);
    }

    private void drawShortcutHints(GuiGraphicsExtractor graphics, Font font) {
        String hints = "UP/DOWN: navigate  |  PAGE: scroll  |  /: search  |  RIGHT-CLICK: uses  |  MIDDLE-CLICK: bookmark";
        graphics.text(font, hints, panelX + 14, panelY + panelH - 18, 0xFF3A5A6A, false);
    }

    private void drawEmpty(GuiGraphicsExtractor graphics, Font font) {
        int x = panelX + 18;
        int y = contentY() + 14;
        graphics.fill(x, y, panelX + panelW - 18, y + 72, 0xAA071014);
        graphics.text(font, "No " + mode.label().toLowerCase() + " indexed for this item.", x + 12, y + 12, WARN, false);
        String message = IndexRecipeUi.emptyMessage(Minecraft.getInstance().player, focusStack.getItem(), mode.viewMode());
        graphics.textWithWordWrap(font, Component.literal(message),
                x + 12, y + 26, panelW - 60, MUTED);
        if (!searchQuery.isEmpty()) {
            graphics.text(font, "Search active: '" + searchQuery + "' — try clearing filters.", x + 12, y + 56, 0xFF5BC0EB, false);
        }
    }

    private void drawRecipeList(GuiGraphicsExtractor graphics, Font font, List<IndexRecipeView> recipes, int mouseX, int mouseY) {
        Rect list = listRect();
        int x = list.x();
        int y = list.y();
        int rowH = 24;
        int visible = Math.min(recipes.size() - firstVisible, visibleRows());
        String range = recipes.isEmpty() ? "0 / 0"
                : (firstVisible + 1) + "-" + (firstVisible + visible) + " / " + recipes.size();
        graphics.text(font, "Matches " + range, x, y - 12, MUTED, false);
        for (int i = 0; i < visible; i++) {
            int index = firstVisible + i;
            IndexRecipeView recipe = recipes.get(index);
            boolean active = index == selected;
            boolean hover = inside(mouseX, mouseY, x, y + i * rowH, listWidth(), 21);
            int rowY = y + i * rowH;
            graphics.fill(x, rowY, x + listWidth(), rowY + 21, active ? 0xFF123241 : hover ? 0xCC102630 : ROW);
            graphics.fill(x, rowY + 19, x + listWidth(), rowY + 21, active ? CYAN : 0x552F5B68);
            graphics.item(recipeIcon(recipe), x + 3, rowY + 2);
            IndexRecipePlan plan = IndexRecipePlanner.plan(Minecraft.getInstance().player, recipe);
            ItemStack categoryIcon = recipe.machine().isEmpty() ? recipeIcon(recipe) : recipe.machine();
            int chipW = Math.min(56, Math.max(32, font.width(IndexRecipeUi.statusLabel(plan, mode == Mode.RECIPES)) + 8));
            int iconX = x + listWidth() - 20;
            graphics.item(categoryIcon, iconX, rowY + 2);
            graphics.fill(iconX - chipW - 4, rowY + 3, iconX - 4, rowY + 18, 0x66102630);
            graphics.outline(iconX - chipW - 4, rowY + 3, chipW, 15, IndexRecipeUi.statusColor(plan, mode == Mode.RECIPES));
            graphics.centeredText(font, trim(font, IndexRecipeUi.statusLabel(plan, mode == Mode.RECIPES), chipW - 4),
                    iconX - chipW - 4 + chipW / 2, rowY + 7, IndexRecipeUi.statusColor(plan, mode == Mode.RECIPES));
            graphics.text(font, trim(font, recipe.title(), listWidth() - chipW - 54), x + 24, rowY + 6,
                    active ? TEXT : 0xFFD8F6FF, false);
        }
        drawScrollbar(graphics, recipes.size(), visible, firstVisible, x + listWidth() + 2, y, 3, visible * rowH - 3);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics, int total, int visible, int first, int x, int y, int w, int h) {
        if (total <= visible) {
            return;
        }
        int thumbH = Math.max(8, h * visible / total);
        int thumbY = y + (h - thumbH) * first / Math.max(1, total - visible);
        IndexThemeStyle.scrollbar(graphics, x, y, w, h, x, thumbY, w, thumbH);
    }

    private void drawRecipeDetails(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe, int mouseX, int mouseY) {
        Rect detail = detailRect();
        int x = detail.x();
        int y = detail.y();
        int w = detail.w();
        int h = detail.h();
        int traceH = drawTraceRail(graphics, font, recipe, x, y, w, mouseX, mouseY);
        int cardH = Math.max(70, h - traceH - 25);
        IndexRecipeUi.drawRecipeCard(graphics, font, recipe, x, y + traceH, w, cardH,
                focusStack, mouseX, mouseY, slotHits);
        drawRecipeActions(graphics, font, recipe, x + 6, y + traceH + cardH + 4, w - 12);
    }

    private int drawTraceRail(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe,
            int x, int y, int w, int mouseX, int mouseY) {
        IndexRecipeTraceState.Trace trace = IndexRecipeTraceState.current();
        if (!traceApplies(trace)) {
            return 0;
        }
        int h = 28;
        graphics.fill(x, y, x + w, y + h - 4, 0x88102630);
        graphics.outline(x, y, w, h - 4, 0x5538DFF4);
        String label = "Path: " + trace.rootStack().getHoverName().getString() + " > missing inputs";
        graphics.text(font, trim(font, label, w - 82), x + 7, y + 8, MUTED, false);
        smallButton(graphics, font, x + w - 74, y + 4, 68, 16,
                recipe != null && recipe.id().equals(trace.rootRecipeId()) ? "Tracing" : "Root", true);
        return h;
    }

    private boolean traceApplies(IndexRecipeTraceState.Trace trace) {
        if (trace == null || !trace.active() || focusStack.isEmpty()) {
            return false;
        }
        Identifier focusId = IndexService.itemId(focusStack.getItem());
        return trace.rootItemId().equals(focusId)
                || trace.entries().stream().anyMatch(entry -> entry.itemId().equals(focusId));
    }

    private void drawRecipeActions(GuiGraphicsExtractor graphics, Font font, IndexRecipeView recipe, int x, int y, int w) {
        IndexRecipePlan plan = IndexRecipePlanner.plan(Minecraft.getInstance().player, recipe);
        int pinW = plan.pinned() ? 48 : 34;
        smallButton(graphics, font, x, y, pinW, 16, plan.pinned() ? "Unpin" : "Pin", true);
        int tx = x + pinW + 5;
        if (plan.canTransfer()) {
            smallButton(graphics, font, tx, y, 54, 16, "Transfer", true);
            tx += 59;
        }
        if (plan.missingCount() > 0) {
            smallButton(graphics, font, tx, y, 44, 16, "Trace", true);
        } else if (!plan.canTransfer()) {
            String note = IndexRecipeUi.statusDetail(plan, true);
            if (!note.isBlank()) {
                graphics.text(font, trim(font, note, Math.max(30, w - pinW - 8)), tx, y + 5,
                        IndexRecipeUi.statusColor(plan, true), false);
            }
        } else {
            String note = IndexRecipeUi.statusDetail(plan, true);
            graphics.text(font, trim(font, note, Math.max(30, w - (tx - x) - 8)), tx, y + 5,
                    IndexRecipeUi.statusColor(plan, true), false);
        }
    }

    private IndexRecipeView selectedRecipe() {
        List<IndexRecipeView> recipes = recipes(focusStack.getItem());
        if (recipes.isEmpty()) {
            return null;
        }
        return recipes.get(clamp(selected, 0, recipes.size() - 1));
    }

    private List<IndexRecipeView> recipes(Item item) {
        List<IndexRecipeView> all = allRecipes(item);
        List<IndexRecipeView> filtered = categoryFilter == null ? all : all.stream()
                .filter(recipe -> recipe.categoryId().equals(categoryFilter))
                .toList();
        if (searchQuery.isEmpty()) {
            return filtered;
        }
        String q = searchQuery.toLowerCase(Locale.ROOT);
        return filtered.stream()
                .filter(recipe -> recipe.title().toLowerCase(Locale.ROOT).contains(q)
                        || recipe.categoryId().getPath().toLowerCase(Locale.ROOT).contains(q)
                        || recipe.sourceModId().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    private List<IndexRecipeView> allRecipes(Item item) {
        Player player = Minecraft.getInstance().player;
        return IndexRecipeUi.viewsFor(player, item, mode.viewMode());
    }

    private List<Identifier> categories() {
        Set<Identifier> categories = new LinkedHashSet<>();
        for (IndexRecipeView recipe : allRecipes(focusStack.getItem())) {
            categories.add(recipe.categoryId());
        }
        return List.copyOf(categories);
    }

    private ItemStack recipeIcon(IndexRecipeView recipe) {
        for (IndexRecipeSlot slot : recipe.slots()) {
            if (slot.role() == IndexSlotRole.OUTPUT && !slot.stacks().isEmpty() && !slot.stacks().getFirst().isEmpty()) {
                return slot.stacks().getFirst();
            }
        }
        return recipe.machine().isEmpty() ? focusStack : recipe.machine();
    }

    private String titleLine() {
        return "ECHO: INDEX | " + mode.label().toUpperCase();
    }

    private boolean wideLayout() {
        return panelW >= 560;
    }

    private int listWidth() {
        return wideLayout() ? Math.max(150, Math.min(196, panelW / 3)) : panelW - 28;
    }

    private int listHeight() {
        int available = Math.max(72, panelY + panelH - 34 - contentY());
        if (wideLayout()) {
            return available;
        }
        return clamp(available / 3, 72, Math.max(72, available - 112));
    }

    private Rect listRect() {
        return new Rect(panelX + 14, contentY(), listWidth(), listHeight());
    }

    private Rect detailRect() {
        if (wideLayout()) {
            int x = panelX + 24 + listWidth();
            int y = contentY();
            return new Rect(x, y, panelW - listWidth() - 42, Math.max(96, panelY + panelH - 34 - y));
        }
        int y = contentY() + listHeight() + 18;
        return new Rect(panelX + 14, y, panelW - 28, Math.max(96, panelY + panelH - 34 - y));
    }

    private int visibleRows() {
        return Math.max(1, listHeight() / 24);
    }

    private int contentY() {
        return panelY + 90;
    }

    private static void renderCoreFrame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        try {
            Class.forName("com.knoxhack.echoindex.integration.IndexRenderCoreScreenIntegration")
                    .getMethod("drawOverlayFrame", GuiGraphicsExtractor.class, int.class, int.class, int.class, int.class)
                    .invoke(null, graphics, x, y, width, height);
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
    }

    private static boolean cinematicStyle() {
        try {
            return Config.UI_CINEMATIC_STYLE.get();
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private static void refreshThemeAliases() {
        IndexRecipeUi.refreshTheme();
        BG = IndexRecipeUi.BG;
        PANEL = IndexRecipeUi.PANEL;
        ROW = IndexRecipeUi.ROW;
        CYAN = IndexRecipeUi.CYAN;
        TEXT = IndexRecipeUi.TEXT;
        MUTED = IndexRecipeUi.MUTED;
        WARN = IndexRecipeUi.WARN;
    }

    private static Mode modeForSlot(IndexRecipeUi.SlotHit hit, int button) {
        if (button == 1) {
            return Mode.USES;
        }
        return hit.role() == IndexSlotRole.OUTPUT ? Mode.RECIPES : Mode.USES;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String trim(Font font, String text, int width) {
        String safe = text == null ? "" : text;
        if (font.width(safe) <= width) {
            return safe;
        }
        String ellipsis = "...";
        while (!safe.isEmpty() && font.width(safe + ellipsis) > width) {
            safe = safe.substring(0, safe.length() - 1);
        }
        return safe + ellipsis;
    }

    public enum Mode {
        RECIPES("Recipes"),
        USES("Uses"),
        SOURCES("Sources");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public IndexRecipeUi.ViewMode viewMode() {
            return switch (this) {
                case RECIPES -> IndexRecipeUi.ViewMode.RECIPES;
                case USES -> IndexRecipeUi.ViewMode.USES;
                case SOURCES -> IndexRecipeUi.ViewMode.SOURCES;
            };
        }
    }

    private static void toggleBookmark(Identifier id) {
        boolean currently = ClientIndexState.isBookmarked(id);
        EchoNetClientActions.sendServerboundAction(new IndexActionPacket(
                currently ? IndexActionPacket.Action.UNBOOKMARK : IndexActionPacket.Action.BOOKMARK,
                id));
    }

    private static void sendRecipeAction(IndexActionPacket.Action action, Identifier recipeId) {
        if (recipeId != null) {
            EchoNetClientActions.sendServerboundAction(new IndexActionPacket(action, recipeId));
        }
    }

    private record Rect(int x, int y, int w, int h) {
    }
}
