package com.knoxhack.echoindex.integration;

import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.EchoIndexClient;
import com.knoxhack.echoindex.IndexIds;
import com.knoxhack.echoindex.client.IndexDiagnosticsScreen;
import com.knoxhack.echoindex.client.IndexRecipeTraceState;
import com.knoxhack.echoindex.client.IndexRecipeUi;
import com.knoxhack.echoindex.network.IndexActionPacket;
import com.knoxhack.echoindex.service.ClientIndexState;
import com.knoxhack.echoindex.service.IndexRecipePlan;
import com.knoxhack.echoindex.service.IndexRecipePlanner;
import com.knoxhack.echoindex.service.IndexRecipeSnapshot;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class IndexTerminalClientIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFF66E8FF;
    public static final Identifier TAB_ID = EchoIndex.id("terminal/index");

    private IndexTerminalClientIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalTab tab = new IndexArchiveTab();
        TerminalTabRegistry.register(tab);
        TerminalNavigationProfiles.register(tab.descriptor().id(), TerminalNavigationProfile.intel(146));
        TerminalRecipeRegistry.register(IndexTerminalRecipeProvider.INSTANCE);
    }

    private static final class IndexArchiveTab implements ClientTerminalTab {
        private final TerminalTabDescriptor descriptor = new TerminalTabDescriptor(TAB_ID, "INDEX", 146, ACCENT);
        private final TerminalTabChrome chrome = TerminalTabChrome.of(
                "Index", TerminalTabChrome.GROUP_FIELD, "IX", "Shared codex and recipe archive", 146);
        private final List<Hit> hits = new ArrayList<>();
        private Identifier selectedCategory = IndexIds.CATEGORY_TUTORIALS;
        private Identifier selectedEntry = IndexIds.ENTRY_OVERVIEW;
        private String query = "";
        private TerminalFilter filter = TerminalFilter.ALL;
        private int recipePage;
        private int categoryScroll;
        private int entryScroll;
        private int detailScroll;
        private int lastCategoryX;
        private int lastCategoryY;
        private int lastCategoryW;
        private int lastCategoryH;
        private int lastCategoryContentH;
        private int lastEntryX;
        private int lastEntryY;
        private int lastEntryW;
        private int lastEntryH;
        private int lastEntryContentH;
        private int lastDetailX;
        private int lastDetailY;
        private int lastDetailW;
        private int lastDetailH;
        private int lastDetailContentH;

        @Override
        public TerminalTabDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public TerminalTabChrome chrome() {
            return chrome;
        }

        @Override
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                float partialTick) {
            hits.clear();
            List<IndexCategory> categories = IndexService.INSTANCE.categories(context.player());
            List<IndexEntry> entries = visibleEntries(context);
            IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshot(context.player());
            ensureSelection(categories, entries);

            int x = context.contentX() + 12;
            int y = context.contentY() + 10;
            int w = context.contentWidth() - 24;
            y = TerminalUi.sectionHeader(context, graphics, "ECHO: INDEX", "TERMINAL // CODEX", x, y, w, ACCENT);

            int searchH = 32;
            TerminalUi.flatHudPanel(context, graphics, x, y, w, searchH, ACCENT);
            TerminalUi.line(context, graphics, query.isBlank() ? "Search ECHO: Index..." : query,
                    x + 12, y + 11, w - 270, query.isBlank() ? TerminalUi.muted(context) : TerminalUi.text(context));
            int fx = x + w - 420;
            filterChip(context, graphics, fx, y + 8, 42, TerminalFilter.ALL, mouseX, mouseY);
            filterChip(context, graphics, fx + 46, y + 8, 78, TerminalFilter.DISCOVERED, mouseX, mouseY);
            filterChip(context, graphics, fx + 128, y + 8, 76, TerminalFilter.BOOKMARKED, mouseX, mouseY);
            filterChip(context, graphics, fx + 208, y + 8, 64, TerminalFilter.RECIPES, mouseX, mouseY);
            filterChip(context, graphics, fx + 276, y + 8, 72, TerminalFilter.WARNINGS, mouseX, mouseY);
            chip(context, graphics, fx + 352, y + 8, 24, "R", false, mouseX, mouseY, HitKind.REFRESH, null);
            chip(context, graphics, fx + 380, y + 8, 38,
                    "#" + snapshot.generation(), false, mouseX, mouseY, HitKind.DIAGNOSTICS, null);

            int bodyY = y + searchH + 10;
            int bodyH = Math.max(260, context.contentHeight() - 78);
            int categoryW = Math.min(170, Math.max(132, w / 5));
            int entryW = Math.min(230, Math.max(176, w / 4));
            int detailW = Math.max(240, w - categoryW - entryW - 24);

            drawCategories(context, graphics, categories, x, bodyY, categoryW, bodyH, mouseX, mouseY);
            drawEntries(context, graphics, entries, x + categoryW + 10, bodyY, entryW, bodyH, mouseX, mouseY);
            drawDetail(context, graphics, selected(entries), x + categoryW + entryW + 20, bodyY, detailW, bodyH,
                    mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(TerminalRenderContext context, double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            for (Hit hit : hits) {
                if (!TerminalUi.inside(mouseX, mouseY, hit.x(), hit.y(), hit.w(), hit.h())) {
                    continue;
                }
                if (hit.kind() == HitKind.CATEGORY && hit.id() != null) {
                    selectedCategory = hit.id();
                    selectedEntry = null;
                    recipePage = 0;
                    entryScroll = 0;
                    detailScroll = 0;
                    context.playCommandSound();
                    return true;
                }
                if (hit.kind() == HitKind.ENTRY && hit.id() != null) {
                    selectedEntry = hit.id();
                    recipePage = 0;
                    detailScroll = 0;
                    context.playCommandSound();
                    return true;
                }
                if (hit.kind() == HitKind.FILTER && hit.id() != null) {
                    filter = TerminalFilter.from(hit.id());
                    selectedEntry = null;
                    recipePage = 0;
                    categoryScroll = 0;
                    entryScroll = 0;
                    detailScroll = 0;
                    context.playCommandSound();
                    return true;
                }
                if (hit.kind() == HitKind.DIAGNOSTICS) {
                    EchoNativeLoadStatus lifecycleStatus = EchoIndexClient.publishNativeScreenLifecycle(
                            "open",
                            "index.terminal_archive.open_diagnostics",
                            getClass().getName(),
                            Map.of(
                                    "targetScreenClass", IndexDiagnosticsScreen.class.getName(),
                                    "transitionSource", "index_terminal_archive"));
                    if (EchoIndexClient.nativeLoaderClientActiveForScreens()
                            && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
                        return false;
                    }
                    Minecraft.getInstance().setScreen(new IndexDiagnosticsScreen());
                    context.playCommandSound();
                    return true;
                }
                if (hit.kind() == HitKind.REFRESH) {
                    IndexService.INSTANCE.rebuildRecipes(context.player(), "terminal index refresh button");
                    EchoNetClientActions.sendServerboundAction(
                            new IndexActionPacket(IndexActionPacket.Action.REQUEST_SYNC, null));
                    recipePage = 0;
                    detailScroll = 0;
                    context.playCommandSound();
                    return true;
                }
                if (hit.kind() == HitKind.RECIPE_PAGE_PREV) {
                    recipePage = Math.max(0, recipePage - 1);
                    context.playCommandSound();
                    return true;
                }
                if (hit.kind() == HitKind.RECIPE_PAGE_NEXT) {
                    recipePage++;
                    context.playCommandSound();
                    return true;
                }
                if ((hit.kind() == HitKind.PIN_RECIPE || hit.kind() == HitKind.UNPIN_RECIPE) && hit.id() != null) {
                    EchoNetClientActions.sendServerboundAction(new IndexActionPacket(
                            hit.kind() == HitKind.PIN_RECIPE
                                    ? IndexActionPacket.Action.PIN_RECIPE
                                    : IndexActionPacket.Action.UNPIN_RECIPE,
                            hit.id()));
                    context.playCommandSound();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseScrolled(TerminalRenderContext context, double mouseX, double mouseY, double delta) {
            int amount = (int) Math.round(delta * 24.0D);
            if (TerminalUi.inside(mouseX, mouseY, lastCategoryX, lastCategoryY, lastCategoryW, lastCategoryH)) {
                categoryScroll = TerminalUi.clampScroll(categoryScroll - amount, lastCategoryContentH, lastCategoryH);
                return true;
            }
            if (TerminalUi.inside(mouseX, mouseY, lastEntryX, lastEntryY, lastEntryW, lastEntryH)) {
                entryScroll = TerminalUi.clampScroll(entryScroll - amount, lastEntryContentH, lastEntryH);
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
            if (key == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                selectedEntry = null;
                recipePage = 0;
                entryScroll = 0;
                detailScroll = 0;
                return true;
            }
            if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_ESCAPE) {
                if (!query.isBlank()) {
                    query = "";
                    selectedEntry = null;
                    recipePage = 0;
                    entryScroll = 0;
                    detailScroll = 0;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean charTyped(TerminalRenderContext context, CharacterEvent event) {
            if (!event.isAllowedChatCharacter() || query.length() >= 48) {
                return false;
            }
            query += event.codepointAsString();
            selectedEntry = null;
            recipePage = 0;
            entryScroll = 0;
            detailScroll = 0;
            return true;
        }

        @Override
        public int contentHeight(TerminalRenderContext context) {
            return context.contentHeight();
        }

        private void drawCategories(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<IndexCategory> categories, int x, int y, int w, int h, int mouseX, int mouseY) {
            lastCategoryX = x;
            lastCategoryY = y;
            lastCategoryW = w;
            lastCategoryH = h;
            lastCategoryContentH = 34 + categories.size() * 35;
            categoryScroll = TerminalUi.clampScroll(categoryScroll, lastCategoryContentH, h);
            TerminalUi.flatHudPanel(context, graphics, x, y, w, h, ACCENT);
            TerminalUi.line(context, graphics, "CATEGORIES", x + 10, y + 10, w - 20, TerminalUi.accent(context));
            graphics.enableScissor(x + 6, y + 26, x + w - 6, y + h - 6);
            int cy = y + 30 - categoryScroll;
            for (IndexCategory category : categories) {
                boolean selected = category.id().equals(selectedCategory);
                boolean hover = TerminalUi.inside(mouseX, mouseY, x + 8, cy, w - 16, 30);
                if (cy + 30 >= y + 26 && cy <= y + h - 6) {
                    graphics.fill(x + 8, cy, x + w - 8, cy + 30, selected ? 0xFF123241 : hover ? 0xAA102630 : 0x66071117);
                    graphics.outline(x + 8, cy, w - 16, 30, selected ? ACCENT : 0x4438DFF4);
                    graphics.item(category.icon(), x + 14, cy + 7);
                    TerminalUi.line(context, graphics, title(category.titleKey()), x + 36, cy + 6, w - 46,
                            selected ? TerminalUi.text(context) : TerminalUi.muted(context));
                    hits.add(new Hit(x + 8, cy, w - 16, 30, HitKind.CATEGORY, category.id()));
                }
                cy += 35;
            }
            graphics.disableScissor();
            TerminalUi.scrollbar(context, graphics, x + w - 9, y + 28, h - 36, categoryScroll,
                    Math.max(0, lastCategoryContentH - h), ACCENT,
                    TerminalUi.inside(mouseX, mouseY, x, y, w, h));
        }

        private void drawEntries(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                List<IndexEntry> entries, int x, int y, int w, int h, int mouseX, int mouseY) {
            lastEntryX = x;
            lastEntryY = y;
            lastEntryW = w;
            lastEntryH = h;
            lastEntryContentH = 34 + entries.size() * 40;
            entryScroll = TerminalUi.clampScroll(entryScroll, lastEntryContentH, h);
            TerminalUi.flatHudPanel(context, graphics, x, y, w, h, ACCENT);
            TerminalUi.line(context, graphics, "ENTRIES", x + 10, y + 10, w - 20, TerminalUi.accent(context));
            graphics.enableScissor(x + 6, y + 26, x + w - 6, y + h - 6);
            int cy = y + 30 - entryScroll;
            for (IndexEntry entry : entries) {
                boolean selected = entry.id().equals(selectedEntry);
                boolean hover = TerminalUi.inside(mouseX, mouseY, x + 8, cy, w - 16, 36);
                if (cy + 36 >= y + 26 && cy <= y + h - 6) {
                    graphics.fill(x + 8, cy, x + w - 8, cy + 36, selected ? 0xFF123241 : hover ? 0xAA102630 : 0x66071117);
                    graphics.outline(x + 8, cy, w - 16, 36, selected ? ACCENT : 0x4438DFF4);
                    graphics.item(entry.icon(), x + 14, cy + 10);
                    TerminalUi.line(context, graphics, title(entry.titleKey()), x + 36, cy + 6, w - 46,
                            selected ? TerminalUi.text(context) : TerminalUi.text(context));
                    TerminalUi.line(context, graphics, summary(entry.summaryKey()), x + 36, cy + 19, w - 46,
                            TerminalUi.muted(context));
                    hits.add(new Hit(x + 8, cy, w - 16, 36, HitKind.ENTRY, entry.id()));
                }
                cy += 40;
            }
            graphics.disableScissor();
            TerminalUi.scrollbar(context, graphics, x + w - 9, y + 28, h - 36, entryScroll,
                    Math.max(0, lastEntryContentH - h), ACCENT,
                    TerminalUi.inside(mouseX, mouseY, x, y, w, h));
        }

        private void drawDetail(TerminalRenderContext context, GuiGraphicsExtractor graphics, IndexEntry entry,
                int x, int y, int w, int h, int mouseX, int mouseY) {
            lastDetailX = x;
            lastDetailY = y;
            lastDetailW = w;
            lastDetailH = h;
            lastDetailContentH = detailContentHeight(context, entry, w);
            detailScroll = TerminalUi.clampScroll(detailScroll, lastDetailContentH, h);
            TerminalUi.flatHudPanel(context, graphics, x, y, w, h, ACCENT);
            if (entry == null) {
                TerminalUi.wrap(context, graphics, "No Index entry matches the current filters.",
                        x + 16, y + 26, w - 32, TerminalUi.muted(context));
                return;
            }
            graphics.enableScissor(x + 6, y + 6, x + w - 6, y + h - 6);
            int cy = y + 18 - detailScroll;
            graphics.item(entry.icon(), x + 16, cy);
            TerminalUi.line(context, graphics, title(entry.titleKey()), x + 42, cy - 4, w - 58,
                    TerminalUi.accent(context));
            TerminalUi.line(context, graphics, entry.sourceModId().toUpperCase(Locale.ROOT),
                    x + 42, cy + 10, w - 58, TerminalUi.muted(context));
            cy += 38;
            cy = drawRenderCorePreview(context, graphics, entry, x + 14, cy, w - 28, mouseX, mouseY);
            cy = TerminalUi.sectionHeader(context, graphics, "SUMMARY", entry.defaultState().name(),
                    x + 14, cy, w - 28, ACCENT);
            TerminalUi.wrap(context, graphics, summary(entry.summaryKey()), x + 14, cy, w - 28, TerminalUi.text(context));
            cy += Math.max(34, TerminalUi.wrappedHeight(context, summary(entry.summaryKey()), w - 28)) + 12;
            cy = TerminalUi.sectionHeader(context, graphics, "ARCHIVE", entry.categoryId().toString(),
                    x + 14, cy, w - 28, ACCENT);
            TerminalUi.wrap(context, graphics, body(entry.bodyKey()), x + 14, cy, w - 28, TerminalUi.text(context));
            cy += Math.max(54, TerminalUi.wrappedHeight(context, body(entry.bodyKey()), w - 28)) + 12;
            TerminalUi.sectionHeader(context, graphics, "LINKS",
                    entry.relatedEntries().size() + " related / " + entry.linkedItems().size() + " items",
                    x + 14, cy, w - 28, ACCENT);
            cy += 22;
            drawLinkedRecipes(context, graphics, entry, x + 14, cy, w - 28, mouseX, mouseY);
            graphics.disableScissor();
            TerminalUi.scrollbar(context, graphics, x + w - 9, y + 8, h - 16, detailScroll,
                    Math.max(0, lastDetailContentH - h), ACCENT,
                    TerminalUi.inside(mouseX, mouseY, x, y, w, h));
        }

        private int detailContentHeight(TerminalRenderContext context, IndexEntry entry, int width) {
            if (entry == null) {
                return 96;
            }
            int inner = Math.max(60, width - 28);
            int height = 96;
            height += Math.max(34, TerminalUi.wrappedHeight(context, summary(entry.summaryKey()), inner)) + 34;
            height += Math.max(54, TerminalUi.wrappedHeight(context, body(entry.bodyKey()), inner)) + 48;
            int linked = linkedRecipes(context, entry, IndexRecipeUi.ViewMode.RECIPES).size()
                    + linkedRecipes(context, entry, IndexRecipeUi.ViewMode.USES).size()
                    + linkedRecipes(context, entry, IndexRecipeUi.ViewMode.SOURCES).size();
            height += linked == 0 ? 64 : 28 + Math.min(1, linked) * 174;
            if (EchoRuntimeModules.isLoaded("echorendercore")) {
                height += 84;
            }
            return height;
        }

        private List<IndexEntry> visibleEntries(TerminalRenderContext context) {
            String lower = query.toLowerCase(Locale.ROOT);
            IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshot(context.player());
            return IndexService.INSTANCE.entries(context.player()).stream()
                    .filter(entry -> selectedCategory == null || selectedCategory.equals(entry.categoryId()) || !query.isBlank())
                    .filter(entry -> lower.isBlank() || matches(entry, lower))
                    .filter(entry -> filterMatches(context, snapshot, entry))
                    .sorted(Comparator.comparingInt(IndexEntry::sortOrder).thenComparing(entry -> entry.id().toString()))
                    .toList();
        }

        private void ensureSelection(List<IndexCategory> categories, List<IndexEntry> entries) {
            if (selectedCategory == null && !categories.isEmpty()) {
                selectedCategory = categories.getFirst().id();
            }
            if (selectedEntry == null || entries.stream().noneMatch(entry -> entry.id().equals(selectedEntry))) {
                selectedEntry = entries.isEmpty() ? null : entries.getFirst().id();
            }
        }

        private IndexEntry selected(List<IndexEntry> entries) {
            return entries.stream().filter(entry -> entry.id().equals(selectedEntry)).findFirst()
                    .orElse(entries.isEmpty() ? null : entries.getFirst());
        }

        private boolean matches(IndexEntry entry, String lower) {
            return entry.id().toString().toLowerCase(Locale.ROOT).contains(lower)
                    || title(entry.titleKey()).toLowerCase(Locale.ROOT).contains(lower)
                    || summary(entry.summaryKey()).toLowerCase(Locale.ROOT).contains(lower)
                    || entry.tags().stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(lower));
        }

        private boolean filterMatches(TerminalRenderContext context, IndexRecipeSnapshot snapshot, IndexEntry entry) {
            return switch (filter) {
                case ALL -> true;
                case DISCOVERED -> {
                    IndexEntryState state = IndexService.INSTANCE.state(context.player(), entry.id());
                    yield state == IndexEntryState.DISCOVERED
                            || state == IndexEntryState.COMPLETED
                            || state == IndexEntryState.ARCHIVED;
                }
                case BOOKMARKED -> IndexService.INSTANCE.isBookmarked(context.player(), entry.id());
                case RECIPES -> !linkedRecipes(context, entry, IndexRecipeUi.ViewMode.RECIPES).isEmpty()
                        || !linkedRecipes(context, entry, IndexRecipeUi.ViewMode.SOURCES).isEmpty();
                case WARNINGS -> !snapshot.warnings().isEmpty();
            };
        }

        private void drawLinkedRecipes(TerminalRenderContext context, GuiGraphicsExtractor graphics, IndexEntry entry,
                int x, int y, int w, int mouseX, int mouseY) {
            List<IndexRecipeView> recipes = linkedRecipes(context, entry, IndexRecipeUi.ViewMode.RECIPES);
            List<IndexRecipeView> uses = linkedRecipes(context, entry, IndexRecipeUi.ViewMode.USES);
            List<IndexRecipeView> sources = linkedRecipes(context, entry, IndexRecipeUi.ViewMode.SOURCES);
            List<LabeledRecipe> cards = new ArrayList<>();
            recipes.forEach(view -> cards.add(new LabeledRecipe("Recipes", view)));
            uses.forEach(view -> cards.add(new LabeledRecipe("Uses", view)));
            sources.forEach(view -> cards.add(new LabeledRecipe("Sources", view)));
            int total = cards.size();
            int warnings = IndexService.INSTANCE.recipeSnapshot(context.player()).warnings().size();
            TerminalUi.sectionHeader(context, graphics, "INDEX DATA",
                    total + " linked / " + warnings + " warning(s)", x, y, w, ACCENT);
            if (warnings > 0) {
                chip(context, graphics, x + w - 82, y + 2, 76, warnings + " WARN", true,
                        mouseX, mouseY, HitKind.DIAGNOSTICS, null);
            }
            if (cards.isEmpty()) {
                TerminalUi.wrap(context, graphics, "No linked recipes, uses, or source cards are indexed for this entry.",
                        x, y + 26, w, TerminalUi.muted(context));
                return;
            }
            int pageSize = 1;
            int pageCount = Math.max(1, (cards.size() + pageSize - 1) / pageSize);
            recipePage = Math.max(0, Math.min(recipePage, pageCount - 1));
            if (pageCount > 1) {
                chip(context, graphics, x + w - 154, y + 2, 24, "<", recipePage > 0,
                        mouseX, mouseY, HitKind.RECIPE_PAGE_PREV, null);
                chip(context, graphics, x + w - 126, y + 2, 62, (recipePage + 1) + "/" + pageCount,
                        false, mouseX, mouseY, HitKind.NONE, null);
                chip(context, graphics, x + w - 60, y + 2, 24, ">", recipePage + 1 < pageCount,
                        mouseX, mouseY, HitKind.RECIPE_PAGE_NEXT, null);
            }
            int cy = y + 24;
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            int start = recipePage * pageSize;
            int end = Math.min(cards.size(), start + pageSize);
            for (int i = start; i < end; i++) {
                LabeledRecipe card = cards.get(i);
                IndexRecipePlan plan = IndexRecipePlanner.plan(context.player(), card.view());
                graphics.text(font, card.label() + " (" + (i + 1) + " / " + cards.size() + ") "
                                + IndexRecipeUi.statusLabel(plan, false),
                        x, cy, IndexRecipeUi.statusColor(plan, false), false);
                boolean pinned = ClientIndexState.isRecipePinned(card.view().id());
                chip(context, graphics, x + w - 52, cy - 3, 48, pinned ? "Unpin" : "Pin", pinned,
                        mouseX, mouseY, pinned ? HitKind.UNPIN_RECIPE : HitKind.PIN_RECIPE, card.view().id());
                cy += 14;
                int cardH = 128;
                IndexRecipeUi.drawRecipeCard(graphics, font, card.view(), x, cy, w, cardH,
                        ItemStack.EMPTY, mouseX, mouseY, null);
                cy += cardH;
                cy = drawDependencySummary(context, graphics, card.view(), plan, x, cy + 4, w);
                cy += 8;
            }
        }

        private int drawDependencySummary(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                IndexRecipeView recipe, IndexRecipePlan plan, int x, int y, int w) {
            IndexRecipeTraceState.Trace trace = IndexRecipeTraceState.preview(
                    IndexRecipeUi.recipeIcon(recipe, ItemStack.EMPTY), recipe, plan);
            if (trace.entries().isEmpty()) {
                TerminalUi.line(context, graphics, "Dependencies: none missing", x + 4, y, w - 8,
                        TerminalUi.muted(context));
                return y + 14;
            }
            StringBuilder line = new StringBuilder("Missing: ");
            int shown = 0;
            for (IndexRecipeTraceState.TraceEntry entry : trace.entries()) {
                if (shown > 0) {
                    line.append(", ");
                }
                line.append(entry.stack().getHoverName().getString()).append(' ').append(entry.countLabel());
                shown++;
                if (shown >= 3) {
                    break;
                }
            }
            if (trace.entries().size() > shown) {
                line.append(", +").append(trace.entries().size() - shown);
            }
            TerminalUi.line(context, graphics, IndexRecipeUi.trim(Minecraft.getInstance().font, line.toString(), w - 8),
                    x + 4, y, w - 8, TerminalUi.muted(context));
            return y + 14;
        }

        private List<IndexRecipeView> linkedRecipes(TerminalRenderContext context, IndexEntry entry,
                IndexRecipeUi.ViewMode mode) {
            List<IndexRecipeView> views = new ArrayList<>();
            for (ItemStack stack : linkedStacks(entry)) {
                for (IndexRecipeView view : IndexRecipeUi.viewsFor(context.player(), stack.getItem(), mode)) {
                    if (views.stream().noneMatch(existing -> existing.id().equals(view.id()))) {
                        views.add(view);
                    }
                }
            }
            return views;
        }

        private List<ItemStack> linkedStacks(IndexEntry entry) {
            List<ItemStack> stacks = new ArrayList<>();
            if (!entry.icon().isEmpty()) {
                stacks.add(entry.icon());
            }
            for (Identifier id : entry.linkedItems()) {
                BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> stacks.add(new ItemStack(item)));
            }
            return stacks;
        }

        private void chip(TerminalRenderContext context, GuiGraphicsExtractor graphics, int x, int y, int w,
                String label, boolean active, int mouseX, int mouseY, HitKind kind, Identifier id) {
            boolean hover = TerminalUi.inside(mouseX, mouseY, x, y, w, 16);
            TerminalUi.compactButton(context, graphics, x, y, w, label, active ? ACCENT : TerminalUi.muted(context),
                    kind != HitKind.NONE, hover);
            if (kind != HitKind.NONE) {
                hits.add(new Hit(x, y, w, 16, kind, id));
            }
        }

        private void filterChip(TerminalRenderContext context, GuiGraphicsExtractor graphics, int x, int y, int w,
                TerminalFilter target, int mouseX, int mouseY) {
            chip(context, graphics, x, y, w, target.label(), filter == target, mouseX, mouseY, HitKind.FILTER,
                    EchoIndex.id("terminal/filter/" + target.name().toLowerCase(Locale.ROOT)));
        }

        private String title(String key) {
            return Component.translatable(key).getString();
        }

        private String summary(String key) {
            return Component.translatable(key).getString();
        }

        private String body(String key) {
            return Component.translatable(key).getString();
        }

        private int drawRenderCorePreview(TerminalRenderContext context, GuiGraphicsExtractor graphics,
                IndexEntry entry, int x, int y, int w, int mouseX, int mouseY) {
            if (!EchoRuntimeModules.isLoaded("echorendercore")) {
                return y;
            }
            try {
                Object result = Class.forName("com.knoxhack.echoindex.integration.IndexRenderCorePreviewBridge")
                        .getMethod("drawPreview", TerminalRenderContext.class, GuiGraphicsExtractor.class,
                                IndexEntry.class, int.class, int.class, int.class, int.class, int.class)
                        .invoke(null, context, graphics, entry, x, y, w, mouseX, mouseY);
                return result instanceof Integer nextY ? nextY : y;
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                return y;
            }
        }
    }

    private enum HitKind {
        NONE,
        FILTER,
        CATEGORY,
        ENTRY,
        DIAGNOSTICS,
        REFRESH,
        RECIPE_PAGE_PREV,
        RECIPE_PAGE_NEXT,
        PIN_RECIPE,
        UNPIN_RECIPE
    }

    private enum TerminalFilter {
        ALL("All"),
        DISCOVERED("Disc"),
        BOOKMARKED("Star"),
        RECIPES("Recipe"),
        WARNINGS("Warn");

        private final String label;

        TerminalFilter(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        static TerminalFilter from(Identifier id) {
            if (id == null) {
                return ALL;
            }
            String path = id.getPath();
            int slash = path.lastIndexOf('/');
            String key = slash >= 0 ? path.substring(slash + 1) : path;
            for (TerminalFilter filter : values()) {
                if (filter.name().equalsIgnoreCase(key)) {
                    return filter;
                }
            }
            return ALL;
        }
    }

    private record Hit(int x, int y, int w, int h, HitKind kind, Identifier id) {
    }

    private record LabeledRecipe(String label, IndexRecipeView view) {
    }
}
