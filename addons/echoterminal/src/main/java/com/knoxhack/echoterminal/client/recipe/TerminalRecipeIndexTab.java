package com.knoxhack.echoterminal.client.recipe;

import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeCategory;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeNote;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSnapshot;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSlot;
import com.knoxhack.echoterminal.client.screen.TerminalScrollbar;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class TerminalRecipeIndexTab implements ClientTerminalTab {
    public static final Identifier TAB_ID = Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "recipe_index");
    private static final int ACCENT = 0xFFFFD166;
    private static final int STATE_REFRESH_TICKS = 20;

    private final TerminalTabDescriptor descriptor =
            new TerminalTabDescriptor(TAB_ID, "RECIPE INDEX", 145, ACCENT);
    private final TerminalTabChrome chrome =
            TerminalTabChrome.of("Recipe Index", TerminalTabChrome.GROUP_CORE, "RI", "ECHO recipe database", 145);
    private final List<Hitbox> hitboxes = new ArrayList<>();

    private Identifier categoryFilter;
    private Mode mode = Mode.RECIPES;
    private SourceFilter sourceFilter = SourceFilter.PROCESSES;
    private Focus focus = Focus.ITEMS;
    private String searchText = "";
    private Item selectedItem;
    private Identifier selectedRecipeId;
    private int focusedItemIndex;
    private int focusedRecipeIndex;
    private int itemScroll;
    private int recipeScroll;
    private int detailScroll;
    private int controlScroll;
    private int lastControlX;
    private int lastControlY;
    private int lastControlW;
    private int lastControlH;
    private int lastControlContentH;
    private int lastItemX;
    private int lastItemY;
    private int lastItemW;
    private int lastItemH;
    private int lastItemContentH;
    private int lastRecipeX;
    private int lastRecipeY;
    private int lastRecipeW;
    private int lastRecipeH;
    private int lastRecipeContentH;
    private int lastDetailX;
    private int lastDetailY;
    private int lastDetailW;
    private int lastDetailH;
    private int lastDetailContentH;
    private ScrollPane scrollbarDragPane = ScrollPane.NONE;
    private int scrollbarDragOffset;
    private int lastItemColumns = 1;
    private int lastRefreshTick = -1;
    private State cachedState = State.empty();

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
        controlScroll = 0;
        itemScroll = 0;
        recipeScroll = 0;
        detailScroll = 0;
        scrollbarDragPane = ScrollPane.NONE;
        lastRefreshTick = -1;
    }

    @Override
    public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hitboxes.clear();
        State state = state(context);
        normalizeSelection(state);

        int x = context.contentX();
        int y = context.contentY();
        int w = context.contentWidth();
        int h = context.contentHeight();
        int headerBottom = drawControls(context, graphics, state, x, y, w, mouseX, mouseY);
        int bodyY = headerBottom + 8;
        int bodyH = Math.max(120, h - (bodyY - y));

        if (w >= 640) {
            int gap = 10;
            int itemW = Math.max(160, Math.min(220, w * 27 / 100));
            int recipeW = Math.max(178, Math.min(260, w * 31 / 100));
            int detailX = x + itemW + gap + recipeW + gap;
            drawItemPane(context, graphics, state, x, bodyY, itemW, bodyH, mouseX, mouseY);
            drawRecipePane(context, graphics, state, x + itemW + gap, bodyY, recipeW, bodyH, mouseX, mouseY);
            drawDetailPane(context, graphics, state, selectedRecipe(state), detailX, bodyY,
                    Math.max(180, w - itemW - recipeW - gap * 2), bodyH, mouseX, mouseY);
        } else {
            int topH = Math.max(120, Math.min(210, bodyH * 42 / 100));
            int gap = 8;
            int itemW = Math.max(140, w / 2 - gap / 2);
            drawItemPane(context, graphics, state, x, bodyY, itemW, topH, mouseX, mouseY);
            drawRecipePane(context, graphics, state, x + itemW + gap, bodyY, Math.max(120, w - itemW - gap),
                    topH, mouseX, mouseY);
            drawDetailPane(context, graphics, state, selectedRecipe(state), x, bodyY + topH + gap,
                    w, Math.max(160, bodyH - topH - gap), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
        if (button == 0 && beginScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        for (Hitbox hitbox : List.copyOf(hitboxes)) {
            if (TerminalUi.inside(mouseX, mouseY, hitbox.x(), hitbox.y(), hitbox.w(), hitbox.h())) {
                hitbox.action().click(button);
                context.playCommandSound();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
        int amount = (int) Math.round(delta * 18.0D);
        if (TerminalUi.inside(mouseX, mouseY, lastControlX, lastControlY, lastControlW, lastControlH)) {
            controlScroll = TerminalUi.clampScroll(controlScroll - amount, lastControlContentH, lastControlH);
            return true;
        }
        if (TerminalUi.inside(mouseX, mouseY, lastItemX, lastItemY, lastItemW, lastItemH)) {
            itemScroll = TerminalUi.clampScroll(itemScroll - amount, lastItemContentH, lastItemH);
            return true;
        }
        if (TerminalUi.inside(mouseX, mouseY, lastRecipeX, lastRecipeY, lastRecipeW, lastRecipeH)) {
            recipeScroll = TerminalUi.clampScroll(recipeScroll - amount, lastRecipeContentH, lastRecipeH);
            return true;
        }
        if (TerminalUi.inside(mouseX, mouseY, lastDetailX, lastDetailY, lastDetailW, lastDetailH)) {
            detailScroll = TerminalUi.clampScroll(detailScroll - amount, lastDetailContentH, lastDetailH);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(TerminalRenderContext context, KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.offsetByCodePoints(searchText.length(), -1));
            resetScroll();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && !searchText.isEmpty()) {
            searchText = "";
            resetScroll();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && categoryFilter != null) {
            categoryFilter = null;
            resetScroll();
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            focus = switch (focus) {
                case ITEMS -> Focus.RECIPES;
                case RECIPES -> Focus.DETAIL;
                case DETAIL -> Focus.ITEMS;
            };
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            State state = state(context);
            normalizeSelection(state);
            if (focus == Focus.ITEMS) {
                moveFocusedItem(state, key == GLFW.GLFW_KEY_LEFT ? -1 : 1);
            } else if (focus == Focus.RECIPES) {
                setMode(mode == Mode.RECIPES ? Mode.USES : Mode.RECIPES);
            } else {
                detailScroll = TerminalUi.clampScroll(detailScroll + (key == GLFW.GLFW_KEY_LEFT ? -32 : 32),
                        lastDetailContentH, lastDetailH);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            State state = state(context);
            normalizeSelection(state);
            if (focus == Focus.ITEMS) {
                moveFocusedItem(state, (key == GLFW.GLFW_KEY_UP ? -1 : 1) * Math.max(1, lastItemColumns));
            } else if (focus == Focus.RECIPES) {
                moveFocusedRecipe(state, key == GLFW.GLFW_KEY_UP ? -1 : 1);
            } else {
                detailScroll = TerminalUi.clampScroll(detailScroll + (key == GLFW.GLFW_KEY_UP ? -32 : 32),
                        lastDetailContentH, lastDetailH);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            State state = state(context);
            normalizeSelection(state);
            if (focus == Focus.ITEMS) {
                selectFocusedItem(state, mode);
            } else if (focus == Focus.RECIPES) {
                selectFocusedRecipe(state);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
        if (event == null || !event.isAllowedChatCharacter() || searchText.length() >= 36) {
            return false;
        }
        String typed = event.codepointAsString();
        if (typed == null || typed.isBlank()) {
            return false;
        }
        searchText += typed.toLowerCase(Locale.ROOT);
        resetScroll();
        return true;
    }

    @Override
    public int contentHeight(TerminalRenderContext context) {
        return context.contentHeight();
    }

    public static List<ItemStack> echoItemsForTests() {
        return echoItems();
    }

    public static List<TerminalRecipeEntry> matchingRecipesForTests(
            List<TerminalRecipeEntry> recipes, ItemStack stack, boolean uses) {
        Item item = stack == null ? null : stack.getItem();
        if (item == null) {
            return List.of();
        }
        return recipes.stream()
                .filter(recipe -> uses ? recipe.uses(item) : recipe.outputs(item))
                .toList();
    }

    public static List<TerminalRecipeEntry> sourceFilteredRecipesForTests(
            List<TerminalRecipeEntry> recipes, String filterName) {
        SourceFilter filter = SourceFilter.fromName(filterName);
        return recipes.stream()
                .filter(filter::matches)
                .toList();
    }

    private int drawControls(TerminalRenderContext context, GuiGraphicsExtractor graphics, State state,
            int x, int y, int w, int mouseX, int mouseY) {
        TerminalUi.sectionHeader(context, graphics, "Recipe Index",
                state.snapshot().providerCount() == 0
                        ? "no providers online"
                        : state.snapshot().providerCount() + " providers",
                x, y, w, ACCENT);
        int chipY = y + 22;
        int modeW = 64;
        boolean stackedControls = w < 390;
        int sourceFilterY = chipY + 21;
        int sourceFilterBottom = sourceFilterY + sourceFilterRows(w) * 18;
        int modeAreaW = Mode.values().length * modeW + Math.max(0, Mode.values().length - 1) * 4;
        int searchX = stackedControls ? x : x + modeAreaW + 16;
        int searchY = stackedControls ? sourceFilterBottom + 3 : chipY;
        int searchW = stackedControls ? w : Math.max(90, Math.min(220, w - (searchX - x)));
        int categoryAreaY = Math.max(searchY + 22, sourceFilterBottom + 3);
        int categoryAreaH = Math.min(96, Math.max(38, context.contentHeight() / 4));
        TerminalUi.filterToolbarPanel(context, graphics, x, chipY - 4, w,
                categoryAreaY + categoryAreaH - chipY + 8, ACCENT);
        int modeIndex = 0;
        for (Mode candidate : Mode.values()) {
            int chipX = x + modeIndex * (modeW + 4);
            boolean active = mode == candidate;
            boolean hover = TerminalUi.inside(mouseX, mouseY, chipX, chipY, modeW, 16);
            TerminalUi.filterChip(context, graphics, chipX, chipY, modeW,
                    candidate.label(), active, true, active ? ACCENT : TerminalUi.muted(context), hover);
            hitboxes.add(new Hitbox(chipX, chipY, modeW, 16, button -> {
                setMode(candidate);
                focus = Focus.RECIPES;
            }));
            modeIndex++;
        }

        int filterX = x;
        int filterY = sourceFilterY;
        for (SourceFilter candidate : SourceFilter.values()) {
            int chipW = candidate.width();
            if (filterX > x && filterX + chipW > x + w) {
                filterX = x;
                filterY += 18;
            }
            boolean active = sourceFilter == candidate;
            boolean hover = TerminalUi.inside(mouseX, mouseY, filterX, filterY, chipW, 16);
            TerminalUi.filterChip(context, graphics, filterX, filterY, chipW,
                    candidate.label(), active, true, active ? ACCENT : TerminalUi.muted(context), hover);
            hitboxes.add(new Hitbox(filterX, filterY, chipW, 16, button -> {
                sourceFilter = candidate;
                selectedRecipeId = null;
                recipeScroll = 0;
                detailScroll = 0;
            }));
            filterX += chipW + 4;
        }

        TerminalUi.sortDropdownLikeChip(graphics, font(context), searchX, searchY, searchW,
                searchText.isBlank() ? "type to search" : "find: " + searchText, ACCENT);

        lastControlX = x;
        lastControlY = categoryAreaY;
        lastControlW = w;
        lastControlH = categoryAreaH;
        controlScroll = TerminalUi.clampScroll(controlScroll, lastControlContentH, categoryAreaH);
        graphics.enableScissor(x, categoryAreaY, x + w, categoryAreaY + categoryAreaH);

        int cx = x;
        int cy = categoryAreaY - controlScroll;
        int rowH = 18;
        int allW = 46;
        boolean allActive = categoryFilter == null;
        boolean allHover = TerminalUi.inside(mouseX, mouseY, cx, cy, allW, 16);
        if (chipVisible(cy, categoryAreaY, categoryAreaH)) {
            TerminalUi.filterChip(context, graphics, cx, cy, allW, "All", allActive, true,
                    allActive ? ACCENT : TerminalUi.muted(context), allHover);
            hitboxes.add(new Hitbox(cx, cy, allW, 16, button -> {
                categoryFilter = null;
                selectedRecipeId = null;
                recipeScroll = 0;
                detailScroll = 0;
            }));
        }
        cx += allW + 4;
        for (TerminalRecipeCategory category : state.categories()) {
            String label = category.title() + " " + state.snapshot().recipeCount(category.id());
            int chipW = Math.max(62, Math.min(132, font(context).width(label) + 18));
            if (cx + chipW > x + w) {
                cx = x;
                cy += rowH;
            }
            boolean active = category.id().equals(categoryFilter);
            boolean hover = TerminalUi.inside(mouseX, mouseY, cx, cy, chipW, 16);
            if (chipVisible(cy, categoryAreaY, categoryAreaH)) {
                TerminalUi.filterChip(context, graphics, cx, cy, chipW, label, active, true,
                        active ? category.accentColor() : TerminalUi.muted(context), hover);
                hitboxes.add(new Hitbox(cx, cy, chipW, 16, button -> {
                    categoryFilter = category.id();
                    selectedRecipeId = null;
                    recipeScroll = 0;
                    detailScroll = 0;
                }));
            }
            cx += chipW + 4;
        }
        graphics.disableScissor();
        lastControlContentH = Math.max(categoryAreaH, cy + 18 + controlScroll - categoryAreaY);
        controlScroll = TerminalUi.clampScroll(controlScroll, lastControlContentH, categoryAreaH);
        TerminalUi.scrollbar(context, graphics, x + w - 9, categoryAreaY, categoryAreaH, controlScroll,
                Math.max(0, lastControlContentH - categoryAreaH), ACCENT,
                TerminalUi.inside(mouseX, mouseY, x, categoryAreaY, w, categoryAreaH));
        return categoryAreaY + categoryAreaH + 2;
    }

    private static boolean chipVisible(int y, int areaY, int areaH) {
        return y + 16 >= areaY && y <= areaY + areaH;
    }

    private void drawItemPane(TerminalRenderContext context, GuiGraphicsExtractor graphics, State state,
            int x, int y, int w, int h, int mouseX, int mouseY) {
        lastItemX = x;
        lastItemY = y;
        lastItemW = w;
        lastItemH = h;
        TerminalUi.flatHudPanel(context, graphics, x, y, w, h, ACCENT);
        TerminalUi.line(context, graphics, "ECHO ITEMS", x + 10, y + 9, w - 20, ACCENT);
        List<ItemStack> items = filteredItems(state);
        int columns = Math.max(1, Math.max(1, w - 18) / 24);
        lastItemColumns = columns;
        lastItemContentH = 26 + ((items.size() + columns - 1) / columns) * 24 + 8;
        itemScroll = TerminalUi.clampScroll(itemScroll, lastItemContentH, h);
        graphics.enableScissor(x + 6, y + 25, x + w - 6, y + h - 6);
        int startY = y + 30 - itemScroll;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            int sx = x + 10 + (i % columns) * 24;
            int sy = startY + (i / columns) * 24;
            if (sy < y + 20 || sy > y + h) {
                continue;
            }
            boolean selected = selectedItem != null && stack.is(selectedItem);
            boolean focused = focus == Focus.ITEMS && i == focusedItemIndex;
            boolean hover = TerminalUi.inside(mouseX, mouseY, sx, sy, 20, 20);
            TerminalUi.itemSlot(context, graphics, stack, sx, sy, selected ? ACCENT : TerminalUi.muted(context), hover);
            if (selected) {
                graphics.outline(sx - 1, sy - 1, 22, 22, ACCENT);
            }
            if (focused) {
                graphics.outline(sx - 3, sy - 3, 26, 26, TerminalUi.text(context));
            }
            hitboxes.add(new Hitbox(sx, sy, 20, 20, button -> {
                selectedItem = stack.getItem();
                selectedRecipeId = null;
                mode = button == 1 ? Mode.USES : Mode.RECIPES;
                focus = Focus.RECIPES;
                focusedItemIndex = items.indexOf(stack);
                recipeScroll = 0;
                detailScroll = 0;
            }));
        }
        graphics.disableScissor();
        TerminalUi.scrollbar(context, graphics, x + w - 9, y + 28, h - 36, itemScroll,
                Math.max(0, lastItemContentH - h), ACCENT,
                TerminalUi.inside(mouseX, mouseY, x, y, w, h));
        if (items.isEmpty()) {
            String empty = state.items().isEmpty()
                    ? "No registered ECHO items are visible to the client yet."
                    : "No ECHO items match the current search.";
            TerminalUi.wrap(context, graphics, empty, x + 10, y + 34, w - 20, TerminalUi.muted(context));
        }
    }

    private void drawRecipePane(TerminalRenderContext context, GuiGraphicsExtractor graphics, State state,
            int x, int y, int w, int h, int mouseX, int mouseY) {
        lastRecipeX = x;
        lastRecipeY = y;
        lastRecipeW = w;
        lastRecipeH = h;
        TerminalUi.flatHudPanel(context, graphics, x, y, w, h, ACCENT);
        String selectedName = selectedItem == null ? "No item selected" : new ItemStack(selectedItem).getHoverName().getString();
        TerminalUi.line(context, graphics, mode.label().toUpperCase(Locale.ROOT), x + 10, y + 9, 62, ACCENT);
        TerminalUi.line(context, graphics, selectedName, x + 76, y + 9, w - 86, TerminalUi.text(context));
        List<TerminalRecipeEntry> recipes = filteredRecipes(state);
        int rowH = 34;
        lastRecipeContentH = 30 + recipes.size() * rowH + 10;
        recipeScroll = TerminalUi.clampScroll(recipeScroll, lastRecipeContentH, h);
        graphics.enableScissor(x + 6, y + 27, x + w - 6, y + h - 6);
        int cy = y + 31 - recipeScroll;
        for (TerminalRecipeEntry recipe : recipes) {
            if (cy + rowH >= y + 26 && cy <= y + h) {
                boolean selected = recipe.id().equals(selectedRecipeId);
                boolean focused = focus == Focus.RECIPES && recipes.indexOf(recipe) == focusedRecipeIndex;
                boolean hover = TerminalUi.inside(mouseX, mouseY, x + 8, cy, w - 16, rowH - 4);
                graphics.fill(x + 8, cy, x + w - 8, cy + rowH - 4,
                        selected ? TerminalUi.tokens(context).colors().rowSelected() : TerminalUi.tokens(context).colors().row());
                if (selected || hover) {
                    graphics.outline(x + 8, cy, w - 16, rowH - 4,
                            selected ? ACCENT : TerminalUi.tokens(context).borders().normal());
                }
                if (focused) {
                    graphics.outline(x + 6, cy - 2, w - 12, rowH, TerminalUi.text(context));
                }
                ItemStack icon = firstOutput(recipe);
                if (icon.isEmpty()) {
                    icon = recipe.machine();
                }
                TerminalUi.itemSlot(context, graphics, icon, x + 12, cy + 5, ACCENT,
                        TerminalUi.inside(mouseX, mouseY, x + 12, cy + 5, 20, 20));
                TerminalUi.line(context, graphics, recipe.title(), x + 38, cy + 5, w - 52,
                        recipe.locked() ? TerminalUi.warning(context) : TerminalUi.text(context));
                TerminalUi.line(context, graphics, categoryTitle(state, recipe.categoryId()), x + 38, cy + 17, w - 52,
                        TerminalUi.muted(context));
                hitboxes.add(new Hitbox(x + 8, cy, w - 16, rowH - 4, button -> {
                    selectedRecipeId = recipe.id();
                    focus = Focus.DETAIL;
                    focusedRecipeIndex = recipes.indexOf(recipe);
                    detailScroll = 0;
                }));
            }
            cy += rowH;
        }
        graphics.disableScissor();
        TerminalUi.scrollbar(context, graphics, x + w - 9, y + 29, h - 37, recipeScroll,
                Math.max(0, lastRecipeContentH - h), ACCENT,
                TerminalUi.inside(mouseX, mouseY, x, y, w, h));
        if (recipes.isEmpty()) {
            String empty = state.snapshot().providerCount() == 0
                    ? "No recipe providers are online yet."
                    : selectedItem == null
                            ? "Select an ECHO item to inspect recipes and uses."
                            : emptyRecipeMessage(state);
            TerminalUi.wrap(context, graphics, empty, x + 10, y + 34, w - 20, TerminalUi.muted(context));
        }
    }

    private void drawDetailPane(TerminalRenderContext context, GuiGraphicsExtractor graphics, State state,
            TerminalRecipeEntry recipe, int x, int y, int w, int h, int mouseX, int mouseY) {
        lastDetailX = x;
        lastDetailY = y;
        lastDetailW = w;
        lastDetailH = h;
        TerminalUi.flatHudPanel(context, graphics, x, y, w, h, ACCENT);
        if (recipe == null) {
            lastDetailContentH = 80;
            TerminalUi.emptyState(context, graphics, x + 10, y + 34, w - 20,
                    "No recipe selected", "Select a result or use entry to inspect its process.", ACCENT);
            TerminalUi.scrollbar(context, graphics, x + w - 9, y + 8, h - 16, detailScroll,
                    Math.max(0, lastDetailContentH - h), ACCENT,
                    TerminalUi.inside(mouseX, mouseY, x, y, w, h));
            return;
        }
        int contentH = detailHeight(context, recipe, w);
        lastDetailContentH = contentH;
        detailScroll = TerminalUi.clampScroll(detailScroll, contentH, h);
        graphics.enableScissor(x + 6, y + 6, x + w - 6, y + h - 6);
        int cy = y + 10 - detailScroll;
        TerminalUi.line(context, graphics, recipe.title(), x + 12, cy, w - 24,
                recipe.locked() ? TerminalUi.warning(context) : ACCENT);
        cy += 14;
        TerminalUi.line(context, graphics, categoryTitle(state, recipe.categoryId()), x + 12, cy, w - 24,
                TerminalUi.muted(context));
        cy += 18;
        if (!recipe.machine().isEmpty()) {
            cy = drawSlotSection(context, graphics, "MACHINE", List.of(TerminalRecipeSlot.machine(recipe.machine())),
                    x + 12, cy, w - 24, mouseX, mouseY);
        }
        cy = drawSlotSection(context, graphics, "INPUTS", slots(recipe, TerminalRecipeSlot.Role.INPUT),
                x + 12, cy, w - 24, mouseX, mouseY);
        cy = drawSlotSection(context, graphics, "CATALYSTS", slots(recipe, TerminalRecipeSlot.Role.CATALYST),
                x + 12, cy, w - 24, mouseX, mouseY);
        cy = drawSlotSection(context, graphics, "OUTPUTS", slots(recipe, TerminalRecipeSlot.Role.OUTPUT),
                x + 12, cy, w - 24, mouseX, mouseY);
        if (recipe.processTicks() > 0) {
            TerminalUi.line(context, graphics,
                    String.format(Locale.ROOT, "Process time: %.1fs (%d ticks)", recipe.processTicks() / 20.0F, recipe.processTicks()),
                    x + 12, cy, w - 24, TerminalUi.muted(context));
            cy += 14;
        }
        if (!recipe.notes().isEmpty()) {
            cy = TerminalUi.sectionHeader(context, graphics, "Notes", "", x + 12, cy + 4, w - 24, ACCENT);
            for (TerminalRecipeNote note : recipe.notes()) {
                int color = note.warning() ? TerminalUi.warning(context) : TerminalUi.text(context);
                TerminalUi.wrap(context, graphics, note.text().getString(), x + 12, cy, w - 24, color);
                cy += TerminalUi.wrappedHeight(context, note.text().getString(), w - 24) + 4;
            }
        }
        graphics.disableScissor();
        TerminalUi.scrollbar(context, graphics, x + w - 9, y + 8, h - 16, detailScroll,
                Math.max(0, lastDetailContentH - h), ACCENT,
                TerminalUi.inside(mouseX, mouseY, x, y, w, h));
    }

    private int drawSlotSection(TerminalRenderContext context, GuiGraphicsExtractor graphics, String title,
            List<TerminalRecipeSlot> slots, int x, int y, int w, int mouseX, int mouseY) {
        if (slots.isEmpty()) {
            return y;
        }
        y = TerminalUi.sectionHeader(context, graphics, title, "", x, y, w, ACCENT);
        int sx = x;
        int sy = y;
        int step = 26;
        int columns = Math.max(1, w / step);
        int index = 0;
        for (TerminalRecipeSlot slot : slots) {
            ItemStack stack = visibleStack(slot);
            int slotX = sx + (index % columns) * step;
            int slotY = sy + (index / columns) * step;
            boolean inventoryRole = slot.role() == TerminalRecipeSlot.Role.INPUT
                    || slot.role() == TerminalRecipeSlot.Role.CATALYST;
            boolean present = !inventoryRole || slotAvailable(context, slot);
            int slotColor = present ? ACCENT : TerminalUi.danger(context);
            TerminalUi.itemSlot(context, graphics, stack, slotX, slotY, slotColor,
                    TerminalUi.inside(mouseX, mouseY, slotX, slotY, 20, 20));
            if (inventoryRole && !present) {
                graphics.outline(slotX - 1, slotY - 1, 22, 22, TerminalUi.danger(context));
            }
            if (!slot.label().isBlank()) {
                String label = inventoryRole && !present ? "Missing: " + slot.label() : slot.label();
                TerminalUi.line(context, graphics, label, slotX + 24, slotY + 6,
                        Math.max(30, w - (slotX - x) - 24),
                        inventoryRole && !present ? TerminalUi.danger(context) : TerminalUi.muted(context));
            }
            index++;
        }
        return y + ((slots.size() + columns - 1) / columns) * step + 8;
    }

    private State state(TerminalRenderContext context) {
        int refresh = (int) (System.currentTimeMillis() / (STATE_REFRESH_TICKS * 50L));
        if (refresh != lastRefreshTick) {
            lastRefreshTick = refresh;
            cachedState = new State(
                    TerminalRecipeRegistry.snapshot(context == null ? null : context.player()),
                    echoItems());
        }
        return cachedState;
    }

    private void normalizeSelection(State state) {
        if (categoryFilter != null && state.categories().stream().noneMatch(category -> category.id().equals(categoryFilter))) {
            categoryFilter = null;
        }
        List<ItemStack> items = filteredItems(state);
        if (items.isEmpty()) {
            selectedItem = null;
            selectedRecipeId = null;
            focusedItemIndex = 0;
            focusedRecipeIndex = 0;
            return;
        }
        focusedItemIndex = Math.max(0, Math.min(focusedItemIndex, items.size() - 1));
        if ((selectedItem == null || items.stream().noneMatch(stack -> stack.is(selectedItem)))) {
            selectedItem = items.get(0).getItem();
            selectedRecipeId = null;
            focusedItemIndex = 0;
        }
        List<TerminalRecipeEntry> recipes = filteredRecipes(state);
        if (recipes.isEmpty()) {
            selectedRecipeId = null;
            focusedRecipeIndex = 0;
            return;
        }
        focusedRecipeIndex = Math.max(0, Math.min(focusedRecipeIndex, recipes.size() - 1));
        if ((selectedRecipeId == null || recipes.stream().noneMatch(recipe -> recipe.id().equals(selectedRecipeId)))) {
            selectedRecipeId = recipes.get(0).id();
            focusedRecipeIndex = 0;
        }
    }

    private List<ItemStack> filteredItems(State state) {
        String query = normalizedSearch();
        return state.items().stream()
                .filter(stack -> query.isBlank() || stackMatches(stack, query) || state.recipes().stream()
                        .anyMatch(recipe -> recipe.mentions(stack.getItem()) && recipeMatches(recipe, query)))
                .toList();
    }

    private List<TerminalRecipeEntry> filteredRecipes(State state) {
        String query = normalizedSearch();
        List<TerminalRecipeEntry> base = switch (mode) {
            case RECIPES -> selectedItem == null ? state.recipes() : state.snapshot().recipesFor(selectedItem);
            case USES -> selectedItem == null ? state.recipes() : state.snapshot().usesFor(selectedItem);
            case SOURCES -> state.recipes().stream()
                    .filter(recipe -> SourceFilter.SOURCES.matches(recipe) || SourceFilter.REWARDS.matches(recipe))
                    .filter(recipe -> selectedItem == null || recipe.outputs(selectedItem) || recipe.mentions(selectedItem))
                    .toList();
            case INFO -> state.recipes().stream()
                    .filter(recipe -> !slots(recipe, TerminalRecipeSlot.Role.INFO).isEmpty() || !recipe.notes().isEmpty())
                    .filter(recipe -> selectedItem == null || recipe.mentions(selectedItem) || recipe.outputs(selectedItem))
                    .toList();
        };
        return base.stream()
                .filter(recipe -> mode == Mode.SOURCES || mode == Mode.INFO || sourceFilter.matches(recipe))
                .filter(recipe -> categoryFilter == null || recipe.categoryId().equals(categoryFilter))
                .filter(recipe -> query.isBlank() || recipeMatches(recipe, query)
                        || (selectedItem != null && stackMatches(new ItemStack(selectedItem), query)))
                .toList();
    }

    private String emptyRecipeMessage(State state) {
        return switch (mode) {
            case RECIPES -> infoOnly(state)
                    ? "No known recipe makes this item. Item notes are available in Info."
                    : "No known recipe makes this item.";
            case USES -> "No known ECHO recipe uses this item.";
            case SOURCES -> "No source or reward entry is known for this item.";
            case INFO -> "No guide notes are attached to this item.";
        };
    }

    private TerminalRecipeEntry selectedRecipe(State state) {
        if (selectedRecipeId == null) {
            return null;
        }
        return filteredRecipes(state).stream()
                .filter(recipe -> recipe.id().equals(selectedRecipeId))
                .findFirst()
                .orElse(null);
    }

    private boolean infoOnly(State state) {
        if (selectedItem == null) {
            return false;
        }
        return state.snapshot().recipesFor(selectedItem).isEmpty()
                && state.snapshot().usesFor(selectedItem).stream()
                        .anyMatch(recipe -> slots(recipe, TerminalRecipeSlot.Role.INFO).stream()
                                .anyMatch(slot -> slot.stacks().stream().anyMatch(stack -> stack.is(selectedItem))));
    }

    private static List<ItemStack> echoItems() {
        return BuiltInRegistries.ITEM.stream()
                .map(ItemStack::new)
                .filter(stack -> {
                    Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    return id != null && id.getNamespace().startsWith("echo");
                })
                .sorted(Comparator.comparing(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static boolean stackMatches(ItemStack stack, String query) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                || (id != null && id.toString().contains(query));
    }

    private boolean recipeMatches(TerminalRecipeEntry recipe, String query) {
        if (recipe.title().toLowerCase(Locale.ROOT).contains(query)
                || recipe.id().toString().contains(query)) {
            return true;
        }
        for (TerminalRecipeNote note : recipe.notes()) {
            if (note.text().getString().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        for (TerminalRecipeSlot slot : recipe.slots()) {
            if (slot.label().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
            for (ItemStack stack : slot.stacks()) {
                if (stackMatches(stack, query)) {
                    return true;
                }
            }
        }
        return stackMatches(recipe.machine(), query);
    }

    private static List<TerminalRecipeSlot> slots(TerminalRecipeEntry recipe, TerminalRecipeSlot.Role role) {
        return recipe.slots().stream().filter(slot -> slot.role() == role).toList();
    }

    private static ItemStack firstOutput(TerminalRecipeEntry recipe) {
        return slots(recipe, TerminalRecipeSlot.Role.OUTPUT).stream()
                .flatMap(slot -> slot.stacks().stream())
                .filter(stack -> !stack.isEmpty())
                .findFirst()
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack visibleStack(TerminalRecipeSlot slot) {
        List<ItemStack> stacks = slot.stacks().stream().filter(stack -> !stack.isEmpty()).toList();
        if (stacks.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int index = (int) ((System.currentTimeMillis() / 1000L) % stacks.size());
        return stacks.get(index);
    }

    private static boolean slotAvailable(TerminalRenderContext context, TerminalRecipeSlot slot) {
        if (context == null || context.player() == null || slot == null || slot.stacks().isEmpty()) {
            return false;
        }
        for (ItemStack candidate : slot.stacks()) {
            if (candidate.isEmpty()) {
                continue;
            }
            int found = 0;
            for (int inventorySlot = 0; inventorySlot < context.player().getInventory().getContainerSize(); inventorySlot++) {
                ItemStack held = context.player().getInventory().getItem(inventorySlot);
                if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, candidate)) {
                    found += held.getCount();
                    if (found >= candidate.getCount()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean beginScrollbarDrag(double mouseX, double mouseY) {
        TerminalScrollbar.Metrics controls = controlScrollbar();
        if (controls.insideTrack(mouseX, mouseY)) {
            scrollbarDragPane = ScrollPane.CONTROLS;
            scrollbarDragOffset = controls.dragOffset(mouseY);
            controlScroll = TerminalUi.clampScroll(controls.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastControlContentH, lastControlH);
            return true;
        }
        TerminalScrollbar.Metrics items = itemScrollbar();
        if (items.insideTrack(mouseX, mouseY)) {
            scrollbarDragPane = ScrollPane.ITEMS;
            scrollbarDragOffset = items.dragOffset(mouseY);
            itemScroll = TerminalUi.clampScroll(items.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastItemContentH, lastItemH);
            return true;
        }
        TerminalScrollbar.Metrics recipes = recipeScrollbar();
        if (recipes.insideTrack(mouseX, mouseY)) {
            scrollbarDragPane = ScrollPane.RECIPES;
            scrollbarDragOffset = recipes.dragOffset(mouseY);
            recipeScroll = TerminalUi.clampScroll(recipes.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastRecipeContentH, lastRecipeH);
            return true;
        }
        TerminalScrollbar.Metrics detail = detailScrollbar();
        if (detail.insideTrack(mouseX, mouseY)) {
            scrollbarDragPane = ScrollPane.DETAIL;
            scrollbarDragOffset = detail.dragOffset(mouseY);
            detailScroll = TerminalUi.clampScroll(detail.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastDetailContentH, lastDetailH);
            return true;
        }
        return false;
    }

    private void updateScrollbarDrag(double mouseY) {
        if (scrollbarDragPane == ScrollPane.CONTROLS) {
            TerminalScrollbar.Metrics controls = controlScrollbar();
            controlScroll = TerminalUi.clampScroll(controls.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastControlContentH, lastControlH);
        } else if (scrollbarDragPane == ScrollPane.ITEMS) {
            TerminalScrollbar.Metrics items = itemScrollbar();
            itemScroll = TerminalUi.clampScroll(items.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastItemContentH, lastItemH);
        } else if (scrollbarDragPane == ScrollPane.RECIPES) {
            TerminalScrollbar.Metrics recipes = recipeScrollbar();
            recipeScroll = TerminalUi.clampScroll(recipes.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastRecipeContentH, lastRecipeH);
        } else if (scrollbarDragPane == ScrollPane.DETAIL) {
            TerminalScrollbar.Metrics detail = detailScrollbar();
            detailScroll = TerminalUi.clampScroll(detail.scrollForMouse(mouseY, scrollbarDragOffset),
                    lastDetailContentH, lastDetailH);
        }
    }

    private TerminalScrollbar.Metrics controlScrollbar() {
        return TerminalScrollbar.vertical(lastControlX + lastControlW - 9, lastControlY, 7, lastControlH,
                controlScroll, Math.max(0, lastControlContentH - lastControlH));
    }

    private TerminalScrollbar.Metrics itemScrollbar() {
        return TerminalScrollbar.vertical(lastItemX + lastItemW - 9, lastItemY + 28, 7,
                Math.max(0, lastItemH - 36), itemScroll, Math.max(0, lastItemContentH - lastItemH));
    }

    private TerminalScrollbar.Metrics recipeScrollbar() {
        return TerminalScrollbar.vertical(lastRecipeX + lastRecipeW - 9, lastRecipeY + 29, 7,
                Math.max(0, lastRecipeH - 37), recipeScroll, Math.max(0, lastRecipeContentH - lastRecipeH));
    }

    private TerminalScrollbar.Metrics detailScrollbar() {
        return TerminalScrollbar.vertical(lastDetailX + lastDetailW - 9, lastDetailY + 8, 7,
                Math.max(0, lastDetailH - 16), detailScroll, Math.max(0, lastDetailContentH - lastDetailH));
    }

    @Override
    public boolean mouseDragged(TerminalRenderContext context, double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        if (button != 0 || scrollbarDragPane == ScrollPane.NONE) {
            return false;
        }
        updateScrollbarDrag(mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(TerminalRenderContext context, double mouseX, double mouseY, int button) {
        if (button != 0 || scrollbarDragPane == ScrollPane.NONE) {
            return false;
        }
        scrollbarDragPane = ScrollPane.NONE;
        return true;
    }

    private String categoryTitle(State state, Identifier id) {
        TerminalRecipeCategory category = state.snapshot().categoryMap().get(id);
        return category == null ? id == null ? "Unknown" : id.getPath() : category.title();
    }

    private int detailHeight(TerminalRenderContext context, TerminalRecipeEntry recipe, int width) {
        int height = 58;
        int columns = Math.max(1, Math.max(40, width - 24) / 26);
        int slotRows = 0;
        if (!recipe.machine().isEmpty()) {
            slotRows++;
        }
        slotRows += sectionRows(recipe, TerminalRecipeSlot.Role.INPUT, columns);
        slotRows += sectionRows(recipe, TerminalRecipeSlot.Role.CATALYST, columns);
        slotRows += sectionRows(recipe, TerminalRecipeSlot.Role.OUTPUT, columns);
        height += slotRows * 26 + 68;
        if (recipe.processTicks() > 0) {
            height += 14;
        }
        for (TerminalRecipeNote note : recipe.notes()) {
            height += TerminalUi.wrappedHeight(context, note.text().getString(), Math.max(40, width - 24)) + 4;
        }
        return height + 30;
    }

    private static int sectionRows(TerminalRecipeEntry recipe, TerminalRecipeSlot.Role role, int columns) {
        int count = slots(recipe, role).size();
        return count == 0 ? 0 : 1 + (count + columns - 1) / columns;
    }

    private String normalizedSearch() {
        return searchText == null ? "" : searchText.toLowerCase(Locale.ROOT).strip();
    }

    private void resetScroll() {
        itemScroll = 0;
        recipeScroll = 0;
        detailScroll = 0;
        selectedRecipeId = null;
        focusedItemIndex = 0;
        focusedRecipeIndex = 0;
    }

    private void setMode(Mode candidate) {
        mode = candidate;
        selectedRecipeId = null;
        focusedRecipeIndex = 0;
        recipeScroll = 0;
        detailScroll = 0;
    }

    private void moveFocusedItem(State state, int delta) {
        List<ItemStack> items = filteredItems(state);
        if (items.isEmpty()) {
            return;
        }
        focusedItemIndex = Math.max(0, Math.min(items.size() - 1, focusedItemIndex + delta));
        selectedItem = items.get(focusedItemIndex).getItem();
        selectedRecipeId = null;
        recipeScroll = 0;
        detailScroll = 0;
        int row = focusedItemIndex / Math.max(1, lastItemColumns);
        itemScroll = TerminalUi.clampScroll(Math.max(0, row * 24 - 12), lastItemContentH, lastItemH);
    }

    private void moveFocusedRecipe(State state, int delta) {
        List<TerminalRecipeEntry> recipes = filteredRecipes(state);
        if (recipes.isEmpty()) {
            return;
        }
        focusedRecipeIndex = Math.max(0, Math.min(recipes.size() - 1, focusedRecipeIndex + delta));
        selectedRecipeId = recipes.get(focusedRecipeIndex).id();
        detailScroll = 0;
        recipeScroll = TerminalUi.clampScroll(Math.max(0, focusedRecipeIndex * 34 - 12), lastRecipeContentH, lastRecipeH);
    }

    private void selectFocusedItem(State state, Mode selectedMode) {
        List<ItemStack> items = filteredItems(state);
        if (items.isEmpty()) {
            return;
        }
        focusedItemIndex = Math.max(0, Math.min(items.size() - 1, focusedItemIndex));
        selectedItem = items.get(focusedItemIndex).getItem();
        setMode(selectedMode);
        focus = Focus.RECIPES;
    }

    private void selectFocusedRecipe(State state) {
        List<TerminalRecipeEntry> recipes = filteredRecipes(state);
        if (recipes.isEmpty()) {
            return;
        }
        focusedRecipeIndex = Math.max(0, Math.min(recipes.size() - 1, focusedRecipeIndex));
        selectedRecipeId = recipes.get(focusedRecipeIndex).id();
        focus = Focus.DETAIL;
        detailScroll = 0;
    }

    private static Font font(TerminalRenderContext context) {
        return context != null && context.minecraft() != null ? context.minecraft().font : Minecraft.getInstance().font;
    }

    private static int sourceFilterRows(int width) {
        int rows = 1;
        int x = 0;
        for (SourceFilter filter : SourceFilter.values()) {
            int chipW = filter.width();
            if (x > 0 && x + chipW > width) {
                rows++;
                x = 0;
            }
            x += chipW + 4;
        }
        return rows;
    }

    private enum Mode {
        RECIPES("Recipes"),
        USES("Uses"),
        SOURCES("Sources"),
        INFO("Info");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    private enum SourceFilter {
        PROCESSES("Processes", 74) {
            @Override
            boolean matches(TerminalRecipeEntry recipe) {
                return !sourceRecipe(recipe) && !rewardRecipe(recipe);
            }
        },
        ALL("All", 42) {
            @Override
            boolean matches(TerminalRecipeEntry recipe) {
                return true;
            }
        },
        SOURCES("Sources", 62) {
            @Override
            boolean matches(TerminalRecipeEntry recipe) {
                return sourceRecipe(recipe) && !rewardRecipe(recipe);
            }
        },
        REWARDS("Rewards", 68) {
            @Override
            boolean matches(TerminalRecipeEntry recipe) {
                return rewardRecipe(recipe);
            }
        };

        private final String label;
        private final int width;

        SourceFilter(String label, int width) {
            this.label = label;
            this.width = width;
        }

        abstract boolean matches(TerminalRecipeEntry recipe);

        String label() {
            return label;
        }

        int width() {
            return width;
        }

        static SourceFilter fromName(String name) {
            if (name != null) {
                for (SourceFilter filter : values()) {
                    if (filter.name().equalsIgnoreCase(name) || filter.label.equalsIgnoreCase(name.strip())) {
                        return filter;
                    }
                }
            }
            return PROCESSES;
        }

        private static boolean sourceRecipe(TerminalRecipeEntry recipe) {
            String categoryPath = recipe == null ? "" : recipe.categoryId().getPath().toLowerCase(Locale.ROOT);
            return categoryPath.equals("recipe/sources")
                    || categoryPath.endsWith("/sources")
                    || categoryPath.contains("source_cards")
                    || haystack(recipe).contains("source type:");
        }

        private static boolean rewardRecipe(TerminalRecipeEntry recipe) {
            String text = haystack(recipe);
            return text.contains("mission reward")
                    || text.contains("mission_rewards")
                    || text.contains("source/mission_reward")
                    || text.contains("recipe/mission_rewards");
        }

        private static String haystack(TerminalRecipeEntry recipe) {
            if (recipe == null) {
                return "";
            }
            StringBuilder builder = new StringBuilder()
                    .append(recipe.id()).append(' ')
                    .append(recipe.categoryId()).append(' ')
                    .append(recipe.title()).append(' ');
            for (TerminalRecipeNote note : recipe.notes()) {
                builder.append(note.text().getString()).append(' ');
            }
            return builder.toString().toLowerCase(Locale.ROOT);
        }
    }

    private enum Focus {
        ITEMS,
        RECIPES,
        DETAIL
    }

    private enum ScrollPane {
        NONE,
        CONTROLS,
        ITEMS,
        RECIPES,
        DETAIL
    }

    private record State(
            TerminalRecipeSnapshot snapshot,
            List<ItemStack> items) {
        private static State empty() {
            return new State(TerminalRecipeSnapshot.empty(), List.of());
        }

        private List<TerminalRecipeCategory> categories() {
            return snapshot.categories();
        }

        private List<TerminalRecipeEntry> recipes() {
            return snapshot.recipes();
        }
    }

    private record Hitbox(int x, int y, int w, int h, ClickAction action) {
    }

    @FunctionalInterface
    private interface ClickAction {
        void click(int button);
    }
}
